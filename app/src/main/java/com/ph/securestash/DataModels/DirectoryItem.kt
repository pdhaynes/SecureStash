package com.ph.securestash.DataModels

class DirectoryItem(
    var name: String,
    var path: String,
    var type: ItemType,
    var locked: Boolean = true
)