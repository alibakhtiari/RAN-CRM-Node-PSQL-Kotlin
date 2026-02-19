package com.ran.crm.utils

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class BooleanTypeAdapter : JsonDeserializer<Boolean> {
    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Boolean {
        if (json.isJsonPrimitive) {
            val primitive = json.asJsonPrimitive
            if (primitive.isBoolean) {
                return primitive.asBoolean
            }
            if (primitive.isNumber) {
                // 1 -> true, 0 -> false
                return primitive.asInt == 1
            }
            if (primitive.isString) {
                val str = primitive.asString
                return when (str.lowercase()) {
                    "true", "1" -> true
                    "false", "0" -> false
                    else -> false
                }
            }
        }
        return false
    }
}
