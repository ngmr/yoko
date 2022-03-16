package org.apache.yoko.orb.OB;

import acme.idl.TestInterfacePackage.user;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_INV_ORDER;
import org.omg.CORBA.NO_IMPLEMENT;
import org.omg.CORBA.ORB;
import org.omg.CORBA.SystemException;
import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.Current;
import org.omg.PortableInterceptor.InvalidSlot;
import org.omg.PortableInterceptor.ORBInitInfo;
import org.omg.PortableInterceptor.ServerRequestInfo;
import testify.iiop.TestClientRequestInterceptor;
import testify.iiop.TestServerRequestInterceptor;
import testify.jupiter.annotation.iiop.ConfigureOrb.UseWithOrb;
import testify.jupiter.annotation.iiop.ConfigureServer.BeforeServer;

import java.util.function.Supplier;

import static acme.interceptors.PICurrentTestSlotValues.CALLER;
import static acme.interceptors.PICurrentTestSlotValues.EMPTY;
import static acme.interceptors.PICurrentTestSlotValues.RECEIVE_EXCEPTION;
import static acme.interceptors.PICurrentTestSlotValues.RECEIVE_OTHER;
import static acme.interceptors.PICurrentTestSlotValues.RECEIVE_REPLY;
import static acme.interceptors.PICurrentTestSlotValues.RECEIVE_REQUEST_0;
import static acme.interceptors.PICurrentTestSlotValues.RECEIVE_REQUEST_1;
import static acme.interceptors.PICurrentTestSlotValues.RECEIVE_REQUEST_SERVICE_CONTEXTS_0;
import static acme.interceptors.PICurrentTestSlotValues.RECEIVE_REQUEST_SERVICE_CONTEXTS_1;
import static acme.interceptors.PICurrentTestSlotValues.SEND_EXCEPTION_0;
import static acme.interceptors.PICurrentTestSlotValues.SEND_EXCEPTION_1;
import static acme.interceptors.PICurrentTestSlotValues.SEND_OTHER_0;
import static acme.interceptors.PICurrentTestSlotValues.SEND_OTHER_1;
import static acme.interceptors.PICurrentTestSlotValues.SEND_POLL;
import static acme.interceptors.PICurrentTestSlotValues.SEND_REPLY_0;
import static acme.interceptors.PICurrentTestSlotValues.SEND_REPLY_1;
import static acme.interceptors.PICurrentTestSlotValues.SEND_REQUEST;
import static org.apache.yoko.orb.OB.PortableInterceptorTest.StubType.DSI_INTERFACE;
import static org.apache.yoko.orb.OB.PortableInterceptorTest.StubType.TEST_INTERFACE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PICurrentTest extends PortableInterceptorTest {
    static final int INVALID_SLOT_ID = -1;

    static Supplier<Current> CLIENT_THREAD_PICURRENT = () -> {throw new BAD_INV_ORDER();};
    static Supplier<Current> SERVER_THREAD_PICURRENT = () -> {throw new BAD_INV_ORDER();};
    static int CLIENT_SLOT_ID = INVALID_SLOT_ID;
    static int SERVER_SLOT_ID = INVALID_SLOT_ID;

    @BeforeAll
    public static void initClient(ORB orb) {
        System.out.println("initClient");
        CLIENT_THREAD_PICURRENT = () -> assertDoesNotThrow(() -> (Current) orb.resolve_initial_references("PICurrent"));
    }

    @BeforeServer
    public static void initServer(ORB orb) {
        System.out.println("initServer");
        SERVER_THREAD_PICURRENT = () -> assertDoesNotThrow(() -> (Current) orb.resolve_initial_references("PICurrent"));
    }

    @UseWithOrb("client orb")
    public static class MyClientInterceptor implements TestClientRequestInterceptor {
        @Override
        public void pre_init(ORBInitInfo info) { CLIENT_SLOT_ID = info.allocate_slot_id(); }

        @Override
        public void send_request(ClientRequestInfo ri) {
            System.out.println("send_request - start");
            assertThrows(InvalidSlot.class, () -> ri.get_slot(INVALID_SLOT_ID));
            assertTrue(assertDoesNotThrow(() -> ri.get_slot(CLIENT_SLOT_ID)).equal(CALLER.any));

            final Current current = CLIENT_THREAD_PICURRENT.get();
            assertThrows(InvalidSlot.class, () -> current.get_slot(INVALID_SLOT_ID));
            assertThrows(InvalidSlot.class, () -> current.set_slot(INVALID_SLOT_ID, SEND_REQUEST.any));
            assertTrue(assertDoesNotThrow(() -> current.get_slot(CLIENT_SLOT_ID)).equal(EMPTY.any));
            // CORBA 3.0.3, section 21.4.4.6
            // this should not propagate to other interception points
            assertDoesNotThrow(() -> current.set_slot(CLIENT_SLOT_ID, SEND_REQUEST.any));
            System.out.println("send_request - end");
        }

        @Override
        public void send_poll(ClientRequestInfo ri) {
            assertThrows(InvalidSlot.class, () -> ri.get_slot(INVALID_SLOT_ID));
            assertTrue(assertDoesNotThrow(() -> ri.get_slot(CLIENT_SLOT_ID)).equal(CALLER.any));

            final Current current = CLIENT_THREAD_PICURRENT.get();
            assertThrows(InvalidSlot.class, () -> current.get_slot(INVALID_SLOT_ID));
            assertThrows(InvalidSlot.class, () -> current.set_slot(INVALID_SLOT_ID, SEND_POLL.any));
            assertTrue(assertDoesNotThrow(() -> ri.get_slot(CLIENT_SLOT_ID)).equal(EMPTY.any));
            // CORBA 3.0.3, section 21.4.4.6
            // this should not propagate to other interception points
            assertDoesNotThrow(() -> current.set_slot(CLIENT_SLOT_ID, SEND_POLL.any));
        }

        @Override
        public void receive_reply(ClientRequestInfo ri) {
            assertThrows(InvalidSlot.class, () -> ri.get_slot(INVALID_SLOT_ID));
            assertTrue(assertDoesNotThrow(() -> ri.get_slot(CLIENT_SLOT_ID)).equal(CALLER.any));

            final Current current = CLIENT_THREAD_PICURRENT.get();
            assertThrows(InvalidSlot.class, () -> current.get_slot(INVALID_SLOT_ID));
            assertThrows(InvalidSlot.class, () -> current.set_slot(INVALID_SLOT_ID, RECEIVE_REPLY.any));
            assertTrue(assertDoesNotThrow(() -> ri.get_slot(CLIENT_SLOT_ID)).equal(EMPTY.any));
            // CORBA 3.0.3, section 21.4.4.6
            // this should not propagate to other interception points
            assertDoesNotThrow(() -> current.set_slot(CLIENT_SLOT_ID, RECEIVE_REPLY.any));
        }

        @Override
        public void receive_exception(ClientRequestInfo ri) {
            assertThrows(InvalidSlot.class, () -> ri.get_slot(INVALID_SLOT_ID));
            assertTrue(assertDoesNotThrow(() -> ri.get_slot(CLIENT_SLOT_ID)).equal(CALLER.any));

            final Current current = CLIENT_THREAD_PICURRENT.get();
            assertThrows(InvalidSlot.class, () -> current.get_slot(INVALID_SLOT_ID));
            assertThrows(InvalidSlot.class, () -> current.set_slot(INVALID_SLOT_ID, RECEIVE_EXCEPTION.any));
            assertTrue(assertDoesNotThrow(() -> ri.get_slot(CLIENT_SLOT_ID)).equal(EMPTY.any));
            // CORBA 3.0.3, section 21.4.4.6
            // this should not propagate to other interception points
            assertDoesNotThrow(() -> current.set_slot(CLIENT_SLOT_ID, RECEIVE_EXCEPTION.any));
        }

        @Override
        public void receive_other(ClientRequestInfo ri) {
            assertThrows(InvalidSlot.class, () -> ri.get_slot(INVALID_SLOT_ID));
            assertTrue(assertDoesNotThrow(() -> ri.get_slot(CLIENT_SLOT_ID)).equal(CALLER.any));

            final Current current = CLIENT_THREAD_PICURRENT.get();
            assertThrows(InvalidSlot.class, () -> current.get_slot(INVALID_SLOT_ID));
            assertThrows(InvalidSlot.class, () -> current.set_slot(INVALID_SLOT_ID, RECEIVE_OTHER.any));
            assertTrue(assertDoesNotThrow(() -> ri.get_slot(CLIENT_SLOT_ID)).equal(EMPTY.any));
            // CORBA 3.0.3, section 21.4.4.6
            // this should not propagate to other interception points
            assertDoesNotThrow(() -> current.set_slot(CLIENT_SLOT_ID, RECEIVE_OTHER.any));
            throw new NO_IMPLEMENT(); // Eat the location forward
        }
    }

    @UseWithOrb("server orb")
    public static class MyServerInterceptor implements TestServerRequestInterceptor {
        @Override
        public void pre_init(ORBInitInfo info) {
            System.out.println("server pre_init");
            SERVER_SLOT_ID = info.allocate_slot_id();
        }

        @Override
        public void receive_request_service_contexts(ServerRequestInfo ri) {
            System.out.println("receive_request_service_contexts - start");
            assertThrows(InvalidSlot.class, () -> ri.get_slot(INVALID_SLOT_ID));
            assertThrows(InvalidSlot.class, () -> ri.set_slot(INVALID_SLOT_ID, RECEIVE_REQUEST_SERVICE_CONTEXTS_0.any));
            assertTrue(assertDoesNotThrow(() -> ri.get_slot(SERVER_SLOT_ID)).equal(EMPTY.any));
            assertDoesNotThrow(() -> ri.set_slot(SERVER_SLOT_ID, RECEIVE_REQUEST_SERVICE_CONTEXTS_0.any));

            final Current current = SERVER_THREAD_PICURRENT.get();
            assertThrows(InvalidSlot.class, () -> current.get_slot(INVALID_SLOT_ID));
            assertThrows(InvalidSlot.class, () -> current.set_slot(INVALID_SLOT_ID, RECEIVE_REQUEST_SERVICE_CONTEXTS_1.any));
            assertTrue(assertDoesNotThrow(() -> current.get_slot(SERVER_SLOT_ID)).equal(EMPTY.any));
            // CORBA 3.0.3, section 21.4.4.6
            // this should not propagate to other interception points
            assertDoesNotThrow(() -> current.set_slot(SERVER_SLOT_ID, RECEIVE_REQUEST_SERVICE_CONTEXTS_1.any));
            System.out.println("receive_request_service_contexts - end");
        }

        @Override
        public void receive_request(ServerRequestInfo ri) {
            System.out.println("receive_request - " + ri.operation());
            assertThrows(InvalidSlot.class, () -> ri.get_slot(INVALID_SLOT_ID));
            assertThrows(InvalidSlot.class, () -> ri.set_slot(INVALID_SLOT_ID, RECEIVE_REQUEST_0.any));
            assertTrue(assertDoesNotThrow(() -> ri.get_slot(SERVER_SLOT_ID)).equal(RECEIVE_REQUEST_SERVICE_CONTEXTS_0.any));
            assertThrows(InvalidSlot.class, () -> ri.set_slot(SERVER_SLOT_ID, RECEIVE_REQUEST_0.any));

            final Current current = SERVER_THREAD_PICURRENT.get();
            assertThrows(InvalidSlot.class, () -> current.get_slot(INVALID_SLOT_ID));
            assertThrows(InvalidSlot.class, () -> current.set_slot(INVALID_SLOT_ID, RECEIVE_REQUEST_1.any));
            assertTrue(assertDoesNotThrow(() -> ri.get_slot(SERVER_SLOT_ID)).equal(EMPTY.any));
            assertDoesNotThrow(() -> current.set_slot(SERVER_SLOT_ID, RECEIVE_REQUEST_1.any));
        }

        @Override
        public void send_reply(ServerRequestInfo ri) {
            assertThrows(InvalidSlot.class, () -> ri.get_slot(INVALID_SLOT_ID));
            assertThrows(InvalidSlot.class, () -> ri.set_slot(INVALID_SLOT_ID, SEND_REPLY_0.any));
            assertTrue(assertDoesNotThrow(() -> ri.get_slot(SERVER_SLOT_ID)).equal(RECEIVE_REQUEST_0.any));
            assertThrows(InvalidSlot.class, () -> ri.set_slot(SERVER_SLOT_ID, SEND_REPLY_0.any));

            final Current current = SERVER_THREAD_PICURRENT.get();
            assertThrows(InvalidSlot.class, () -> current.get_slot(INVALID_SLOT_ID));
            assertThrows(InvalidSlot.class, () -> current.set_slot(INVALID_SLOT_ID, SEND_REPLY_1.any));
            assertTrue(assertDoesNotThrow(() -> current.get_slot(SERVER_SLOT_ID)).equal(RECEIVE_REQUEST_1.any));
            assertDoesNotThrow(() -> current.set_slot(SERVER_SLOT_ID, SEND_REPLY_1.any));
        }

        @Override
        public void send_exception(ServerRequestInfo ri) {
            assertThrows(InvalidSlot.class, () -> ri.get_slot(INVALID_SLOT_ID));
            assertThrows(InvalidSlot.class, () -> ri.set_slot(INVALID_SLOT_ID, SEND_EXCEPTION_0.any));
            assertTrue(assertDoesNotThrow(() -> ri.get_slot(SERVER_SLOT_ID)).equal(RECEIVE_REQUEST_0.any));
            assertThrows(InvalidSlot.class, () -> ri.set_slot(SERVER_SLOT_ID, SEND_EXCEPTION_0.any));

            final Current current = SERVER_THREAD_PICURRENT.get();
            assertThrows(InvalidSlot.class, () -> current.get_slot(INVALID_SLOT_ID));
            assertThrows(InvalidSlot.class, () -> current.set_slot(INVALID_SLOT_ID, SEND_EXCEPTION_1.any));
            assertTrue(assertDoesNotThrow(() -> current.get_slot(SERVER_SLOT_ID)).equal(RECEIVE_REQUEST_1.any));
            assertDoesNotThrow(() -> current.set_slot(SERVER_SLOT_ID, SEND_EXCEPTION_1.any));
        }

        @Override
        public void send_other(ServerRequestInfo ri) {
            assertThrows(InvalidSlot.class, () -> ri.get_slot(INVALID_SLOT_ID));
            assertThrows(InvalidSlot.class, () -> ri.set_slot(INVALID_SLOT_ID, SEND_OTHER_0.any));
            assertTrue(assertDoesNotThrow(() -> ri.get_slot(SERVER_SLOT_ID)).equal(RECEIVE_REQUEST_0.any));
            assertThrows(InvalidSlot.class, () -> ri.set_slot(SERVER_SLOT_ID, SEND_OTHER_0.any));

            final Current current = SERVER_THREAD_PICURRENT.get();
            assertThrows(InvalidSlot.class, () -> current.get_slot(INVALID_SLOT_ID));
            assertThrows(InvalidSlot.class, () -> current.set_slot(INVALID_SLOT_ID, SEND_OTHER_1.any));
            assertTrue(assertDoesNotThrow(() -> current.get_slot(SERVER_SLOT_ID)).equal(RECEIVE_REQUEST_1.any));
            assertDoesNotThrow(() -> current.set_slot(SERVER_SLOT_ID, SEND_OTHER_1.any));
        }
    }

    @BeforeEach
    public void setupSlot() {
        System.out.println("setupSlot");
        assertDoesNotThrow(() -> CLIENT_THREAD_PICURRENT.get().set_slot(CLIENT_SLOT_ID, CALLER.any));
    }

    @AfterEach
    public void checkSlot() {
        final Any actual = assertDoesNotThrow(() -> CLIENT_THREAD_PICURRENT.get().get_slot(CLIENT_SLOT_ID));
        assertTrue(CALLER.any.equal(actual), () -> "Slot value " + actual + " not equal to expected " + CALLER.any);
    }

    @Test
    public void testNormal() {
        assertDoesNotThrow(() -> STUB_MAP.get(TEST_INTERFACE).noargs());
        assertDoesNotThrow(() -> STUB_MAP.get(DSI_INTERFACE).noargs());
    }

    // TODO: drive send_poll

    @Test
    public void testUserException() {
        assertThrows(user.class, STUB_MAP.get(TEST_INTERFACE)::userexception);
        assertThrows(user.class, STUB_MAP.get(DSI_INTERFACE)::userexception);
    }

    @Test
    public void testSystemException() {
        assertThrows(SystemException.class, STUB_MAP.get(TEST_INTERFACE)::systemexception);
        assertThrows(SystemException.class, STUB_MAP.get(DSI_INTERFACE)::systemexception);
    }

    @Test
    public void testLocationForward() {
        System.out.println("testForward non-DSI");
        assertThrows(NO_IMPLEMENT.class, STUB_MAP.get(TEST_INTERFACE)::location_forward);
        System.out.println("testForward DSI");
        assertThrows(NO_IMPLEMENT.class, STUB_MAP.get(DSI_INTERFACE)::location_forward);
        System.out.println("testForward done");
    }
}
