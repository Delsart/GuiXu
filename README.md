# 归墟 GuiXu Database (Working in progress)

## Performance
![benchmark](/pictures/Snipaste_2025-02-18_11-56-15.png)

## Usage
```kotlin
// define your class
@Serializable
data class TestClass(
    var name: String,
    var age: Int,
) : StoreData()

// setup db
val path = Path("E:\\Projects\\GuiXuDB\\test")
val db = GuiXu(path)
var box = db.boxFor<TestClass>()

// create (id = 0)
box.put(TestClass("Aa", 18))

// update (id = exist one)
box.put(TestClass("Bb", 20).also { it.id = 1 })

// read/query by id
val data = box.get(1)

// remove by id
box.remove(1)

// remove all entry
box.clear()

// get all entry
val list = box.all()
```