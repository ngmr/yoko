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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
//import org.omg.CORBA.ORB;
//import org.omg.CORBA.ORBPackage.InvalidName;
//import org.omg.PortableServer.POA;
//import org.omg.PortableServer.POAHelper;
//import org.omg.PortableServer.POAManagerPackage.AdapterInactive;
//import org.omg.PortableServer.POAPackage.ServantAlreadyActive;
//import org.omg.PortableServer.POAPackage.WrongPolicy;
//import test.fvd.Bounceable;
//import test.fvd.Bouncer;
//import test.fvd.ClassUsurper;
//import test.fvd.Marshalling;
//import test.fvd.versioned.BounceableImpl;
//import test.fvd.versioned.BouncerImpl;
//import test.fvd.versioned.Marshalling;
//import test.fvd.versioned._BouncerImpl_Tie;
//import testify.bus.Bus;
//import testify.jupiter.annotation.iiop.ConfigureServer;
//import testify.jupiter.annotation.iiop.ConfigureServer.BeforeServer;
//
//import javax.rmi.CORBA.Util;
//import javax.rmi.PortableRemoteObject;

import static test.fvd.Marshalling.VERSION1;

//@ConfigureServer
public class FullValueDescriptorTest {
//    private static final ClassUsurper serverUsurper = new ClassUsurper("SERVER", "test.fvd.versioned.*", FullValueDescriptorTest.class);
//    private static final ClassUsurper clientUsurper = new ClassUsurper("CLIENT","test.fvd.versioned.*", FullValueDescriptorTest.class);
//
//    @BeforeServer
//    public static void initServer (ORB orb, Bus bus) {
//        if (serverUsurper.usurp(orb, bus)) return;
//        ////////////////////// CODE BELOW HERE EXECUTES IN USURPING LOADER ONLY //////////////////////
//        Marshalling.VERSION2.select(); // server version
//
//        System.out.println("### SERVER rep id: " + Util.createValueHandler().getRMIRepositoryID(BounceableImpl.class));
//
//        try {
//            POA rootPOA = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
//            rootPOA.the_POAManager().activate();
//            ////// create a Bouncer object and write out the IOR //////
//            BouncerImpl bouncer = new BouncerImpl(orb);
//            _BouncerImpl_Tie tie = new _BouncerImpl_Tie();
//            tie.setTarget(bouncer);
//            rootPOA.activate_object(tie);
//            String ior = orb.object_to_string(tie.thisObject());
//            bus.put("ior", ior);
//        } catch (InvalidName | AdapterInactive | ServantAlreadyActive | WrongPolicy e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Test
//    public void testFVD(ORB orb, Bus bus) throws Exception {
//        initClient(orb, bus);
//    }
//
//    public static void initClient(ORB orb, Bus bus) throws Exception {
//        if (clientUsurper.usurp(orb, bus)) return;
//        Marshalling.VERSION1.select(); // client version
//        System.out.println("### CLIENT rep id: " + Util.createValueHandler().getRMIRepositoryID(BounceableImpl.class));
//        String ior = bus.get("ior");
//        Bouncer bouncer = (Bouncer)PortableRemoteObject.narrow(orb.string_to_object(ior), Bouncer.class);
//        Bounceable bounceable = new BounceableImpl().validateAndReplace();
//        Bounceable returned = (Bounceable) bouncer.bounceObject(bounceable);
//        returned.validateAndReplace();
//    }

    @Test
    public void testVersionedClassesAreNotAvailable() {
        Assertions.assertThrows(NoClassDefFoundError.class, () -> VERSION1.select());
    }

}
