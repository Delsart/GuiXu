@file:Suppress("NOTHING_TO_INLINE")

package work.delsart.guixu


import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.ExperimentalSerializationApi
import work.delsart.guixu.bean.StoreData
import work.delsart.guixu.box.BasicBox
import work.delsart.guixu.box.ByteArrayBox
import work.delsart.guixu.box.KVBox
import work.delsart.guixu.box.TypedBox
import work.delsart.guixu.platform.AutoIncreaseFileAccess
import work.delsart.guixu.platform.AutoIncreaseFileBuilder
import work.delsart.guixu.platform.FileAccessMode
import work.delsart.guixu.utils.ByteArrayOperator
import work.delsart.guixu.utils.DangerousOperation
import work.delsart.guixu.utils.DoNotUseDirectly
import kotlin.reflect.typeOf

private const val metaInfoName = "metaInfo"

// structure: nameLength(4) | name(n) | typeLength(4) | type(n)
private class BoxItem(val name: String, val type: String, val instance: BasicBox? = null)

@OptIn(ExperimentalSerializationApi::class, DoNotUseDirectly::class)
class GuiXu(
    val path: Path,
    // maybe we need some specific file access implementation
    val autoIncreaseFileBuilder: AutoIncreaseFileBuilder = { path, minimumSize, mode ->
        AutoIncreaseFileAccess(path, minimumSize, mode)
    }
) {
    // structure: boxItem[N]
//    private val metaInfoFile = autoIncreaseFileBuilder(Path(path, metaInfoName), 0, FileAccessMode.RW)
    private val boxList: MutableList<BoxItem> = mutableListOf()

    init {
        if (!SystemFileSystem.exists(path))
            SystemFileSystem.createDirectories(path)

//        println(TypedBox::class.qualifiedName)
//        val length = metaInfoFile.length()
//        var buffer = ByteArray(length.toInt())
//        val operator = ByteArrayOperator(buffer)
//
//        var stringLength: Int
//        var name: String
//        var type: String
//        while (operator.position < length) {
//            stringLength = operator.readInt()
//            name = operator.readString(stringLength)
//            stringLength = operator.readInt()
//            type = operator.readString(stringLength)
//        }
//
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

//    @DangerousOperation
//    fun clear(reInit: Boolean = false) {
//        boxList.forEach {
//            it.clear(reInit)
//        }
//    }

}

