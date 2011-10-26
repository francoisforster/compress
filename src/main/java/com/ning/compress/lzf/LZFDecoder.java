/* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.ning.compress.lzf;

import java.io.IOException;

import com.ning.compress.lzf.util.DecompressorLoader;

/**
 * Decoder that handles decoding of sequence of encoded LZF chunks,
 * combining them into a single contiguous result byte array.
 * As of version 0.9, this class has been mostly replaced by
 * {@link LZFDecompressor}, although static methods are left here
 * and may still be used.
 * All static methods use {@link DecompressorLoader#optimalInstance}
 * to find actual {@link LZFDecompressor} instance to use.
 * 
 * @author Tatu Saloranta (tatu@ning.com)
 * 
 * @deprecated As of 0.9, use {@link LZFDecompressor} instead
 */
@Deprecated
public class LZFDecoder
{
    /*
    ///////////////////////////////////////////////////////////////////////
    // Old API
    ///////////////////////////////////////////////////////////////////////
     */

    public static byte[] decode(final byte[] inputBuffer) throws IOException {
        return decode(inputBuffer, 0, inputBuffer.length);
    }
    
    public static byte[] decode(final byte[] inputBuffer, int inputPtr, int inputLen) throws IOException {
        return DecompressorLoader.optimalInstance().decompress(inputBuffer);
    }
    
    public static int decode(final byte[] inputBuffer, final byte[] targetBuffer) throws IOException {
        return decode(inputBuffer, 0, inputBuffer.length, targetBuffer);
    }

    public static int decode(final byte[] sourceBuffer, int inPtr, int inLength, final byte[] targetBuffer) throws IOException {
        return DecompressorLoader.optimalInstance().decompress(sourceBuffer, inPtr, inLength, targetBuffer);        
    }

    public static int calculateUncompressedSize(byte[] data, int ptr, int length) throws IOException {
        return LZFDecompressor.calculateUncompressedSize(data, ptr, length);
    }
}
