package lemmini.game;

import java.nio.charset.StandardCharsets;
import lemmini.tools.ToolBox;
import org.apache.commons.lang3.BooleanUtils;

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
 * @author Volker Oth
 * Create and evaluate lemmings level codes
 * Based on the documentation and Basic code of Herman Perk (LemGen)
 */
public class LevelCode {
    //  nibble 0  | nibble 1  | nibble 2  | nibble 3  | nibble 4  | nibble 5  | nibble 6
    // -----------|-----------|-----------|-----------|-----------|-----------|-----------
    //  3  2  1  0| 3  2  1  0| 3  2  1  0| 3  2  1  0| 3  2  1  0| 3  2  1  0| 3  2  1  0
    // -----------|-----------|-----------|-----------|-----------|-----------|-----------
    // L0 %0 F0 U0|U1 L1  0 %1|U2 L2 %2 F1|U4 U3 %3 L3|%4 U5 L4 F2|U6 %5 F3 L5|L7 L6  0 %6

    /* level: L */
    private static final int[] LMASK   = {1, 2, 4,  8, 16, 32, 192};
    private static final int[] LSHIFTL = {3, 1, 0,  0,  0,  0,   0};
    private static final int[] LSHIFTR = {0, 0, 0,  3,  3,  5,   4};
    /* percent: % */
    private static final int[] PMASK   = {1, 2, 4,  8, 16, 32,  64};
    private static final int[] PSHIFTL = {2, 0, 0,  0,  0,  0,   0};
    private static final int[] PSHIFTR = {0, 1, 1,  2,  1,  3,   6};
    /* failed: F */
    private static final int[] FMASK   = {1, 0, 2,  0,  4,  8,   0};
    private static final int[] FSHIFTL = {1, 0, 0,  0,  0,  0,   0};
    private static final int[] FSHIFTR = {0, 0, 1,  0,  2,  2,   0};
    /* unknown: U */
    private static final int[] UMASK   = {1, 2, 4, 24, 32, 64,   0};
    private static final int[] USHIFTL = {0, 2, 1,  0,  0,  0,   0};
    private static final int[] USHIFTR = {0, 0, 0,  1,  3,  3,   0};
    
    private static final int MAX_LVL_NUM = 255;
    private static final int MAX_PERCENT = 127;
    private static final int MAX_FAILED = 15;
    private static final int MAX_UNKNOWN = 127;
    
    private static final int FIRST_LETTER = 0x41;
    private static final int LAST_LETTER = 0x5A;


    /**
     * Create a level code from the given parameters
     * @param seed The seed string used as base for the level code
     * @param lvl The level number (0-255)
     * @param percent Percentage of levels saved in the level won to get this code
     * @param failed The number of times that the level was failed
     * @param unknown
     * @param offset Used to get a higher code for the first level
     * @return String containing level code
     */
    public static String create(final String seed, final int lvl, int percent,
            int failed, int unknown, final int offset) {
        if (lvl > MAX_LVL_NUM || lvl < 0 || seed == null || seed.length() != 10) {
            return null;
        }
        for (int i = 0; i < 10; i++) {
            if (seed.charAt(i) < FIRST_LETTER || seed.charAt(i) > LAST_LETTER) {
                return null;
            }
        }
        percent = ToolBox.cap(0, percent, MAX_PERCENT);
        failed = ToolBox.cap(0, failed, MAX_FAILED);
        unknown = ToolBox.cap(0, unknown, MAX_UNKNOWN);
        byte[] bi;
        bi = seed.getBytes(StandardCharsets.US_ASCII);
        byte[] bo = new byte[bi.length];

        // add offset and wrap around
        int level = lvl + offset;
        level %= (MAX_LVL_NUM + 1);

        // create first 7 bytes
        int sum = 0;
        for (int i = 0; i < 7; i++) {
            bi[i] |= (byte) (((level & LMASK[i]) << LSHIFTL[i]) >>> LSHIFTR[i]);
            bi[i] |= (byte) (((percent & PMASK[i]) << PSHIFTL[i]) >>> PSHIFTR[i]);
            bi[i] |= (byte) (((failed & FMASK[i]) << FSHIFTL[i]) >>> FSHIFTR[i]);
            bi[i] |= (byte) (((unknown & UMASK[i]) << USHIFTL[i]) >>> USHIFTR[i]);
            if (bi[i] > LAST_LETTER) {
                bi[i] -= 26;
            }
            bo[(i + 8 - (level % 8)) % 7] = bi[i]; // rotate
            sum += Byte.toUnsignedInt(bi[i]); // checksum
        }
        // create 8th and 9th bytes (level)
        bo[7] = (byte) (bi[7] + (level & 0xf));
        if (bo[7] > LAST_LETTER) {
            bo[7] -= 26;
        }
        bo[8] = (byte) (bi[8] + ((level & 0xf0) >> 4));
        if (bo[8] > LAST_LETTER) {
            bo[8] -= 26;
        }
        sum += Byte.toUnsignedInt(bo[7]) + Byte.toUnsignedInt(bo[8]);
        // create 10th byte (checksum)
        bo[9] = (byte) (bi[9] + (sum & 0x0f));
        if (bo[9] > LAST_LETTER) {
            bo[9] -= 26;
        }
        return new String(bo, StandardCharsets.US_ASCII);
    }

    /**
     * Extract the level info from the level code and seed
     * @param seed The seed string used as base for the level code
     * @param code Code that contains the level number (amongst other things)
     * @param offset Used to get a higher code for the first level
     * @return Array if ints containing level info (null in case of error)
     */
    public static int[] getLevel(final String seed, final String code, final int offset) {
        byte[] bs;
        byte[] bi;
        bs = seed.getBytes(StandardCharsets.US_ASCII);
        bi = code.getBytes(StandardCharsets.US_ASCII);
        byte[] bo = new byte[bi.length];

        if (seed.length() != 10 || code.length() != 10) {
            return null;
        }
        for (int i = 0; i < 10; i++) {
            if (seed.charAt(i) < FIRST_LETTER || seed.charAt(i) > LAST_LETTER
                    || code.charAt(i) < FIRST_LETTER || code.charAt(i) > LAST_LETTER) {
                return null;
            }
        }
        
        // verify checksum
        if (bi[9] < bs[9]) {
            bi[9] += 26;
        } 
        if (((bi[0] + bi[1] + bi[2] + bi[3] + bi[4] + bi[5] + bi[6] + bi[7] + bi[8]) & 0xf) != bi[9] - bs[9]) {
            return null;
        }
        
        for (int i = 7; i < 9; i++) {
            if (bi[i] < bs[i]) {
                bi[i] += 26;
            }
        }
        int level = ((bi[7] - bs[7]) & 0xf) | (((bi[8] - bs[8]) & 0xf) << 4);
        
        // unrotate
        for (int i = 0; i < 7; i++) {
            bo[(i + 6 + (level % 8)) % 7] = bi[i];
        }
        for (int i = 0; i < 7; i++) {
            if (bi[i] < bs[i]) {
                bi[i] += 26;
            }
        }
        
        // check bits that must be 0
        if (BooleanUtils.toBoolean((bo[1] - bs[1]) & 2) || BooleanUtils.toBoolean((bo[6] - bs[6]) & 2)) {
            return null;
        }
        
        // decode
        int level_ = 0;
        int percent = 0;
        int failed = 0;
        int unknown = 0;
        for (int i = 0; i < 7; i++) {
            int nibble = (bo[i] - bs[i]) & 0xff; // reconstruct nibble stored
            level_ += ((nibble << LSHIFTR[i]) >> LSHIFTL[i]) & LMASK[i];
            percent += ((nibble << PSHIFTR[i]) >> PSHIFTL[i]) & PMASK[i];
            failed += ((nibble << FSHIFTR[i]) >> FSHIFTL[i]) & FMASK[i];
            unknown += ((nibble << USHIFTR[i]) >> USHIFTL[i]) & UMASK[i];
        }
        if (level != level_) {
            return null;
        }
        
        level -= offset;
        while (level < 0) {
            level += MAX_LVL_NUM;
        }

        int[] ret = {level, percent, failed, unknown};
        return ret;
    }
}
