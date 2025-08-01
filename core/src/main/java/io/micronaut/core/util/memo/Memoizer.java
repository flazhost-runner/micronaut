/*
 * Copyright 2017-2025 original authors
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
package io.micronaut.core.util.memo;

import io.micronaut.core.annotation.Experimental;
import io.micronaut.core.annotation.NonNull;

/**
 * An object that can optionally store additional caller-customizable memoized data. Essentially,
 * a memoizer contains a {@code Map<MemoizedReference, T>}, and {@link #getMemoized} does a
 * {@code computeIfAbsent} on that map.
 *
 * <p>Each {@link Memoizer} <i>type</i> is associated with a {@link MemoizerNamespace}. The
 * namespace keeps track of the fields ({@link MemoizedReference}s and {@link MemoizedFlag}s) that
 * are permitted for that namespace.
 *
 * <p>The namespace allows for the {@link Memoizer} implementation to be more efficient than a
 * simple {@link java.util.Map}, e.g. by storing booleans as a bit field.
 *
 * @param <SELF> The type the memoization functions use
 * @since 4.10.0
 * @author Jonas Konrad
 */
@Experimental
public interface Memoizer<SELF extends Memoizer<SELF>> {
    /**
     * Get a memoized reference value.
     *
     * @param reference The memoized field to load or compute
     * @param <R>       The memoized value type
     * @return The memoized value
     * @implNote The default implementation does no storage and computes the memoized value each time.
     */
    @SuppressWarnings("unchecked")
    default <R> R getMemoized(@NonNull MemoizedReference<SELF, R> reference) {
        return reference.compute.apply((SELF) this);
    }

    /**
     * Get a memoized boolean value.
     *
     * @param flag The memoized flag to load or compute
     * @return The memoized value
     * @implNote The default implementation does no storage and computes the memoized value each time.
     */
    @SuppressWarnings("unchecked")
    default boolean getMemoized(@NonNull MemoizedFlag<SELF> flag) {
        return flag.compute((SELF) this);
    }
}
