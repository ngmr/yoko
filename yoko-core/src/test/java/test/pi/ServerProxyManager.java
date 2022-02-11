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

import org.omg.PortableInterceptor.ORBInitInfo;
import org.omg.PortableInterceptor.ORBInitInfoPackage.DuplicateName;
import org.omg.PortableInterceptor.ServerRequestInterceptor;

import java.util.List;

import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.generate;

final class ServerProxyManager implements AutoCloseable {
    private final List<ServerInterceptorProxy_impl> interceptorProxies =
            unmodifiableList(generate(ServerInterceptorProxy_impl::new)
                    .limit(3)
                    .collect(toList()));

    ServerProxyManager(ORBInitInfo info) {
        try {
            for (ServerRequestInterceptor sri : interceptorProxies) info.add_server_request_interceptor(sri);
        } catch (DuplicateName e) {
            throw new RuntimeException(e);
        }
    }

    void setInterceptor(int which, ServerRequestInterceptor sri) {
        interceptorProxies.get(which).changeInterceptor(sri);
    }

    public void close() {
        for (ServerInterceptorProxy_impl p: interceptorProxies) {
            // Drive try-with-resources cleanup
            //noinspection EmptyTryBlock
            try(ServerInterceptorProxy_impl ignored = p) {}
        }
    }
}
