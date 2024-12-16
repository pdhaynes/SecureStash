package com.example.securestash.DataModels

import android.util.Log

enum class ItemType(val typeBytes: ByteArray) {
    IMAGE("IMG".toByteArray()),
    DOCUMENT("DOC".toByteArray()),
    AUDIO("AUD".toByteArray()),
    VIDEO("VID".toByteArray()),
    DIRECTORY("DIR".toByteArray());

    companion object {
        fun fromByteArray(bytes: ByteArray): ItemType? {
            Log.d("MetadataBytes", bytes.contentToString())
//            for (item in entries) {
//                Log.d("Item", item.typeBytes.contentToString())
//            }
            return entries.find { it.typeBytes.contentEquals(bytes) }
        }

        fun fromEnum(itemType: ItemType): ByteArray {
            return itemType.typeBytes
        }
    }
}