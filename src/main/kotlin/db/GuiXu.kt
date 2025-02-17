@file:Suppress("NOTHING_TO_INLINE")
package work.delsart.guixu.db


import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.reflect.typeOf

@Serializable
private data class BoxInfo(
    val name: String,
    var nextId: Long = 1
)

@Serializable
private data class GuiXuMetaInfo(
    val maxSize: Long = 1024 * 1024 * 10,
    var boxInfoList: List<BoxInfo> = emptyList<BoxInfo>()
)

@OptIn(ExperimentalSerializationApi::class)
class GuiXu(val path: Path) {

    private val metaInfoPath = Path(path, metaInfoName)

    private val metaInfo: GuiXuMetaInfo = if (SystemFileSystem.exists(metaInfoPath))
        SystemFileSystem.source(metaInfoPath).buffered().readByteArray().let { ProtoBuf.decodeFromByteArray(it) }
    else GuiXuMetaInfo().also { storeMetaInfo(it) }


    init {
        if (!SystemFileSystem.exists(path))
            SystemFileSystem.createDirectories(path)
    }

    private inline fun storeMetaInfo(data: GuiXuMetaInfo) {
        SystemFileSystem.sink(metaInfoPath).buffered().write(ProtoBuf.encodeToByteArray(data))
    }

    inline fun <reified T: StoreData> boxFor(name: String? = null): StorageBox<T> {
        val kType = typeOf<T>()
        val theName = name ?: kType.toString()
        return StorageBox(kType, path, theName)
    }

}

private const val metaInfoName = "metaInfo"