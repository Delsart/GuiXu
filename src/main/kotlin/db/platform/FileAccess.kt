package work.delsart.guixu.db.platform

import kotlinx.io.files.Path
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import kotlin.math.min


enum class FileAccessMode {
    RW {
        override val stringValue = "rw"
    };

    abstract val stringValue: String
}


interface FileAccess {
    val path: Path
    fun length(): Long
    fun seek(position: Long)
    fun read(position: Long, dst: ByteArray, offset: Int, size: Int)
    fun write(position: Long, source: ByteArray, offset: Int, size: Int)
    fun writeInt(position: Long, data: Int)
    fun readInt(position: Long): Int
    fun writeLong(position: Long, data: Long)
    fun readLong(position: Long): Long
    fun writeByte(position: Long, data: Byte)
    fun readByte(position: Long): Byte
    fun close()
}


class FixSizeFileAccess(
    override val path: Path,
    val size: Long = 0,
    mode: FileAccessMode = FileAccessMode.RW
) : FileAccess {
    private val file = RandomAccessFile(path.toString(), mode.stringValue)

    override fun length(): Long = size

    init {
        file.setLength(size)
    }

    override fun seek(position: Long) {
        file.seek(position)
    }

    override fun read(position: Long, dst: ByteArray, offset: Int, size: Int) {
        file.seek(position)
        file.read(dst, offset, size)
    }

    override fun write(position: Long, source: ByteArray, offset: Int, size: Int) {
        file.seek(position)
        file.write(source, offset, size)
    }

    override fun writeInt(position: Long, data: Int) {
        file.seek(position)
        file.writeInt(data)
    }

    override fun readInt(position: Long): Int {
        file.seek(position)
        return file.readInt()
    }


    override fun writeLong(position: Long, data: Long) {
        file.seek(position)
        file.writeLong(data)
    }


    override fun readLong(position: Long): Long {
        file.seek(position)
        return file.readLong()
    }

    override fun writeByte(position: Long, data: Byte) {
        file.seek(position)
        file.writeByte(data.toInt())
    }

    override fun readByte(position: Long): Byte {
        file.seek(position)
        return file.readByte()
    }

    override fun close() {
        file.close()
    }
}



