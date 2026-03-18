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
 * Netty-based {@link AsyncHttpClient} that reuses the {@link DefaultHttpClient} infrastructure
 * while exposing a {@link java.util.concurrent.CompletionStage}-centric API.
 */
final class DefaultAsyncHttpClient implements AsyncHttpClient {
    private final DefaultHttpClient delegate;

    DefaultAsyncHttpClient(DefaultHttpClient delegate) {
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
