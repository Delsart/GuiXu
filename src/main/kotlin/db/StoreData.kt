package work.delsart.guixu.db


abstract class StoreData {
    /**
     * Unique identifier for the data item.
     * - The value is globally unique, monotonically increasing, and never reused.
     * - Setting it to `0` indicates the creation of a new data item.
     * - Setting it to an existing `id` value indicates updating the corresponding data item.
     * - It must not exceed the `nextId` value of the `box`.
     * - Manually setting this value is not recommended.
     */
    var id: Long = 0
}