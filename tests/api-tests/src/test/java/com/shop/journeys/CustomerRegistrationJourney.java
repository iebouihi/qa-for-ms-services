package com.shop.journeys;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Journey 1 - customer registration")
class CustomerRegistrationJourney extends JourneySupport {

    @Test
    void newCustomerCanRegisterAndBeRetrieved() {
        String email = uniqueEmail();

        String id = api("registration")
                .body("""
                        {"email":"%s","firstName":"Jane","lastName":"Doe"}
                        """.formatted(email))
                .post("/api/customers")
                .then().statusCode(201)
                .body("id", notNullValue())
                .extract().path("id");

        api("registration").get("/api/customers/{id}", id)
                .then().statusCode(200)
                .body("email", equalTo(email));
    }

    @Test
    void sameEmailCannotRegisterTwice() {
        String email = uniqueEmail();
        String body = """
                {"email":"%s","firstName":"Jane","lastName":"Doe"}
                """.formatted(email);

        api("registration-duplicate").body(body).post("/api/customers").then().statusCode(201);
        api("registration-duplicate").body(body).post("/api/customers")
                .then().statusCode(409)
                .body("code", equalTo("EMAIL_ALREADY_REGISTERED"));
    }
}
