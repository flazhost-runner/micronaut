package io.micronaut.inject.dependent

import io.micronaut.annotation.processing.test.AbstractTypeElementSpec
import io.micronaut.context.ApplicationContextBuilder
import io.micronaut.context.BeanRegistration
import io.micronaut.context.BeanResolutionContext
import io.micronaut.context.BeanResolutionCustomizer

class BeanResolutionCustomizerSpec extends AbstractTypeElementSpec {

    void "customizer can destroy dependent beans after the current resolution completes"() {
        given:
        def context = buildContext('test.Owner', '''
package test;

import io.micronaut.context.annotation.Prototype;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@interface DestroyAfterResolution {
}

@Prototype
class Helper {
    static final List<String> DESTROYED = new ArrayList<>();
    String owner;

    @PreDestroy
    void close() {
        DESTROYED.add(owner);
    }
}

@Singleton
class Owner {
    @Inject
    Owner(@DestroyAfterResolution Helper transientHelper, Helper normalHelper) {
        transientHelper.owner = "ctor-transient";
        normalHelper.owner = "ctor-normal";
    }

    @Inject
    void init(@DestroyAfterResolution Helper transientHelper, Helper normalHelper) {
        transientHelper.owner = "init-transient";
        normalHelper.owner = "init-normal";
    }
}
''')
        def owner = getBean(context, 'test.Owner')
        def helperClass = context.classLoader.loadClass('test.Helper')

        expect:
        helperClass.DESTROYED == ['ctor-transient', 'init-transient']

        when:
        context.destroyBean(owner)

        then:
        helperClass.DESTROYED.containsAll(['ctor-normal', 'init-normal'])

        cleanup:
        context.close()
    }

    @Override
    protected void configureContext(ApplicationContextBuilder contextBuilder) {
        contextBuilder.beanResolutionCustomizer(new BeanResolutionCustomizer() {
            @Override
            boolean shouldDestroyDependentBeanAfterResolution(BeanResolutionContext resolutionContext, BeanRegistration<?> beanRegistration) {
                return resolutionContext.path.currentSegment()
                    .map(segment -> segment.argument.annotationMetadata.hasAnnotation('test.DestroyAfterResolution'))
                    .orElse(false)
            }
        })
    }
}
