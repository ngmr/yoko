/*
 * Copyright 2023 IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an \"AS IS\" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.apache.yoko;

import org.apache.yoko.orb.OB.BootManagerHelper;
import org.apache.yoko.orb.OB.ObjectKey;
import org.apache.yoko.orb.OB.ObjectKeyData;
import org.apache.yoko.orb.OBPortableServer.POAManagerHelper;
import org.apache.yoko.orb.OCI.Acceptor;
import org.apache.yoko.orb.OCI.IIOP.AcceptorInfo;
import org.apache.yoko.orb.OCI.IIOP.AcceptorInfoHelper;
import org.apache.yoko.util.AssertionFailed;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.UserException;
import org.omg.PortableServer.IdAssignmentPolicyValue;
import org.omg.PortableServer.LifespanPolicyValue;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;
import test.ins.URLTest.IIOPAddress;
import test.ins.URLTest.IIOPAddressHelper;
import test.ins.URLTest.IIOPAddress_impl;
import testify.annotation.Logging;
import testify.bus.Bus;
import testify.iiop.annotation.ConfigureServer;
import testify.iiop.annotation.ConfigureServer.BeforeServer;

import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.fail;

// TODO: integrate more tightly with newer features of the testify framework.
@ConfigureServer
public class InsTest {
    private static String iorFromServer;
    // object reference to use during testing
    private static IIOPAddress iiopAddress;

    @BeforeServer
    public static void startServer(ORB orb, Bus bus) throws UserException {
        // corbaloc key
        bus.put("key", "TestINS");
        String keyStr = bus.get("key");

        POA rootPoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
        rootPoa.the_POAManager().activate();

        // Create POA
        Policy[] policies = new Policy[2];
        policies[0] = rootPoa.create_id_assignment_policy(IdAssignmentPolicyValue.USER_ID);
        policies[1] = rootPoa.create_lifespan_policy(LifespanPolicyValue.PERSISTENT);
        POA testPOA = rootPoa.create_POA("testPOA", rootPoa.the_POAManager(), policies);

        AcceptorInfo iiopInfo = getAcceptorInfo(rootPoa);

        String[] hosts = iiopInfo.hosts();
        int port = (char /*i.e. unsigned short*/) iiopInfo.port();

        // corbaloc test object
        IIOPAddress corbaURLObj;

        IIOPAddress_impl urlServant = new IIOPAddress_impl(orb, hosts[0], port, keyStr, "corbaloc");
        byte[] oid = urlServant.getKey().getBytes();

        testPOA.activate_object_with_id(oid, urlServant);
        corbaURLObj = urlServant._this(orb);

        // Also make the object available as a bootstrap object via corbaloc::localhost/TestINS
        BootManagerHelper.narrow(orb.resolve_initial_references("BootManager"))
                .add_binding(oid, corbaURLObj);

        // Save references. This must be done after POA manager
        // activation, otherwise there is a potential for a race
        // condition between the client sending request and the server
        // not being ready yet.
        bus.put("ior", orb.object_to_string(corbaURLObj));
    }

    private static AcceptorInfo getAcceptorInfo(POA poa) throws AdapterInactive {
        // Find the POA Manager's IIOP Acceptor Info
        return Stream.of(POAManagerHelper.narrow(poa.the_POAManager()).get_acceptors())
                .map(Acceptor::get_info)
                .map(AcceptorInfoHelper::narrow)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(Error::new);
    }

    @BeforeAll
    public static void setUp(ORB orb, Bus bus) {
        iorFromServer = bus.get("ior");
        iiopAddress = stringToIIOPAddress(orb, iorFromServer);
    }

    @AfterAll
    public static void tearDown() throws Exception {
        iiopAddress.deactivate();
    }

    // Convert a URL string to a test.ins.URLTest.IIOPAddress reference
    // raises a TestFailure exception if the reference is null or the
    // URL is invalid.
    private static IIOPAddress stringToIIOPAddress(ORB orb, String objStr) {
        org.omg.CORBA.Object obj = null;
        IIOPAddress nObj = null;

        try {
            obj = orb.string_to_object(objStr);
            assertThat(obj, notNullValue());
            nObj = IIOPAddressHelper.narrow(obj);
            assertThat(nObj, notNullValue());
            return nObj;
        } catch (SystemException se) {
            System.err.println("Error stringToIIOPAddress:");
            se.printStackTrace();
            System.err.println("Original URL: " + objStr);
            System.err.println("Resultant URL: " + (obj == null ? "NONE, string_to_object raised the exception" : orb.object_to_string(obj)));
            throw new AssertionFailed(se.getMessage());
        }
    }

    // Compare two IIOPAddress objects by querying them for
    // host, port and key information. This *does not* compare
    // the results of getString.
    private static void assertSimilar(IIOPAddress iop1, IIOPAddress iop2) {
        assertThat(iop1.getKey(), is(equalTo(iop2.getKey())));
        assertThat(iop1.getHost(), is(equalTo(iop2.getHost())));
        assertThat(iop1.getPort(), is(equalTo(iop2.getPort())));
    }

    //
    // Simplistic escape - escapes more than necessary but is ok
    // for testing
    //
    String escape(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 3);
        for (byte b : bytes) {
            result.append('%');
            result.append(Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16)));
            result.append(Character.toUpperCase(Character.forDigit(b & 0xF, 16)));
        }
        return result.toString();
    }

    // Construct the iiop address portion of a URL for an IIOPAddress.
    // "iiop:555objs.com:2809" for example.
    String getURLAddressComponent(IIOPAddress nObj, String prefix) {
        return (prefix == null ? ":" : prefix) + nObj.getHost() + ":" + ((int) (char) nObj.getPort());
    }

    @Test
    public void testStringToObject() {
        iiopAddress.getKey();
        iiopAddress.getHost();
        iiopAddress.getPort();
    }

    @Test
    public void testObjectToString(ORB orb) {
        assertSimilar(iiopAddress, stringToIIOPAddress(orb, orb.object_to_string(iiopAddress)));
    }

    @Test
    public void testNullUrlString(ORB orb) {
        try {
            orb.string_to_object(null);
            fail("Expected an exception");
        } catch (BAD_PARAM expected) {
        } catch (Exception e) {
            fail("Unexpected exception: " + e);
        }
    }

    @Test
    public void testCorbalocBadPorts(ORB orb) {
        // Try same URL with legal, and illegal low, high ports.
        int exceptionCount = 0;

        try {
            orb.string_to_object("corbaloc:iiop:localhost:2809/a/b/c");
        } catch (BAD_PARAM bp) {
            fail("legal URL must not raise exception");
        }
        Stream.of(
                "corbaloc:iiop:localhost:0/a/b/c",
                "corbaloc:iiop:localhost:65536/a/b/c",
                "corbaloc:iiop:localhost:-90/a/b/c",
                "corbaloc:iiop:localhost:not_a_number/a/b/c",
                "corbaloc:corbaloc-bad-scheme:localhost:not_a_number/a/b/c"
        ).forEach(s -> {
            try {
                orb.string_to_object(s);
                fail("Expected an exception");
            } catch (BAD_PARAM expected) {}
        });
    }

    @Test
    public void testDefaultVersion10CorbalocUrls(ORB orb) {
        String url = iiopAddress.getCorbalocURL();
        IIOPAddress nObj = stringToIIOPAddress(orb, url);
        assertSimilar(iiopAddress, nObj);
        assertThat(nObj.getString(), is(equalTo("corbaloc")));
    }

    @Test
    public void testOtherVersionsCorbalocUrls(ORB orb) {
        for (int iv = 0; iv < 10; ++iv) {
            String addr = getURLAddressComponent(iiopAddress, ":1." + iv + "@");
            String url = "corbaloc:" + addr + "/" + iiopAddress.getKey();

            IIOPAddress nObj = stringToIIOPAddress(orb, url);

            assertSimilar(iiopAddress, nObj);
        }
    }

    @Test
    public void testUnsupportedIiopVersionCorbalocUrls(ORB orb) {
        // Try iiop major versions 0 and 2
        Stream.of(":0.", ":2.").forEach(iiopMajor -> {
            IntStream.range(0, 10).forEach(iiopMinor -> {
                String addr = getURLAddressComponent(iiopAddress, iiopMajor + iiopMinor + "@");
                String url = "corbaloc:" + addr + "/" + iiopAddress.getKey();
                try {
                    orb.string_to_object(url);
                    fail("Incorrectly accepted '" + url + "'");
                } catch (BAD_PARAM ignored) {
                }
            });
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {":1.1@"/*, ":1.2@"*/}) // value source only applies to arg0, so string must be first param
    @Logging("org.apache.yoko.orb.OB")
    @Logging("yoko.verbose.data.in")
    public void testImrStyleCorbalocUrls(String iiopVersion, ORB orb) {
        // Create the ORBacus specific object key
        String iorKey = iiopAddress.getKey();

        byte[] oid = iorKey.getBytes();
        ObjectKeyData data = new ObjectKeyData();
        data.persistent = true;
        data.createTime = 0;
        data.oid = oid;
        data.serverId = "_RootPOA";
        data.poaId = new String[]{"testPOA"};

        byte[] oct = ObjectKey.CreateObjectKey(data);
        String keyStr = escape(oct);

        // Create a corbaloc URL for this
        String addr = getURLAddressComponent(iiopAddress, iiopVersion);
        String url = "corbaloc:" + addr + '/' + keyStr;

        IIOPAddress nObj = stringToIIOPAddress(orb, url);

        assertSimilar(iiopAddress, nObj);
        assertThat(nObj.getString(), is(equalTo("corbaloc")));
    }

    @Test
    public void testActiveProfile1InMultiProfileIor(ORB orb) {
        String url = "corbaloc:";
        String addr = getURLAddressComponent(iiopAddress, null);
        url += addr;

        //
        // add 2 dummy profiles at corbaloc reserved port
        //
        url += ",:localhost:2809";
        url += ",:localhost:2809";

        url += "/" + iiopAddress.getKey();
        IIOPAddress nObj = stringToIIOPAddress(orb, url);
        assertSimilar(iiopAddress, nObj);
        assertThat(nObj.getString(), is(equalTo("corbaloc")));
    }

//    @Test
//    @Disabled("was disabled before junit 5 port — runs in sequence but fails on its own")
    public void testActiveProfile3InMultiProfileIor(ORB orb) {
        String url = "corbaloc:";

        // 2 dummy profiles at corbaloc reserved port
        url += ":localhost:2809";
        url += ",:localhost:2809,";

        // usable profile
        String addr = getURLAddressComponent(iiopAddress, null);
        url += addr;

        url += "/" + iiopAddress.getKey();

        IIOPAddress nObj = stringToIIOPAddress(orb, url);

        assertSimilar(iiopAddress, nObj);
    }
}
