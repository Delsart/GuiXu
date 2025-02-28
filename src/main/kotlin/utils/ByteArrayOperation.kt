package work.delsart.guixu.utils


import kotlin.Byte
import kotlin.ByteArray
import kotlin.Int
import kotlin.Long


@Suppress("NOTHING_TO_INLINE")
internal class ByteArrayOperator(var hb: ByteArray, var position: Int = 0) {
    constructor(capacity: Int) : this(ByteArray(capacity), 0)


    inline fun writeByte(v: Byte) {
        hb[position++] = v
    }

    inline fun readByte(): Byte {
        return hb[position++]
    }


    inline fun writeInt(v: Int) {
        hb[position++] = (v shr 24).toByte()
        hb[position++] = (v shr 16).toByte()
        hb[position++] = (v shr 8).toByte()
        hb[position++] = v.toByte()
    }

    inline fun readInt(): Int {
        return (((hb[position++].toInt() and 0xff) shl 24) or
                ((hb[position++].toInt() and 0xff) shl 16) or
                ((hb[position++].toInt() and 0xff) shl 8) or
                (hb[position++].toInt() and 0xff))
    }


    inline fun writeLong(v: Long) {
        hb[position++] = (v shr 56).toByte()
        hb[position++] = (v shr 48).toByte()
        hb[position++] = (v shr 40).toByte()
        hb[position++] = (v shr 32).toByte()
        hb[position++] = (v shr 24).toByte()
        hb[position++] = (v shr 16).toByte()
        hb[position++] = (v shr 8).toByte()
        hb[position++] = v.toByte()
    }

    inline fun readLong(): Long {
        return (((hb[position++].toLong() and 0xff) shl 56) or
                ((hb[position++].toLong() and 0xff) shl 48) or
                ((hb[position++].toLong() and 0xff) shl 40) or
                ((hb[position++].toLong() and 0xff) shl 32) or
                ((hb[position++].toLong() and 0xff) shl 24) or
                ((hb[position++].toLong() and 0xff) shl 16) or
                ((hb[position++].toLong() and 0xff) shl 8) or
                (hb[position++].toLong() and 0xff))
    }


    inline fun writeFloat(v: Float) = writeInt(v.toBits())
    inline fun readFloat(): Float = Float.fromBits(readInt())

    inline fun writeDouble(v: Double) = writeLong(v.toBits())
    inline fun readDouble(): Double = Double.fromBits(readLong())


    inline fun writeBytes(src: ByteArray) = writeBytes(src, 0, src.size)

    inline fun writeBytes(src: ByteArray, offset: Int, size: Int) {
        System.arraycopy(src, offset, hb, position, size)
        position += size
    }

    inline fun readBytes(len: Int): ByteArray {
        val bytes = ByteArray(len)
        System.arraycopy(hb, position, bytes, 0, len)
        position += len
        return bytes
    }

    inline fun writeString(string: String) {
        val src = string.toByteArray(Charsets.UTF_8)
        System.arraycopy(src, 0, hb, position, src.size)
        position += src.size
    }

    inline fun readString(len: Int): String {
        val result = hb.decodeToString(startIndex = position, endIndex = position + len)
        position += len
        return result
    }


//    inline fun getChecksum(start: Int, size: Int): Long {
//        if (size <= 0) return 0L
//        var p = start
//        val n = size shr 3
//        val remain = size and 7
//        var checkSum = 0L
//        for (i in 0..<n) {
//            checkSum = checkSum xor getLong(p)
//            p += 8
//        }
//        val maxShift = remain shl 3
//        var i = 0
//        while (i < maxShift) {
//            checkSum = checkSum xor ((hb[p++].toLong() and 0xFFL) shl i)
//            i += 8
//        }
//        val shift = (start and 7) shl 3
//        return (checkSum shl shift) or (checkSum shr (64 - shift))
//    }

}

// bigEndian
fun Int.toByteArray(): ByteArray = byteArrayOf(
    (this shr 24).toByte(),
    (this shr 16).toByte(),
    (this shr 8).toByte(),
    toByte(),
)

fun Long.toByteArray(): ByteArray = byteArrayOf(
    (this shr 56).toByte(),
    (this shr 48).toByte(),
    (this shr 40).toByte(),
    (this shr 32).toByte(),
    (this shr 24).toByte(),
    (this shr 16).toByte(),
    (this shr 8).toByte(),
    this.toByte(),
)


fun ByteArray.toInt(): Int = (((this[0].toInt() and 0xff) shl 24) or
        ((this[1].toInt() and 0xff) shl 16) or
        ((this[2].toInt() and 0xff) shl 8) or
        (this[3].toInt() and 0xff))


fun ByteArray.toLong(): Long = (((this[0].toLong() and 0xff) shl 56) or
        ((this[1].toLong() and 0xff) shl 48) or
        ((this[2].toLong() and 0xff) shl 40) or
        ((this[3].toLong() and 0xff) shl 32) or
        ((this[4].toLong() and 0xff) shl 24) or
        ((this[5].toLong() and 0xff) shl 16) or
        ((this[6].toLong() and 0xff) shl 8) or
        (this[7].toLong() and 0xff))

