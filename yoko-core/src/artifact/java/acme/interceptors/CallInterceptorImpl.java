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

import acme.idl.MY_CLIENT_POLICY_ID;
import acme.idl.MY_COMPONENT_ID;
import acme.idl.MyClientPolicy;
import acme.idl.MyClientPolicyHelper;
import acme.idl.MyComponentHelper;
import acme.idl.REPLY_CONTEXT_1_ID;
import acme.idl.REPLY_CONTEXT_2_ID;
import acme.idl.REPLY_CONTEXT_3_ID;
import acme.idl.REPLY_CONTEXT_4_ID;
import acme.idl.REQUEST_CONTEXT_ID;
import acme.idl.ReplyContext;
import acme.idl.ReplyContextHelper;
import acme.idl.RequestContext;
import acme.idl.RequestContextHelper;
import acme.idl.TestInterfacePackage.sHelper;
import acme.idl.TestInterfacePackage.userHelper;
import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_INV_ORDER;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.NO_IMPLEMENT;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ParameterMode;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;
import org.omg.Dynamic.Parameter;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecFactory;
import org.omg.IOP.CodecFactoryHelper;
import org.omg.IOP.ENCODING_CDR_ENCAPS;
import org.omg.IOP.Encoding;
import org.omg.IOP.ServiceContext;
import org.omg.IOP.TAG_INTERNET_IOP;
import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.ClientRequestInterceptor;
import org.omg.PortableInterceptor.Current;
import org.omg.PortableInterceptor.CurrentHelper;
import org.omg.PortableInterceptor.LOCATION_FORWARD;
import org.omg.PortableInterceptor.SUCCESSFUL;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;
import org.omg.PortableInterceptor.USER_EXCEPTION;

import java.util.Arrays;

import static org.apache.yoko.orb.OB.Util.unmarshalSystemException;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.omg.CORBA.ParameterMode.PARAM_IN;
import static org.omg.CORBA.ParameterMode.PARAM_INOUT;
import static org.omg.CORBA.ParameterMode.PARAM_OUT;

public final class CallInterceptorImpl extends LocalObject implements ClientRequestInterceptor {
    private int req;
    private final Codec codec;
    private final Current pic;

    private static String getContent(String opName, Any any) {
        return opName.contains("string") ? any.extract_string() : sHelper.extract(any).sval;
    }

    private void testArgs(ClientRequestInfo ri, boolean resultAvail) {
        final String op = ri.operation();
        final Parameter[] args = ri.arguments();
        if (op.startsWith("_get_")) {
            assertEquals(0, args.length);
            if (resultAvail) assertThat(getContent(op, ri.result()), startsWith("TEST"));
        } else if (op.startsWith("_set_")) {
            assertEquals(1, args.length);
            assertSame(PARAM_IN, args[0].mode);
            if (resultAvail) assertThat(getContent(op, args[0].argument), startsWith("TEST"));
        } else if (op.startsWith("one_")) {
            final String which = op.substring("one_string_".length()); // or "one_struct_"
            if (which.equals("return")) {
                assertEquals(0, args.length);
                if (resultAvail) assertThat(getContent(op, ri.result()), startsWith("TEST"));
            } else {
                assertEquals(1, args.length);
                ParameterMode mode = which.equals("in") ? PARAM_IN : which.equals("out") ? PARAM_OUT : PARAM_INOUT;
                assertSame(mode, args[0].mode);
                if (resultAvail) assertSame(TCKind.tk_void, ri.result().type().kind());
                if (resultAvail || PARAM_OUT != mode) assertThat(getContent(op, args[0].argument), startsWith("TEST"));
            }
        } else {
            assertEquals(0, args.length);
        }
        if (!resultAvail) {
            assertThrows(BAD_INV_ORDER.class, ri::result);
        }
    }

    public CallInterceptorImpl(ORB orb) throws Exception {
        final CodecFactory factory = CodecFactoryHelper.narrow(orb.resolve_initial_references("CodecFactory"));
        assertNotNull(factory);
        pic = CurrentHelper.narrow(orb.resolve_initial_references("PICurrent"));
        assertNotNull(pic);
        codec = factory.create_codec(new Encoding(ENCODING_CDR_ENCAPS.value, (byte) 0, (byte) 0));
        assertNotNull(codec);
    }

    public String name() { return "CRI"; }

    public void destroy() {}

    private void checkExceptions(String op, TypeCode[] exceptions) {
        if (op.equals("userexception")) {
            assertEquals(1, exceptions.length);
            assertTrue(exceptions[0].equal(userHelper.type()));
        } else {
            assertEquals(0, exceptions.length);
        }
    }

    public void send_request(ClientRequestInfo ri) {
        req++;
        ri.request_id();
        final String op = ri.operation();
        final boolean oneway = op.equals("noargs_oneway");
        testArgs(ri, false);
        checkExceptions(op, ri.exceptions());
        assertNotEquals(oneway, ri.response_expected(), "Either the message should be one-way or a response should be expected");
        assertNotNull(ri.target());
        assertNotNull(ri.effective_target());
        assertEquals(TAG_INTERNET_IOP.value, ri.effective_profile().tag);
        assertThrows(BAD_INV_ORDER.class, ri::reply_status);
        assertThrows(BAD_INV_ORDER.class, ri::received_exception);
        assertThrows(BAD_INV_ORDER.class, ri::forward_reference);
        final byte[] componentData = ri.get_effective_component(MY_COMPONENT_ID.value).component_data;
        final Any componentAny = assertDoesNotThrow(() -> codec.decode_value(componentData, MyComponentHelper.type()));
        assertEquals(10, MyComponentHelper.extract(componentAny).val);
        final MyClientPolicy myClientPolicy = MyClientPolicyHelper.narrow(ri.get_request_policy(MY_CLIENT_POLICY_ID.value));
        assertNotNull(myClientPolicy);
        assertEquals(10, myClientPolicy.value());
        assertThrows(BAD_PARAM.class, () -> ri.get_request_service_context(REQUEST_CONTEXT_ID.value));
        assertThrows(BAD_PARAM.class, () -> ri.get_reply_service_context(REQUEST_CONTEXT_ID.value));
        if (op.equals("test_service_context")) {
            final RequestContext rc = new RequestContext("request", assertDoesNotThrow(() -> ri.get_slot(0)).extract_long());
            assertEquals(10, rc.val);
            final Any any = ORB.init().create_any();
            RequestContextHelper.insert(any, rc);
            final byte[] data = assertDoesNotThrow(() -> codec.encode_value(any));
            ri.add_request_service_context(new ServiceContext(REQUEST_CONTEXT_ID.value, Arrays.copyOf(data, data.length)), false);
            assertDoesNotThrow(() -> ri.get_request_service_context(REQUEST_CONTEXT_ID.value));
        } else {
            assertThrows(BAD_PARAM.class, () -> ri.get_request_service_context(REQUEST_CONTEXT_ID.value));
        }

        assertEquals(TCKind._tk_null, assertDoesNotThrow(() -> ri.get_slot(0)).type().kind().value());
        final Any newSlotData = ORB.init().create_any();
        newSlotData.insert_long(15);
        assertDoesNotThrow(() -> pic.set_slot(0, newSlotData));
    }

    public void send_poll(ClientRequestInfo ri) {
        fail();
    }

    private void checkServiceContext(ClientRequestInfo ri, int contextId, String expectedData, int expectedValue) {
        final ServiceContext sc = assertDoesNotThrow(() -> ri.get_reply_service_context(contextId));
        assertEquals(sc.context_id, contextId);
        final byte[] data = Arrays.copyOf(sc.context_data, sc.context_data.length);
        final Any any = assertDoesNotThrow(() -> codec.decode_value(data, ReplyContextHelper.type()));
        final ReplyContext context = assertDoesNotThrow(() -> ReplyContextHelper.extract(any));
        assertEquals(expectedData, context.data);
        assertEquals(expectedValue, context.val);
    }

    public void receive_reply(ClientRequestInfo ri) {
        ri.request_id();
        final String op = ri.operation();
        if (op.equals("deactivate")) return;
        testArgs(ri, true);
        checkExceptions(op, ri.exceptions());
        assertNotEquals(op.equals("noargs_oneway"), ri.response_expected(), "Either the message should be one-way or a response should be expected");
        assertNotNull(ri.target());
        assertNotNull(ri.effective_target());
        assertEquals(TAG_INTERNET_IOP.value, ri.effective_profile().tag);
        final byte[] componentData = ri.get_effective_component(MY_COMPONENT_ID.value).component_data;
        final Any componentAny = assertDoesNotThrow(() -> codec.decode_value(componentData, MyComponentHelper.type()));
        assertEquals(10, MyComponentHelper.extract(componentAny).val);
        final MyClientPolicy myClientPolicy = MyClientPolicyHelper.narrow(ri.get_request_policy(MY_CLIENT_POLICY_ID.value));
        assertNotNull(myClientPolicy);
        assertEquals(10, myClientPolicy.value());
        assertEquals(SUCCESSFUL.value, ri.reply_status());
        assertThrows(BAD_INV_ORDER.class, ri::received_exception);
        assertThrows(BAD_INV_ORDER.class, ri::forward_reference);
        if (op.equals("test_service_context")) {
            assertDoesNotThrow(() -> ri.get_request_service_context(REQUEST_CONTEXT_ID.value));
            checkServiceContext(ri, REPLY_CONTEXT_1_ID.value, "reply1", 101);
            checkServiceContext(ri, REPLY_CONTEXT_2_ID.value, "reply2", 102);
            checkServiceContext(ri, REPLY_CONTEXT_3_ID.value, "reply3", 103);
            checkServiceContext(ri, REPLY_CONTEXT_4_ID.value, "reply4", 104);
        } else {
            assertThrows(BAD_PARAM.class, () -> ri.get_request_service_context(REQUEST_CONTEXT_ID.value));
            assertThrows(BAD_PARAM.class, () -> ri.get_reply_service_context(REPLY_CONTEXT_1_ID.value));
        }
        assertThrows(BAD_INV_ORDER.class, () -> ri.add_request_service_context(new ServiceContext(REQUEST_CONTEXT_ID.value, null), false));
        assertEquals(15, assertDoesNotThrow(() -> ri.get_slot(0)).extract_long());
        final Any newSlotData = ORB.init().create_any();
        newSlotData.insert_long(16);
        assertDoesNotThrow(() -> pic.set_slot(0, newSlotData));
    }

    public void receive_other(ClientRequestInfo ri) {
        ri.request_id();
        final String op = ri.operation();
        assertEquals("location_forward", op);
        assertThrows(BAD_INV_ORDER.class, ri::arguments);
        checkExceptions(op, ri.exceptions());
        assertTrue(ri.response_expected());
        assertNotNull(ri.target());
        assertNotNull(ri.effective_target());
        assertEquals(TAG_INTERNET_IOP.value, ri.effective_profile().tag);
        final byte[] componentData = ri.get_effective_component(MY_COMPONENT_ID.value).component_data;
        final Any componentAny = assertDoesNotThrow(() -> codec.decode_value(componentData, MyComponentHelper.type()));
        assertEquals(10, MyComponentHelper.extract(componentAny).val);
        final MyClientPolicy myClientPolicy = MyClientPolicyHelper.narrow(ri.get_request_policy(MY_CLIENT_POLICY_ID.value));
        assertNotNull(myClientPolicy);
        assertEquals(10, myClientPolicy.value());
        assertEquals(LOCATION_FORWARD.value, ri.reply_status());
        assertThrows(BAD_INV_ORDER.class, ri::received_exception);
        assertDoesNotThrow(ri::forward_reference);
        assertThrows(BAD_PARAM.class, () -> ri.get_request_service_context(REQUEST_CONTEXT_ID.value));
        assertThrows(BAD_PARAM.class, () -> ri.get_reply_service_context(REPLY_CONTEXT_1_ID.value));
        assertThrows(BAD_INV_ORDER.class, () -> ri.add_request_service_context(new ServiceContext(REQUEST_CONTEXT_ID.value, null), false));
        assertEquals(15, assertDoesNotThrow(() -> ri.get_slot(0)).extract_long());
        final Any newSlotData = ORB.init().create_any();
        newSlotData.insert_long(16);
        assertDoesNotThrow(() -> pic.set_slot(0, newSlotData));
        throw new NO_IMPLEMENT(); // Eat the location forward
    }

    public void receive_exception(ClientRequestInfo ri) {
        ri.request_id();
        final String op = ri.operation();
        if (op.equals("deactivate")) return;
        assertTrue(op.equals("systemexception") || op.equals("userexception"));
        assertThrows(BAD_INV_ORDER.class, ri::arguments);
        checkExceptions(op, ri.exceptions());
        assertTrue(ri.response_expected());
        assertNotNull(ri.target());
        assertNotNull(ri.effective_target());
        assertEquals( TAG_INTERNET_IOP.value, ri.effective_profile().tag);
        final byte[] componentData = ri.get_effective_component(MY_COMPONENT_ID.value).component_data;
        final Any componentAny = assertDoesNotThrow(() -> codec.decode_value(componentData, MyComponentHelper.type()));
        assertEquals(10, MyComponentHelper.extract(componentAny).val);
        final MyClientPolicy myClientPolicy = MyClientPolicyHelper.narrow(ri.get_request_policy(MY_CLIENT_POLICY_ID.value));
        assertNotNull(myClientPolicy);
        assertEquals(10, myClientPolicy.value());
        final Any rc = assertDoesNotThrow(ri::received_exception);
        if (op.equals("userexception")) {
            assertEquals(USER_EXCEPTION.value, ri.reply_status());
            assertEquals(userHelper.id(), ri.received_exception_id());
            assertDoesNotThrow(() -> userHelper.extract(rc));
        } else {
            assertEquals(SYSTEM_EXCEPTION.value, ri.reply_status());
            assertDoesNotThrow(() -> unmarshalSystemException(rc.create_input_stream()));
        }
        assertThrows(BAD_INV_ORDER.class, ri::forward_reference);
        assertThrows(BAD_PARAM.class, () -> ri.get_reply_service_context(REPLY_CONTEXT_1_ID.value));
        assertThrows(BAD_PARAM.class, () -> ri.get_request_service_context(REQUEST_CONTEXT_ID.value));
        assertThrows(BAD_INV_ORDER.class, () -> ri.add_request_service_context(new ServiceContext(REQUEST_CONTEXT_ID.value, null), false));
        assertEquals(15, assertDoesNotThrow(() -> ri.get_slot(0)).extract_long());
        final Any newSlotData = ORB.init().create_any();
        newSlotData.insert_long(16);
        assertDoesNotThrow(() -> pic.set_slot(0, newSlotData));
    }

    public int _OB_numReq() {
        return req;
    }
}
