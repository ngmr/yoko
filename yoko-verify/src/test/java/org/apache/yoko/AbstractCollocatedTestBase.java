/*
 * Copyright 2019 IBM Corporation and others.
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

import junit.framework.TestSuite;

/** Test client and server using the same ORB */
public final class AbstractCollocatedTestBase extends AbstractOrbTestBase {
    private Class<?> mainClass;
    private String[] args;
    public AbstractCollocatedTestBase(String testName) {
        super(testName);
    }
    public void testCollocated() throws Exception {
        client.invokeMain(mainClass, args);
    }
        
    public static TestSuite generateTestSuite(Class<?> mainClass, String[][] args) {
        TestSuite suite = new TestSuite();
        for (String[] arg : args) {
            AbstractCollocatedTestBase test = new AbstractCollocatedTestBase("testCollocated");
            test.mainClass = mainClass;
            test.args = arg;
            suite.addTest(test);
        }
        return suite;
    }
}
