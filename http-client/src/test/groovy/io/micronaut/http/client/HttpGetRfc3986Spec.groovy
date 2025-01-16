package io.micronaut.http.client

import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest
@Property(name = 'spec.name', value = 'HttpGetSpec')
@Property(name = 'micronaut.http.client.read-timeout', value = '30s')
@Property(name = 'micronaut.http.client.url-encoding-kind', value = 'RFC_3986')
class HttpGetRfc3986Spec  extends Specification {
    @Inject
    HttpGetSpec.MyGetClient myGetClient

    void "test query parameter with @Client interface"() {
        expect:
        myGetClient.queryParam('Hello World') == 'Hello World'
        myGetClient.queryParam2('Hello World') == '/get/queryParam2?foo=Hello%20World'
    }
}
