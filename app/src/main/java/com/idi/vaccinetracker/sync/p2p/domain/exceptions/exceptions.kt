package com.idi.vaccinetracker.sync.p2p.domain.exceptions

import java.io.IOException

class SendMessageException(override val cause: Exception) : IOException()