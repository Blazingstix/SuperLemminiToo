package lemmini.extract;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Adler32;
import org.apache.commons.io.output.ByteArrayOutputStream;

/*
 * FILE MODIFIED BY RYAN SAKOWSKI
 * 
 * 
 * Copyright 2009 Volker Oth
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Simple diff/patch algorithm for text or binary files
 * In contrast to common line based diff utilities, this algorithm
 * works byte based to create small binary difference files.
 * However this very simple approach is only sensible for small files.
 * It is by no way meant as rival to full featured approaches like XDelta.
 *
 * @author Volker Oth
 */
public class Diff {
    /** insert n bytes */
    private static final byte INSERT = 0;
    /** delete n bytes */
    private static final byte DELETE = 1;
    /** replace n bytes with n bytes */
    private static final byte REPLACE = 2;
    /** substitute n bytes with m bytes */
    private static final byte SUBSTITUTE = 3;
    
    /** print info to System.out */
    private static final boolean VERBATIM = false;
    
    /** magic number for header ID */
    private static final int HEADER_ID = 0xdeadbeef;
    /** magic number for data ID */
    private static final int DATA_ID = 0xfade0ff;
    
    /** re-synchronization length */
    private static int resyncLength = 4;
    /** re-synchronization window length */
    private static int windowLength = 512;
    
    /** target CRC */
    public  static int targetCRC = 0;
    
    /**
     * Set diff parameters
     * @param winLen Length of windows to search for re-synchronization
     * @param resyncLen Number of equal bytes needed for re-synchronization
     */
    public static void setParameters(final int winLen, final int resyncLen) {
        resyncLength = resyncLen;
        windowLength = winLen;
    }
    
    /**
     * Create diff buffer from the differences between source and target buffer
     * @param bsrc source buffer (the file to be patched)
     * @param btrg target buffer (the file as it should be)
     * @return buffer of differences
     */
    public static byte[] diffBuffers(final byte[] bsrc, final byte[] btrg) {
        ByteArrayOutputStream patch = new ByteArrayOutputStream(16 * 1024);
        ByteBuffer src = ByteBuffer.wrap(bsrc).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer trg = ByteBuffer.wrap(btrg).order(ByteOrder.LITTLE_ENDIAN);
        
        // compare crcs
        Adler32 crcSrc = new Adler32();
        crcSrc.update(src.array());
        Adler32 crcTrg = new Adler32();
        crcTrg.update(trg.array());
        targetCRC = (int) crcTrg.getValue();
        if (crcTrg.getValue() == crcSrc.getValue()) {
            return null;
        }
        
        // write header
        writeInt(patch, HEADER_ID);
        // write lengths to patch list
        writeLen(patch, src.capacity());
        writeLen(patch, trg.capacity());
        // write crcs to patch list
        writeInt(patch, (int) crcSrc.getValue());
        writeInt(patch, (int) crcTrg.getValue());
        writeInt(patch, DATA_ID);
        
        // examine source buffer
        int ofs = 0;
        while (src.position() < src.capacity()) {
            src.mark();
            trg.mark();
            // search for difference
            int s = Byte.toUnsignedInt(src.get());
            int t = Byte.toUnsignedInt(trg.get());
            if (s == t) {
                ofs++;
                continue;
            }
            // reset indices
            src.reset();
            trg.reset();
            // write offset
            writeLen(patch, ofs);
            out(String.format("Offset: %d", ofs));
            ofs = 0;
            // check for insert, delete, replace
            int len, leni, lend, lenr;
            int[] lens;
            int state = -1;
            
            leni = checkInsert(src, trg);
            lend = checkDelete(src, trg);
            lenr = checkReplace(src, trg);
            lens = checkSubstitute(src, trg);
            len = Math.min(leni, lend);
            len = Math.min(len, lenr);
            len = Math.min(len, lens[1]);
            if (len > windowLength) {
                // completely lost synchronisation
                //int rs = src.capacity() - src.position();
                //int rt = trg.capacity() - trg.position();
                //if (rs == rt) {
                //    len = rs;
                //    state = REPLACE;
                //} else {
                //    len = rt;
                //    state = INSERT;
                //}
                break;
            }
            if (len == leni) {
                state = INSERT;
            } else if (len == lend) {
                state = DELETE;
            } else if (len == lenr) {
                state = REPLACE;
            } else if (len == lens[1]) {
                state = SUBSTITUTE;
            }
            
            switch (state) {
                case INSERT:
                    // insert
                    out(String.format("Insert: %d", len));
                    patch.write(INSERT);
                    writeLen(patch, len);
                    byte[] lenBuf = new byte[len];
                    trg.get(lenBuf);
                    patch.write(lenBuf, 0, lenBuf.length);
                    break;
                case DELETE:
                    // delete
                    out(String.format("Delete: %d", len));
                    patch.write(DELETE);
                    writeLen(patch, len);
                    src.position(src.position() + len);
                    break;
                case REPLACE:
                    // replace
                    out(String.format("Replace: %d", len));
                    patch.write(REPLACE);
                    writeLen(patch, len);
                    lenBuf = new byte[len];
                    trg.get(lenBuf);
                    patch.write(lenBuf, 0, lenBuf.length);
                    src.position(src.position() + len);
                    break;
                case SUBSTITUTE:
                    // replace
                    out(String.format("Substitute: %d/%d", lens[0], lens[1]));
                    patch.write(SUBSTITUTE);
                    writeLen(patch, lens[0]);
                    writeLen(patch, lens[1]);
                    byte[] lensBuf = new byte[lens[1]];
                    trg.get(lensBuf);
                    patch.write(lensBuf, 0, lensBuf.length);
                    src.position(src.position() + lens[0]);
                    break;
                default:
                    break;
            }
        }
        
        // if the files end identically, the offset needs to be written
        if (ofs != 0) {
            out(String.format("Offset: %d", ofs));
            writeLen(patch, ofs);
        }
        
        // check for stuff to insert in target
        if (trg.position() < trg.capacity()) {
            patch.write(INSERT);
            int len = trg.capacity() - trg.position();
            out(String.format("Insert (End): %d", len));
            writeLen(patch, len);
            byte[] lenBuf = new byte[len];
            trg.get(lenBuf);
            patch.write(lenBuf, 0, lenBuf.length);
        }
        
        out(String.format("Patch length: %d", patch.size()));
        
        if (patch.size() == 0) {
            return null;
        }
        
        return patch.toByteArray();
    }
    
    /**
     * Create a target buffer from a source buffer and a buffer of differences
     * @param bsrc source buffer
     * @param bpatch buffer containing differences
     * @return target buffer created from a source buffer and a buffer of differences
     * @throws DiffException
     */
    public static byte[] patchBuffers(final byte[] bsrc, final byte[] bpatch) throws DiffException {
        ByteBuffer src = ByteBuffer.wrap(bsrc).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer patch = ByteBuffer.wrap(bpatch).asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);
        // calculate src crc
        Adler32 crc = new Adler32();
        crc.update(src.array());
        // analyze header
        if (patch.getInt() != Diff.HEADER_ID) {
            throw new DiffException("No header ID found in patch.");
        }
        int lenSrc = getLen(patch);
        if (lenSrc != src.capacity()) {
            throw new DiffException("Size of source differs from that in patch header");
        }
        int lenTrg = getLen(patch);
        int crcPatchSrc = patch.getInt();
        if (crcPatchSrc != (int) crc.getValue()) {
            throw new DiffException(String.format("CRC of source (0x%x) differs from that in patch header (0x%x).",
                    crc.getValue(), crcPatchSrc));
        }
        int crcTrg = patch.getInt();
        if (patch.getInt() != Diff.DATA_ID) {
            throw new DiffException("No data ID found in patch header.");
        }
        
        ByteBuffer trg = ByteBuffer.allocate(lenTrg).order(ByteOrder.LITTLE_ENDIAN);
        
        // step through patch buffer
        try {
            while (patch.position() < patch.capacity()) {
                int ofs = getLen(patch);
                out(String.format("Offset: %d", ofs));
                // copy bytes from source buffer
                byte[] ofsBuf = new byte[ofs];
                src.get(ofsBuf);
                trg.put(ofsBuf);
                // check for patch buffer empty
                if (patch.position() == patch.capacity()) {
                    break;
                }
                // now there must follow a command followed by a
                int cmdIdx = patch.position(); // just for exception
                int cmd = Byte.toUnsignedInt(patch.get());
                int len = getLen(patch);
                switch (cmd) {
                    case Diff.DELETE:
                        out(String.format("Delete: %d", len));
                        src.position(src.position() + len);
                        break;
                    case Diff.REPLACE:
                        out("Replace/");
                        src.position(src.position() + len);
                        /* falls through */
                    case Diff.INSERT:
                        out(String.format("Insert: %d", len));
                        byte[] lenBuf = new byte[len];
                        patch.get(lenBuf);
                        trg.put(lenBuf);
                        break;
                    case Diff.SUBSTITUTE:
                        int lenT = getLen(patch);
                        out(String.format("Substitute: %d/%d", len, lenT));
                        src.position(src.position() + len);
                        byte[] lenTBuf = new byte[lenT];
                        patch.get(lenTBuf);
                        trg.put(lenTBuf);
                        break;
                    default:
                        throw new DiffException(String.format("Unknown command %d at patch offset %d", cmd, cmdIdx));
                }
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new DiffException("Array index exceeds bounds. Patch file corrupt...");
        }
        
        // check length
        if (trg.position() != lenTrg) {
            throw new DiffException("Size of target differs from that in patch header.");
        }
        
        // compare crc
        crc.reset();
        crc.update(trg.array());
        if (crcTrg != (int) crc.getValue()) {
            throw new DiffException("CRC of target differs from that in patch.");
        }
        
        return trg.array();
    }
    
    /**
     * Lengths/Offsets are stored as 7-bit values. The 8th bit is used as marker if the number
     * is continued in the next byte.
     * @param b Buffer from which to read the length/offset
     * @return integer value of length/offset
     * @throws ArrayIndexOutOfBoundsException
     */
    private static int getLen(final ByteBuffer b) throws ArrayIndexOutOfBoundsException {
        int val = 0;
        int v;
        int shift = 0;
        do {
            v = Byte.toUnsignedInt(b.get());
            if ((v & 0x80) == 0) {
                // no continue bit set
                val |= (v << shift);
                break;
            }
            // erase continue marker bit
            v &= 0x7f;
            val |= (v << shift);
            shift += 7;
        } while (true);
        return val;
    }
    
    /**
     * Store length/offset information in 7-bit encoding. A set 8th bit means: continued in next byte
     * So 127 is stored as 0x7f, but 128 is stored as 0x80 0x01 (where 0x80 means 0, highest bit is marker)
     * @param l Patch list to add length/offset in 7-bit encoding
     * @param value Value to add in 7bit encoding
     */
    private static void writeLen(final ByteArrayOutputStream l, final int value) {
        int val = value;
        while (val > 0x7f) {
            l.write(val & 0x7f | 0x80);
            val >>>= 7;
        }
        l.write(val);
    }
    
    /**
     * Check for "insert" difference
     * @param src source buffer
     * @param trg target buffer
     * @return number of bytes inserted
     * @throws ArrayIndexOutOfBoundsException
     */
    private static int checkInsert(final ByteBuffer src, final ByteBuffer trg) throws ArrayIndexOutOfBoundsException {
        byte[] bs = src.array();
        int is = src.position();
        byte[] bt = trg.array();
        int it = trg.position();
        int len = windowLength;
        if (is + len + resyncLength >= bs.length) {
            len = bs.length - is - resyncLength;
        }
        if (it + len + resyncLength >= bt.length) {
            len = bt.length - it - resyncLength;
        }
        for (int w = 1; w < len; w++) {
            int r;
            for (r = 0; r < resyncLength; r++) {
                if (bs[is + r] != bt[it + w + r]) {
                    break;
                }
            }
            if (r == resyncLength) {
                return w;
            }
        }
        return Integer.MAX_VALUE;
    }
    
    /**
     * Check for "delete" difference
     * @param src source buffer
     * @param trg target buffer
     * @return number of bytes deleted
     * @throws ArrayIndexOutOfBoundsException
     */
    private static int checkDelete(final ByteBuffer src, final ByteBuffer trg) throws ArrayIndexOutOfBoundsException {
        byte[] bs = src.array();
        int is = src.position();
        byte[] bt = trg.array();
        int it = trg.position();
        int len = windowLength;
        if (is + len + resyncLength >= bs.length) {
            len = bs.length - is - resyncLength;
        }
        if (it + len + resyncLength >= bt.length) {
            len = bt.length - it - resyncLength;
        }
        for (int w = 1; w < len; w++) {
            int r;
            for (r = 0; r<resyncLength; r++) {
                if (bs[is + w + r] != bt[it + r]) {
                    break;
                }
            }
            if (r == resyncLength) {
                return w;
            }
        }
        return Integer.MAX_VALUE;
    }
    
    /**
     * Check for "replace" difference
     * @param src source buffer
     * @param trg target buffer
     * @return number of bytes replaced
     * @throws ArrayIndexOutOfBoundsException
     */
    private static int checkReplace(final ByteBuffer src, final ByteBuffer trg) throws ArrayIndexOutOfBoundsException {
        byte[] bs = src.array();
        int is = src.position();
        byte[] bt = trg.array();
        int it = trg.position();
        int len = windowLength;
        if (is + len + resyncLength >= bs.length) {
            len = bs.length - is - resyncLength;
        }
        if (it + len + resyncLength >= bt.length) {
            len = bt.length - it - resyncLength;
        }
        for (int w = 1; w < len; w++) {
            int r;
            for (r = 0; r<resyncLength; r++) {
                if (bs[is + w + r] != bt[it + w + r]) {
                    break;
                }
            }
            if (r == resyncLength) {
                return w;
            }
        }
        return Integer.MAX_VALUE;
    }
    
    /**
     * Check for "substitute" difference
     * @param src source buffer
     * @param trg target buffer
     * @return integer array: [0]: number of bytes to delete in source, [1]: number of bytes to insert in target
     * @throws ArrayIndexOutOfBoundsException
     */
    private static int[] checkSubstitute(final ByteBuffer src, final ByteBuffer trg) throws ArrayIndexOutOfBoundsException {
        byte[] bs = src.array();
        int is = src.position();
        byte[] bt = trg.array();
        int it = trg.position();
        int len = windowLength;
        if (is + len + resyncLength >= bs.length) {
            len = bs.length - is - resyncLength;
        }
        if (it + len + resyncLength >= bt.length) {
            len = bt.length - it - resyncLength;
        }
        
        List<int[]> solutions = new ArrayList<>();
        
        for (int ws = 1; ws < len; ws++) {
            for (int wt = 1; wt < len; wt++) {
                int r;
                for (r = 0; r < resyncLength; r++) {
                    if (bs[is + ws + r] != bt[it + wt + r]) {
                        break;
                    }
                }
                if (r == resyncLength) {
                    int[] retVal = new int[2];
                    retVal[0] = ws;
                    retVal[1] = wt;
                    solutions.add(retVal);
                }
            }
        }
        
        if (solutions.isEmpty()) {
            // nothing found
            int[] retVal = new int[2];
            retVal[0] = Integer.MAX_VALUE;
            retVal[1] = Integer.MAX_VALUE;
            return retVal;
        }
        
        // search best solution
        int sMinIdx = 0;
        for (int i = 1; i < solutions.size(); i++) {
            int[] s = solutions.get(i);
            int[] sMin = solutions.get(sMinIdx);
            if (s[0] + s[1] < sMin[0] + sMin[1]) {
                sMinIdx = i;
            }
        }
        return solutions.get(sMinIdx);
    }
    
    /**
     * Write int to difference list
     * @param l difference list
     * @param val int value
     */
    private static void writeInt(final ByteArrayOutputStream l, final int val) {
        l.write(val);
        l.write(val >> 8);
        l.write(val >> 16);
        l.write(val >> 24);
    }
    
    private static void out(final String s) {
        if (VERBATIM) {
            System.out.println(s);
        }
    }
}


/**
 * Generic Exception for Diff.
 * @author Volker Oth
 */
class DiffException extends Exception {
    
    /**
     * Constructor.
     */
    public DiffException() {
        super();
    }
    
    /**
     * Constructor.
     * @param s Exception string
     */
    public DiffException(String s) {
        super(s);
    }
}