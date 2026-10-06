package io.axoniq.demo.orderfulfillment;

import com.zaxxer.hikari.HikariDataSource;
import io.axoniq.demo.orderfulfillment.history.JdbcWorkflowHistoryRepository;
import io.axoniq.framework.workflow.dsl.api.StepRetryInfo;
import io.axoniq.framework.workflow.dsl.api.StepStatus;
import io.axoniq.framework.workflow.dsl.api.WorkflowError;
import io.axoniq.framework.workflow.dsl.api.WorkflowStatus;
import io.axoniq.framework.workflow.history.inmemory.WorkflowHistoryProjector;
import io.axoniq.framework.workflow.query.api.WorkflowStateQuery;
import io.axoniq.framework.workflow.runtime.execution.payload.PayloadReducerRegistry;
import io.axoniq.framework.workflow.runtime.util.MetadataUtils;
import org.axonframework.conversion.jackson.JacksonConverter;
import org.axonframework.messaging.core.ApplicationContext;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.Metadata;
import org.axonframework.messaging.core.VersionedType;
import org.axonframework.messaging.core.unitofwork.SimpleUnitOfWorkFactory;
import org.axonframework.messaging.eventhandling.EventMessage;
import org.axonframework.messaging.eventhandling.GenericEventMessage;
import org.axonframework.messaging.eventhandling.conversion.DelegatingEventConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdbcWorkflowHistoryRepositoryTest {

    @TempDir
    Path directory;

    private final DelegatingEventConverter converter = new DelegatingEventConverter(new JacksonConverter());
    private final PayloadReducerRegistry reducers = new PayloadReducerRegistry();
    private final SimpleUnitOfWorkFactory units = new SimpleUnitOfWorkFactory(new ApplicationContext() {
        @Override
        public <C> C component(Class<C> type, String name) {
            if (type == PayloadReducerRegistry.class) return type.cast(reducers);
            throw new IllegalArgumentException("Unexpected component: " + type);
        }
    });

    @Test
    void fileHistoryReopensAndContinuesAnUnfinishedWorkflowWithoutDuplicatingEvents() {
        var started = started("order-1");
        var waiting = event(Map.of("orderId", "order-1"), MetadataUtils.create("order-1", "awaitPayment", StepStatus.STARTED));
        try (var database = database()) {
            var history = repository(database);
            project(history, started);
            project(history, waiting);
            assertThat(history.detail("order-1")).isPresent();
        }

        // A fresh connection pool and repository; H2 is closed and reopened from disk.
        try (var database = database()) {
            var history = repository(database);
            var restored = history.findById("order-1").join().orElseThrow().state();
            assertThat(restored.workflowStatus()).isEqualTo(WorkflowStatus.STARTED);
            assertThat(restored.payload()).containsEntry("customerId", "alice");
            assertThat(restored.getStep("awaitPayment").status()).isEqualTo(StepStatus.STARTED);

            var paid = event(Map.of("transactionId", "txn-1"), MetadataUtils.create("order-1", "awaitPayment", StepStatus.COMPLETED));
            var completed = event(Map.of(), MetadataUtils.create("order-1", WorkflowStatus.COMPLETED));
            project(history, paid);
            project(history, completed);
            // Simulate tokens lagging the committed history after a crash or token reset.
            project(history, started);
            project(history, waiting);
            project(history, paid);
            project(history, completed);

            var state = history.findById("order-1").join().orElseThrow().state();
            assertThat(state.workflowStatus()).isEqualTo(WorkflowStatus.COMPLETED);
            assertThat(state.getStep("awaitPayment").status()).isEqualTo(StepStatus.COMPLETED);
            assertThat(state.getStep("awaitPayment").result()).isEqualTo(Map.of("transactionId", "txn-1"));
            assertThat(history.timeline("order-1", 0, 100)).hasSize(4);
            assertThat(history.findAll(WorkflowStateQuery.byPayloadValue("customerId", "alice")
                                              .stepStatus("awaitPayment", StepStatus.COMPLETED)).join()).hasSize(1);
            assertThat(history.findAll(WorkflowStateQuery.byWorkflowStatus(WorkflowStatus.STARTED)).join()).isEmpty();
            assertThat(history.list("order-1", "COMPLETED", 10, 0).get("total")).isEqualTo(1L);
            var first = history.timeline("order-1", 0, 1).getFirst();
            assertThat(history.timeline("order-1", (Long) first.get("sequence"), 100)).hasSize(3);
        }
        try (var database = database()) {
            var history = repository(database);
            assertThat(history.findById("order-1").join().orElseThrow().state().workflowStatus())
                    .isEqualTo(WorkflowStatus.COMPLETED);
            assertThat(history.timeline("order-1", 0, 100)).hasSize(4);
        }
    }

    @Test
    void errorsRetriesAndVersionMigrationsSurviveReload() {
        try (var database = database()) {
            var history = repository(database);
            project(history, started("failed-order"));
            var error = WorkflowError.from(new IllegalStateException("Gateway unavailable"));
            project(history, event(new StepRetryInfo(1, 3, error),
                                   MetadataUtils.create("failed-order", "payment", StepStatus.RETRYING)));
            var retry = history.findById("failed-order").join().orElseThrow().state().getStep("payment");
            assertThat(retry.result()).isInstanceOf(StepRetryInfo.class);
            assertThat(retry.error()).hasMessageContaining("Gateway unavailable");
            project(history, event(Map.of(), MetadataUtils.createVersionMigrationStep("failed-order", "payment-v2", "2.0.0")));
            project(history, event(error, MetadataUtils.create("failed-order", "payment", StepStatus.FAILED)));
            project(history, event(error, MetadataUtils.create("failed-order", WorkflowStatus.FAILED)));
        }
        try (var database = database()) {
            var history = repository(database);
            var state = history.findById("failed-order").join().orElseThrow().state();
            assertThat(state.workflowStatus()).isEqualTo(WorkflowStatus.FAILED);
            assertThat(state.getStep("payment").error()).hasMessageContaining("Gateway unavailable");
            assertThat(state.versionMigrations()).containsEntry("payment-v2", "2.0.0");
            assertThat(history.timeline("failed-order", 0, 100)).hasSize(5);
            history.clear();
            assertThat(history.findById("failed-order").join()).isEmpty();
            assertThat(history.timeline("failed-order", 0, 100)).isEmpty();
        }
    }

    @Test
    void failedProjectionRollsBackBothTheEventAndStateAndCanBeRetried() {
        try (var database = database()) {
            var notifications = new ArrayList<String>();
            var history = new JdbcWorkflowHistoryRepository(database, converter, notifications::add);
            history.initialize(units);
            var event = started("rollback-order");
            var projector = new WorkflowHistoryProjector(history);
            assertThatThrownBy(() -> units.create().executeWithResult(context -> {
                history.interceptOnHandle(event, context, (message, pc) -> {
                    projector.handle(message, pc);
                    assertThat(notifications).isEmpty(); // No notification before commit.
                    return MessageStream.failed(new IllegalStateException("Simulated failure after save"));
                });
                return CompletableFuture.completedFuture(null);
            }).join()).hasRootCauseMessage("Simulated failure after save");
            assertThat(history.detail("rollback-order")).isEmpty();
            assertThat(history.timeline("rollback-order", 0, 100)).isEmpty();
            assertThat(notifications).isEmpty();
            project(history, event);
            assertThat(history.findById("rollback-order").join()).isPresent();
            assertThat(history.timeline("rollback-order", 0, 100)).hasSize(1);
            assertThat(notifications).containsExactly("rollback-order");
            project(history, event);
            assertThat(notifications).containsExactly("rollback-order"); // No duplicate notification.
        }
    }

    private HikariDataSource database() {
        var source = new HikariDataSource();
        source.setJdbcUrl("jdbc:h2:file:" + directory.resolve("history") + ";DB_CLOSE_ON_EXIT=FALSE");
        source.setUsername("sa");
        source.setMaximumPoolSize(2);
        return source;
    }

    private JdbcWorkflowHistoryRepository repository(HikariDataSource source) {
        var history = new JdbcWorkflowHistoryRepository(source, converter);
        history.initialize(units);
        return history;
    }

    private void project(JdbcWorkflowHistoryRepository history, EventMessage event) {
        units.create().executeWithResult(context -> {
            history.interceptOnHandle(event, context, new WorkflowHistoryProjector(history)::handle);
            return CompletableFuture.completedFuture(null);
        }).join();
    }

    private EventMessage started(String id) {
        var metadata = MetadataUtils.withWorkflowDefinitionId(MetadataUtils.create(id, WorkflowStatus.STARTED),
                VersionedType.of("OrderFulfillmentWorkflow", "1.0.0"))
                .and(MetadataUtils.METADATA_KEY_MODIFY_PAYLOAD, "local_only");
        return event(Map.of("orderId", id, "customerId", "alice"), metadata);
    }

    private EventMessage event(Object payload, Metadata metadata) {
        return new GenericEventMessage(UUID.randomUUID().toString(), new MessageType("test.HistoryEvent", "1.0.0"),
                                       payload, metadata, Instant.now()).withConverter(converter);
    }
}
