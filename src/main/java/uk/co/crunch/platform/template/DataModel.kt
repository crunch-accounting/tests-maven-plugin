package uk.co.crunch.platform.template

class DataModel(val data: MutableMap<String, Any> = mutableMapOf()) {
    operator fun set(key: String, value: Any) {
        data[key] = value
    }

    fun with(key: String, value: Any) = run {
        data[key] = value
        this
    }

    operator fun get(key: String) = data[key]

    fun containsKey(key: String) = data.containsKey(key)
}