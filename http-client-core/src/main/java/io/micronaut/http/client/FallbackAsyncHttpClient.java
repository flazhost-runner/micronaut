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

import io.micronaut.core.annotation.Internal;
import org.jspecify.annotations.Nullable;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.UnaryOperator;

/**
 * Fallback {@link AsyncHttpClient} implementation that adapts a reactive {@link HttpClient}
 * to the {@link CompletionStage}-based API.
 *
 * @author Denis Stepanov
 * @since 4.5
 */
@Internal
public final class FallbackAsyncHttpClient implements AsyncHttpClient {
    private static final Logger LOG = LoggerFactory.getLogger(FallbackAsyncHttpClient.class);

    private final HttpClient delegate;
    private final UnaryOperator<HttpClientResponseException> exceptionDecorator;

    /**
     * Creates a new async client wrapping the given reactive client.
     *
     * @param delegate The reactive HTTP client
     */
    public FallbackAsyncHttpClient(HttpClient delegate) {
        this(delegate, UnaryOperator.identity());
    }

    /**
     * Creates a new async client wrapping the given reactive client and applying the provided error decorator.
     *
     * @param delegate           The reactive HTTP client
     * @param exceptionDecorator The decorator applied to {@link HttpClientResponseException}s
     */
    public FallbackAsyncHttpClient(HttpClient delegate, UnaryOperator<HttpClientResponseException> exceptionDecorator) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.exceptionDecorator = Objects.requireNonNull(exceptionDecorator, "exceptionDecorator");
    }

    @Override
    public <I, O, E> CompletionStage<HttpResponse<O>> exchange(HttpRequest<I> request,
                                                               Argument<O> bodyType,
                                                               Argument<E> errorType) {
        return single(delegate.exchange(request, bodyType, errorType));
    }

    @Override
    public <I, O, E> CompletionStage<O> retrieve(HttpRequest<I> request, Argument<O> bodyType, Argument<E> errorType) {
        if (bodyType.getType() == void.class) {
            return exchange(request, bodyType, errorType).thenApply(response -> {
                @SuppressWarnings("unchecked")
                O body = (O) null;
                return body;
            });
        }
        return exchange(request, bodyType, errorType)
            .thenApply(response -> HttpClientResponseBodyHandler.requireBody(response, bodyType, exceptionDecorator));
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

    @SuppressWarnings("FutureReturnValueIgnored")
    private static <T> CompletionStage<T> single(Publisher<T> publisher) {
        CompletableFuture<T> future = new CompletableFuture<>();
        publisher.subscribe(new Subscriber<>() {
            private @Nullable Subscription subscription;
            private boolean valueReceived;

            @Override
            public void onSubscribe(Subscription s) {
                this.subscription = s;
                future.whenComplete((value, throwable) -> {
                    if (throwable instanceof CancellationException && subscription != null) {
                        subscription.cancel();
                    }
                });
                s.request(1);
            }

            @Override
            public void onNext(T t) {
                if (!valueReceived) {
                    valueReceived = true;
                    future.complete(t);
                    if (subscription != null) {
                        subscription.cancel();
                    }
                } else {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("Publisher emitted more than one value; cancelling subscription");
                    }
                    if (subscription != null) {
                        subscription.cancel();
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                future.completeExceptionally(t);
            }

            @Override
            public void onComplete() {
                if (!valueReceived && !future.isDone()) {
                    future.completeExceptionally(new NoSuchElementException("Publisher completed without emitting a value"));
                }
            }
        });
        return future;
    }
}
