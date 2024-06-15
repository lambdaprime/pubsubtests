/*
 * Copyright 2022 pubsubtests project
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
/**
 * Generic tests for clients with Publisher/Subscriber model.
 *
 * <ul>
 *   <li>Publisher/Subscriber basic functionality tests
 *   <li>Performance tests (latency, throughput)
 *   <li>Testing of non-Java based clients
 *   <li>Testing how clients from different vendors interact with each other
 * </ul>
 *
 * <p>Same Publisher/Subscriber protocol can be implemented by different vendors. This result in
 * different client implementations. Ideally if all such clients implement same Publisher/Subscriber
 * protocol correctly then they should be able to interact with each other:
 *
 * <ul>
 *   <li>when both subscriber and publisher are using same client implementation from the same
 *       vendor
 *   <li>when subscriber is using client implementation from VendorA and publisher is using client
 *       implementation from Vendor2 (and vice-versa).
 * </ul>
 *
 * <p><b>pubsubtests</b> allows to test all such combinations.
 *
 * <p>To run tests against particular client implementation users need to implement {@link
 * id.pubsubtests.TestPubSubClient}.
 *
 * <p>By default <b>pubsubtests</b> performs tests against single {@link
 * id.pubsubtests.TestPubSubClient} client implementation. They validate that Publisher and
 * Subscriber which are using same implementation of {@link id.pubsubtests.TestPubSubClient} can
 * interact with each other.
 *
 * <p>To test how different {@link id.pubsubtests.TestPubSubClient} client implementations interact
 * with each other users need to use {@link id.pubsubtests.CompositePubSubClient}
 *
 * <p>The interaction between Publisher/Subscriber happens through {@link
 * id.pubsubtests.data.Message} and they all created with {@link
 * id.pubsubtests.data.MessageFactory}. Some tests allow users to specify their own {@link
 * id.pubsubtests.data.MessageFactory}. This allows users to redefine messages behavior: equals, how
 * they are created and generated etc.
 *
 * <p>To use tests:
 *
 * <ul>
 *   <li>Using client which is going to be tested implement {@link id.pubsubtests.TestPubSubClient}
 *   <li>Create a JUnit test class for the client and let it extend any of the available test
 *       classes: {@link id.pubsubtests.PubSubClientTests}, {@link
 *       id.pubsubtests.PubSubClientThroughputTests}
 *   <li>Define dataProvider method (see extended test class documentation for details)
 * </ul>
 *
 * <p>If some of the tests from any extended test classes are irrelevant to the client which is
 * being tested then their methods can be overridden in the actual test class itself.
 *
 * <h2>Testing of non-Java based clients</h2>
 *
 * <p>It is possible by implementing {@link id.pubsubtests.TestPubSubClient} and forwarding all
 * calls to the external client (through JNI, FFM or stdin/stdout)
 *
 * @author lambdaprime intid@protonmail.com
 */
open module pubsubtests {
    exports id.pubsubtests;
    exports id.pubsubtests.data;

    requires id.xfunction;
    requires org.junit.jupiter.api;
    requires org.junit.jupiter.params;
}
