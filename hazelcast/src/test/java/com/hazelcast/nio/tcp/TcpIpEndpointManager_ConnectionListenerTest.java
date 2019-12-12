/*
 * Copyright (c) 2008-2019, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.nio.tcp;

import com.hazelcast.instance.EndpointQualifier;
import com.hazelcast.nio.Connection;
import com.hazelcast.nio.ConnectionListener;
import com.hazelcast.test.AssertTask;
import com.hazelcast.test.HazelcastSerialClassRunner;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static com.hazelcast.instance.EndpointQualifier.MEMBER;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

@RunWith(HazelcastSerialClassRunner.class)
@Category(QuickTest.class)
public class TcpIpEndpointManager_ConnectionListenerTest
        extends TcpIpConnection_AbstractTest {

    @Test(expected = NullPointerException.class)
    public void addConnectionListener_whenNull() {
        networkingServiceA.getEndpointManager(MEMBER).addConnectionListener(null);
    }

    @Test
    public void whenConnectionAdded() {
        startAllNetworkingServices();

        final ConnectionListener listener = mock(ConnectionListener.class);
        networkingServiceA.getEndpointManager(MEMBER).addConnectionListener(listener);

        final Connection c = connect(networkingServiceA, addressB);

        assertTrueEventually(new AssertTask() {
            @Override
            public void run() throws Exception {
                listener.connectionAdded(c);
            }
        });
    }

    @Test
    public void whenConnectionDestroyed() {
        startAllNetworkingServices();


        final ConnectionListener listener = mock(ConnectionListener.class);
        networkingServiceA.getEndpointManager(MEMBER).addConnectionListener(listener);

        final Connection c = connect(networkingServiceA, addressB);
        c.close(null, null);

        assertTrueEventually(new AssertTask() {
            @Override
            public void run() throws Exception {
                listener.connectionRemoved(c);
            }
        });
    }

    @Test
    public void whenConnectionManagerShutdown_thenListenersRemoved() {
        startAllNetworkingServices();

        ConnectionListener listener = mock(ConnectionListener.class);
        networkingServiceA.getEndpointManager(MEMBER).addConnectionListener(listener);

        networkingServiceA.shutdown();

        final MemberViewUnifiedEndpointManager endpointManager = (MemberViewUnifiedEndpointManager) networkingServiceA
                .getEndpointManager(EndpointQualifier.MEMBER);

        assertEquals(0, endpointManager.getConnectionListenersCount());
    }
}