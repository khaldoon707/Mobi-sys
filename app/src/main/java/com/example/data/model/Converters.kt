package com.example.data.model

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    @TypeConverter
    fun fromOrderItemList(value: String): List<OrderItem>? {
        if (value.isBlank()) return emptyList()
        val type = Types.newParameterizedType(List::class.java, OrderItem::class.java)
        val adapter = moshi.adapter<List<OrderItem>>(type)
        return adapter.fromJson(value)
    }

    @TypeConverter
    fun toOrderItemList(list: List<OrderItem>?): String {
        val type = Types.newParameterizedType(List::class.java, OrderItem::class.java)
        val adapter = moshi.adapter<List<OrderItem>>(type)
        return adapter.toJson(list ?: emptyList())
    }

    @TypeConverter
    fun fromPOItemList(value: String): List<POItem>? {
        if (value.isBlank()) return emptyList()
        val type = Types.newParameterizedType(List::class.java, POItem::class.java)
        val adapter = moshi.adapter<List<POItem>>(type)
        return adapter.fromJson(value)
    }

    @TypeConverter
    fun toPOItemList(list: List<POItem>?): String {
        val type = Types.newParameterizedType(List::class.java, POItem::class.java)
        val adapter = moshi.adapter<List<POItem>>(type)
        return adapter.toJson(list ?: emptyList())
    }
}
