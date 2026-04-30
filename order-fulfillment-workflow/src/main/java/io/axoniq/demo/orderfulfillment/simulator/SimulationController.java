package io.axoniq.demo.orderfulfillment.simulator;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/simulate")
public class SimulationController {

    private final Simulator simulator;

    public SimulationController(Simulator simulator) {
        this.simulator = simulator;
    }

    @PostMapping("/single")
    public Map<String, String> single(@RequestParam(value = "scenario", required = false) String scenario) {
        var orderId = simulator.placeRandom(normalize(scenario));
        return Map.of("orderId", orderId);
    }

    @PostMapping("/burst")
    public Map<String, Object> burst(@RequestParam(value = "count", defaultValue = "10") int count,
                                     @RequestParam(value = "scenario", required = false) String scenario) {
        simulator.burst(count, normalize(scenario));
        return Map.of("queued", count, "scenario", String.valueOf(normalize(scenario)));
    }

    @PostMapping("/scenario")
    public Map<String, String> scenario(@RequestParam("type") String type) {
        var orderId = simulator.placeRandom(normalize(type));
        return Map.of("orderId", orderId, "scenario", type);
    }

    private static String normalize(String scenario) {
        if (scenario == null || scenario.isBlank() || "happy".equalsIgnoreCase(scenario)) {
            return "happy";
        }
        return scenario;
    }
}
