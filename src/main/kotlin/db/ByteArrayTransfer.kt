@file:Suppress("NOTHING_TO_INLINE")

package work.delsart.guixu.db


inline fun Int.toByteBigEndian(byteArray: ByteArray) {
    byteArray[0] = (this shr 24).toByte()
    byteArray[1] = (this shr 16).toByte()
    byteArray[2] = (this shr 8).toByte()
    byteArray[3] = toByte()
}



inline fun Long.toByteBigEndian(byteArray: ByteArray) {
    byteArray[0] = (this ushr 56).toByte()
    byteArray[1] = (this ushr 48).toByte()
    byteArray[2] = (this ushr 40).toByte()
    byteArray[3] = (this ushr 32).toByte()
    byteArray[4] = (this ushr 24).toByte()
    byteArray[5] = (this ushr 16).toByte()
    byteArray[6] = (this ushr 8).toByte()
    byteArray[7] = this.toByte()
}



