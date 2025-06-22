package io.micronaut.http.netty.channel.loom;

import io.micronaut.core.annotation.Experimental;
import io.micronaut.core.annotation.Internal;
import io.netty.util.internal.PlatformDependent;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.Executor;

@Internal
@Experimental
public final class LoomPrototypeSupport {

    private static final MethodHandle SCHEDULER;
    private static final MethodHandle GET_THREAD;
    private static final MethodHandle GET_ATTACHMENT;
    private static final MethodHandle SET_ATTACHMENT;
    private static final MethodHandle GET_CURRENT_THREAD_ATTACHMENT;
    private static final Throwable FAILURE;

    static {
        MethodHandle scheduler;
        MethodHandle getThread;
        MethodHandle getAttachment;
        MethodHandle setAttachment;
        MethodHandle getCurrentThreadAttachment;
        Throwable failure;
        try {
            Class<?> ofVirtual = Class.forName("java.lang.Thread$Builder$OfVirtual");
            scheduler = MethodHandles.lookup()
                .findVirtual(ofVirtual, "scheduler", MethodType.methodType(ofVirtual, Executor.class))
                .asType(MethodType.methodType(void.class, Object.class, Executor.class));
            Class<?> virtualThreadTask = Class.forName("java.lang.Thread$VirtualThreadTask");
            getThread = MethodHandles.lookup()
                .findVirtual(virtualThreadTask, "thread", MethodType.methodType(Thread.class))
                .asType(MethodType.methodType(Thread.class, Runnable.class));
            getAttachment = MethodHandles.lookup()
                .findVirtual(virtualThreadTask, "attachment", MethodType.methodType(Object.class))
                .asType(MethodType.methodType(Object.class, Runnable.class));
            setAttachment = MethodHandles.lookup()
                .findVirtual(virtualThreadTask, "attach", MethodType.methodType(Object.class, Object.class))
                .asType(MethodType.methodType(Object.class, Runnable.class, Object.class));
            getCurrentThreadAttachment = MethodHandles.lookup()
                .findStatic(virtualThreadTask, "currentVirtualThreadTaskAttachment", MethodType.methodType(Object.class));
            failure = null;
        } catch (Throwable e) {
            failure = e;
            scheduler = null;
            getThread = null;
            getAttachment = null;
            setAttachment = null;
            getCurrentThreadAttachment = null;
        }
        SCHEDULER = scheduler;
        GET_THREAD = getThread;
        GET_ATTACHMENT = getAttachment;
        SET_ATTACHMENT = setAttachment;
        GET_CURRENT_THREAD_ATTACHMENT = getCurrentThreadAttachment;
        FAILURE = failure;
    }

    public static void setScheduler(Object builder, Executor executor) {
        try {
            SCHEDULER.invokeExact(builder, executor);
        } catch (Throwable e) {
            PlatformDependent.throwException(e);
        }
    }

    public static Thread getThread(Runnable task) {
        try {
            return (Thread) GET_THREAD.invokeExact(task);
        } catch (Throwable e) {
            PlatformDependent.throwException(e);
            throw new AssertionError(e);
        }
    }

    public static Object getAttachment(Runnable task) {
        try {
            return (Object) GET_ATTACHMENT.invokeExact(task);
        } catch (Throwable e) {
            PlatformDependent.throwException(e);
            throw new AssertionError(e);
        }
    }

    public static Object setAttachment(Runnable task, Object attachment) {
        try {
            return (Object) SET_ATTACHMENT.invokeExact(task, attachment);
        } catch (Throwable e) {
            PlatformDependent.throwException(e);
            throw new AssertionError(e);
        }
    }

    public static Object getCurrentThreadAttachment() {
        try {
            return (Object) GET_CURRENT_THREAD_ATTACHMENT.invokeExact();
        } catch (Throwable e) {
            PlatformDependent.throwException(e);
            throw new AssertionError(e);
        }
    }

    public static boolean isSupported() {
        return FAILURE == null;
    }
}
