package io.axoniq.demo.tripplanner.api;

/**
 * Trigger event for the {@code EuropeTripPlannerWorkflow}.
 *
 * @param id    chat / trip identifier — the workflow id.
 * @param brief one-line description of what the user is looking for.
 */
public record StartTripPlanning(String id, String brief) {
}
