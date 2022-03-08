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

import acme.idl.TestInterfacePackage.s;
import acme.idl.TestInterfacePackage.sHolder;
import acme.idl.TestInterfacePackage.user;
import org.omg.CORBA.Any;
import org.omg.CORBA.NO_IMPLEMENT;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CORBA.StringHolder;
import org.omg.PortableInterceptor.Current;
import org.omg.PortableInterceptor.CurrentHelper;
import org.omg.PortableInterceptor.InvalidSlot;
import org.omg.PortableServer.POA;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public final class TestInterface_impl extends TestInterfacePOA {
    private final ORB orb_;
    private final POA poa_;
    private final Current current_;

    public TestInterface_impl(ORB orb, POA poa) {
        orb_ = orb;
        poa_ = poa;

        try {
            org.omg.CORBA.Object obj = orb.resolve_initial_references("PICurrent");
            current_ = CurrentHelper.narrow(obj);
        } catch (InvalidName ignored) {
            fail();
            throw null;
        }
    }

    // ----------------------------------------------------------------------
    // TestInterface_impl public member implementation
    // ----------------------------------------------------------------------

    public void noargs() {}

    public void noargs_oneway() {}

    public void systemexception() {
        throw new NO_IMPLEMENT();
    }

    public void userexception() throws user { throw new user(); }

    public void location_forward() { fail(); }

    public void test_service_context() {
        //
        // Test: get_slot
        //
        Any slotData = null;
        try {
            slotData = current_.get_slot(0);
        } catch (InvalidSlot ex) {
            fail();
        }
        int v = slotData.extract_long();
        assertEquals(10, v);

        //
        // Test: set_slot
        //
        slotData.insert_long(20);
        try {
            current_.set_slot(0, slotData);
        } catch (InvalidSlot ex) {
            fail();
        }
    }

    public String string_attrib() {
        return "TEST";
    }

    public void string_attrib(String param) {
        assertEquals("TEST", param);
    }

    public void one_string_in(String param) {
        assertEquals("TEST", param);
    }

    public void one_string_inout(StringHolder param) {
        assertEquals("TESTINOUT", param.value);
        param.value = "TEST";
    }

    public void one_string_out(StringHolder param) {
        param.value = "TEST";
    }

    public String one_string_return() {
        return "TEST";
    }

    public s struct_attrib() {
        s r = new s();
        r.sval = "TEST";
        return r;
    }

    public void struct_attrib(s param) {
        assertEquals("TEST", param.sval);
    }

    public void one_struct_in(s param) {
        assertEquals("TEST", param.sval);
    }

    public void one_struct_inout(sHolder param) {
        param.value.sval = "TEST";
    }

    public void one_struct_out(sHolder param) {
        param.value = new s();
        param.value.sval = "TEST";
    }

    public s one_struct_return() {
        s r = new s();
        r.sval = "TEST";
        return r;
    }

    public void deactivate() {
        System.out.println("TestInterface_Impl.deactivate() - calling orb.shutdown(false)");
        orb_.shutdown(false);
        System.out.println("TestInterface_Impl.deactivate() - returned from orb.shutdown(false)");
    }

    public POA _default_POA() {
        return poa_;
    }
}
