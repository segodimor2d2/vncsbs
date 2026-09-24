package com.rec.vncsbs.vnc

class VncClient {

    fun connect(
        host: String,
        port: Int
    ) {
        println("VncClient: connect $host:$port")
    }

    fun disconnect() {
        println("VncClient: disconnect")
    }
}
