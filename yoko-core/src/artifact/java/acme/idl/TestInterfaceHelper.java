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
import org.omg.CORBA.BAD_OPERATION;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.ObjectImpl;
import org.omg.CORBA.portable.OutputStream;

//
// IDL:TestInterface:1.0
//
final public class TestInterfaceHelper
{
    public static void insert(Any any, TestInterface val)
    {
        any.insert_Object(val, type());
    }

    public static TestInterface extract(Any any) {
        if(any.type().equivalent(type())) return narrow(any.extract_Object());

        throw new BAD_OPERATION();
    }

    private static TypeCode typeCode_;

    public static TypeCode type() {
        if(null == typeCode_) {
            ORB orb = ORB.init();
            typeCode_ = orb.create_interface_tc(id(), "TestInterface");
        }

        return typeCode_;
    }

    public static String id()
    {
        return "IDL:TestInterface:1.0";
    }

    public static TestInterface read(InputStream in) {
        org.omg.CORBA.Object _ob_v = in.read_Object();

        try {
            return (TestInterface)_ob_v;
        } catch (ClassCastException ignored) {}

        ObjectImpl _ob_impl;
        _ob_impl = (ObjectImpl)_ob_v;
        _TestInterfaceStub _ob_stub = new _TestInterfaceStub();
        _ob_stub._set_delegate(_ob_impl._get_delegate());
        return _ob_stub;
    }

    public static void write(OutputStream out, TestInterface val)
    {
        out.write_Object(val);
    }

    public static TestInterface narrow(org.omg.CORBA.Object val) {
        if(null != val) {
            try {
                return (TestInterface)val;
            } catch(ClassCastException ignored) {}

            if(val._is_a(id()))
            {
                ObjectImpl _ob_impl;
                _TestInterfaceStub _ob_stub = new _TestInterfaceStub();
                _ob_impl = (ObjectImpl)val;
                _ob_stub._set_delegate(_ob_impl._get_delegate());
                return _ob_stub;
            }

            throw new org.omg.CORBA.BAD_PARAM();
        }

        return null;
    }

    public static TestInterface
    unchecked_narrow(org.omg.CORBA.Object val)
    {
        if(val != null)
        {
            try {
                return (TestInterface)val;
            } catch(ClassCastException ignored) {}

            ObjectImpl _ob_impl;
            _TestInterfaceStub _ob_stub = new _TestInterfaceStub();
            _ob_impl = (ObjectImpl)val;
            _ob_stub._set_delegate(_ob_impl._get_delegate());
            return _ob_stub;
        }

        return null;
    }
}
