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
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;

/**
 * @author lambdaprime intid@protonmail.com
 */
public interface TestPubSubClient extends AutoCloseable {
    void subscribe(String topic, Subscriber<Message> subscriber);

    void publish(String topic, Publisher<Message> publisher);

    @Override
    void close();
}
