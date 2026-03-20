package io.micronaut.validation.routes

import io.micronaut.inject.ast.ClassElement
import io.micronaut.inject.ast.MethodElement
import io.micronaut.validation.routes.rules.SuspendedReactiveReturnTypeRule
import io.micronaut.validation.routes.rules.RouteValidationRule
import spock.lang.Specification

class SuspendedReactiveReturnTypeRuleSpec extends Specification {

    private final RouteValidationRule rule = new SuspendedReactiveReturnTypeRule()

    void "test suspended route with regular return type is allowed"() {
        given:
        ClassElement returnType = Stub() {
            isAssignable(org.reactivestreams.Publisher) >> false
            isAssignable(java.util.concurrent.CompletionStage) >> false
            getName() >> 'java.lang.String'
        }
        MethodElement method = Stub() {
            isSuspend() >> true
            getGenericReturnType() >> returnType
        }

        expect:
        rule.validate([], [] as io.micronaut.inject.ast.ParameterElement[], method).valid
    }

    void "test suspended route with reactive return type is rejected"() {
        given:
        ClassElement returnType = Stub() {
            isAssignable(org.reactivestreams.Publisher) >> true
            isAssignable(java.util.concurrent.CompletionStage) >> false
            getName() >> 'org.reactivestreams.Publisher'
        }
        MethodElement method = Stub() {
            isSuspend() >> true
            getGenericReturnType() >> returnType
        }

        when:
        def result = rule.validate([], [] as io.micronaut.inject.ast.ParameterElement[], method)

        then:
        !result.valid
        result.errorMessages == ['Unsupported suspended controller return type [org.reactivestreams.Publisher]. Suspend functions must not return reactive or async types.']
    }

    void "test suspended route with async return type is rejected"() {
        given:
        ClassElement returnType = Stub() {
            isAssignable(org.reactivestreams.Publisher) >> false
            isAssignable(java.util.concurrent.CompletionStage) >> true
            getName() >> 'java.util.concurrent.CompletionStage'
        }
        MethodElement method = Stub() {
            isSuspend() >> true
            getGenericReturnType() >> returnType
        }

        when:
        def result = rule.validate([], [] as io.micronaut.inject.ast.ParameterElement[], method)

        then:
        !result.valid
        result.errorMessages == ['Unsupported suspended controller return type [java.util.concurrent.CompletionStage]. Suspend functions must not return reactive or async types.']
    }

    void "test non suspended route is ignored"() {
        given:
        ClassElement returnType = Stub() {
            isAssignable(org.reactivestreams.Publisher) >> true
            isAssignable(java.util.concurrent.CompletionStage) >> true
            getName() >> 'org.reactivestreams.Publisher'
        }
        MethodElement method = Stub() {
            isSuspend() >> false
            getGenericReturnType() >> returnType
        }

        expect:
        rule.validate([], [] as io.micronaut.inject.ast.ParameterElement[], method).valid
    }
}
