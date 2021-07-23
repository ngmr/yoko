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
package testify.jupiter.annotation.logging;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import testify.jupiter.annotation.Summoner;

import java.util.List;

import static testify.jupiter.annotation.logging.TestLogger.getLogFinisher;

class LoggingExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {
    private static final Summoner<List<Logging>, TestLogger> SUMMONER = Summoner.forRepeatableAnnotation(Logging.class, TestLogger.class, TestLogger::new);

    public void beforeTestExecution(ExtensionContext ctx) { SUMMONER.forContext(ctx).requestSteward(); }

    public void afterTestExecution(ExtensionContext ctx) { SUMMONER.forContext(ctx).requestSteward().ifPresent(getLogFinisher(ctx)); }
}
