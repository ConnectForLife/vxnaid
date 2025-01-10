package com.idi.vaccinetracker.common.helpers

import com.idi.vaccinetracker.common.domain.entities.SizeUnit

val Int.b: Long get() = toLong()

val Int.kb: Long get() = SizeUnit.KB.toB(this.toDouble()).toLong()
val Int.mb: Long get() = SizeUnit.MB.toB(this.toDouble()).toLong()
val Int.gb: Long get() = SizeUnit.GB.toB(this.toDouble()).toLong()