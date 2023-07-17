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
package testify.util.function;

import org.opentest4j.AssertionFailedError;

import java.io.Serializable;

/** A three-argument function. */
@FunctionalInterface
public interface RawTriFunction<T,U,V,R> extends Serializable {
    R applyRaw(T t, U u, V v) throws Exception;

    default R apply(T t, U u, V v) {
        try {
            return applyRaw(t, u, v);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new AssertionFailedError("", e);
        }

    }

    default RawBiFunction<U,V,R> curry(T t) {
        return (u,v) -> applyRaw(t, u, v);
    }
}
