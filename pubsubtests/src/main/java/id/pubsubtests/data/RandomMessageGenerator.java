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
package id.pubsubtests.data;

import java.util.Random;

/**
 * @author lambdaprime intid@protonmail.com
 */
public class RandomMessageGenerator {

    private MessageFactory factory;
    private Random random;
    private int messageSizeInBytes;

    public RandomMessageGenerator(MessageFactory factory, long seed, int messageSizeInBytes) {
        this.factory = factory;
        this.messageSizeInBytes = messageSizeInBytes;
        random = new Random(seed);
    }

    public Message nextRandomMessage() {
        var buf = new byte[messageSizeInBytes];
        random.nextBytes(buf);
        return factory.create(buf);
    }

    /** Reuse existing message memory and populate it with random body */
    public void populateMessage(Message message) {
        random.nextBytes(message.getBody());
    }
}
