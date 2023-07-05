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
package testify.annotation.logging;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import testify.annotation.Logging;
import testify.annotation.impl.PartRunnerSteward;
import testify.annotation.impl.SimpleParameterResolver;
import testify.parts.PartRunner;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.platform.commons.support.AnnotationSupport.findRepeatableAnnotations;
import static testify.parts.PartRunner.State.CONFIGURING;

/**
 * Log each test and print out the log messages
 * as directed by the annotation for that test.
 */
public final class LoggingExtension implements CloseableResource, BeforeAllCallback, BeforeTestExecutionCallback, AfterTestExecutionCallback, AfterAllCallback, TestExecutionExceptionHandler, SimpleParameterResolver<LogPublisher> {
    private LogPublisher logPublisher;

    public void beforeAll(ExtensionContext ctx) {
        PartRunner runner = PartRunnerSteward.getPartRunner(ctx);
        assertThat(runner.getState(), is(CONFIGURING));
        logPublisher = LogPublisher.create(runner);
        LogRecorder.create(runner.bus("junit"));
        startLogging(ctx);
        ctx.getStore(Namespace.create(this)).put(this, this); // so that the namespace will call close() during cleanup
        runner.addJVMStartupHook(LogRecorder::create);
    }
    public void beforeTestExecution(ExtensionContext ctx) { startLogging(ctx); }
    public void afterTestExecution(ExtensionContext ctx) { endLogging(ctx); }
    public void afterAll(ExtensionContext ctx) { endLogging(ctx); }
    public void handleTestExecutionException(ExtensionContext ctx, Throwable throwable) throws Throwable {
        logPublisher.somethingWentWrong(throwable);
        throw throwable; // rethrow or tests won't fail
    }
    public void close() throws Throwable {
        try (AutoCloseable toBeClosed = logPublisher){}
    }

    private void startLogging(ExtensionContext ctx) {
        List<LogSetting> settings = findRepeatableAnnotations(ctx.getElement(), Logging.class)
                .stream()
                .map(LogSetting::new)
                .collect(Collectors.toList());
        logPublisher.pushSettings(settings);
        logPublisher.flushLogs("BEFORE: " + ctx.getDisplayName());
    }

    private void endLogging(ExtensionContext ctx) {
        logPublisher.flushLogs("AFTER: " + ctx.getDisplayName());
        logPublisher.popSettings();
    }

    @Override
    public Class<LogPublisher> getSupportedParameterType() { return LogPublisher.class; }
    @Override
    public LogPublisher resolveParameter(ExtensionContext ctx) { return logPublisher; }
}
