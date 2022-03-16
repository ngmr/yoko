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
package org.apache.yoko.orb.OB;

import acme.idl.MY_CLIENT_POLICY_ID;
import acme.idl.MY_SERVER_POLICY_ID;
import acme.idl.MyClientPolicyFactory;
import acme.idl.MyServerPolicy;
import acme.idl.MyServerPolicyFactory;
import acme.idl.MyServerPolicyHelper;
import acme.idl.REPLY_CONTEXT_1_ID;
import acme.idl.REPLY_CONTEXT_2_ID;
import acme.idl.REPLY_CONTEXT_3_ID;
import acme.idl.REPLY_CONTEXT_4_ID;
import acme.idl.REQUEST_CONTEXT_ID;
import acme.idl.ReplyContext;
import acme.idl.ReplyContextHelper;
import acme.idl.RequestContext;
import acme.idl.RequestContextHelper;
import acme.idl.TestInterface;
import acme.idl.TestInterfaceDSI_impl;
import acme.idl.TestInterfaceHelper;
import acme.idl.TestInterfacePackage.s;
import acme.idl.TestInterfacePackage.sHelper;
import acme.idl.TestInterfacePackage.userHelper;
import acme.idl.TestInterface_impl;
import acme.idl.TestLocator_impl;
import org.apache.yoko.orb.OBPortableServer.POAHelper;
import org.junit.jupiter.api.BeforeAll;
import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_INV_ORDER;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.INV_POLICY;
import org.omg.CORBA.NO_RESOURCES;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ParameterMode;
import org.omg.CORBA.Policy;
import org.omg.CORBA.PolicyError;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;
import org.omg.Dynamic.Parameter;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecFactoryHelper;
import org.omg.IOP.ENCODING_CDR_ENCAPS;
import org.omg.IOP.Encoding;
import org.omg.IOP.ServiceContext;
import org.omg.PortableInterceptor.LOCATION_FORWARD;
import org.omg.PortableInterceptor.ORBInitInfo;
import org.omg.PortableInterceptor.SUCCESSFUL;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.omg.PortableInterceptor.USER_EXCEPTION;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAManager;
import testify.bus.Bus;
import testify.bus.StringSpec;
import testify.iiop.TestORBInitializer;
import testify.iiop.TestServerRequestInterceptor;
import testify.jupiter.annotation.iiop.ConfigureOrb.UseWithOrb;
import testify.jupiter.annotation.iiop.ConfigureServer;
import testify.jupiter.annotation.iiop.ConfigureServer.BeforeServer;
import testify.streams.BiStream;

import java.util.EnumMap;

import static java.util.Arrays.copyOf;
import static java.util.function.Function.identity;
import static org.apache.yoko.orb.OB.PortableInterceptorTest.StubType.DSI_INTERFACE;
import static org.apache.yoko.orb.OB.PortableInterceptorTest.StubType.TEST_INTERFACE;
import static org.apache.yoko.orb.OB.Util.unmarshalSystemException;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.omg.CORBA.SetOverrideType.ADD_OVERRIDE;
import static org.omg.PortableServer.IdAssignmentPolicyValue.USER_ID;
import static org.omg.PortableServer.ImplicitActivationPolicyValue.NO_IMPLICIT_ACTIVATION;
import static org.omg.PortableServer.LifespanPolicyValue.PERSISTENT;
import static org.omg.PortableServer.RequestProcessingPolicyValue.USE_SERVANT_MANAGER;
import static org.omg.PortableServer.ServantRetentionPolicyValue.NON_RETAIN;

@ConfigureServer
public class PortableInterceptorTest {
    @UseWithOrb("client orb")
    public static final class MyClientPolicyInitializer implements TestORBInitializer {
        @Override
        public void pre_init(ORBInitInfo info) {
            assertDoesNotThrow(() -> info.register_policy_factory(MY_CLIENT_POLICY_ID.value, new MyClientPolicyFactory()));
        }
    }

    @BeforeServer
    public static void beforeServer(ORB orb, Bus bus) {
        // can't currently configure the POA that the framework would supply, so we'll create our own
        final POA rootPoa = POAHelper.narrow(assertDoesNotThrow(() -> orb.resolve_initial_references("RootPOA")));
        final POAManager poaManager = rootPoa.the_POAManager();
        final Any any = orb.create_any();
        any.insert_long(10);
        final Policy[] policies = {
                rootPoa.create_lifespan_policy(PERSISTENT),
                rootPoa.create_id_assignment_policy(USER_ID),
                rootPoa.create_request_processing_policy(USE_SERVANT_MANAGER),
                rootPoa.create_servant_retention_policy(NON_RETAIN),
                rootPoa.create_implicit_activation_policy(NO_IMPLICIT_ACTIVATION),
                assertDoesNotThrow(() -> orb.create_policy(MY_SERVER_POLICY_ID.value, any))
        };

        final POA persistentPoa = assertDoesNotThrow(() -> rootPoa.create_POA("persistent", poaManager, policies));

        final TestInterface_impl impl = new TestInterface_impl(orb, persistentPoa);
        final TestInterfaceDSI_impl dsiImpl = new TestInterfaceDSI_impl(orb);
        final TestLocator_impl locatorImpl = new TestLocator_impl(impl, dsiImpl);
        assertDoesNotThrow(() -> persistentPoa.set_servant_manager(locatorImpl._this(orb)));

        assertThat(CodecFactoryHelper.narrow(assertDoesNotThrow(() -> orb.resolve_initial_references("CodecFactory"))), notNullValue());

        final org.omg.CORBA.Object objImpl = persistentPoa.create_reference_with_id("test".getBytes(), TestInterfaceHelper.id());
        final org.omg.CORBA.Object objDsiImpl = persistentPoa.create_reference_with_id("testDSI".getBytes(), TestInterfaceHelper.id());
        bus.put(TEST_INTERFACE, orb.object_to_string(objImpl));
        bus.put(DSI_INTERFACE, orb.object_to_string(objDsiImpl));
    }

    enum StubType implements StringSpec {TEST_INTERFACE, DSI_INTERFACE}
    static final EnumMap<StubType, TestInterface> STUB_MAP = new EnumMap<>(StubType.class);

    @BeforeAll
    public static void beforeAll(ORB orb, Bus bus) throws PolicyError {
        Any any = orb.create_any();
        any.insert_long(10);
        Policy[] pl = { orb.create_policy(MY_CLIENT_POLICY_ID.value, any) };
        // populate the enum map with stubs narrowed from the stringified IORs put in the bus by the server
        BiStream.of(StubType.values(), identity())
                .mapValues(bus::get)
                .mapValues(orb::string_to_object)
                .mapValues(o -> o._set_policy_override(pl, ADD_OVERRIDE))
                .mapValues(TestInterfaceHelper::narrow)
                .forEach(STUB_MAP::put);
    }

    @UseWithOrb("server orb")
    public static final class MyServerPolicyInitializer implements TestORBInitializer {
        @Override
        public void pre_init(ORBInitInfo info) {
            assertDoesNotThrow(() -> info.register_policy_factory(MY_SERVER_POLICY_ID.value, new MyServerPolicyFactory()));
        }
    }

    @UseWithOrb("server orb")
    public static final class MyServerRequestInterceptor implements TestServerRequestInterceptor {
        private Codec cdrCodec;

        @Override
        public void post_init(ORBInitInfo info) {
            try {
                Encoding encoding = new Encoding(ENCODING_CDR_ENCAPS.value, (byte) 0, (byte) 0);
                cdrCodec = assertDoesNotThrow(() -> info.codec_factory().create_codec(encoding));
            } finally {
                //TestServerRequestInterceptor.super.post_init(info);
            }
        }

        private void testArgs(ServerRequestInfo ri, boolean resultAvail) {
            String op = ri.operation();
            Parameter[] args = ri.arguments();
            if (op.startsWith("_set_") || op.startsWith("_get_")) {
                boolean isstr; // struct or string?
                isstr = (op.contains("string"));
                if (op.startsWith("_get_")) {
                    assertThat(args.length, is(0));
                    if (resultAvail) {
                        // Test: result
                        Any result = ri.result();
                        if (isstr) {
                            String str = result.extract_string();
                            assertThat(str.startsWith("TEST"), is(true));
                        } else {
                            s sp = sHelper.extract(result);
                            assertThat(sp.sval.startsWith("TEST"), is(true));
                        }
                    }
                } else {
                    assertThat(args.length, is(1));
                    assertThat(ParameterMode.PARAM_IN, sameInstance(args[0].mode));
                    if (resultAvail) {
                        if (isstr) {
                            String str = args[0].argument.extract_string();
                            assertThat(str.startsWith("TEST"), is(true));
                        } else {
                            s sp = sHelper.extract(args[0].argument);
                            assertThat(sp.sval.startsWith("TEST"), is(true));
                        }
                    }
                }
            } else if (op.startsWith("one_")) {
                String which = op.substring(4); // Which operation?
                boolean isstr; // struct or string?
                ParameterMode mode; // The parameter mode

                // if which.startsWith("struct"))
                isstr = which.startsWith("string");

                which = which.substring(7); // Skip <string|struct>_

                if (which.equals("return")) {
                    assertThat(args.length, is(0));
                    if (resultAvail) {
                        // Test: result
                        Any result = ri.result();
                        if (isstr) {
                            String str = result.extract_string();
                            assertThat(str.startsWith("TEST"), is(true));
                        } else {
                            s sp = sHelper.extract(result);
                            assertThat(sp.sval.startsWith("TEST"), is(true));
                        }
                    }
                } else {
                    assertThat(args.length, is(1));
                    if (which.equals("in")) mode = ParameterMode.PARAM_IN;
                    else if (which.equals("inout")) mode = ParameterMode.PARAM_INOUT;
                    else
                        // if(which.equals("out"))
                        mode = ParameterMode.PARAM_OUT;

                    assertThat(mode, sameInstance(args[0].mode));

                    if (mode != ParameterMode.PARAM_OUT || resultAvail) {
                        if (isstr) {
                            String str = args[0].argument.extract_string();
                            assertThat(str.startsWith("TEST"), is(true));
                        } else {
                            s sp = sHelper.extract(args[0].argument);
                            assertThat(sp.sval.startsWith("TEST"), is(true));
                        }

                        if (resultAvail) {
                            // Test: result
                            Any result = ri.result();
                            TypeCode tc = result.type();
                            assertThat(tc.kind(), sameInstance(TCKind.tk_void));
                        }
                    }
                }
            } else {
                assertThat(args.length, is(0));
            }

            if (!resultAvail) {
                // Test: result is not available
                assertThrows(BAD_INV_ORDER.class, ri::result);
            }
        }

        private void testServiceContext(String op, ServerRequestInfo ri, boolean addContext) {
            if (op.equals("test_service_context")) {
                { // Test: get_request_service_context
                    ServiceContext sc = assertDoesNotThrow(() -> ri.get_request_service_context(REQUEST_CONTEXT_ID.value));
                    assertThat(sc.context_id, is(REQUEST_CONTEXT_ID.value));
                }
                // Test: get_reply_service_context
                {
                    ServiceContext sc = assertDoesNotThrow(() -> ri.get_reply_service_context(REPLY_CONTEXT_4_ID.value));
                    Any any = assertDoesNotThrow(() -> cdrCodec.decode_value(copyOf(sc.context_data, sc.context_data.length), ReplyContextHelper.type()));
                    ReplyContext context = ReplyContextHelper.extract(any);
                    assertThat(context.data, is("reply4"));
                    assertThat(context.val, is(114));
                }

                if (addContext) {
                    final Any any = ORB.init().create_any();
                    ReplyContextHelper.insert(any, new ReplyContext("reply3", REPLY_CONTEXT_3_ID.value));
                    {
                        byte[] data = assertDoesNotThrow(() -> cdrCodec.encode_value(any));
                        ServiceContext sc = new ServiceContext(REPLY_CONTEXT_3_ID.value, copyOf(data, data.length));
                        assertDoesNotThrow(() -> ri.add_reply_service_context(sc, false));
                        assertThrows(BAD_INV_ORDER.class, () -> ri.add_reply_service_context(sc, false));
                        assertDoesNotThrow(() -> ri.add_reply_service_context(sc, true));
                    }
                    ReplyContextHelper.insert(any, new ReplyContext("reply4", 124));
                    byte[] data = assertDoesNotThrow(() -> cdrCodec.encode_value(any));
                    assertDoesNotThrow(() -> ri.add_reply_service_context(new ServiceContext(REPLY_CONTEXT_4_ID.value, copyOf(data, data.length)), true));
                }
            } else {
                assertThrows(BAD_PARAM.class, () -> ri.get_request_service_context(REQUEST_CONTEXT_ID.value));
                assertThrows(BAD_PARAM.class, () -> ri.get_reply_service_context(REPLY_CONTEXT_1_ID.value));
            }
        }

        @Override
        public void receive_request_service_contexts(ServerRequestInfo ri) {
            assertNotEquals(ri.operation().equals("noargs_oneway"), ri.response_expected(), "Either the message should be one-way or a response should be expected");
            assertThrows(BAD_INV_ORDER.class, ri::arguments);
            assertThrows(BAD_INV_ORDER.class, ri::result);
            assertThrows(BAD_INV_ORDER.class, ri::exceptions);
            assertThrows(BAD_INV_ORDER.class, ri::reply_status);
            assertThrows(BAD_INV_ORDER.class, ri::forward_reference);
            assertThrows(BAD_INV_ORDER.class, ri::object_id);
            assertThrows(BAD_INV_ORDER.class, ri::adapter_id);
            assertThrows(BAD_INV_ORDER.class, ri::target_most_derived_interface);
            assertThrows(BAD_INV_ORDER.class, ri::server_id);
            assertThrows(BAD_INV_ORDER.class, ri::orb_id);
            assertThrows(BAD_INV_ORDER.class, ri::adapter_name);
            assertThrows(BAD_INV_ORDER.class, () -> ri.target_is_a(""));

            if (ri.operation().equals("test_service_context")) {
                final ServiceContext sc = ri.get_request_service_context(REQUEST_CONTEXT_ID.value);
                assertThat(sc.context_id, is(REQUEST_CONTEXT_ID.value));
                final Any any = assertDoesNotThrow(() -> cdrCodec.decode_value(copyOf(sc.context_data, sc.context_data.length), RequestContextHelper.type()));
                final RequestContext requestContext = RequestContextHelper.extract(any);
                assertThat(requestContext.data, is("request"));
                assertThat(requestContext.val, is(10));
                // Test: PortableInterceptor::Current
                Any slotData = ORB.init().create_any();
                slotData.insert_long(requestContext.val);
                assertDoesNotThrow(() -> ri.set_slot(0, slotData));

                // Test: add_reply_service_context
                final ReplyContext replyContext = new ReplyContext("reply1", 101);
                ReplyContextHelper.insert(any, replyContext);
                byte[] data = assertDoesNotThrow(() -> cdrCodec.encode_value(any));

                sc.context_id = REPLY_CONTEXT_1_ID.value;
                sc.context_data = copyOf(data, data.length);
                assertDoesNotThrow(() -> ri.add_reply_service_context(sc, false));

                // Test: add same context again (no replace)
                assertThrows(BAD_INV_ORDER.class, () -> ri.add_reply_service_context(sc, false));

                // Test: add same context again (replace)
                assertDoesNotThrow(() -> ri.add_reply_service_context(sc, true));

                // Test: add second context
                replyContext.data = "reply4";
                replyContext.val = 104;
                ReplyContextHelper.insert(any, replyContext);
                data = assertDoesNotThrow(() -> cdrCodec.encode_value(any));

                sc.context_id = REPLY_CONTEXT_4_ID.value;
                sc.context_data = copyOf(data, data.length);

                assertDoesNotThrow(() -> ri.add_reply_service_context(sc, false));
            } else {
                // Test: get_request_service_context
                assertThrows(BAD_PARAM.class, () -> ri.get_request_service_context(REQUEST_CONTEXT_ID.value));
            }

            // Test: get_reply_service_context
            assertThrows(BAD_INV_ORDER.class, () -> ri.get_reply_service_context(REPLY_CONTEXT_1_ID.value));

            // Test: sending exception is not available
            assertThrows(BAD_INV_ORDER.class, ri::sending_exception);

            // Test: get_server_policy
            Policy policy = ri.get_server_policy(MY_SERVER_POLICY_ID.value);
            MyServerPolicy myServerPolicy = MyServerPolicyHelper.narrow(policy);
            assertThat(myServerPolicy, not(sameInstance(null)));
            assertThat(myServerPolicy.value(), is(10));

            assertThrows(INV_POLICY.class, () -> ri.get_server_policy(1013));
        }

        @Override
        public void receive_request(ServerRequestInfo ri) {
            String op = ri.operation();

            boolean oneway = op.equals("noargs_oneway");

            testArgs(ri, false);

            assertThrows(BAD_INV_ORDER.class, ri::result);

            try {
                TypeCode[] exceptions = ri.exceptions();
                if (op.equals("userexception")) {
                    assertThat(exceptions.length, is(1));
                    assertThat(exceptions[0].equal(userHelper.type()), is(true));
                } else {
                    assertThat(exceptions.length, is(0));
                }
            } catch (NO_RESOURCES ex) {
                // Expected (if servant is DSI)
            }

            // Test: response expected and oneway should be equivalent
            boolean expr = (oneway && !ri.response_expected()) || (!oneway && ri.response_expected());
            assertThat(expr, is(true));

            // TODO: test sync scope

            // Test: reply status is not available
            assertThrows(BAD_INV_ORDER.class, ri::reply_status);

            // Test: forward reference is not available
            assertThrows(BAD_INV_ORDER.class, ri::forward_reference);

            if (op.equals("test_service_context")) {
                // Test: get_request_service_context
                final ServiceContext sc = assertDoesNotThrow(() -> ri.get_request_service_context(REQUEST_CONTEXT_ID.value));
                assertThat(sc.context_id, is(REQUEST_CONTEXT_ID.value));

                // Test: add_reply_service_context
                final ReplyContext context = new ReplyContext("reply2", 102);
                final Any any = ORB.init().create_any();
                ReplyContextHelper.insert(any, context);
                byte[] data = assertDoesNotThrow(() -> cdrCodec.encode_value(any));

                sc.context_id = REPLY_CONTEXT_2_ID.value;
                sc.context_data = copyOf(data, data.length);
                assertDoesNotThrow(() -> ri.add_reply_service_context(sc, false));

                // Test: add same context again (no replace)
                assertThrows(BAD_INV_ORDER.class, () -> ri.add_reply_service_context(sc, false));

                // Test: add same context again (replace)
                assertDoesNotThrow(() -> ri.add_reply_service_context(sc, true));

                // Test: replace context added in
                // receive_request_service_context
                context.data = "reply4";
                context.val = 114;
                ReplyContextHelper.insert(any, context);
                data = assertDoesNotThrow(() -> cdrCodec.encode_value(any));

                sc.context_id = REPLY_CONTEXT_4_ID.value;
                sc.context_data = copyOf(data, data.length);

                assertDoesNotThrow(() -> ri.add_reply_service_context(sc, true));
            } else {
                // Test: get_request_service_context
                assertThrows(BAD_PARAM.class, () -> ri.get_request_service_context(REQUEST_CONTEXT_ID.value));
            }

            // Test: get_reply_service_context
            assertThrows(BAD_INV_ORDER.class, () -> ri.get_reply_service_context(REPLY_CONTEXT_1_ID.value));

            // Test: sending exception is not available
            assertThrows(BAD_INV_ORDER.class, ri::sending_exception);

            // Test: object id is correct
            byte[] oid = ri.object_id();
            assertThat((oid.length == 4 && (new String(oid)).equals("test")) || (oid.length == 7 && (new String(oid)).equals("testDSI")), is(true));

            // Test: adapter id is correct (this is a tough one to test)
            byte[] adapterId = ri.adapter_id();
            assertThat(adapterId.length, not(is(0)));

            // Test: servant most derived interface is correct
            String mdi = ri.target_most_derived_interface();
            assertThat(mdi, is("IDL:TestInterface:1.0"));

            // Test: server id is correct
            String serverId = ri.server_id();
            assertThat(serverId, is(""));

            // Test: orb id is correct
            String orbId = ri.orb_id();
            assertThat(orbId, is("server orb"));

            // Test: adapter name is correct
            String[] adapterName = ri.adapter_name();
            assertThat(adapterName.length == 1 && adapterName[0].equals("persistent"), is(true));

            // Test: servant is a is correct
            assertThat(ri.target_is_a("IDL:TestInterface:1.0"), is(true));

            // Test: get_server_policy
            Policy policy = ri.get_server_policy(MY_SERVER_POLICY_ID.value);
            MyServerPolicy myServerPolicy = MyServerPolicyHelper.narrow(policy);
            assertThat(myServerPolicy, not(sameInstance(null)));
            assertThat(myServerPolicy.value(), is(10));

            assertThrows(INV_POLICY.class, () -> ri.get_server_policy(1013));
        }

        @Override
        public void send_reply(ServerRequestInfo ri) {
            // Test: get operation name
            String op = ri.operation();

            // If "deactivate" then we're done
            if (op.equals("deactivate")) return;

            boolean oneway = op.equals("noargs_oneway");

            // Test: Arguments should be available
            testArgs(ri, true);

            // TODO: test operation_context

            // Test: exceptions
            try {
                TypeCode[] exceptions = ri.exceptions();
                if (op.equals("userexception")) {
                    assertThat(exceptions.length, is(1));
                    assertThat(exceptions[0].equal(userHelper.type()), is(true));
                } else {
                    assertThat(exceptions.length, is(0));
                }
            } catch (NO_RESOURCES ex) {
                // Expected (if servant is DSI)
            }

            // Test: response expected and oneway should be equivalent
            boolean expr = (oneway && !ri.response_expected()) || (!oneway && ri.response_expected());
            assertThat(expr, is(true));

            // TODO: test sync scope

            // Test: reply status is available
            assertThat(ri.reply_status(), is(SUCCESSFUL.value));

            // Test: forward reference is not available
            assertThrows(BAD_INV_ORDER.class, ri::forward_reference);

            // Test: get_request_service_context
            // Test: get_reply_service_context
            // Test: add_reply_service_context
            testServiceContext(op, ri, true);

            // Test: sending exception is not available
            assertThrows(BAD_INV_ORDER.class, ri::sending_exception);

            // Test: object id is correct
            byte[] oid = ri.object_id();
            assertThat((oid.length == 4 && (new String(oid)).equals("test")) || (oid.length == 7 && (new String(oid)).equals("testDSI")), is(true));

            // Test: adapter id is correct (this is a tough one to test)
            byte[] adapterId = ri.adapter_id();
            assertThat(adapterId.length, not(is(0)));

            // Test: target_most_derived_interface raises BAD_INV_ORDER
            assertThrows(BAD_INV_ORDER.class, ri::target_most_derived_interface);

            // Test: server id is correct
            String serverId = ri.server_id();
            assertThat(serverId, is(""));

            // Test: orb id is correct
            String orbId = ri.orb_id();
            assertThat(orbId, is("server orb"));

            // Test: adapter name is correct
            String[] adapterName = ri.adapter_name();
            assertThat(adapterName.length == 1 && adapterName[0].equals("persistent"), is(true));

            // Test: target_is_a raises BAD_INV_ORDER
            assertThrows(BAD_INV_ORDER.class, () -> ri.target_is_a("IDL:TestInterface:1.0"));

            // Test: get_server_policy
            Policy policy = ri.get_server_policy(MY_SERVER_POLICY_ID.value);
            MyServerPolicy myServerPolicy = MyServerPolicyHelper.narrow(policy);
            assertThat(myServerPolicy, not(sameInstance(null)));
            assertThat(myServerPolicy.value(), is(10));

            assertThrows(INV_POLICY.class, () -> ri.get_server_policy(1013));

            // Test: get_slot
            if (op.equals("test_service_context")) {
                int val;
                Any slotData = assertDoesNotThrow(() -> ri.get_slot(0));
                val = slotData.extract_long();
                assertThat(val, is(20));
            }
        }

        @Override
        public void send_other(ServerRequestInfo ri) {
            // Test: get operation name
            String op = ri.operation();

            assertThat(op, is("location_forward"));

            // Test: Arguments should not be available
            assertThrows(BAD_INV_ORDER.class, ri::arguments);

            // Test: exceptions
            try {
                TypeCode[] exceptions = ri.exceptions();
                assertThat(exceptions.length, is(0));
            } catch (BAD_INV_ORDER ex) {
                // Expected, depending on what raised the exception
            } catch (NO_RESOURCES ex) {
                // Expected (if servant is DSI)
            }

            // TODO: test operation_context

            // Test: response expected should be true
            assertThat(ri.response_expected(), is(true));

            // TODO: test sync scope

            // Test: reply status is available
            assertThat(ri.reply_status(), is(LOCATION_FORWARD.value));

            // Test: forward reference is available
            assertDoesNotThrow(ri::forward_reference);

            // Test: get_request_service_context
            // Test: get_reply_service_context
            testServiceContext(op, ri, false);

            // Test: sending exception is not available
            assertThrows(BAD_INV_ORDER.class, ri::sending_exception);

            // Test: object id is correct
            byte[] oid = ri.object_id();
            assertThat((oid.length == 4 && (new String(oid)).equals("test")) || (oid.length == 7 && (new String(oid)).equals("testDSI")), is(true));

            // Test: adapter id is correct (this is a tough one to test)
            byte[] adapterId = ri.adapter_id();
            assertThat(adapterId.length, not(is(0)));

            // Test: target_most_derived_interface raises BAD_INV_ORDER
            assertThrows(BAD_INV_ORDER.class, ri::target_most_derived_interface);

            // Test: server id is correct
            String serverId = ri.server_id();
            assertThat(serverId, is(""));

            // Test: orb id is correct
            String orbId = ri.orb_id();
            assertThat(orbId, is("myORB"));

            // Test: adapter name is correct
            String[] adapterName = ri.adapter_name();
            assertThat(adapterName.length == 1 && adapterName[0].equals("persistent"), is(true));

            // Test: target_is_a raises BAD_INV_ORDER
            assertThrows(BAD_INV_ORDER.class, () -> ri.target_is_a("IDL:TestInterface:1.0"));

            // Test: get_server_policy
            Policy policy = ri.get_server_policy(MY_SERVER_POLICY_ID.value);
            MyServerPolicy myServerPolicy = MyServerPolicyHelper.narrow(policy);
            assertThat(myServerPolicy, not(sameInstance(null)));
            assertThat(myServerPolicy.value(), is(10));

            assertThrows(INV_POLICY.class, () -> ri.get_server_policy(1013));
        }

        @Override
        public void send_exception(ServerRequestInfo ri) {
            // Test: get operation name
            String op = ri.operation();

            assertThat(op.equals("systemexception") || op.equals("userexception") || op.equals("deactivate"), is(true));

            boolean user = op.equals("userexception");

            // If "deactivate" then we're done
            if (op.equals("deactivate")) return;

            // Test: Arguments should not be available
            assertThrows(BAD_INV_ORDER.class, ri::arguments);

            // TODO: test operation_context

            // Test: result is not available
            assertThrows(BAD_INV_ORDER.class, ri::result);

            // Test: exceptions
            assertDoesNotThrow(() -> {
                try {
                    TypeCode[] exceptions = ri.exceptions();
                    if (op.equals("userexception")) {
                        assertThat(exceptions.length, is(1));
                        assertThat(exceptions[0].equal(userHelper.type()), is(true));
                    } else {
                        assertThat(exceptions.length, is(0));
                    }
                } catch (NO_RESOURCES ex) {
                    // Expected (if servant is DSI)
                }
            });

            // Test: response expected should be true
            assertThat(ri.response_expected(), is(true));

            // TODO: test sync scope

            // Test: reply status is available
            if (user) assertThat(ri.reply_status(), is(USER_EXCEPTION.value));
            else assertThat(ri.reply_status(), is(SYSTEM_EXCEPTION.value));

            // Test: forward reference is not available
            assertThrows(BAD_INV_ORDER.class, ri::forward_reference);

            // Test: get_request_service_context
            // Test: get_reply_service_context
            testServiceContext(op, ri, false);

            // Test: sending exception is available
            try {
                Any any = ri.sending_exception();
                if (user) {
                    //noinspection ThrowableNotThrown
                    userHelper.extract(any);
                } else {
                    //noinspection ThrowableNotThrown
                    unmarshalSystemException(any.create_input_stream());
                }
            } catch (BAD_INV_ORDER ex) {
                assertThat(false, is(true));
            } catch (NO_RESOURCES ignored) {}// TODO: remove this!

            // Test: object id is correct
            byte[] oid = ri.object_id();
            assertThat((oid.length == 4 && (new String(oid)).equals("test")) || (oid.length == 7 && (new String(oid)).equals("testDSI")), is(true));

            // Test: adapter id is correct (this is a tough one to test)
            byte[] adapterId = ri.adapter_id();
            assertThat(adapterId.length, not(is(0)));

            // Test: target_most_derived_interface raises BAD_INV_ORDER
            assertThrows(BAD_INV_ORDER.class, ri::target_most_derived_interface);

            // Test: server id is correct
            String serverId = ri.server_id();
            assertThat(serverId, is(""));

            // Test: orb id is correct
            String orbId = ri.orb_id();
            assertThat(orbId, is("myORB"));

            // Test: adapter name is correct
            String[] adapterName = ri.adapter_name();
            assertEquals(1, adapterName.length);
            assertThat(adapterName[0], is("persistent"));

            // Test: target_is_a raises BAD_INV_ORDER
            assertThrows(BAD_INV_ORDER.class, () -> ri.target_is_a("IDL:TestInterface:1.0"));

            // Test: get_server_policy
            Policy policy = ri.get_server_policy(MY_SERVER_POLICY_ID.value);
            MyServerPolicy myServerPolicy = MyServerPolicyHelper.narrow(policy);
            assertThat(myServerPolicy, not(sameInstance(null)));
            assertThat(myServerPolicy.value(), is(10));

            assertThrows(INV_POLICY.class, () -> ri.get_server_policy(1013));
        }
    }
}
