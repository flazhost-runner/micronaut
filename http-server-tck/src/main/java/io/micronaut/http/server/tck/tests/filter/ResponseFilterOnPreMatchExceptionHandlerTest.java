/*
 * Copyright 2017-2023 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.http.server.tck.tests.filter;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.annotation.RequestFilter;
import io.micronaut.http.annotation.ResponseFilter;
import io.micronaut.http.annotation.ServerFilter;
import io.micronaut.http.server.annotation.PreMatching;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import io.micronaut.http.tck.AssertionUtils;
import io.micronaut.http.tck.BodyAssertion;
import io.micronaut.http.tck.HttpResponseAssertion;
import io.micronaut.http.tck.TestScenario;
import io.micronaut.web.router.RouteInfo;
import io.micronaut.web.router.RouteMatch;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static io.micronaut.http.annotation.Filter.MATCH_ALL_PATTERN;

@SuppressWarnings({
    "java:S5960", // We're allowed assertions, as these are used in tests only
    "checkstyle:MissingJavadocType",
    "checkstyle:DesignForExtension"
})
public class ResponseFilterOnPreMatchExceptionHandlerTest {
    private static final String SPEC_NAME = "ResponseFilterOnPreMatchExceptionHandlerTest";

    @Test
    public void exceptionHandlerTest() throws IOException {
        TestScenario.builder()
            .specName(SPEC_NAME)
            .request(HttpRequest.GET("/foo"))
            .assertion((server, request) -> AssertionUtils.assertDoesNotThrow(server, request, HttpResponseAssertion.builder()
                .status(HttpStatus.OK)
                .body(BodyAssertion.builder().body("EXCEPTION MAPPER RESPONSE #2 FILTER RESPONSE #1 FILTER").equals())
                .build()))
            .run();
    }

    static class FooException extends RuntimeException {

    }

    @ServerFilter(MATCH_ALL_PATTERN)
    @Requires(property = "spec.name", value = SPEC_NAME)
    static class ErrorThrowingFilter {

        @ResponseFilter
        public void onResponse1(MutableHttpResponse<?> response) {
            response.body(response.getBody(String.class).get() + " RESPONSE #1 FILTER");
        }

        @ResponseFilter
        public void onResponse2(MutableHttpResponse<?> response) {
            response.body(response.getBody(String.class).get() + " RESPONSE #2 FILTER");
        }

        @ResponseFilter
        public void onResponseWithRouteInfo(RouteInfo<?> routeInfo, MutableHttpResponse<?> response) {
            response.body(response.getBody(String.class).get() + " FOOBAR");
        }

        @ResponseFilter
        public void onResponseWithRouteMatch(RouteMatch<?> routeMatch, MutableHttpResponse<?> response) {
            response.body(response.getBody(String.class).get() + " XYZ");
        }

        @PreMatching
        @RequestFilter
        public void onPreMatching(MutableHttpRequest<?> request) {
            throw new FooException();
        }

        @ResponseFilter
        public void onResponse3(MutableHttpResponse<?> response) {
            response.body(response.getBody(String.class).get() + " RESPONSE #4 FILTER");
        }

        @ResponseFilter
        public void onResponse4(MutableHttpResponse<?> response) {
            response.body(response.getBody(String.class).get() + " RESPONSE #4 FILTER");
        }

    }

    @Requires(property = "spec.name", value = SPEC_NAME)
    @Controller("/foo")
    static class FooController {
        @Produces(MediaType.TEXT_PLAIN)
        @Get
        String index() {
            return "Hello World";
        }
    }

    @Requires(property = "spec.name", value = SPEC_NAME)
    @Singleton
    static class FooExceptionHandler implements ExceptionHandler<FooException, HttpResponse<?>> {

        @Override
        public HttpResponse<?> handle(HttpRequest request, FooException exception) {
            return HttpResponse.ok("EXCEPTION MAPPER");
        }
    }
}
