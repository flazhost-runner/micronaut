package io.micronaut.http.client;
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.http.ssl.ClientSslConfiguration;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(startApplication = false)
public class HostnameVerificationTest {
    @Inject
    BeanContext beanContext;
    @Test
    @Property(name = "micronaut.http.client.ssl.disable-hostname-verification", value = "true")
    void testSslConfigurationWithHostnameVerificationDisabled() {
        ClientSslConfiguration sslConfig = beanContext.getBean(ClientSslConfiguration.class);
        System.out.println("disable-hostname-verification = " + sslConfig.isDisableHostnameVerification());
        assertTrue(sslConfig.isDisableHostnameVerification(),
            "Expected hostname verification to be disabled");
    }

    @Test
    @Property(name = "micronaut.http.client.ssl.disable-hostname-verification", value = "false")
    void testSslConfigurationWithHostnameVerificationEnabled() {
        ClientSslConfiguration sslConfig = beanContext.getBean(ClientSslConfiguration.class);
        System.out.println("disable-hostname-verification = " + sslConfig.isDisableHostnameVerification());
        assertFalse(sslConfig.isDisableHostnameVerification(),
            "Expected hostname verification to be enabled");
    }

}
