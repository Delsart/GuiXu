@file:Suppress("NOTHING_TO_INLINE")

package work.delsart.guixu.db.platform

import kotlinx.io.files.Path
import work.delsart.guixu.db.toByteBigEndian
import java.io.RandomAccessFile
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.max


interface FileAccess {
    val path: Path
    fun length(): Long
    fun seek(position: Long)
    fun read(position: Long, dst: ByteArray, offset: Int, size: Int)
    fun write(position: Long, source: ByteArray, offset: Int, size: Int)
    fun close()
}


open class FixSizeFileAccess(override val path: Path, val size: Long = 0, mode: String = "rw") : FileAccess {
    private val file = RandomAccessFile(path.toString(), mode)
    private val channel = file.channel.also {
        if (it.size() < size)
            it.truncate(size)
    }
    private var mmap = channel.map(FileChannel.MapMode.READ_WRITE, 0, size)
    override fun length(): Long = size


    override fun seek(position: Long) {
        mmap.position(position.toInt())
    }

    override fun read(position: Long, dst: ByteArray, offset: Int, size: Int) {
        mmap.get(position.toInt(), dst, offset, size)
    }

    override fun write(position: Long, source: ByteArray, offset: Int, size: Int) {
        mmap.put(position.toInt(), source, offset, size)
    }

    override fun close() {
        mmap = null
        channel.close()
        file.close()
    }

}


open class AutoIncreaseFileAccess(override val path: Path, minimumSize: Long = 0, mode: String = "rw") : FileAccess {
    private val file = RandomAccessFile(path.toString(), mode)
    private var fileLength = file.length()
    private val channel = file.channel
    private var mmap = channel.map(FileChannel.MapMode.READ_WRITE, 0, fileLength)

    private var buffer = ByteArray(8)

    private var actualLength = if (fileLength > 7) {
        mmap.position(0)
        mmap.getLong()
    } else {
        resizeSize(minimumSize + 8)
        minimumSize.toByteBigEndian(buffer)
        mmap.put(0, buffer)
        minimumSize
    }

    private inline fun resizeSize(newSize: Long) {
        channel.truncate(newSize)
        mmap = channel.map(FileChannel.MapMode.READ_WRITE, 0, newSize)
        fileLength = newSize
    }

    private inline fun movedPosition(position: Long): Int = position.toInt() + 8



    override fun length(): Long = actualLength

    override fun seek(position: Long) {
        mmap.position(movedPosition(position))
    }


    override fun read(position: Long, dst: ByteArray, offset: Int, size: Int) {
        mmap.get(movedPosition(position), dst, offset, size)
    }

    override fun write(position: Long, source: ByteArray, offset: Int, size: Int) {
        val movedPosition = movedPosition(position)
        val length = size - offset
        if (movedPosition + length >= fileLength) {
            resizeSize(
                max(
                    (movedPosition(position) + length).toLong() shl 1,
                    movedPosition(position) + length + 655370L
                )
            )
        }

        mmap.put(movedPosition, source, offset, size)

        val newLength = position + length
        if (newLength > actualLength) {
            actualLength = newLength
            actualLength.toByteBigEndian(buffer)
            mmap.put(0, buffer)
        }
    }


//    fun unmapMappedByteBuffer(buffer: MappedByteBuffer) {
//        try {
//            val cleanerMethod: Method = buffer.javaClass.getMethod("cleaner")
//            cleanerMethod.isAccessible = true
//            val cleaner = cleanerMethod.invoke(buffer)
//            if (cleaner != null) {
//                val cleanMethod = cleaner.javaClass.getMethod("clean")
//                cleanMethod.invoke(cleaner)
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }


    override fun close() {
        mmap = null
        channel.close()
        file.close()
    }

    fun clear() {
        mmap.clear()
        actualLength = 0
        mmap.putLong(0, actualLength)
    }

    private inline fun MappedByteBuffer.myGet(
        position: Int,
        dst: ByteArray,
        offset: Int = 0,
        size: Int = dst.size
    ) {
        position(position)
        get(dst, offset, size)
    }


    private inline fun MappedByteBuffer.myPut(
        position: Int,
        dst: ByteArray,
        offset: Int = 0,
        size: Int = dst.size
    ) {
        position(position)
        put(dst, offset, size)
    }
}

