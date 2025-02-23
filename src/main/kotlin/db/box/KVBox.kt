package work.delsart.guixu.db.box

import kotlinx.io.IOException
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import work.delsart.guixu.db.GuiXu
import work.delsart.guixu.db.platform.FileAccessMode
import work.delsart.guixu.db.utils.toByteArray
import work.delsart.guixu.db.utils.toInt
import work.delsart.guixu.db.utils.toLong
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndIncrement

private const val kvItemFixLength = 13L

private object DeletedEmpty
private data class KVItem(
    val filePosition: Long, val id: Long, val type: Byte, val value: Any
)

private const val booleanTrue: Byte = 1

// for performance, we do not use enum class
private const val valueTypeDelete: Byte = 0
private const val valueTypeBoolean: Byte = 1
private const val valueTypeByte: Byte = 2
private const val valueTypeInt: Byte = 3
private const val valueTypeLong: Byte = 4
private const val valueTypeFloat: Byte = 5
private const val valueTypeDouble: Byte = 6
private const val valueTypeString: Byte = 7
private const val valueTypeByteArray: Byte = 8

@OptIn(ExperimentalAtomicApi::class)
class KVBox(host: GuiXu, path: Path, name: String) : BasicBox(host, path, name) {
    private var map = HashMap<String, KVItem>(0)

    // structure: capacity(4) | keyItem[N]
    // keyItem: keyLength(4) | type(1) | id(8) | key(N)
    private var mapFile = host.autoIncreaseFileBuilder(Path(path, "$name-K2IdMap"), 4, FileAccessMode.RW)
    private val mapFileAppendPosition = AtomicLong(0L)

    init {
        initialMap()
    }

    private fun initialMap() {
        val length = mapFile.length()
        map = HashMap<String, KVItem>(mapFile.readInt(0))
        mapFileAppendPosition.store(length)

        val buffer = ByteArray(32)
        var cursor = 4L
        var offset: Long
        var keyLength: Int
        var type: Byte
        var id: Long
        var key: String
        var data: Any
        while (cursor < length) {
            offset = cursor
            keyLength = mapFile.readInt(cursor)
            cursor += 4
            type = mapFile.readByte(cursor)
            cursor += 1

            id = mapFile.readLong(cursor)
            cursor += 8

            mapFile.read(cursor, buffer, 0, keyLength)
            key = buffer.decodeToString(startIndex = 0, endIndex = keyLength)
            cursor += keyLength

            data = if (type == valueTypeDelete) {
                DeletedEmpty
            } else
                convertByteArrayToData(getByte(id), type)

            map[key] = KVItem(
                filePosition = offset,
                type = type,
                id = id,
                value = data
            )
        }
    }

    // only primitive
    private fun convertDataToByteArray(data: Any, type: Byte): ByteArray {
        return when (type) {
            valueTypeBoolean -> byteArrayOf(if (data as Boolean) 1 else 0)
            valueTypeByte -> byteArrayOf(data as Byte)
            valueTypeInt -> (data as Int).toByteArray()
            valueTypeLong -> (data as Long).toByteArray()
            valueTypeFloat -> (data as Float).toRawBits().toByteArray()
            valueTypeDouble -> (data as Double).toRawBits().toByteArray()
            valueTypeString -> (data as String).toByteArray()
            valueTypeByteArray -> data as ByteArray
            else -> error("$type can not be convert")
        }
    }

    // only primitive
    private fun convertByteArrayToData(data: ByteArray, type: Byte): Any {
        return when (type) {
            valueTypeBoolean -> (data[0] == booleanTrue)
            valueTypeByte -> data[0]
            valueTypeInt -> data.toInt()
            valueTypeLong -> data.toLong()
            valueTypeFloat -> Float.fromBits(data.toInt())
            valueTypeDouble -> Double.fromBits(data.toLong())
            valueTypeString -> data.toString()
            valueTypeByteArray -> data
            else -> error("$type can not be convert")
        }
    }

    private fun addKVItem(key: String, id: Long, type: Byte, data: Any): KVItem {
        val keyByteArray = key.toByteArray(charset = Charsets.UTF_8)
        var position = mapFileAppendPosition.fetchAndAdd(kvItemFixLength + keyByteArray.size)
        val item = KVItem(
            filePosition = position,
            id,
            type,
            data
        )
        mapFile.writeInt(position, keyByteArray.size)
        position += 4

        mapFile.writeByte(position, type)
        position += 1

        mapFile.writeLong(position, id)
        position += 8

        mapFile.write(position, keyByteArray, 0, keyByteArray.size)

        // update map size
        mapFile.writeInt(0, map.size shl 1)
        return item
    }

    private fun updateKVItem(old: KVItem, type: Byte, data: Any): KVItem {
        if (old.type != valueTypeDelete && old.type != type)
            throw TypeErrorException(old.type)
        mapFile.writeByte(old.filePosition + 4, type)
        return old.copy(type = type, value = data)
    }


    private fun putData(key: String, type: Byte, data: Any) {
        val oldKVItem = map[key]
        val id: Long
        val newKVItem: KVItem
        if (oldKVItem == null) {
            id = nextId.fetchAndIncrement()
            newKVItem = addKVItem(key, id, type, data)
        } else {
            id = oldKVItem.id
            newKVItem = updateKVItem(oldKVItem, type, data)
        }
        map[key] = newKVItem
        appendStore(id = id, convertDataToByteArray(data, type))
    }

    private fun getData(key: String, type: Byte): Any {
        val item = map[key]
        if (item == null || item.type == valueTypeDelete)
            throw KeyNoFoundException(key)
        if (item.type != type)
            throw TypeErrorException(item.type)
        return item.value
    }

    fun remove(key: String) {
        val item = map[key]
        if (item != null) {
            removeEntry(item.id)
            mapFile.writeByte(item.filePosition + 4, valueTypeDelete)
            map[key] = item.copy(type = valueTypeDelete, value = DeletedEmpty)
        }
    }

    fun putBoolean(key: String, data: Boolean) {
        putData(key, valueTypeBoolean, data)
    }

    fun putByte(key: String, data: Byte) {
        putData(key, valueTypeByte, data)
    }

    fun putInt(key: String, data: Int) {
        putData(key, valueTypeInt, data)
    }

    fun putLong(key: String, data: Long) {
        putData(key, valueTypeLong, data)
    }

    fun putFloat(key: String, data: Float) {
        putData(key, valueTypeFloat, data)
    }

    fun putDouble(key: String, data: Double) {
        putData(key, valueTypeDouble, data)
    }

    fun putString(key: String, data: String) {
        putData(key, valueTypeString, data)
    }

    fun putByteArray(key: String, data: ByteArray) {
        putData(key, valueTypeByteArray, data)
    }


    fun getBoolean(key: String): Boolean {
        return getData(key, valueTypeBoolean) as Boolean
    }

    fun getByte(key: String): Byte {
        return getData(key, valueTypeByte) as Byte
    }

    fun getInt(key: String): Int {
        return getData(key, valueTypeInt) as Int
    }

    fun getLong(key: String): Long {
        return getData(key, valueTypeLong) as Long
    }

    fun getFloat(key: String): Float {
        return getData(key, valueTypeFloat) as Float
    }

    fun getDouble(key: String): Double {
        return getData(key, valueTypeDouble) as Double
    }

    fun getString(key: String): String {
        return getData(key, valueTypeString) as String
    }

    fun getByteArray(key: String): ByteArray {
        return getData(key, valueTypeByteArray) as ByteArray
    }

    override fun clear(reInit: Boolean) {
        mapFile.close()
        super.clear(reInit)
        while (true) {
            try {
                SystemFileSystem.deleteIfExist(mapFile.path)
                break
            } catch (e: IOException) {
                // wait for gc
                println(e)
                waitTime(spineTime)
            }
        }
        if (reInit) {
            mapFile = host.autoIncreaseFileBuilder(Path(path, "$name-K2IdMap"), 4, FileAccessMode.RW)
            initialMap()
        }
    }

}

class TypeErrorException(exactlyType: Byte) : Exception("exactlyType:$exactlyType")
class KeyNoFoundException(key: String) : Exception("key:$key")
