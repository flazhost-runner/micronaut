package io.micronaut.docs.ioc.mappers;

import io.micronaut.context.annotation.Mapper;
import jakarta.inject.Singleton;

@Singleton
public interface ListContainerMapper {
    @Mapper
    ContainerA toA(ContainerB input);
}
