package work.delsart.guixu.db.box

import kotlinx.io.files.Path
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer
import work.delsart.guixu.db.GuiXu
import work.delsart.guixu.db.bean.StoreData
import kotlin.reflect.KType


@Suppress("UNCHECKED_CAST")
class TypedBox<T : StoreData>(kType: KType, host: GuiXu, path: Path, name: String) : BasicBox(host, path, name) {
    private val serializer: KSerializer<T> = serializer(kType) as KSerializer<T>

    @OptIn(ExperimentalSerializationApi::class)
    fun put(data: T) {
        data.id = checkIdAndGet(data.id)
        val byteArray = ProtoBuf.encodeToByteArray(serializer, data)
        appendStore(id = data.id, load = byteArray)
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun get(id: Long): T {
        return ProtoBuf.decodeFromByteArray(serializer, getByte(id)).also { it.id = id }
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun all(): List<T> {
        val result = mutableListOf<T>()
        traversAll {
            result.add(ProtoBuf.decodeFromByteArray(serializer, it))
        }
        return result
    }

    fun remove(id: Long) {
        removeEntry(id)
    }
}