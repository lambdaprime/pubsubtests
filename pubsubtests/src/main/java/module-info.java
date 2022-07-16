/**
 * Generic tests for clients with Publisher/Subscriber model
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
