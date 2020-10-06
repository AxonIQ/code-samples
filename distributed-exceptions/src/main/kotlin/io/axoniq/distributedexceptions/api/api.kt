package io.axoniq.distributedexceptions.api

import org.axonframework.modelling.command.TargetAggregateIdentifier


data class IssueCmd(@TargetAggregateIdentifier val id: String, val amount: Int)
data class IssuedEvt(val id: String, val amount: Int)
data class RedeemCmd(@TargetAggregateIdentifier val id: String, val amount: Int)
data class RedeemedEvt(val id: String, val amount: Int)
data class CancelCmd(@TargetAggregateIdentifier val id: String)
data class CancelEvt(val id: String)

data class GiftCardBusinessError(
    val name: String,
    val code: GiftCardBusinessErrorCode? = null,
    val message: String? = null
)

/**
 * Public domain specific error codes
 * @author Stefan Andjelkovic
 */
enum class GiftCardBusinessErrorCode {
    NEGATIVE_AMOUNT, INSUFFICIENT_FUNDS, UNKNOWN
}
