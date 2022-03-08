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
import org.omg.CORBA.BAD_POLICY;
import org.omg.CORBA.BAD_POLICY_TYPE;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.Policy;
import org.omg.CORBA.PolicyError;
import org.omg.PortableInterceptor.PolicyFactory;

public final class MyServerPolicyFactory_impl extends LocalObject implements PolicyFactory {
    public Policy create_policy(int type, Any any) throws PolicyError {
        if (MY_SERVER_POLICY_ID.value == type) {
            try {
                int val = any.extract_long();
                return new MyServerPolicy_impl(val);
            } catch (BAD_OPERATION ex) {
                throw new PolicyError(BAD_POLICY_TYPE.value);
            }
        }
        throw new PolicyError(BAD_POLICY.value);
    }
}
