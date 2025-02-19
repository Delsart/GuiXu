# 归墟 GuiXu Database (Working in progress)

## Introduction

GuiXu is a high-performance, minimalist embedded database based on Kotlin, combining Kotlin Serialization and Memory-Mapped File (MMAP) technology to provide efficient read/write operations and excellent space utilization. Inspired by LevelDB's SSTable (Sorted String Table), it has been optimized for performance and functionality, making it particularly suitable for scenarios requiring high throughput and low latency.

## Features

- **High Performance**: 10 million read/write operations take approximately 500 milliseconds.
- **Low Overhead**: Each data entry introduces only about 18 bytes of additional information, ensuring high space utilization.
- **No Write Amplification**: The optimized storage structure avoids write amplification issues.
- **Non-blocking Compaction**: Compaction operations do not block read/write operations, ensuring high system availability.
- **Concurrency Support**: Supports concurrent access by multiple read/write threads.

## Development Motivation

The GuiXu database was born out of the following core needs:
1. **Strengthening Immutable Data Structures**: In most scenarios, data is primarily appended, with minimal updates or deletions. Therefore, GuiXu optimizes write performance while reducing the frequency of compaction operations. Although compaction is non-blocking, our design ensures that it rarely needs to be triggered.
2. **High Performance**: Through Memory-Mapped File (MMAP) technology and efficient indexing structures, GuiXu achieves excellent read/write performance, making it suitable for high-throughput scenarios.
3. **Pure Kotlin Implementation**: GuiXu is entirely developed in Kotlin, with no need for compiler plugins or dependencies on other languages, ensuring a pure and consistent development experience.
4. **Minimalist API**: Provides a simple and easy-to-use API, reducing the learning curve and allowing developers to quickly get started and integrate it into their projects.

## Performance

![benchmark](/pictures/Snipaste_2025-02-18_11-56-15.png)

## Usage Example

```kotlin
// Define data class
@Serializable
class TestClass(
    var name: String,
    var age: Int,
) : StoreData()

// Initialize database
val path = Path("E:\\Projects\\GuiXuDB\\test")
val db = GuiXu(path)
val box = db.boxFor<TestClass>()

// Create (id = 0 indicates a new entry)
box.put(TestClass("Aa", 18))

// Update (id is an existing value)
box.put(TestClass("Bb", 20).also { it.id = 1 })

// Read by ID
val data = box.get(1)

// Delete by ID
box.remove(1)

// Clear all
box.clear()

// Get all
val list = box.all()
```

## Technical Details

### Storage Structure

- **Index File**: The position and length information of each data entry is stored in the index file, structured as `position (8 bytes) | length (4 bytes)`, with each index entry occupying 12 bytes.
- **Data File**: The actual data content is stored in the data file, with data entries located using the position information from the index file.
- **Metadata File**: Stores the current working file number and file count, ensuring correct file switching and compaction operations.

### Memory-Mapped File (MMAP)

GuiXu uses memory-mapped file technology to map files directly into memory, avoiding frequent system calls and memory copying, thereby significantly improving read/write performance.

### Data Compaction

GuiXu's compaction operations are non-blocking, ensuring that read/write operations can proceed normally during compaction. The compaction process includes the following steps:
1. Create a new temporary file.
2. Merge data from multiple files into the temporary file.
3. Replace old files using atomic operations.
4. Delete old files that are no longer needed.

### Concurrency Support

GuiXu supports concurrent access by multiple read threads and one write thread. Through fine-grained locking mechanisms and file switching strategies, it ensures data consistency and performance in high-concurrency scenarios.

### Serialization

GuiXu uses Kotlin Serialization for data serialization and deserialization, supporting the storage and querying of custom data classes.

## Contributions and Feedback

GuiXu is still under development. We welcome issues and pull requests.


## 简介

归墟（GuiXu）是一个基于 Kotlin 的高性能极简嵌入式数据库，结合了 Kotlin Serialization
和内存映射文件（MMAP）技术，提供了高效的读写操作和优秀的空间利用率。其设计灵感来源于 LevelDB 的 SSTable（Sorted String
Table），但在性能和功能上进行了优化，特别适合需要高吞吐量和低延迟的场景。

## 特性

- **高性能**：1000 万次读写操作仅需约 500 毫秒。
- **低开销**：每个数据项（Entry）仅引入约 18 字节的额外信息，空间利用率高。
- **无写入放大**：通过优化的存储结构，避免了写入放大问题。
- **非阻塞合并**：合并操作（Compaction）不会阻塞读写操作，确保系统的高可用性。
- **并发支持**：支持多个读写线程的并发访问。

## 开发初衷

归墟数据库的诞生源于以下几个核心需求：
1. **强化不可变数据结构** ：在绝大部分场景中，数据以追加为主，极少更新或删除，因此归墟数据库优化了写入性能，同时减少了合并操作（Compaction）的频率。尽管合并操作是非阻塞的，但我们通过设计使其在大多数情况下无需频繁触发。
2. **高性能**：通过内存映射文件（MMAP）和高效索引结构，归墟数据库实现了出色的读写性能，适合高吞吐量场景。
3. **纯 Kotlin 实现**：归墟数据库完全基于 Kotlin 开发，无需编译器插件或其他语言依赖，确保开发体验的纯粹性和一致性。
4. **极简 API**：提供简单易用的 API，降低学习成本，让开发者能够快速上手并集成到项目中。

## 性能

![benchmark](/pictures/Snipaste_2025-02-18_11-56-15.png)

## 使用示例

```kotlin
// 定义数据类
@Serializable
class TestClass(
    var name: String,
    var age: Int,
) : StoreData()

// 初始化数据库
val path = Path("E:\\Projects\\GuiXuDB\\test")
val db = GuiXu(path)
val box = db.boxFor<TestClass>()

// 创建数据项（id = 0 表示创建新项）
box.put(TestClass("Aa", 18))

// 更新数据项（id 为已存在的值）
box.put(TestClass("Bb", 20).also { it.id = 1 })

// 通过 ID 读取数据项
val data = box.get(1)

// 通过 ID 删除数据项
box.remove(1)

// 清空所有数据项
box.clear()

// 获取所有数据项
val list = box.all()
```

## 技术细节

### 存储结构

- **索引文件**：每个数据项的位置和长度信息存储在索引文件中，结构为 `position(8字节) | length(4字节)`，每个索引项占用 12 字节。
- **数据文件**：实际的数据内容存储在数据文件中，数据项通过索引文件中的位置信息进行定位。
- **元数据文件**：存储当前工作文件编号和文件数量，确保文件切换和合并操作的正确性。

### 内存映射文件（MMAP）

归墟数据库使用内存映射文件技术，将文件直接映射到内存中，避免了频繁的系统调用和内存拷贝，从而大幅提升了读写性能。

### 数据合并（Compaction）

归墟数据库的合并操作是非阻塞的，确保在合并过程中读写操作仍可正常进行。合并过程包括以下步骤：
1. 创建新的临时文件。
2. 将多个文件中的数据合并到临时文件中。
3. 使用原子操作替换旧文件。
4. 删除不再需要的旧文件。

### 并发支持

归墟数据库支持多个读线程和一个写线程的并发访问。通过细粒度的锁机制和文件切换策略，确保在高并发场景下的数据一致性和性能。

### 序列化

归墟数据库使用 Kotlin Serialization 进行数据的序列化和反序列化，支持自定义数据类的存储和查询。

## 贡献与反馈

归墟数据库目前仍在开发中，欢迎提交 Issue 和 Pull Request。