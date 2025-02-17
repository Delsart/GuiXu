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

private const val MaxIncreaseSize = 655370L

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
        val newLength = position + size
        if (newLength > actualLength) {
            if (newLength >= fileLength - 8) {
                resizeSize(
                    max(
                        newLength shl 1,
                        newLength + MaxIncreaseSize
                    )
                )
            }
            actualLength = newLength
            actualLength.toByteBigEndian(buffer)
            mmap.put(0, buffer)
        }
        mmap.put(movedPosition, source, offset, size)
    }


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

