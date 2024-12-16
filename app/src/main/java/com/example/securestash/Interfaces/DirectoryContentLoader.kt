package com.example.securestash.Interfaces

import java.io.File

interface DirectoryContentLoader {
    fun loadDirectoryContents(selectedDirectory: File?)
}
