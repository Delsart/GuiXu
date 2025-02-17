@file:Suppress("NOTHING_TO_INLINE")

package work.delsart.guixu.db

import kotlinx.io.IOException
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer
import work.delsart.guixu.db.platform.AutoIncreaseFileAccess
import work.delsart.guixu.db.platform.FixSizeFileAccess
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KType

abstract class StoreData {
    var id: Long = 0
}

// position(6) | length(4)
private const val indexItemLength = 10

private const val deleteFlag = -2147483647

class FileItem(val indexFile: AutoIncreaseFileAccess, val storeFile: AutoIncreaseFileAccess)

@Suppress("UNCHECKED_CAST")
class StorageBox<T : StoreData>(kType: KType, val path: Path, val name: String) {

    // structure: workingFileNum(1) | fileCount(1)
    // fileCount is ensured less than 3,
    private var metaDataFile = FixSizeFileAccess(Path(path, name), 2)

    private var fileArray: Array<FileItem> = emptyArray()

    private val serializer: KSerializer<T> = serializer(kType) as KSerializer<T>

    private val writeBuffer = ByteArray(indexItemLength)
    private val readBuffer = ByteArray(indexItemLength)

    var nextId = 1L
        private set

    private var inCompactReplaceFile = false

    init {
        initialMetaData()
    }

    private fun initialMetaData() {
        // initial metaData
        var workingFileNum = 0
        var fileCount = 1
        metaDataFile.read(0, readBuffer, 0, 2)
        if (readBuffer[1] > 0) {
            workingFileNum = readBuffer[0].toInt()
            fileCount = readBuffer[1].toInt()
        } else {
            writeBuffer[0] = 0
            writeBuffer[1] = 1
            metaDataFile.write(0, writeBuffer, 0, 2)
        }

        // initial fileArray
        fileArray = arrayOfNulls<FileItem>(fileCount) as Array<FileItem>
        var i = 0
        var fileNum: Int
        while (i < fileCount) {
            fileNum = (workingFileNum - i + 3) % 3
            fileArray[i] = FileItem(
                indexFile = AutoIncreaseFileAccess(Path(path, "$name-index-$fileNum"), indexItemLength.toLong()),
                storeFile = AutoIncreaseFileAccess(Path(path, "$name-$fileNum"))
            )
            i++
        }

        // initial nextId
        nextId = max(fileArray[0].indexFile.length() / indexItemLength, 1L)
    }


    private fun mergeFile(
        outputStore: AutoIncreaseFileAccess,
        outputIndex: AutoIncreaseFileAccess,
        mergeFileArray: Array<FileItem>,
    ) {
        val indexItemBuffer = ByteArray(indexItemLength)
        var buffer = ByteArray(32)
        val maxId = mergeFileArray[0].indexFile.length() / indexItemLength

        var id = 0L
        var length = 0
        var oldPosition: Long
        var newPosition = 0L
        var mergeFileI: Int
        var indexPosition: Long
        var fileItem: FileItem
        while (id < maxId) {
            mergeFileI = 0
            indexPosition = id * indexItemLength

            while (mergeFileI < mergeFileArray.size) {
                fileItem = mergeFileArray[mergeFileI]
                if (indexPosition > fileItem.indexFile.length()) break
                fileItem.indexFile.read(indexPosition, indexItemBuffer, 0, indexItemLength)
                length = indexItemBuffer.toIntBigEndianOffset6()
                // find valid item
                if (length > 0) break
                mergeFileI++
            }

            // no data, directly copy index item
            if (length < 1) {
                outputIndex.write(indexPosition, indexItemBuffer, 0, indexItemLength)
                id++
                continue
            }

            // copy data
            oldPosition = indexItemBuffer.toLongBigEndian6byte()
            if (length > buffer.size) buffer = ByteArray(length)
            mergeFileArray[mergeFileI].storeFile.read(oldPosition, buffer, 0, length)
            outputStore.write(newPosition, buffer, 0, length)

            // copy index item
            newPosition.toByteBigEndian6Byte(indexItemBuffer)
            outputIndex.write(indexPosition, indexItemBuffer, 0, indexItemLength)

            newPosition += length
            id++
        }
    }

    fun compact() {
        val buffer = ByteArray(2)
        metaDataFile.read(0, buffer, 0, 2)
        val workingFileNum = (buffer[0].toInt() + 1) % 3

        val oldFileArray = fileArray
        val newStoreFile = AutoIncreaseFileAccess(Path(path, "$name-$workingFileNum"))
        val newIndexFile =
            AutoIncreaseFileAccess(Path(path, "$name-index-$workingFileNum"), oldFileArray[0].indexFile.length())
        val outputStoreFile = AutoIncreaseFileAccess(Path(path, "$name-temp"))
        val outputIndexFile = AutoIncreaseFileAccess(Path(path, "$name-index-temp"), oldFileArray[0].indexFile.length())

        fileArray =
            if (oldFileArray.size > 1) arrayOf(FileItem(newIndexFile, newStoreFile), oldFileArray[0], oldFileArray[1])
            else arrayOf(FileItem(newIndexFile, newStoreFile), oldFileArray[0])

        mergeFile(outputStoreFile, outputIndexFile, oldFileArray)

        outputStoreFile.close()
        outputIndexFile.close()

        // replace file
        inCompactReplaceFile = true
        oldFileArray.forEach {
            it.indexFile.close()
            it.storeFile.close()
        }
        System.gc()
        while (true) {
            try {
                SystemFileSystem.atomicMove(outputStoreFile.path, oldFileArray[0].storeFile.path)
                SystemFileSystem.atomicMove(outputIndexFile.path, oldFileArray[0].indexFile.path)
                break
            } catch (e: IOException) {
                // wait for gc
                println(e)
                waitTime(5)
            }
        }
        // reopen new object
        fileArray = arrayOf(
            FileItem(
                indexFile = AutoIncreaseFileAccess(oldFileArray[0].indexFile.path),
                storeFile = AutoIncreaseFileAccess(oldFileArray[0].storeFile.path)
            ), oldFileArray[0]
        )
        inCompactReplaceFile = false

        // delete other file
        var i = 1
        while (i < oldFileArray.size) {
            SystemFileSystem.delete(oldFileArray[i].indexFile.path)
            SystemFileSystem.delete(oldFileArray[i].storeFile.path)
            i++
        }

        buffer[0] = workingFileNum.toByte()
        buffer[1] = min(buffer[1].toInt() + 1, 2).toByte()
        metaDataFile.write(0, buffer, 0, 2)
    }

    private fun setIndex(file: AutoIncreaseFileAccess, id: Long, position: Long, length: Int) {
        position.toByteBigEndian6Byte(writeBuffer)
        length.toByteBigEndianOffset6(writeBuffer)
        file.write(id * indexItemLength, writeBuffer, 0, indexItemLength)
    }

    private inline fun appendStore(id: Long, load: ByteArray) {
        val fileItem = fileArray[0]
        val length = load.size
        var position = fileItem.storeFile.length()
        setIndex(fileItem.indexFile, id, position, length)
        fileItem.storeFile.write(position, load, 0, length)
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun put(data: T) {
        if (data.id >= nextId) throw IllegalIdException(data.id, nextId)
        if (data.id == 0L) {
            data.id = nextId++
        }
        val byteArray = ProtoBuf.encodeToByteArray(serializer, data)
        appendStore(id = data.id, load = byteArray)
    }


    fun getByte(id: Long, fileIndex: Int = 0): ByteArray {
        if (fileIndex >= fileArray.size) throw EntryNoFoundException(id)
        val indexPosition = id * indexItemLength
        var fileItem = fileArray[fileIndex]
        try {
            fileItem.indexFile.read(indexPosition, readBuffer, 0, indexItemLength)

            val length = readBuffer.toIntBigEndianOffset6()

            if (length == 0) return getByte(id, fileIndex + 1)
            if (length < 0) throw EntryNoFoundException(id)

            val position = readBuffer.toLongBigEndian6byte()
            val array = ByteArray(length)

            fileItem.storeFile.read(position, array, 0, length)
            return array
        } catch (e: NullPointerException) {
            // when throw null point exception it means file is replacing,
            // so wait for finishing
            println(e)
            while (inCompactReplaceFile) {
                waitTime(5)
            }
            return getByte(id, 0)
        }
    }


    @OptIn(ExperimentalSerializationApi::class)
    fun get(id: Long): T {
        return ProtoBuf.decodeFromByteArray(serializer, getByte(id)).also { it.id = id }
    }

    fun remove(id: Long) {
        setIndex(fileArray[0].indexFile, id, position = 0, length = deleteFlag)
    }

    fun clear() {
        metaDataFile.close()
        fileArray.forEach {
            it.indexFile.close()
            it.storeFile.close()
        }
        System.gc()
        while (true) {
            try {
                SystemFileSystem.delete(metaDataFile.path)
                fileArray.forEach {
                    SystemFileSystem.delete(it.indexFile.path)
                    SystemFileSystem.delete(it.storeFile.path)
                }
                break
            } catch (e: IOException) {
                // wait for gc
                println(e)
                waitTime(5)
            }
        }
        metaDataFile = FixSizeFileAccess(Path(path, name), 2)
        initialMetaData()
    }
}

inline fun Long.toByteBigEndian6Byte(byteArray: ByteArray) {
    byteArray[0] = (this ushr 40).toByte()
    byteArray[1] = (this ushr 32).toByte()
    byteArray[2] = (this ushr 24).toByte()
    byteArray[3] = (this ushr 16).toByte()
    byteArray[4] = (this ushr 8).toByte()
    byteArray[5] = this.toByte()
}

inline fun Int.toByteBigEndianOffset6(byteArray: ByteArray) {
    byteArray[6] = (this shr 24).toByte()
    byteArray[7] = (this shr 16).toByte()
    byteArray[8] = (this shr 8).toByte()
    byteArray[9] = toByte()
}

inline fun ByteArray.toLongBigEndian6byte(): Long =
    (((this[0].toLong() and 0xFF) shl 40) or ((this[1].toLong() and 0xFF) shl 32) or ((this[2].toLong() and 0xFF) shl 24) or ((this[3].toLong() and 0xFF) shl 16) or ((this[4].toLong() and 0xFF) shl 8) or (this[5].toLong() and 0xFF))

inline fun ByteArray.toIntBigEndianOffset6(): Int =
    (((this[6].toInt() and 0xFF) shl 24) or ((this[7].toInt() and 0xFF) shl 16) or ((this[8].toInt() and 0xFF) shl 8) or (this[9].toInt() and 0xFF))


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
