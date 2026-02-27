module io.micronaut.http.client.netty {
    requires transitive io.micronaut.context;
    requires transitive io.micronaut.http.client;
    requires transitive io.micronaut.http.netty;
    requires transitive io.micronaut.websocket;

    requires reactor.core;

    requires transitive io.netty.codec;
    requires transitive io.netty.handler.proxy;
    requires transitive io.netty.resolver;

    exports io.micronaut.http.client.netty;
    exports io.micronaut.http.client.netty.ssl;
    exports io.micronaut.http.client.netty.websocket;
}
