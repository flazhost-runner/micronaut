package io.micronaut.http.server.tck.tests.binding;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.tck.AssertionUtils;
import io.micronaut.http.tck.BodyAssertion;
import io.micronaut.http.tck.HttpResponseAssertion;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static io.micronaut.http.tck.TestScenario.asserts;

@SuppressWarnings({
    "java:S5960", // We're allowed assertions, as these are used in tests only
    "checkstyle:MissingJavadocType",
    "checkstyle:DesignForExtension"
})
public class DecodingTest {
    public static final String SPEC_NAME = "DecodingTest";

    @Test
    void pathVarWithPercentIsDecoded() throws IOException {
        asserts(SPEC_NAME,
            HttpRequest.GET("/decoding/path/foo%20bar"),
            (server, request) -> AssertionUtils.assertDoesNotThrow(server, request, HttpResponseAssertion.builder()
                .status(HttpStatus.OK)
                .body(BodyAssertion.builder().body("foo bar").equals())
                .build()));
    }

    @Test
    void pathVarWithPlusIsNotDecoded() throws IOException {
        asserts(SPEC_NAME,
            HttpRequest.GET("/decoding/path/foo+bar"),
            (server, request) -> AssertionUtils.assertDoesNotThrow(server, request, HttpResponseAssertion.builder()
                .status(HttpStatus.OK)
                .body(BodyAssertion.builder().body("foo+bar").equals())
                .build()));
    }

    @Test
    void queryVarWithPercentIsDecoded() throws IOException {
        asserts(SPEC_NAME,
            HttpRequest.GET("/decoding/query?queryVar=foo%20bar"),
            (server, request) -> AssertionUtils.assertDoesNotThrow(server, request, HttpResponseAssertion.builder()
                .status(HttpStatus.OK)
                .body(BodyAssertion.builder().body("foo bar").equals())
                .build()));
    }

    @Test
    void queryVarWithPlusIsDecoded() throws IOException {
        asserts(SPEC_NAME,
            HttpRequest.GET("/decoding/query?queryVar=foo+bar"),
            (server, request) -> AssertionUtils.assertDoesNotThrow(server, request, HttpResponseAssertion.builder()
                .status(HttpStatus.OK)
                .body(BodyAssertion.builder().body("foo bar").equals())
                .build()));
    }

    @Requires(property = "spec.name", value = SPEC_NAME)
    @Controller("/decoding")
    static class Ctrl {

        @Get("/path/{pathVar}")
        String pathVar(@PathVariable String pathVar) {
            return pathVar;
        }

        @Get("/query/{?queryVar")
        String queryVar(@QueryValue String queryVar) {
            return queryVar;
        }
    }
}
