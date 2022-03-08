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
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.MARSHAL;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;

//
// IDL:MyServerPolicy:1.0
//
final public class MyServerPolicyHelper
{
    public static void insert(Any any, MyServerPolicy val)
    {
        any.insert_Object(val, type());
    }

    public static MyServerPolicy extract(Any any) {
        if(any.type().equivalent(type())) return narrow(any.extract_Object());

        throw new BAD_OPERATION();
    }

    private static TypeCode typeCode_;

    public static TypeCode type() {
        if(null == typeCode_) {
            ORB orb = ORB.init();
            typeCode_ = ((org.omg.CORBA_2_4.ORB)orb).create_local_interface_tc(id(), "MyServerPolicy");
        }

        return typeCode_;
    }

    public static String id()
    {
        return "IDL:MyServerPolicy:1.0";
    }

    @SuppressWarnings("unused")
    public static MyServerPolicy read(InputStream in)
    {
        throw new MARSHAL();
    }

    @SuppressWarnings("unused")
    public static void write(OutputStream out, MyServerPolicy val)
    {
        throw new MARSHAL();
    }

    public static MyServerPolicy narrow(org.omg.CORBA.Object val) {
        try {
            return (MyServerPolicy)val;
        } catch(ClassCastException ignored) {}

        throw new BAD_PARAM();
    }
}
