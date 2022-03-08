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

package acme.idl;

import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.PortableServer.ForwardRequest;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.Servant;
import org.omg.PortableServer.ServantLocatorPOA;
import org.omg.PortableServer.ServantLocatorPackage.CookieHolder;

//
// ServantLocator implementation to provide location forwarding
//
public final class TestLocator_impl extends ServantLocatorPOA {

    private final TestInterface_impl test_;

    private final TestInterfaceDSI_impl testDSI_;

    public TestLocator_impl(TestInterface_impl test, TestInterfaceDSI_impl testDSI) {
        test_ = test;
        testDSI_ = testDSI;
    }

    public Servant preinvoke(byte[] oid, POA poa, String operation, CookieHolder the_cookie) throws ForwardRequest {
        String oidString = new String(oid);

        //
        // Request for object "test" or "testDSI"
        //
        if (oidString.equals("test") || oidString.equals("testDSI")) {
            //
            // Location forward requested? Location forward back to
            // the same object. (The client-side interceptor consumes
            // the location forward).
            //
            if (operation.equals("location_forward")) {
                org.omg.CORBA.Object obj = poa.create_reference_with_id(oid, "IDL:TestInterface:1.0");
                throw new ForwardRequest(obj);
            }

            if (oidString.equals("test")) return test_;
            return testDSI_;
        }

        //
        // Fail
        //
        throw new OBJECT_NOT_EXIST();
    }

    public void postinvoke(byte[] oid, POA poa, String operation, java.lang.Object the_cookie, Servant the_servant) {
    }
}
