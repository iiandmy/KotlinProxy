package logger

import java.io.File
import java.io.FileWriter

class FileLogger(fileName: String): Logger {
    private val pathToLogs: String = "./out/"
    private val logFile: File

    init {
        File("$pathToLogs$fileName").delete()
        logFile = File("$pathToLogs$fileName")
    }

    override fun log(input: String) {
        FileWriter(logFile, true).use { writer ->
            writer.write("$input\n")
        }
    }
}