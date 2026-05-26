package io.axoniq.demo.tripplanner.api;

/**
 * Published by the workflow when the agent has delivered the itinerary.
 */
public record TripPlanReady(String id, String itinerary) {
}
