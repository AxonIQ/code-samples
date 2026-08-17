package io.axoniq.distributedexceptions.command;

import io.axoniq.distributedexceptions.api.GiftCardBusinessError;
import io.axoniq.distributedexceptions.api.GiftCardBusinessErrorCode;
import org.axonframework.messaging.commandhandling.CommandExecutionException;
import org.axonframework.messaging.commandhandling.CommandMessage;
import org.axonframework.messaging.core.MessageHandlerInterceptor;
import org.axonframework.messaging.core.MessageHandlerInterceptorChain;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.core.unitofwork.ProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;

@Component
@Profile("command")
public class ExceptionWrappingHandlerInterceptor implements MessageHandlerInterceptor<CommandMessage> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public MessageStream<?> interceptOnHandle(CommandMessage message,
                                              ProcessingContext context,
                                              MessageHandlerInterceptorChain<CommandMessage> chain) {
        return chain.proceed(message, context)
                    .onErrorContinue(throwable -> MessageStream.failed(
                            new CommandExecutionException(
                                    "An exception has occurred during command execution",
                                    throwable,
                                    exceptionDetails(throwable)
                            )
                    ));
    }

    // Domain specific details can be returned in a couple of forms.
    // Shared, domain specific Error Codes as enumeration are a reasonable and common method.
    // For simplicity Enums/Error Codes could be returned directly without any wrapping in a domain Error object.
    private GiftCardBusinessError exceptionDetails(Throwable throwable) {

        // alternatively this can be a centralised place to do a check on exception's more specific instance
        // and populate the details accordingly (code, message, etc), instead of relying on the Domain Exception
        // to contain all the information and mapping logic
        if (throwable instanceof GiftCardException gce) {
            GiftCardBusinessError businessError = new GiftCardBusinessError(
                    gce.getClass().getName(), gce.getErrorCode(), gce.getErrorMessage()
            );
            logger.info("Converted GiftCardException to " + businessError);
            return businessError;
        } else {
            GiftCardBusinessError businessError = new GiftCardBusinessError(
                    throwable.getClass().getName(), GiftCardBusinessErrorCode.UNKNOWN, throwable.getMessage()
            );
            logger.info("Converted CommandExecutionException to " + businessError);
            return businessError;
        }
    }
}
