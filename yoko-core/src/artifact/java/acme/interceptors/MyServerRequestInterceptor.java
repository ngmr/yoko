package acme.interceptors;

import acme.idl.MY_SERVER_POLICY_ID;
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
import acme.idl.TestInterfacePackage.s;
import acme.idl.TestInterfacePackage.sHelper;
import acme.idl.TestInterfacePackage.userHelper;
import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_INV_ORDER;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.INV_POLICY;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.NO_RESOURCES;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ParameterMode;
import org.omg.CORBA.Policy;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;
import org.omg.Dynamic.Parameter;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecFactory;
import org.omg.IOP.CodecPackage.FormatMismatch;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;
import org.omg.IOP.CodecPackage.TypeMismatch;
import org.omg.IOP.ENCODING_CDR_ENCAPS;
import org.omg.IOP.Encoding;
import org.omg.IOP.ServiceContext;
import org.omg.PortableInterceptor.InvalidSlot;
import org.omg.PortableInterceptor.LOCATION_FORWARD;
import org.omg.PortableInterceptor.SUCCESSFUL;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.omg.PortableInterceptor.ServerRequestInterceptor;
import org.omg.PortableInterceptor.USER_EXCEPTION;

import static java.util.Arrays.copyOf;
import static org.apache.yoko.orb.OB.Util.unmarshalSystemException;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class MyServerRequestInterceptor extends LocalObject implements ServerRequestInterceptor {
    private final Codec cdrCodec;

    public MyServerRequestInterceptor(CodecFactory codecFactory) {
        Encoding encoding = new Encoding(ENCODING_CDR_ENCAPS.value, (byte) 0, (byte) 0);
        cdrCodec = assertDoesNotThrow(() -> codecFactory.create_codec(encoding));
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

    // IDL to Java Mapping

    public String name() {
        return "ServerTestInterceptor";
    }

    public void destroy() {
    }

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
            try {
                ServiceContext sc = ri.get_request_service_context(REQUEST_CONTEXT_ID.value);
                assertThat(sc.context_id, is(REQUEST_CONTEXT_ID.value));
                byte[] data = new byte[sc.context_data.length];
                System.arraycopy(sc.context_data, 0, data, 0, sc.context_data.length);

                Any any = null;
                try {
                    any = cdrCodec.decode_value(data, RequestContextHelper.type());
                } catch (FormatMismatch | TypeMismatch ex) {
                    assertThat(false, is(true));
                }
                RequestContext context = RequestContextHelper.extract(any);
                assertThat(context.data, is("request"));
                assertThat(context.val, is(10));

                // Test: PortableInterceptor::Current
                Any slotData = ORB.init().create_any();
                slotData.insert_long(context.val);
                try {
                    ri.set_slot(0, slotData);
                } catch (InvalidSlot ex) {
                    assertThat(false, is(true));
                }
            } catch (BAD_PARAM ex) {
                assertThat(false, is(true));
            }

            // Test: add_reply_service_context
            ReplyContext context = new ReplyContext();
            context.data = "reply1";
            context.val = 101;
            Any any = ORB.init().create_any();
            ReplyContextHelper.insert(any, context);
            byte[] data = null;
            try {
                data = cdrCodec.encode_value(any);
            } catch (InvalidTypeForEncoding ex) {
                assertThat(false, is(true));
            }

            ServiceContext sc = new ServiceContext();
            sc.context_id = REPLY_CONTEXT_1_ID.value;
            sc.context_data = new byte[data.length];
            System.arraycopy(data, 0, sc.context_data, 0, data.length);

            try {
                ri.add_reply_service_context(sc, false);
            } catch (BAD_INV_ORDER ex) {
                assertThat(false, is(true));
            }

            // Test: add same context again (no replace)
            assertThrows(BAD_INV_ORDER.class, () -> ri.add_reply_service_context(sc, false));

            // Test: add same context again (replace)
            try {
                ri.add_reply_service_context(sc, true);
            } catch (BAD_INV_ORDER ex) {
                assertThat(false, is(true));
            }

            // Test: add second context
            context.data = "reply4";
            context.val = 104;
            ReplyContextHelper.insert(any, context);
            try {
                data = cdrCodec.encode_value(any);
            } catch (InvalidTypeForEncoding ex) {
                assertThat(false, is(true));
            }

            sc.context_id = REPLY_CONTEXT_4_ID.value;
            sc.context_data = new byte[data.length];
            System.arraycopy(data, 0, sc.context_data, 0, data.length);

            // try
            // {
            ri.add_reply_service_context(sc, false);
            // }
            // catch(BAD_INV_ORDER ex)
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
            try {
                ServiceContext sc = ri.get_request_service_context(REQUEST_CONTEXT_ID.value);
                assertThat(sc.context_id, is(REQUEST_CONTEXT_ID.value));
            } catch (BAD_PARAM ex) {
                assertThat(false, is(true));
            }

            // Test: add_reply_service_context
            ReplyContext context = new ReplyContext();
            context.data = "reply2";
            context.val = 102;
            Any any = ORB.init().create_any();
            ReplyContextHelper.insert(any, context);
            byte[] data = null;
            try {
                data = cdrCodec.encode_value(any);
            } catch (InvalidTypeForEncoding ex) {
                assertThat(false, is(true));
            }

            ServiceContext sc = new ServiceContext();
            sc.context_id = REPLY_CONTEXT_2_ID.value;
            sc.context_data = new byte[data.length];
            System.arraycopy(data, 0, sc.context_data, 0, data.length);

            try {
                ri.add_reply_service_context(sc, false);
            } catch (BAD_INV_ORDER ex) {
                assertThat(false, is(true));
            }

            // Test: add same context again (no replace)
            assertThrows(BAD_INV_ORDER.class, () -> ri.add_reply_service_context(sc, false));

            // Test: add same context again (replace)
            try {
                ri.add_reply_service_context(sc, true);
            } catch (BAD_INV_ORDER ex) {
                assertThat(false, is(true));
            }

            // Test: replace context added in
            // receive_request_service_context
            context.data = "reply4";
            context.val = 114;
            ReplyContextHelper.insert(any, context);
            try {
                data = cdrCodec.encode_value(any);
            } catch (InvalidTypeForEncoding ex) {
                assertThat(false, is(true));
            }

            sc.context_id = REPLY_CONTEXT_4_ID.value;
            sc.context_data = new byte[data.length];
            System.arraycopy(data, 0, sc.context_data, 0, data.length);

            try {
                ri.add_reply_service_context(sc, true);
            } catch (BAD_INV_ORDER ex) {
                assertThat(false, is(true));
            }
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
            Any slotData = null;
            try {
                slotData = ri.get_slot(0);
            } catch (InvalidSlot ex) {
                assertThat(false, is(true));
            }
            val = slotData.extract_long();
            assertThat(val, is(20));
        }
    }

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
            try {
                ri.forward_reference();
            } catch (BAD_INV_ORDER ex) {
                assertThat(false, is(true));
            }

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
            try {
                TypeCode[] exceptions = ri.exceptions();
                if (op.equals("userexception")) {
                    assertThat(exceptions.length, is(1));
                    assertThat(exceptions[0].equal(userHelper.type()), is(true));
                } else {
                    assertThat(exceptions.length, is(0));
                }
            } catch (BAD_INV_ORDER ex) {
                assertThat(false, is(true));
            } catch (NO_RESOURCES ex) {
                // Expected (if servant is DSI)
            }

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
            } catch (NO_RESOURCES ignored) // TODO: remove this!
            {
            }

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
