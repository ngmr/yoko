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

package test.poa;

//
// IDL:TestLocationForwardServer:1.0
//
/***/

public interface TestLocationForwardServerOperations
{
    //
    // IDL:TestLocationForwardServer/setForwardRequest:1.0
    //
    /***/

    void
    setForwardRequest(org.omg.CORBA.Object obj);

    //
    // IDL:TestLocationForwardServer/get_servant:1.0
    //
    /***/

    org.omg.CORBA.Object
    get_servant();

    //
    // IDL:TestLocationForwardServer/deactivate:1.0
    //
    /***/

    void
    deactivate();
}
