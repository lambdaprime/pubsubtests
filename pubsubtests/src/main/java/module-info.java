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
 * @author lambdaprime intid@protonmail.com
 */
open module pubsubtests {
    exports id.pubsubtests;
    exports id.pubsubtests.data;

    requires id.xfunction;
    requires org.junit.jupiter.api;
    requires org.junit.jupiter.params;
}
