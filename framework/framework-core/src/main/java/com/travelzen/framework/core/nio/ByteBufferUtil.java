/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.travelzen.framework.core.nio;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.apache.commons.lang3.ArrayUtils;

/**
 * Utility methods to make ByteBuffers less painful The following should
 * illustrate the different ways byte buffers can be used
 * 
 * public void testArrayOffet() {
 * 
 * byte[] b = "test_slice_array".getBytes(); ByteBuffer bb =
 * ByteBuffer.allocate(1024);
 * 
 * assert bb.position() == 0; assert bb.limit() == 1024; assert bb.capacity() ==
 * 1024;
 * 
 * bb.put(b);
 * 
 * assert bb.position() == b.length; assert bb.remaining() == bb.limit() -
 * bb.position();
 * 
 * ByteBuffer bb2 = bb.slice();
 * 
 * assert bb2.position() == 0;
 * 
 * //slice should begin at other buffers current position assert
 * bb2.arrayOffset() == bb.position();
 * 
 * //to match the position in the underlying array one needs to //track
 * arrayOffset assert bb2.limit()+bb2.arrayOffset() == bb.limit();
 * 
 * 
 * assert bb2.remaining() == bb.remaining();
 * 
 * }
 * 
 * }
 * 
 */
public class ByteBufferUtil {

	public static byte[] getBytes(ByteBuffer byteBuffer) {

		byte[] bytes =
		// new byte[byteBuffer.limit()-byteBuffer.position()];
		new byte[byteBuffer.remaining()];
		

		byteBuffer.get(bytes);
		return bytes;

	}

	public static int compareUnsigned(ByteBuffer o1, ByteBuffer o2) {
		return compareUnsigned(o1.array(), o2.array(),
				o1.arrayOffset() + o1.position(),
				o2.arrayOffset() + o2.position(),
				o1.limit() + o1.arrayOffset(), o2.limit() + o2.arrayOffset());
	}

	public static int compare(byte[] o1, ByteBuffer o2) {
		return compareUnsigned(o1, o2.array(), 0,
				o2.arrayOffset() + o2.position(), o1.length,
				o2.limit() + o2.arrayOffset());
	}

	public static int compare(ByteBuffer o1, byte[] o2) {
		return compareUnsigned(o1.array(), o2,
				o1.arrayOffset() + o1.position(), 0,
				o1.limit() + o1.arrayOffset(), o2.length);
	}

	public static String string(ByteBuffer b, Charset charset) {
		return new String(b.array(), b.arrayOffset() + b.position(),
				b.remaining(), charset);
	}

	public static String string(ByteBuffer b) {
		return new String(b.array(), b.arrayOffset() + b.position(),
				b.remaining());
	}

	public static ByteBuffer bytes(String s) {
		try {
			return ByteBuffer.wrap(s.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public static ByteBuffer clone(ByteBuffer o) {
		assert o != null;

		if (o.remaining() == 0)
			return ByteBuffer.wrap(ArrayUtils.EMPTY_BYTE_ARRAY);

		ByteBuffer clone = ByteBuffer.allocate(o.remaining());

		if (o.isDirect()) {
			for (int i = o.position(); i < o.limit(); i++) {
				clone.put(o.get(i));
			}
			clone.flip();
		} else {
			System.arraycopy(o.array(), o.arrayOffset() + o.position(),
					clone.array(), 0, o.remaining());
		}

		return clone;
	}

	private static int compareUnsigned(byte[] bytes1, byte[] bytes2,
			int offset1, int offset2, int len1, int len2) {
		if (bytes1 == null) {
			return bytes2 == null ? 0 : -1;
		}
		if (bytes2 == null)
			return 1;

		int minLength = Math.min(len1 - offset1, len2 - offset2);
		for (int x = 0, i = offset1, j = offset2; x < minLength; x++, i++, j++) {
			if (bytes1[i] == bytes2[j])
				continue;
			// compare non-equal bytes as unsigned
			return (bytes1[i] & 0xFF) < (bytes2[j] & 0xFF) ? -1 : 1;
		}
		if ((len1 - offset1) == (len2 - offset2))
			return 0;
		else
			return ((len1 - offset1) < (len2 - offset2)) ? -1 : 1;
	}
}
