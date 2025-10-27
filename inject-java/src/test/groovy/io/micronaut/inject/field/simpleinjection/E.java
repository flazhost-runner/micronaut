package io.micronaut.inject.field.simpleinjection;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Value;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.annotation.Nullable;

public class E {
    @Nullable
    @Value("${greeting}")
    @ReflectiveAccess
    private String value = "Default greeting";

    @Nullable
    @Property(name = "greeting")
    @ReflectiveAccess
    private String property = "Default greeting";

    String getValue() {
        return value;
    }

    String getProperty() {
        return property;
    }
}
