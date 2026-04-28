package io.micronaut.context.i18n;

import io.micronaut.context.MessageSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceBundleMessageSourceTest {

    @DisabledInNativeImage
    @Test
    void resourceBundleMessageCacheHasAMaxSize() {
        ResourceBundleMessageSource resourceBundleMessageSource = new ResourceBundleMessageSource("io.micronaut.docs.i18n.messages");
        int size = 110;
        int max = 100;
        assertTrue(max < size);
        for (int i = 1; i <= size; i++) {
            resourceBundleMessageSource.getRawMessage("hello", MessageSource.MessageContext.of(locale(i)));
        }
        Map<?, Optional<ResourceBundle>> cache = assertDoesNotThrow(() -> cache(resourceBundleMessageSource));
        assertEquals(max, cache.size());
    }

    private static Locale locale(int i) {
        return Locale.forLanguageTag(String.format("zz-%04d", i));
    }

    private static Map<?, Optional<ResourceBundle>> cache(ResourceBundleMessageSource instance) {
        try {
            Field field = instance.getClass().getDeclaredField("bundleCache");
            field.setAccessible(true);
            return (Map<?, Optional<ResourceBundle>>) field.get(instance);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
