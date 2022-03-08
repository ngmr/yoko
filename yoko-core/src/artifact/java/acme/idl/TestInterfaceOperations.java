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

//
// IDL:TestInterface:1.0
//

import acme.idl.TestInterfacePackage.s;
import acme.idl.TestInterfacePackage.sHolder;
import acme.idl.TestInterfacePackage.user;
import org.omg.CORBA.StringHolder;

/***/

public interface TestInterfaceOperations {
    //
    // IDL:TestInterface/noargs:1.0
    //

    /***/

    void noargs();

    //
    // IDL:TestInterface/noargs_oneway:1.0
    //

    /***/

    void noargs_oneway();

    //
    // IDL:TestInterface/systemexception:1.0
    //

    /***/

    void systemexception();

    //
    // IDL:TestInterface/userexception:1.0
    //

    /***/

    void userexception() throws user;

    //
    // IDL:TestInterface/location_forward:1.0
    //

    /***/

    void location_forward();

    //
    // IDL:TestInterface/test_service_context:1.0
    //

    /***/

    void test_service_context();

    //
    // IDL:TestInterface/string_attrib:1.0
    //

    /***/

    String string_attrib();

    void string_attrib(String val);

    //
    // IDL:TestInterface/one_string_in:1.0
    //

    /***/

    void one_string_in(String param);

    //
    // IDL:TestInterface/one_string_inout:1.0
    //

    /***/

    void one_string_inout(StringHolder param);

    //
    // IDL:TestInterface/one_string_out:1.0
    //

    /***/

    void one_string_out(StringHolder param);

    //
    // IDL:TestInterface/one_string_return:1.0
    //

    /***/

    String one_string_return();

    //
    // IDL:TestInterface/struct_attrib:1.0
    //

    /***/

    s struct_attrib();

    void struct_attrib(s val);

    //
    // IDL:TestInterface/one_struct_in:1.0
    //

    /***/

    void one_struct_in(s param);

    //
    // IDL:TestInterface/one_struct_inout:1.0
    //

    /***/

    void one_struct_inout(sHolder param);

    //
    // IDL:TestInterface/one_struct_out:1.0
    //

    /***/

    void one_struct_out(sHolder param);

    //
    // IDL:TestInterface/one_struct_return:1.0
    //

    /***/

    s one_struct_return();

    //
    // IDL:TestInterface/deactivate:1.0
    //

    /***/

    void deactivate();
}
