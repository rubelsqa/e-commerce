package simulations;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class GetApiSimulation extends Simulation {

    private static final String BASE_URL = System.getProperty("baseUrl", "https://httpbin.org");
    private static final String API_PATH = System.getProperty("apiPath", "/get");
    //private static final double TARGET_RPS = 500.0 / 60.0;
    private static final double TARGET_RPS = 500; // per Sec

    HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .userAgentHeader("Gatling Performance Test")
            .shareConnections(); // better for server-to-server style traffic
    ChainBuilder smokeRequest =
            exec(
                    http("Stage 1 - Smoke")
                            .get(API_PATH)
                            .check(status().in(200, 201, 202, 204))
            );

    ChainBuilder rampRequest =
            exec(
                    http("Stage 3 - Ramp")
                            .get(API_PATH)
                            .check(status().in(200, 201, 202, 204))
            );

    ChainBuilder steadyRequest =
            exec(
                    http("Stage 4 - Steady (500 Per min)")
                            .get(API_PATH)
                            .check(status().in(200, 201, 202, 204))
            );

    ScenarioBuilder smokeScenario = scenario("Smoke Scenario").exec(smokeRequest);
    ScenarioBuilder rampScenario = scenario("Ramp Scenario").exec(rampRequest);
    ScenarioBuilder steadyScenario = scenario("Steady Scenario").exec(steadyRequest);

    {
        setUp(
                smokeScenario.injectOpen(
                        nothingFor(Duration.ofSeconds(5)),
                        constantUsersPerSec(1).during(Duration.ofSeconds(5))
                ),
                rampScenario.injectOpen(
                        nothingFor(Duration.ofSeconds(10)),
                        rampUsersPerSec(1).to(3).during(Duration.ofSeconds(5)),
                        rampUsersPerSec(3).to(5).during(Duration.ofSeconds(10)),
                        rampUsersPerSec(5).to(TARGET_RPS).during(Duration.ofSeconds(15))
                ),
                steadyScenario.injectOpen(
                        nothingFor(Duration.ofSeconds(30)),
                        constantUsersPerSec(TARGET_RPS).during(Duration.ofSeconds(30))
                )
        )
                .protocols(httpProtocol)
                .assertions(
                        global().responseTime().percentile3().lt(1000),
                        global().responseTime().percentile4().lt(5000)
                );
    }
}