/*
 * Copyright 2017-2022 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.http.ssl;

import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Nullable;
import jakarta.inject.Inject;

/**
 * The default {@link SslConfiguration} used for HTTP clients.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@ConfigurationProperties(ClientSslConfiguration.PREFIX)
@BootstrapContextCompatible
public class ClientSslConfiguration extends AbstractClientSslConfiguration {

    /**
     * The prefix used to resolve this configuration.
     */
    public static final String PREFIX = "micronaut.http.client.ssl";
    private boolean disableHostnameVerification;

    /**
     * Overrides the default constructor and sets {@link #isEnabled()} to true.
     *
     * @param defaultSslConfiguration The default SSL config
     * @param defaultKeyConfiguration The default key config
     * @param defaultKeyStoreConfiguration The default keystore config
     * @param defaultTrustStoreConfiguration The Default truststore config
     */
    @Inject
    public ClientSslConfiguration(
            DefaultSslConfiguration defaultSslConfiguration,
            DefaultSslConfiguration.DefaultKeyConfiguration defaultKeyConfiguration,
            DefaultSslConfiguration.DefaultKeyStoreConfiguration defaultKeyStoreConfiguration,
            SslConfiguration.TrustStoreConfiguration defaultTrustStoreConfiguration) {
        readExisting(defaultSslConfiguration, defaultKeyConfiguration, defaultKeyStoreConfiguration, defaultTrustStoreConfiguration);
        setEnabled(true);
    }

    /**
     * The default client configuration.
     */
    public ClientSslConfiguration() {
        setEnabled(true);
    }

    /**
     * Returns whether hostname verification is disabled for SSL connections.
     * <p>
     * When this setting is enabled, the client will skip the verification that the SSL certificate matches
     * the expected hostname. Disabling this feature exposes the application to security risks,
     * as it no longer verifies that the server's certificate matches the expected hostname.
     * <b>Important:</b> Only disable hostname verification in trusted environments, such as internal
     * testing or during debugging. In production environments, hostname verification should be enabled
     * to protect the integrity and confidentiality of communications.
     *
     * @since 4.8.0
     */
    public boolean isDisableHostnameVerification() {
        return disableHostnameVerification;
    }

    /**
     * Sets whether to disable hostname verification for SSL connections.
     * <p>
     * This setting controls the verification of the hostname during SSL handshake. Disabling it prevents
     * the client from verifying that the SSL certificate matches the hostname it connects to.
     * This may expose the application to security risks, where
     * an attacker could intercept or modify the data exchanged between the client and the server.
     * <b>Warning:</b> This setting should only be used in trusted environments, such as internal testing,
     * and should not be used in production systems.
     *
     * @since 4.8.0
     */
    public void setDisableHostnameVerification(boolean disableHostnameVerification) {
        this.disableHostnameVerification = disableHostnameVerification;
    }

    /**
     * Sets the key configuration.
     *
     * @param keyConfiguration The key configuration.
     */
    @Inject
    void setKey(@Nullable DefaultKeyConfiguration keyConfiguration) {
        if (keyConfiguration != null) {
            super.setKey(keyConfiguration);
        }
    }

    /**
     * Sets the key store.
     *
     * @param keyStoreConfiguration The key store configuration
     */
    @SuppressWarnings("unused")
    @Inject
    void setKeyStore(@Nullable DefaultKeyStoreConfiguration keyStoreConfiguration) {
        if (keyStoreConfiguration != null) {
            super.setKeyStore(keyStoreConfiguration);
        }
    }

    /**
     * Sets trust store configuration.
     *
     * @param trustStore The trust store configuration
     */
    @SuppressWarnings("unused")
    @Inject
    void setTrustStore(@Nullable DefaultTrustStoreConfiguration trustStore) {
        if (trustStore != null) {
            super.setTrustStore(trustStore);
        }
    }

    /**
     * The default {@link io.micronaut.http.ssl.SslConfiguration.KeyConfiguration}.
     */
    @SuppressWarnings("WeakerAccess")
    @ConfigurationProperties(KeyConfiguration.PREFIX)
    @BootstrapContextCompatible
    @Requires(property = ClientSslConfiguration.PREFIX + "." + KeyConfiguration.PREFIX)
    public static class DefaultKeyConfiguration extends KeyConfiguration {
    }

    /**
     * The default {@link io.micronaut.http.ssl.SslConfiguration.KeyStoreConfiguration}.
     */
    @SuppressWarnings("WeakerAccess")
    @ConfigurationProperties(KeyStoreConfiguration.PREFIX)
    @BootstrapContextCompatible
    @Requires(property = ClientSslConfiguration.PREFIX + "." + KeyStoreConfiguration.PREFIX)
    public static class DefaultKeyStoreConfiguration extends KeyStoreConfiguration {
    }

    /**
     * The default {@link io.micronaut.http.ssl.SslConfiguration.TrustStoreConfiguration}.
     */
    @SuppressWarnings("WeakerAccess")
    @ConfigurationProperties(TrustStoreConfiguration.PREFIX)
    @BootstrapContextCompatible
    @Requires(property = ClientSslConfiguration.PREFIX + "." + TrustStoreConfiguration.PREFIX)
    public static class DefaultTrustStoreConfiguration extends TrustStoreConfiguration {
    }
}
