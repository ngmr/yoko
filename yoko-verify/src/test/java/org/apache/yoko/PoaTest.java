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

import test.poa.TestDispatchStrategyClient;
import test.poa.TestDispatchStrategyServer;
import test.poa.TestMultipleOrbsClient;
import test.poa.TestMultipleOrbsServer;
import test.poa.TestMultipleOrbsThreadedClient;
import test.poa.TestMultipleOrbsThreadedServer;

public class PoaTest extends AbstractOrbTestBase {

    public void setUp() throws Exception {
        super.setUp();
        setWaitForFile("Test.ref");
    }

    public void testDispatchStrategyServer() throws Exception {
        runServerClientTest(TestDispatchStrategyServer.class, TestDispatchStrategyClient.class);
    }

    public void testMultipleOrbsServer() throws Exception {
        runServerClientTest(TestMultipleOrbsServer.class, TestMultipleOrbsClient.class);
    }

    public void testMultipleOrbsThreadedServer() throws Exception {
        runServerClientTest(TestMultipleOrbsThreadedServer.class, TestMultipleOrbsThreadedClient.class);
    }
}
