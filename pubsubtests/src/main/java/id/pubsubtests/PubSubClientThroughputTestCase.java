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
 * Describe test case configuration for {@link PubSubClientThroughputTests}
 *
 * @author lambdaprime intid@protonmail.com
 * @see <a
 *     href="https://junit.org/junit5/docs/current/user-guide/#writing-tests-parameterized-tests">Parameterized
 *     Tests</a>
 */
public class PubSubClientThroughputTestCase extends PubSubClientTestCase {

    private Duration maxTestDuration;
    private int maxCountOfPublishedMessages;
    private int messageSizeInBytes;
    private int expectedReceivedMessageCount;
    private Duration publishTimeout;
    private String testCaseName;

    public PubSubClientThroughputTestCase(
            String testCaseName,
            Supplier<TestPubSubClient> clientFactory,
            Duration maxTestDuration,
            int messageSizeInBytes,
            int maxCountOfPublishedMessages,
            Duration publishTimeout,
            int expectedReceivedMessageCount) {
        super(clientFactory);
        this.testCaseName = testCaseName;
        this.maxTestDuration = maxTestDuration;
        this.maxCountOfPublishedMessages = maxCountOfPublishedMessages;
        this.messageSizeInBytes = messageSizeInBytes;
        this.publishTimeout = publishTimeout;
        this.expectedReceivedMessageCount = expectedReceivedMessageCount;
    }

    public Duration getMaxTestDuration() {
        return maxTestDuration;
    }

    public int getMaxCountOfPublishedMessages() {
        return maxCountOfPublishedMessages;
    }

    public int getMessageSizeInBytes() {
        return messageSizeInBytes;
    }

    public int getExpectedMinReceivedMessageCount() {
        return expectedReceivedMessageCount;
    }

    public Duration getPublishTimeout() {
        return publishTimeout;
    }

    @Override
    public String toString() {
        return testCaseName;
    }
}
