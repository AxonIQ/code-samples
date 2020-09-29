package io.axoniq.distributedexceptions

import io.axoniq.distributedexceptions.command.ExceptionWrappingHandlerInterceptor
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile

/**
 * @author Stefan Andjelkovic
 */
@SpringBootApplication
class DistributedExceptionsApplication {
    @Bean
    @Profile("command")
    fun exceptionWrappingHandlerInterceptor() = ExceptionWrappingHandlerInterceptor()
}

fun main(args: Array<String>) {
    runApplication<DistributedExceptionsApplication>(*args)
}
