package org.apache.yoko.orb.OB;

import acme.idl.MY_CLIENT_POLICY_ID;
import acme.idl.MY_SERVER_POLICY_ID;
import acme.idl.MyClientPolicyFactory_impl;
import acme.idl.MyServerPolicy;
import acme.idl.MyServerPolicyFactory_impl;
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
import acme.idl.TestInterfacePackage.sHolder;
import acme.idl.TestInterfacePackage.user;
import acme.idl.TestInterfacePackage.userHelper;
import acme.idl.TestInterface_impl;
import acme.idl.TestLocator_impl;
import acme.idl.foo;
import acme.idl.fooHelper;
import acme.interceptors.CallInterceptorImpl;
import org.apache.yoko.orb.OBPortableServer.POAHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_INV_ORDER;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.INV_POLICY;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.NO_IMPLEMENT;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.NO_RESOURCES;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CORBA.ParameterMode;
import org.omg.CORBA.Policy;
import org.omg.CORBA.PolicyError;
import org.omg.CORBA.StringHolder;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.portable.InputStream;
import org.omg.Dynamic.Parameter;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecFactory;
import org.omg.IOP.CodecFactoryHelper;
import org.omg.IOP.CodecFactoryPackage.UnknownEncoding;
import org.omg.IOP.CodecPackage.FormatMismatch;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;
import org.omg.IOP.CodecPackage.TypeMismatch;
import org.omg.IOP.ENCODING_CDR_ENCAPS;
import org.omg.IOP.Encoding;
import org.omg.IOP.ServiceContext;
import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.ClientRequestInterceptor;
import org.omg.PortableInterceptor.Current;
import org.omg.PortableInterceptor.CurrentHelper;
import org.omg.PortableInterceptor.InvalidSlot;
import org.omg.PortableInterceptor.LOCATION_FORWARD;
import org.omg.PortableInterceptor.ORBInitInfo;
import org.omg.PortableInterceptor.ORBInitInfoPackage.DuplicateName;
import org.omg.PortableInterceptor.ORBInitializer;
import org.omg.PortableInterceptor.SUCCESSFUL;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.omg.PortableInterceptor.ServerRequestInterceptor;
import org.omg.PortableInterceptor.USER_EXCEPTION;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAManager;
import org.omg.PortableServer.ServantLocator;
import test.common.TestException;
import testify.bus.Bus;
import testify.bus.StringSpec;
import testify.jupiter.annotation.iiop.ConfigureOrb.UseWithOrb;
import testify.jupiter.annotation.iiop.ConfigureServer;
import testify.jupiter.annotation.iiop.ConfigureServer.BeforeServer;
import testify.streams.BiStream;

import java.util.EnumMap;

import static java.util.function.Function.identity;
import static org.apache.yoko.orb.OB.TestPortableInterceptors.Ior.DSI_INTERFACE;
import static org.apache.yoko.orb.OB.TestPortableInterceptors.Ior.TEST_INTERFACE;
import static org.apache.yoko.orb.OB.TestPortableInterceptors.MyClientRequestInterceptor.FIRST;
import static org.apache.yoko.orb.OB.TestPortableInterceptors.MyClientRequestInterceptor.SECOND;
import static org.apache.yoko.orb.OB.TestPortableInterceptors.MyClientRequestInterceptor.THIRD;
import static org.apache.yoko.orb.OB.Util.unmarshalSystemException;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.omg.CORBA.SetOverrideType.ADD_OVERRIDE;
import static org.omg.PortableServer.IdAssignmentPolicyValue.USER_ID;
import static org.omg.PortableServer.ImplicitActivationPolicyValue.NO_IMPLICIT_ACTIVATION;
import static org.omg.PortableServer.LifespanPolicyValue.PERSISTENT;
import static org.omg.PortableServer.RequestProcessingPolicyValue.USE_SERVANT_MANAGER;
import static org.omg.PortableServer.ServantRetentionPolicyValue.NON_RETAIN;

@ConfigureServer
public class TestPortableInterceptors {
    //===***===

    @UseWithOrb("server orb")
    public static final class MyServerRequestInterceptor extends LocalObject implements ServerRequestInterceptor, ORBInitializer {
        @Override
        public void pre_init(ORBInitInfo info) {
            try {
                info.add_server_request_interceptor(this);
                info.register_policy_factory(MY_SERVER_POLICY_ID.value, new MyServerPolicyFactory_impl());
            } catch (DuplicateName e) {
                throw new Error(e);
            }
        }

        @Override
        public void post_init(ORBInitInfo info) {
            try {
                cdrCodec_ = info.codec_factory().create_codec(new Encoding(ENCODING_CDR_ENCAPS.value, (byte)0, (byte)0));
            } catch (UnknownEncoding e) {
                throw new Error(e);
            }
        }

        private Codec cdrCodec_; // The cached CDR codec

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
                // Test: get_request_service_context
                try {
                    ServiceContext sc = ri.get_request_service_context(REQUEST_CONTEXT_ID.value);
                    assertThat(sc.context_id, is(REQUEST_CONTEXT_ID.value));
                } catch (BAD_PARAM ex) {
                    assertThat(false, is(true));
                }

                // Test: get_reply_service_context
                ServiceContext sc;
                try {
                    sc = ri.get_reply_service_context(REPLY_CONTEXT_4_ID.value);
                } catch (BAD_INV_ORDER ex) {
                    sc = null;
                    assertThat(false, is(true));
                }
                byte[] data = new byte[sc.context_data.length];
                System.arraycopy(sc.context_data, 0, data, 0, sc.context_data.length);

                Any any = null;
                try {
                    any = cdrCodec_.decode_value(data, ReplyContextHelper.type());
                } catch (FormatMismatch | TypeMismatch ex) {
                    assertThat(false, is(true));
                }
                ReplyContext context = ReplyContextHelper.extract(any);
                assertThat(context.data, is("reply4"));
                assertThat(context.val, is(114));

                if (addContext) {
                    // Test: add_reply_service_context
                    context.data = "reply3";
                    context.val = 103;
                    any = ORB.init().create_any();
                    ReplyContextHelper.insert(any, context);
                    try {
                        data = cdrCodec_.encode_value(any);
                    } catch (InvalidTypeForEncoding ex) {
                        assertThat(false, is(true));
                    }

                    sc.context_id = REPLY_CONTEXT_3_ID.value;
                    sc.context_data = new byte[data.length];
                    System.arraycopy(data, 0, sc.context_data, 0, data.length);

                    try {
                        ri.add_reply_service_context(sc, false);
                    } catch (BAD_INV_ORDER ex) {
                        assertThat(false, is(true));
                    }

                    // Test: add same context again (no replace)
                    ServiceContext sc1 = sc;
                    assertThrows(BAD_INV_ORDER.class, () -> ri.add_reply_service_context(sc1, false));

                    // Test: add same context again (replace)
                    try {
                        ri.add_reply_service_context(sc, true);
                    } catch (BAD_INV_ORDER ex) {
                        assertThat(false, is(true));
                    }

                    // Test: replace context added in receive_request
                    context.data = "reply4";
                    context.val = 124;
                    ReplyContextHelper.insert(any, context);
                    try {
                        data = cdrCodec_.encode_value(any);
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
            try {
                // Test: get operation name
                String op = ri.operation();

                boolean oneway = (op.equals("noargs_oneway"));

                // Test: Arguments should not be available
                assertThrows(BAD_INV_ORDER.class, ri::arguments);

                // TODO: test operation_context

                // Test: result is not available
                assertThrows(BAD_INV_ORDER.class, ri::result);

                // Test: exceptions
                assertThrows(BAD_INV_ORDER.class, ri::exceptions);

                // Test: response expected and oneway should be equivalent
                boolean expr = (oneway && !ri.response_expected()) || (!oneway && ri.response_expected());
                assertThat(expr, is(true));

                // TODO: test sync scope

                // Test: reply status is not available
                assertThrows(BAD_INV_ORDER.class, ri::reply_status);

                // Test: forward reference is not available
                assertThrows(BAD_INV_ORDER.class, ri::forward_reference);

                // Test: object id is not available
                assertThrows(BAD_INV_ORDER.class, ri::object_id);

                // Test: adapter id is not available
                assertThrows(BAD_INV_ORDER.class, ri::adapter_id);

                // Test: servant_most_derived_interface is not available
                assertThrows(BAD_INV_ORDER.class, ri::target_most_derived_interface);

                // Test: server id is not available
                assertThrows(BAD_INV_ORDER.class, ri::server_id);

                // Test: orb id is not available
                assertThrows(BAD_INV_ORDER.class, ri::orb_id);

                // Test: adapter name is not available
                assertThrows(BAD_INV_ORDER.class, ri::adapter_name);

                // Test: servant_is_a is not available
                assertThrows(BAD_INV_ORDER.class, () -> ri.target_is_a(""));

                if (op.equals("test_service_context")) {
                    // Test: get_request_service_context
                    try {
                        ServiceContext sc = ri.get_request_service_context(REQUEST_CONTEXT_ID.value);
                        assertThat(sc.context_id, is(REQUEST_CONTEXT_ID.value));
                        byte[] data = new byte[sc.context_data.length];
                        System.arraycopy(sc.context_data, 0, data, 0, sc.context_data.length);

                        Any any = null;
                        try {
                            any = cdrCodec_.decode_value(data, RequestContextHelper.type());
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
                        data = cdrCodec_.encode_value(any);
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
                        data = cdrCodec_.encode_value(any);
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
                Policy policy = ri.get_server_policy(test.pi.MY_SERVER_POLICY_ID.value);
                MyServerPolicy myServerPolicy = MyServerPolicyHelper.narrow(policy);
                assertThat(myServerPolicy, not(sameInstance(null)));
                assertThat(myServerPolicy.value(), is(10));

                assertThrows(INV_POLICY.class, () -> ri.get_server_policy(1013));
            } catch (TestException ex) {
                ex.printStackTrace();
                throw ex;
            }
        }

        public void receive_request(ServerRequestInfo ri) {
            try {
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
                        data = cdrCodec_.encode_value(any);
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
                        data = cdrCodec_.encode_value(any);
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
                Policy policy = ri.get_server_policy(test.pi.MY_SERVER_POLICY_ID.value);
                MyServerPolicy myServerPolicy = MyServerPolicyHelper.narrow(policy);
                assertThat(myServerPolicy, not(sameInstance(null)));
                assertThat(myServerPolicy.value(), is(10));

                assertThrows(INV_POLICY.class, () -> ri.get_server_policy(1013));
            } catch (TestException ex) {
                ex.printStackTrace();
                throw ex;
            }
        }

        public void send_reply(ServerRequestInfo ri) {
            try {
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
                Policy policy = ri.get_server_policy(test.pi.MY_SERVER_POLICY_ID.value);
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
            } catch (TestException ex) {
                ex.printStackTrace();
                throw ex;
            }
        }

        public void send_other(ServerRequestInfo ri) {
            try {
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
                Policy policy = ri.get_server_policy(test.pi.MY_SERVER_POLICY_ID.value);
                MyServerPolicy myServerPolicy = MyServerPolicyHelper.narrow(policy);
                assertThat(myServerPolicy, not(sameInstance(null)));
                assertThat(myServerPolicy.value(), is(10));

                assertThrows(INV_POLICY.class, () -> ri.get_server_policy(1013));
            } catch (TestException ex) {
                ex.printStackTrace();
                throw ex;
            }
        }

        public void send_exception(ServerRequestInfo ri) {
            try {
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
                assertThat(adapterName.length == 1 && adapterName[0].equals("persistent"), is(true));

                // Test: target_is_a raises BAD_INV_ORDER
                assertThrows(BAD_INV_ORDER.class, () -> ri.target_is_a("IDL:TestInterface:1.0"));

                // Test: get_server_policy
                Policy policy = ri.get_server_policy(test.pi.MY_SERVER_POLICY_ID.value);
                MyServerPolicy myServerPolicy = MyServerPolicyHelper.narrow(policy);
                assertThat(myServerPolicy, not(sameInstance(null)));
                assertThat(myServerPolicy.value(), is(10));

                assertThrows(INV_POLICY.class, () -> ri.get_server_policy(1013));
            } catch (TestException ex) {
                ex.printStackTrace();
                throw ex;
            }
        }
    }

    @SuppressWarnings("unused")
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
                info.register_policy_factory(MY_CLIENT_POLICY_ID.value, new MyClientPolicyFactory_impl());
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
    //===***===

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

    enum Ior implements StringSpec {TEST_INTERFACE, DSI_INTERFACE}
    private static final EnumMap<Ior, TestInterface> STUB_MAP = new EnumMap<>(Ior.class);

    @BeforeAll
    public static void beforeAll(ORB orb, Bus bus) throws PolicyError {
        Any any = orb.create_any();
        any.insert_long(10);
        Policy[] pl = {orb.create_policy(MY_CLIENT_POLICY_ID.value, any)};
        // populate the enum map with stubs narrowed from the stringified IORs put in the bus by the server
        BiStream.of(Ior.values(), identity())
                .mapValues(bus::get)
                .mapValues(orb::string_to_object)
                .mapValues(o -> o._set_policy_override(pl, ADD_OVERRIDE))
                .mapValues(TestInterfaceHelper::narrow)
                .forEach(STUB_MAP::put);
    }
    
    // TODO: Port test.pi.Client & test.pi.Collocated

    @Test
    void testCodec(ORB orb) {
        //
        // Test: Resolve CodecFactory
        //
        CodecFactory factory = null;
        try {
            factory = CodecFactoryHelper.narrow(orb.resolve_initial_references("CodecFactory"));
        } catch (InvalidName ex) {
            fail();
        }
        assertNotNull(factory);

        Encoding how = new Encoding();
        how.major_version = 0;
        how.minor_version = 0;

        //
        // Test: Create non-existent codec
        //
        try {
            how.format = 1; // Some unknown value
            factory.create_codec(how);
            fail();
        } catch (UnknownEncoding ex) {
            // Expected
        }

        //
        // Test: CDR Codec
        //
        how.format = ENCODING_CDR_ENCAPS.value;
        Codec cdrCodec = null;
        try {
            cdrCodec = factory.create_codec(how);
        } catch (UnknownEncoding ex) {
            fail();
        }
        assertNotNull(cdrCodec);

        //
        // Test: Encode/decode
        //
        foo f = new foo();
        f.l = 10;
        Any any = orb.create_any();
        fooHelper.insert(any, f);

        byte[] encoding = null;
        try {
            encoding = cdrCodec.encode(any);
        } catch (InvalidTypeForEncoding ex) {
            fail();
        }
        Any result = null;
        try {
            result = cdrCodec.decode(encoding);
        } catch (FormatMismatch ex) {
            fail();
        }

        foo newf = fooHelper.extract(result);
        assertEquals(10, newf.l);

        //
        // Test: Encode/decode
        //
        try {
            encoding = cdrCodec.encode_value(any);
        } catch (InvalidTypeForEncoding ex) {
            fail();
        }
        try {
            result = cdrCodec.decode_value(encoding, fooHelper.type());
        } catch (FormatMismatch | TypeMismatch ex) {
            fail();
        }

        newf = fooHelper.extract(result);
        assertEquals(10, newf.l);
    }

    @Test
    void testTranslation(ORB orb) {
        TestInterface ti = STUB_MAP.get(TEST_INTERFACE);

        FIRST.throwOnRequest(new NO_PERMISSION());
        assertThrows(NO_PERMISSION.class, ti::noargs);

        FIRST.noThrowOnRequest();
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

    @Test
    private static void TestCalls(ORB orb, TestInterface ti) throws Exception {
        final Current pic;
        org.omg.CORBA.Object obj = orb.resolve_initial_references("PICurrent");
        pic = CurrentHelper.narrow(obj);
        assertNotNull(pic);

        Any slotData = orb.create_any();
        slotData.insert_long(10);

        pic.set_slot(0, slotData);

        //
        // Set up the correct interceptor
        //
        CallInterceptorImpl impl = new CallInterceptorImpl(orb);
        ClientRequestInterceptor interceptor = impl;
        manager.setInterceptor(0, interceptor);
        int num = 0;

        ti.noargs();
        assertTrue(++num == impl._OB_numReq());

        ti.noargs_oneway();
        assertTrue(++num == impl._OB_numReq());

        assertThrows(user.class, () -> ti.userexception());
        assertTrue(++num == impl._OB_numReq());

        assertThrows(SystemException.class, () -> ti.systemexception());
        assertTrue(++num == impl._OB_numReq());

        ti.test_service_context();
        assertTrue(++num == impl._OB_numReq());

        try {
            ti.location_forward();
        } catch (NO_IMPLEMENT ex) {
        }
        assertTrue(++num == impl._OB_numReq());

        //
        // Test simple attribute
        //
        ti.string_attrib("TEST");
        assertTrue(++num == impl._OB_numReq());
        String satt = ti.string_attrib();
        assertTrue(satt.equals("TEST"));
        assertTrue(++num == impl._OB_numReq());

        //
        // Test in, inout and out simple parameters
        //
        ti.one_string_in("TEST");
        assertTrue(++num == impl._OB_numReq());

        StringHolder spinout = new StringHolder("TESTINOUT");
        ti.one_string_inout(spinout);
        assertTrue(spinout.value.equals("TEST"));
        assertTrue(++num == impl._OB_numReq());

        StringHolder spout = new StringHolder();
        ti.one_string_out(spout);
        assertTrue(spout.value.equals("TEST"));
        assertTrue(++num == impl._OB_numReq());

        String sprc = ti.one_string_return();
        assertTrue(sprc.equals("TEST"));
        assertTrue(++num == impl._OB_numReq());

        //
        // Test struct attribute
        //
        s ss = new s();
        ss.sval = "TEST";
        ti.struct_attrib(ss);
        assertTrue(++num == impl._OB_numReq());
        s ssatt = ti.struct_attrib();
        assertTrue(ssatt.sval.equals("TEST"));
        assertTrue(++num == impl._OB_numReq());

        //
        // Test in, inout and out struct parameters
        //
        ti.one_struct_in(ss);
        assertTrue(++num == impl._OB_numReq());

        sHolder sinout = new sHolder(new s("TESTINOUT"));
        ti.one_struct_inout(sinout);
        assertTrue(sinout.value.sval.equals("TEST"));
        assertTrue(++num == impl._OB_numReq());

        sHolder sout = new sHolder();
        ti.one_struct_out(sout);
        assertTrue(sout.value.sval.equals("TEST"));
        assertTrue(++num == impl._OB_numReq());

        s ssrc = ti.one_struct_return();
        assertTrue(ssrc.sval.equals("TEST"));
        assertTrue(++num == impl._OB_numReq());

        manager.clearInterceptors();

        //
        // Test: PortableInterceptor::Current still has the same value
        //
        Any slotData2 = null;
        try {
            slotData2 = pic.get_slot(0);
        } catch (InvalidSlot ex) {
            assertTrue(false);
        }
        int v = slotData2.extract_long();
        assertTrue(v == 10);
    }


}
