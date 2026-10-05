package io.axoniq.demo.orderfulfillment.config;

import io.axoniq.demo.orderfulfillment.history.JdbcWorkflowHistoryRepository;
import io.axoniq.demo.orderfulfillment.history.WorkflowHistoryUpdates;
import io.axoniq.demo.orderfulfillment.workflow.OrderFulfillmentWorkflow;
import io.axoniq.framework.workflow.configuration.WorkflowModule;
import io.axoniq.framework.workflow.dsl.simple.SimpleWorkflowContext;
import org.axonframework.conversion.GeneralConverter;
import org.axonframework.messaging.core.unitofwork.UnitOfWorkFactory;
import org.axonframework.messaging.core.unitofwork.transaction.jdbc.JdbcTransactionalExecutorProvider;
import org.axonframework.messaging.eventhandling.conversion.EventConverter;
import org.axonframework.messaging.eventhandling.processing.streaming.token.store.jdbc.GenericTokenTableFactory;
import org.axonframework.messaging.eventhandling.processing.streaming.token.store.jdbc.JdbcTokenStore;
import org.axonframework.messaging.eventhandling.processing.streaming.token.store.jdbc.JdbcTokenStoreConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/** The two framework extension points demonstrated by this sample. */
@Configuration(proxyBeanMethods = false)
public class WorkflowHistoryConfig {

    @Bean
    JdbcWorkflowHistoryRepository workflowHistoryRepository(DataSource dataSource, EventConverter converter,
                                                            WorkflowHistoryUpdates updates) {
        return new JdbcWorkflowHistoryRepository(dataSource, converter, updates::publish);
    }

    @Bean
    WorkflowModule<SimpleWorkflowContext> orderFulfillmentModule(
            OrderFulfillmentWorkflow workflow, JdbcWorkflowHistoryRepository history,
            DataSource dataSource, GeneralConverter converter) {
        // Only the history processor uses durable tokens. The map projection and demo
        // simulators retain their existing in-memory token store.
        var historyTokens = new JdbcTokenStore(new JdbcTransactionalExecutorProvider(dataSource),
                                              converter, JdbcTokenStoreConfiguration.DEFAULT);
        historyTokens.createSchema(GenericTokenTableFactory.INSTANCE);

        return WorkflowModule.defaults("OrderFulfillment", SimpleWorkflowContext.class)
                .definition(definitions -> definitions.autodetected(configuration -> workflow))
                .withHistory(configuration -> {
                    history.initialize(configuration.getComponent(UnitOfWorkFactory.class));
                    return history;
                })
                .historyProcessorConfiguration(processor -> processor
                        .tokenStore(historyTokens)
                        .initialSegmentCount(1)
                        .batchSize(1)
                        .withInterceptor(history));
    }
}
