@file:Suppress("NOTHING_TO_INLINE")

package work.delsart.guixu.db


import kotlinx.io.files.Path
import kotlinx.serialization.ExperimentalSerializationApi
import work.delsart.guixu.db.bean.StoreData
import work.delsart.guixu.db.box.BasicBox
import work.delsart.guixu.db.box.ByteArrayBox
import work.delsart.guixu.db.box.KVBox
import work.delsart.guixu.db.box.TypedBox
import work.delsart.guixu.db.platform.AutoIncreaseFileAccess
import work.delsart.guixu.db.platform.AutoIncreaseFileBuilder
import work.delsart.guixu.db.platform.FileAccess
import kotlin.reflect.typeOf

private const val metaInfoName = "metaInfo"


@OptIn(ExperimentalSerializationApi::class)
class GuiXu(
    val path: Path,
    // maybe we need some specific file access implementation
    val autoIncreaseFileBuilder: AutoIncreaseFileBuilder = { path, minimumSize, mode ->
        AutoIncreaseFileAccess(path, minimumSize, mode)
    }
) {
    private val boxList: MutableList<BasicBox> = mutableListOf()

    private val metaInfoPath = Path(path, metaInfoName)


    init {

    }

    inline fun <reified T : StoreData> boxFor(name: String? = null): TypedBox<T> {
        val kType = typeOf<T>()
        val theName = name ?: kType.toString()
        return TypedBox(kType, this, path, theName)
    }


    fun byteArrayBoxFor(name: String): ByteArrayBox {
        return ByteArrayBox(this, path, name)
    }

    fun kvBoxFor(name: String): KVBox {
        return KVBox(this, path, name)
    }

    fun clear(reInit: Boolean = false) {
        boxList.forEach {
            it.clear(reInit)
        }
    }

}

