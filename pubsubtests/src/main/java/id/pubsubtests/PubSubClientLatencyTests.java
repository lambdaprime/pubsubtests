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
import id.xfunction.Preconditions;
import id.xfunction.concurrent.flow.SimpleSubscriber;
import id.xfunction.concurrent.flow.SynchronousPublisher;
import id.xfunction.lang.XThread;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test that time between when message was sent and received does not exceed given limit.
 *
 * <p>Required dataProvider method to be defined in subclasses:
 *
 * <pre>{@code
 * static Stream<PubSubClientLatencyTestCase> dataProvider() {
 *      return Stream.of(new PubSubClientLatencyTestCase(...));
 * }
 * }</pre>
 *
 * @see <b>pubsubtests</b> module documentation for more usage information
 * @author lambdaprime intid@protonmail.com
 */
@Nested
public abstract class PubSubClientLatencyTests {

    @AfterEach
    public void clean() {
        // clean up all unused objects
        // this allow users to monitor memory of each test separately
        // without any leftovers in the heap from the previous tests
        System.gc();
    }

    @ParameterizedTest
    @MethodSource("dataProvider")
    public void test_latency(PubSubClientLatencyTestCase testCase) throws Exception {
        try (var subscriberClient = testCase.clientFactory().get();
                var publisherClient = testCase.clientFactory().get();
                var publisher = new SynchronousPublisher<Message>()) {
            String topic = "testTopicLatency";
            var latencyList = new CopyOnWriteArrayList<Long>();
            var future =
                    new CompletableFuture<Void>()
                            .completeOnTimeout(
                                    null,
                                    testCase.getMaxTestDuration().toMillis(),
                                    TimeUnit.MILLISECONDS);
            var subscriber =
                    new SimpleSubscriber<Message>() {
                        private long previousTimestamp;

                        @Override
                        public void onNext(Message item) {
                            var timestamp = ByteBuffer.wrap(item.getBody()).getLong();
                            var now = System.currentTimeMillis();
                            var l = now - timestamp;
                            System.out.format(
                                    "published %s, received %s, latency %s ms\n",
                                    timestamp, now, l);
                            if (timestamp < previousTimestamp)
                                future.completeExceptionally(
                                        new AssertionError("Message out-of-order"));
                            previousTimestamp = timestamp;
                            latencyList.add(l);
                            if (future.isDone()) getSubscription().get().cancel();
                            else getSubscription().get().request(1);
                        }
                    };
            publisherClient.publish(topic, publisher);
            subscriberClient.subscribe(topic, subscriber);
            Preconditions.isTrue(
                    testCase.getMessageSizeInBytes() >= Long.BYTES,
                    "Message size is too small: %s",
                    testCase.getMessageSizeInBytes());
            var data = "a".repeat(testCase.getMessageSizeInBytes() - Long.BYTES - 1).getBytes();
            new Thread() {
                @Override
                public void run() {
                    XThread.sleep(testCase.getDiscoveryDuration().toMillis());
                    while (!future.isDone()) {
                        var buf = ByteBuffer.allocate(testCase.getMessageSizeInBytes());
                        long timestamp = System.currentTimeMillis();
                        buf.putLong(timestamp);
                        buf.put(data);
                        publisher.submit(testCase.messageFactory().create(buf.array()));
                        System.out.println("published " + timestamp);
                    }
                    System.out.println("Stop publishing");
                }
            }.start();

            // wait test to complete
            future.get();
            Assertions.assertEquals(false, latencyList.isEmpty(), "No messages received");
            System.out.println("Received number of messages: " + latencyList.size());
            var stat = latencyList.stream().mapToLong(Long::longValue).summaryStatistics();
            System.out.println("Max observed latency in ms is " + stat.getMax());
            System.out.println("Avg latency in ms is " + stat.getAverage());
            var exceededLatencyList =
                    latencyList.stream()
                            .filter(l -> l >= testCase.getExpectedMaxLatency().toMillis())
                            .toList();
            Assertions.assertEquals(
                    0,
                    exceededLatencyList.size(),
                    "Number of messages where latency exceeded is: " + exceededLatencyList.size());
            Assertions.assertEquals(
                    true,
                    testCase.getExpectedMinReceivedMessageCount() <= latencyList.size(),
                    "Expected min received message count %s, actual %s"
                            .formatted(
                                    testCase.getExpectedMinReceivedMessageCount(),
                                    latencyList.size()));
        }
    }
}
