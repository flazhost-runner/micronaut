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

import io.micronaut.http.annotation.ContentDisposition;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import reactor.core.publisher.Flux;

/**
 * Controller responsible for handling report generation requests.
 * Provides an endpoint to generate and download a CSV report.
 */
@Controller("/report")
public class ReportController {

    /**
     * Generates a CSV report based on the provided query parameters.
     * The report is returned as a downloadable file with the `Content-Disposition` header.
     *
     * @return A Flux emitting the rows of the CSV report.
     */
    @Get("/result{?values*}")
    @ContentDisposition("attachment; filename=report.csv")
    public Flux<String> result() {
        return Flux.just("Header1,Header2", "Value1,Value2");
    }
}
