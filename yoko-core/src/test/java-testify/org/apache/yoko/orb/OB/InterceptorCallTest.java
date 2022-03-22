package org.apache.yoko.orb.OB;

import acme.idl.MY_CLIENT_POLICY_ID;
import acme.idl.MY_COMPONENT_ID;
import acme.idl.MY_SERVER_POLICY_ID;
import acme.idl.MyClientPolicy;
import acme.idl.MyClientPolicyHelper;
import acme.idl.MyComponent;
import acme.idl.MyComponentHelper;
import acme.idl.MyServerPolicy;
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
import acme.idl.TestInterfacePackage.s;
import acme.idl.TestInterfacePackage.sHelper;
import acme.idl.TestInterfacePackage.sHolder;
import acme.idl.TestInterfacePackage.user;
import acme.idl.TestInterfacePackage.userHelper;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_INV_ORDER;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.INV_POLICY;
import org.omg.CORBA.NO_IMPLEMENT;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ParameterMode;
import org.omg.CORBA.Policy;
import org.omg.CORBA.StringHolder;
import org.omg.CORBA.SystemException;
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
import org.omg.IOP.TaggedComponent;
import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.IORInfo;
import org.omg.PortableInterceptor.LOCATION_FORWARD;
import org.omg.PortableInterceptor.ORBInitInfo;
import org.omg.PortableInterceptor.SUCCESSFUL;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;
import org.omg.PortableInterceptor.USER_EXCEPTION;
import testify.iiop.TestClientRequestInterceptor;
import testify.iiop.TestIORInterceptor;
import testify.jupiter.annotation.iiop.ConfigureOrb.UseWithOrb;
import testify.jupiter.annotation.logging.Logging;

import java.util.Arrays;
import java.util.Objects;

import static java.util.Arrays.copyOf;
import static org.apache.yoko.orb.OB.PortableInterceptorTest.StubType.DSI_INTERFACE;
import static org.apache.yoko.orb.OB.PortableInterceptorTest.StubType.TEST_INTERFACE;
import static org.apache.yoko.orb.OB.Util.unmarshalSystemException;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
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

@Logging
class InterceptorCallTest extends PortableInterceptorTest {
    private static Runnable CHECK_REQUEST_COUNT = () -> { throw new BAD_INV_ORDER(); };

    @UseWithOrb("client orb")
    public static class MyClientInterceptor implements TestClientRequestInterceptor {
        @Override
        public void post_init(ORBInitInfo info) {
            assertDoesNotThrow(() -> info.add_client_request_interceptor(new MyClientInterceptor(info)));
        }

        private final Codec codec;
        private int actualRequestCount;
        private int expectedRequestCount;

        @SuppressWarnings("unused")
        public MyClientInterceptor() { codec = null; }
        private MyClientInterceptor(ORBInitInfo info) {
            final CodecFactory factory = CodecFactoryHelper.narrow(info.codec_factory());
            assertNotNull(factory);
            codec = assertDoesNotThrow(() -> factory.create_codec(new Encoding(ENCODING_CDR_ENCAPS.value, (byte) 0, (byte) 0)));
            assertNotNull(codec);
            CHECK_REQUEST_COUNT = this::expectOneMoreRequest;
        }

        public void expectOneMoreRequest() {
            expectedRequestCount++;
            assertEquals(expectedRequestCount, actualRequestCount);
        }

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

        private void checkExceptions(String op, TypeCode[] exceptions) {
            if (op.equals("userexception")) {
                assertEquals(1, exceptions.length);
                assertTrue(exceptions[0].equal(userHelper.type()));
            } else {
                assertEquals(0, exceptions.length);
            }
        }

        public void send_request(ClientRequestInfo ri) {
            actualRequestCount++;
            ri.request_id();
            final String op = ri.operation();
            testArgs(ri, false);
            checkExceptions(op, ri.exceptions());
            assertNotEquals(op.equals("noargs_oneway"), ri.response_expected(), "Either the message should be one-way or a response should be expected");
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
            assertThrows(BAD_INV_ORDER.class, () -> ri.get_reply_service_context(REQUEST_CONTEXT_ID.value));
            if (op.equals("test_service_context")) {
                final Any any = ORB.init().create_any();
                RequestContextHelper.insert(any, new RequestContext("request", 10));
                final byte[] data = assertDoesNotThrow(() -> codec.encode_value(any));
                ri.add_request_service_context(new ServiceContext(REQUEST_CONTEXT_ID.value, copyOf(data, data.length)), false);
                assertDoesNotThrow(() -> ri.get_request_service_context(REQUEST_CONTEXT_ID.value));
            } else {
                assertThrows(BAD_PARAM.class, () -> ri.get_request_service_context(REQUEST_CONTEXT_ID.value));
            }
        }

        public void send_poll(ClientRequestInfo ri) {
            fail();
        }

        private void checkServiceContext(ClientRequestInfo ri, int contextId, String expectedData, int expectedValue) {
            final ServiceContext sc = assertDoesNotThrow(() -> ri.get_reply_service_context(contextId));
            assertEquals(sc.context_id, contextId);
            final byte[] data = copyOf(sc.context_data, sc.context_data.length);
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
        }

        public void receive_other(ClientRequestInfo ri) {
            ri.request_id();
            final String op = ri.operation();
            assertEquals("location_forward", op);
            assertThrows(BAD_INV_ORDER.class, ri::arguments);
            assertEquals(0, ri.exceptions().length);
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
            throw new NO_IMPLEMENT(); // Eat the location forward
        }

        public void receive_exception(ClientRequestInfo ri) {
            ri.request_id();
            final String op = ri.operation();
            if (op.equals("deactivate")) return;
            assertThat(Objects.toString(ri.received_exception()), op, anyOf(is("systemexception"), is("userexception")));
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
        }
    }

    @UseWithOrb("server orb")
    public static final class MyIORInterceptor implements TestIORInterceptor {
        private final Codec cdrCodec;

        @Override
        public void post_init(ORBInitInfo info) {
            assertDoesNotThrow(() -> info.add_ior_interceptor(new MyIORInterceptor(info)));
        }

        @SuppressWarnings("unused")
        public MyIORInterceptor() { cdrCodec = null; }

        @SuppressWarnings("unused")
        private MyIORInterceptor(ORBInitInfo info) {
            final CodecFactory factory = info.codec_factory();
            final Encoding how = new Encoding((byte) ENCODING_CDR_ENCAPS.value, (byte) 0, (byte) 0);

            cdrCodec = assertDoesNotThrow(() -> factory.create_codec(how));
            assertNotNull(cdrCodec);
        }

        public void establish_components(IORInfo info) {
            try {
                final Policy p = assertDoesNotThrow(() -> info.get_effective_policy(MY_SERVER_POLICY_ID.value));
                if (p == null) return;
                final MyServerPolicy policy = MyServerPolicyHelper.narrow(p);

                final Any any = ORB.init().create_any();
                MyComponentHelper.insert(any, new MyComponent(policy.value()));

                final byte[] encoding = assertDoesNotThrow(() -> cdrCodec.encode_value(any));

                final TaggedComponent component = new TaggedComponent(MY_COMPONENT_ID.value, copyOf(encoding, encoding.length));
                info.add_ior_component(component);
            } catch (INV_POLICY ignored) {}
        }
    }

    @Test
    void testCalls() {
        testCalls(STUB_MAP.get(TEST_INTERFACE));
        testCalls(STUB_MAP.get(DSI_INTERFACE));
    }

    void testCalls(TestInterface ti) {
        ti.noargs();
        CHECK_REQUEST_COUNT.run();

        ti.noargs_oneway();
        CHECK_REQUEST_COUNT.run();

        assertThrows(user.class, ti::userexception);
        CHECK_REQUEST_COUNT.run();

        assertThrows(SystemException.class, ti::systemexception);
        CHECK_REQUEST_COUNT.run();

        ti.test_service_context();
        CHECK_REQUEST_COUNT.run();

        assertThrows(NO_IMPLEMENT.class, ti::location_forward);
        CHECK_REQUEST_COUNT.run();

        // Test simple attribute
        ti.string_attrib("TEST");
        CHECK_REQUEST_COUNT.run();
        assertEquals("TEST", ti.string_attrib());
        CHECK_REQUEST_COUNT.run();

        // Test in, inout and out simple parameters
        ti.one_string_in("TEST");
        CHECK_REQUEST_COUNT.run();

        ti.one_string_inout(new StringHolder("TESTINOUT"));
        assertEquals("TEST", new StringHolder("TESTINOUT").value);
        CHECK_REQUEST_COUNT.run();

        ti.one_string_out(new StringHolder());
        assertEquals("TEST", new StringHolder().value);
        CHECK_REQUEST_COUNT.run();

        assertEquals("TEST", ti.one_string_return());
        CHECK_REQUEST_COUNT.run();

        // Test struct attribute
        ti.struct_attrib(new s("TEST"));
        CHECK_REQUEST_COUNT.run();
        assertEquals("TEST", ti.struct_attrib().sval);
        CHECK_REQUEST_COUNT.run();

        // Test in, inout and out struct parameters
        ti.one_struct_in(new s("TEST"));
        CHECK_REQUEST_COUNT.run();

        final sHolder testinout = new sHolder(new s("TESTINOUT"));
        ti.one_struct_inout(testinout);
        assertEquals("TEST", testinout.value.sval);
        CHECK_REQUEST_COUNT.run();

        final sHolder param = new sHolder();
        ti.one_struct_out(param);
        assertEquals("TEST", param.value.sval);
        CHECK_REQUEST_COUNT.run();

        assertEquals("TEST", ti.one_struct_return().sval);
        CHECK_REQUEST_COUNT.run();
    }
}
