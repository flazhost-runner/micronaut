package io.micronaut.http.server.tck.netty.tests;

import io.micronaut.core.propagation.PropagatedContextConfiguration;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;
import org.junit.platform.suite.api.*;

@Suite
@SelectPackages("io.micronaut.http.server.tck.tests")
@SuiteDisplayName("HTTP Server TCK for Netty")
// fails on native
    @ExcludeClassNamePatterns("io.micronaut.http.server.tck.tests.staticresources.StaticResourceTest")
public class NettyHttpServerTestSuite {

    static {
        PropagatedContextConfiguration.set(PropagatedContextConfiguration.Mode.SCOPED_VALUE);
    }

}
