package io.micronaut.docs.ioc.mappers;

import io.micronaut.core.annotation.Introspected;

import java.util.List;

@Introspected
public record ContainerB(String name, ItemB inner, List<ItemB> items) {
}
