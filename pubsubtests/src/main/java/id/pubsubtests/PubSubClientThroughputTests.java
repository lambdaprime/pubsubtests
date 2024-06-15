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
package id.pubsubtests;

import id.pubsubtests.data.Message;
import id.pubsubtests.data.RandomMessageGenerator;
import id.xfunction.concurrent.flow.SimpleSubscriber;
import id.xfunction.concurrent.flow.SynchronousPublisher;
import id.xfunction.lang.XThread;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Client requirements: reliable and deliver all messages in-order. Messages which were published
 * before Publisher and Subscriber discovered each other should be replayed to the Subscriber.
 *
 * <p>Each message contains unique randomly generated data.
 *
 * <p>Required dataProvider method to be defined in subclasses:
 *
 * <pre>{@code
 * static Stream<PubSubClientThroughputTestCase> dataProvider() {
 *      return Stream.of(new PubSubClientThroughputTestCase(...));
 * }
 * }</pre>
 *
 * @see <b>pubsubtests</b> module documentation for more usage information
 * @author lambdaprime intid@protonmail.com
 */
@Nested
public abstract class PubSubClientThroughputTests {

    private static class MySubscriber extends SimpleSubscriber<Message> {
        private CompletableFuture<Boolean> future;
        private RandomMessageGenerator dataGenerator;
        private Message expectedData;
        private int messageCount;
        private int maxPublishedMessages;
        private boolean isReplayable;

        public MySubscriber(
                CompletableFuture<Boolean> future,
                RandomMessageGenerator dataGenerator,
                boolean isReplayable,
                int maxPublishedMessages) {
            this.future = future;
            this.isReplayable = isReplayable;
            this.maxPublishedMessages = maxPublishedMessages;
            this.dataGenerator = dataGenerator;
            expectedData = dataGenerator.nextRandomMessage();
        }

        @Override
        public void onNext(Message item) {
            if (messageCount == 0 && !isReplayable) {
                while (!Objects.equals(item, expectedData)) {
                    dataGenerator.populateMessage(expectedData);
                }
            } else if (messageCount > 0) dataGenerator.populateMessage(expectedData);
            if (Objects.equals(item, expectedData)) {
                messageCount++;
                System.out.println("Received message" + messageCount);
                if (future.isDone() || messageCount == maxPublishedMessages) {
                    subscription.cancel();
                    future.complete(true);
                } else subscription.request(1);
            } else {
                System.out.println("Data mismatch after message " + messageCount);
                subscription.cancel();
                future.complete(false);
            }
        }

        public int getMessageCount() {
            return messageCount;
        }
    }

    @ParameterizedTest
    @MethodSource("dataProvider")
    public void test_throughput(PubSubClientThroughputTestCase testCase) throws Exception {
        try (var subscriberClient = testCase.clientFactory().get();
                var publisherClient = testCase.clientFactory().get();
                var publisher = new SynchronousPublisher<Message>()) {
            String topic = "testTopic1";
            var seed = System.currentTimeMillis();
            var startAt = Instant.now();
            var future =
                    new CompletableFuture<Boolean>()
                            .completeOnTimeout(
                                    true,
                                    testCase.getMaxTestDuration().toMillis(),
                                    TimeUnit.MILLISECONDS);
            var messageCount = testCase.getMaxCountOfPublishedMessages();
            var subscriber =
                    new MySubscriber(
                            future,
                            testCase.messageFactory()
                                    .createGenerator(seed, testCase.getMessageSizeInBytes()),
                            testCase.isReplayable(),
                            testCase.getMaxCountOfPublishedMessages());
            publisherClient.publish(topic, publisher);
            subscriberClient.subscribe(topic, subscriber);
            var dataGenerator =
                    testCase.messageFactory()
                            .createGenerator(seed, testCase.getMessageSizeInBytes());
            while (!future.isDone() && messageCount > 0) {
                messageCount--;
                var data = dataGenerator.nextRandomMessage();
                publisher.submit(data);
                System.out.println("Sent message");
                XThread.sleep(testCase.getPublishTimeout().toMillis());
            }
            System.out.println("Stop publishing");
            Assertions.assertEquals(true, future.get(), "Data mismatch on Subscriber occurred");
            System.out.println("Received number of messages: " + subscriber.getMessageCount());
            System.out.println(
                    "Test execution time took: " + Duration.between(startAt, Instant.now()));
            Assertions.assertEquals(
                    true,
                    testCase.getExpectedMinReceivedMessageCount() <= subscriber.getMessageCount(),
                    testCase.getExpectedMinReceivedMessageCount()
                            + " <= "
                            + subscriber.getMessageCount());
        }
    }
}
