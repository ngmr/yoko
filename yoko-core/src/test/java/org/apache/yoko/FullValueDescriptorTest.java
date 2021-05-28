/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.yoko;

import org.junit.jupiter.api.Test;
import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.Servant;
import test.fvd.Bounceable;
import test.fvd.BounceableImpl;
import test.fvd.Bouncer;
import test.fvd.BouncerImpl;
import test.fvd.ClassUsurper;
import test.fvd.Marshalling;
import testify.bus.Bus;
import testify.jupiter.annotation.iiop.ConfigureServer;
import testify.jupiter.annotation.iiop.ConfigureServer.BeforeServer;
import testify.jupiter.annotation.logging.Logging;

import javax.rmi.CORBA.Tie;
import javax.rmi.CORBA.Util;
import javax.rmi.PortableRemoteObject;
import java.security.PrivilegedExceptionAction;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ConfigureServer
public class FullValueDescriptorTest {
    private static final String[] VERSIONED_CLASSPATH = System.getProperty("versioned.classpath").split(" ");
    public static final ClassUsurper SERVER_USURPER = new ClassUsurper("SERVER", VERSIONED_CLASSPATH, FullValueDescriptorTest.class);
    public static final ClassUsurper CLIENT_USURPER = new ClassUsurper("CLIENT",VERSIONED_CLASSPATH, FullValueDescriptorTest.class);

    private static PrivilegedExceptionAction<byte[]> action;

    @BeforeServer
    public static void initServer(ORB orb, Bus bus) throws Exception {
        if (FullValueDescriptorTest.SERVER_USURPER.usurp(orb, bus)) return;
        ////////////////////// CODE BELOW HERE EXECUTES IN USURPING LOADER ONLY //////////////////////
        Marshalling.VERSION2.select(); // server version
        System.out.println("### SERVER rep id: " + Util.createValueHandler().getRMIRepositoryID(BounceableImpl.class));

        POA rootPOA = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
        rootPOA.the_POAManager().activate();
        ////// create a Bouncer object and write out the IOR //////
        BouncerImpl bouncer = new BouncerImpl(orb);
        PortableRemoteObject.exportObject(bouncer);
        Tie tie = Util.getTie(bouncer);
        System.out.println("About to activate object with root POA");
        rootPOA.activate_object((Servant)tie);
        String ior = orb.object_to_string(tie.thisObject());
        bus.put("ior", ior);
    }

    @Test
    public void testVersionedClassesAreNotAvailable() {
        assertThrows(NoClassDefFoundError.class, () -> System.out.println(Marshalling.class));
    }

    @Test
    public void testVersionedClassesAreAvailableFromClient() {
        assertVersionedClassesAreAvailable(CLIENT_USURPER);
    }

    @Test
    public void testVersionedClassesAreAvailableFromServer() {
        assertVersionedClassesAreAvailable(SERVER_USURPER);
    }

    public static void assertVersionedClassesAreAvailable(ClassUsurper usurper) {
        if (usurper.usurp(usurper)) return;
        System.out.println(Marshalling.DEFAULT_VERSION);
    }

    @Test
    @Logging("yoko.verbose")
    public void testFVD(ORB orb, Bus bus) throws Exception {
        initClient(orb, bus);
    }

    public static void initClient(ORB orb, Bus bus) throws Exception {
        if (CLIENT_USURPER.usurp(orb, bus)) return;
        Marshalling.VERSION1.select(); // client version
        System.out.println("### CLIENT rep id: " + Util.createValueHandler().getRMIRepositoryID(BounceableImpl.class));
        String ior = bus.get("ior");
        Bouncer bouncer = (Bouncer)PortableRemoteObject.narrow(orb.string_to_object(ior), Bouncer.class);
        Bounceable bounceable = new BounceableImpl().validateAndReplace();
        Bounceable returned = (Bounceable) bouncer.bounceObject(bounceable);
        returned.validateAndReplace();
    }
}


