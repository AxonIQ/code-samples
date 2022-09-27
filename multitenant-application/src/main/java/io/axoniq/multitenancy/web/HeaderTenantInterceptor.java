package io.axoniq.multitenancy.web;

import org.axonframework.messaging.Message;
import org.axonframework.messaging.MessageDispatchInterceptor;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import static org.axonframework.extensions.multitenancy.autoconfig.TenantConfiguration.TENANT_CORRELATION_KEY;

/**
 * @author Stefan Dragisic
 */
public class HeaderTenantInterceptor<T extends Message<?>>
        implements MessageDispatchInterceptor<T> {

    @Override
    public T handle(T message) {
        if( RequestContextHolder.getRequestAttributes() == null ) return message;
        //read tenant attribute from header - if any
        String tenant = (((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest()).getHeader(
                "Tenant-Id");
        if (tenant != null) {
            //set tenant attribute to every message that will be dispatched using REST endpoint
            return (T) message.andMetaData(Collections.singletonMap(TENANT_CORRELATION_KEY, tenant));
        }
        return message;
    }

    @Override
    public BiFunction<Integer, T, T> handle(List<? extends T> messages) {
        return null;
    }

};
