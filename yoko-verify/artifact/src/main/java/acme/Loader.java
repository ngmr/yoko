/*
 * Copyright 2023 IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an \"AS IS\" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package acme;

import org.apache.yoko.io.SimplyCloseable;
import org.apache.yoko.util.AssertionFailed;
import testify.util.Assertions;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static testify.util.Throw.invokeWithImpunity;

/**
 * A simplified mechanism (with a lot of debug) for loading classes from the specialised test class paths.
 * Ensure that the build.gradle for this project sets up the class paths appropriately.
 */
@SuppressWarnings("unchecked cast")
public enum Loader {
    V0,
    V1,
    V2;
    final URLClassLoader loader;

    Loader() {
        this.loader = getLoader("acme.loader." + name().toLowerCase() + ".path");
    }

    private static URLClassLoader getLoader(String classpathProp) {
        // NOTE:
        // Directory URLs for URLClassLoader need a trailing slash.
        // Paths.get(path).toURI().toURL() sets this up nicely.
        String classpath = System.getProperty(classpathProp);
        assertThat(classpath, not(nullValue()));
        assertThat(classpath, not(emptyString()));
        URL[] urls = Stream.of(classpath.split(File.pathSeparator))
                .sequential()
                .map(Paths::get)
                .map(Path::toUri)
                .map(uri -> {
                    try {
                        return uri.toURL();
                    } catch (MalformedURLException e) {
                        throw new IllegalArgumentException("Invalid url: " + uri, e);
                    }
                })
                .collect(Collectors.toList())
                .toArray(new URL[]{});
        ClassLoader parentLoader = Loader.class.getClassLoader();
        return new URLClassLoader(urls, parentLoader);
    }

    public <T> Class<? extends T> loadClass(String className) {
        try {
            return (Class<? extends T>) loader.loadClass(className);
        } catch (ClassNotFoundException cnfe) {
            String resourceName = className.replace('.', '/') + ".class";
            URL url = loader.findResource(resourceName);
            StringBuilder sb = new StringBuilder();
            sb.append("\n### Could not load class " + className);
            sb.append("\n### URLs:");
            Stream.of(loader.getURLs()).map(URL::toString).map("\n\t### "::concat).forEach(sb::append);
            sb.append("\n### loader.findResource(").append(resourceName).append(") returns ").append(url);
            throw new AssertionFailed("Caught " + cnfe + sb, cnfe);
        }
    }

    public <T> T deserializeFromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
        try (
                ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                ObjectInputStream ois = new ObjectInputStream(bais) {
                    @Override
                    protected Class<?> resolveClass(ObjectStreamClass desc) throws ClassNotFoundException {
                        return Class.forName(desc.getName(), false, loader);
                    }
                };
        ) {
            return (T)ois.readObject();
        }
    }

    public <T> Constructor<T> getConstructor(String className, Class<?>...paramTypes) {
        return (Constructor<T>) invokeWithImpunity(loadClass(className)::getConstructor, paramTypes);
    }

    public <T> T newInstance(String className) {
        return (T) invokeWithImpunity(getConstructor(className)::newInstance);
    }

    public <T> T newInstance(String className, Object...params) {
        Class<T> theClass = (Class<T>) loadClass(className);
        Constructor<?>[] constructors = theClass.getConstructors();
        // filter only constructors with the right number of params
        Stream<Constructor<?>> stream = Stream.of(constructors)
                .filter(cons -> cons.getParameterTypes().length == params.length);
        // filter only constructors that will accept each parameter in the right position
        for (int i = 0; i < params.length; i++) {
            final int index = i;
            stream = stream.filter(c -> c.getParameterTypes()[index].isInstance(params[index]));
        }

        List<Constructor<?>> list = stream.collect(Collectors.toList());
        switch (list.size()) {
            case 0: throw Assertions.failf("Could not find constructor suitable for params %s. Available constructors:%n\t%s", Arrays.toString(params), Stream.of(constructors).map(Object::toString).collect(joining("%n\t")));
            case 1: return (T) invokeWithImpunity(() -> list.get(0).newInstance(params));
            default: throw Assertions.failf("Ambiguous parameter list for params %s: found too many matching constructors: %n\t%s", Arrays.toString(params), list.stream().map(Object::toString).collect(joining("%n\t")));
        }
    }

    public SimplyCloseable setAsThreadContextClassLoader() {
        Thread t = Thread.currentThread();
        ClassLoader old = t.getContextClassLoader();
        t.setContextClassLoader(this.loader);
        return () -> t.setContextClassLoader(old);
    }
}
