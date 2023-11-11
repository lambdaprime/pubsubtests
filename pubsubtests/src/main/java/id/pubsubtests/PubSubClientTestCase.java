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
 * Describe test case configuration for run of all tests inside {@link PubSubClientTests}
 *
 * @author lambdaprime intid@protonmail.com
 * @see <a
 *     href="https://junit.org/junit5/docs/current/user-guide/#writing-tests-parameterized-tests">Parameterized
 *     Tests</a>
 */
public record PubSubClientTestCase(
        Supplier<TestPubSubClient> clientFactory,
        Duration test_publish_multiple_60kb_messages_expected_timeout,
        Duration test_publish_single_message_over_5mb_expected_timeout,
        int test_throutput_expected_message_count) {

    /**
     * Client factory which produce new instance of same client implementation with same
     * configuration. This allows to test not only different {@link TestPubSubClient} client
     * implementations but also how they act with different configurations.
     *
     * <p>For example, each client which implements Publisher/Subscriber model ( {@link
     * TestPubSubClient}) may allow users to configure certain specific parameters (timeout, queue
     * size, ...). With {@link PubSubClientTestCase} it is possible to test different combinations
     * of these configurations (one instance of {@link PubSubClientTestCase} can return factory for
     * clients which has timeout = 10 and queue size = 1, another instance may return factory for
     * clients with timeout = 100 and queue size = 5, ...)
     */
    public Supplier<TestPubSubClient> clientFactory() {
        return clientFactory;
    }
}
