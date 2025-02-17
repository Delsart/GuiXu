@file:OptIn(ExperimentalUnsignedTypes::class)

package work.delsart.guixu.db

import java.time.ZoneOffset
import kotlin.Byte
import kotlin.ByteArray
import kotlin.Int
import kotlin.Long

//
//@Suppress("NOTHING_TO_INLINE")
//internal class ByteArrayOperator(var hb: ByteArray, var position: Int = 0) {
//    constructor(capacity: Int) : this(ByteArray(capacity), 0)
//
//    inline fun skip(count: Int) {
//        position += count
//    }
//
//    inline fun get(): Byte {
//        return hb[position++]
//    }
//
//    inline fun put(v: Byte) {
//        hb[position++] = v
//    }
//
//    val int: Int
//        inline get() = ((hb[position++].toUByte().toInt() shl 24) or
//                (hb[position++].toUByte().toInt() shl 16) or
//                (hb[position++].toUByte().toInt() shl 8) or
//                hb[position++].toUByte().toInt())
//
//
//    inline fun putInt(v: Int) {
//        hb[position++] = (v ushr 24).toByte()
//        hb[position++] = (v ushr 16).toByte()
//        hb[position++] = (v ushr 8).toByte()
//        hb[position++] = v.toByte()
//    }
//
//
//    inline fun putLong(v: Long) {
//        hb[position++] = (v ushr 56).toByte()
//        hb[position++] = (v ushr 48).toByte()
//        hb[position++] = (v ushr 40).toByte()
//        hb[position++] = (v ushr 32).toByte()
//        hb[position++] = (v ushr 24).toByte()
//        hb[position++] = (v ushr 16).toByte()
//        hb[position++] = (v ushr 8).toByte()
//        hb[position++] = v.toByte()
//    }
//
//
//    val long: Long
//        inline get() = ((hb[position++].toUByte().toLong() shl 56) or
//                (hb[position++].toUByte().toLong() shl 48) or
//                (hb[position++].toUByte().toLong() shl 40) or
//                (hb[position++].toUByte().toLong() shl 32) or
//                (hb[position++].toUByte().toLong() shl 24) or
//                (hb[position++].toUByte().toLong() shl 16) or
//                (hb[position++].toUByte().toLong() shl 8) or
//                hb[position++].toUByte().toLong())
//
//
//    val float: Float
//        inline get() = Float.fromBits(int)
//
//
//    val double: Double
//        inline get() = Double.fromBits(long)
//
//
//    inline fun getBytes(len: Int): ByteArray {
//        val bytes = ByteArray(len)
//        System.arraycopy(hb, position, bytes, 0, len)
//        position += len
//        return bytes
//    }
//
//
//    inline fun putBytes(src: ByteArray) = putBytes(src, 0, src.size)
//    inline fun putBytes(src: ByteArray, offset: Int, size: Int) {
//        System.arraycopy(src, offset, hb, position, size)
//        position += size
//    }
//
//
////    inline fun getChecksum(start: Int, size: Int): Long {
////        if (size <= 0) return 0L
////        var p = start
////        val n = size ushr 3
////        val remain = size and 7
////        var checkSum = 0L
////        for (i in 0..<n) {
////            checkSum = checkSum xor getLong(p)
////            p += 8
////        }
////        val maxShift = remain shl 3
////        var i = 0
////        while (i < maxShift) {
////            checkSum = checkSum xor ((hb[p++].toLong() and 0xFFL) shl i)
////            i += 8
////        }
////        val shift = (start and 7) shl 3
////        return (checkSum shl shift) or (checkSum ushr (64 - shift))
////    }
//
//}