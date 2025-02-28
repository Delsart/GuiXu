package work.delsart.guixu.box

import kotlinx.io.files.Path
import work.delsart.guixu.GuiXu
import work.delsart.guixu.utils.DoNotUseDirectly

class ByteArrayBox @DoNotUseDirectly constructor(
    host: GuiXu, path: Path, name: String,
) : BasicBox(host, path, name) {

    fun put(id: Long = 0, data: ByteArray) {
        appendStore(id = checkIdAndGet(id), load = data)
    }

    fun get(id: Long): ByteArray {
        return getByte(id)
    }

    fun all(): List<ByteArray> {
        val result = mutableListOf<ByteArray>()
        traversAll {
            result.add(it)
        }
        return result
    }

    fun remove(id: Long) {
        removeEntry(id)
    }
}