package server

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

class ProxyServer(port: Int): Thread() {
    private val serverSocket: ServerSocket
    private val bufferSize: Int = 65536 //65536

    init {
        serverSocket = ServerSocket(port)
    }

    override fun run() {
        while (true) {
            val client = serverSocket.accept()
            Thread {
                listenClient(client)
            }.start()
        }
    }

    private fun listenClient(client: Socket) {
        client.use { client ->
            val browserInput = DataInputStream(client.getInputStream())
            val browserOutput = DataOutputStream(client.getOutputStream())
            val buffer = ByteArray(bufferSize)

            while (client.isConnected) {
                try {
                    val readCount = browserInput.read(buffer)
                    processRequest(buffer, browserInput, browserOutput, readCount)
                } catch (ex: Exception) {
                    client.close()
                    return
                }
            }
        }
    }

    private fun processRequest(request: ByteArray, browserInput: DataInputStream, browserOutput: DataOutputStream, length: Int) {
        try {
            val inputStr = String(request, Charsets.UTF_8)

            val rawHostName = parseHostName(inputStr)
            val hostName = rawHostName.split(":")

            val server: Socket = if (hostName.count() == 1) {
                Socket(hostName[0], 80)
            } else {
                Socket(hostName[0], hostName[1].toInt())
            }

            val serverInput = server.getInputStream()
            val serverOutput = server.getOutputStream()

            val deletedHostName = deleteHostName(inputStr).toByteArray()

            try {
                serverOutput.write(deletedHostName, 0, length)
                val answerBuffer = ByteArray(bufferSize)

                val readCount = serverInput.read(answerBuffer)
                if (readCount <= 0) {
                    return
                }

                val answer = String(answerBuffer, Charsets.UTF_8)

                val responseCode = parseResponseCode(answer)
                if (responseCode != null) {
                    println("${hostName[0]} $responseCode")
                }

                browserOutput.write(answerBuffer, 0, readCount)
                serverInput.copyTo(browserOutput)
            } catch (ex: Exception) {
//                println("Exception: ${ex.stackTrace}")
                ex.printStackTrace()
            } finally {
                serverOutput.flush()
                serverInput.close()
                serverOutput.close()
            }

        } catch (_: Exception) {

        } finally {
            browserOutput.flush()
            browserInput.close()
            browserOutput.close()
        }
    }

    private fun parseHostName(input: String): String {
        val strings = input.trim().split("\r\n")
        val hostLine = strings.firstOrNull {
            it.contains("Host")
        }

        return hostLine.orEmpty().replace("Host:", "").trim()
    }

    private fun parseResponseCode(input: String): String? {
        val strings = input.trim().split("\r\n")
        val header = strings[0]
        val response = header.split(Regex(" "), 2)
        if (response.count() < 2) {
//            throw SocketException("Wrong Response Code ${input}")
            return null
        }

        return response[1]
    }

    private fun deleteHostName(input: String): String {
        return input.replace(Regex("http://[a-z0-9а-яё:.]*"), "")
    }
}

fun main() {
    val port = 8080
    val thread = ProxyServer(port)
    thread.start()
}
