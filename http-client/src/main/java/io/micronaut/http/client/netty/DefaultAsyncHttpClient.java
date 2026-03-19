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
package io.micronaut.http.client.netty;

import io.micronaut.core.execution.ExecutionFlow;
import io.micronaut.core.propagation.PropagatedContext;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.AsyncHttpClient;
import io.micronaut.http.client.HttpClientResponseBodyHandler;
import io.micronaut.core.type.Argument;
import io.micronaut.http.client.exceptions.HttpClientResponseException;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.UnaryOperator;

/**
 * Netty-based {@link AsyncHttpClient} that reuses the {@link NettyHttpClient} infrastructure
 * while exposing a {@link java.util.concurrent.CompletionStage}-centric API.
 */
final class DefaultAsyncHttpClient implements AsyncHttpClient {
    private final NettyHttpClient delegate;

    DefaultAsyncHttpClient(NettyHttpClient delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public <I, O, E> CompletionStage<HttpResponse<O>> exchange(HttpRequest<I> request,
                                                               Argument<O> bodyType,
                                                               Argument<E> errorType) {
        PropagatedContext propagatedContext = PropagatedContext.getOrEmpty();
        ExecutionFlow<HttpResponse<O>> flow = delegate.exchangeFlow(request, bodyType, errorType, null, propagatedContext);
        return toCompletionStage(flow, propagatedContext);
    }

    @Override
    public <I, O, E> CompletionStage<O> retrieve(HttpRequest<I> request,
                                                 Argument<O> bodyType,
                                                 Argument<E> errorType) {
        PropagatedContext propagatedContext = PropagatedContext.getOrEmpty();
        if (bodyType.getType() == void.class) {
            ExecutionFlow<HttpResponse<O>> flow = delegate.exchangeFlow(request, bodyType, errorType, null, propagatedContext);
            return toCompletionStage(flow.map(response -> null), propagatedContext);
        }
        ExecutionFlow<HttpResponse<O>> flow = delegate.exchangeFlow(request, bodyType, errorType, null, propagatedContext);
        UnaryOperator<HttpClientResponseException> decorator = delegate::decorate;
        ExecutionFlow<O> mapped = flow.map(response -> HttpClientResponseBodyHandler.requireBody(response, bodyType, decorator));
        return toCompletionStage(mapped, propagatedContext);
    }

    @Override
    public AsyncHttpClient start() {
        delegate.start();
        return this;
    }

    @Override
    public AsyncHttpClient stop() {
        delegate.stop();
        return this;
    }

    @Override
    public boolean isRunning() {
        return delegate.isRunning();
    }

    @Override
    public void close() {
        delegate.close();
    }

    private static <T> CompletionStage<T> toCompletionStage(ExecutionFlow<T> flow, PropagatedContext context) {
        CompletableFuture<T> future = new CompletableFuture<>();
        context.wrap((Runnable) () -> flow.completeTo(future)).run();
        return future;
    }
}
