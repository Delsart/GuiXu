package work.delsart.guixu


import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.delay
import kotlinx.io.files.Path
import kotlinx.serialization.Serializable
import work.delsart.guixu.db.GuiXu
import work.delsart.guixu.db.StorageBox
import work.delsart.guixu.db.StoreData
import java.lang.Thread.sleep
import java.util.concurrent.Executors
import kotlin.Int
import kotlin.String
import kotlin.Unit
import kotlin.collections.plus
import kotlin.plus
import kotlin.repeat
import kotlin.sequences.plus
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis
import kotlin.text.plus


@Serializable
data class TestClass(
    var name: String,
    var age: Int,
//    val message:List<String> = List(1000) { "tfajhlajkshfjghjfghfhjfhfjgkjashfklhasklfhhajskf" }
) : StoreData()

fun main() {
    val path = Path("E:\\Projects\\GuiXuDB\\test")
    val db = GuiXu(path)
    var testBox = db.boxFor<TestClass>()

    testBox.clear()

//    repeat(100) {
//        testBox.put(TestClass("Aa",1).apply { id=1L+it })
//    }

    testConcurrent(testBox)

//    normalTest(
//        testBox,
//        create = { testBox.put(TestClass("a", age = it)) },
//        update = { testBox.put(TestClass("d", age = it * 2).apply { id = it + 1L }) },
//        read = { testBox.get(it + 1L) },
//        remove = { testBox.remove(it + 1L) }
//    )

//
//    testBox.remove(1)
//    println(testBox.get(1))
}

//fun testPrint(message: Any) = println("${System.currentTimeMillis()} - ${message.toString()}")
fun printTime() = println(System.currentTimeMillis())

fun testConcurrent(
    box: StorageBox<TestClass>
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
        sleep(20)
        repeat(100) {
            sleep(2)
            println(box.get((it + 1).toLong()))
        }
    }
}


inline fun normalTest(
    box: StorageBox<TestClass>,
    create: (Int) -> Unit,
    update: (Int) -> Unit,
    read: (Int) -> Unit,
    remove: (Int) -> Unit,
) {

    val count = 10_000_000

    println("count:$count")

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

fun writeIntervalTest(box: StorageBox<TestClass>) {
    runBlocking {
        repeat(100) {
            box.clear()

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