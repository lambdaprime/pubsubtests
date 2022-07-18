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
 * Generic tests for clients with Publisher/Subscriber model. Tests are performed against the client
 * itself. They validate that Publisher and Subscriber can interact with each other.
 *
 * <p>To use tests:
 *
 * <ul>
 *   <li>Using client which is going to be tested implement {@link id.pubsubtests.TestPubSubClient}
 *   <li>Create a JUnit test class for the client and let it extend {@link
 *       id.pubsubtests.PubSubClientTests}
 *   <li>Define dataProvider method:
 *       <pre>{@code
 * static Stream<TestCase> dataProvider() {
 *      return Stream.of(new TestCase(RtpsTalkTestPubSubClient::new));
 * }
 * }</pre>
 * </ul>
 *
 * <p>If some of the tests from {@link id.pubsubtests.PubSubClientTests} are irrelevant to the
 * client which is being tested then their methods can be overridden in the test class.
 *
 * @author lambdaprime intid@protonmail.com
 */
open module pubsubtests {
    exports id.pubsubtests;

    requires id.xfunction;
    requires org.junit.jupiter.api;
    requires org.junit.jupiter.params;
}
