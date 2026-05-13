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
package io.micronaut.context;

import io.micronaut.core.annotation.Experimental;
import io.micronaut.core.type.Argument;
import io.micronaut.inject.BeanDefinition;

import java.util.Optional;

/**
 * Customizes selected bean resolution behavior for integrations.
 *
 * @since 5.1
 */
@Experimental
public interface BeanResolutionCustomizer {

    /**
     * The default customizer.
     */
    BeanResolutionCustomizer DEFAULT = new BeanResolutionCustomizer() {
    };

    /**
     * Whether an object array injection point should first resolve an exact array bean.
     *
     * @param injectionPoint The array injection point
     * @return True to resolve an exact array bean
     */
    default boolean shouldResolveArrayAsBean(Argument<?> injectionPoint) {
        return false;
    }

    /**
     * Resolve an additional argument that may be used to find bean candidates if the
     * requested type does not resolve a bean.
     *
     * @param beanType The requested bean type
     * @return The additional bean type to use for lookup
     * @since 5.1
     */
    default Argument<?> resolveBeanLookupArgument(Argument<?> beanType) {
        return beanType;
    }

    /**
     * Resolve a replacement value for a bean that was found but produced {@code null}.
     *
     * @param requestedBeanType The originally requested bean type
     * @param resolvedBeanType The resolved bean lookup type
     * @param beanDefinition The bean definition that produced {@code null}
     * @return A replacement bean value, or empty to preserve the default behavior
     * @since 5.1
     */
    default Optional<?> resolveNullBean(Argument<?> requestedBeanType, Argument<?> resolvedBeanType, BeanDefinition<?> beanDefinition) {
        return Optional.empty();
    }
}
