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
import io.micronaut.core.annotation.AnnotationMetadataProvider;
import io.micronaut.core.annotation.Experimental;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.Headers;
import io.micronaut.core.type.MutableHeaders;
import io.micronaut.core.util.ObjectUtils;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.codec.CodecException;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Base class for {@link MessageBodyHandlerRegistry} that handles caching and exposes the raw
 * handlers (String, byte[] and such).
 *
 * @author Graeme Rocher
 * @since 4.0.0
 */
@Internal
@Experimental
abstract sealed class AbstractMessageBodyHandlerRegistry implements MessageBodyHandlerRegistry permits ContextlessMessageBodyHandlerRegistry, DefaultMessageBodyHandlerRegistry {
    private final Map<HandlerKey<?>, List<MessageBodyReaderDefinition<?>>> readers = new ConcurrentHashMap<>(10);
    private final Map<HandlerKey<?>, List<MessageBodyWriterDefinition<?>>> writers = new ConcurrentHashMap<>(10);

    protected void resolveMediaType(MediaType mediaType, Consumer<MediaType> mediaTypeConsumer) {
        mediaTypeConsumer.accept(mediaType);
    }

    @Nullable
    protected abstract <T> List<MessageBodyReaderDefinition<T>> findReaderImpl(@NonNull Argument<T> type, @NonNull MediaType mediaTypes);

    @Override
    public <T> Optional<MessageBodyReader<T>> findReader(Argument<T> type, List<MediaType> mediaTypes) {
        if (mediaTypes.isEmpty()) {
            mediaTypes = List.of(MediaType.ALL_TYPE);
        }
        return resolveReader(type, mediaTypes.stream());
    }

    private <T> Optional<MessageBodyReader<T>> resolveReader(Argument<T> type, Stream<MediaType> mediaTypes) {
        List<MessageBodyReaderDefinition<T>> readers = mediaTypes
            .mapMulti(this::resolveMediaType)
            .flatMap(mt -> findReadersInternal(type, mt).stream())
            .toList();
        if (readers.isEmpty()) {
            return Optional.empty();
        }
        if (readers.size() == 1) {
            return Optional.of(readers.get(0).reader());
        }
        return Optional.of(multipleCandidatesMessageBodyReader(readers));
    }

    @Override
    public <T> Optional<MessageBodyReader<T>> findReader(Argument<T> type, MediaType mediaType) {
        if (mediaType == null) {
            mediaType = MediaType.ALL_TYPE;
        }
        return resolveReader(type, Stream.of(mediaType));
    }

    private <T> Map<HandlerKey<T>, List<MessageBodyReaderDefinition<T>>> readersCache() {
        return (Map) readers; // safe cast
    }

    @NonNull
    private <T> List<MessageBodyReaderDefinition<T>> findReadersInternal(Argument<T> type, MediaType mediaType) {
        HandlerKey<T> key = new HandlerKey<>(type, mediaType);
        Map<HandlerKey<T>, List<MessageBodyReaderDefinition<T>>> readersCache = readersCache();
        List<MessageBodyReaderDefinition<T>> existingReaders = null;
//        List<MessageBodyReaderDefinition<T>> existingReaders = readersCache.get(key);
        if (existingReaders == null) {
            List<MessageBodyReaderDefinition<T>> readers = findReaderImpl(type, mediaType);
            if (readers == null) {
                readers = List.of();
            }
            readersCache.put(key, readers);
            return readers;
        }
        return existingReaders;
    }

    @Nullable
    protected abstract <T> List<MessageBodyWriterDefinition<T>> findWriterImpl(@NonNull Argument<T> type, @NonNull MediaType mediaType);

    @Override
    public <T> Optional<MessageBodyWriter<T>> findWriter(Argument<T> type, List<MediaType> mediaTypes) {
        if (mediaTypes.isEmpty()) {
            mediaTypes = List.of(MediaType.ALL_TYPE);
        }
        Stream<MediaType> stream = mediaTypes.stream();
        return resolveWriter(type, stream);
    }

    private <T> Optional<MessageBodyWriter<T>> resolveWriter(Argument<T> type, Stream<MediaType> stream) {
        List<MessageBodyWriterDefinition<T>> writers = stream
            .mapMulti(this::resolveMediaType)
            .flatMap(mt -> findWriterInternal(type, mt).stream())
            .toList();
        if (writers.isEmpty()) {
            return Optional.empty();
        }
        if (writers.size() == 1) {
            return Optional.of(writers.get(0).writer());
        }
        return Optional.of(multipleCandidatesMessageBodyWriter(writers));
    }

    @Override
    public <T> Optional<MessageBodyWriter<T>> findWriter(Argument<T> type, MediaType mediaType) {
        if (mediaType == null) {
            mediaType = MediaType.ALL_TYPE;
        }
        return resolveWriter(type, Stream.of(mediaType));
    }

    private <T> Map<HandlerKey<T>, List<MessageBodyWriterDefinition<T>>> writersCache() {
        return (Map) writers; // safe cast
    }

    @NonNull
    private <T> List<MessageBodyWriterDefinition<T>> findWriterInternal(@NonNull Argument<T> type, @NonNull MediaType mediaType) {
        if (type.getType() == Object.class) {
            return List.of();
        }
        HandlerKey<T> key = new HandlerKey<>(type, mediaType);
        Map<HandlerKey<T>, List<MessageBodyWriterDefinition<T>>> writersCache = writersCache();
//        List<MessageBodyWriterDefinition<T>> existingWriters = writersCache.get(key);
        List<MessageBodyWriterDefinition<T>> existingWriters = null;
        if (existingWriters == null) {
            List<MessageBodyWriterDefinition<T>> writers = findWriterImpl(type, mediaType);
            if (writers == null) {
                writers = List.of();
            }
            writersCache.put(key, writers);
            return writers;
        }
        return existingWriters;
    }

    protected static <K extends AnnotationMetadataProvider> Collection<K> filterByMediaType(MediaType mediaType, String annotationType, Collection<K> candidates) {
        List<MediaTypeCandidate<K>> mediaTypeMatch = new ArrayList<>(candidates.size());
        boolean visitedApplicableTypes = false;
        candidatesLoop:
        for (K candidate : candidates) {
            String[] applicableTypes = candidate.getAnnotationMetadata().stringValues(annotationType);
            if (applicableTypes.length == 0) {
                mediaTypeMatch.add(new MediaTypeCandidate<>(candidate, MediaType.ALL_TYPE));
                continue;
            }
            visitedApplicableTypes = true;
            List<MediaType> applicableMediaTypes = MediaType.orderedOf(applicableTypes);
            for (MediaType applicableMediaType : applicableMediaTypes) {
                // Applicable types and required media types should be now ordered by priority
                if (mediaType.equals(MediaType.ALL_TYPE) || applicableMediaType.matches(mediaType)) {
                    // Handlers with a media type defined should have a priority
                    mediaTypeMatch.add(new MediaTypeCandidate<>(candidate, applicableMediaType));
                    continue candidatesLoop;
                }
                // Skip handlers that define a media type that doesn't match
            }
        }
        if (!visitedApplicableTypes) {
            return candidates;
        }
        // Last sort to find the closest media type
        mediaTypeMatch.sort(null);
        List<K> list = new ArrayList<>(mediaTypeMatch.size());
        for (MediaTypeCandidate<K> i : mediaTypeMatch) {
            list.add(i.candidate);
        }
        return list;
    }

    private <T> MessageBodyReader<T> multipleCandidatesMessageBodyReader(List<MessageBodyReaderDefinition<T>> candidates) {
        candidates = candidates.stream()
            .flatMap(c -> {
                if (c.reader instanceof MultipleMessageBodyReader<T> mc) {
                    return mc.candidates().stream();
                }
                return Stream.of(c);
            })
            .toList();
        return new MultipleMessageBodyReader<>(candidates);
    }

    private <T> MessageBodyWriter<T> multipleCandidatesMessageBodyWriter(List<MessageBodyWriterDefinition<T>> candidates) {
        candidates = candidates.stream()
            .flatMap(c -> {
                if (c.writer instanceof MultipleMessageBodyWriter<T> mc) {
                    return mc.candidates().stream();
                }
                return Stream.of(c);
            })
            .toList();
        return new MultipleMessageBodyWriter<>(candidates);
    }

    private record MediaTypeCandidate<K>(K candidate,
                                         MediaType mediaType) implements Comparable<MediaTypeCandidate<K>> {
        @Override
        public int compareTo(MediaTypeCandidate<K> o) {
            return MediaType.naturalSort(mediaType, o.mediaType);
        }
    }


    /**
     * A record that defines a MessageBodyWriter along with its associated media type and annotation metadata.
     *
     * @param <T> The type of the object that the MessageBodyWriter can serialize or write.
     * @since 4.10
     */
    protected record MessageBodyWriterDefinition<T>(MessageBodyWriter<T> writer,
                                                    MediaType mediaType,
                                                    AnnotationMetadata annotationMetadata)
        implements AnnotationMetadataProvider {
    }

    /**
     * A record that defines a MessageBodyReader along with its associated media type and annotation metadata.
     *
     * @param <T> The type of object that the MessageBodyReader can read
     * @since 4.10
     */
    protected record MessageBodyReaderDefinition<T>(MessageBodyReader<T> reader,
                                                    MediaType mediaType,
                                                    AnnotationMetadata annotationMetadata)
        implements AnnotationMetadataProvider {
    }

    private record HandlerKey<T>(@NonNull Argument<T> type, @NonNull MediaType mediaType) {
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            HandlerKey<?> that = (HandlerKey<?>) o;
            return type.equals(that.type) && mediaType.equals(that.mediaType);
        }

        @Override
        public int hashCode() {
            return ObjectUtils.hash(type.typeHashCode(), mediaType);
        }
    }

    private record MultipleMessageBodyReader<T>(
        List<MessageBodyReaderDefinition<T>> candidates) implements MessageBodyReader<T> {

        @Override
        public boolean isReadable(Argument<T> type, MediaType mediaType) {
            for (MessageBodyReaderDefinition<T> candidate : candidates) {
                if (candidate.reader.isReadable(type, mediaType)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public T read(Argument<T> type, MediaType mediaType, Headers httpHeaders, InputStream inputStream) throws CodecException {
            return findReader(type, mediaType)
                .read(type, mediaType, httpHeaders, inputStream);
        }

        private MessageBodyReader<T> findReader(Argument<T> type, MediaType mediaType) {
            return filterByMediaType(mediaType, Consumes.class.getName(), candidates)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Cannot select a reader for type:" + type + " and media type: " + mediaType))
                .reader();
        }
    }

    private record MultipleMessageBodyWriter<T>(
        List<MessageBodyWriterDefinition<T>> candidates) implements MessageBodyWriter<T> {

        @Override
        public MessageBodyWriter<T> createSpecific(Argument<T> type) {
            return new MultipleMessageBodyWriter<>(
                candidates.stream()
                    .map(c -> new MessageBodyWriterDefinition<>(c.writer.createSpecific(type), c.mediaType, c.annotationMetadata))
                    .toList()
            );
        }

        @Override
        public boolean isWriteable(Argument<T> type, MediaType mediaType) {
            for (MessageBodyWriterDefinition<T> candidate : candidates) {
                if (candidate.writer().isWriteable(type, mediaType)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void writeTo(Argument<T> type, MediaType mediaType, T object, MutableHeaders outgoingHeaders, OutputStream outputStream) throws CodecException {
            MessageBodyWriter<T> writer = findWriter(type, mediaType);
            writer
                .writeTo(type, mediaType, object, outgoingHeaders, outputStream);
        }

        private MessageBodyWriter<T> findWriter(Argument<T> type, MediaType mediaType) {
            return filterByMediaType(mediaType, Produces.class.getName(), candidates)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Cannot select a writer for type:" + type + " and media type: " + mediaType))
                .writer();
        }
    }
}
