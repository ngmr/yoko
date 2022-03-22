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

import org.omg.CORBA.LocalObject;
import org.omg.CORBA.Policy;

final class MyClientPolicy_impl extends LocalObject implements MyClientPolicy {
    private final int value_;

    MyClientPolicy_impl(int value) {
        value_ = value;
    }

    //
    // Standard IDL to Java Mapping
    //

    public int value() {
        return value_;
    }

    public int policy_type() {
        return MY_CLIENT_POLICY_ID.value;
    }

    public Policy copy() {
        return this;
    }

    public void destroy() {
    }
}
