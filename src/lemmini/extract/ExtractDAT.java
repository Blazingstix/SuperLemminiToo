/*
 * Copyright 2014 Ryan Sakowski.
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
package lemmini.extract;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.BooleanUtils;

/**
 * Extracts files from Lemmings DAT files.
 * @author Ryan Sakowski
 */
public class ExtractDAT {
    
    public static byte[][] decompress(Path source) throws Exception {
        List<byte[]> decompressedSections = new ArrayList<>(32);
        try (SeekableByteChannel datFile = Files.newByteChannel(source)) {
            do {
                if (datFile.position() == datFile.size()) {
                    // End of DAT file reached, so break here.
                    break;
                }
                DATSection section = DATSection.getDATSection(datFile);
                if (section == null) {
                    throw new Exception(String.format("%s is not a valid Lemmings DAT file.", source));
                }
                
                byte[] decompressedSection = section.decompress();
                decompressedSections.add(decompressedSection);
            } while (true);
        } catch (IOException e) {
            throw new Exception(String.format("I/O error while reading %s.", source));
        }
        return decompressedSections.toArray(new byte[decompressedSections.size()][]);
    }
}

/**
 * Class representing a compressed section of a Lemmings DAT file. Apart from
 * the header, all of the compressed data is read "backwards"--decompression
 * starts at the last byte, and the last byte of the decompressed data is
 * extracted first. For further information, see ccexplore's documentation of
 * the DAT format here:
 * http://www.camanis.net/lemmings/files/docs/lemmings_dat_file_format.txt
 * @author Ryan Sakowski
 */
class DATSection {
    
    /**
     * The size of the header--always 10 bytes.
     */
    private static final int HEADER_SIZE = 10;
    
    /**
     * Byte array representing the compressed data.
     */
    private final byte[] compressedData;
    /** 
     * Number of bits to read from the first read byte (that is, the last one
     * in the array) of the compressed data.
     */
    private final int numBitsInFirstByte;
    /**
     * Size of the decompressed data.
     */
    private final int decompressedDataSize;
    /** 
     * Current byte index into compressedData. Reading starts at the last
     * byte of the compressed data.
     */
    private int index;
    /**
     * Current bit index into the current byte. The least significant bit of
     * each byte of the compressed data is read first.
     */
    private int bitIndex;
    
    /**
     * Constructor for DatSection.
     * @param compressedData
     * @param sizeInBits
     * @param decompressedDataSize 
     */
    DATSection(byte[] compressedData, int numBitsInFirstByte, int decompressedDataSize) {
        this.compressedData = compressedData;
        this.numBitsInFirstByte = numBitsInFirstByte;
        this.decompressedDataSize = decompressedDataSize;
        resetIndex();
    }
    
    /**
     * Static method for creating a DATSection from a ByteChannel.
     * @param bc the ByteChannel to read from
     * @return DATSection if the section was read successfully, null otherwise.
     * @throws IOException 
     */
    static DATSection getDATSection(ByteChannel bc) throws IOException {
        // Read the header.
        ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE);
        if (bc.read(buffer) != HEADER_SIZE) {
            return null;
        }
        buffer.rewind();
        byte numBitsInFirstByte = buffer.get();
        byte targetChecksum = buffer.get();
        int decompressedDataSize = buffer.getInt();
        int compressedDataSize = buffer.getInt() - HEADER_SIZE;
        
        // Make sure that numBitsInFirstByte is in the range of 0-8.
        // Technically, it shouldn't be 0, but that can be handled easily.
        if (numBitsInFirstByte < 0 || numBitsInFirstByte > 8) {
            return null;
        }
        
        // Read the compressed data.
        buffer = ByteBuffer.allocate(compressedDataSize);
        if (bc.read(buffer) != compressedDataSize) {
            return null;
        }
        byte[] compressedData = buffer.array();
        
        // Calculate the compressed data's checksum by XORing all of its bytes,
        // and then make sure that the checksum matches the one in the header.
        byte sectionChecksum = 0;
        for (byte b : compressedData) {
            sectionChecksum ^= b;
        }
        if (sectionChecksum != targetChecksum) {
            return null;
        }
        
        return new DATSection(compressedData, numBitsInFirstByte, decompressedDataSize);
    }
    
    /**
     * Returns a byte array containing the decompressed data of this section.
     * @return a byte array containing the decompressed data
     */
    byte[] decompress() {
        byte[] decompressedData = new byte[decompressedDataSize];
        // Bytes are decompressed in reverse order, so dIndex must start at the
        // last index of the array.
        int dIndex = decompressedData.length - 1;
        while (dIndex >= 0) {
            int byteCount;
            int offset;
            // Read the next 2 or 3 bits to determine the decompression method.
            // Unless otherwise noted, all values extracted require 1 to be
            // added for the correct values to be obtained.
            switch (nextValue(1)) {
                case 0b0:
                    switch (nextValue(1)) {
                        case 0b0:
                            // Method 00: Extract a number of bytes from the
                            // compressed data. The number of bytes to extract
                            // is indicated by the next 3-byte value.
                            byteCount = nextValue(3) + 1;
                            extractBytes(decompressedData, dIndex, byteCount);
                            dIndex -= byteCount;
                            break;
                        case 0b1:
                            // Method 01: Copy two previously decompressed
                            // consecutive bytes to the current position. The
                            // next 8-bit value determines the offset to the
                            // previous bytes.
                            offset = nextValue(8) + 1;
                            copyBytes(decompressedData, dIndex, 2, offset);
                            dIndex -= 2;
                            break;
                        default:
                            break;
                    }
                    break;
                case 1:
                    switch (nextValue(2)) {
                        case 0b00:
                            // Method 100: Similar to method 01, except the
                            // offset is a 9-bit value, and three bytes must be
                            // copied.
                            offset = nextValue(9) + 1;
                            copyBytes(decompressedData, dIndex, 3, offset);
                            dIndex -= 3;
                            break;
                        case 0b01:
                            // Method 101: Similar to methods 01 and 100,
                            // except the offset is a 10-bit value, and four
                            // bytes must be copied.
                            offset = nextValue(10) + 1;
                            copyBytes(decompressedData, dIndex, 4, offset);
                            dIndex -= 4;
                            break;
                        case 0b10:
                            // Method 110: Similar to methods 01, 100, and 101,
                            // except the number of bytes to copy is determined 
                            // by the next 8-bit value, and the offset (which
                            // is read after the byte count) is a 12-bit value.
                            byteCount = nextValue(8) + 1;
                            offset = nextValue(12) + 1;
                            copyBytes(decompressedData, dIndex, byteCount, offset);
                            dIndex -= byteCount;
                            break;
                        case 0b11:
                            // Method 111: Similar to method 00, except the
                            // byte count is an 8-bit value, and 9 must be
                            // added to it instead of 1.
                            byteCount = nextValue(8) + 9;
                            extractBytes(decompressedData, dIndex, byteCount);
                            dIndex -= byteCount;
                            break;
                        default:
                            break;
                    }
                    break;
                default:
                    break;
            }
        }
        resetIndex();
        return decompressedData;
    }
    
    /**
     * Resets the index to the end of the compressed data in preparation for
     * decompression.
     */
    private void resetIndex() {
        index = compressedData.length - ((numBitsInFirstByte == 0) ? 2 : 1);
        bitIndex = 0;
    }
    
    /**
     * Reads a bit from the compressed data and advances the index.
     * @return true if the bit is set to 1, false if it's set to 0
     */
    private boolean nextBit() {
        boolean retBool = BooleanUtils.toBoolean(compressedData[index] & (1 << bitIndex));
        bitIndex++;
        if (bitIndex >= 8 || (index == compressedData.length - 1 && bitIndex >= numBitsInFirstByte)) {
            // All bits read from this byte; advance (decrease) the byte index
            // and reset the bit index.
            index--;
            bitIndex = 0;
        }
        return retBool;
    }
    
    /**
     * Extracts a value of the specified size from the compressed data and
     * advances the index. The most significant bit is read first.
     * @param size the number of bits to read
     * @return the value extracted from the compressed data
     */
    private int nextValue(int size) {
        int retValue = 0;
        for (int i = size - 1; i >= 0; i--) {
            retValue |= (nextBit() ? 1 : 0) << i;
        }
        return retValue;
    }
    
    /**
     * Extracts a byte from the compressed data. Equivalent to calling
     * nextValue(8) and casting to byte.
     * @return the byte extracted from the compressed data
     */
    private byte nextByte() {
        return (byte) nextValue(8);
    }
    
    /**
     * Extracts the specified number of bytes from the compressed data into the
     * given array. The first byte is copied to the specified index, and each
     * subsequent byte is copied to the previous index.
     * @param data the array to extract bytes to
     * @param idx the index into the given array to start extracting bytes to
     * @param count the number of bytes to extract
     */
    private void extractBytes(byte[] data, int idx, int count) {
        for (int i = 0; i < count; i++) {
            data[idx--] = nextByte();
        }
    }
    
    /**
     * Copies the specified number of bytes from one index of the given array
     * to another index. The first byte is copied from the specified offest to
     * the specified index, and each subsequent byte is copied from the
     * previous offset to the previous index.
     * @param data the array containing bytes to copy
     * @param idx the first index to copy to
     * @param count the number of bytes to copy
     * @param offset the offset from the index to copy from
     */
    private void copyBytes(byte[] data, int idx, int count, int offset) {
        if (offset >= count) {
            // Source and destination regions don't overlap; use
            // System.arraycopy().
            System.arraycopy(data, idx + offset - (count - 1), data, idx - (count - 1), count);
        } else {
            // Source and destination regions overlap; System.arraycopy() won't
            // give the correct result here, so copy one byte at a time.
            for (int i = 0; i < count; i++) {
                data[idx] = data[idx + offset];
                idx--;
            }
        }
    }
}
