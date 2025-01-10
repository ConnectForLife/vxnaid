package com.idi.vaccinetracker.sync.p2p.common.models

import java.net.InetAddress

data class ReceivedData(val data: String, val fromAddress: InetAddress, val port: Int)