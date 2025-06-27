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
package io.micronaut.http.netty.channel.loom;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.scheduling.LoomSupport;
import io.netty.util.AttributeMap;
import io.netty.util.concurrent.EventExecutor;

/**
 * Metadata for a virtual thread on the custom scheduler. Does not change throughout a virtual
 * thread's lifetime. This class allows for creating local shared resources, e.g. HTTP connections
 * that run on the same event loop.
 *
 * @since 4.9.0
 * @author Jonas Konrad
 */
@Internal
public sealed interface CarriedVThreadAttachment
    permits LoomCarrierGroup.IoExecutor, LoomCarrierGroup.SingleThreadScheduler, LoomCarrierGroup.InheritableScheduler {

    /**
     * Get a shared {@link AttributeMap} for this scheduler.
     *
     * @return The attribute map
     */
    @NonNull
    AttributeMap attributeMap();

    /**
     * Get the preferred event loop.
     *
     * @return The event loop
     */
    @NonNull
    EventExecutor eventLoop();

    @NonNull
    Thread createVirtualThread(@Nullable String name, @NonNull Runnable task);

    @Nullable
    static CarriedVThreadAttachment forCurrentThread() {
        if (LoomPrototypeSupport.isSupported() &&
            LoomSupport.isVirtual(Thread.currentThread()) &&
            LoomPrototypeSupport.getCurrentThreadAttachment() instanceof CarriedVThreadAttachment el) {
            return el;
        } else if (PrivateLoomSupport.isSupported() &&
            LoomSupport.isVirtual(Thread.currentThread()) &&
            PrivateLoomSupport.getScheduler(Thread.currentThread()) instanceof CarriedVThreadAttachment el) {
            return el;
        } else {
            return null;
        }
    }
}
