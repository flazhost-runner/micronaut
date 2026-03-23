package io.micronaut.docs.ioc.mappers;

import io.micronaut.core.annotation.Introspected;

import java.util.List;

@Introspected
public record ContainerA(String name, ItemA inner, List<ItemA> items) {
}
