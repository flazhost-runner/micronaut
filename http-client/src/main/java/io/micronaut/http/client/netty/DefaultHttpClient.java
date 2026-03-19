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

import io.micronaut.core.annotation.AnnotationMetadataResolver;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.execution.ExecutionFlow;
import io.micronaut.core.io.buffer.ByteBuffer;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.bind.RequestBinderRegistry;
import io.micronaut.http.body.CloseableByteBody;
import io.micronaut.http.body.MessageBodyHandlerRegistry;
import io.micronaut.http.client.AsyncHttpClient;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.HttpClientConfiguration;
import io.micronaut.http.client.HttpVersionSelection;
import io.micronaut.http.client.LoadBalancer;
import io.micronaut.http.client.ProxyHttpClient;
import io.micronaut.http.client.ProxyRequestOptions;
import io.micronaut.http.client.RawHttpClient;
import io.micronaut.http.client.filter.ClientFilterResolutionContext;
import io.micronaut.http.client.sse.SseClient;
import io.micronaut.http.client.netty.ssl.ClientSslBuilder;
import io.micronaut.http.codec.MediaTypeCodecRegistry;
import io.micronaut.http.filter.HttpClientFilter;
import io.micronaut.http.filter.HttpClientFilterResolver;
import io.micronaut.http.filter.HttpFilterResolver;
import io.micronaut.http.client.StreamingHttpClient;
import io.micronaut.http.sse.Event;
import io.micronaut.websocket.WebSocketClient;
import io.micronaut.websocket.context.WebSocketBeanRegistry;
import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.resolver.AddressResolverGroup;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;

import java.io.Closeable;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadFactory;

/**
 * Public facade delegating {@link DefaultHttpClient} behaviour to the internal {@link NettyHttpClient}.
 *
 * @since 4.8
 */
@Internal
@SuppressWarnings("checkstyle:DesignForExtension")
public class DefaultHttpClient implements
    WebSocketClient,
    HttpClient,
    StreamingHttpClient,
    SseClient,
    ProxyHttpClient,
    RawHttpClient,
    AutoCloseable,
    Closeable {

    private final NettyHttpClient delegate;

    public DefaultHttpClient(NettyHttpClient delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Deprecated
    public DefaultHttpClient(@Nullable LoadBalancer loadBalancer,
                             HttpClientConfiguration configuration,
                             @Nullable String contextPath,
                             @Nullable ThreadFactory threadFactory,
                             ClientSslBuilder nettyClientSslBuilder,
                             MediaTypeCodecRegistry codecRegistry,
                             MessageBodyHandlerRegistry handlerRegistry,
                             @Nullable AnnotationMetadataResolver annotationMetadataResolver,
                             io.micronaut.core.convert.ConversionService conversionService,
                             HttpClientFilter... filters) {
        this(new NettyHttpClient(
            loadBalancer,
            configuration,
            contextPath,
            threadFactory,
            nettyClientSslBuilder,
            codecRegistry,
            handlerRegistry,
            annotationMetadataResolver,
            conversionService,
            filters
        ));
    }

    @Deprecated
    public DefaultHttpClient(@Nullable LoadBalancer loadBalancer,
                             @Nullable HttpVersionSelection explicitHttpVersion,
                             HttpClientConfiguration configuration,
                             @Nullable String contextPath,
                             HttpClientFilterResolver<ClientFilterResolutionContext> filterResolver,
                             List<HttpFilterResolver.FilterEntry> clientFilterEntries,
                             @Nullable ThreadFactory threadFactory,
                             ClientSslBuilder nettyClientSslBuilder,
                             MediaTypeCodecRegistry codecRegistry,
                             MessageBodyHandlerRegistry handlerRegistry,
                             WebSocketBeanRegistry webSocketBeanRegistry,
                             RequestBinderRegistry requestBinderRegistry,
                             @Nullable EventLoopGroup eventLoopGroup,
                             ChannelFactory<? extends SocketChannel> socketChannelFactory,
                             ChannelFactory<? extends DatagramChannel> udpChannelFactory,
                             NettyClientCustomizer clientCustomizer,
                             @Nullable String informationalServiceId,
                             io.micronaut.core.convert.ConversionService conversionService,
                             @Nullable AddressResolverGroup<?> resolverGroup) {
        this(new NettyHttpClient(
            loadBalancer,
            explicitHttpVersion,
            configuration,
            contextPath,
            filterResolver,
            clientFilterEntries,
            threadFactory,
            nettyClientSslBuilder,
            codecRegistry,
            handlerRegistry,
            webSocketBeanRegistry,
            requestBinderRegistry,
            eventLoopGroup,
            socketChannelFactory,
            udpChannelFactory,
            clientCustomizer,
            informationalServiceId,
            conversionService,
            resolverGroup
        ));
    }

    DefaultHttpClient(DefaultHttpClientBuilder builder) {
        this(new NettyHttpClient(NettyHttpClientBuilder.from(builder)));
    }

    public DefaultHttpClient(@Nullable URI uri) {
        this(new NettyHttpClient(uri));
    }

    public DefaultHttpClient() {
        this(new NettyHttpClient());
    }

    public DefaultHttpClient(@Nullable URI uri, HttpClientConfiguration configuration) {
        this(new NettyHttpClient(uri, configuration));
    }

    public DefaultHttpClient(@Nullable URI uri, HttpClientConfiguration configuration, ClientSslBuilder clientSslBuilder) {
        this(new NettyHttpClient(uri, configuration, clientSslBuilder));
    }

    public DefaultHttpClient(@Nullable LoadBalancer loadBalancer, HttpClientConfiguration configuration) {
        this(new NettyHttpClient(loadBalancer, configuration));
    }

    public static DefaultHttpClientBuilder builder() {
        return new DefaultHttpClientBuilder();
    }

    NettyHttpClient delegate() {
        return delegate;
    }

    public HttpClientConfiguration getConfiguration() {
        return delegate.getConfiguration();
    }

    public Logger getLog() {
        return delegate.getLog();
    }

    public ConnectionManager connectionManager() {
        return delegate.connectionManager();
    }

    public ConnectionManager getConnectionManager() {
        return connectionManager();
    }

    public void setConnectionManager(ConnectionManager connectionManager) {
        delegate.connectionManager = connectionManager;
    }

    @Override
    public HttpClient start() {
        delegate.start();
        return this;
    }

    @Override
    public boolean isRunning() {
        return delegate.isRunning();
    }

    @Override
    public HttpClient stop() {
        delegate.stop();
        return this;
    }

    @Override
    public HttpClient refresh() {
        delegate.refresh();
        return this;
    }

    public MediaTypeCodecRegistry getMediaTypeCodecRegistry() {
        return delegate.getMediaTypeCodecRegistry();
    }

    public void setMediaTypeCodecRegistry(MediaTypeCodecRegistry mediaTypeCodecRegistry) {
        delegate.setMediaTypeCodecRegistry(mediaTypeCodecRegistry);
    }

    public MessageBodyHandlerRegistry getHandlerRegistry() {
        return delegate.getHandlerRegistry();
    }

    public void setHandlerRegistry(MessageBodyHandlerRegistry handlerRegistry) {
        delegate.setHandlerRegistry(handlerRegistry);
    }

    @Override
    public AsyncHttpClient toAsync() {
        return delegate.toAsync();
    }

    @Override
    public BlockingHttpClient toBlocking() {
        return delegate.toBlocking();
    }

    @Override
    public <I> Publisher<Event<ByteBuffer<?>>> eventStream(io.micronaut.http.HttpRequest<I> request) {
        return delegate.eventStream(request);
    }

    @Override
    public <I, B> Publisher<Event<B>> eventStream(io.micronaut.http.HttpRequest<I> request,
                                                  Argument<B> eventType,
                                                  Argument<?> errorType) {
        return delegate.eventStream(request, eventType, errorType);
    }

    @Override
    public <I, B> Publisher<Event<B>> eventStream(io.micronaut.http.HttpRequest<I> request,
                                                  Argument<B> eventType) {
        return delegate.eventStream(request, eventType);
    }

    @Override
    public <I> Publisher<ByteBuffer<?>> dataStream(io.micronaut.http.HttpRequest<I> request) {
        return delegate.dataStream(request);
    }

    @Override
    public <I> Publisher<ByteBuffer<?>> dataStream(io.micronaut.http.HttpRequest<I> request,
                                                   @Nullable Argument<?> errorType) {
        return delegate.dataStream(request, errorType);
    }

    @Override
    public <I> Publisher<HttpResponse<ByteBuffer<?>>> exchangeStream(io.micronaut.http.HttpRequest<I> request) {
        return delegate.exchangeStream(request);
    }

    @Override
    public <I> Publisher<HttpResponse<ByteBuffer<?>>> exchangeStream(io.micronaut.http.HttpRequest<I> request,
                                                                     Argument<?> errorType) {
        return delegate.exchangeStream(request, errorType);
    }

    @Override
    public <I> Publisher<Map<String, Object>> jsonStream(io.micronaut.http.HttpRequest<I> request) {
        return delegate.jsonStream(request);
    }

    @Override
    public <I, O> Publisher<O> jsonStream(io.micronaut.http.HttpRequest<I> request, Argument<O> type) {
        return delegate.jsonStream(request, type);
    }

    @Override
    public <I, O> Publisher<O> jsonStream(io.micronaut.http.HttpRequest<I> request,
                                          Argument<O> type,
                                          Argument<?> errorType) {
        return delegate.jsonStream(request, type, errorType);
    }

    @Override
    public <I, O, E> Publisher<HttpResponse<O>> exchange(io.micronaut.http.HttpRequest<I> request,
                                                         Argument<O> bodyType,
                                                         Argument<E> errorType) {
        return delegate.exchange(request, bodyType, errorType);
    }

    @Override
    public <I, O, E> Publisher<O> retrieve(io.micronaut.http.HttpRequest<I> request,
                                           Argument<O> bodyType,
                                           Argument<E> errorType) {
        return delegate.retrieve(request, bodyType, errorType);
    }

    @Override
    public <T extends AutoCloseable> Publisher<T> connect(Class<T> clientEndpointType,
                                                          MutableHttpRequest<?> request) {
        return delegate.connect(clientEndpointType, request);
    }

    @Override
    public <T extends AutoCloseable> Publisher<T> connect(Class<T> clientEndpointType,
                                                          Map<String, Object> parameters) {
        return delegate.connect(clientEndpointType, parameters);
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public Publisher<MutableHttpResponse<?>> proxy(io.micronaut.http.HttpRequest<?> request) {
        return delegate.proxy(request);
    }

    @Override
    public Publisher<MutableHttpResponse<?>> proxy(io.micronaut.http.HttpRequest<?> request,
                                                   ProxyRequestOptions options) {
        return delegate.proxy(request, options);
    }

    @Override
    public Publisher<? extends HttpResponse<?>> exchange(io.micronaut.http.HttpRequest<?> request,
                                                         @Nullable CloseableByteBody requestBody,
                                                         @Nullable Thread blockedThread) {
        return delegate.exchange(request, requestBody, blockedThread);
    }

    public <I> ExecutionFlow<URI> resolveRequestURI(io.micronaut.http.HttpRequest<I> request) {
        return delegate.resolveRequestURI(request);
    }

    public <I> ExecutionFlow<URI> resolveRequestURI(io.micronaut.http.HttpRequest<I> request,
                                                    boolean includeContextPath) {
        return delegate.resolveRequestURI(request, includeContextPath);
    }
}
