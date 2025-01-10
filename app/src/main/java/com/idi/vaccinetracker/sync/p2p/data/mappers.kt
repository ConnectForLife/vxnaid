package com.idi.vaccinetracker.sync.p2p.data

import com.idi.vaccinetracker.sync.p2p.common.models.ReceiverInfo
import com.idi.vaccinetracker.sync.p2p.data.receiver.SecureReceiver

fun SecureReceiver.toReceiverInfo() = ReceiverInfo(port = localPort)