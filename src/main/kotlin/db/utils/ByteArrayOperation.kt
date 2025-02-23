package work.delsart.guixu.db.utils


import java.time.ZoneOffset
import kotlin.Byte
import kotlin.ByteArray
import kotlin.Int
import kotlin.Long
import kotlin.toUByte


@Suppress("NOTHING_TO_INLINE")
internal class ByteArrayOperator(var hb: ByteArray, var position: Int = 0) {
    constructor(capacity: Int) : this(ByteArray(capacity), 0)

    inline fun skip(count: Int) {
        position += count
    }

    inline fun get(): Byte {
        return hb[position++]
    }

    inline fun put(v: Byte) {
        hb[position++] = v
    }

    val int: Int
        inline get() = (((hb[position++].toInt() and 0xff) shl 24) or
                ((hb[position++].toInt() and 0xff) shl 16) or
                ((hb[position++].toInt() and 0xff) shl 8) or
                (hb[position++].toInt() and 0xff))


    inline fun putInt(v: Int) {
        hb[position++] = (v shr 24).toByte()
        hb[position++] = (v shr 16).toByte()
        hb[position++] = (v shr 8).toByte()
        hb[position++] = v.toByte()
    }


    inline fun putLong(v: Long) {
        hb[position++] = (v shr 56).toByte()
        hb[position++] = (v shr 48).toByte()
        hb[position++] = (v shr 40).toByte()
        hb[position++] = (v shr 32).toByte()
        hb[position++] = (v shr 24).toByte()
        hb[position++] = (v shr 16).toByte()
        hb[position++] = (v shr 8).toByte()
        hb[position++] = v.toByte()
    }


    val long: Long
        inline get() = (((hb[position++].toLong() and 0xff) shl 56) or
                ((hb[position++].toLong() and 0xff) shl 48) or
                ((hb[position++].toLong() and 0xff) shl 40) or
                ((hb[position++].toLong() and 0xff) shl 32) or
                ((hb[position++].toLong() and 0xff) shl 24) or
                ((hb[position++].toLong() and 0xff) shl 16) or
                ((hb[position++].toLong() and 0xff) shl 8) or
                (hb[position++].toLong() and 0xff))


    val float: Float
        inline get() = Float.fromBits(int)


    val double: Double
        inline get() = Double.fromBits(long)


    inline fun getBytes(len: Int): ByteArray {
        val bytes = ByteArray(len)
        System.arraycopy(hb, position, bytes, 0, len)
        position += len
        return bytes
    }


    inline fun putBytes(src: ByteArray) = putBytes(src, 0, src.size)
    inline fun putBytes(src: ByteArray, offset: Int, size: Int) {
        System.arraycopy(src, offset, hb, position, size)
        position += size
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