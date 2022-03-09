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
import acme.idl.MyServerPolicyFactory;
import acme.idl.TestInterface;
import acme.idl.TestInterfaceDSI_impl;
import acme.idl.TestInterfaceHelper;
import acme.idl.TestInterfacePackage.s;
import acme.idl.TestInterfacePackage.sHolder;
import acme.idl.TestInterfacePackage.user;
import acme.idl.TestInterface_impl;
import acme.idl.TestLocator_impl;
import acme.interceptors.CallInterceptorImpl;
import acme.interceptors.MyServerRequestInterceptor;
import org.apache.yoko.orb.OBPortableServer.POAHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_INV_ORDER;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.NO_IMPLEMENT;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;
import org.omg.CORBA.PolicyError;
import org.omg.CORBA.StringHolder;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.portable.InputStream;
import org.omg.IOP.CodecFactory;
import org.omg.IOP.CodecFactoryHelper;
import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.ClientRequestInterceptor;
import org.omg.PortableInterceptor.Current;
import org.omg.PortableInterceptor.CurrentHelper;
import org.omg.PortableInterceptor.ORBInitInfo;
import org.omg.PortableInterceptor.ORBInitInfoPackage.DuplicateName;
import org.omg.PortableInterceptor.ORBInitializer;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAManager;
import org.omg.PortableServer.ServantLocator;
import testify.bus.Bus;
import testify.bus.StringSpec;
import testify.jupiter.annotation.iiop.ConfigureOrb.UseWithOrb;
import testify.jupiter.annotation.iiop.ConfigureServer;
import testify.jupiter.annotation.iiop.ConfigureServer.BeforeServer;
import testify.streams.BiStream;

import java.util.EnumMap;

import static java.util.function.Function.identity;
import static org.apache.yoko.orb.OB.PortableInterceptorTest.InterceptorThrowingTest.MyClientRequestInterceptor.FIRST;
import static org.apache.yoko.orb.OB.PortableInterceptorTest.InterceptorThrowingTest.MyClientRequestInterceptor.SECOND;
import static org.apache.yoko.orb.OB.PortableInterceptorTest.InterceptorThrowingTest.MyClientRequestInterceptor.THIRD;
import static org.apache.yoko.orb.OB.PortableInterceptorTest.StubType.DSI_INTERFACE;
import static org.apache.yoko.orb.OB.PortableInterceptorTest.StubType.TEST_INTERFACE;
import static org.apache.yoko.orb.OB.Util.unmarshalSystemException;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.omg.CORBA.SetOverrideType.ADD_OVERRIDE;
import static org.omg.PortableServer.IdAssignmentPolicyValue.USER_ID;
import static org.omg.PortableServer.ImplicitActivationPolicyValue.NO_IMPLICIT_ACTIVATION;
import static org.omg.PortableServer.LifespanPolicyValue.PERSISTENT;
import static org.omg.PortableServer.RequestProcessingPolicyValue.USE_SERVANT_MANAGER;
import static org.omg.PortableServer.ServantRetentionPolicyValue.NON_RETAIN;

@ConfigureServer
public class PortableInterceptorTest {
    @UseWithOrb("server orb")
    public static final class MyServerInitializer extends LocalObject implements ORBInitializer {
        @Override
        public void pre_init(ORBInitInfo info) {
            assertDoesNotThrow(() -> info.register_policy_factory(MY_SERVER_POLICY_ID.value, new MyServerPolicyFactory()));
        }

        @Override
        public void post_init(ORBInitInfo info) {
            final MyServerRequestInterceptor requestInterceptor = new MyServerRequestInterceptor(info.codec_factory());
            assertDoesNotThrow(() -> info.add_server_request_interceptor(requestInterceptor));
        }
    }
    @BeforeServer
    public static void beforeServer(ORB orb, Bus bus) throws Exception {
        //Can't currently configure the POA that the framework would supply, so we'll create our own
        POA rootPoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
        POAManager poaManager = rootPoa.the_POAManager();
        Any any = orb.create_any();
        any.insert_long(10);
        Policy[] policies = {
                rootPoa.create_lifespan_policy(PERSISTENT),
                rootPoa.create_id_assignment_policy(USER_ID),
                rootPoa.create_request_processing_policy(USE_SERVANT_MANAGER),
                rootPoa.create_servant_retention_policy(NON_RETAIN),
                rootPoa.create_implicit_activation_policy(NO_IMPLICIT_ACTIVATION),
                orb.create_policy(MY_SERVER_POLICY_ID.value, any)
        };

        POA persistentPoa = rootPoa.create_POA("persistent", poaManager, policies);

        final TestInterface_impl impl = new TestInterface_impl(orb, persistentPoa);
        final org.omg.CORBA.Object objImpl = persistentPoa.create_reference_with_id("test".getBytes(), TestInterfaceHelper.id());
        final TestInterfaceDSI_impl dsiImpl = new TestInterfaceDSI_impl(orb);
        final org.omg.CORBA.Object objDsiImpl = persistentPoa.create_reference_with_id("testDSI".getBytes(), TestInterfaceHelper.id());

        TestLocator_impl locatorImpl = new TestLocator_impl(impl, dsiImpl);
        ServantLocator locator = locatorImpl._this(orb);
        persistentPoa.set_servant_manager(locator);

        final CodecFactory codecFactory = CodecFactoryHelper.narrow(orb.resolve_initial_references("CodecFactory"));
        assertThat(codecFactory, notNullValue());
        bus.put(TEST_INTERFACE, orb.object_to_string(objImpl));
        bus.put(DSI_INTERFACE, orb.object_to_string(objDsiImpl));
    }

    enum StubType implements StringSpec {TEST_INTERFACE, DSI_INTERFACE}
    private static final EnumMap<StubType, TestInterface> STUB_MAP = new EnumMap<>(StubType.class);

    @BeforeAll
    public static void beforeAll(ORB orb, Bus bus) throws PolicyError {
        Any any = orb.create_any();
        any.insert_long(10);
        Policy[] pl = {orb.create_policy(MY_CLIENT_POLICY_ID.value, any)};
        // populate the enum map with stubs narrowed from the stringified IORs put in the bus by the server
        BiStream.of(StubType.values(), identity())
                .mapValues(bus::get)
                .mapValues(orb::string_to_object)
                .mapValues(o -> o._set_policy_override(pl, ADD_OVERRIDE))
                .mapValues(TestInterfaceHelper::narrow)
                .forEach(STUB_MAP::put);
    }
    
    static class InterceptorThrowingTest extends PortableInterceptorTest {
        @UseWithOrb("client orb")
        public static final class MyClientRequestInterceptor extends LocalObject implements ClientRequestInterceptor, ORBInitializer {
            static final MyClientRequestInterceptor
                    FIRST = new MyClientRequestInterceptor(),
                    SECOND = new MyClientRequestInterceptor(),
                    THIRD = new MyClientRequestInterceptor();

            @Override
            public void pre_init(ORBInitInfo info) {
                try {
                    info.add_client_request_interceptor(FIRST);
                    info.add_client_request_interceptor(SECOND);
                    info.add_client_request_interceptor(THIRD);
                    info.register_policy_factory(MY_CLIENT_POLICY_ID.value, new MyClientPolicyFactory());
                } catch (DuplicateName e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void post_init(ORBInitInfo info) {}

            private SystemException requestEx_;
            private SystemException replyEx_;
            private SystemException exceptionEx_;
            private SystemException expected_;

            private static int count = 0;
            private final int id = ++count;
            public String name() { return "MyCRI#" + id; }

            public void destroy() {}
            public void send_poll(ClientRequestInfo ri) {}
            public void receive_other(ClientRequestInfo ri) {}

            public void send_request(ClientRequestInfo ri) {
                if (requestEx_ != null) throw requestEx_;
            }

            public void receive_reply(ClientRequestInfo ri) {
                Assertions.assertNull(expected_);
                if (replyEx_ != null) throw replyEx_;
            }

            public void receive_exception(ClientRequestInfo ri) {
                if (expected_ != null) {
                    Any any = ri.received_exception();
                    InputStream in = any.create_input_stream();
                    SystemException ex = unmarshalSystemException(in);
                    assertEquals(expected_.getClass(), ex.getClass());
                }
                if (exceptionEx_ != null) throw exceptionEx_;
            }

            void throwOnRequest(SystemException ex)   { requestEx_ = ex; }
            void noThrowOnRequest()                   { requestEx_ = null; }
            void throwOnReply(SystemException ex)     { replyEx_ = ex; }
            void noThrowOnReply()                     { replyEx_ = null; }
            void throwOnException(SystemException ex) { exceptionEx_ = ex; }
            void expectException(SystemException ex)  { expected_ = ex; }
        }

        @Test
        void testTranslation() {
            TestInterface ti = STUB_MAP.get(TEST_INTERFACE);

            assertDoesNotThrow(ti::noargs);
            FIRST.throwOnRequest(new NO_PERMISSION());
            assertThrows(NO_PERMISSION.class, ti::noargs);

            FIRST.noThrowOnRequest();
            assertDoesNotThrow(ti::noargs);
            FIRST.throwOnReply(new NO_PERMISSION());
            assertThrows(NO_PERMISSION.class, ti::noargs);

            FIRST.noThrowOnReply();
            SECOND.throwOnReply(new NO_PERMISSION());
            FIRST.expectException(new NO_PERMISSION());
            assertThrows(NO_PERMISSION.class, ti::noargs);

            SECOND.noThrowOnReply();
            FIRST.expectException(new NO_PERMISSION());
            SECOND.expectException(new BAD_INV_ORDER());
            SECOND.throwOnException(new NO_PERMISSION());
            THIRD.throwOnRequest(new BAD_INV_ORDER());
            assertThrows(NO_PERMISSION.class, ti::noargs);

            THIRD.noThrowOnRequest();
            THIRD.throwOnReply(new BAD_INV_ORDER());
            assertThrows(NO_PERMISSION.class, ti::noargs);
        }
    }

    static class CallTest extends PortableInterceptorTest {
        private static CallInterceptorImpl clientRequestInterceptor;

        @UseWithOrb("client orb")
        public static class MyClientInitializer extends LocalObject implements ORBInitializer {
            public void pre_init(ORBInitInfo info) {
                assertDoesNotThrow(() -> info.register_policy_factory(MY_CLIENT_POLICY_ID.value, new MyClientPolicyFactory()));
            }

            public void post_init(ORBInitInfo info) {
                clientRequestInterceptor = assertDoesNotThrow(() -> new CallInterceptorImpl(info));
                assertDoesNotThrow(() -> info.add_client_request_interceptor(clientRequestInterceptor));
            }
        }

        @Test
        void testCalls(ORB orb) throws Exception {
            testCalls(orb, STUB_MAP.get(TEST_INTERFACE));
            testCalls(orb, STUB_MAP.get(DSI_INTERFACE));
        }
        void testCalls(ORB orb, TestInterface ti) throws Exception {
            final Current pic = CurrentHelper.narrow(orb.resolve_initial_references("PICurrent"));

            Any slotData = orb.create_any();
            slotData.insert_long(10);

            pic.set_slot(clientRequestInterceptor.getSlotId(), slotData);

            ti.noargs();
            clientRequestInterceptor.expectOneMoreRequest();

            ti.noargs_oneway();
            clientRequestInterceptor.expectOneMoreRequest();

            assertThrows(user.class, ti::userexception);
            clientRequestInterceptor.expectOneMoreRequest();

            assertThrows(SystemException.class, ti::systemexception);
            clientRequestInterceptor.expectOneMoreRequest();

            ti.test_service_context();
            clientRequestInterceptor.expectOneMoreRequest();

            assertThrows(NO_IMPLEMENT.class, ti::location_forward);
            clientRequestInterceptor.expectOneMoreRequest();

            // Test simple attribute
            ti.string_attrib("TEST");
            clientRequestInterceptor.expectOneMoreRequest();
            assertEquals("TEST", ti.string_attrib());
            clientRequestInterceptor.expectOneMoreRequest();

            // Test in, inout and out simple parameters
            ti.one_string_in("TEST");
            clientRequestInterceptor.expectOneMoreRequest();

            ti.one_string_inout(new StringHolder("TESTINOUT"));
            assertEquals("TEST", new StringHolder("TESTINOUT").value);
            clientRequestInterceptor.expectOneMoreRequest();

            ti.one_string_out(new StringHolder());
            assertEquals("TEST", new StringHolder().value);
            clientRequestInterceptor.expectOneMoreRequest();

            assertEquals("TEST", ti.one_string_return());
            clientRequestInterceptor.expectOneMoreRequest();

            // Test struct attribute
            ti.struct_attrib(new s("TEST"));
            clientRequestInterceptor.expectOneMoreRequest();
            assertEquals("TEST", ti.struct_attrib().sval);
            clientRequestInterceptor.expectOneMoreRequest();

            // Test in, inout and out struct parameters
            ti.one_struct_in(new s("TEST"));
            clientRequestInterceptor.expectOneMoreRequest();

            final sHolder testinout = new sHolder(new s("TESTINOUT"));
            ti.one_struct_inout(testinout);
            assertEquals("TEST", testinout.value.sval);
            clientRequestInterceptor.expectOneMoreRequest();

            final sHolder param = new sHolder();
            ti.one_struct_out(param);
            assertEquals("TEST", param.value.sval);
            clientRequestInterceptor.expectOneMoreRequest();

            assertEquals("TEST", ti.one_struct_return().sval);
            clientRequestInterceptor.expectOneMoreRequest();

            // Test: PortableInterceptor::Current still has the same value
            assertEquals(10, assertDoesNotThrow(() -> pic.get_slot(clientRequestInterceptor.getSlotId())).extract_long());
        }
    }
}
