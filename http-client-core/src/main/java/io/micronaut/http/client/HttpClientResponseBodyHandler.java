/*
 * Copyright 2017-2026 original authors
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
package io.micronaut.http.client;

import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.core.annotation.Internal;

import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Common logic for extracting a response body according to the Micronaut HTTP client semantics.
 *
 * @author Denis Stepanov
 * @since 4.5
 */
@Internal
public final class HttpClientResponseBodyHandler {

    private static final Argument<byte[]> BYTE_ARRAY_ARGUMENT = Argument.of(byte[].class);

    private HttpClientResponseBodyHandler() {
    }

    /**
     * Extracts the body from the given response or throws a decorated {@link HttpClientResponseException}.
     *
     * @param response           The HTTP response
     * @param bodyType           The expected body type
     * @param exceptionDecorator The decorator used to customise thrown {@link HttpClientResponseException}s
     * @param <O>                The body type
     * @return The response body
     */
    public static <O> O requireBody(HttpResponse<O> response,
                                    Argument<O> bodyType,
                                    UnaryOperator<HttpClientResponseException> exceptionDecorator) {
        if (bodyType.getType() == HttpStatus.class) {
            @SuppressWarnings("unchecked")
            O status = (O) response.getStatus();
            return status;
        }
        Optional<O> body = response.getBody();
        if (body.isEmpty() && response.getBody(BYTE_ARRAY_ARGUMENT).isPresent()) {
            throw exceptionDecorator.apply(new HttpClientResponseException(
            "Failed to decode the body for the given content type [%s]".formatted(response.getContentType().orElse(null)),
                response
            ));
        }
        return body.orElseThrow(() -> exceptionDecorator.apply(new HttpClientResponseException(
            "Empty body",
            response
        )));
    }
}
