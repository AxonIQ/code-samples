package io.axoniq.distributedexceptions.command

import io.axoniq.distributedexceptions.api.*
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.spring.stereotype.Aggregate
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import java.lang.invoke.MethodHandles

/**
 * @author Stefan Andjelkovic
 */
@Aggregate
@Profile("command")
class GiftCard {
    @AggregateIdentifier
    private var id: String? = null
    private var remainingValue = 0

    constructor() {
        log.debug("empty constructor invoked")
    }

    @CommandHandler
    constructor(cmd: IssueCmd) {
        log.debug("handling {}", cmd)
        requirePositiveAmount(cmd.amount)
        AggregateLifecycle.apply(IssuedEvt(cmd.id, cmd.amount))
    }

    @CommandHandler
    fun handle(cmd: RedeemCmd) {
        log.debug("handling {}", cmd)
        requirePositiveAmount(cmd.amount)
        checkSufficientFunds(cmd)
        AggregateLifecycle.apply(RedeemedEvt(id!!, cmd.amount))
    }

    private fun checkSufficientFunds(cmd: RedeemCmd) {
        if (cmd.amount > remainingValue) {
            throw InsufficientFunds()
        }
    }

    private fun requirePositiveAmount(amount: Int) {
        if (amount <= 0) {
            throw NegativeOrZeroAmount(amount)
        }
    }

    @CommandHandler
    fun handle(cmd: CancelCmd) {
        log.debug("handling {}", cmd)
        AggregateLifecycle.apply(CancelEvt(id!!))
    }

    @EventSourcingHandler
    fun on(evt: IssuedEvt) {
        log.debug("applying {}", evt)
        id = evt.id
        remainingValue = evt.amount
        log.debug("new remaining value: {}", remainingValue)
    }

    @EventSourcingHandler
    fun on(evt: RedeemedEvt) {
        log.debug("applying {}", evt)
        remainingValue -= evt.amount
        log.debug("new remaining value: {}", remainingValue)
    }

    @EventSourcingHandler
    fun on(evt: CancelEvt) {
        log.debug("applying {}", evt)
        remainingValue = 0
        log.debug("new remaining value: {}", remainingValue)
    }

    companion object {
        private val log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
}
