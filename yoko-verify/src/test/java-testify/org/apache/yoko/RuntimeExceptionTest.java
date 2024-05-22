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
package org.apache.yoko;

import org.junit.jupiter.api.Test;
import testify.iiop.annotation.ConfigureServer;
import testify.iiop.annotation.ConfigureServer.RemoteImpl;

import java.awt.image.RasterFormatException;
import java.rmi.Remote;
import java.rmi.RemoteException;

import static org.junit.jupiter.api.Assertions.fail;

@ConfigureServer
public class RuntimeExceptionTest {
    public interface Lobber extends Remote {
        void lob() throws RemoteException, RasterFormatException;
    }

    @RemoteImpl
    public static final Lobber LOBBER = ()->{ throw new RasterFormatException("bob"); };

    @Test
    public void testRemoteThrowRuntimeException(Lobber lobber) throws Exception {
        try {
            lobber.lob();
            fail("Should have thrown expected exception");
        } catch (RasterFormatException e) {
        }
    }
}
