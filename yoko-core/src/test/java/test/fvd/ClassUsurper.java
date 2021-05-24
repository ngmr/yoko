/*
 * =============================================================================
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * =============================================================================
 */
package test.fvd;

import testify.util.Throw;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.unmodifiableSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class ClassUsurper extends URLClassLoader {
    private static final ThreadLocal<Boolean> reentrant = ThreadLocal.withInitial(() -> false);
    private final String usurperName;
    private final Set<String> classNames;
    private final Map<String, Supplier<Class<?>>> usurpedClasses = new ConcurrentHashMap<>();

    private static URL[] asURLs(String[] strings) {
        return Stream.of(strings)
                .sequential()
                .map(s -> {
                    try {
                        return new URL(s);
                    } catch (MalformedURLException e) {
                        throw new IllegalArgumentException("Invalid url: " + s, e);
                    }
                })
                .collect(Collectors.toList())
                .toArray(new URL[]{});
    }

    private ClassUsurper(String name, String[] urls, ClassLoader parent, Class<?>... classes) {
        super(asURLs(urls), parent);
        this.usurperName = name;
        this.classNames = unmodifiableSet(Stream.of(classes).map(Class::getName).collect(Collectors.toSet()));
    }

    public ClassUsurper(String name, String[] urls, Class<?>... classes) {
        this(name, urls, ClassUsurper.class.getClassLoader(), classes);
    }

    private boolean usurpable(String className) {
        return classNames.contains(className);
    }

    @Override
    protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
        final Class<?> clazz;
        if (usurpable(className)) {
            clazz = usurpedClasses.computeIfAbsent(className, this::findClassLazily).get();
        } else {
            clazz = super.loadClass(className, resolve);
        }
        return clazz;
    }

    @Override
    protected Class<?> findClass(String className) throws ClassNotFoundException {
        return usurpable(className)
                ? findClassOverride(className)
                : super.findClass(className);
    }

    private Supplier<Class<?>> findClassLazily(String className) {
        return new LazyFinalRef<>(() -> findClassOverride(className));
    }

    private Class<?> findClassOverride(String className) {
        String resourceName = className.replace('.', '/') + ".class";
        try (InputStream in = getResourceAsStream(resourceName)) {
            byte[] bytes = new byte[in.available()]; // could fail if available() is pessimistic
            int bytesRead = in.read(bytes);
            assert bytesRead == bytes.length;
            assert in.available() == 0;
            return defineClass(className, bytes, 0, bytes.length);
        } catch (IOException e) {
            throw (Error) (new NoClassDefFoundError(className).initCause(e));
        }
    }

    /**
     * Call this method to re-invoke the calling method , but on the target class loader.
     * The calling method must be public and static and must NOT be overloaded.
     * The caller should test for the result of this call and exit early if it is <code>true</code>.
     *
     * @return true if this method succeeds in invoking the calling method on an aped class and false if it is already in such an invocation.
     */
    public boolean  usurp(Object... args) {
        if (reentrant.get()) {
            return false;
        }
        try {
            reentrant.set(true);
            invokeUsurped(args);
            return true;
        } finally {
            reentrant.set(false);
        }
    }

    private void invokeUsurped(Object... args) {
        final StackTraceElement frame = getCallingStackFrame();
        final String className = frame.getClassName();
        final String methodName = frame.getMethodName();
        final ClassLoader oldTCCL = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this);
        try {
            Class<?> targetClass = Class.forName(className, true, this);
            final List<Method> methods = Arrays.stream(targetClass.getDeclaredMethods())
                    .filter(m -> Modifier.isStatic(m.getModifiers()))
                    .filter(m -> Modifier.isPublic(m.getModifiers()))
                    .filter(m -> m.getName().equals(methodName))
                    .collect(Collectors.toList());
            assertThat("There should be exactly one public static method matching " + className + "." + methodName, methods, hasSize(1));
            Method m = methods.get(0);
            m.invoke(null, args);
        } catch (ClassNotFoundException e) {
            throw new Error("Failed to load mirrored class " + className, e);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw new Error("Failed to invoke method main(String[]) for mirrored class" + className, e);
        } catch (InvocationTargetException e) {
            throw Throw.andThrowAgain(e.getTargetException());
        } finally {
            Thread.currentThread().setContextClassLoader(oldTCCL);
        }
    }

    public String toString() {
        return String.format("%s[%s]", ClassUsurper.class.getSimpleName(), usurperName);
    }

    private static StackTraceElement getCallingStackFrame() {
        StackTraceElement[] frames = new Throwable().getStackTrace();
        int i = 0;
        // find this class in the stack
        while (!!!frames[i].getClassName().equals(ClassUsurper.class.getName())) i++;
        // find the next class down in the stack
        while (frames[i].getClassName().equals(ClassUsurper.class.getName())) i++;
        StackTraceElement frame = frames[i];
        return frame;
    }

    private static class LazyFinalRef<T> implements Supplier<T> {
        T value;
        Supplier<T> initializer;
        LazyFinalRef(Supplier<T> initializer) { this.initializer = initializer; }
        public synchronized T get() { return null == value ? value = initializer.get() : value; }
    }
}
