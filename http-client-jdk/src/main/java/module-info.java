module io.micronaut.http.client.jdk {
    requires transitive io.micronaut.http.client;
    requires java.net.http;
    requires reactor.core;

    exports io.micronaut.http.client.jdk;
    exports io.micronaut.http.client.jdk.cookie;
}
