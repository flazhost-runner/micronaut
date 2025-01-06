/*
 * Copyright 2017-2023 original authors
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
package io.micronaut.http.filter;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

/**
 * A filter that adds the `Content-Disposition` header to HTTP responses for specific endpoints.
 */
@Singleton
@Filter("/**")
public class ContentDispositionFilter implements HttpServerFilter {

    /**
     * Applies the filter logic to the HTTP request and modifies the response if the request
     * matches specific criteria.
     *
     * @param request The incoming HTTP request.
     * @param chain   The filter chain to proceed with the request.
     * @return A publisher that emits the modified or unmodified HTTP response.
     */
    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        if (request.getPath().startsWith("/report/result")) {
            return Flux.from(chain.proceed(request))
                .map(response -> {
                    MutableHttpResponse<?> mutableResponse = response;
                    mutableResponse.getHeaders().add("Content-Disposition", "attachment; filename=report.csv");
                    return mutableResponse;
                });
        }

        return chain.proceed(request);
    }
}
