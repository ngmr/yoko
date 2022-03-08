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

import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.CORBA.StructMember;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;

import static org.omg.CORBA.TCKind.tk_long;

//
// IDL:MyComponent:1.0
//
final public class MyComponentHelper
{
    public static void insert(Any any, MyComponent val) {
        OutputStream out = any.create_output_stream();
        write(out, val);
        any.read_value(out.create_input_stream(), type());
    }

    public static MyComponent extract(Any any) {
        if(any.type().equivalent(type())) return read(any.create_input_stream());
        throw new org.omg.CORBA.BAD_OPERATION();
    }

    private static TypeCode typeCode_;

    public static TypeCode type() {
        if(null == typeCode_) {
            ORB orb = ORB.init();
            StructMember[] members = new StructMember[1];

            members[0] = new StructMember();
            members[0].name = "val";
            members[0].type = orb.get_primitive_tc(tk_long);

            typeCode_ = orb.create_struct_tc(id(), "MyComponent", members);
        }

        return typeCode_;
    }

    public static String id() { return MyComponent._ob_id; }

    public static MyComponent read(InputStream in) {
        MyComponent _ob_v = new MyComponent();
        _ob_v.val = in.read_long();
        return _ob_v;
    }

    public static void write(OutputStream out, MyComponent val)
    {
        out.write_long(val.val);
    }
}
