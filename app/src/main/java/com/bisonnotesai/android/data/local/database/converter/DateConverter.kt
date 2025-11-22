package com.bisonnotesai.android.data.local.database.converter

import androidx.room.TypeConverter
import java.util.Date

/**
 * Type converter for Date objects in Room database
 * Converts Date to Long (timestamp) and vice versa
 */
class DateConverter {

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
