/*
 * Copyright 2017-2024 original authors
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
package io.micronaut.http.server.netty;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Property;
import io.micronaut.core.io.Writable;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MicronautTest
@DisabledInNativeImage
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("Issue10199")
@Requires(property = "spec.name", value = "WritableRuntimeExceptionTest")
@Property(name = "spec.name", value = "WritableRuntimeExceptionTest")
final class WritableRuntimeExceptionTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void writableRuntimeExceptionDoesNotHang() {
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () ->
            client.toBlocking().exchange(HttpRequest.GET("/writable-runtime-exception"))
        );

        assertEquals(500, ex.getStatus().getCode());
    }

    @Controller
    @Requires(property = "spec.name", value = "WritableRuntimeExceptionTest")
    static final class Ctrl {
        @Get("/writable-runtime-exception")
        HttpResponse<Writable> index() {
            return HttpResponse.ok((Writable) out -> {
                throw new RuntimeException("Baaaaad!");
            });
        }
    }
}
