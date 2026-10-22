package com.shop.journeys;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Journey 2 - browse products")
class BrowseProductsJourney extends JourneySupport {

    @Test
    void catalogCanBeListedSearchedAndOpened() {
        List<String> ids = api("browse").get("/api/products")
                .then().statusCode(200)
                .body("totalElements", greaterThan(0))
                .extract().path("content.id");

        List<String> names = api("browse").queryParam("q", "keyboard").get("/api/products")
                .then().statusCode(200)
                .extract().path("content.name");
        assertThat(names).isNotEmpty().allMatch(n -> n.toLowerCase().contains("keyboard"));

        api("browse").get("/api/products/{id}", ids.getFirst())
                .then().statusCode(200)
                .body("id", equalTo(ids.getFirst()));
    }
}
