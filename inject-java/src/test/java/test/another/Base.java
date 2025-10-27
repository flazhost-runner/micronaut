package test.another;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;

public class Base {
    public boolean base;
    @Inject
    @ReflectiveAccess
    void injectPackagePrivateMethod() {
        base = true;
    }
}
