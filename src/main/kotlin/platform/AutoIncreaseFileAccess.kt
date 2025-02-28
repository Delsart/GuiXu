@file:Suppress("NOTHING_TO_INLINE")

package work.delsart.guixu.platform

import kotlinx.io.bytestring.getByteString
import kotlinx.io.files.Path
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import kotlin.concurrent.atomics.AtomicLong
import kotlin.math.min


// path, initialSize, mode
internal typealias AutoIncreaseFileBuilder = (Path, Long, FileAccessMode) -> AutoIncreaseFileAccess

private const val maxIncreaseSize = (1L shl 24) // 16M
//private const val segmentLengthBit = 20 // 1M
//private const val segmentLength = 1L shl segmentLengthBit
//private const val segmentLengthMask = (1L shl segmentLengthBit) - 1


class AutoIncreaseFileAccess(
    override val path: Path,
    initialSize: Long = 0,
    mode: FileAccessMode = FileAccessMode.RW
) : FileAccess {
    private val file = RandomAccessFile(path.toString(), mode.stringValue)
    var fileSize = file.length()
        private set
    private val channel = file.channel
    private var mmap = channel.map(FileChannel.MapMode.READ_WRITE, 0, fileSize)


    private var actualLength = 0L

    init {
        if (fileSize >= 8) {
            actualLength = mmap.getLong((fileSize - 8).toInt())
        } else {
            resizeSize(initialSize + 8)
            mmap.putLong((fileSize - 8).toInt(),initialSize)
            actualLength = initialSize
        }
    }

    private inline fun resizeSize(newSize: Long) {
        file.setLength(newSize)
        mmap = channel.map(FileChannel.MapMode.READ_WRITE, 0, newSize)
        fileSize = newSize
    }

    private inline fun movedPosition(position: Long): Int = position.toInt()

    override fun length(): Long = actualLength

    override fun seek(position: Long) {
        mmap.position(movedPosition(position))
    }

    override fun read(position: Long, dst: ByteArray, offset: Int, size: Int) {
        mmap.get(movedPosition(position), dst, offset, size)
    }

    private fun updateLength(newLength: Long) {
        if (newLength > actualLength) {
            if (newLength >= fileSize - 8) {
                resizeSize(
                    min(
                        newLength shl 3,
                        newLength + maxIncreaseSize
                    )
                )
            }
            actualLength = newLength
            mmap.putLong((fileSize - 8).toInt(), actualLength)
        }
    }

    override fun write(position: Long, source: ByteArray, offset: Int, size: Int) {
        val movedPosition = movedPosition(position)
        val newLength = position + size
        updateLength(newLength)
        mmap.put(movedPosition, source, offset, size)
    }

    override fun writeInt(position: Long, data: Int) {
        updateLength(position + 4)
        mmap.putInt(movedPosition(position), data)
    }

    override fun readInt(position: Long): Int = mmap.getInt(movedPosition(position))

    override fun writeLong(position: Long, data: Long) {
        updateLength(position + 8)
        mmap.putLong(movedPosition(position), data)
    }

    override fun readLong(position: Long): Long = mmap.getLong(movedPosition(position))

    override fun writeByte(position: Long, data: Byte) {
        updateLength(position + 8)
        mmap.put(movedPosition(position), data)
    }

    override fun readByte(position: Long) = mmap.get(movedPosition(position))

    override fun close() {
        mmap.getByteString()
        mmap = null
        channel.close()
        file.close()
    }

    fun clear() {
        mmap.clear()
        actualLength = 0
        mmap.putLong(0, actualLength)
    }
}


//@Suppress("OVERRIDE_BY_INLINE")
//open class AutoIncreaseFileAccess2(override val path: Path, minimumSize: Long = 0, mode: String = "rw") : FileAccess {
//    private val file = RandomAccessFile(path.toString(), mode)
//    private var fileLength = file.length()
//    private val channel = file.channel
//
//    //    private var  getSegment(position)SegmentList = channel.map(FileChannel.MapMode.READ_WRITE, 0, fileLength)
//    private var threadSegmentArray = ThreadLocal.withInitial {
//        arrayOf<WeakReference<MappedByteBuffer>>(WeakReference<MappedByteBuffer>(null))
//    }
//
//
//    var actualLength = if (fileLength > 7) {
//        file.seek(fileLength - 8)
//        file.readLong()
//    } else {
//        resizeSize(minimumSize + 8)
//        file.seek(fileLength - 8)
//        file.writeLong(minimumSize)
//        minimumSize
//    }
//        private set
//
//    private inline fun resizeSize(newSize: Long) {
//        channel.truncate(newSize)
//        fileLength = newSize
//        val segmentArray = threadSegmentArray.get()
//        segmentArray.forEach {
//            it.clear()
//        }
//    }
//
//    private fun inSegmentPosition(position: Long): Int = (position and segmentLengthMask).toInt()
//
//
//    private fun getSegment(position: Long): MappedByteBuffer {
//        val index = (position shr segmentLengthBit).toInt()
//        var segmentArray = threadSegmentArray.get()
//        if (index >= segmentArray.size) {
//            val increaseCount = 5
//            val newArray =
//                arrayOfNulls<WeakReference<MappedByteBuffer>>(segmentArray.size + increaseCount) as Array<WeakReference<MappedByteBuffer>>
//            System.arraycopy(segmentArray, 0, newArray, 0, segmentArray.size)
//            repeat(increaseCount) {
//                newArray[segmentArray.size + it] = WeakReference<MappedByteBuffer>(null)
//            }
//            segmentArray = newArray
//            threadSegmentArray.set(newArray)
//        }
//
//        val segment = segmentArray[index].get()
//        if (segment == null) {
//            val position = index.toLong() shl segmentLengthBit
//            val length = min(segmentLength, fileLength - position)
//            return channel.map(
//                FileChannel.MapMode.READ_WRITE,
//                position,
//                length
//            ).also {
//                segmentArray[index] = WeakReference(it)
//            }
//        } else return segment
//
//    }
//
//
//    override fun length(): Long = actualLength
//
//    override fun seek(position: Long) {
//        TODO()
//    }
//
//    override fun read(position: Long, dst: ByteArray, offset: Int, size: Int) {
//        getSegment(position).get(inSegmentPosition(position), dst, offset, size)
//    }
//
//    private fun updateLength(newLength: Long) {
//        val fileLengthPosition = fileLength - 8
//        if (newLength > actualLength) {
//            if (newLength >= fileLengthPosition) {
//                resizeSize(
//                    min(
//                        newLength shl 2, newLength + maxIncreaseSize
//                    )
//                )
//            }
//            actualLength = newLength
//
//            getSegment(fileLengthPosition).putLong(inSegmentPosition(fileLengthPosition), actualLength)
//        }
//    }
//
//
//    override fun write(position: Long, source: ByteArray, offset: Int, size: Int) {
//        val newLength = position + size
//        updateLength(newLength)
//        getSegment(position).put(inSegmentPosition(position), source, offset, size)
//    }
//
//    override fun writeInt(position: Long, data: Int) {
//        updateLength(position + 4)
//        getSegment(position).putInt(inSegmentPosition(position), data)
//    }
//
//
//    override fun readInt(position: Long): Int = getSegment(position).getInt(inSegmentPosition(position))
//
//
//    override fun writeLong(position: Long, data: Long) {
//        updateLength(position + 8)
//        getSegment(position).putLong(inSegmentPosition(position), data)
//    }
//
//
//    override fun readLong(position: Long): Long = getSegment(position).getLong(inSegmentPosition(position))
//
//
//    override fun close() {
//        val segmentArray = threadSegmentArray.get()
//        segmentArray.forEach {
//            it.clear()
//        }
//        channel.close()
//        file.close()
//    }
//
//    fun clear() {
//        actualLength = 0
//        file.seek(fileLength - 8)
//        file.writeLong(actualLength)
//    }
//
//    private inline fun MappedByteBuffer.myGet(
//        position: Int, dst: ByteArray, offset: Int = 0, size: Int = dst.size
//    ) {
//        position(position)
//        get(dst, offset, size)
//    }
//
//
//    private inline fun MappedByteBuffer.myPut(
//        position: Int, dst: ByteArray, offset: Int = 0, size: Int = dst.size
//    ) {
//        position(position)
//        put(dst, offset, size)
//    }
//}
//