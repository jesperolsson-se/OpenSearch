/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.action.support;

import org.opensearch.OpenSearchTimeoutException;
import org.opensearch.action.ActionListener;
import org.opensearch.cluster.coordination.DeterministicTaskQueue;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.threadpool.ThreadPool;
import org.junit.Before;
import org.opensearch.action.support.ListenerTimeouts;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.opensearch.node.Node.NODE_NAME_SETTING;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

public class ListenerTimeoutsTests extends OpenSearchTestCase {

    private final TimeValue timeout = TimeValue.timeValueMillis(10);
    private final String generic = ThreadPool.Names.GENERIC;
    private DeterministicTaskQueue taskQueue;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Settings settings = Settings.builder().put(NODE_NAME_SETTING.getKey(), "node").build();
        taskQueue = new DeterministicTaskQueue(settings, random());
    }

    public void testListenerTimeout() {
        AtomicBoolean success = new AtomicBoolean(false);
        AtomicReference<Exception> exception = new AtomicReference<>();
        ActionListener<Void> listener = wrap(success, exception);

        ActionListener<Void> wrapped = ListenerTimeouts.wrapWithTimeout(taskQueue.getThreadPool(), listener, timeout, generic, "test");
        assertTrue(taskQueue.hasDeferredTasks());
        taskQueue.advanceTime();
        taskQueue.runAllRunnableTasks();

        wrapped.onResponse(null);
        wrapped.onFailure(new IOException("incorrect exception"));

        assertFalse(success.get());
        assertThat(exception.get(), instanceOf(OpenSearchTimeoutException.class));
    }

    public void testFinishNormallyBeforeTimeout() {
        AtomicBoolean success = new AtomicBoolean(false);
        AtomicReference<Exception> exception = new AtomicReference<>();
        ActionListener<Void> listener = wrap(success, exception);

        ActionListener<Void> wrapped = ListenerTimeouts.wrapWithTimeout(taskQueue.getThreadPool(), listener, timeout, generic, "test");
        wrapped.onResponse(null);
        wrapped.onFailure(new IOException("boom"));
        wrapped.onResponse(null);

        assertTrue(taskQueue.hasDeferredTasks());
        taskQueue.advanceTime();
        taskQueue.runAllRunnableTasks();

        assertTrue(success.get());
        assertNull(exception.get());
    }

    public void testFinishExceptionallyBeforeTimeout() {
        AtomicBoolean success = new AtomicBoolean(false);
        AtomicReference<Exception> exception = new AtomicReference<>();
        ActionListener<Void> listener = wrap(success, exception);

        ActionListener<Void> wrapped = ListenerTimeouts.wrapWithTimeout(taskQueue.getThreadPool(), listener, timeout, generic, "test");
        wrapped.onFailure(new IOException("boom"));

        assertTrue(taskQueue.hasDeferredTasks());
        taskQueue.advanceTime();
        taskQueue.runAllRunnableTasks();

        assertFalse(success.get());
        assertThat(exception.get(), instanceOf(IOException.class));
    }

    private ActionListener<Void> wrap(AtomicBoolean success, AtomicReference<Exception> exception) {
        return new ActionListener<Void>() {

            private final AtomicBoolean completed = new AtomicBoolean();

            @Override
            public void onResponse(Void aVoid) {
                assertTrue(completed.compareAndSet(false, true));
                assertTrue(success.compareAndSet(false, true));
            }

            @Override
            public void onFailure(Exception e) {
                assertTrue(completed.compareAndSet(false, true));
                assertTrue(exception.compareAndSet(null, e));
            }
        };
    }
}
