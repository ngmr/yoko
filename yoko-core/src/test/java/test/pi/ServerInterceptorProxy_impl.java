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

package test.pi;

import org.omg.CORBA.LocalObject;
import org.omg.PortableInterceptor.ForwardRequest;
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.omg.PortableInterceptor.ServerRequestInterceptor;
import test.common.TestException;

import java.util.concurrent.atomic.AtomicInteger;

final class ServerInterceptorProxy_impl extends LocalObject
        implements ServerRequestInterceptor, AutoCloseable {
    private static final AtomicInteger nextId = new AtomicInteger();
    private final String name = String.format("%s#%04x", this.getClass().getName(), nextId.getAndIncrement());

    public static void TEST(boolean expr) {
        if (!expr) throw new TestException();
    }

    private ServerRequestInterceptor interceptor;

    private final AtomicInteger activeCalls = new AtomicInteger();

    public void close() {
        TEST(0 == activeCalls.get());
    }

    // IDL to Java Mapping

    public String name() {
        return name;
    }

    public void destroy() {
    }

    public void receive_request_service_contexts(ServerRequestInfo ri)
            throws ForwardRequest {
        TEST(0 <= activeCalls.getAndIncrement());
        if (null == interceptor) return;
        interceptor.receive_request_service_contexts(ri);
    }

    public void receive_request(ServerRequestInfo ri) throws ForwardRequest {
        TEST(0 < activeCalls.get());
        if (null == interceptor) return;
        interceptor.receive_request(ri);
    }

    public void send_reply(ServerRequestInfo ri) {
        TEST(0 < activeCalls.getAndDecrement());
        if (null == interceptor) return;
        interceptor.send_reply(ri);
    }

    public void send_other(ServerRequestInfo ri) throws ForwardRequest {
        TEST(0 < activeCalls.getAndDecrement());
        if (null == interceptor) return;
        interceptor.send_other(ri);
    }

    public void send_exception(ServerRequestInfo ri) throws ForwardRequest {
        TEST(0 < activeCalls.getAndDecrement());
        if (null == interceptor) return;
        interceptor.send_exception(ri);
    }

    void changeInterceptor(ServerRequestInterceptor interceptor) {
        this.interceptor = interceptor;
    }
}
