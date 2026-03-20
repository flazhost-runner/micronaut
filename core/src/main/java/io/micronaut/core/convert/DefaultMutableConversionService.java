/*
 * Copyright 2017-2020 original authors
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
package io.micronaut.core.convert;

import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.convert.converters.MultiValuesConverterFactory;
import io.micronaut.core.convert.exceptions.ConversionErrorException;
import io.micronaut.core.convert.format.Format;
import io.micronaut.core.convert.format.FormattingTypeConverter;
import io.micronaut.core.convert.format.ReadableBytes;
import io.micronaut.core.convert.format.ReadableBytesTypeConverter;
import io.micronaut.core.convert.value.ConvertibleValues;
import io.micronaut.core.convert.value.ConvertibleValuesMap;
import io.micronaut.core.io.IOUtils;
import io.micronaut.core.io.buffer.ReadBuffer;
import io.micronaut.core.io.buffer.ReferenceCounted;
import io.micronaut.core.io.service.SoftServiceLoader;
import io.micronaut.core.reflect.ClassUtils;
import io.micronaut.core.reflect.ReflectionUtils;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.ArrayUtils;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.CopyOnWriteMap;
import io.micronaut.core.util.ObjectUtils;
import io.micronaut.core.util.StringUtils;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static io.micronaut.core.reflect.ReflectionUtils.EMPTY_CLASS_ARRAY;

/**
 * The default conversion service. Handles basic type conversion operations.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class DefaultMutableConversionService implements MutableConversionService {

    private static final int CACHE_MAX = 256;
    private static final int CACHE_EVICTION_BATCH = 64;
    private static final TypeConverter UNCONVERTIBLE = (object, targetType, context) -> Optional.empty();

    private static final Map<Class<?>, List<Class<?>>> COMMON_TYPE_HIERARCHY = CollectionUtils.newHashMap(30);

    static {
        // Optimize common hierarchy scenarios
        COMMON_TYPE_HIERARCHY.put(String.class, List.of(String.class, CharSequence.class, Object.class));
        COMMON_TYPE_HIERARCHY.put(CharSequence.class, List.of(CharSequence.class, Object.class));
        COMMON_TYPE_HIERARCHY.put(Character.class, List.of(Character.class, Object.class));
        COMMON_TYPE_HIERARCHY.put(Number.class, List.of(Number.class, Object.class));
        COMMON_TYPE_HIERARCHY.put(Integer.class, List.of(Integer.class, Number.class, Object.class));
        COMMON_TYPE_HIERARCHY.put(Double.class, List.of(Double.class, Number.class, Object.class));
        COMMON_TYPE_HIERARCHY.put(Float.class, List.of(Float.class, Number.class, Object.class));
        COMMON_TYPE_HIERARCHY.put(Long.class, List.of(Long.class, Number.class, Object.class));
        COMMON_TYPE_HIERARCHY.put(Short.class, List.of(Short.class, Number.class, Object.class));
        COMMON_TYPE_HIERARCHY.put(Byte.class, List.of(Byte.class, Number.class, Object.class));
        COMMON_TYPE_HIERARCHY.put(BigInteger.class, List.of(BigInteger.class, Number.class, Object.class));
        COMMON_TYPE_HIERARCHY.put(BigDecimal.class, List.of(BigDecimal.class, Number.class, Object.class));
        COMMON_TYPE_HIERARCHY.put(Iterable.class, List.of(Iterable.class, Object.class));
        COMMON_TYPE_HIERARCHY.put(Collection.class, List.of(Collection.class, Iterable.class, Object.class));
        COMMON_TYPE_HIERARCHY.put(List.class, List.of(List.class, Collection.class, Iterable.class, Object.class));
        COMMON_TYPE_HIERARCHY.put(Set.class, List.of(Set.class, Collection.class, Iterable.class, Object.class));
        COMMON_TYPE_HIERARCHY.put(ArrayList.class, List.of(ArrayList.class, List.class, Collection.class, Iterable.class, Object.class));
        COMMON_TYPE_HIERARCHY.put(LinkedList.class, List.of(LinkedList.class, List.class, Collection.class, Iterable.class, Object.class));
        COMMON_TYPE_HIERARCHY.put(HashSet.class, List.of(HashSet.class, Set.class, Collection.class, Iterable.class, Object.class));
        COMMON_TYPE_HIERARCHY.put(LinkedHashSet.class, List.of(LinkedHashSet.class, Set.class, Collection.class, Iterable.class, Object.class));
        COMMON_TYPE_HIERARCHY.put(Map.class, List.of(Map.class, Object.class));
        COMMON_TYPE_HIERARCHY.put(HashMap.class, List.of(HashMap.class, Map.class, Object.class));
        COMMON_TYPE_HIERARCHY.put(LinkedHashMap.class, List.of(LinkedHashMap.class, Map.class, Object.class));
    }

    private final Map<ConvertiblePair, TypeConverter> internalConverters = CollectionUtils.newHashMap(300);
    private final Map<ConvertiblePair, TypeConverter> customConverters = new ConcurrentHashMap<>();
    private final Map<ConvertiblePair, TypeConverter> converterCache = new ConcurrentHashMap<>();

    private final MutableConversionService internalMutableConversionService = new MutableConversionService() {
        @Override
        public <S, T> void addConverter(Class<S> sourceType, Class<T> targetType, Function<S, T> typeConverter) {
            addInternalConverter(sourceType, targetType, typeConverter);
        }
        @Override
        public <S, T> void addConverter(Class<S> sourceType, Class<T> targetType, TypeConverter<S, T> typeConverter) {
            addInternalConverter(sourceType, targetType, typeConverter);
        }
        @Override
        public <T> Optional<T> convert(@Nullable Object object, Class<T> targetType, ConversionContext context) {
            return DefaultMutableConversionService.this.convert(object, targetType, context);
        }
        @Override
        public <S, T> Optional<T> convert(@Nullable S object, Class<? super S> sourceType, Class<T> targetType, ConversionContext context) {
            return DefaultMutableConversionService.this.convert(object, sourceType, targetType, context);
        }
        @Override
        public <S, T> boolean canConvert(Class<S> sourceType, Class<T> targetType) {
            return DefaultMutableConversionService.this.canConvert(sourceType, targetType);
        }
        @Override
        public <T> Optional<T> convert(@Nullable Object object, Class<T> targetType) {
            return DefaultMutableConversionService.this.convert(object, targetType);
        }
        @Override
        public <T> Optional<T> convert(@Nullable Object object, Argument<T> targetType) {
            return DefaultMutableConversionService.this.convert(object, targetType);
        }
        @Override
        public <T> Optional<T> convert(@Nullable Object object, ArgumentConversionContext<T> context) {
            return DefaultMutableConversionService.this.convert(object, context);
        }
        @Override
        public <T> T convertRequired(@Nullable Object value, Class<T> type) {
            return DefaultMutableConversionService.this.convertRequired(value, type);
        }
        @Override
        public <T> T convertRequired(@Nullable Object value, Argument<T> argument) {
            return DefaultMutableConversionService.this.convertRequired(value, argument);
        }
        @Override
        public <T> T convertRequired(@Nullable Object value, ArgumentConversionContext<T> context) {
            return DefaultMutableConversionService.this.convertRequired(value, context);
        }
    };

    public DefaultMutableConversionService() {
        registerDefaultConverters();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S, T> Optional<T> convert(@Nullable S object, Class<? super S> sourceType, Class<T> targetType, ConversionContext context) {
        if (object == null || targetType == null || context == null) {
            return Optional.empty();
        }
        if (targetType == Object.class) {
            return Optional.of((T) object);
        }
        targetType = targetType.isPrimitive() ? (Class<T>) ReflectionUtils.getWrapperType(targetType) : targetType;
        if (targetType.isInstance(object) && !(object instanceof Iterable) && !(object instanceof Map)) {
            return Optional.of((T) object);
        }
        final AnnotationMetadata annotationMetadata = context.getAnnotationMetadata();
        String formattingAnnotation;
        if (annotationMetadata.hasStereotypeNonRepeating(Format.class)) {
            formattingAnnotation = annotationMetadata.getAnnotationNameByStereotype(Format.class).orElse(null);
        } else {
            formattingAnnotation = null;
        }
        ConvertiblePair pair = new ConvertiblePair(sourceType, targetType, formattingAnnotation);
        TypeConverter<Object, T> typeConverter = findConverter(pair);
        if (typeConverter == null) {
            typeConverter = findTypeConverter(sourceType, targetType, formattingAnnotation);
            addToConverterCache(pair, typeConverter);
        }
        if (typeConverter == UNCONVERTIBLE) {
            return Optional.empty();
        }
        return typeConverter.convert(object, targetType, context);
    }

    @Override
    public <S, T> boolean canConvert(Class<S> sourceType, Class<T> targetType) {
        ConvertiblePair pair = new ConvertiblePair(sourceType, targetType, null);
        TypeConverter<Object, T> typeConverter = findConverter(pair);
        if (typeConverter == null) {
            typeConverter = findTypeConverter(sourceType, targetType, null);
            addToConverterCache(pair, typeConverter);
            return typeConverter != UNCONVERTIBLE;
        }
        return typeConverter != UNCONVERTIBLE;
    }

    @Nullable
    private <T, S> TypeConverter<T, S> findConverter(ConvertiblePair pair) {
        TypeConverter typeConverter = internalConverters.get(pair);
        if (typeConverter != null) {
            return typeConverter;
        }
        return converterCache.get(pair);
    }

    @Nullable
    private <T, S> TypeConverter<T, S> findRegisteredConverter(ConvertiblePair pair) {
        TypeConverter typeConverter = internalConverters.get(pair);
        if (typeConverter != null) {
            return typeConverter;
        }
        return customConverters.get(pair);
    }

    @Override
    public <S, T> void addConverter(Class<S> sourceType, Class<T> targetType, TypeConverter<S, T> typeConverter) {
        addConverterAnalyzeSource(customConverters, sourceType, targetType, typeConverter);
    }

    @Internal
    public <S, T> void addInternalConverter(Class<S> sourceType, Class<T> targetType, TypeConverter<S, T> typeConverter) {
        addConverterAnalyzeSource(internalConverters, sourceType, targetType, typeConverter);
    }

    private <S, T> void addConverterAnalyzeSource(Map<ConvertiblePair, TypeConverter> typeConverters,
                                                  Class<S> sourceType,
                                                  Class<T> targetType,
                                                  TypeConverter<S, T> typeConverter) {
        addConverterToMap(typeConverters, sourceType, targetType, typeConverter);
        if (sourceType == CharSequence.class) {
            TypeConverter<String, T> converter;
            if (typeConverter instanceof FormattingTypeConverter<S, T, ?> formattingTypeConverter) {
                converter = new FormattingTypeConverter<>() {
                    @Override
                    public Class<Annotation> annotationType() {
                        return (Class<Annotation>) formattingTypeConverter.annotationType();
                    }
                    @Override
                    public Optional<T> convert(String value, Class<T> targetType, ConversionContext context) {
                        return typeConverter.convert((S) value.toString(), (Class<T>) CharSequence.class, context);
                    }
                };
            } else {
                converter = (value, theTarget, context) -> typeConverter.convert((S) value.toString(), theTarget, context);
            }
            addConverterToMap(typeConverters, String.class, targetType, converter);
        } else if (sourceType == String.class) {
            addConverterToMap(typeConverters, CharSequence.class, targetType, (TypeConverter) typeConverter);
        }
    }

    private <S, T> void addConverterToMap(Map<ConvertiblePair, TypeConverter> typeConverters,
                                          Class<S> sourceType,
                                          Class<T> targetType,
                                          TypeConverter<S, T> typeConverter) {
        ConvertiblePair pair = newPair(sourceType, targetType, typeConverter);
        typeConverters.put(pair, typeConverter);
        if (typeConverters != internalConverters) {
            addToConverterCache(pair, typeConverter);
        }
    }

    @Override
    public <S, T> void addConverter(Class<S> sourceType, Class<T> targetType, Function<S, T> function) {
        addConverter(sourceType, targetType, TypeConverter.of(sourceType, targetType, function));
    }

    @Internal
    public <S, T> void addInternalConverter(Class<S> sourceType, Class<T> targetType, Function<S, T> function) {
        addInternalConverter(sourceType, targetType, TypeConverter.of(sourceType, targetType, function));
    }

    private void addToConverterCache(ConvertiblePair pair, TypeConverter<?, ?> typeConverter) {
        converterCache.put(pair, typeConverter);
        if (converterCache.size() > CACHE_MAX) {
            CopyOnWriteMap.evict(converterCache, CACHE_EVICTION_BATCH);
        }
    }

    @SuppressWarnings({"OptionalIsPresent", "unchecked"})
    private void registerDefaultConverters() {
        addInternalConverter(Reader.class, String.class, (object, targetType, context) -> {
            try (BufferedReader reader = object instanceof BufferedReader bufferedReader ? bufferedReader : new BufferedReader(object)) {
                return Optional.of(IOUtils.readText(reader));
            } catch (IOException e) {
                context.reject(e);
                return Optional.empty();
            }
        });
        addInternalConverter(CharSequence.class, String.class, (object, targetType, context) -> Optional.of(object.toString()));
        addInternalConverter(CharSequence.class, Iterable.class, (CharSequence object, Class<Iterable> targetType, ConversionContext context) -> {
            Optional<Argument<?>> typeVariable = context.getFirstTypeVariable();
            Argument<?> componentType = typeVariable.orElse(Argument.OBJECT_ARGUMENT);
            ConversionContext newContext = context.with(componentType);
            Class<?> targetComponentType = ReflectionUtils.getWrapperType(componentType.getType());
            String[] strings = object.toString().split(",");
            List<Object> list = new ArrayList<>();
            for (String string : strings) {
                Optional<?> converted = convert(string, targetComponentType, newContext);
                if (converted.isPresent()) {
                    list.add(converted.get());
                }
            }
            return CollectionUtils.convertCollection((Class) targetType, list);
        });
        addInternalConverter(Iterable.class, Iterable.class, (object, targetType, context) -> {
            Optional<Argument<?>> typeVariable = context.getFirstTypeVariable();
            Argument<?> componentType = typeVariable.orElse(Argument.OBJECT_ARGUMENT);
            Class<?> targetComponentType = ReflectionUtils.getWrapperType(componentType.getType());
            if (targetType.isInstance(object) && targetComponentType == Object.class) {
                return Optional.of(object);
            }
            List<Object> list = new ArrayList<>();
            ConversionContext newContext = context.with(componentType);
            for (Object o : object) {
                Optional<?> converted = convert(o, targetComponentType, newContext);
                if (converted.isPresent()) {
                    list.add(converted.get());
                }
            }
            return CollectionUtils.convertCollection((Class) targetType, list);
        });
        internalMutableConversionService.addConverterRegistrar(new MultiValuesConverterFactory());
        SoftServiceLoader.load(ConversionServiceRegistrar.class).forEach(registrar -> registrar.register(internalMutableConversionService));
    }

    private NumberFormat resolveNumberFormat(ConversionContext context) { return null; }
    private SimpleDateFormat resolveFormat(ConversionContext context) { return new SimpleDateFormat(); }
    private @Nullable TypeConverter<Object, ?> findTypeConverter(Class<?> sourceType, Class<?> targetType, @Nullable String formattingAnnotation) {
        if (sourceType == targetType) {
            return ConversionService.IDENTITY;
        }
        TypeConverter<Object, ?> typeConverter = findRegisteredConverter(new ConvertiblePair(sourceType, targetType, formattingAnnotation));
        if (typeConverter != null) {
            return typeConverter;
        }
        if (sourceType == Object.class || targetType == Object.class) {
            return findRegisteredConverter(new ConvertiblePair(sourceType, targetType, null));
        }
        return null;
    }
    private ConvertiblePair newPair(Class<?> sourceType, Class<?> targetType, TypeConverter<?, ?> typeConverter) {
        if (typeConverter instanceof FormattingTypeConverter<?, ?, ?> formattingTypeConverter) {
            return new ConvertiblePair(sourceType, targetType, formattingTypeConverter.annotationType().getName());
        }
        return new ConvertiblePair(sourceType, targetType, null);
    }
}
