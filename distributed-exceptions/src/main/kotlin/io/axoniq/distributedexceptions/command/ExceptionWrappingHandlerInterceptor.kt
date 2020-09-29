package io.axoniq.distributedexceptions.command

import io.axoniq.distributedexceptions.api.BusinessError
import org.axonframework.commandhandling.CommandExecutionException
import org.axonframework.commandhandling.CommandMessage
import org.axonframework.messaging.InterceptorChain
import org.axonframework.messaging.MessageHandlerInterceptor
import org.axonframework.messaging.unitofwork.UnitOfWork
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

/**
 * @author Stefan Andjelkovic
 */
open class ExceptionWrappingHandlerInterceptor : MessageHandlerInterceptor<CommandMessage<*>> {
    @Throws(Exception::class)
    override fun handle(unitOfWork: UnitOfWork<out CommandMessage<*>?>, interceptorChain: InterceptorChain): Any? {
        return try {
            interceptorChain.proceed()
        } catch (e: Exception) {
            throw CommandExecutionException("An exception has occurred during command execution", e, exceptionDetails(e))
        }
    }

    // Domain specific details can be returned in a couple of forms.
    // Shared, domain specific Error Codes as enumeration are a reasonable and common method.
    // For simplicity Enums/Error Codes could be returned directly without any wrapping in a domain Error object.
    private fun exceptionDetails(e: Exception): BusinessError {
        return when (e) {
            // alternatively this can be a centralised place to do a `when` check on exception's more specific instance
            // and populate the details accordingly (code, message, etc), instead of relying on the Domain Exception
            // to contain all the information and mapping logic
            is GiftCardException -> BusinessError(name = e.javaClass.name, code = e.errorCode.toString(), message = e.message)
                .also { log.info("Converted GiftCardException to $it") }
            else -> BusinessError(e.javaClass.name, message = e.message)
                .also { log.info("Converted CommandExecutionException to $it") }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
}
