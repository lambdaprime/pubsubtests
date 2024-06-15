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

/**
 * @author lambdaprime intid@protonmail.com
 */
public class ByteMessageFactory implements MessageFactory {

    @Override
    public Message create(String body) {
        return new Message(body);
    }

    @Override
    public Message create(byte[] body) {
        return new Message(body);
    }

    @Override
    public RandomMessageGenerator createGenerator(long seed, int messageSizeInBytes) {
        return new RandomMessageGenerator(this, seed, messageSizeInBytes);
    }
}
