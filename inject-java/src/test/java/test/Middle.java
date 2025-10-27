package test;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import test.another.Base;

public class Middle extends Base {
    public boolean middle;
    @Inject
    @ReflectiveAccess
    void injectPackagePrivateMethod() {
        middle = true;
    }
}
