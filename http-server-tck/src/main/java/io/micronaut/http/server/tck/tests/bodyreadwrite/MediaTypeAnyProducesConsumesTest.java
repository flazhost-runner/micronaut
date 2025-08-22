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
package io.micronaut.http.server.tck.tests.bodyreadwrite;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.Headers;
import io.micronaut.core.type.MutableHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.body.MessageBodyReader;
import io.micronaut.http.body.MessageBodyWriter;
import io.micronaut.http.codec.CodecException;
import io.micronaut.http.tck.AssertionUtils;
import io.micronaut.http.tck.HttpResponseAssertion;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import static io.micronaut.http.tck.TestScenario.asserts;

@SuppressWarnings({
    "java:S5960", // We're allowed assertions, as these are used in tests only
    "checkstyle:MissingJavadocType",
    "checkstyle:DesignForExtension"
})
public class MediaTypeAnyProducesConsumesTest {
    public static final String SPEC_NAME = "MediaTypeAnyProducesConsumesTest";

    @Test
    void textPlain() throws IOException {
        asserts(SPEC_NAME,
            HttpRequest.POST("/myController", "test").contentType(MediaType.TEXT_PLAIN).accept(MediaType.TEXT_PLAIN),
            (server, request) -> AssertionUtils.assertDoesNotThrow(server, request, HttpResponseAssertion.builder()
                .status(HttpStatus.OK)
                .body("test|read-type:text/plain|write-type:text/plain")
                .build()));
    }

    @Test
    void textHtml() throws IOException {
        asserts(SPEC_NAME,
            HttpRequest.POST("/myController", "test").contentType(MediaType.TEXT_HTML_TYPE).accept(MediaType.TEXT_HTML_TYPE),
            (server, request) -> AssertionUtils.assertDoesNotThrow(server, request, HttpResponseAssertion.builder()
                .status(HttpStatus.OK)
                .body("test|read-type:text/*|write-type:text")
                .build()));
    }

    @Test
    void textXml() throws IOException {
        asserts(SPEC_NAME,
            HttpRequest.POST("/myController", "test").contentType(MediaType.TEXT_XML_TYPE).accept(MediaType.TEXT_XML_TYPE),
            (server, request) -> AssertionUtils.assertDoesNotThrow(server, request, HttpResponseAssertion.builder()
                .status(HttpStatus.OK)
                .body("test|read-type:text/*|write-type:text")
                .build()));
    }

    @Test
    void textXmlInApplicationOut() throws IOException {
        asserts(SPEC_NAME,
            HttpRequest.POST("/myController", "test").contentType(MediaType.TEXT_HTML_TYPE).accept(MediaType.APPLICATION_GRAPHQL),
            (server, request) -> AssertionUtils.assertDoesNotThrow(server, request, HttpResponseAssertion.builder()
                .status(HttpStatus.OK)
                .body("test|read-type:text/*")
                .build()));
    }

    @Test
    void any() throws IOException {
        asserts(SPEC_NAME,
            HttpRequest.POST("/myController", "test").contentType(MediaType.APPLICATION_GRAPHQL).accept(MediaType.APPLICATION_GRAPHQL),
            (server, request) -> AssertionUtils.assertDoesNotThrow(server, request, HttpResponseAssertion.builder()
                .status(HttpStatus.OK)
                .body("test")
                .build()));
    }

    @Test
    void any2() throws IOException {
        asserts(SPEC_NAME,
            HttpRequest.POST("/myController", "test"),
            (server, request) -> AssertionUtils.assertDoesNotThrow(server, request, HttpResponseAssertion.builder()
                .status(HttpStatus.OK)
                .body("test")
                .build()));
    }

    @Controller("/myController")
    @Requires(property = "spec.name", value = SPEC_NAME)
    static class MyController {

        @Post
        @Produces(MediaType.ALL)
        @Consumes(MediaType.ALL)
        StringBean method(@Body StringBean foobar) {
            return foobar;
        }

    }

    @Singleton
    @Consumes("text/*")
    @Produces("text/*")
    static class TextWildCardStringBeanEntityProvider
        extends StringBeanEntityProvider {

        @Override
        public StringBean read(Argument<StringBean> type, MediaType mediaType, Headers httpHeaders, InputStream inputStream) throws CodecException {
            StringBean bean = super.read(type, mediaType, httpHeaders, inputStream);
            bean.set(bean.get() + "|read-type:text/*");
            return bean;
        }

        @Override
        public void writeTo(Argument<StringBean> type, MediaType mediaType, StringBean bean, MutableHeaders outgoingHeaders, OutputStream outputStream) throws CodecException {
            super.writeTo(type, mediaType, bean, outgoingHeaders, outputStream);
            try {
                outputStream.write("|write-type:text/*".getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Singleton
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    static class TextPlainCardStringBeanEntityProvider
        extends StringBeanEntityProvider {

        @Override
        public StringBean read(Argument<StringBean> type, MediaType mediaType, Headers httpHeaders, InputStream inputStream) throws CodecException {
            StringBean bean = super.read(type, mediaType, httpHeaders, inputStream);
            bean.set(bean.get() + "|read-type:text/plain");
            return bean;
        }

        @Override
        public void writeTo(Argument<StringBean> type, MediaType mediaType, StringBean bean, MutableHeaders outgoingHeaders, OutputStream outputStream) throws CodecException {
            super.writeTo(type, mediaType, bean, outgoingHeaders, outputStream);
            try {
                outputStream.write(("|write-type:text/plain").getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    @Singleton
    static class StringBeanEntityProvider implements MessageBodyReader<StringBean>, MessageBodyWriter<StringBean> {

        @Override
        public StringBean read(Argument<StringBean> type, MediaType mediaType, Headers httpHeaders, InputStream inputStream) throws CodecException {
            try {
                return new StringBean(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void writeTo(Argument<StringBean> type, MediaType mediaType, StringBean bean, MutableHeaders outgoingHeaders, OutputStream outputStream) throws CodecException {
            try {
                outputStream.write(bean.get().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static final class StringBean {

        private String value;

        public StringBean(String value) {
            this.value = value;
        }

        public void set(String value) {
            this.value = value;
        }

        public String get() {
            return value;
        }
    }

}
