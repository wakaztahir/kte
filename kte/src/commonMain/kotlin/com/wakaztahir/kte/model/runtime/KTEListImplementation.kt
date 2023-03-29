package com.wakaztahir.kte.model.runtime

import com.wakaztahir.kte.model.BooleanValue
import com.wakaztahir.kte.model.IntValue
import com.wakaztahir.kte.model.StringValue
import com.wakaztahir.kte.model.model.*

object KTEListImplementation {

    val propertyMap by lazy { hashMapOf<String, KTEValue>().apply { putObjectFunctions() } }

    fun HashMap<String, KTEValue>.putObjectFunctions() {
        put("getType", object : KTEFunction() {
            override fun invoke(model: KTEObject, invokedOn: KTEValue, parameters: List<ReferencedValue>): KTEValue {
                return StringValue("list")
            }
            override fun toString(): String = "getType() : string"
        })
        put("get", object : KTEFunction() {
            override fun invoke(model: KTEObject, invokedOn: KTEValue, parameters: List<ReferencedValue>): KTEValue {
                val index = parameters.getOrNull(0)?.asNullablePrimitive(model)?.value as? Int
                require(index != null) {
                    "list.get(int) expects a single Int parameter instead of ${parameters.size}"
                }
                return (invokedOn.asNullableList(model)!!.collection)[index]
            }

            override fun toString(): String = "get(number) : KTEValue"

        })
        put("size", object : KTEFunction() {
            override fun invoke(model: KTEObject, invokedOn: KTEValue, parameters: List<ReferencedValue>): KTEValue {
                return IntValue(invokedOn.asNullableList(model)!!.collection.size)
            }

            override fun toString(): String = "size() : Int"
        })
        put("contains", object : KTEFunction() {
            override fun invoke(model: KTEObject, invokedOn: KTEValue, parameters: List<ReferencedValue>): KTEValue {
                return BooleanValue(invokedOn.asNullableList(model)!!.collection.containsAll(parameters))
            }

            override fun toString(): String = "contains(parameter) : Boolean"

        })
        put("indexOf", object : KTEFunction() {
            override fun invoke(model: KTEObject, invokedOn: KTEValue, parameters: List<ReferencedValue>): KTEValue {
                require(parameters.size == 1) {
                    "indexOf requires a single parameter"
                }
                return IntValue(invokedOn.asNullableList(model)!!.collection.indexOf(parameters[0]))
            }

            override fun toString(): String = "indexOf(parameter) : Int"

        })
        put("joinToString", object : KTEFunction() {
            override fun invoke(model: KTEObject, invokedOn: KTEValue, parameters: List<ReferencedValue>): KTEValue {
                val list = invokedOn.asNullableList(model)
                require(list != null) { "list is null" }
                val separator = parameters.getOrNull(0)?.asNullablePrimitive(model)?.value?.let { it as? String } ?: ","
                return StringValue(list.collection.joinToString(separator))
            }

            override fun toString(): String = "joinToString(separator : string?) : String"

        })
    }

}

