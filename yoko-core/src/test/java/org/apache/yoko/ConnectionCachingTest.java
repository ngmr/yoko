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
package org.apache.yoko;

import org.apache.yoko.orb.OBPortableServer.POA;
import org.apache.yoko.orb.OBPortableServer.POAHelper;
import org.apache.yoko.orb.OBPortableServer.POAManager;
import org.apache.yoko.orb.OBPortableServer.POAManagerHelper;
import org.apache.yoko.orb.OCI.Acceptor;
import org.apache.yoko.orb.OCI.IIOP.AcceptorInfo;
import org.apache.yoko.orb.OCI.IIOP.AcceptorInfoHelper;
import org.apache.yoko.orb.spi.naming.NameServiceInitializer;
import org.apache.yoko.orb.spi.naming.RemoteAccess;
import org.junit.Before;
import org.junit.Test;
import org.omg.CORBA.BAD_OPERATION;
import org.omg.CORBA.ORB;
import org.omg.CORBA.portable.OutputStream;
import org.omg.CORBA.portable.ResponseHandler;
import org.omg.CORBA_2_3.portable.InputStream;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContext;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.CosNaming.NamingContextHelper;
import org.omg.PortableInterceptor.ORBInitializer;
import testify.iiop.Skellington;
import testify.iiop.TestServerRequestInterceptor;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Properties;

import static javax.rmi.PortableRemoteObject.narrow;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class ConnectionCachingTest {
    private static final NameComponent[] OBJECT_NAME = { new NameComponent("object", "") };
    ORB serverORB;
    ORB clientORB;

    @Before
    public void setup() throws Exception {
        serverORB = Util.createServerOrb();
        clientORB = Util.createClientORB(serverORB);
        // make a GIOP 1.0 call first
        NamingContext ctx = NamingContextHelper.narrow(clientORB.string_to_object(Util.getNameServerUrl(serverORB)));
        ctx.new_context();
    }

    @Test
    public void testSingleNull() throws Exception {
        assertThat(newRemoteImpl(clientORB).bounce(null), nullValue());
    }

    @Test
    public void testSingleNullSameOrb() throws Exception {
        assertThat(newRemoteImpl(serverORB).bounce(null), nullValue());
    }

    @Test
    public void testSingleEmptyString() throws Exception {
        assertThat(newRemoteImpl(clientORB).bounce(""), is(""));
    }

    @Test
    public void testSingleEmptyStringSameOrb() throws Exception {
        assertThat(newRemoteImpl(serverORB).bounce(""), is(""));
    }

    @Test
    public void testSingleNonEmptyString() throws Exception {
        assertThat(newRemoteImpl(clientORB).bounce("hello"), is("hello"));
    }

    @Test
    public void testSingleNonEmptyStringSameOrb() throws Exception {
        assertThat(newRemoteImpl(serverORB).bounce("hello"), is("hello"));
    }

    @Test
    public void testLotsOfInvocations() throws Exception {
        assertThat(newRemoteImpl(clientORB).bounce(null), nullValue());
        assertThat(newRemoteImpl(clientORB).bounce(""), is(""));
        assertThat(newRemoteImpl(clientORB).bounce("a"), is("a"));
        assertThat(newRemoteImpl(clientORB).bounce("ab"), is("ab"));
        assertThat(newRemoteImpl(clientORB).bounce("abc"), is("abc"));
        assertThat(newRemoteImpl(clientORB).bounce("abcd"), is("abcd"));
        assertThat(newRemoteImpl(clientORB).bounce("abcde"), is("abcde"));
    }

    @Test
    public void testLotsOfInvocationsSameOrb() throws Exception {
        assertThat(newRemoteImpl(serverORB).bounce(null), nullValue());
        assertThat(newRemoteImpl(serverORB).bounce(""), is(""));
        assertThat(newRemoteImpl(serverORB).bounce("a"), is("a"));
        assertThat(newRemoteImpl(serverORB).bounce("ab"), is("ab"));
        assertThat(newRemoteImpl(serverORB).bounce("abc"), is("abc"));
        assertThat(newRemoteImpl(serverORB).bounce("abcd"), is("abcd"));
        assertThat(newRemoteImpl(serverORB).bounce("abcde"), is("abcde"));
    }

    private TheInterface newRemoteImpl(ORB callerOrb) throws Exception {
        TheImpl theImpl = new TheImpl();
        theImpl.publish(serverORB);
        // bind it into the naming context
        Util.getNameService(serverORB).rebind(OBJECT_NAME, theImpl.thisObject());
        // look it up from the caller orb
        Object stub = Util.getNameService(callerOrb).resolve(OBJECT_NAME);
        return (TheInterface)narrow(stub, TheInterface.class);
    }

    public interface TheInterface extends Remote {
        String bounce(String text) throws RemoteException;
    }

    private static class TheImpl extends Skellington implements TheInterface {
        @Override
        protected OutputStream dispatch(String method, InputStream in, ResponseHandler reply) throws RemoteException {
            if (!"bounce".equals(method)) throw new BAD_OPERATION();
            String result = bounce((String) in.read_value(String.class));
            OutputStream out = reply.createReply();
            ((org.omg.CORBA_2_3.portable.OutputStream) out).write_value(result, String.class);
            return out;
        }

        @Override
        public String bounce(String s) {return s;}
    }

    public static class DummyInterceptor implements TestServerRequestInterceptor {}


    private static class Util {

        private static int getPort(ORB orb) throws Exception {
            POA rootPoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            POAManager poaMgr = POAManagerHelper.narrow(rootPoa.the_POAManager());
            for (Acceptor acceptor : poaMgr.get_acceptors()) {
                AcceptorInfo info = AcceptorInfoHelper.narrow(acceptor.get_info());
                if (info != null) return (char) info.port();
            }
            throw new Error("No IIOP Acceptor found");
        }

        private static String getNameServerUrl(ORB orb) throws Exception {
            return "corbaname::localhost:" + getPort(orb);
        }

        private static ORB createServerOrb() throws Exception {
            Properties serverProps = new Properties();
            serverProps.put(NameServiceInitializer.NS_ORB_INIT_PROP, "");
            serverProps.put(NameServiceInitializer.NS_REMOTE_ACCESS_ARG, RemoteAccess.readWrite.toString());
            serverProps.put(ORBInitializer.class.getName() + "Class." + DummyInterceptor.class.getName(), "");
            ORB orb =  ORB.init((String[])null, serverProps);
            POAHelper.narrow(orb.resolve_initial_references("RootPOA")).the_POAManager().activate();
            return orb;
        }

        private static ORB createClientORB(ORB targetORB) throws Exception {
            return ORB.init(new String[]{"-ORBInitRef", "NameService=" + getNameServerUrl(targetORB)}, null);
        }

        private static NamingContextExt getNameService(ORB orb) throws Exception {
            return NamingContextExtHelper.narrow(orb.resolve_initial_references("NameService"));
        }
    }
}
