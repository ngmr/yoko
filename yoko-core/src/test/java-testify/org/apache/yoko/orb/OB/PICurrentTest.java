package org.apache.yoko.orb.OB;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_INV_ORDER;
import org.omg.CORBA.NO_IMPLEMENT;
import org.omg.CORBA.ORB;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.UNKNOWN;
import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.Current;
import org.omg.PortableInterceptor.ForwardRequest;
import org.omg.PortableInterceptor.InvalidSlot;
import org.omg.PortableInterceptor.ORBInitInfo;
import org.omg.PortableInterceptor.ServerRequestInfo;
import testify.iiop.TestClientRequestInterceptor;
import testify.iiop.TestServerRequestInterceptor;
import testify.jupiter.annotation.iiop.ConfigureOrb.UseWithOrb;
import testify.jupiter.annotation.iiop.ConfigureServer;
import testify.jupiter.annotation.iiop.ConfigureServer.BeforeServer;
import testify.jupiter.annotation.iiop.ConfigureServer.ClientStub;

import javax.rmi.CORBA.Util;
import javax.rmi.PortableRemoteObject;
import java.rmi.Remote;
import java.rmi.RemoteException;
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
import static org.apache.yoko.orb.OB.PICurrentTest.MyRemoteImpl.forwardImpl;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static testify.jupiter.annotation.iiop.ConfigureServer.Separation.COLLOCATED;

@ConfigureServer
public class PICurrentTest {
    /** Re-run the tests collocated */
    @ConfigureServer(separation = COLLOCATED)
    static class TestCollocated extends PICurrentTest {}

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
        public void pre_init(ORBInitInfo info) {
            System.out.println("client pre_init ...");
            CLIENT_SLOT_ID = info.allocate_slot_id();
        }

        @Override
        public void send_request(ClientRequestInfo ri) {
            try {
                System.out.println("send_request ...");
                assertThrows(InvalidSlot.class, () -> ri.get_slot(INVALID_SLOT_ID));
                checkEqual(CALLER.any, () -> ri.get_slot(CLIENT_SLOT_ID));

                final Current current = CLIENT_THREAD_PICURRENT.get();
                assertThrows(InvalidSlot.class, () -> current.get_slot(INVALID_SLOT_ID));
                assertThrows(InvalidSlot.class, () -> current.set_slot(INVALID_SLOT_ID, SEND_REQUEST.any));
                checkEqual(EMPTY.any, () -> current.get_slot(CLIENT_SLOT_ID));
                // CORBA 3.0.3, section 21.4.4.6
                // this should not propagate to other interception points
                assertDoesNotThrow(() -> current.set_slot(CLIENT_SLOT_ID, SEND_REQUEST.any));
            } catch (RuntimeException e) {
                e.printStackTrace(System.out);
                throw e;
            }
        }

        @Override
        public void send_poll(ClientRequestInfo ri) {
            try {
                System.out.println("send_poll ...");
                assertThrows(InvalidSlot.class, () -> ri.get_slot(INVALID_SLOT_ID));
                checkEqual(CALLER.any, () -> ri.get_slot(CLIENT_SLOT_ID));

                final Current current = CLIENT_THREAD_PICURRENT.get();
                assertThrows(InvalidSlot.class, () -> current.get_slot(INVALID_SLOT_ID));
                assertThrows(InvalidSlot.class, () -> current.set_slot(INVALID_SLOT_ID, SEND_POLL.any));
                checkEqual(EMPTY.any, () -> current.get_slot(CLIENT_SLOT_ID));
                // CORBA 3.0.3, section 21.4.4.6
                // this should not propagate to other interception points
                assertDoesNotThrow(() -> current.set_slot(CLIENT_SLOT_ID, SEND_POLL.any));
            } catch (RuntimeException e) {
                e.printStackTrace(System.out);
                throw e;
            }
        }

        @Override
        public void receive_reply(ClientRequestInfo ri) {
            try {
                System.out.println("receive_reply ...");
                assertThrows(InvalidSlot.class, () -> ri.get_slot(INVALID_SLOT_ID));
                checkEqual(CALLER.any, () -> ri.get_slot(CLIENT_SLOT_ID));

                final Current current = CLIENT_THREAD_PICURRENT.get();
                assertThrows(InvalidSlot.class, () -> current.get_slot(INVALID_SLOT_ID));
                assertThrows(InvalidSlot.class, () -> current.set_slot(INVALID_SLOT_ID, RECEIVE_REPLY.any));
                // TODO: checkEqual(EMPTY.any, () -> current.get_slot(CLIENT_SLOT_ID));
                // CORBA 3.0.3, section 21.4.4.6
                // this should not propagate to other interception points
                assertDoesNotThrow(() -> current.set_slot(CLIENT_SLOT_ID, RECEIVE_REPLY.any));
            } catch (RuntimeException e) {
                e.printStackTrace(System.out);
                throw e;
            }
        }

        @Override
        public void receive_exception(ClientRequestInfo ri) {
            try {
                System.out.println("receive_exception ...");
                assertThrows(InvalidSlot.class, () -> ri.get_slot(INVALID_SLOT_ID));
                checkEqual(CALLER.any, () -> ri.get_slot(CLIENT_SLOT_ID));

                final Current current = CLIENT_THREAD_PICURRENT.get();
                assertThrows(InvalidSlot.class, () -> current.get_slot(INVALID_SLOT_ID));
                assertThrows(InvalidSlot.class, () -> current.set_slot(INVALID_SLOT_ID, RECEIVE_EXCEPTION.any));
                // TODO: checkEqual(EMPTY.any, () -> current.get_slot(CLIENT_SLOT_ID));
                // CORBA 3.0.3, section 21.4.4.6
                // this should not propagate to other interception points
                assertDoesNotThrow(() -> current.set_slot(CLIENT_SLOT_ID, RECEIVE_EXCEPTION.any));
            } catch (RuntimeException e) {
                e.printStackTrace(System.out);
                throw e;
            }
        }

        @Override
        public void receive_other(ClientRequestInfo ri) {
            try {
                System.out.println("receive_other ...");
                assertThrows(InvalidSlot.class, () -> ri.get_slot(INVALID_SLOT_ID));
                checkEqual(CALLER.any, () -> ri.get_slot(CLIENT_SLOT_ID));

                final Current current = CLIENT_THREAD_PICURRENT.get();
                assertThrows(InvalidSlot.class, () -> current.get_slot(INVALID_SLOT_ID));
                assertThrows(InvalidSlot.class, () -> current.set_slot(INVALID_SLOT_ID, RECEIVE_OTHER.any));
                // TODO: checkEqual(EMPTY.any, () -> current.get_slot(CLIENT_SLOT_ID));
                // CORBA 3.0.3, section 21.4.4.6
                // this should not propagate to other interception points
                assertDoesNotThrow(() -> current.set_slot(CLIENT_SLOT_ID, RECEIVE_OTHER.any));
            } catch (RuntimeException e) {
                e.printStackTrace(System.out);
                throw e;
            }
            throw new NO_IMPLEMENT(); // Eat the location forward
        }
    }

    @UseWithOrb("server orb")
    public static class MyServerInterceptor implements TestServerRequestInterceptor {
        @Override
        public void pre_init(ORBInitInfo info) {
            System.out.println("server pre_init ...");
            SERVER_SLOT_ID = info.allocate_slot_id();
        }

        @Override
        public void receive_request_service_contexts(ServerRequestInfo ri) {
            try {
                System.out.printf("receive_request_service_contexts (%s) ...%n", ri.operation());
                assertThrows(InvalidSlot.class, () -> ri.get_slot(INVALID_SLOT_ID));
                assertThrows(InvalidSlot.class, () -> ri.set_slot(INVALID_SLOT_ID, RECEIVE_REQUEST_SERVICE_CONTEXTS_0.any));
                checkEqual(EMPTY.any, () -> ri.get_slot(SERVER_SLOT_ID));
                assertDoesNotThrow(() -> ri.set_slot(SERVER_SLOT_ID, RECEIVE_REQUEST_SERVICE_CONTEXTS_0.any));

                final Current current = SERVER_THREAD_PICURRENT.get();
                assertThrows(InvalidSlot.class, () -> current.get_slot(INVALID_SLOT_ID));
                assertThrows(InvalidSlot.class, () -> current.set_slot(INVALID_SLOT_ID, RECEIVE_REQUEST_SERVICE_CONTEXTS_1.any));
                checkEqual(EMPTY.any, () -> current.get_slot(SERVER_SLOT_ID));
                // CORBA 3.0.3, section 21.4.4.6
                // this should not propagate to other interception points
                assertDoesNotThrow(() -> current.set_slot(SERVER_SLOT_ID, RECEIVE_REQUEST_SERVICE_CONTEXTS_1.any));
            } catch (RuntimeException e) {
                e.printStackTrace(System.out);
                throw e;
            }
        }

        @Override
        public void receive_request(ServerRequestInfo ri) throws ForwardRequest {
            if ("locationForward".equals(ri.operation())) throw new ForwardRequest(Util.getTie(forwardImpl).thisObject());
            try {
                System.out.printf("receive_request (%s) ...%n", ri.operation());
                assertThrows(InvalidSlot.class, () -> ri.get_slot(INVALID_SLOT_ID));
                assertThrows(InvalidSlot.class, () -> ri.set_slot(INVALID_SLOT_ID, RECEIVE_REQUEST_0.any));
                checkEqual(RECEIVE_REQUEST_SERVICE_CONTEXTS_0.any, () -> ri.get_slot(SERVER_SLOT_ID));
                assertDoesNotThrow(() -> ri.set_slot(SERVER_SLOT_ID, RECEIVE_REQUEST_0.any));

                final Current current = SERVER_THREAD_PICURRENT.get();
                assertThrows(InvalidSlot.class, () -> current.get_slot(INVALID_SLOT_ID));
                assertThrows(InvalidSlot.class, () -> current.set_slot(INVALID_SLOT_ID, RECEIVE_REQUEST_1.any));
                // TODO: checkEqual(EMPTY.any, () -> current.get_slot(SERVER_SLOT_ID));
                assertDoesNotThrow(() -> current.set_slot(SERVER_SLOT_ID, RECEIVE_REQUEST_1.any));
            } catch (RuntimeException e) {
                e.printStackTrace(System.out);
                throw e;
            }
        }

        @Override
        public void send_reply(ServerRequestInfo ri) {
            try {
                System.out.println("send_reply ...");
                assertThrows(InvalidSlot.class, () -> ri.get_slot(INVALID_SLOT_ID));
                assertThrows(InvalidSlot.class, () -> ri.set_slot(INVALID_SLOT_ID, SEND_REPLY_0.any));
                // TODO: checkEqual(RECEIVE_REQUEST_0.any, () -> ri.get_slot(SERVER_SLOT_ID));
                assertDoesNotThrow(() -> ri.set_slot(SERVER_SLOT_ID, SEND_REPLY_0.any));

                final Current current = SERVER_THREAD_PICURRENT.get();
                assertThrows(InvalidSlot.class, () -> current.get_slot(INVALID_SLOT_ID));
                assertThrows(InvalidSlot.class, () -> current.set_slot(INVALID_SLOT_ID, SEND_REPLY_1.any));
                // TODO: checkEqual(RECEIVE_REQUEST_1.any, () -> current.get_slot(SERVER_SLOT_ID));
                assertDoesNotThrow(() -> current.set_slot(SERVER_SLOT_ID, SEND_REPLY_1.any));
            } catch (RuntimeException e) {
                e.printStackTrace(System.out);
                throw e;
            }
        }

        @Override
        public void send_exception(ServerRequestInfo ri) {
            try {
                System.out.println("send_exception ...");
                assertThrows(InvalidSlot.class, () -> ri.get_slot(INVALID_SLOT_ID));
                assertThrows(InvalidSlot.class, () -> ri.set_slot(INVALID_SLOT_ID, SEND_EXCEPTION_0.any));
                // TODO: checkEqual(RECEIVE_REQUEST_0.any, () -> ri.get_slot(SERVER_SLOT_ID));
                assertDoesNotThrow(() -> ri.set_slot(SERVER_SLOT_ID, SEND_EXCEPTION_0.any));

                final Current current = SERVER_THREAD_PICURRENT.get();
                assertThrows(InvalidSlot.class, () -> current.get_slot(INVALID_SLOT_ID));
                assertThrows(InvalidSlot.class, () -> current.set_slot(INVALID_SLOT_ID, SEND_EXCEPTION_1.any));
                // TODO: checkEqual(RECEIVE_REQUEST_1.any, () -> current.get_slot(SERVER_SLOT_ID));
                assertDoesNotThrow(() -> current.set_slot(SERVER_SLOT_ID, SEND_EXCEPTION_1.any));
            } catch (RuntimeException e) {
                e.printStackTrace(System.out);
                throw e;
            }
        }

        @Override
        public void send_other(ServerRequestInfo ri) {
            try {
                System.out.println("send_other ...");
                assertThrows(InvalidSlot.class, () -> ri.get_slot(INVALID_SLOT_ID));
                assertThrows(InvalidSlot.class, () -> ri.set_slot(INVALID_SLOT_ID, SEND_OTHER_0.any));
                // TODO: checkEqual(RECEIVE_REQUEST_0.any, () -> ri.get_slot(SERVER_SLOT_ID));
                assertDoesNotThrow(() -> ri.set_slot(SERVER_SLOT_ID, SEND_OTHER_0.any));

                final Current current = SERVER_THREAD_PICURRENT.get();
                assertThrows(InvalidSlot.class, () -> current.get_slot(INVALID_SLOT_ID));
                assertThrows(InvalidSlot.class, () -> current.set_slot(INVALID_SLOT_ID, SEND_OTHER_1.any));
                // TODO: checkEqual(RECEIVE_REQUEST_1.any, () -> current.get_slot(SERVER_SLOT_ID));
                assertDoesNotThrow(() -> current.set_slot(SERVER_SLOT_ID, SEND_OTHER_1.any));
            } catch (RuntimeException e) {
                e.printStackTrace(System.out);
                throw e;
            }
        }
    }

    private static void checkEqual(Any expected, ThrowingSupplier<Any> actualSupplier) {
        final Any actual = assertDoesNotThrow(actualSupplier);
        assertNotNull(actual);
        if (actual.equal(expected)) return;
        fail(String.format("actual: %s does not match expected: %s", actual, expected));
    }

    public static final class MyException extends RuntimeException {}

    public interface MyRemote extends Remote {
        default void normal() throws RemoteException {
            System.out.println("MyRemote.normal()");
        }
        default void userException() throws RemoteException {
            System.out.println("MyRemote.userException()");
            throw new MyException();
        }
        default void systemException() throws RemoteException {
            System.out.println("MyRemote.systemException()");
            throw new UNKNOWN();
        }
        default void locationForward() throws RemoteException {
            System.out.println("MyRemote.locationForward()");
        }
        default void remoteException() throws RemoteException {
            System.out.println("MyRemote.remoteException");
            throw new RemoteException();
        }
    }
    public static class MyRemoteImpl extends PortableRemoteObject implements MyRemote {
        public static final MyRemote forwardImpl;

        static {
            MyRemote r = null;
            try {
                r = new MyRemoteImpl();
            } catch (RemoteException ignored) {
            } finally {
                forwardImpl = r;
            }
        }

        public MyRemoteImpl() throws RemoteException {}
    }


    @ClientStub(MyRemoteImpl.class)
    public static MyRemote stub;

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
        assertDoesNotThrow(stub::normal);
    }

    // TODO: drive send_poll

    @Test
    public void testUserException() {
        assertThrows(MyException.class, stub::userException);
    }

    @Test
    public void testRemoteException() { assertThrows(RemoteException.class, stub::remoteException); }

    @Test
    public void testSystemException() {
        assertThrows(SystemException.class, () -> { throw assertThrows(RemoteException.class, stub::systemException).getCause(); });
    }

    @Test
    public void testLocationForward() {
        assertThrows(NO_IMPLEMENT.class, () -> { throw assertThrows(RemoteException.class, stub::locationForward).getCause(); });
    }
}
