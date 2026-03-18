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

import io.micronaut.context.LifeCycle;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.exceptions.HttpClientResponseException;

import java.io.Closeable;
import java.util.concurrent.CompletionStage;
import java.util.function.UnaryOperator;

/**
 * A {@link java.util.concurrent.CompletableFuture}-based HTTP client interface providing an alternative to
 * the reactive {@link HttpClient}.
 *
 * @author Denis Stepanov
 * @since 4.5
 */
public interface AsyncHttpClient extends Closeable, LifeCycle<AsyncHttpClient> {

    /**
     * Perform an HTTP request for the given request object returning the full {@link HttpResponse} with the body
     * converted to the specified type.
     *
     * @param request   The {@link HttpRequest} to execute
     * @param bodyType  The body type
     * @param errorType The error type
     * @param <I>       The request body type
     * @param <O>       The response body type
     * @param <E>       The error type
     * @return A {@link CompletionStage} that completes with the full {@link HttpResponse}
     */
    <I, O, E> CompletionStage<HttpResponse<O>> exchange(HttpRequest<I> request,
                                                        Argument<O> bodyType,
                                                        Argument<E> errorType);

    /**
     * Perform an HTTP request for the given request object returning the full {@link HttpResponse} with the body
     * converted to the specified type.
     *
     * @param request  The {@link HttpRequest} to execute
     * @param bodyType The body type
     * @param <I>      The request body type
     * @param <O>      The response body type
     * @return A {@link CompletionStage} that completes with the full {@link HttpResponse}
     */
    default <I, O> CompletionStage<HttpResponse<O>> exchange(HttpRequest<I> request, Argument<O> bodyType) {
        return exchange(request, bodyType, HttpClient.DEFAULT_ERROR_TYPE);
    }

    /**
     * Perform an HTTP request for the given request object returning the full {@link HttpResponse} with the body
     * converted to the specified type.
     *
     * @param request  The {@link HttpRequest} to execute
     * @param bodyType The body type
     * @param <I>      The request body type
     * @param <O>      The response body type
     * @return A {@link CompletionStage} that completes with the full {@link HttpResponse}
     */
    default <I, O> CompletionStage<HttpResponse<O>> exchange(HttpRequest<I> request, Class<O> bodyType) {
        return exchange(request, Argument.of(bodyType));
    }

    /**
     * Perform an HTTP request for the given request object and convert the response body to the specified type.
     *
     * @param request   The {@link HttpRequest} to execute
     * @param bodyType  The body type
     * @param errorType The error type
     * @param <I>       The request body type
     * @param <O>       The response body type
     * @param <E>       The error type
     * @return A {@link CompletionStage} that completes with the response body
     * @throws HttpClientResponseException when an error status is returned
     */
    default <I, O, E> CompletionStage<O> retrieve(HttpRequest<I> request,
                                                  Argument<O> bodyType,
                                                  Argument<E> errorType) {
        if (bodyType.getType() == void.class) {
            return exchange(request, bodyType, errorType).thenApply(response -> {
                @SuppressWarnings("unchecked")
                O body = (O) null;
                return body;
            });
        }
        return exchange(request, bodyType, errorType)
            .thenApply(response -> HttpClientResponseBodyHandler.requireBody(response, bodyType, UnaryOperator.identity()));
    }

    /**
     * Perform an HTTP request for the given request object and convert the response body to the specified type.
     *
     * @param request  The {@link HttpRequest} to execute
     * @param bodyType The body type
     * @param <I>      The request body type
     * @param <O>      The response body type
     * @return A {@link CompletionStage} that completes with the response body
     * @throws HttpClientResponseException when an error status is returned
     */
    default <I, O> CompletionStage<O> retrieve(HttpRequest<I> request, Argument<O> bodyType) {
        return retrieve(request, bodyType, HttpClient.DEFAULT_ERROR_TYPE);
    }

    /**
     * Perform an HTTP request for the given request object and convert the response body to the specified type.
     *
     * @param request  The {@link HttpRequest} to execute
     * @param bodyType The body type
     * @param <I>      The request body type
     * @param <O>      The response body type
     * @return A {@link CompletionStage} that completes with the response body
     * @throws HttpClientResponseException when an error status is returned
     */
    default <I, O> CompletionStage<O> retrieve(HttpRequest<I> request, Class<O> bodyType) {
        return retrieve(request, Argument.of(bodyType));
    }

    /**
     * Perform an HTTP request for the given request object and convert the response body to {@link String}.
     *
     * @param request The {@link HttpRequest} to execute
     * @param <I>     The request body type
     * @return A {@link CompletionStage} that completes with the response body
     * @throws HttpClientResponseException when an error status is returned
     */
    default <I> CompletionStage<String> retrieve(HttpRequest<I> request) {
        return retrieve(request, String.class);
    }

    /**
     * Perform an HTTP GET request for the given URI and convert the response body to {@link String}.
     *
     * @param uri The URI
     * @return A {@link CompletionStage} that completes with the response body
     * @throws HttpClientResponseException when an error status is returned
     */
    default CompletionStage<String> retrieve(String uri) {
        return retrieve(HttpRequest.GET(uri), String.class);
    }

    /**
     * Perform an HTTP GET request for the given URI and convert the response body to the specified type.
     *
     * @param uri      The URI
     * @param bodyType The body type
     * @param <O>      The response body type
     * @return A {@link CompletionStage} that completes with the response body
     * @throws HttpClientResponseException when an error status is returned
     */
    default <O> CompletionStage<O> retrieve(String uri, Class<O> bodyType) {
        return retrieve(HttpRequest.GET(uri), bodyType);
    }
}
