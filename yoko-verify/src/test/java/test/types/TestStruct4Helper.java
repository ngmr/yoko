/*
 * Copyright 2010 IBM Corporation and others.
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
package test.types;

//
// IDL:TestStruct4:1.0
//
final public class TestStruct4Helper
{
    public static void
    insert(org.omg.CORBA.Any any, TestStruct4 val)
    {
        org.omg.CORBA.portable.OutputStream out = any.create_output_stream();
        write(out, val);
        any.read_value(out.create_input_stream(), type());
    }

    public static TestStruct4
    extract(org.omg.CORBA.Any any)
    {
        if(any.type().equivalent(type()))
            return read(any.create_input_stream());
        else
            throw new org.omg.CORBA.BAD_OPERATION();
    }

    private static org.omg.CORBA.TypeCode typeCode_;

    public static org.omg.CORBA.TypeCode
    type()
    {
        if(typeCode_ == null)
        {
            org.omg.CORBA.ORB orb = org.omg.CORBA.ORB.init();
            org.omg.CORBA.StructMember[] members = new org.omg.CORBA.StructMember[2];

            members[0] = new org.omg.CORBA.StructMember();
            members[0].name = "a";
            members[0].type = TestStruct3Helper.type();

            members[1] = new org.omg.CORBA.StructMember();
            members[1].name = "b";
            members[1].type = orb.create_sequence_tc(0, TestStruct3Helper.type());

            typeCode_ = orb.create_struct_tc(id(), "TestStruct4", members);
        }

        return typeCode_;
    }

    public static String
    id()
    {
        return "IDL:TestStruct4:1.0";
    }

    public static TestStruct4
    read(org.omg.CORBA.portable.InputStream in)
    {
        TestStruct4 _ob_v = new TestStruct4();
        _ob_v.a = TestStruct3Helper.read(in);
        int len0 = in.read_ulong();
        _ob_v.b = new TestStruct3[len0];
        for(int i0 = 0; i0 < len0; i0++)
            _ob_v.b[i0] = TestStruct3Helper.read(in);
        return _ob_v;
    }

    public static void
    write(org.omg.CORBA.portable.OutputStream out, TestStruct4 val)
    {
        TestStruct3Helper.write(out, val.a);
        int len0 = val.b.length;
        out.write_ulong(len0);
        for(int i0 = 0; i0 < len0; i0++)
            TestStruct3Helper.write(out, val.b[i0]);
    }
}
