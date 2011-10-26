package com.ning.compress.lzf.impl;

import java.io.IOException;
import java.io.InputStream;

import com.ning.compress.lzf.*;

public class LZFDecompressorVanilla extends LZFDecompressor
{
    public LZFDecompressorVanilla() { }

    @Override
    public final int decompressChunk(final InputStream is, final byte[] inputBuffer, final byte[] outputBuffer) 
            throws IOException
    {
        int bytesInOutput;
        /* note: we do NOT read more than 5 bytes because otherwise might need to shuffle bytes
         * for output buffer (could perhaps optimize in future?)
         */
        int bytesRead = readHeader(is, inputBuffer);
        if ((bytesRead < HEADER_BYTES)
                || inputBuffer[0] != LZFChunk.BYTE_Z || inputBuffer[1] != LZFChunk.BYTE_V) {
            if (bytesRead == 0) { // probably fine, clean EOF
                return -1;
            }
            throw new IOException("Corrupt input data, block did not start with 2 byte signature ('ZV') followed by type byte, 2-byte length)");
        }
        int type = inputBuffer[2];
        int compLen = uint16(inputBuffer, 3);
        if (type == LZFChunk.BLOCK_TYPE_NON_COMPRESSED) { // uncompressed
            readFully(is, false, outputBuffer, 0, compLen);
            bytesInOutput = compLen;
        } else { // compressed
            readFully(is, true, inputBuffer, 0, 2+compLen); // first 2 bytes are uncompressed length
            int uncompLen = uint16(inputBuffer, 0);
            decompressChunk(inputBuffer, 2, outputBuffer, 0, uncompLen);
            bytesInOutput = uncompLen;
        }
        return bytesInOutput;
    }
    
    @Override
    public final void decompressChunk(byte[] in, int inPos, byte[] out, int outPos, int outEnd)
            throws IOException
    {
        do {
            int ctrl = in[inPos++] & 255;
            if (ctrl < LZFChunk.MAX_LITERAL) { // literal run
                /*
                copyUpTo32WithSwitch(in, inPos, out, outPos, ctrl);
                ++ctrl;
                inPos += ctrl;
                outPos += ctrl;
                */
                switch (ctrl) {
                case 31:
                    out[outPos++] = in[inPos++];
                case 30:
                    out[outPos++] = in[inPos++];
                case 29:
                    out[outPos++] = in[inPos++];
                case 28:
                    out[outPos++] = in[inPos++];
                case 27:
                    out[outPos++] = in[inPos++];
                case 26:
                    out[outPos++] = in[inPos++];
                case 25:
                    out[outPos++] = in[inPos++];
                case 24:
                    out[outPos++] = in[inPos++];
                case 23:
                    out[outPos++] = in[inPos++];
                case 22:
                    out[outPos++] = in[inPos++];
                case 21:
                    out[outPos++] = in[inPos++];
                case 20:
                    out[outPos++] = in[inPos++];
                case 19:
                    out[outPos++] = in[inPos++];
                case 18:
                    out[outPos++] = in[inPos++];
                case 17:
                    out[outPos++] = in[inPos++];
                case 16:
                    out[outPos++] = in[inPos++];
                case 15:
                    out[outPos++] = in[inPos++];
                case 14:
                    out[outPos++] = in[inPos++];
                case 13:
                    out[outPos++] = in[inPos++];
                case 12:
                    out[outPos++] = in[inPos++];
                case 11:
                    out[outPos++] = in[inPos++];
                case 10:
                    out[outPos++] = in[inPos++];
                case 9:
                    out[outPos++] = in[inPos++];
                case 8:
                    out[outPos++] = in[inPos++];
                case 7:
                    out[outPos++] = in[inPos++];
                case 6:
                    out[outPos++] = in[inPos++];
                case 5:
                    out[outPos++] = in[inPos++];
                case 4:
                    out[outPos++] = in[inPos++];
                case 3:
                    out[outPos++] = in[inPos++];
                case 2:
                    out[outPos++] = in[inPos++];
                case 1:
                    out[outPos++] = in[inPos++];
                case 0:
                    out[outPos++] = in[inPos++];
                }
                continue;
            }
            // back reference
            int len = ctrl >> 5;
            ctrl = -((ctrl & 0x1f) << 8) - 1;
            if (len < 7) { // 2 bytes; length of 3 - 8 bytes
                ctrl -= in[inPos++] & 255;
                out[outPos] = out[outPos++ + ctrl];
                out[outPos] = out[outPos++ + ctrl];
                switch (len) {
                case 6:
                    out[outPos] = out[outPos++ + ctrl];
                case 5:
                    out[outPos] = out[outPos++ + ctrl];
                case 4:
                    out[outPos] = out[outPos++ + ctrl];
                case 3:
                    out[outPos] = out[outPos++ + ctrl];
                case 2:
                    out[outPos] = out[outPos++ + ctrl];
                case 1:
                    out[outPos] = out[outPos++ + ctrl];
                }
                continue;
            }

            // long version (3 bytes, length of up to 264 bytes)
            len = in[inPos++] & 255;
            ctrl -= in[inPos++] & 255;
            
            // First: if there is no overlap, can just use arraycopy:
            if ((ctrl + len) < -9) {
                len += 9;
                if (len <= 32) {
                    copyUpTo32WithSwitch(out, outPos+ctrl, out, outPos, len-1);
                } else {
                    System.arraycopy(out, outPos+ctrl, out, outPos, len);
                }
                outPos += len;
                continue;
            }

            // otherwise manual copy: so first just copy 9 bytes we know are needed
            out[outPos] = out[outPos++ + ctrl];
            out[outPos] = out[outPos++ + ctrl];
            out[outPos] = out[outPos++ + ctrl];
            out[outPos] = out[outPos++ + ctrl];
            out[outPos] = out[outPos++ + ctrl];
            out[outPos] = out[outPos++ + ctrl];
            out[outPos] = out[outPos++ + ctrl];
            out[outPos] = out[outPos++ + ctrl];
            out[outPos] = out[outPos++ + ctrl];

            // then loop
            // Odd: after extensive profiling, looks like magic number
            // for unrolling is 4: with 8 performance is worse (even
            // bit less than with no unrolling).
            len += outPos;
            final int end = len - 3;
            while (outPos < end) {
                out[outPos] = out[outPos++ + ctrl];
                out[outPos] = out[outPos++ + ctrl];
                out[outPos] = out[outPos++ + ctrl];
                out[outPos] = out[outPos++ + ctrl];
            }
            switch  (len - outPos) {
            case 3:
                out[outPos] = out[outPos++ + ctrl];
            case 2:
                out[outPos] = out[outPos++ + ctrl];
            case 1:
                out[outPos] = out[outPos++ + ctrl];
            }
        } while (outPos < outEnd);

        // sanity check to guard against corrupt data:
        if (outPos != outEnd) throw new IOException("Corrupt data: overrun in decompress, input offset "+inPos+", output offset "+outPos);
    }

    /*
    ///////////////////////////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////////////////////////
     */
    
//    private final static void copyUpTo32(byte[] input, int inputIndex, byte[] output, int outputIndex, int length) {
//        copyUpTo32WithSwitch(input, inputIndex, output, outputIndex, length-1);

        // or:
        
//        if (length > 15) {
//            System.arraycopy(input, inputIndex, output, outputIndex, length);
//        } else {
//            copyUpTo16WithSwitch(input, inputIndex, output, outputIndex, length-1);
//        }
//    }
 
    protected static final void copyUpTo32WithSwitch(byte[] in, int inPos, byte[] out, int outPos,
            int lengthMinusOne)
    {
        switch (lengthMinusOne) {
        case 31:
            out[outPos++] = in[inPos++];
        case 30:
            out[outPos++] = in[inPos++];
        case 29:
            out[outPos++] = in[inPos++];
        case 28:
            out[outPos++] = in[inPos++];
        case 27:
            out[outPos++] = in[inPos++];
        case 26:
            out[outPos++] = in[inPos++];
        case 25:
            out[outPos++] = in[inPos++];
        case 24:
            out[outPos++] = in[inPos++];
        case 23:
            out[outPos++] = in[inPos++];
        case 22:
            out[outPos++] = in[inPos++];
        case 21:
            out[outPos++] = in[inPos++];
        case 20:
            out[outPos++] = in[inPos++];
        case 19:
            out[outPos++] = in[inPos++];
        case 18:
            out[outPos++] = in[inPos++];
        case 17:
            out[outPos++] = in[inPos++];
        case 16:
            out[outPos++] = in[inPos++];
        case 15:
            out[outPos++] = in[inPos++];
        case 14:
            out[outPos++] = in[inPos++];
        case 13:
            out[outPos++] = in[inPos++];
        case 12:
            out[outPos++] = in[inPos++];
        case 11:
            out[outPos++] = in[inPos++];
        case 10:
            out[outPos++] = in[inPos++];
        case 9:
            out[outPos++] = in[inPos++];
        case 8:
            out[outPos++] = in[inPos++];
        case 7:
            out[outPos++] = in[inPos++];
        case 6:
            out[outPos++] = in[inPos++];
        case 5:
            out[outPos++] = in[inPos++];
        case 4:
            out[outPos++] = in[inPos++];
        case 3:
            out[outPos++] = in[inPos++];
        case 2:
            out[outPos++] = in[inPos++];
        case 1:
            out[outPos++] = in[inPos++];
        case 0:
            out[outPos++] = in[inPos++];
        }
    }
    
}
