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

import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.Experimental;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.io.buffer.ByteBufferFactory;
import io.micronaut.core.type.Argument;
import io.micronaut.http.MediaType;
import io.micronaut.runtime.ApplicationConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * {@link MessageBodyHandlerRegistry} implementation that does not need an application context.
 *
 * @author Jonas Konrad
 * @since 4.0.0
 */
@Internal
@Experimental
public final class ContextlessMessageBodyHandlerRegistry extends AbstractMessageBodyHandlerRegistry {
    private final List<MessageBodyReaderDefinition<?>> readers = new ArrayList<>();
    private final List<MessageBodyWriterDefinition<?>> writers = new ArrayList<>();
    private final List<TypedMessageBodyReader<?>> typedMessageBodyReaders;
    private final List<TypedMessageBodyWriter<?>> typedMessageBodyWriters;

    /**
     * @param applicationConfiguration The configuration
     * @param byteBufferFactory        The buffer factory
     * @param otherRawHandlers         Raw handlers to add on top of the default ones
     */
    public ContextlessMessageBodyHandlerRegistry(ApplicationConfiguration applicationConfiguration,
                                                 ByteBufferFactory<?, ?> byteBufferFactory,
                                                 TypedMessageBodyHandler<?>... otherRawHandlers) {
        this.typedMessageBodyReaders = new ArrayList<>(3 + otherRawHandlers.length);
        this.typedMessageBodyReaders.add(new StringBodyReader(applicationConfiguration));
        this.typedMessageBodyReaders.add(new ByteArrayBodyHandler());
        this.typedMessageBodyReaders.add(new ByteBufferBodyHandler(byteBufferFactory));
        this.typedMessageBodyWriters = new ArrayList<>(3 + otherRawHandlers.length);
        this.typedMessageBodyWriters.add(new CharSequenceBodyWriter(applicationConfiguration));
        this.typedMessageBodyWriters.add(new ByteArrayBodyHandler());
        this.typedMessageBodyWriters.add(new ByteBufferBodyHandler(byteBufferFactory));
        for (TypedMessageBodyHandler<?> otherRawHandler : otherRawHandlers) {
            this.typedMessageBodyReaders.add(otherRawHandler);
            this.typedMessageBodyWriters.add(otherRawHandler);
        }
        add(MediaType.TEXT_PLAIN_TYPE, new TextPlainObjectBodyReader<>(applicationConfiguration, ConversionService.SHARED));
        add(MediaType.TEXT_PLAIN_TYPE, new TextPlainObjectBodyWriter());
    }

    /**
     * Add a {@link MessageBodyHandler} for the given media type.
     *
     * @param mediaType The media type the handler applies to
     * @param handler   The handler
     */
    public void add(@NonNull MediaType mediaType, @NonNull MessageBodyHandler<?> handler) {
        writers.add(new MessageBodyWriterDefinition<>(handler, mediaType, AnnotationMetadata.EMPTY_METADATA));
        readers.add(new MessageBodyReaderDefinition<>(handler, mediaType, AnnotationMetadata.EMPTY_METADATA));
    }

    /**
     * Add a {@link MessageBodyWriter} for the given media type.
     *
     * @param mediaType The media type the handler applies to
     * @param handler   The handler
     */
    public void add(@NonNull MediaType mediaType, @NonNull MessageBodyWriter<?> handler) {
        writers.add(new MessageBodyWriterDefinition<>(handler, mediaType, AnnotationMetadata.EMPTY_METADATA));
    }

    /**
     * Add a {@link MessageBodyReader} for the given media type.
     *
     * @param mediaType The media type the handler applies to
     * @param handler   The handler
     */
    public void add(@NonNull MediaType mediaType, @NonNull MessageBodyReader<?> handler) {
        readers.add(new MessageBodyReaderDefinition<>(handler, mediaType, AnnotationMetadata.EMPTY_METADATA));
    }

    @Override
    @NonNull
    protected <T> List<MessageBodyReaderDefinition<T>> findReaderImpl(Argument<T> type, MediaType mediaType) {
        Stream<MessageBodyReaderDefinition<T>> typedStream = typedMessageBodyReaders.stream()
            .filter(typed -> {
                TypedMessageBodyReader<T> reader = (TypedMessageBodyReader<T>) typed;
                return type.getType().isAssignableFrom(reader.getType().getType()) && reader.isReadable(type, mediaType);
            })
            .map(typed -> new MessageBodyReaderDefinition<>((TypedMessageBodyReader<T>) typed, mediaType, AnnotationMetadata.EMPTY_METADATA));
        Stream<MessageBodyReaderDefinition<T>> readersStream = readers.stream()
            .filter(reader -> mediaType.matches(reader.mediaType()))
            .map(reader -> (MessageBodyReaderDefinition<T>) reader);
        return Stream.concat(typedStream, readersStream).toList();
    }

    @Override
    @NonNull
    protected <T> List<MessageBodyWriterDefinition<T>> findWriterImpl(Argument<T> type, MediaType mediaType) {
        Stream<MessageBodyWriterDefinition<T>> typedStream = typedMessageBodyWriters
            .stream().filter(typed -> {
                TypedMessageBodyWriter<T> writer = (TypedMessageBodyWriter<T>) typed;
                return typed.getType().isAssignableFrom(type.getType()) && writer.isWriteable(type, mediaType);
            })
            .map(typed -> new MessageBodyWriterDefinition<>((MessageBodyWriter<T>) typed, mediaType, AnnotationMetadata.EMPTY_METADATA));
        Stream<MessageBodyWriterDefinition<T>> writersStream = writers.stream()
            .filter(writer -> mediaType.matches(writer.mediaType()))
            .map(writer -> (MessageBodyWriterDefinition<T>) writer);
        return Stream.concat(typedStream, writersStream).toList();
    }

}
