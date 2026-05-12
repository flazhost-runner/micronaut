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
}
