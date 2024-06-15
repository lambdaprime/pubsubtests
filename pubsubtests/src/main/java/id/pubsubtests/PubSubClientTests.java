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
import id.xfunction.concurrent.flow.CollectorSubscriber;
import id.xfunction.concurrent.flow.FixedCollectorSubscriber;
import id.xfunction.concurrent.flow.SynchronousPublisher;
import id.xfunction.concurrent.flow.TransformSubscriber;
import id.xfunction.lang.XThread;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Required dataProvider method to be defined in subclasses:
 *
 * <pre>{@code
 * static Stream<PubSubClientTestCase> dataProvider() {
 *      return Stream.of(new PubSubClientTestCase(...));
 * }
 * }</pre>
 *
 * @see <b>pubsubtests</b> module documentation for more usage information
 * @author lambdaprime intid@protonmail.com
 */
@Nested
public abstract class PubSubClientTests {

    /**
     * Test that publisher does not drop messages when there is no subscribers and once its internal
     * queue will be full, it eventually block to accept new ones.
     */
    @ParameterizedTest
    @MethodSource("dataProvider")
    public void test_publish_when_no_subscribers(PubSubClientTestCase testCase) {
        try (var publisherClient = testCase.clientFactory().get(); ) {
            String topic = "testTopic1";
            var publisher = new SubmissionPublisher<Message>(ForkJoinPool.commonPool(), 1);
            var data = testCase.messageFactory().create("hello");
            publisherClient.publish(topic, publisher);
            var queueSize = testCase.getPublisherQueueSize() + publisher.getMaxBufferCapacity();
            // if messages are not dropped then publisher will eventually stop accepting messages
            // method offer returns false when message cannot be added
            while (publisher.offer(data, 10, TimeUnit.MILLISECONDS, null) >= 0) queueSize--;
            // expect no messages are lost
            Assertions.assertEquals(0, queueSize);
        }
    }

    @ParameterizedTest
    @MethodSource("dataProvider")
    public void test_multiple_subscribers_same_topic(PubSubClientTestCase testCase)
            throws Exception {
        var maxNumOfMessages = 15;
        // User owned Publishers are represented by Flow.Publisher interface and it has no close
        // method
        try (var subscriberClient = testCase.clientFactory().get();
                var publisherClient = testCase.clientFactory().get(); ) {
            String topic = "testTopic1";
            var publisher = new SubmissionPublisher<Message>();
            publisherClient.publish(topic, publisher);
            var subscribers =
                    Stream.generate(
                                    () ->
                                            new FixedCollectorSubscriber<>(
                                                    new ArrayList<Message>(), maxNumOfMessages))
                            .limit(3)
                            .toList();
            subscribers.forEach(sub -> subscriberClient.subscribe(topic, sub));
            var expected = new ArrayList<>();
            ForkJoinPool.commonPool()
                    .execute(
                            () -> {
                                int c = 0;
                                while (expected.size() < maxNumOfMessages) {
                                    var data = "hello " + c++;
                                    expected.add(data);
                                    publisher.submit(testCase.messageFactory().create(data));
                                }
                                publisher.close();
                            });
            for (var sub : subscribers) {
                var data =
                        sub.getFuture().get().stream()
                                .map(Message::getBody)
                                .map(String::new)
                                .toList();
                Assertions.assertEquals(expected, data);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("dataProvider")
    public void test_publish_forever(PubSubClientTestCase testCase) throws Exception {
        // User owned Publishers are represented by Flow.Publisher interface and it has no close
        // method
        try (var publisherClient = testCase.clientFactory().get();
                var subscriberClient = testCase.clientFactory().get(); ) {
            String topic = "testTopic1";
            String data = "hello";
            var publisher = new SubmissionPublisher<Message>();
            publisherClient.publish(topic, publisher);
            var collector = new FixedCollectorSubscriber<>(new ArrayList<Message>(), 1);
            subscriberClient.subscribe(topic, collector);
            ForkJoinPool.commonPool()
                    .execute(
                            () -> {
                                while (!collector.getFuture().isDone()) {
                                    publisher.submit(testCase.messageFactory().create(data));
                                }
                                publisher.close();
                            });
            Assertions.assertEquals(
                    data,
                    collector.getFuture().get().stream()
                            .map(Message::getBody)
                            .map(String::new)
                            .findFirst()
                            .get());
        }
    }

    @ParameterizedTest
    @MethodSource("dataProvider")
    public void test_publish_order(PubSubClientTestCase testCase) throws Exception {
        try (var subscriberClient = testCase.clientFactory().get();
                var publisherClient = testCase.clientFactory().get(); ) {
            String topic = "/testTopic1";
            var publisher = new SynchronousPublisher<Message>();
            publisherClient.publish(topic, publisher);
            var collector = new FixedCollectorSubscriber<>(new ArrayList<Message>(), 50);
            subscriberClient.subscribe(topic, collector);
            ForkJoinPool.commonPool()
                    .execute(
                            () -> {
                                int c = 0;
                                while (!collector.getFuture().isDone()) {
                                    var msg = "" + c++;
                                    System.out.println("          " + msg);
                                    publisher.submit(testCase.messageFactory().create(msg));
                                }
                            });
            var received =
                    collector.getFuture().get().stream()
                            .map(Message::getBody)
                            .map(String::new)
                            .mapToInt(Integer::parseInt)
                            .toArray();
            var start = received[0];
            for (int i = 0; i < received.length; i++) {
                Assertions.assertEquals(start + i, received[i]);
            }
        }
    }

    /**
     * Test discovery time should not take more than N seconds. Discovery time it is time after both
     * Publisher and Subscriber were registered and before first message can be received by the
     * Subscriber
     */
    @ParameterizedTest
    @MethodSource("dataProvider")
    public void test_discovery_time(PubSubClientTestCase testCase) throws Exception {
        try (var subscriberClient = testCase.clientFactory().get();
                var publisherClient = testCase.clientFactory().get(); ) {
            String topic = "testTopic1";
            var publisher = new SubmissionPublisher<Message>();
            String data = "hello";
            publisherClient.publish(topic, publisher);
            var collector = new FixedCollectorSubscriber<>(new ArrayList<Message>(), 1);
            subscriberClient.subscribe(topic, collector);
            XThread.sleep(testCase.getDiscoveryDuration().toMillis());
            publisher.submit(testCase.messageFactory().create(data));
            Assertions.assertEquals(
                    data,
                    collector.getFuture().get().stream()
                            .map(Message::getBody)
                            .map(String::new)
                            .findFirst()
                            .get());
        }
    }

    @ParameterizedTest
    @MethodSource("dataProvider")
    public void test_multiple_publishers(PubSubClientTestCase testCase) throws Exception {
        String topic = "/testTopic1";
        try (var subscriberClient = testCase.clientFactory().get();
                var publisherClient1 = testCase.clientFactory().get();
                var publisherClient2 = testCase.clientFactory().get();
                var publisher1 = new SubmissionPublisher<Message>();
                var publisher2 = new SubmissionPublisher<Message>()) {
            publisherClient1.publish(topic, publisher1);
            publisherClient2.publish(topic, publisher2);
            var collector = new FixedCollectorSubscriber<>(new HashSet<String>(), 2);
            var trans =
                    new TransformSubscriber<Message, String>(
                            collector, data -> Optional.of(new String(data.getBody())));
            subscriberClient.subscribe(topic, trans);
            var executor = Executors.newSingleThreadExecutor();
            executor.execute(
                    () -> {
                        while (!collector.getFuture().isDone()) {
                            var msg1 = testCase.messageFactory().create("1");
                            var msg2 = testCase.messageFactory().create("2");
                            publisher1.submit(msg1);
                            publisher2.submit(msg2);
                        }
                    });
            Assertions.assertEquals(
                    "[1, 2]", collector.getFuture().get().stream().sorted().toList().toString());
            // terminating executor before closing the publisher to avoid race condition
            // when executor tries to publish messages after the publisher already closed
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.MINUTES);
        }
    }

    /** Test that publisher delivers messages which are in its queue before it is being closed. */
    @ParameterizedTest
    @MethodSource("dataProvider")
    public void test_publisher_on_close(PubSubClientTestCase testCase) throws Exception {
        var received = new ArrayList<Message>();
        var collector = new CollectorSubscriber<>(received);
        try (var subscriberClient = testCase.clientFactory().get();
                var publisherClient = testCase.clientFactory().get();
                // disable any buffering for user publisher so that all messages go directly to
                // client
                var publisher = new SynchronousPublisher<Message>()) {
            String topic = "testTopic1";
            String data = "hello";
            publisherClient.publish(topic, publisher);
            subscriberClient.subscribe(topic, collector);
            var c = 0;
            while (received.isEmpty()) {
                for (int i = 1; i < 10; i++) {
                    publisher.submit(testCase.messageFactory().create(data + ++c));
                }
                XThread.sleep(100);
            }
            publisherClient.close();
            Assertions.assertEquals(
                    data + c, new String(received.get(received.size() - 1).getBody()));
        }
    }
}
