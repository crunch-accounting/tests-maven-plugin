package uk.co.crunch.platform.template

class DataModel(private val data: MutableMap<String, Any> = mutableMapOf()) {
    operator fun set(key: String, value: Any) {
        data[key] = value
    }

    operator fun get(key: String) = data[key]
}
