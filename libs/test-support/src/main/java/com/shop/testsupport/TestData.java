package com.shop.testsupport;

import java.util.UUID;

/**
 * Test data rule #1: every test creates the data it needs, with unique values.
 * No shared fixtures, so tests are independent, can run in parallel and in any order.
 * The "test-" prefix makes synthetic data easy to recognise and purge in shared environments.
 */
public final class TestData {

    private TestData() {
    }

    public static String uniqueEmail() {
        return "test-" + UUID.randomUUID().toString().substring(0, 8) + "@example.test";
    }
}
