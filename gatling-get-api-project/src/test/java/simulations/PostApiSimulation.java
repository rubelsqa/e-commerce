package simulations;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import utils.RequestPayloadBuilder;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class PostApiSimulation extends Simulation {

    private static final String BASE_URL = System.getProperty("baseUrl", "https://httpbin.org");
    private static final String API_PATH = System.getProperty("apiPath", "/post");
    private static final double TARGET_RPS = 10;

    Iterator<Map<String, Object>> rampFeeder =
            RequestPayloadBuilder.buildFeederFromCsv("src/test/resources/testdata.csv");

    Iterator<Map<String, Object>> steadyFeeder =
            RequestPayloadBuilder.buildFeederFromCsv("src/test/resources/testdata.csv");

    HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .userAgentHeader("Gatling Performance Test")
            .shareConnections();

    ChainBuilder smokeRequest =
            exec(
                    http("Stage 1 - Smoke")
                            .post(API_PATH)
                            .body(StringBody(RequestPayloadBuilder.buildJson("A986587", "github-write-access")))
                            .check(status().in(200, 201, 202, 204))
            );

    ChainBuilder rampRequest =
            feed(rampFeeder).exec(
                    http("Stage 3 - Ramp")
                            .post(API_PATH)
                            .body(StringBody(session ->
                                    RequestPayloadBuilder.buildJson(
                                            session.getString("User"),
                                            session.getString("Entitlement")
                                    )
                            ))
                            .check(status().in(200, 201, 202, 204))
            );

    ChainBuilder steadyRequest =
            feed(steadyFeeder).exec(
                    http("Stage 4 - Steady")
                            .post(API_PATH)
                            .body(StringBody(session ->
                                    RequestPayloadBuilder.buildJson(
                                            session.getString("User"),
                                            session.getString("Entitlement")
                                    )
                            ))
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