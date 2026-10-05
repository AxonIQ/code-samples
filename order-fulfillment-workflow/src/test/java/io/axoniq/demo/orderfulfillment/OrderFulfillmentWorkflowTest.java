package io.axoniq.demo.orderfulfillment;

import io.axoniq.demo.orderfulfillment.api.OrderPlaced;
import io.axoniq.demo.orderfulfillment.api.PaymentConfirmed;
import io.axoniq.demo.orderfulfillment.service.InventoryService;
import io.axoniq.demo.orderfulfillment.service.NotificationService;
import io.axoniq.demo.orderfulfillment.service.PaymentService;
import io.axoniq.demo.orderfulfillment.service.ShippingService;
import io.axoniq.demo.orderfulfillment.workflow.OrderFulfillmentWorkflow;
import io.axoniq.framework.axonserver.connector.configuration.AxonServerConfigurationEnhancer;
import io.axoniq.framework.workflow.configuration.WorkflowModule;
import io.axoniq.framework.workflow.dsl.api.StepStatus;
import io.axoniq.framework.workflow.dsl.api.StepFailedException;
import io.axoniq.framework.workflow.dsl.api.WorkflowStatus;
import io.axoniq.framework.workflow.dsl.simple.SimpleWorkflowContext;
import io.axoniq.framework.workflow.runtime.test.fixture.GivenWhen;
import io.axoniq.framework.workflow.runtime.test.fixture.Then;
import io.axoniq.framework.workflow.runtime.test.fixture.WorkflowTestFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

class OrderFulfillmentWorkflowTest {

    private WorkflowTestFixture<GivenWhen.Phase, Then.Phase> fixture;

    @BeforeEach
    void setUp() {
        var workflow = new OrderFulfillmentWorkflow(new InventoryService(), new PaymentService(),
                                                    new ShippingService(), new NotificationService());
        var module = WorkflowModule.defaults("OrderFulfillment", SimpleWorkflowContext.class)
                                   .definition(d -> d.autodetected(c -> workflow));
        fixture = WorkflowTestFixture.of(module, configurer -> configurer.componentRegistry(
                registry -> registry.disableEnhancer(AxonServerConfigurationEnhancer.class)));
    }

    @AfterEach
    void tearDown() {
        if (fixture != null) {
            fixture.then().stop();
        }
    }

    @Test
    void confirmedPaymentShipsAndNotifiesCustomer() {
        fixture.given().publishEvent(order("happy"))
               .execute("reserveStock")
               .execute("initiatePayment");
        fixture.then().workflowNotFinished().waitingIn("awaitPayment")
               .noStep("shipOrder").noStep("notifyCustomer");

        fixture.when().publishEvent(new PaymentConfirmed("order-1", "txn-1"))
               .execute("shipOrder")
               .execute("notifyCustomer");

        fixture.then().workflowFinished(WorkflowStatus.COMPLETED)
               .step("awaitPayment", StepStatus.COMPLETED)
               .stepsPassed("reserveStock", "initiatePayment", "shipOrder", "notifyCustomer")
               .noStep("recordPaymentTimeout");
    }

    @Test
    void paymentArrivingBeforePaymentInitiationIsRetained() {
        fixture.given().publishEvent(order("happy"));
        fixture.then().executionExists().waitingIn("awaitPayment", "reserveStock");

        fixture.when().publishEvent(new PaymentConfirmed("order-1", "txn-early"))
               .execute("reserveStock");
        fixture.then().workflowNotFinished()
               .step("awaitPayment", StepStatus.COMPLETED)
               .waitingIn("initiatePayment")
               .noStep("shipOrder").noStep("notifyCustomer");

        fixture.when().execute("initiatePayment")
               .execute("shipOrder")
               .execute("notifyCustomer");

        fixture.then().workflowFinished(WorkflowStatus.COMPLETED)
               .step("awaitPayment", StepStatus.COMPLETED);
    }

    @Test
    void failedInitiationCancelsConfirmationWaitAndPreventsShipping() {
        fixture.given().publishEvent(order("happy")).execute("reserveStock");

        fixture.when().executeFailing("initiatePayment", new StepFailedException("Payment provider unavailable"));

        fixture.then().workflowFinished(WorkflowStatus.FAILED)
               .step("initiatePayment", StepStatus.FAILED)
               .step("awaitPayment", StepStatus.CANCELLED)
               .step("recordPaymentFailure", StepStatus.COMPLETED)
               .noStep("shipOrder").noStep("notifyCustomer");
    }

    @Test
    void earlyConfirmationCannotHideFailedInitiation() {
        fixture.given().publishEvent(order("happy")).execute("reserveStock");
        fixture.when().publishEvent(new PaymentConfirmed("order-1", "txn-early"));
        fixture.then().step("awaitPayment", StepStatus.COMPLETED).noStep("shipOrder");

        fixture.when().executeFailing("initiatePayment", new StepFailedException("Payment provider unavailable"));

        fixture.then().workflowFinished(WorkflowStatus.FAILED)
               .step("awaitPayment", StepStatus.COMPLETED)
               .step("initiatePayment", StepStatus.FAILED)
               .step("recordPaymentFailure", StepStatus.COMPLETED)
               .noStep("shipOrder").noStep("notifyCustomer");
    }

    @Test
    void confirmationTimeoutCancelsPendingInitiation() {
        fixture.given().publishEvent(order("payment-timeout")).execute("reserveStock");

        fixture.when().timePasses(Duration.ofSeconds(5));

        fixture.then().workflowFinished(WorkflowStatus.FAILED)
               .step("awaitPayment", StepStatus.TIMED_OUT)
               .step("initiatePayment", StepStatus.CANCELLED)
               .step("recordPaymentTimeout", StepStatus.COMPLETED)
               .noStep("shipOrder").noStep("notifyCustomer");
    }

    @Test
    void initiationTimeoutPreventsShippingEvenAfterConfirmation() {
        fixture.given().publishEvent(order("happy")).execute("reserveStock");
        fixture.when().publishEvent(new PaymentConfirmed("order-1", "txn-early"));
        fixture.then().step("awaitPayment", StepStatus.COMPLETED).noStep("shipOrder");

        fixture.when().timePasses(Duration.ofSeconds(31));

        fixture.then().workflowFinished(WorkflowStatus.FAILED)
               .step("awaitPayment", StepStatus.COMPLETED)
               .step("initiatePayment", StepStatus.TIMED_OUT)
               .step("recordPaymentFailure", StepStatus.COMPLETED)
               .noStep("shipOrder").noStep("notifyCustomer");
    }

    @Test
    void paymentForAnotherOrderDoesNotPreventTimeout() {
        fixture.given().publishEvent(order("payment-timeout"))
               .execute("reserveStock")
               .execute("initiatePayment");
        fixture.then().executionExists().waitingIn("awaitPayment");

        fixture.when().publishEvent(new PaymentConfirmed("another-order", "txn-unrelated"))
               .timePasses(Duration.ofSeconds(5));

        fixture.then().workflowFinished(WorkflowStatus.FAILED)
               .step("awaitPayment", StepStatus.TIMED_OUT)
               .step("recordPaymentTimeout", StepStatus.COMPLETED)
               .noStep("shipOrder")
               .noStep("notifyCustomer");
    }

    @Test
    void unavailableStockCancelsPaymentWaitAndFailsOrder() {
        fixture.given().publishEvent(order("out-of-stock"));

        fixture.when().execute("reserveStock");

        fixture.then().workflowFinished(WorkflowStatus.FAILED)
               .step("awaitPayment", StepStatus.CANCELLED)
               .step("recordOutOfStock", StepStatus.COMPLETED)
               .noStep("initiatePayment")
               .noStep("shipOrder");
    }

    private static OrderPlaced order(String scenario) {
        return new OrderPlaced("order-1", "customer-1", "customer@example.com", 99.95,
                               "New York", 40.7128, -74.0060,
                               "Boston", 42.3601, -71.0589, scenario);
    }
}
