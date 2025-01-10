package com.idi.vaccinetracker.setup.models

enum class P2pDeviceRole {
    SERVER, CLIENT;

    val isSender get() = this == SERVER
    val isReceiver get() = this == CLIENT
}