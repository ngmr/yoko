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

import org.omg.PortableInterceptor.Current;
import org.omg.PortableInterceptor.InvalidSlot;
import org.omg.PortableInterceptor.ServerRequestInfo;
import testify.iiop.TestServerRequestInterceptor;

import java.util.function.Supplier;

import static acme.interceptors.PICurrentTestSlotValues.EMPTY;
import static acme.interceptors.PICurrentTestSlotValues.RECEIVE_REQUEST_0;
import static acme.interceptors.PICurrentTestSlotValues.RECEIVE_REQUEST_1;
import static acme.interceptors.PICurrentTestSlotValues.RECEIVE_REQUEST_SERVICE_CONTEXTS_0;
import static acme.interceptors.PICurrentTestSlotValues.RECEIVE_REQUEST_SERVICE_CONTEXTS_1;
import static acme.interceptors.PICurrentTestSlotValues.SEND_EXCEPTION_0;
import static acme.interceptors.PICurrentTestSlotValues.SEND_EXCEPTION_1;
import static acme.interceptors.PICurrentTestSlotValues.SEND_OTHER_0;
import static acme.interceptors.PICurrentTestSlotValues.SEND_OTHER_1;
import static acme.interceptors.PICurrentTestSlotValues.SEND_REPLY_0;
import static acme.interceptors.PICurrentTestSlotValues.SEND_REPLY_1;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PICurrentServerInterceptor implements TestServerRequestInterceptor {
    static final int INVALID_SLOT_ID = -1;

    final int mySlotId;
    final Supplier<Current> threadSpecificPICurrentSupplier;

    public PICurrentServerInterceptor(Supplier<Current> piCurrentSupplier, int mySlotId) {
        this.mySlotId = mySlotId;
        this.threadSpecificPICurrentSupplier = piCurrentSupplier;
    }

    @Override
    public void receive_request_service_contexts(ServerRequestInfo ri) {
        assertThrows(InvalidSlot.class, () -> ri.get_slot(INVALID_SLOT_ID));
        assertThrows(InvalidSlot.class, () -> ri.set_slot(INVALID_SLOT_ID, RECEIVE_REQUEST_SERVICE_CONTEXTS_0.any));
        assertTrue(assertDoesNotThrow(() -> ri.get_slot(mySlotId)).equal(EMPTY.any));
        assertDoesNotThrow(() -> ri.set_slot(mySlotId, RECEIVE_REQUEST_SERVICE_CONTEXTS_0.any));

        final Current current = threadSpecificPICurrentSupplier.get();
        assertThrows(InvalidSlot.class, () -> current.get_slot(INVALID_SLOT_ID));
        assertThrows(InvalidSlot.class, () -> current.set_slot(INVALID_SLOT_ID, RECEIVE_REQUEST_SERVICE_CONTEXTS_1.any));
        assertTrue(assertDoesNotThrow(() -> current.get_slot(mySlotId)).equal(EMPTY.any));
        // CORBA 3.0.3, section 21.4.4.6
        // this should not propagate to other interception points
        assertDoesNotThrow(() -> current.set_slot(mySlotId, RECEIVE_REQUEST_SERVICE_CONTEXTS_1.any));
    }

    @Override
    public void receive_request(ServerRequestInfo ri) {
        assertThrows(InvalidSlot.class, () -> ri.get_slot(INVALID_SLOT_ID));
        assertThrows(InvalidSlot.class, () -> ri.set_slot(INVALID_SLOT_ID, RECEIVE_REQUEST_0.any));
        assertTrue(assertDoesNotThrow(() -> ri.get_slot(mySlotId)).equal(RECEIVE_REQUEST_SERVICE_CONTEXTS_0.any));
        assertThrows(InvalidSlot.class, () -> ri.set_slot(mySlotId, RECEIVE_REQUEST_0.any));

        final Current current = threadSpecificPICurrentSupplier.get();
        assertThrows(InvalidSlot.class, () -> current.get_slot(INVALID_SLOT_ID));
        assertThrows(InvalidSlot.class, () -> current.set_slot(INVALID_SLOT_ID, RECEIVE_REQUEST_1.any));
        assertTrue(assertDoesNotThrow(() -> ri.get_slot(mySlotId)).equal(EMPTY.any));
        assertDoesNotThrow(() -> current.set_slot(mySlotId, RECEIVE_REQUEST_1.any));
    }

    @Override
    public void send_reply(ServerRequestInfo ri) {
        assertThrows(InvalidSlot.class, () -> ri.get_slot(INVALID_SLOT_ID));
        assertThrows(InvalidSlot.class, () -> ri.set_slot(INVALID_SLOT_ID, SEND_REPLY_0.any));
        assertTrue(assertDoesNotThrow(() -> ri.get_slot(mySlotId)).equal(RECEIVE_REQUEST_0.any));
        assertThrows(InvalidSlot.class, () -> ri.set_slot(mySlotId, SEND_REPLY_0.any));

        final Current current = threadSpecificPICurrentSupplier.get();
        assertThrows(InvalidSlot.class, () -> current.get_slot(INVALID_SLOT_ID));
        assertThrows(InvalidSlot.class, () -> current.set_slot(INVALID_SLOT_ID, SEND_REPLY_1.any));
        assertTrue(assertDoesNotThrow(() -> current.get_slot(mySlotId)).equal(RECEIVE_REQUEST_1.any));
        assertDoesNotThrow(() -> current.set_slot(mySlotId, SEND_REPLY_1.any));
    }

    @Override
    public void send_exception(ServerRequestInfo ri) {
        assertThrows(InvalidSlot.class, () -> ri.get_slot(INVALID_SLOT_ID));
        assertThrows(InvalidSlot.class, () -> ri.set_slot(INVALID_SLOT_ID, SEND_EXCEPTION_0.any));
        assertTrue(assertDoesNotThrow(() -> ri.get_slot(mySlotId)).equal(RECEIVE_REQUEST_0.any));
        assertThrows(InvalidSlot.class, () -> ri.set_slot(mySlotId, SEND_EXCEPTION_0.any));

        final Current current = threadSpecificPICurrentSupplier.get();
        assertThrows(InvalidSlot.class, () -> current.get_slot(INVALID_SLOT_ID));
        assertThrows(InvalidSlot.class, () -> current.set_slot(INVALID_SLOT_ID, SEND_EXCEPTION_1.any));
        assertTrue(assertDoesNotThrow(() -> current.get_slot(mySlotId)).equal(RECEIVE_REQUEST_1.any));
        assertDoesNotThrow(() -> current.set_slot(mySlotId, SEND_EXCEPTION_1.any));
    }

    @Override
    public void send_other(ServerRequestInfo ri) {
        assertThrows(InvalidSlot.class, () -> ri.get_slot(INVALID_SLOT_ID));
        assertThrows(InvalidSlot.class, () -> ri.set_slot(INVALID_SLOT_ID, SEND_OTHER_0.any));
        assertTrue(assertDoesNotThrow(() -> ri.get_slot(mySlotId)).equal(RECEIVE_REQUEST_0.any));
        assertThrows(InvalidSlot.class, () -> ri.set_slot(mySlotId, SEND_OTHER_0.any));

        final Current current = threadSpecificPICurrentSupplier.get();
        assertThrows(InvalidSlot.class, () -> current.get_slot(INVALID_SLOT_ID));
        assertThrows(InvalidSlot.class, () -> current.set_slot(INVALID_SLOT_ID, SEND_OTHER_1.any));
        assertTrue(assertDoesNotThrow(() -> current.get_slot(mySlotId)).equal(RECEIVE_REQUEST_1.any));
        assertDoesNotThrow(() -> current.set_slot(mySlotId, SEND_OTHER_1.any));
    }
}
