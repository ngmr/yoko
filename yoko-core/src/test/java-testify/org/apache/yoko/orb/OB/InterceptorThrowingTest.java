package org.apache.yoko.orb.OB;

import acme.idl.TestInterface;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.BAD_INV_ORDER;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.SystemException;
import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.ORBInitInfo;
import testify.iiop.TestClientRequestInterceptor;
import testify.jupiter.annotation.iiop.ConfigureOrb.UseWithOrb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

import static org.apache.yoko.orb.OB.InterceptorThrowingTest.InterceptorActions.DO_NOTHING;
import static org.apache.yoko.orb.OB.InterceptorThrowingTest.InterceptorActions.REGISTER;
import static org.apache.yoko.orb.OB.InterceptorThrowingTest.InterceptorActions.THROW_BAD_INV_ORDER;
import static org.apache.yoko.orb.OB.InterceptorThrowingTest.InterceptorActions.THROW_NO_PERMISSION;
import static org.apache.yoko.orb.OB.PortableInterceptorTest.StubType.TEST_INTERFACE;
import static org.apache.yoko.orb.OB.Util.unmarshalSystemException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InterceptorThrowingTest extends PortableInterceptorTest {
    static final List<MyClientInterceptor> INTERCEPTORS = new ArrayList<>();
    static final int NUM_INTERCEPTORS = 3;

    public enum InterceptorActions implements BiConsumer<MyClientInterceptor,ClientRequestInfo> {
        REGISTER((cri, info) -> { INTERCEPTORS.add(cri); }),
        DO_NOTHING((cri, info) -> {}),
        THROW_NO_PERMISSION((cri, info) -> { throw new NO_PERMISSION(); }),
        THROW_BAD_INV_ORDER((cri, info) -> { throw new BAD_INV_ORDER(); });

        private final BiConsumer<MyClientInterceptor,ClientRequestInfo> action;

        InterceptorActions(BiConsumer<MyClientInterceptor,ClientRequestInfo> action) { this.action = action; }

        @Override
        public void accept(MyClientInterceptor cri,ClientRequestInfo info) {
            action.accept(cri, info);
        }
    }

    @UseWithOrb("client orb")
    public static final class MyClientInterceptor implements TestClientRequestInterceptor {
        private final String name;

        BiConsumer<MyClientInterceptor,ClientRequestInfo> onRequest = REGISTER;
        BiConsumer<MyClientInterceptor,ClientRequestInfo> onReply = DO_NOTHING;
        BiConsumer<MyClientInterceptor,ClientRequestInfo> onException = DO_NOTHING;
        Class<? extends SystemException> expectedExceptionType;

        @Override
        public void post_init(ORBInitInfo info) {
            IntStream.range(0, NUM_INTERCEPTORS)
                    .mapToObj(MyClientInterceptor::new)
                    .forEach(cri -> assertDoesNotThrow(() -> info.add_client_request_interceptor(cri)));
        }

        /** Constructor needed for the ORB initializer */
        @SuppressWarnings("unused")
        public MyClientInterceptor() { this.name = null; }

        public MyClientInterceptor(int id) { this.name = TestClientRequestInterceptor.super.name() + "#" + id; }

        @Override
        public String name() { return name; }

        @Override
        public void send_request(ClientRequestInfo ri) {
            onRequest.accept(this, ri);
        }

        @Override
        public void receive_reply(ClientRequestInfo ri) {
            assertNull(expectedExceptionType);
            onReply.accept(this, ri);
        }

        @Override
        public void receive_exception(ClientRequestInfo ri) {
            assertNotNull(expectedExceptionType);
            final SystemException ex = unmarshalSystemException(ri.received_exception().create_input_stream());
            assertSame(expectedExceptionType, ex.getClass());
            onException.accept(this, ri);
        }
    }

    @Test
    void testTranslation() {
        final TestInterface ti = STUB_MAP.get(TEST_INTERFACE);
        assertDoesNotThrow(ti::noargs); // drive registration
        final MyClientInterceptor[] interceptors = INTERCEPTORS.toArray(new MyClientInterceptor[0]);
        assertEquals(NUM_INTERCEPTORS, interceptors.length);
        Arrays.stream(interceptors).forEach(i -> i.onRequest = DO_NOTHING);

        interceptors[0].onRequest = THROW_NO_PERMISSION;
        assertThrows(NO_PERMISSION.class, ti::noargs);

        interceptors[0].onRequest = DO_NOTHING;
        assertDoesNotThrow(ti::noargs);
        interceptors[0].onReply = THROW_NO_PERMISSION;
        assertThrows(NO_PERMISSION.class, ti::noargs);

        interceptors[0].onReply = DO_NOTHING;
        interceptors[1].onReply = THROW_NO_PERMISSION;
        interceptors[0].expectedExceptionType = NO_PERMISSION.class;
        assertThrows(NO_PERMISSION.class, ti::noargs);

        interceptors[1].onReply = DO_NOTHING;
        interceptors[0].expectedExceptionType = NO_PERMISSION.class;
        interceptors[1].expectedExceptionType = BAD_INV_ORDER.class;
        interceptors[1].onException = THROW_NO_PERMISSION;
        interceptors[2].onRequest = THROW_BAD_INV_ORDER;
        assertThrows(NO_PERMISSION.class, ti::noargs);

        interceptors[2].onRequest = DO_NOTHING;
        interceptors[2].onReply = THROW_BAD_INV_ORDER;
        assertThrows(NO_PERMISSION.class, ti::noargs);
    }
}
