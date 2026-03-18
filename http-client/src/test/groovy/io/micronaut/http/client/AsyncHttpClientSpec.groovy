package io.micronaut.http.client

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.runtime.server.EmbeddedServer
import spock.lang.AutoCleanup
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.CompletionStage
import java.util.concurrent.TimeUnit

class AsyncHttpClientSpec extends Specification {

    @AutoCleanup
    EmbeddedServer embeddedServer = ApplicationContext.run(EmbeddedServer, [
        'spec.name': 'AsyncHttpClientSpec'
    ])

    @AutoCleanup
    ApplicationContext context = embeddedServer.applicationContext

    void "async client covers full API surface"() {
        given:
        HttpClient httpClient = context.createBean(HttpClient, embeddedServer.URL)
        AsyncHttpClient asyncClient = httpClient.toAsync()
        Argument<String> stringArgument = Argument.of(String)
        Argument<String> errorArgument = Argument.of(String)

        when: "exchange with explicit body/error types"
        HttpResponse<String> explicitExchange = await(asyncClient.exchange(HttpRequest.GET('/async/hello'), stringArgument, errorArgument))

        then:
        explicitExchange.body() == 'hello'

        when: "exchange overloads"
        HttpResponse<String> argumentExchange = await(asyncClient.exchange(HttpRequest.GET('/async/hello'), stringArgument))
        HttpResponse<String> classExchange = await(asyncClient.exchange(HttpRequest.GET('/async/hello'), String))
        HttpResponse<Void> voidExchange = await(asyncClient.exchange(HttpRequest.GET('/async/void'), Argument.VOID))

        then:
        argumentExchange.body() == 'hello'
        classExchange.body() == 'hello'
        voidExchange.body() == null

        when: "retrieve with explicit error type"
        String explicitRetrieve = await(asyncClient.retrieve(HttpRequest.GET('/async/hello'), stringArgument, errorArgument))
        Void explicitVoidRetrieve = await(asyncClient.retrieve(HttpRequest.GET('/async/void'), Argument.VOID, errorArgument))

        then:
        explicitRetrieve == 'hello'
        explicitVoidRetrieve == null

        when: "retrieve overloads"
        String argumentRetrieve = await(asyncClient.retrieve(HttpRequest.GET('/async/hello'), stringArgument))
        String classRetrieve = await(asyncClient.retrieve(HttpRequest.GET('/async/hello'), String))
        String defaultRetrieve = await(asyncClient.retrieve(HttpRequest.GET('/async/hello')))
        String uriRetrieve = await(asyncClient.retrieve('/async/hello'))
        String uriClassRetrieve = await(asyncClient.retrieve('/async/hello', String))
        Void voidRetrieve = await(asyncClient.retrieve(HttpRequest.GET('/async/void'), Argument.VOID))

        then:
        [argumentRetrieve, classRetrieve, defaultRetrieve, uriRetrieve, uriClassRetrieve].every { it == 'hello' }
        voidRetrieve == null

        cleanup:
        asyncClient.close()
        httpClient.close()
    }

    void "async client lifecycle delegates to underlying client"() {
        given:
        HttpClient httpClient = HttpClient.create(embeddedServer.URL)
        AsyncHttpClient asyncClient = httpClient.toAsync()
        PollingConditions conditions = new PollingConditions(timeout: 5)

        expect:
        asyncClient.isRunning()

        when:
        asyncClient.stop()

        then:
        conditions.eventually {
            !asyncClient.isRunning()
        }

        when:
        asyncClient.start()

        then:
        conditions.eventually {
            asyncClient.isRunning()
        }

        cleanup:
        asyncClient.close()
        httpClient.close()
    }

    private static <T> T await(CompletionStage<T> stage) {
        stage.toCompletableFuture().get(5, TimeUnit.SECONDS)
    }

    @Controller("/async")
    @Requires(property = "spec.name", value = "AsyncHttpClientSpec")
    static class AsyncController {

        @Get("/hello")
        String hello() {
            "hello"
        }

        @Get("/void")
        HttpResponse<Void> noop() {
            HttpResponse.ok()
        }
    }
}
