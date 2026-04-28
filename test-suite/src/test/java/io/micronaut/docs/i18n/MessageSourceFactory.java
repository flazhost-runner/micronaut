package io.micronaut.docs.i18n;

//tag::imports[]
import io.micronaut.context.MessageSource;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.i18n.ResourceBundleMessageSource;
import jakarta.inject.Singleton;
//end::imports[]

@Requires(property = "spec.name", value = "I18nSpec")
//tag::clazz[]
@Factory
class MessageSourceFactory {
    @Singleton
    MessageSource createMessageSource() {
        return new ResourceBundleMessageSource("io.micronaut.docs.i18n.messages");
    }
}
//end::clazz[]