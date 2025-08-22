package io.micronaut.http.server.tck.netty.tests;

import io.micronaut.http.server.tck.tests.bodyreadwrite.WriteBodyInteractionsTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@SelectClasses(WriteBodyInteractionsTest.class)
@Suite
@SuiteDisplayName("HTTP Server TCK for Netty")
public class NettyHttpServerTestSuite {
}
