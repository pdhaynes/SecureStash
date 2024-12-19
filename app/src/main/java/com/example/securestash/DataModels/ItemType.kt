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
            return entries.find { it.typeBytes.contentEquals(bytes) }
        }

        fun fromEnum(itemType: ItemType): ByteArray {
            return itemType.typeBytes
        }

        fun fromName(itemName: String): ItemType? {
            return entries.find { it.name.equals(itemName) }
        }
    }
}