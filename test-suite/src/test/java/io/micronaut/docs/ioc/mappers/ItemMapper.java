package io.micronaut.docs.ioc.mappers;

import io.micronaut.context.annotation.Mapper;
import jakarta.inject.Singleton;

@Singleton
public interface ItemMapper {
    @Mapper
    ItemA toA(ItemB input);
}
