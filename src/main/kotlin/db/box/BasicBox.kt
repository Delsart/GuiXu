@file:Suppress("NOTHING_TO_INLINE")

package work.delsart.guixu.db.box

import kotlinx.io.IOException
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import work.delsart.guixu.db.GuiXu
import work.delsart.guixu.db.bean.BoxInfo
import work.delsart.guixu.db.platform.AutoIncreaseFileAccess
import work.delsart.guixu.db.platform.FileAccessMode
import work.delsart.guixu.db.platform.FixSizeFileAccess
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndIncrement
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.math.max
import kotlin.math.min


// structure: position(8) | length(4)
const val indexItemLength = 12

const val deleteFlag = -2147483647

const val spineTime = 5L

@OptIn(ExperimentalAtomicApi::class)
class FileItem(
    val indexFile: AutoIncreaseFileAccess,
    val storeFile: AutoIncreaseFileAccess,
    val storeAppendPosition: AtomicLong = AtomicLong(storeFile.length())
)


@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalAtomicApi::class)
abstract class BasicBox(val host: GuiXu, val path: Path, val name: String) {
    // structure: workingFileNum(1) | fileCount(1)
    // fileCount is ensured less than 3,
    protected var metaDataFile = FixSizeFileAccess(Path(path, name), 2)
    protected var fileArray: Array<FileItem> = emptyArray()

    // need be manipulated by implementation of box
    // example: create: id = nextId++,
    // arbitrary set value of id will cause file size increasing unexpectedly
    protected val nextId = AtomicLong(1)

    protected var inCompactReplaceFile = false

    init {
        initialMetaData()
    }

    private fun initialMetaData() {
        // initial metaData
        var workingFileNum = 0
        var fileCount = 1
        val buffer = ByteArray(2)
        metaDataFile.read(0, buffer, 0, 2)
        if (buffer[1] > 0) {
            workingFileNum = buffer[0].toInt()
            fileCount = buffer[1].toInt()
        } else {
            buffer[0] = 0
            buffer[1] = 1
            metaDataFile.write(0, buffer, 0, 2)
        }

        // initial fileArray
        var fileNum: Int
        fileArray = Array<FileItem>(fileCount) {
            fileNum = (workingFileNum - it + 3) % 3
            FileItem(
                indexFile = host.autoIncreaseFileBuilder(
                    Path(path, "$name-index-$fileNum"), indexItemLength.toLong(), FileAccessMode.RW
                ),
                storeFile = host.autoIncreaseFileBuilder(Path(path, "$name-$fileNum"), 0, FileAccessMode.RW)
            )
        }

        // initial nextId
        nextId.store(max(fileArray[0].indexFile.length() / indexItemLength, 1L))
    }

    private fun mergeFile(
        outputStore: AutoIncreaseFileAccess,
        outputIndex: AutoIncreaseFileAccess,
        mergeFileArray: Array<FileItem>,
    ) {
        var buffer = ByteArray(32)
        val emptyIndexItemArray = ByteArray(12)
        val maxId = mergeFileArray[0].indexFile.length() / indexItemLength

        var id = 0L
        var length = 0
        var oldPosition: Long
        var newPosition = 0L
        var mergeFileI: Int
        var indexPosition: Long
        var fileItem: FileItem = fileArray[0]
        while (id < maxId) {
            mergeFileI = 0
            indexPosition = id * indexItemLength

            while (mergeFileI < mergeFileArray.size) {
                fileItem = mergeFileArray[mergeFileI]
                if (indexPosition > fileItem.indexFile.length()) break
                length = fileItem.indexFile.readInt(indexPosition + 8)
                // find valid item
                if (length > 0) break
                mergeFileI++
            }

            // no data, set empty index item
            if (length < 1) {
                outputIndex.write(indexPosition, emptyIndexItemArray, 0, indexItemLength)
                id++
                continue
            }

            // copy data
            oldPosition = fileItem.indexFile.readLong(indexPosition)
            if (length > buffer.size) buffer = ByteArray(length)
            fileItem.storeFile.read(oldPosition, buffer, 0, length)
            outputStore.write(newPosition, buffer, 0, length)

            // copy index item
            outputIndex.writeLong(indexPosition, newPosition)
            outputIndex.writeInt(indexPosition + 8, length)

            newPosition += length
            id++
        }
    }

    fun compact() {
        val buffer = ByteArray(2)
        metaDataFile.read(0, buffer, 0, 2)
        val workingFileNum = (buffer[0].toInt() + 1) % 3
        val oldFileArray = fileArray

        val newStoreFile = host.autoIncreaseFileBuilder(Path(path, "$name-$workingFileNum"), 0, FileAccessMode.RW)
        val newIndexFile = host.autoIncreaseFileBuilder(
            Path(path, "$name-index-$workingFileNum"), oldFileArray[0].indexFile.length(), FileAccessMode.RW
        )

        fileArray =
            if (oldFileArray.size > 1) arrayOf(FileItem(newIndexFile, newStoreFile), oldFileArray[0], oldFileArray[1])
            else arrayOf(FileItem(newIndexFile, newStoreFile), oldFileArray[0])

        // the output file is used under known circumstance,
        // so we directly use AutoIncreaseFileAccess
        val outputStoreFile = AutoIncreaseFileAccess(Path(path, "$name-temp"))
        val outputIndexFile = AutoIncreaseFileAccess(Path(path, "$name-index-temp"), oldFileArray[0].indexFile.length())



        mergeFile(outputStoreFile, outputIndexFile, oldFileArray)

        // there is no competition between write and compact
        // the only competition between read and compact is replacing file progress below.

        // replace file
        inCompactReplaceFile = true
        oldFileArray.forEach {
            it.indexFile.close()
            it.storeFile.close()
        }
        outputStoreFile.close()
        outputIndexFile.close()
        System.gc()
        while (true) {
            try {
                SystemFileSystem.atomicMove(outputStoreFile.path, oldFileArray[0].storeFile.path)
                SystemFileSystem.atomicMove(outputIndexFile.path, oldFileArray[0].indexFile.path)
                break
            } catch (e: IOException) {
                // wait for gc
                println(e)
                waitTime(spineTime)
            }
        }
        // reopen new object
        fileArray = arrayOf(
            FileItem(
                indexFile = host.autoIncreaseFileBuilder(oldFileArray[0].indexFile.path, 0, FileAccessMode.RW),
                storeFile = host.autoIncreaseFileBuilder(oldFileArray[0].storeFile.path, 0, FileAccessMode.RW)
            ), oldFileArray[0]
        )
        inCompactReplaceFile = false

        // delete other file
        var i = 1
        while (i < oldFileArray.size) {
            SystemFileSystem.deleteIfExist(oldFileArray[i].indexFile.path)
            SystemFileSystem.deleteIfExist(oldFileArray[i].storeFile.path)
            i++
        }

        buffer[0] = workingFileNum.toByte()
        buffer[1] = min(buffer[1].toInt() + 1, 2).toByte()
        metaDataFile.write(0, buffer, 0, 2)
    }

    protected inline fun setIndex(file: AutoIncreaseFileAccess, id: Long, position: Long, length: Int) {
        val filePosition = id * indexItemLength
        file.writeLong(filePosition, position)
        file.writeInt(filePosition + 8, length)
    }

    protected inline fun appendStore(id: Long, load: ByteArray) {
        val fileItem = fileArray[0]
        val length = load.size
        val position = fileItem.storeAppendPosition.fetchAndAdd(length.toLong())
        setIndex(fileItem.indexFile, id, position, length)
        fileItem.storeFile.write(position, load, 0, length)
    }

    protected fun getByte(id: Long, fileIndex: Int = 0): ByteArray {
        if (fileIndex >= fileArray.size) throw EntryNoFoundException(id)
        val indexPosition = id * indexItemLength
        var fileItem = fileArray[fileIndex]
        try {
            val length = fileItem.indexFile.readInt(indexPosition + 8)

            if (length == 0) return getByte(id, fileIndex + 1)
            if (length < 0) throw EntryNoFoundException(id)

            val position = fileItem.indexFile.readLong(indexPosition)
            val array = ByteArray(length)

            fileItem.storeFile.read(position, array, 0, length)
            return array
        } catch (e: NullPointerException) {
            // when throw null point exception it means file is replacing,
            // so wait for finishing
            println(e)
            while (inCompactReplaceFile) {
                waitTime(spineTime)
            }
            return getByte(id, 0)
        }
    }

    protected inline fun traversAll(action: (ByteArray) -> Unit) {
        val fileItem = fileArray[0]
        val maxId = fileItem.indexFile.length() / indexItemLength
        var id = 1L
        while (id < maxId) {
            try {
                action(getByte(id))
            } catch (e: EntryNoFoundException) {
            }
            id++
        }
    }

    protected inline fun checkIdAndGet(id: Long): Long {
        if (id == 0L) {
            return nextId.fetchAndIncrement()
        }
        val localNextId = nextId.load()
        if (id >= localNextId) throw IllegalIdException(id, localNextId)
        return id
    }


    open fun clear(reInit: Boolean = true) {
        metaDataFile.close()
        fileArray.forEach {
            it.indexFile.close()
            it.storeFile.close()
        }
        System.gc()
        while (true) {
            try {
                SystemFileSystem.deleteIfExist(metaDataFile.path)
                fileArray.forEach {
                    SystemFileSystem.deleteIfExist(it.indexFile.path)
                    SystemFileSystem.deleteIfExist(it.storeFile.path)
                }
                break
            } catch (e: IOException) {
                // wait for gc
                println(e)
                waitTime(spineTime)
            }
        }
        if (reInit) {
            metaDataFile = FixSizeFileAccess(Path(path, name), 2)
            initialMetaData()
        }
    }

    protected inline fun removeEntry(id: Long) {
        setIndex(fileArray[0].indexFile, id, position = 0, length = deleteFlag)
    }

    open fun close() {
        metaDataFile.close()
        fileArray.forEach {
            it.indexFile.close()
            it.storeFile.close()
        }
    }

    open fun getInfo(): BoxInfo {
        var dataSize = 0L
        var indexSize = 0L
        val localFileArray = fileArray
        localFileArray.forEach {
            dataSize += it.storeFile.fileSize
            indexSize += it.indexFile.fileSize
        }
        return BoxInfo(dataSize, indexSize)
    }
}


@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
inline fun <T> Array<T>.forEach(action: (T) -> Unit) {
    contract { callsInPlace(action) }
    var i = 0
    while (i < this.size) {
        action(get(i))
        i++
    }
}

inline fun waitTime(time: Long) {
    Thread.sleep(time)
}

class EntryNoFoundException(id: Long) : Exception("item id:$id do not exist")
class IllegalIdException(id: Long, nextId: Long) : Exception("id > nextId id:$id,nextId:$nextId")

inline fun FileSystem.deleteIfExist(path: Path) {
    if (exists(path))
        delete(path)
}