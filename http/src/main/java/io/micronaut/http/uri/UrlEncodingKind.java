/*
 * Copyright 2017-2025 original authors
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
package io.micronaut.http.uri;

import io.micronaut.core.annotation.NonNull;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/**
 * The kind of URL encoding to apply.
 *
 * @since 4.8.0
 */
public enum UrlEncodingKind {
    /**
     * Form encoding ({@code application/ x-www-form-urlencoded}) as per {@link java.net.URLEncoder}.
     */
    FORM_URLENCODED((str) -> URLEncoder.encode(str, StandardCharsets.UTF_8)),

    /**
     * Encoding compatible with RFC_3986 (https://www.rfc-editor.org/rfc/rfc3986#page-13).
     */
    RFC_3986(RFC3986UrlEncoder::encode);

    private final Function<String, String> encoder;

    UrlEncodingKind(Function<String, String> encoder) {
        this.encoder = encoder;
    }

    /**
     * Encode the string.
     * @param str The string
     * @return The encoded string
     */
    public @NonNull String encode(@NonNull String str) {
        return encoder.apply(str);
    }
}
