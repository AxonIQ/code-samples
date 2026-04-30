package io.axoniq.demo.orderfulfillment.simulator;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class CustomerPool {

    public record Customer(String id, String name, String email) {}

    public static final List<Customer> ALL = List.of(
            new Customer("cust-001", "Alice Johnson",   "alice@example.com"),
            new Customer("cust-002", "Bob Martinez",    "bob@example.com"),
            new Customer("cust-003", "Carla Singh",     "carla@example.com"),
            new Customer("cust-004", "Daniel Park",     "daniel@example.com"),
            new Customer("cust-005", "Elena Petrova",   "elena@example.com"),
            new Customer("cust-006", "Felix Tanaka",    "felix@example.com"),
            new Customer("cust-007", "Grace O'Connor",  "grace@example.com"),
            new Customer("cust-008", "Hassan Reyes",    "hassan@example.com"),
            new Customer("cust-009", "Iris Berg",       "iris@example.com"),
            new Customer("cust-010", "Jonas Weber",     "jonas@example.com")
    );

    private CustomerPool() {}

    public static Customer random() {
        return ALL.get(ThreadLocalRandom.current().nextInt(ALL.size()));
    }

    public static double randomAmount() {
        var rnd = ThreadLocalRandom.current();
        return Math.round(rnd.nextDouble(15.0, 499.0) * 100.0) / 100.0;
    }
}
