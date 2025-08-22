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
package io.micronaut.http.body;

import io.micronaut.context.BeanContext;
import io.micronaut.context.BeanRegistration;
import io.micronaut.context.BeanResolutionContext;
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.core.annotation.Experimental;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.type.Argument;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.codec.CodecConfiguration;
import io.micronaut.http.codec.MediaTypeCodec;
import io.micronaut.inject.BeanType;
import io.micronaut.inject.qualifiers.FilteringQualifier;
import io.micronaut.inject.qualifiers.MatchArgumentQualifier;
import io.micronaut.inject.qualifiers.Qualifiers;
import jakarta.inject.Singleton;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Stores message body readers and writers.
 *
 * @author Graeme Rocher
 * @since 4.0.0
 */
@SuppressWarnings("unused")
@Experimental
@Singleton
@BootstrapContextCompatible
public final class DefaultMessageBodyHandlerRegistry extends AbstractMessageBodyHandlerRegistry {
    private final BeanContext beanLocator;
    private final List<CodecConfiguration> codecConfigurations;

    /**
     * Default constructor.
     *
     * @param beanLocator         The bean locator.
     * @param codecConfigurations The codec configurations
     */
    DefaultMessageBodyHandlerRegistry(BeanContext beanLocator,
                                      List<CodecConfiguration> codecConfigurations) {
        this.beanLocator = beanLocator;
        this.codecConfigurations = codecConfigurations;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    @NonNull
    protected <T> List<MessageBodyReaderDefinition<T>> findReaderImpl(@NonNull Argument<T> type, @NonNull MediaType mediaType) {
        return beanLocator.getBeanRegistrations(
                Argument.of(MessageBodyReader.class), // Select all readers and eliminate by the type later
                Qualifiers.byQualifiers(
                    MatchArgumentQualifier.covariant(MessageBodyReader.class, type),
                    new MediaTypeQualifier<>(Argument.of(MessageBodyReader.class, type), mediaType, Consumes.class)
                )
            ).stream()
            .filter(reg -> reg.bean().isReadable(type, mediaType))
            .map(reg -> new MessageBodyReaderDefinition<T>(reg.getBean(), mediaType, reg.definition().getAnnotationMetadata()))
            .toList();
    }


    @NonNull
    @Override
    protected void resolveMediaType(MediaType mediaType, Consumer<MediaType> mediaTypeConsumer) {
        mediaTypeConsumer.accept(mediaType);
        if (codecConfigurations.isEmpty()) {
            return;
        }
        for (CodecConfiguration codecConfiguration : codecConfigurations) {
            List<MediaType> additionalTypes = codecConfiguration.getAdditionalTypes();
            if (additionalTypes.contains(mediaType)) {
                beanLocator.findBean(MediaTypeCodec.class, Qualifiers.byName(codecConfiguration.getName()))
                    .stream()
                    .flatMap(c -> c.getMediaTypes().stream())
                    .forEach(mediaTypeConsumer);
                break;
            }
        }
    }

    @SuppressWarnings({"unchecked"})
    @Override
    @NonNull
    protected <T> List<MessageBodyWriterDefinition<T>> findWriterImpl(@NonNull Argument<T> type, @NonNull MediaType mediaType) {
        return beanLocator.getBeanRegistrations(
                Argument.of(MessageBodyWriter.class), // Select all writers and eliminate by the type later
                Qualifiers.byQualifiers(
                    MatchArgumentQualifier.contravariant(MessageBodyWriter.class, type),
                    // Filter by media types first before filtering by the type hierarchy
                    new MediaTypeQualifier<>(Argument.of(MessageBodyWriter.class, type), mediaType, Produces.class)
                )
            ).stream()
            .filter(reg -> reg.bean().isWriteable(type, mediaType))
            .map(reg -> new MessageBodyWriterDefinition<T>(reg.getBean(), mediaType, reg.definition().getAnnotationMetadata()))
            .toList();
    }

    private static final class MediaTypeQualifier<T> extends FilteringQualifier<T> {
        private final Argument<?> type;
        private final MediaType mediaType;
        private final Class<? extends Annotation> annotationType;
        private static final List<MediaType> ALL_MATCH = List.of(MediaType.ALL_TYPE);

        private MediaTypeQualifier(@NonNull Argument<?> type,
                                   @NonNull MediaType mediaType,
                                   @NonNull Class<? extends Annotation> annotationType) {
            this.mediaType = Objects.requireNonNull(mediaType, "MediaType must not be null");
            this.type = type;
            this.annotationType = annotationType;
        }

        @Override
        public <K extends BeanType<T>> Collection<K> filter(Class<T> beanType, Collection<K> candidates) {
            return DefaultMessageBodyHandlerRegistry.filterByMediaType(mediaType, annotationType.getTypeName(), candidates);
        }

        record Item<K>(K candidate, MediaType mediaType) implements Comparable<Item<K>> {
            @Override
            public int compareTo(Item<K> o) {
                return MediaType.naturalSort(mediaType, o.mediaType);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            MediaTypeQualifier<?> that = (MediaTypeQualifier<?>) o;
            return type.equalsType(that.type) && mediaType.equals(that.mediaType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type.typeHashCode(), mediaType);
        }

        @Override
        public String toString() {
            return "MediaTypeQualifier[" +
                "type=" + type + ", " +
                "mediaType=" + mediaType + ", " +
                "annotationType=" + annotationType + ']';
        }

    }

    private record BeanRegistrationHolder<T>(
        BeanRegistration<T> registration) implements BeanType<T> {
        @Override
        public Class<T> getBeanType() {
            return registration.getBeanType();
        }

        @Override
        public boolean isEnabled(BeanContext context, BeanResolutionContext resolutionContext) {
            return true;
        }
    }
}
