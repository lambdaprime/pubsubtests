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

import java.util.Arrays;

/**
 * @author lambdaprime intid@protonmail.com
 */
public class Message implements Comparable<Message> {
    private byte[] body;

    protected Message(byte[] body) {
        this.body = body;
    }

    protected Message(String body) {
        this(body.getBytes());
    }

    protected Message(int bodySizeInBytes) {
        this(new byte[bodySizeInBytes]);
    }

    public byte[] getBody() {
        return body;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Message m) return compareTo(m) == 0;
        return false;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(body);
    }

    @Override
    public int compareTo(Message o) {
        return Arrays.equals(body, o.body) ? 0 : 1;
    }
}
