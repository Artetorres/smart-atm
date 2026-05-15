package com.smartatm

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SmartAtmApplication

fun main(args: Array<String>) {
    runApplication<SmartAtmApplication>(*args)
}
