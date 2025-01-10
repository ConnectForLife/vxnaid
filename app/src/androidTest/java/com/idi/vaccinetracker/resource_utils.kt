package com.idi.vaccinetracker

import java.io.InputStream

fun readResource(fileName: String): InputStream {
    val classloader =
        Thread.currentThread().contextClassLoader!!
    return classloader.getResourceAsStream(fileName)
}