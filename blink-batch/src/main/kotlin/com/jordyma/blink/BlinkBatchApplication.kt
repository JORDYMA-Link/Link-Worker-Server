package com.jordyma.blink

import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import kotlin.system.exitProcess

@SpringBootApplication(exclude = [RedisAutoConfiguration::class])
class BlinkBatchApplication
inline fun <reified T> T.logger() = LoggerFactory.getLogger(T::class.java)!!

fun main(args: Array<String>) {
    val name: String? = System.getenv("job.name")
    val exit = SpringApplication.exit(
        SpringApplicationBuilder(BlinkBatchApplication::class.java).run(*args),
    )
    exitProcess(exit)
}
