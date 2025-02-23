package work.delsart.guixu


import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import kotlinx.serialization.Serializable
import work.delsart.guixu.db.GuiXu
import work.delsart.guixu.db.bean.StoreData
import work.delsart.guixu.db.box.TypedBox
import java.lang.Thread.sleep
import java.util.concurrent.Executors
import kotlin.Int
import kotlin.String
import kotlin.Unit
import kotlin.repeat
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis


@Serializable
data class TestClass(
    var name: String,
    var age: Int,
//    val message: List<String> = emptyList<String>()
) : StoreData()

fun main() {
    val path = Path("E:\\Projects\\GuiXuDB\\test")
    val db = GuiXu(path)
//    var typedBox = db.boxFor<TestClass>()
//    var byteArrayBox = db.byteArrayBoxFor("test")
    var kvBox = db.kvBoxFor("testKv")


//    typedBox.clear()
//    byteArrayBox.clear()
    kvBox.clear()

//    testConcurrent(testBox)
//
//    normalTest(
//        create = { typedBox.put(TestClass("a", age = it)) },
//        update = { typedBox.put(TestClass("a", age = it shl 1).apply { id = it + 1L }) },
//        read = { typedBox.get(it + 1L) },
//        remove = { typedBox.remove(it + 1L) }
//    )

//    val byteArray = byteArrayOf(1, 23, 2, 4, 1)
//    normalTest(
//        create = { byteArrayBox.put(data = byteArray) },
//        update = { byteArrayBox.put(it + 1L, byteArray.also { array -> array[0] = it.toByte() }) },
//        read = { byteArrayBox.get(it + 1L) },
//        remove = { byteArrayBox.remove(it + 1L) }
//    )


    normalTest(
        count = 1000,
        create = { kvBox.putInt(it.toString(),0) },
        update = { kvBox.putInt(it.toString(),1) },
        read = { kvBox.getInt(it.toString()) },
        remove = {  kvBox.remove(it.toString()) }
    )

}


fun test() {
    val i = 13213
    var time = measureNanoTime {
        repeat(1_000_000) {
            i * 12
        }

    }
    println("${time}ns")

    time = measureNanoTime {
        repeat(1_000_000) {
            (i shl 3) + (i shl 2)
        }

    }
    println("${time}ns")
}


fun testConcurrent(
    box: TypedBox<TestClass>
) {
    val executorService = Executors.newCachedThreadPool()
    executorService.execute {
        val time = measureTimeMillis {
            repeat(1_000_000) {
                box.put(TestClass("a", age = it))
            }

        }
        println("put: ${time}ms")

    }
    executorService.execute {
        sleep(40)
        val time = measureTimeMillis {
            box.compact()
        }
        println("compact: ${time}ms")
    }

    executorService.execute {
        sleep(30)
        repeat(100) {
            sleep(2)
            println(box.get((it + 1).toLong()))
        }
    }
}


inline fun normalTest(
    count: Int = 10_000_000,
    create: (Int) -> Unit,
    update: (Int) -> Unit,
    read: (Int) -> Unit,
    remove: (Int) -> Unit,
) {


    println("count: $count")

    var time = measureTimeMillis {
        repeat(count) {
            create(it)
        }
    }
    println("create: ${time}ms")
    println(time.toFloat() / count)


    time = measureTimeMillis {
        repeat(count) {
            update(it)
        }
    }
    println("update: ${time}ms")
    println(time.toFloat() / count)


    time = measureTimeMillis {
        repeat(count) {
            read(it)
        }
    }
    println("read: ${time}ms")
    println(time.toFloat() / count)

    time = measureTimeMillis {
        repeat(count) {
            remove(it)
        }
    }
    println("remove: ${time}ms")
    println(time.toFloat() / count)


}

fun writeIntervalTest(box: TypedBox<TestClass>) {
    runBlocking {
        repeat(100) {

            var time = System.currentTimeMillis()
            val count = 100
            repeat(count) {
                box.put(TestClass("a", age = it))
            }
            val timeDiffer = System.currentTimeMillis() - time
            println("write $timeDiffer-${timeDiffer.toFloat() / count}")

            delay(1000)
            repeat(count) {
                box.get(it + 1L)
            }
            println("read $timeDiffer-${timeDiffer.toFloat() / count}")
            delay(1000)

        }
    }

}