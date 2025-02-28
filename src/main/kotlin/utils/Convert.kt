package work.delsart.guixu.utils

internal fun IntArray.toByteArray(): ByteArray {
    val operator = ByteArrayOperator(ByteArray(this.size shl 2))
    var i = 0
    while (i < size) {
        operator.writeInt(this[i])
        i++
    }
    return operator.hb
}

internal fun LongArray.toByteArray(): ByteArray {
    val operator = ByteArrayOperator(ByteArray(this.size shl 3))
    var i = 0
    while (i < size) {
        operator.writeLong(this[i])
        i++
    }
    return operator.hb
}

internal fun FloatArray.toByteArray(): ByteArray {
    val operator = ByteArrayOperator(ByteArray(this.size shl 2))
    var i = 0
    while (i < size) {
        operator.writeFloat(this[i])
        i++
    }
    return operator.hb
}


internal fun DoubleArray.toByteArray(): ByteArray {
    val operator = ByteArrayOperator(ByteArray(this.size shl 3))
    var i = 0
    while (i < size) {
        operator.writeDouble(this[i])
        i++
    }
    return operator.hb
}


internal fun ByteArray.toIntArray(): IntArray {
    val operator = ByteArrayOperator(this)
    val result = IntArray(size shr 2)
    var i = 0
    while (i < result.size) {
        result[i] = operator.readInt()
        i++
    }
    return result
}

internal fun ByteArray.toLongArray(): LongArray {
    val operator = ByteArrayOperator(this)
    val result = LongArray(size shr 3)
    var i = 0
    while (i < result.size) {
        result[i] = operator.readLong()
        i++
    }
    return result
}


internal fun ByteArray.toFloatArray(): FloatArray {
    val operator = ByteArrayOperator(this)
    val result = FloatArray(size shr 2)
    var i = 0
    while (i < result.size) {
        result[i] = operator.readFloat()
        i++
    }
    return result
}


internal fun ByteArray.toDoubleArray(): DoubleArray {
    val operator = ByteArrayOperator(this)
    val result = DoubleArray(size shr 3)
    var i = 0
    while (i < result.size) {
        result[i] = operator.readDouble()
        i++
    }
    return result
}

