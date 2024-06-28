/*
 * Copyright 2024 pubsubtests project
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
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;

/**
 * Implementation of {@link TestPubSubClient} which delegates all subscribe and publish calls to
 * separate instances of {@link TestPubSubClient}
 *
 * <p>Ordinary {@link TestPubSubClient} allows to run <b>pubsubtests</b> against {@link
 * #publish(String, Publisher)} and {@link #subscribe(String, Subscriber)} methods of the same
 * client implementation.
 *
 * <p>This class on the other hand allows to run <b>pubsubtests</b> against {@link #publish(String,
 * Publisher)} and {@link #subscribe(String, Subscriber)} methods of different clients
 * implementations. This primarily helps to test if Publisher/Subscriber clients from different
 * vendors can interact with each other (Subscriber from vendor A can be subscribed to Publisher
 * from vendor B and vice-versa).
 *
 * @author lambdaprime intid@protonmail.com
 */
public class CompositePubSubClient implements TestPubSubClient {

    private TestPubSubClient subscribeClient;
    private TestPubSubClient publishClient;

    /**
     * @param subscribeClient client to handle {@link #subscribe(String,
     *     java.util.concurrent.Flow.Subscriber)} calls
     * @param publishClient client to handle {@link #publish(String,
     *     java.util.concurrent.Flow.Publisher)}
     */
    public CompositePubSubClient(TestPubSubClient subscribeClient, TestPubSubClient publishClient) {
        this.subscribeClient = subscribeClient;
        this.publishClient = publishClient;
    }

    @Override
    public void close() {
        subscribeClient.close();
    }

    @Override
    public void publish(String topic, Publisher<Message> publisher) {
        publishClient.publish(topic, publisher);
    }

    @Override
    public void subscribe(String topic, Subscriber<Message> subscriber) {
        subscribeClient.subscribe(topic, subscriber);
    }
}
