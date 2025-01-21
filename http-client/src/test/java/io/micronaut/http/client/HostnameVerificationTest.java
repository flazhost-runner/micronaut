package io.micronaut.http.client;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.ssl.ClientSslConfiguration;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(startApplication = false)
class HostnameVerificationTest {
    @Test
    @Property(name = "micronaut.http.client.ssl.disable-hostname-verification", value = StringUtils.TRUE)
    void testSslConfigurationWithHostnameVerificationDisabled(ClientSslConfiguration sslConfig) {
        assertTrue(sslConfig.isDisableHostnameVerification(),
            "Expected hostname verification to be disabled");
    }

    @Test
    void testSslConfigurationWithHostnameVerificationDisabledByDefault(ClientSslConfiguration sslConfig) {
        assertFalse(sslConfig.isDisableHostnameVerification(),
            "Expected hostname verification to be enabled");
    }

    @Test
    @Property(name = "micronaut.http.client.ssl.disable-hostname-verification", value = StringUtils.FALSE)
    void testSslConfigurationWithHostnameVerificationEnabled(ClientSslConfiguration sslConfig) {
        assertFalse(sslConfig.isDisableHostnameVerification(),
            "Expected hostname verification to be enabled");
    }

}
