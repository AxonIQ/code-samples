package io.axoniq.distributedexceptions.command

/**
 * Domain specific exception
 * @author Stefan Andjelkovic
 */
open class GiftCardException(
        open val errorMessage: String,
        open val errorCode: GiftCardErrorCode
) : RuntimeException(errorMessage)

/**
 * @author Stefan Andjelkovic
 */
class NegativeOrZeroAmount(
        amount: Int,
        override val errorMessage: String = "amount $amount <=0"
) : GiftCardException(
        errorMessage = errorMessage,
        errorCode = GiftCardErrorCode.NEGATIVE_AMOUNT
)

/**
 * @author Stefan Andjelkovic
 */
class InsufficientFunds(
        override val errorMessage: String = "amount > remaining value"
) : GiftCardException(
        errorMessage = errorMessage,
        errorCode = GiftCardErrorCode.INSUFFICIENT_FUNDS
)

/**
 * Domain specific error codes
 * @author Stefan Andjelkovic
 */
enum class GiftCardErrorCode {
    NEGATIVE_AMOUNT, INSUFFICIENT_FUNDS
}
