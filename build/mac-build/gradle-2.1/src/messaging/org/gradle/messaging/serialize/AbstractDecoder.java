/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.messaging.serialize;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public abstract class AbstractDecoder implements Decoder {
    private DecoderStream stream;

    public InputStream getInputStream() {
        if (stream == null) {
            stream = new DecoderStream();
        }
        return stream;
    }

    public void readBytes(byte[] buffer) throws IOException {
        readBytes(buffer, 0, buffer.length);
    }

    public byte[] readBinary() throws EOFException, IOException {
        int size = readSmallInt();
        byte[] result = new byte[size];
        readBytes(result);
        return result;
    }

    public int readSmallInt() throws EOFException, IOException {
        return readInt();
    }

    public long readSmallLong() throws EOFException, IOException {
        return readLong();
    }

    public String readNullableString() throws EOFException, IOException {
        if (readBoolean()) {
            return readString();
        } else {
            return null;
        }
    }

    public void skipBytes(long count) throws EOFException, IOException {
        long remaining = count;
        while (remaining > 0) {
            long skipped = maybeSkip(remaining);
            if (skipped <= 0) {
                break;
            }
            remaining -= skipped;
        }
        if (remaining > 0) {
            throw new EOFException();
        }
    }

    protected abstract int maybeReadBytes(byte[] buffer, int offset, int count) throws IOException;

    protected abstract long maybeSkip(long count) throws IOException;

    private class DecoderStream extends InputStream {
        byte[] buffer = new byte[1];

        @Override
        public long skip(long n) throws IOException {
            return maybeSkip(n);
        }

        @Override
        public int read() throws IOException {
            int read = maybeReadBytes(buffer, 0, 1);
            if (read <= 0) {
                return read;
            }
            return buffer[0] & 0xff;
        }

        @Override
        public int read(byte[] buffer) throws IOException {
            return maybeReadBytes(buffer, 0, buffer.length);
        }

        @Override
        public int read(byte[] buffer, int offset, int count) throws IOException {
            return maybeReadBytes(buffer, offset, count);
        }
    }
}
