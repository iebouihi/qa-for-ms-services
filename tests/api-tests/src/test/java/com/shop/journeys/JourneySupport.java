package com.shop.journeys;

import static io.restassured.RestAssured.given;

import java.util.UUID;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/**
 * Shared setup for journey tests. Every request carries a correlation id that names the
 * test, so a failure in CI can be followed across all three services' logs.
 */
abstract class JourneySupport {

    static final String BASE_URL = System.getProperty("shop.baseUrl", "http://localhost:8080");

    static {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails(LogDetail.ALL);
    }

    static RequestSpecification api(String journey) {
        return given().spec(new RequestSpecBuilder()
                .setBaseUri(BASE_URL)
                .setContentType(ContentType.JSON)
                .addHeader("X-Correlation-Id", "journey-" + journey + "-" + UUID.randomUUID())
                .build());
    }

    /** Synthetic, recognisable, unique: safe on shared environments and easy to purge. */
    static String uniqueEmail() {
        return "test-" + UUID.randomUUID().toString().substring(0, 8) + "@example.test";
    }

    static String registerCustomer(String journey) {
        return api(journey)
                .body("""
                        {"email":"%s","firstName":"Journey","lastName":"Tester"}
                        """.formatted(uniqueEmail()))
                .post("/api/customers")
                .then().statusCode(201)
                .extract().path("id");
    }
}
