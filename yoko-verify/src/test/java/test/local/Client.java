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
package test.local;

import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.omg.CORBA.*;

public class Client {
    public static int run(ORB orb, String[] args)
            throws org.omg.CORBA.UserException {
        //
        // Get "test" object
        //
        org.omg.CORBA.Object obj = orb.string_to_object("relfile:/Test.ref");
        if (obj == null) {
            System.err.println("cannot read IOR from Test.ref");
            return 1;
        }

        Test test = TestHelper.narrow(obj);
        assertTrue(test != null);

        Test localTest = new LocalTest_impl();

        org.omg.IOP.CodecFactory factory = org.omg.IOP.CodecFactoryHelper
                .narrow(orb.resolve_initial_references("CodecFactory"));
        assertTrue(factory != null);

        org.omg.IOP.Encoding how = new org.omg.IOP.Encoding();
        how.major_version = 0;
        how.minor_version = 0;
        how.format = org.omg.IOP.ENCODING_CDR_ENCAPS.value;

        org.omg.IOP.Codec codec = factory.create_codec(how);
        assertTrue(codec != null);

        System.out.print("Testing Codec... ");
        System.out.flush();
        try {
            org.omg.CORBA.Any a = orb.create_any();
            TestHelper.insert(a, test);
            byte[] data = codec.encode_value(a);
        } catch (org.omg.CORBA.SystemException ex) {
            assertTrue(false);
        }
        try {
            org.omg.CORBA.Any a = orb.create_any();
            TestHelper.insert(a, localTest);
            byte[] data = codec.encode_value(a);
            assertTrue(false);
        } catch (org.omg.CORBA.MARSHAL ex) {
            // Expected
        }
        System.out.println("Done!");

        System.out.print("Testing simple RPC call... ");
        System.out.flush();
        test.say("Hi");
        System.out.println("Done!");

        System.out.print("Testing passing non-local object... ");
        System.out.flush();
        try {
            test.intest(test);
        } catch (org.omg.CORBA.SystemException ex) {
            assertTrue(false);
        }
        System.out.println("Done!");

        System.out.print("Testing passing local object... ");
        System.out.flush();
        try {
            test.intest(localTest);
            assertTrue(false);
        } catch (org.omg.CORBA.MARSHAL ex) {
            // Expected
        }
        System.out.println("Done!");

        System.out.print("Testing passing non-local object in any... ");
        System.out.flush();
        try {
            org.omg.CORBA.Any a = orb.create_any();
            TestHelper.insert(a, test);
            test.inany(a);
        } catch (org.omg.CORBA.SystemException ex) {
            assertTrue(false);
        }
        System.out.println("Done!");

        System.out.print("Testing insertion of local object in any... ");
        System.out.flush();
        try {
            org.omg.CORBA.Any a = orb.create_any();
            TestHelper.insert(a, localTest);
            Test t = TestHelper.extract(a);
            assertTrue(t == localTest);
        } catch (org.omg.CORBA.SystemException ex) {
            assertTrue(false);
        }
        System.out.println("Done!");

        System.out.print("Testing passing local object in any... ");
        System.out.flush();
        try {
            org.omg.CORBA.Any a = orb.create_any();
            TestHelper.insert(a, localTest);
            test.inany(a);
            assertTrue(false);
        } catch (org.omg.CORBA.MARSHAL ex) {
            // Expected
        }
        System.out.println("Done!");

        System.out.print("Testing returning local object... ");
        System.out.flush();
        try {
            Test t = test.returntest();
            assertTrue(false);
        } catch (org.omg.CORBA.MARSHAL ex) {
            // Expected
        }
        System.out.println("Done!");

        System.out.print("Testing returning local object in any... ");
        System.out.flush();
        try {
            org.omg.CORBA.AnyHolder a = new org.omg.CORBA.AnyHolder();
            test.outany(a);
            assertTrue(false);
        } catch (org.omg.CORBA.MARSHAL ex) {
            // Expected
        }
        System.out.println("Done!");

        test.shutdown();

        return 0;
    }

    public static void main(String args[]) {
        java.util.Properties props = new Properties();
        props.putAll(System.getProperties());
        props.put("org.omg.CORBA.ORBClass", "org.apache.yoko.orb.CORBA.ORB");
        props.put("org.omg.CORBA.ORBSingletonClass",
                "org.apache.yoko.orb.CORBA.ORBSingleton");

        int status = 0;
        ORB orb = null;

        try {
            orb = ORB.init(args, props);
            status = run(orb, args);
        } catch (Exception ex) {
            ex.printStackTrace();
            status = 1;
        }

        if (orb != null) {
            try {
                orb.destroy();
            } catch (Exception ex) {
                ex.printStackTrace();
                status = 1;
            }
        }

        System.exit(status);
    }
}
