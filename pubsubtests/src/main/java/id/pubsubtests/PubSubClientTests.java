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

import id.xfunction.concurrent.SameThreadExecutorService;
import id.xfunction.concurrent.flow.CollectorSubscriber;
import id.xfunction.concurrent.flow.FixedCollectorSubscriber;
import id.xfunction.lang.XThread;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Collection of tests for {@link TestPubSubClient} implementations.
 *
 * @author lambdaprime intid@protonmail.com
 */
@Nested
public abstract class PubSubClientTests {

    public record TestCase(Supplier<TestPubSubClient> clientFactory) {}

    /**
     * Test that publisher does not drop messages when there is no subscribers and will block
     * eventually accepting new ones,
     */
    @ParameterizedTest
    @MethodSource("dataProvider")
    public void test_publish_when_no_subscribers(TestCase testCase) {
        try (var publisherClient = testCase.clientFactory.get(); ) {
            String topic = "testTopic1";
            var publisher = new SubmissionPublisher<String>();
            String data = "hello";
            publisherClient.publish(topic, publisher);
            while (publisher.offer(data, null) >= 0)
                ;
        }
    }

    @ParameterizedTest
    @MethodSource("dataProvider")
    public void test_multiple_subscribers_same_topic(TestCase testCase) throws Exception {
        var maxNumOfMessages = 15;
        try (var subscriberClient = testCase.clientFactory.get();
                var publisherClient = testCase.clientFactory.get(); ) {
            String topic = "testTopic1";
            var publisher = new SubmissionPublisher<String>();
            publisherClient.publish(topic, publisher);
            var subscribers =
                    Stream.generate(
                                    () ->
                                            new FixedCollectorSubscriber<>(
                                                    new ArrayList<String>(), maxNumOfMessages))
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
                                    publisher.submit(data);
                                }
                            });
            for (var sub : subscribers) {
                var data = sub.getFuture().get();
                Assertions.assertEquals(expected, data);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("dataProvider")
    public void test_publish_forever(TestCase testCase) throws Exception {
        try (var subscriberClient = testCase.clientFactory.get();
                var publisherClient = testCase.clientFactory.get(); ) {
            String topic = "testTopic1";
            var publisher = new SubmissionPublisher<String>();
            String data = "hello";
            publisherClient.publish(topic, publisher);
            var collector = new FixedCollectorSubscriber<>(new ArrayList<String>(), 1);
            subscriberClient.subscribe(topic, collector);
            ForkJoinPool.commonPool()
                    .execute(
                            () -> {
                                while (!collector.getFuture().isDone()) {
                                    publisher.submit(data);
                                }
                            });
            Assertions.assertEquals(data, collector.getFuture().get().get(0));
        }
    }

    @ParameterizedTest
    @MethodSource("dataProvider")
    public void test_publish_order(TestCase testCase) throws Exception {
        try (var subscriberClient = testCase.clientFactory.get();
                var publisherClient = testCase.clientFactory.get(); ) {
            String topic = "/testTopic1";
            var publisher = new SubmissionPublisher<String>(new SameThreadExecutorService(), 1);
            publisherClient.publish(topic, publisher);
            var collector = new FixedCollectorSubscriber<>(new ArrayList<String>(), 50);
            subscriberClient.subscribe(topic, collector);
            ForkJoinPool.commonPool()
                    .execute(
                            () -> {
                                int c = 0;
                                while (!collector.getFuture().isDone()) {
                                    var msg = "" + c++;
                                    System.out.println("          " + msg);
                                    publisher.submit(msg);
                                }
                            });
            var received =
                    collector.getFuture().get().stream().mapToInt(Integer::parseInt).toArray();
            var start = received[0];
            for (int i = 0; i < received.length; i++) {
                Assertions.assertEquals(start + i, received[i]);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("dataProvider")
    public void test_publish_single_message(TestCase testCase) throws Exception {
        try (var subscriberClient = testCase.clientFactory.get();
                var publisherClient = testCase.clientFactory.get(); ) {
            String topic = "testTopic1";
            var publisher = new SubmissionPublisher<String>();
            String data = "hello";
            publisherClient.publish(topic, publisher);
            var collector = new FixedCollectorSubscriber<>(new ArrayList<String>(), 1);
            subscriberClient.subscribe(topic, collector);
            // to discover subscriber should take less than 1sec
            XThread.sleep(1000);
            publisher.submit(data);
            Assertions.assertEquals(data, collector.getFuture().get().get(0));
        }
    }

    @ParameterizedTest
    @MethodSource("dataProvider")
    public void test_multiple_publishers(TestCase testCase) throws Exception {
        String topic = "/testTopic1";
        try (var subscriberClient = testCase.clientFactory.get();
                var publisherClient1 = testCase.clientFactory.get();
                var publisherClient2 = testCase.clientFactory.get();
                var publisher1 = new SubmissionPublisher<String>();
                var publisher2 = new SubmissionPublisher<String>()) {
            publisherClient1.publish(topic, publisher1);
            publisherClient2.publish(topic, publisher2);
            var collector = new FixedCollectorSubscriber<>(new HashSet<String>(), 2);
            subscriberClient.subscribe(topic, collector);
            ForkJoinPool.commonPool()
                    .execute(
                            () -> {
                                while (!collector.getFuture().isDone()) {
                                    var msg1 = "1";
                                    var msg2 = "2";
                                    publisher1.submit(msg1);
                                    publisher2.submit(msg2);
                                }
                            });
            Assertions.assertEquals(
                    "[1, 2]", collector.getFuture().get().stream().sorted().toList().toString());
        }
    }

    /** Test that publisher delivers messages which are in its queue before it is being closed. */
    @ParameterizedTest
    @MethodSource("dataProvider")
    public void test_publisher_on_close(TestCase testCase) throws Exception {
        var received = new ArrayList<String>();
        var collector = new CollectorSubscriber<>(received);
        try (var subscriberClient = testCase.clientFactory.get();
                var publisherClient = testCase.clientFactory.get();
                // disable any buffering for user publisher so that all messages go directly to
                // client
                var publisher =
                        new SubmissionPublisher<String>(new SameThreadExecutorService(), 1)) {
            String topic = "testTopic1";
            String data = "hello";
            publisherClient.publish(topic, publisher);
            subscriberClient.subscribe(topic, collector);
            var c = 0;
            while (received.isEmpty()) {
                for (int i = 1; i < 10; i++) {
                    publisher.submit(data + ++c);
                }
                XThread.sleep(100);
            }
            publisherClient.close();
            Assertions.assertEquals(data + c, received.get(received.size() - 1));
        }
    }
}
