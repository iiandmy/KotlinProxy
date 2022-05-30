package server

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket

fun main() {
    val serverName = "localhost"
    val port = 8080

    println("Connecting to $serverName in port $port")
    val client = Socket(serverName, port)

    val outputToServer = client.getOutputStream()
    val output = DataOutputStream(outputToServer)

    output.writeUTF("Hello from ${client.localSocketAddress}")
    val inputFromServer = client.getInputStream()
    val input = DataInputStream(inputFromServer)

    println("Server answered: ${input.readUTF()}")
    client.close()
}