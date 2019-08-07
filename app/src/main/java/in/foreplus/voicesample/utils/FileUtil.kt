package `in`.foreplus.voicesample.utils

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.Exception
import java.nio.channels.FileChannel

object FileUtil {

    fun copy(source: File, destination: File) {

        val cin = FileInputStream(source).channel
        val out = FileOutputStream(destination).channel

        try {
            cin.transferTo(0, cin.size(), out)
        } catch (e: Exception) {
            // post to log
        } finally {
            cin?.close()
            out?.close()
        }
    }

    fun decodeData(data: File): Pair<String, List<String>> {
        val lines = data.readLines()
        return Pair(lines[0], lines.subList(0, lines.size))
    }

}