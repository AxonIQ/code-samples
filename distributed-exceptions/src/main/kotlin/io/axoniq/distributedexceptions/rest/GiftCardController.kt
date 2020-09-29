package io.axoniq.distributedexceptions.rest

import io.axoniq.distributedexceptions.api.BusinessError
import io.axoniq.distributedexceptions.api.IssueCmd
import io.axoniq.distributedexceptions.api.RedeemCmd
import org.axonframework.commandhandling.CommandExecutionException
import org.axonframework.commandhandling.gateway.CommandGateway
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.lang.invoke.MethodHandles
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * @author Stefan Andjelkovic
 */
@Profile("rest")
@RestController
@RequestMapping("/giftcard")
class GiftCardController(
    val commandGateway: CommandGateway
) {
    @PostMapping
    fun issueNewGiftCard(@RequestBody request: IssueNewGiftCardDto): CompletableFuture<ResponseEntity<String>> =
        commandGateway.send<String>(IssueCmd(UUID.randomUUID().toString(), request.amount))
            .thenApply { ResponseEntity.ok(it) }
            .exceptionally { e ->
                logException(e)
                val errorResponse = getErrorResponseMessage(e.cause)
                ResponseEntity.badRequest().body(errorResponse)
            }

    @PutMapping("/{id}")
    fun redeem(@PathVariable id: String, @RequestBody dto: RedeemDto): CompletableFuture<ResponseEntity<String>> =
        commandGateway.send<Any>(RedeemCmd(id, dto.amount))
            .thenApply { ResponseEntity.ok("") }
            .exceptionally { e ->
                logException(e)
                val errorResponse = getErrorResponseMessage(e.cause)
                // This is also the place where this error can be handled, the command retried
                // or a different logical flow can be attempted
                ResponseEntity.badRequest().body(errorResponse)
            }

    private fun logException(e: Throwable) {
        log.info("exception is ${e.javaClass.simpleName} and cause is ${e.cause?.javaClass?.simpleName}")
        log.error(e.message)
    }

    private fun getErrorResponseMessage(e: Throwable?): String? {
        return when (e) {
            // or HandlerExecutionException for more generic exception handling
            is CommandExecutionException -> e.getDetails<BusinessError>()
                .map {
                    log.debug("Received BusinessError with data: $it")
                    log.error("Unable to create GiftCard due to validation constrains. Reason: $it")
                    "${it.code}: ${it.name}"
                }
                .orElseGet {
                    log.error("Unable to create GiftCard due to $e")
                    "${e.message}"
                }
            else -> {
                log.error("Unable to create GiftCard due to unknown generic exception $e")
                "${e?.message}"
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
}
