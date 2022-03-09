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

import org.omg.CORBA.NO_IMPLEMENT;
import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.Current;
import org.omg.PortableInterceptor.InvalidSlot;
import testify.iiop.TestClientRequestInterceptor;

import java.util.function.Supplier;

import static acme.interceptors.PICurrentTestSlotValues.CALLER;
import static acme.interceptors.PICurrentTestSlotValues.EMPTY;
import static acme.interceptors.PICurrentTestSlotValues.RECEIVE_EXCEPTION;
import static acme.interceptors.PICurrentTestSlotValues.RECEIVE_OTHER;
import static acme.interceptors.PICurrentTestSlotValues.RECEIVE_REPLY;
import static acme.interceptors.PICurrentTestSlotValues.SEND_POLL;
import static acme.interceptors.PICurrentTestSlotValues.SEND_REQUEST;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PICurrentClientInterceptor implements TestClientRequestInterceptor {
    public static final int INVALID_SLOT_ID = -1;

    private final int mySlotId;
    private final Supplier<Current> threadSpecificPICurrentSupplier;

    public PICurrentClientInterceptor(Supplier<Current> piCurrentSupplier, int mySlotId) {
        this.mySlotId = mySlotId;
        this.threadSpecificPICurrentSupplier = piCurrentSupplier;
    }

    @Override
    public void send_request(ClientRequestInfo ri) {
        assertThrows(InvalidSlot.class, () -> ri.get_slot(INVALID_SLOT_ID));
        assertTrue(assertDoesNotThrow(() -> ri.get_slot(mySlotId)).equal(CALLER.any));

        final Current current = threadSpecificPICurrentSupplier.get();
        assertThrows(InvalidSlot.class, () -> current.get_slot(INVALID_SLOT_ID));
        assertThrows(InvalidSlot.class, () -> current.set_slot(INVALID_SLOT_ID, SEND_REQUEST.any));
        assertTrue(assertDoesNotThrow(() -> ri.get_slot(mySlotId)).equal(EMPTY.any));
        // CORBA 3.0.3, section 21.4.4.6
        // this should not propagate to other interception points
        assertDoesNotThrow(() -> current.set_slot(mySlotId, SEND_REQUEST.any));
    }

    @Override
    public void send_poll(ClientRequestInfo ri) {
        assertThrows(InvalidSlot.class, () -> ri.get_slot(INVALID_SLOT_ID));
        assertTrue(assertDoesNotThrow(() -> ri.get_slot(mySlotId)).equal(CALLER.any));

        final Current current = threadSpecificPICurrentSupplier.get();
        assertThrows(InvalidSlot.class, () -> current.get_slot(INVALID_SLOT_ID));
        assertThrows(InvalidSlot.class, () -> current.set_slot(INVALID_SLOT_ID, SEND_POLL.any));
        assertTrue(assertDoesNotThrow(() -> ri.get_slot(mySlotId)).equal(EMPTY.any));
        // CORBA 3.0.3, section 21.4.4.6
        // this should not propagate to other interception points
        assertDoesNotThrow(() -> current.set_slot(mySlotId, SEND_POLL.any));
    }

    @Override
    public void receive_reply(ClientRequestInfo ri) {
        assertThrows(InvalidSlot.class, () -> ri.get_slot(INVALID_SLOT_ID));
        assertTrue(assertDoesNotThrow(() -> ri.get_slot(mySlotId)).equal(CALLER.any));

        final Current current = threadSpecificPICurrentSupplier.get();
        assertThrows(InvalidSlot.class, () -> current.get_slot(INVALID_SLOT_ID));
        assertThrows(InvalidSlot.class, () -> current.set_slot(INVALID_SLOT_ID, RECEIVE_REPLY.any));
        assertTrue(assertDoesNotThrow(() -> ri.get_slot(mySlotId)).equal(EMPTY.any));
        // CORBA 3.0.3, section 21.4.4.6
        // this should not propagate to other interception points
        assertDoesNotThrow(() -> current.set_slot(mySlotId, RECEIVE_REPLY.any));
    }

    @Override
    public void receive_exception(ClientRequestInfo ri) {
        assertThrows(InvalidSlot.class, () -> ri.get_slot(INVALID_SLOT_ID));
        assertTrue(assertDoesNotThrow(() -> ri.get_slot(mySlotId)).equal(CALLER.any));

        final Current current = threadSpecificPICurrentSupplier.get();
        assertThrows(InvalidSlot.class, () -> current.get_slot(INVALID_SLOT_ID));
        assertThrows(InvalidSlot.class, () -> current.set_slot(INVALID_SLOT_ID, RECEIVE_EXCEPTION.any));
        assertTrue(assertDoesNotThrow(() -> ri.get_slot(mySlotId)).equal(EMPTY.any));
        // CORBA 3.0.3, section 21.4.4.6
        // this should not propagate to other interception points
        assertDoesNotThrow(() -> current.set_slot(mySlotId, RECEIVE_EXCEPTION.any));
    }

    @Override
    public void receive_other(ClientRequestInfo ri) {
        assertThrows(InvalidSlot.class, () -> ri.get_slot(INVALID_SLOT_ID));
        assertTrue(assertDoesNotThrow(() -> ri.get_slot(mySlotId)).equal(CALLER.any));

        final Current current = threadSpecificPICurrentSupplier.get();
        assertThrows(InvalidSlot.class, () -> current.get_slot(INVALID_SLOT_ID));
        assertThrows(InvalidSlot.class, () -> current.set_slot(INVALID_SLOT_ID, RECEIVE_OTHER.any));
        assertTrue(assertDoesNotThrow(() -> ri.get_slot(mySlotId)).equal(EMPTY.any));
        // CORBA 3.0.3, section 21.4.4.6
        // this should not propagate to other interception points
        assertDoesNotThrow(() -> current.set_slot(mySlotId, RECEIVE_OTHER.any));
        throw new NO_IMPLEMENT(); // Eat the location forward
    }
}
