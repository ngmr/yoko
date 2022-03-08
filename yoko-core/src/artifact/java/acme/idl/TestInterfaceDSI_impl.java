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
import acme.idl.TestInterfacePackage.sHelper;
import acme.idl.TestInterfacePackage.user;
import acme.idl.TestInterfacePackage.userHelper;
import org.omg.CORBA.ARG_IN;
import org.omg.CORBA.ARG_INOUT;
import org.omg.CORBA.ARG_OUT;
import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_OPERATION;
import org.omg.CORBA.BAD_OPERATIONHelper;
import org.omg.CORBA.Bounds;
import org.omg.CORBA.NO_IMPLEMENT;
import org.omg.CORBA.NO_IMPLEMENTHelper;
import org.omg.CORBA.NVList;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CORBA.ServerRequest;
import org.omg.PortableInterceptor.Current;
import org.omg.PortableInterceptor.CurrentHelper;
import org.omg.PortableInterceptor.InvalidSlot;
import org.omg.PortableServer.DynamicImplementation;
import org.omg.PortableServer.POA;

import static org.junit.Assert.*;

public final class TestInterfaceDSI_impl extends DynamicImplementation {
    private final ORB orb_;
    private final Current current_;

    public TestInterfaceDSI_impl(ORB orb) {
        orb_ = orb;

        try {
            org.omg.CORBA.Object obj = orb.resolve_initial_references("PICurrent");
            current_ = CurrentHelper.narrow(obj);
        } catch (InvalidName ignored) {
            fail();
            throw null;
        }
    }

    // ----------------------------------------------------------------------
    // TestInterfaceDSI_impl public member implementation
    // ----------------------------------------------------------------------

    static final String[] interfaces_ = { "IDL:TestInterface:1.0" };

    public String[] _all_interfaces(POA poa, byte[] oid) {
        return interfaces_;
    }

    public boolean _is_a(String name) {
        return name.equals("IDL:TestInterface:1.0") || super._is_a(name);
    }

    public void invoke(ServerRequest request) {
        String name = request.operation();

        if (name.equals("noargs")) {
            NVList list = orb_.create_list(0);
            request.arguments(list);

            return;
        }

        if (name.equals("noargs_oneway")) {
            NVList list = orb_.create_list(0);
            request.arguments(list);

            return;
        }

        if (name.equals("systemexception")) {
            NVList list = orb_.create_list(0);
            request.arguments(list);

            Any result = orb_.create_any();
            NO_IMPLEMENTHelper.insert(result, new NO_IMPLEMENT());
            request.set_exception(result);
            return;
        }

        if (name.equals("userexception")) {
            NVList list = orb_.create_list(0);
            request.arguments(list);

            Any result = orb_.create_any();
            userHelper.insert(result, new user());
            request.set_exception(result);
            return;
        }

        if (name.equals("location_forward")) {
            fail();
            return;
        }

        if (name.equals("test_service_context")) {
            NVList list = orb_.create_list(0);
            request.arguments(list);

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

            return;
        }

        if (name.equals("_get_string_attrib")) {
            NVList list = orb_.create_list(0);
            request.arguments(list);

            Any result = orb_.create_any();
            result.insert_string("TEST");
            request.set_result(result);

            return;
        }

        if (name.equals("_set_string_attrib")) {
            NVList list = orb_.create_list(0);
            Any any = orb_.create_any();
            any.type(orb_.create_string_tc(0));
            list.add_value("", any, ARG_IN.value);
            request.arguments(list);

            try {
                any = list.item(0).value();
            } catch (Bounds ex) {
                fail();
            }
            String param = any.extract_string();
            assertEquals("TEST", param);

            return;
        }

        if (name.equals("one_string_in")) {
            NVList list = orb_.create_list(0);
            Any any = orb_.create_any();
            any.type(orb_.create_string_tc(0));
            list.add_value("", any, ARG_IN.value);
            request.arguments(list);

            try {
                any = list.item(0).value();
            } catch (Bounds ex) {
                fail();
            }
            String param = any.extract_string();
            assertEquals("TEST", param);

            return;
        }

        if (name.equals("one_string_inout")) {
            NVList list = orb_.create_list(0);
            Any any = orb_.create_any();
            any.type(orb_.create_string_tc(0));
            list.add_value("", any, ARG_INOUT.value);
            request.arguments(list);

            try {
                any = list.item(0).value();
            } catch (Bounds ex) {
                fail();
            }
            String param = any.extract_string();
            assertEquals("TESTINOUT", param);
            any.insert_string("TEST");

            return;
        }

        if (name.equals("one_string_out")) {
            NVList list = orb_.create_list(0);
            Any any = orb_.create_any();
            any.type(orb_.create_string_tc(0));
            list.add_value("", any, ARG_OUT.value);
            request.arguments(list);

            try {
                any = list.item(0).value();
            } catch (Bounds ex) {
                fail();
            }
            any.insert_string("TEST");

            return;
        }

        if (name.equals("one_string_return")) {
            NVList list = orb_.create_list(0);
            request.arguments(list);

            Any result = orb_.create_any();
            result.insert_string("TEST");
            request.set_result(result);

            return;
        }

        if (name.equals("_get_struct_attrib")) {
            NVList list = orb_.create_list(0);
            request.arguments(list);

            s rc = new s();
            rc.sval = "TEST";

            Any result = orb_.create_any();
            sHelper.insert(result, rc);
            request.set_result(result);

            return;
        }

        if (name.equals("_set_struct_attrib")) {
            NVList list = orb_.create_list(0);
            Any any = orb_.create_any();
            any.type(sHelper.type());
            list.add_value("", any, ARG_IN.value);
            request.arguments(list);

            try {
                any = list.item(0).value();
            } catch (Bounds ex) {
                fail();
            }
            s param = sHelper.extract(any);
            assertEquals("TEST", param.sval);

            return;
        }

        if (name.equals("one_struct_in")) {
            NVList list = orb_.create_list(0);
            Any any = orb_.create_any();
            any.type(sHelper.type());
            list.add_value("", any, ARG_IN.value);
            request.arguments(list);

            try {
                any = list.item(0).value();
            } catch (Bounds ex) {
                fail();
            }
            s param = sHelper.extract(any);
            assertEquals("TEST", param.sval);

            return;
        }

        if (name.equals("one_struct_inout")) {
            NVList list = orb_.create_list(0);
            Any any = orb_.create_any();
            any.type(sHelper.type());
            list.add_value("", any, ARG_INOUT.value);
            request.arguments(list);

            try {
                any = list.item(0).value();
            } catch (Bounds ex) {
                fail();
            }
            s param = sHelper.extract(any);
            assertEquals("TESTINOUT", param.sval);
            s rc = new s();
            rc.sval = "TEST";
            sHelper.insert(any, rc);

            return;
        }

        if (name.equals("one_struct_out")) {
            NVList list = orb_.create_list(0);
            Any any = orb_.create_any();
            any.type(sHelper.type());
            list.add_value("", any, ARG_OUT.value);
            request.arguments(list);

            try {
                any = list.item(0).value();
            } catch (Bounds ex) {
                fail();
            }
            s rc = new s();
            rc.sval = "TEST";
            sHelper.insert(any, rc);

            return;
        }

        if (name.equals("one_struct_return")) {
            NVList list = orb_.create_list(0);
            request.arguments(list);

            s rc = new s();
            rc.sval = "TEST";

            Any result = orb_.create_any();
            sHelper.insert(result, rc);
            request.set_result(result);

            return;
        }

        if (name.equals("deactivate")) {
            NVList list = orb_.create_list(0);
            request.arguments(list);

            orb_.shutdown(false);

            return;
        }

        System.err.println("DSI implementation: unknown operation: " + name);

        NVList list = orb_.create_list(0);
        request.arguments(list);

        Any exAny = orb_.create_any();
        BAD_OPERATIONHelper.insert(exAny, new BAD_OPERATION());
        request.set_exception(exAny);
    }
}
