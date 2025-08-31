package com.buspljus

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.GZIPInputStream

class Raspakivanje {
    @Throws(IOException::class)
    fun gunzip(gzipFile: InputStream, outputFile: File) {
        GZIPInputStream(gzipFile).use { gzipInputStream ->
            FileOutputStream(outputFile).use { fileOutputStream ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var len: Int
                while (gzipInputStream.read(buffer).also { len = it } > 0) {
                    fileOutputStream.write(buffer, 0, len)
                }
            }
        }
    }
}