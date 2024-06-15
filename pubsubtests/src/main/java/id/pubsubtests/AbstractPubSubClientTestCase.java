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

import id.pubsubtests.data.ByteMessageFactory;
import id.pubsubtests.data.MessageFactory;
import java.util.function.Supplier;

/**
 * Describes test case configuration which is common across all test cases.
 *
 * @author lambdaprime intid@protonmail.com
 * @see <a
 *     href="https://junit.org/junit5/docs/current/user-guide/#writing-tests-parameterized-tests">Parameterized
 *     Tests</a>
 */
public abstract class AbstractPubSubClientTestCase {

    private Supplier<TestPubSubClient> clientFactory;
    private String testCaseName;
    private MessageFactory messageFactory;

    protected AbstractPubSubClientTestCase(
            String testCaseName, Supplier<TestPubSubClient> clientFactory) {
        this(testCaseName, clientFactory, new ByteMessageFactory());
    }

    protected AbstractPubSubClientTestCase(
            String testCaseName,
            Supplier<TestPubSubClient> clientFactory,
            MessageFactory messageFactory) {
        this.testCaseName = testCaseName;
        this.clientFactory = clientFactory;
        this.messageFactory = messageFactory;
    }

    /**
     * Client factory which produce new instance of same client implementation with same
     * configuration. This allows to test not only different {@link TestPubSubClient} client
     * implementations but also how they act with different configurations.
     *
     * <p>For example, each client which implements Publisher/Subscriber model ( {@link
     * TestPubSubClient}) may allow users to configure certain specific parameters (timeout, queue
     * size, ...). With {@link AbstractPubSubClientTestCase} it is possible to test different
     * combinations of these configurations (one instance of {@link AbstractPubSubClientTestCase}
     * can return factory for clients which has timeout = 10 and queue size = 1, another instance
     * may return factory for clients with timeout = 100 and queue size = 5, ...)
     */
    public Supplier<TestPubSubClient> clientFactory() {
        return clientFactory;
    }

    public MessageFactory messageFactory() {
        return messageFactory;
    }

    public String name() {
        return testCaseName;
    }

    @Override
    public String toString() {
        return testCaseName;
    }
}
