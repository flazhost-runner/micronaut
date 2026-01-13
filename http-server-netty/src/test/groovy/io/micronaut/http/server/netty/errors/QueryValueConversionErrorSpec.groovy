package io.micronaut.http.server.netty.errors

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.client.BlockingHttpClient
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.server.EmbeddedServer
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

class QueryValueConversionErrorSpec extends Specification {
    @Shared
    @AutoCleanup
    EmbeddedServer embeddedServer = ApplicationContext.run(EmbeddedServer, [
            'spec.name': 'QueryValueConversionErrorSpec'
    ])

    @Shared
    @AutoCleanup
    HttpClient httpClient = embeddedServer.applicationContext.createBean(HttpClient, embeddedServer.URL)

    BlockingHttpClient getClient() {
        httpClient.toBlocking()
    }

    void "test QueryValue conversion error shows proper message"() {
        when:
        client.exchange(HttpRequest.GET('/test?count=a'), Map)

        then:
        HttpClientResponseException ex = thrown()
        ex.response.status == HttpStatus.BAD_REQUEST

        when:
        Map payload = ex.response.body()

        then:
        payload._embedded.errors[0].message.contains('Failed to convert argument [count]')
        payload._embedded.errors[0].message.contains('For input string: "a"')
    }

    void "test QueryValue with named parameter conversion error"() {
        when:
        client.exchange(HttpRequest.GET('/test-named?num=invalid'), Map)

        then:
        HttpClientResponseException ex = thrown()
        ex.response.status == HttpStatus.BAD_REQUEST

        when:
        Map payload = ex.response.body()

        then:
        payload._embedded.errors[0].message.contains('Failed to convert argument [number]')
        payload._embedded.errors[0].message.contains('For input string: "invalid"')
    }

    @Requires(property = 'spec.name', value = 'QueryValueConversionErrorSpec')
    @Controller
    static class TestController {
        @Get("/test")
        String helloWorld(@QueryValue(value = "count") Integer count) {
            return "Hello World"
        }

        @Get("/test-named")
        String testNamed(@QueryValue(value = "num") Integer number) {
            return "Hello World"
        }
    }
}
