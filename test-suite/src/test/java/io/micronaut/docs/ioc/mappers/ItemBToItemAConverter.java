package io.micronaut.docs.ioc.mappers;

import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.convert.TypeConverter;
import jakarta.inject.Singleton;

import java.util.Optional;

@Singleton
public class ItemBToItemAConverter implements TypeConverter<ItemB, ItemA> {
    @Override
    public Optional<ItemA> convert(ItemB object, Class<ItemA> targetType, ConversionContext context) {
        if (object == null) {
            return Optional.empty();
        }
        return Optional.of(new ItemA(object.value()));
    }
}
