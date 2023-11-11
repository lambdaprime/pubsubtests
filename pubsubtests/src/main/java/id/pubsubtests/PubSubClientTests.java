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

import id.pubsubtests.impl.AlitaFileHelper;
import id.xfunction.concurrent.SameThreadExecutorService;
import id.xfunction.concurrent.flow.CollectorSubscriber;
import id.xfunction.concurrent.flow.FixedCollectorSubscriber;
import id.xfunction.concurrent.flow.SimpleSubscriber;
import id.xfunction.concurrent.flow.TransformProcessor;
import id.xfunction.function.Unchecked;
import id.xfunction.lang.XThread;
import id.xfunction.nio.file.XFiles;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
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
 * Collection of tests for {@link TestPubSubClient} implementations.
 *
 * <p>To use tests:
 *
 * <ul>
 *   <li>Using client which is going to be tested implement {@link id.pubsubtests.TestPubSubClient}
 *   <li>Create a JUnit test class for the client and let it extend {@link
 *       id.pubsubtests.PubSubClientTests}
 *   <li>Define dataProvider method:
 *       <pre>{@code
 * static Stream<PubSubClientTestCase> dataProvider() {
 *      return Stream.of(new TestCase(RtpsTalkTestPubSubClient::new));
 * }
 * }</pre>
 * </ul>
 *
 * <p>If some of the tests from {@link id.pubsubtests.PubSubClientTests} are irrelevant to the
 * client which is being tested then their methods can be overridden in the test class.
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
@Nested
public abstract class PubSubClientTests {

    /**
     * Test that publisher does not drop messages when there is no subscribers and will block
     * eventually accepting new ones,
     */
    @ParameterizedTest
    @MethodSource("dataProvider")
    public void test_publish_when_no_subscribers(PubSubClientTestCase testCase) {
        try (var publisherClient = testCase.clientFactory().get(); ) {
            String topic = "testTopic1";
            var publisher = new SubmissionPublisher<byte[]>();
            String data = "hello";
            publisherClient.publish(topic, publisher);
            while (publisher.offer(data.getBytes(), null) >= 0)
                ;
        }
    }

    @ParameterizedTest
    @MethodSource("dataProvider")
    public void test_multiple_subscribers_same_topic(PubSubClientTestCase testCase)
            throws Exception {
        var maxNumOfMessages = 15;
        try (var subscriberClient = testCase.clientFactory().get();
                var publisherClient = testCase.clientFactory().get(); ) {
            String topic = "testTopic1";
            var publisher = new SubmissionPublisher<byte[]>();
            publisherClient.publish(topic, publisher);
            var subscribers =
                    Stream.generate(
                                    () ->
                                            new FixedCollectorSubscriber<>(
                                                    new ArrayList<byte[]>(), maxNumOfMessages))
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
                                    publisher.submit(data.getBytes());
                                }
                            });
            for (var sub : subscribers) {
                var data = sub.getFuture().get().stream().map(String::new).toList();
                Assertions.assertEquals(expected, data);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("dataProvider")
    public void test_publish_forever(PubSubClientTestCase testCase) throws Exception {
        try (var subscriberClient = testCase.clientFactory().get();
                var publisherClient = testCase.clientFactory().get(); ) {
            String topic = "testTopic1";
            var publisher = new SubmissionPublisher<byte[]>();
            String data = "hello";
            publisherClient.publish(topic, publisher);
            var collector = new FixedCollectorSubscriber<>(new ArrayList<byte[]>(), 1);
            subscriberClient.subscribe(topic, collector);
            ForkJoinPool.commonPool()
                    .execute(
                            () -> {
                                while (!collector.getFuture().isDone()) {
                                    publisher.submit(data.getBytes());
                                }
                            });
            Assertions.assertEquals(
                    data, collector.getFuture().get().stream().map(String::new).findFirst().get());
        }
    }

    @ParameterizedTest
    @MethodSource("dataProvider")
    public void test_publish_order(PubSubClientTestCase testCase) throws Exception {
        try (var subscriberClient = testCase.clientFactory().get();
                var publisherClient = testCase.clientFactory().get(); ) {
            String topic = "/testTopic1";
            var publisher = new SubmissionPublisher<byte[]>(new SameThreadExecutorService(), 1);
            publisherClient.publish(topic, publisher);
            var collector = new FixedCollectorSubscriber<>(new ArrayList<byte[]>(), 50);
            subscriberClient.subscribe(topic, collector);
            ForkJoinPool.commonPool()
                    .execute(
                            () -> {
                                int c = 0;
                                while (!collector.getFuture().isDone()) {
                                    var msg = "" + c++;
                                    System.out.println("          " + msg);
                                    publisher.submit(msg.getBytes());
                                }
                            });
            var received =
                    collector.getFuture().get().stream()
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
            var publisher = new SubmissionPublisher<byte[]>();
            String data = "hello";
            publisherClient.publish(topic, publisher);
            var collector = new FixedCollectorSubscriber<>(new ArrayList<byte[]>(), 1);
            subscriberClient.subscribe(topic, collector);
            // to discover subscriber should take less than 5sec
            XThread.sleep(5000);
            publisher.submit(data.getBytes());
            Assertions.assertEquals(
                    data, collector.getFuture().get().stream().map(String::new).findFirst().get());
        }
    }

    @ParameterizedTest
    @MethodSource("dataProvider")
    public void test_multiple_publishers(PubSubClientTestCase testCase) throws Exception {
        String topic = "/testTopic1";
        try (var subscriberClient = testCase.clientFactory().get();
                var publisherClient1 = testCase.clientFactory().get();
                var publisherClient2 = testCase.clientFactory().get();
                var publisher1 = new SubmissionPublisher<byte[]>();
                var publisher2 = new SubmissionPublisher<byte[]>()) {
            publisherClient1.publish(topic, publisher1);
            publisherClient2.publish(topic, publisher2);
            var collector = new FixedCollectorSubscriber<>(new HashSet<String>(), 2);
            var trans =
                    new TransformProcessor<byte[], String>(data -> Optional.of(new String(data)));
            trans.subscribe(collector);
            subscriberClient.subscribe(topic, trans);
            var executor = Executors.newSingleThreadExecutor();
            executor.execute(
                    () -> {
                        while (!collector.getFuture().isDone()) {
                            var msg1 = "1";
                            var msg2 = "2";
                            publisher1.submit(msg1.getBytes());
                            publisher2.submit(msg2.getBytes());
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
        var received = new ArrayList<byte[]>();
        var collector = new CollectorSubscriber<>(received);
        try (var subscriberClient = testCase.clientFactory().get();
                var publisherClient = testCase.clientFactory().get();
                // disable any buffering for user publisher so that all messages go directly to
                // client
                var publisher =
                        new SubmissionPublisher<byte[]>(new SameThreadExecutorService(), 1)) {
            String topic = "testTopic1";
            String data = "hello";
            publisherClient.publish(topic, publisher);
            subscriberClient.subscribe(topic, collector);
            var c = 0;
            while (received.isEmpty()) {
                for (int i = 1; i < 10; i++) {
                    publisher.submit((data + ++c).getBytes());
                }
                XThread.sleep(100);
            }
            publisherClient.close();
            Assertions.assertEquals(data + c, new String(received.get(received.size() - 1)));
        }
    }

    @ParameterizedTest
    @MethodSource("dataProvider")
    public void test_publish_multiple_60kb_messages(PubSubClientTestCase testCase)
            throws Exception {
        try (var subscriberClient = testCase.clientFactory().get();
                var publisherClient = testCase.clientFactory().get();
                var publisher =
                        new SubmissionPublisher<byte[]>(new SameThreadExecutorService(), 1)) {
            String topic = "testTopic1";
            var imgFile = AlitaFileHelper.extractToTempFolderIfMissing();
            var future = new CompletableFuture<Path>();
            publisherClient.publish(topic, publisher);
            subscriberClient.subscribe(
                    topic,
                    new SimpleSubscriber<>() {
                        Path outputFile = Files.createTempFile("alita", "");
                        int bytesLeft = (int) Files.size(imgFile);
                        FileOutputStream fos = new FileOutputStream(outputFile.toFile());

                        public void onNext(byte[] item) {
                            try {
                                fos.write(item, 0, Math.min(item.length, bytesLeft));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            var subscription = getSubscription().get();
                            bytesLeft -= item.length;
                            if (bytesLeft <= 0) {
                                subscription.cancel();
                                future.complete(outputFile);
                                Unchecked.run(fos::close);
                            }
                            subscription.request(1);
                        }
                    });
            try (var fis = new FileInputStream(imgFile.toFile())) {
                Assertions.assertEquals(AlitaFileHelper.SIZE_IN_BYTES, fis.available());
                while (fis.available() != 0) {
                    var buf = new byte[60_000];
                    fis.read(buf);
                    publisher.submit(buf);
                }
                System.out.println("Image sent");
            }
            var startAt = Instant.now();
            var imgReceived =
                    Assertions.assertTimeout(
                            testCase.test_publish_multiple_60kb_messages_expected_timeout(),
                            () -> future.get());
            System.out.println(
                    "Receive time test_publish_multiple_60kb_messages: "
                            + Duration.between(startAt, Instant.now()));
            Assertions.assertEquals(
                    true, XFiles.isContentEqual(imgFile.toFile(), imgReceived.toFile()));
        }
    }

    @ParameterizedTest
    @MethodSource("dataProvider")
    public void test_publish_single_message_over_5mb(PubSubClientTestCase testCase)
            throws Exception {
        try (var subscriberClient = testCase.clientFactory().get();
                var publisherClient = testCase.clientFactory().get();
                var publisher =
                        new SubmissionPublisher<byte[]>(new SameThreadExecutorService(), 1)) {
            String topic = "testTopic1";
            var imgFile = AlitaFileHelper.extractToTempFolderIfMissing();
            var future = new CompletableFuture<Path>();
            publisherClient.publish(topic, publisher);
            subscriberClient.subscribe(
                    topic,
                    new SimpleSubscriber<>() {
                        Path outputFile = Files.createTempFile("alita", "");

                        public void onNext(byte[] item) {
                            try (FileOutputStream fos = new FileOutputStream(outputFile.toFile())) {
                                fos.write(item);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            subscription.cancel();
                            future.complete(outputFile);
                        }
                    });
            var data = Files.readAllBytes(imgFile);
            Assertions.assertEquals(AlitaFileHelper.SIZE_IN_BYTES, data.length);
            publisher.submit(data);
            var startAt = Instant.now();
            var imgReceived =
                    Assertions.assertTimeout(
                            testCase.test_publish_single_message_over_5mb_expected_timeout(),
                            () -> future.get());
            System.out.println(
                    "Receive time test_publish_single_message_over_5mb: "
                            + Duration.between(startAt, Instant.now()));
            Assertions.assertEquals(
                    true, XFiles.isContentEqual(imgFile.toFile(), imgReceived.toFile()));
        }
    }

    /**
     * Constantly publish messages over 5mb for period of 1 minute. Assert how many messages
     * Subscriber received.
     */
    @ParameterizedTest
    @MethodSource("dataProvider")
    public void test_throutput(PubSubClientTestCase testCase) throws Exception {
        try (var subscriberClient = testCase.clientFactory().get();
                var publisherClient = testCase.clientFactory().get();
                var publisher =
                        new SubmissionPublisher<byte[]>(new SameThreadExecutorService(), 1)) {
            String topic = "testTopic1";
            var imgFile = AlitaFileHelper.extractToTempFolderIfMissing();
            var future =
                    new CompletableFuture<Boolean>().completeOnTimeout(true, 1, TimeUnit.MINUTES);
            var count = new int[1];
            publisherClient.publish(topic, publisher);
            subscriberClient.subscribe(
                    topic,
                    new SimpleSubscriber<>() {
                        public void onNext(byte[] item) {
                            System.out.println("Received message" + count[0]);
                            if (AlitaFileHelper.isEquals(item)) count[0]++;
                            else future.complete(false);
                            if (future.isDone()) {
                                System.out.println("Cancel subscription");
                                subscription.cancel();
                            } else subscription.request(1);
                        }
                    });
            var data = Files.readAllBytes(imgFile);
            Assertions.assertEquals(AlitaFileHelper.SIZE_IN_BYTES, data.length);
            while (!future.isDone()) {
                publisher.submit(data);
                XThread.sleep(300);
                System.out.println("Sent message");
            }
            System.out.println("Stop publishing");
            Assertions.assertEquals(true, future.get());
            System.out.println("Received number of messages test_throutput: " + count[0]);
            Assertions.assertEquals(0,
                    count[0] - testCase.test_throutput_expected_message_count());
        }
    }
}
