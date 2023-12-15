/*
 * Copyright 2023 pubsubtests project
 * 
 * Website: https://github.com/lambdaprime/pubsubtests
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package id.pubsubtests;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Describe test case configuration for {@link PubSubClientLatencyTests}
 *
 * @author lambdaprime intid@protonmail.com
 * @see <a
 *     href="https://junit.org/junit5/docs/current/user-guide/#writing-tests-parameterized-tests">Parameterized
 *     Tests</a>
 */
public class PubSubClientLatencyTestCase extends AbstractPubSubClientTestCase {

    private Duration maxTestDuration;
    private int messageSizeInBytes;
    private Duration expectedMaxLatency;
    private Duration discoveryDuration;
    private int expectedMinReceivedMessageCount;

    public PubSubClientLatencyTestCase(
            String testCaseName,
            Supplier<TestPubSubClient> clientFactory,
            Duration discoveryDuration,
            Duration maxTestDuration,
            int messageSizeInBytes,
            Duration expectedMaxLatency,
            int expectedMinReceivedMessageCount) {
        super(testCaseName, clientFactory);
        this.discoveryDuration = discoveryDuration;
        this.maxTestDuration = maxTestDuration;
        this.messageSizeInBytes = messageSizeInBytes;
        this.expectedMaxLatency = expectedMaxLatency;
        this.expectedMinReceivedMessageCount = expectedMinReceivedMessageCount;
    }

    /**
     * How much time to wait before start publishing first message. Discovery time it is time needed
     * for Publisher and Subscriber to discover each other.
     */
    public Duration getDiscoveryDuration() {
        return discoveryDuration;
    }

    public Duration getMaxTestDuration() {
        return maxTestDuration;
    }

    public int getMessageSizeInBytes() {
        return messageSizeInBytes;
    }

    public Duration getExpectedMaxLatency() {
        return expectedMaxLatency;
    }

    public int getExpectedMinReceivedMessageCount() {
        return expectedMinReceivedMessageCount;
    }
}
