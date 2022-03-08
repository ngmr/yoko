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

import org.omg.CORBA.TypeCode;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;
import org.omg.CORBA.portable.Streamable;

//
// IDL:MyClientPolicy:1.0
//
final public class MyClientPolicyHolder implements Streamable
{
    public MyClientPolicy value;

    public MyClientPolicyHolder() {}

    public MyClientPolicyHolder(MyClientPolicy initial)
    {
        value = initial;
    }

    public void _read(InputStream in)
    {
        value = MyClientPolicyHelper.read(in);
    }

    public void _write(OutputStream out)
    {
        MyClientPolicyHelper.write(out, value);
    }

    public TypeCode _type()
    {
        return MyClientPolicyHelper.type();
    }
}
