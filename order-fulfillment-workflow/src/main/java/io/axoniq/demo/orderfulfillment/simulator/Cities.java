package io.axoniq.demo.orderfulfillment.simulator;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class Cities {

    public record City(String name, double lat, double lng) {}

    public static final List<City> ALL = List.of(
            new City("New York",     40.7128, -74.0060),
            new City("Los Angeles",  34.0522, -118.2437),
            new City("Chicago",      41.8781, -87.6298),
            new City("Houston",      29.7604, -95.3698),
            new City("Phoenix",      33.4484, -112.0740),
            new City("Philadelphia", 39.9526, -75.1652),
            new City("San Antonio",  29.4241, -98.4936),
            new City("San Diego",    32.7157, -117.1611),
            new City("Dallas",       32.7767, -96.7970),
            new City("Austin",       30.2672, -97.7431),
            new City("Boston",       42.3601, -71.0589),
            new City("Seattle",      47.6062, -122.3321),
            new City("Denver",       39.7392, -104.9903),
            new City("Miami",        25.7617, -80.1918),
            new City("Atlanta",      33.7490, -84.3880),
            new City("Portland",     45.5152, -122.6784),
            new City("Minneapolis",  44.9778, -93.2650),
            new City("St. Louis",    38.6270, -90.1994),
            new City("Nashville",    36.1627, -86.7816),
            new City("New Orleans",  29.9511, -90.0715)
    );

    private Cities() {}

    public static City random() {
        return ALL.get(ThreadLocalRandom.current().nextInt(ALL.size()));
    }

    public static City[] randomPair() {
        var rnd = ThreadLocalRandom.current();
        var origin = ALL.get(rnd.nextInt(ALL.size()));
        City destination;
        do {
            destination = ALL.get(rnd.nextInt(ALL.size()));
        } while (destination == origin);
        return new City[]{origin, destination};
    }
}
