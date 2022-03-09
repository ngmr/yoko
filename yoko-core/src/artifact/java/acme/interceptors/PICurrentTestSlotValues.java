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
package acme.interceptors;

import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;

public enum PICurrentTestSlotValues {
    EMPTY,
    // client side
    CALLER, SEND_REQUEST, SEND_POLL, RECEIVE_REPLY, RECEIVE_EXCEPTION, RECEIVE_OTHER,
    // server side
    RECEIVE_REQUEST_SERVICE_CONTEXTS_0, RECEIVE_REQUEST_SERVICE_CONTEXTS_1,
    RECEIVE_REQUEST_0, RECEIVE_REQUEST_1,
    SEND_REPLY_0, SEND_REPLY_1,
    SEND_EXCEPTION_0, SEND_EXCEPTION_1,
    SEND_OTHER_0, SEND_OTHER_1;

    public final Any any;

    PICurrentTestSlotValues() {
        final Any any = ORB.init().create_any();
        final String name = name();
        if (!name.equals("EMPTY")) any.insert_string(name);
        this.any = any;
    }
}
