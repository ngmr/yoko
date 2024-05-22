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

import acme.RemoteConsumer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.COMM_FAILURE;
import org.omg.CORBA.INV_OBJREF;
import org.omg.CORBA.MARSHAL;
import org.omg.CORBA.NO_IMPLEMENT;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.CORBA.SystemException;
import testify.iiop.annotation.ConfigureServer;
import testify.iiop.annotation.ConfigureServer.RemoteImpl;

import java.rmi.AccessException;
import java.rmi.MarshalException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.util.function.Supplier;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@ConfigureServer
public class RemoteExceptionTest {
    enum Conversion {
        ACCESS(NO_PERMISSION.class, NO_PERMISSION::new, AccessException.class),
        MARSHAL1(MARSHAL.class, MARSHAL::new, MarshalException.class),
        MARSHAL2(BAD_PARAM.class, BAD_PARAM::new, MarshalException.class),
        MARSHAL3(COMM_FAILURE.class, COMM_FAILURE::new, MarshalException.class),
        NONESUCH1(INV_OBJREF.class, INV_OBJREF::new, NoSuchObjectException.class),
        NONESUCH2(NO_IMPLEMENT.class, NO_IMPLEMENT::new, NoSuchObjectException.class),
        NONESUCH3(OBJECT_NOT_EXIST.class, OBJECT_NOT_EXIST::new, NoSuchObjectException.class)
        ;

        final Class<? extends SystemException> systemExceptionClass;
        final Supplier<? extends SystemException> factory;
        final Class<? extends RemoteException> remoteExceptionType;


        <S extends SystemException, R extends RemoteException>
        Conversion(Class<S> systemExceptionType, Supplier<S> factory, Class<R> remoteExceptionType) {
            this.systemExceptionClass = systemExceptionType;
            this.factory = factory;
            this.remoteExceptionType = remoteExceptionType;
        }

        public void throwSystemException() { throw factory.get(); }
    }

    interface Thrower extends RemoteConsumer<Conversion> {}

    @RemoteImpl
    public static final Thrower IMPL = Conversion::throwSystemException;

    @ParameterizedTest
    @EnumSource(Conversion.class)
    public void testConversion(Conversion conversion, Thrower stub) {
        try {
            stub.accept(conversion);
            fail("Should have thrown " + conversion.remoteExceptionType.getName());
        } catch (RemoteException re) {
            assertThat(re, instanceOf(conversion.remoteExceptionType));
            assertThat(re.detail, instanceOf(conversion.systemExceptionClass));
        }
    }
}
