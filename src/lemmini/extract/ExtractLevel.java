package lemmini.extract;

import com.ibm.icu.lang.UCharacter;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lemmini.tools.ToolBox;
import static org.apache.commons.lang3.BooleanUtils.toBoolean;
import org.apache.commons.lang3.StringUtils;

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
 * Convert binary "Lemmings for Win95" level files into text format.
 */
public class ExtractLevel {
    
    /** Scale (to convert lowres levels into hires levels) */
    private static final int SCALE = 2;
    private static final int DEFAULT_WIDTH = 1584;
    private static final int DEFAULT_HEIGHT = 160;
    private static final int MINIMUM_WIDTH = 320;
    private static final int MINIMUM_HEIGHT = 160;
    static final int FAKE_OBJECT_CUTOFF = 16;
    private static final int MAX_ENTRANCES = 4;
    private static final int MAX_ENTRANCES_MULTI = 2;
    private static final int MAX_GREEN_FLAGS = 1;
    
    /** names for default styles */
    private static final Map<Integer, String> STYLES = new HashMap<>();
    /** names for default special styles */
    private static final Map<Integer, String> SPECIAL_STYLES = new HashMap<>();
    private static final Map<Integer, String> MUSIC = new HashMap<>();
    static {
        STYLES.put(0, "dirt");
        STYLES.put(1, "fire");
        STYLES.put(2, "marble");
        STYLES.put(3, "pillar");
        STYLES.put(4, "crystal");
        STYLES.put(5, "brick");
        STYLES.put(6, "rock");
        STYLES.put(7, "snow");
        STYLES.put(8, "bubble");
        STYLES.put(9, "xmas");
        
        SPECIAL_STYLES.put(0, "awesome");
        SPECIAL_STYLES.put(1, "menace");
        SPECIAL_STYLES.put(2, "beastii");
        SPECIAL_STYLES.put(3, "beasti");
        SPECIAL_STYLES.put(4, "covox");
        SPECIAL_STYLES.put(5, "prima");
        SPECIAL_STYLES.put(15, "apple");
        SPECIAL_STYLES.put(101, "apple");
        
        MUSIC.put(1, "cancan.mod");
        MUSIC.put(2, "lemming1.mod");
        MUSIC.put(3, "tim2.mod");
        MUSIC.put(4, "lemming2.mod");
        MUSIC.put(5, "tim8.mod");
        MUSIC.put(6, "tim3.mod");
        MUSIC.put(7, "tim5.mod");
        MUSIC.put(8, "doggie.mod");
        MUSIC.put(9, "tim6.mod");
        MUSIC.put(10, "lemming3.mod");
        MUSIC.put(11, "tim7.mod");
        MUSIC.put(12, "tim9.mod");
        MUSIC.put(13, "tim1.mod");
        MUSIC.put(14, "tim10.mod");
        MUSIC.put(15, "tim4.mod");
        MUSIC.put(16, "tenlemms.mod");
        MUSIC.put(17, "mountain.mod");
        MUSIC.put(18, "tune1.mod");
        MUSIC.put(19, "tune2.mod");
        MUSIC.put(20, "tune3.mod");
        MUSIC.put(21, "tune4.mod");
        MUSIC.put(22, "tune5.mod");
        MUSIC.put(23, "tune6.mod");
    }
    
    private static final int GIMMICK_FLAG_SUPERLEMMING = 1;
    private static final int GIMMICK_FLAG_CHEAPO_FALL_DISTANCE = 1 << 30;
    
    private static final int SKILL_FLAG_CLIMBER = 1 << 14;
    private static final int SKILL_FLAG_FLOATER = 1 << 12;
    private static final int SKILL_FLAG_BOMBER = 1 << 9;
    private static final int SKILL_FLAG_BLOCKER = 1 << 7;
    private static final int SKILL_FLAG_BUILDER = 1 << 5;
    private static final int SKILL_FLAG_BASHER = 1 << 3;
    private static final int SKILL_FLAG_MINER = 1 << 2;
    private static final int SKILL_FLAG_DIGGER = 1 << 1;
    
    private static final int OPTION_FLAG_NEGATIVE_STEEL = 1;
    private static final int OPTION_FLAG_AUTOSTEEL = 1 << 1;
    private static final int OPTION_FLAG_IGNORE_STEEL = 1 << 2;
    private static final int OPTION_FLAG_SIMPLE_AUTOSTEEL = 1 << 3;
    private static final int OPTION_FLAG_CUSTOM_GIMMICKS = 1 << 5;
    private static final int OPTION_FLAG_CUSTOM_SKILL_SET = 1 << 6;
    private static final int OPTION_FLAG_INVERT_ONE_WAY = 1 << 7;
    
    /**
     * Convert one binary LVL file into text file
     * @param fnIn Name of binary LVL file
     * @param fnOut Name of target text file
     * @param multi Whether this is a multiplayer level
     * @param classic Whether to convert in classic mode
     * @throws Exception
     */
    public static void convertLevel(final Path fnIn, final Path fnOut, final boolean multi, final boolean classic) throws Exception {
        byte[] b;
        try {
            if (!Files.isRegularFile(fnIn)) {
                throw new Exception(String.format("File %s not found.", fnIn));
            }
            long fileSize = Files.size(fnIn);
            if (fileSize != 2048L && fileSize != 10240L) {
                throw new Exception("Lemmings level files must be 2,048 or 10,240 bytes in size!");
            }
            b = Files.readAllBytes(fnIn);
        } catch (IOException e) {
            throw new Exception(String.format("I/O error while reading %s.", fnIn));
        }
        convertLevel(b, fnIn.getFileName().toString(), fnOut, multi, classic);
    }

    /**
     * Convert one binary LVL file into text file
     * @param in Byte array of binary LVL file
     * @param fName File name
     * @param fnOut Name of target text file
     * @param multi Whether this is a multiplayer level
     * @param classic Whether to convert in classic mode
     * @throws Exception
     */
    public static void convertLevel(final byte[] in, final String fName, final Path fnOut, final boolean multi, final boolean classic) throws Exception {
        int format = 0;
        /* release rate : 0 is slowest, 0x0FA (250) is fastest */
        int releaseRate = 0;
        /* number of Lemmings in this level (maximum 0x0072 in original LVL format) */
        int numLemmings = 0;
        /* number of Lemmings to rescue : should be less than or equal to number of Lemmings */
        int numToRescue = 0;
        /* time limit in seconds */
        int timeLimitSeconds = 0;
        /* time limit in minutes: max 0x00FF, 0x0001 to 0x0009 works best */
        int timeLimit = 0;
        int gimmickFlags = 0;
        /* number of climbers in this level : max 0xfa (250) */
        int numClimbers = 0;
        /* number of floaters in this level : max 0xfa (250) */
        int numFloaters = 0;
        /* number of bombers in this level : max 0xfa (250) */
        int numBombers = 0;
        /* number of blockers in this level : max 0xfa (250) */
        int numBlockers = 0;
        /* number of builders in this level : max 0xfa (250) */
        int numBuilders = 0;
        /* number of bashers in this level : max 0xfa (250) */
        int numBashers = 0;
        /* number of miners in this level : max 0xfa (250) */
        int numMiners = 0;
        /* number of diggers in this level : max 0xfa (250) */
        int numDiggers = 0;
        /* start screen x pos : 0 - 0x04f0 (1264) rounded to modulo 8 */
        int xPos = 0;
        int yPos = 80 * SCALE;
        int optionFlags = 0;
        /*
         * 0x0000 is dirt,  <br>0x0001 is fire,   <br>0x0002 is marble,  <br>
         * 0x0003 is pillar,<br>0x0004 is crystal,<br>0x0005 is brick,   <br>
         * 0x0006 is rock,  <br>0x0007 is snow,   <br>0x0008 is bubble,  <br>
         * 0x0009 is xmas
         */
        int style;
        String styleStr = null;
        /* special style */
        int specialStyle = -1;
        String specialStyleStr = null;
        int specialStylePositionX = 0;
        int specialStylePositionY = 0;
        Integer music = 0;
        String musicStr = null;
        int extra1 = 0;
        int extra2 = 0;
        /* objects like doors - 32 objects each consists of 8 bytes */
        LvlObject[] objects;
        byte[][] objBytes;
        /* terrain the Lemmings walk on etc. - 400 tiles, 4 bytes each */
        Terrain[] terrain;
        byte[][] terBytes;
        /* steel areas which are indestructible - 32 objects, 4 bytes each */
        Steel[] steel;
        byte[][] stlBytes;
        List<Integer> entranceOrder = new ArrayList<>(32);
        List<Integer> remappedEntranceOrder = new ArrayList<>(32);
        int width = DEFAULT_WIDTH * SCALE;
        int height = DEFAULT_HEIGHT * SCALE;
        String origLvlName = null;
        /* 32 byte level name - filled with whitespaces */
        String lvlName = StringUtils.EMPTY;
        String author = StringUtils.EMPTY;
        int entranceCount = 0;
        int activeEntranceCount = 0;
        int greenFlagCount = 0;
        
        // read file into buffer
        if (in.length != 2048 && in.length != 10240) {
            throw new Exception("Lemmings level files must be 2,048 or 10,240 bytes in size!");
        }
        ByteBuffer b = ByteBuffer.wrap(in).asReadOnlyBuffer();
        // analyze buffer
        if (classic) {
            if (in.length != 2048L) {
                throw new Exception("Format 0 level files must be 2,048 bytes in size!");
            }
            // read configuration in big endian word
            releaseRate = b.getShort();
            if (releaseRate >= 100) {
                releaseRate -= 65536;
            }
            numLemmings = b.getShort() & 0xffff;
            numToRescue = b.getShort() & 0xffff;
            timeLimit = b.getShort() & 0xffff;
            numClimbers = b.getShort() & 0xffff;
            numFloaters = b.getShort() & 0xffff;
            numBombers = b.getShort() & 0xffff;
            numBlockers = b.getShort() & 0xffff;
            numBuilders = b.getShort() & 0xffff;
            numBashers = b.getShort() & 0xffff;
            numMiners = b.getShort() & 0xffff;
            numDiggers = b.getShort() & 0xffff;
            xPos = b.getShort() & 0xffff;
            xPos += multi ? 72 : 160;
            xPos *= SCALE;
            style = b.getShort() & 0xffff;
            styleStr = STYLES.get(style);
            if (styleStr == null) {
                throw new Exception(String.format("%s uses an invalid style: %d", fName, style));
            }
            specialStyle = (b.getShort() & 0xffff) - 1;
            if (specialStyle > -1) {
                specialStyleStr = SPECIAL_STYLES.get(specialStyle);
                if (specialStyleStr == null) {
                    throw new Exception(String.format("%s uses an invalid special style: %d", fName, specialStyle));
                }
            }
            extra1 = b.get();
            extra2 = b.get();
        } else {
            int skillFlags = 0;
            int[] skillCounts;
            format = b.get();
            switch (format) {
                case 0:
                    if (in.length != 2048L) {
                        throw new Exception("Format 0 level files must be 2,048 bytes in size!");
                    }
                    releaseRate = b.get();
                    if (releaseRate >= 100) {
                        releaseRate -= 256;
                    }
                    numLemmings = b.getShort() & 0xffff;
                    numToRescue = b.getShort() & 0xffff;
                    timeLimitSeconds = b.get() & 0xff;
                    timeLimit = b.get() & 0xff;
                    if (timeLimit * 60 + timeLimitSeconds >= 6000) {
                        timeLimitSeconds = 0;
                        timeLimit = 0;
                    }
                    skillCounts = new int[8];
                    gimmickFlags = (b.get() & 0xff) << 24;
                    skillCounts[0] = b.get() & 0xff;
                    gimmickFlags |= (b.get() & 0xff) << 16;
                    skillCounts[1] = b.get() & 0xff;
                    gimmickFlags |= (b.get() & 0xff) << 8;
                    skillCounts[2] = b.get() & 0xff;
                    gimmickFlags |= b.get() & 0xff;
                    skillCounts[3] = b.get() & 0xff;
                    b.get();
                    skillCounts[4] = b.get() & 0xff;
                    b.get();
                    skillCounts[5] = b.get() & 0xff;
                    skillFlags = (b.get() & 0xff) << 8;
                    skillCounts[6] = b.get() & 0xff;
                    skillFlags |= b.get() & 0xff;
                    skillCounts[7] = b.get() & 0xff;
                    for (int i = 0; i < skillCounts.length; i++) {
                        if (skillCounts[i] >= 100) {
                            skillCounts[i] = Integer.MAX_VALUE;
                        }
                    }
                    xPos = b.getShort() & 0xffff;
                    xPos += multi ? 72 : 160;
                    xPos *= SCALE;
                    music = b.get() & 0xff;
                    if (music > 0 && music < 253) {
                        musicStr = MUSIC.get(music);
                        if (musicStr == null) {
                            throw new Exception(String.format("%s uses an invalid music index: %d", fName, music));
                        }
                    }
                    style = b.get() & 0xff;
                    styleStr = STYLES.get(style);
                    if (styleStr == null) {
                        throw new Exception(String.format("%s uses an invalid style: %d", fName, style));
                    }
                    optionFlags = b.get();
                    specialStyle = (b.get() & 0xff) - 1;
                    if (specialStyle > -1) {
                        specialStyleStr = SPECIAL_STYLES.get(specialStyle);
                        if (specialStyleStr == null) {
                            throw new Exception(String.format("%s uses an invalid special style: %d", fName, specialStyle));
                        }
                    }
                    extra1 = b.get();
                    extra2 = b.get();
                    break;
                case 1:
                case 2:
                case 3:
                    if (in.length != 10240L) {
                        throw new Exception("Format 1, 2, and 3 level files must be 10,240 bytes in size!");
                    }
                    b.order(ByteOrder.LITTLE_ENDIAN);
                    if (format >= 3) {
                        music = b.get() & 0xff;
                    } else {
                        b.get();
                    }
                    numLemmings = b.getShort() & 0xffff;
                    numToRescue = b.getShort() & 0xffff;
                    timeLimitSeconds = b.getShort() & 0xffff;
                    if (timeLimitSeconds >= 6000) {
                        timeLimitSeconds = 0;
                    }
                    releaseRate = b.get();
                    if (releaseRate >= 100) {
                        releaseRate -= 256;
                    }
                    optionFlags = b.get();
                    style = b.get() & 0xff;
                    specialStyle = (b.get() & 0xff) - 1;
                    if (format >= 2) {
                        xPos = b.getShort() & 0xffff;
                        xPos += multi ? 72 : 160;
                        xPos *= SCALE;
                        yPos = (b.getShort() & 0xffff) + 80;
                        yPos *= SCALE;
                    } else {
                        music = b.get() & 0xff;
                        b.get();
                        xPos = b.getShort() & 0xffff;
                        xPos += multi ? 72 : 160;
                        xPos *= SCALE;
                    }
                    if (music > 0 && music < 253) {
                        musicStr = MUSIC.get(music);
                        if (musicStr == null) {
                            throw new Exception(String.format("%s uses an invalid music index: %d", fName, music));
                        }
                    }
                    skillCounts = new int[16];
                    skillCounts[0] = b.get() & 0xff;
                    skillCounts[1] = b.get() & 0xff;
                    skillCounts[2] = b.get() & 0xff;
                    skillCounts[3] = b.get() & 0xff;
                    skillCounts[4] = b.get() & 0xff;
                    skillCounts[5] = b.get() & 0xff;
                    skillCounts[6] = b.get() & 0xff;
                    skillCounts[7] = b.get() & 0xff;
                    skillCounts[8] = b.get() & 0xff;
                    skillCounts[9] = b.get() & 0xff;
                    skillCounts[10] = b.get() & 0xff;
                    skillCounts[11] = b.get() & 0xff;
                    skillCounts[12] = b.get() & 0xff;
                    skillCounts[13] = b.get() & 0xff;
                    skillCounts[14] = b.get() & 0xff;
                    skillCounts[15] = b.get() & 0xff;
                    for (int i = 0; i < skillCounts.length; i++) {
                        if (skillCounts[i] >= 100) {
                            skillCounts[i] = Integer.MAX_VALUE;
                        }
                    }
                    gimmickFlags = b.getInt();
                    skillFlags = b.getShort() & 0xffff;
                    b.get();
                    b.get();
                    width = StrictMath.max(DEFAULT_WIDTH + b.getShort(), MINIMUM_WIDTH) * SCALE;
                    height = StrictMath.max(DEFAULT_HEIGHT + b.getShort(), MINIMUM_HEIGHT) * SCALE;
                    specialStylePositionX = b.getShort() * SCALE;
                    specialStylePositionY = b.getShort() * SCALE;
                    byte[] bString = new byte[16];
                    b.get(bString);
                    author = new String(bString, StandardCharsets.US_ASCII).trim();
                    bString = new byte[32];
                    b.get(bString);
                    lvlName = new String(bString, StandardCharsets.US_ASCII);
                    if (format >= 3) {
                        bString = new byte[16];
                        b.get(bString);
                        String strTemp = UCharacter.toLowerCase(Locale.ROOT, new String(bString, StandardCharsets.US_ASCII).trim());
                        if (!strTemp.isEmpty()) {
                            styleStr = strTemp;
                        }
                        bString = new byte[16];
                        b.get(bString);
                        strTemp = UCharacter.toLowerCase(Locale.ROOT, new String(bString, StandardCharsets.US_ASCII).trim());
                        if (strTemp.equals("none")) {
                            specialStyle = -1;
                            specialStyleStr = null;
                        } else if (!strTemp.isEmpty()) {
                            specialStyleStr = strTemp;
                        }
                    } else {
                        b.position(b.position() + 32);
                    }
                    if (styleStr == null && style != 255) {
                        styleStr = STYLES.get(style);
                        if (styleStr == null) {
                            throw new Exception(String.format("%s uses an invalid style: %d", fName, style));
                        }
                    }
                    if (specialStyleStr == null && specialStyle != 255 && specialStyle > -1) {
                        specialStyleStr = SPECIAL_STYLES.get(specialStyle);
                        if (specialStyleStr == null) {
                            throw new Exception(String.format("%s uses an invalid special style: %d", fName, specialStyle));
                        }
                    }
                    if (format >= 3) {
                        for (int i = 0; i < 32; i++) {
                            int entranceIndex = b.get() & 0xff;
                            if (toBoolean(entranceIndex & 0x80)) {
                                entranceOrder.add(entranceIndex & 0x7f);
                            }
                        }
                    } else {
                        b.position(b.position() + 32);
                    }
                    b.position(b.position() + 32);
                    break;            
                default:
                    throw new Exception(String.format("Unsupported level format: %d", format));
            }
            if (format >= 1 || toBoolean(optionFlags & OPTION_FLAG_CUSTOM_SKILL_SET)) {
                int skillIndex = 15;
                int numSkills = 0;
                int skillCountIndex;
                while (skillIndex >= 0 && numSkills < 8) {
                    switch (format) {
                        case 0:
                            skillCountIndex = numSkills;
                            break;
                        case 1:
                        case 2:
                        case 3:
                            skillCountIndex = skillCounts.length - 1 - skillIndex;
                            break;
                        default:
                            throw new Exception(String.format("Unsupported level format: %d", format));
                    }
                    switch (1 << skillIndex) {
                        case SKILL_FLAG_CLIMBER:
                            if (toBoolean(skillFlags & SKILL_FLAG_CLIMBER)) {
                                numClimbers = skillCounts[skillCountIndex];
                                numSkills++;
                            } else {
                                numClimbers = 0;
                            }
                            break;
                        case SKILL_FLAG_FLOATER:
                            if (toBoolean(skillFlags & SKILL_FLAG_FLOATER)) {
                                numFloaters = skillCounts[skillCountIndex];
                                numSkills++;
                            } else {
                                numFloaters = 0;
                            }
                            break;
                        case SKILL_FLAG_BOMBER:
                            if (toBoolean(skillFlags & SKILL_FLAG_BOMBER)) {
                                numBombers = skillCounts[skillCountIndex];
                                numSkills++;
                            } else {
                                numBombers = 0;
                            }
                            break;
                        case SKILL_FLAG_BLOCKER:
                            if (toBoolean(skillFlags & SKILL_FLAG_BLOCKER)) {
                                numBlockers = skillCounts[skillCountIndex];
                                numSkills++;
                            } else {
                                numBlockers = 0;
                            }
                            break;
                        case SKILL_FLAG_BUILDER:
                            if (toBoolean(skillFlags & SKILL_FLAG_BUILDER)) {
                                numBuilders = skillCounts[skillCountIndex];
                                numSkills++;
                            } else {
                                numBuilders = 0;
                            }
                            break;
                        case SKILL_FLAG_BASHER:
                            if (toBoolean(skillFlags & SKILL_FLAG_BASHER)) {
                                numBashers = skillCounts[skillCountIndex];
                                numSkills++;
                            } else {
                                numBashers = 0;
                            }
                            break;
                        case SKILL_FLAG_MINER:
                            if (toBoolean(skillFlags & SKILL_FLAG_MINER)) {
                                numMiners = skillCounts[skillCountIndex];
                                numSkills++;
                            } else {
                                numMiners = 0;
                            }
                            break;
                        case SKILL_FLAG_DIGGER:
                            if (toBoolean(skillFlags & SKILL_FLAG_DIGGER)) {
                                numDiggers = skillCounts[skillCountIndex];
                                numSkills++;
                            } else {
                                numDiggers = 0;
                            }
                            break;
                        default:
                            if (toBoolean(skillFlags & (1 << skillIndex))) {
                                numSkills++;
                            }
                    }
                    skillIndex--;
                }
            } else {
                numClimbers = skillCounts[0];
                numFloaters = skillCounts[1];
                numBombers = skillCounts[2];
                numBlockers = skillCounts[3];
                numBuilders = skillCounts[4];
                numBashers = skillCounts[5];
                numMiners = skillCounts[6];
                numDiggers = skillCounts[7];
            }
        }
        // read objects
        switch (format) {
            case 0:
                objBytes = new byte[32][8];
                break;
            case 1:
            case 2:
                objBytes = new byte[64][16];
                break;
            case 3:
                objBytes = new byte[128][8];
                break;
            default:
                throw new Exception(String.format("Unsupported level format: %d", format));
        }
        objects = new LvlObject[objBytes.length];
        int[] entranceLookup = new int[objBytes.length];
        Arrays.fill(entranceLookup, -1);
        for (int i = 0; i < objBytes.length; i++) {
            for (int j = 0; j < objBytes[i].length; j++) {
                objBytes[i][j] = b.get();
            }
            LvlObject obj = new LvlObject(objBytes[i], SCALE, classic, format);
            if (obj.exists) {
                if (classic) {
                    if (obj.id == LvlObject.ENTRANCE_ID) {
                        if (++entranceCount > (multi ? MAX_ENTRANCES_MULTI : MAX_ENTRANCES)) {
                            obj.flags |= LvlObject.FLAG_FAKE;
                        }
                    } else if (obj.id == LvlObject.GREEN_FLAG_ID) {
                        if (++greenFlagCount > MAX_GREEN_FLAGS) {
                            obj.flags |= LvlObject.FLAG_FAKE;
                        }
                    } else if (i >= FAKE_OBJECT_CUTOFF) {
                        obj.flags |= LvlObject.FLAG_FAKE;
                    }
                }
                if (obj.id == LvlObject.ENTRANCE_ID && !toBoolean(obj.flags & LvlObject.FLAG_FAKE)) {
                    entranceLookup[i] = activeEntranceCount++;
                }
            }
            objects[i] = obj;
        }
        for (int entranceIndex : entranceOrder) {
            if (entranceLookup[entranceIndex] >= 0) {
                remappedEntranceOrder.add(entranceLookup[entranceIndex]);
            }
        }
        // read terrain
        switch (format) {
            case 0:
                terBytes = new byte[400][4];
                break;
            case 1:
            case 2:
            case 3:
                terBytes = new byte[1000][8];
                break;
            default:
                throw new Exception(String.format("Unsupported level format: %d", format));
        }
        terrain = new Terrain[terBytes.length];
        for (int i = 0; i < terBytes.length; i++) {
            for (int j = 0; j < terBytes[i].length; j++) {
                terBytes[i][j] = b.get();
            }
            Terrain ter = new Terrain(terBytes[i], SCALE, classic, format);
            if (toBoolean(optionFlags & OPTION_FLAG_INVERT_ONE_WAY)) {
                if (toBoolean(ter.modifier & Terrain.FLAG_NO_ONE_WAY)) {
                    ter.modifier &= ~Terrain.FLAG_NO_ONE_WAY;
                } else {
                    ter.modifier |= Terrain.FLAG_NO_ONE_WAY;
                }
            }
            terrain[i] = ter;
        }
        // read steel blocks
        switch (format) {
            case 0:
                stlBytes = new byte[32][4];
                break;
            case 1:
            case 2:
            case 3:
                stlBytes = new byte[128][8];
                break;
            default:
                throw new Exception(String.format("Unsupported level format: %d", format));
        }
        steel = new Steel[stlBytes.length];
        for (int i = 0; i < stlBytes.length; i++) {
            for (int j = 0; j < stlBytes[i].length; j++) {
                stlBytes[i][j] = b.get();
            }
            Steel stl = new Steel(stlBytes[i], SCALE, classic, format);
            if (format == 0 && toBoolean(optionFlags & OPTION_FLAG_NEGATIVE_STEEL) && i >= 16) {
                stl.negative = true;
            }
            steel[i] = stl;
        }
        // read name
        if (format == 0) {
            byte[] bName = new byte[32];
            b.get(bName);
            lvlName = new String(bName, StandardCharsets.US_ASCII);
        }
        lvlName = ToolBox.addBackslashes(lvlName, false);
        if (classic && lvlName.indexOf('`') != StringUtils.INDEX_NOT_FOUND) {
            origLvlName = lvlName;
            // replace wrong apostrophes
            lvlName = lvlName.replace('`', '\'');
        }
        
        // write the level
        try (Writer w = Files.newBufferedWriter(fnOut, StandardCharsets.UTF_8)) {
            // add only file name without the path in the first line
            w.write("# LVL extracted by SuperLemmini # " + fName + "\r\n");
            // write configuration
            w.write("releaseRate = " + releaseRate + "\r\n");
            w.write("numLemmings = " + numLemmings + "\r\n");
            w.write("numToRescue = " + numToRescue + "\r\n");
            if (classic) {
                w.write("timeLimit = " + timeLimit + "\r\n");
                w.write("numClimbers = " + numClimbers + "\r\n");
                w.write("numFloaters = " + numFloaters + "\r\n");
                w.write("numBombers = " + numBombers + "\r\n");
                w.write("numBlockers = " + numBlockers + "\r\n");
                w.write("numBuilders = " + numBuilders + "\r\n");
                w.write("numBashers = " + numBashers + "\r\n");
                w.write("numMiners = " + numMiners + "\r\n");
                w.write("numDiggers = " + numDiggers + "\r\n");
            } else {
                w.write("timeLimitSeconds = " + (timeLimit * 60 + timeLimitSeconds) + "\r\n");
                w.write("numClimbers = " + ToolBox.intToString(numClimbers, false) + "\r\n");
                w.write("numFloaters = " + ToolBox.intToString(numFloaters, false) + "\r\n");
                w.write("numBombers = " + ToolBox.intToString(numBombers, false) + "\r\n");
                w.write("numBlockers = " + ToolBox.intToString(numBlockers, false) + "\r\n");
                w.write("numBuilders = " + ToolBox.intToString(numBuilders, false) + "\r\n");
                w.write("numBashers = " + ToolBox.intToString(numBashers, false) + "\r\n");
                w.write("numMiners = " + ToolBox.intToString(numMiners, false) + "\r\n");
                w.write("numDiggers = " + ToolBox.intToString(numDiggers, false) + "\r\n");
                if (!remappedEntranceOrder.isEmpty()) {
                    w.write("entranceOrder = ");
                    for (Iterator<Integer> it = remappedEntranceOrder.iterator(); it.hasNext(); ) {
                        w.write(it.next().toString());
                        if (it.hasNext()) {
                            w.write(", ");
                        }
                    }
                    w.write("\r\n");
                } else if (activeEntranceCount == 3) {
                    w.write("entranceOrder = 0, 1, 2\r\n");
                }
            }
            w.write("xPosCenter = " + xPos + "\r\n");
            if (!classic) {
                w.write("yPosCenter = " + yPos + "\r\n");
            }
            w.write("style = " + ToolBox.addBackslashes(styleStr, false) + "\r\n");
            if (specialStyleStr != null) {
                w.write("specialStyle = " + ToolBox.addBackslashes(specialStyleStr, false) + "\r\n");
                if (format >= 3) {
                    w.write("specialStylePositionX = " + specialStylePositionX + "\r\n");
                    w.write("specialStylePositionY = " + specialStylePositionY + "\r\n");
                }
            }
            if (musicStr != null) {
                w.write("music = " + ToolBox.addBackslashes(musicStr, false) + "\r\n");
            }
            if (toBoolean(optionFlags & OPTION_FLAG_AUTOSTEEL)) {
                if (toBoolean(optionFlags & OPTION_FLAG_SIMPLE_AUTOSTEEL)) {
                    w.write("autosteelMode = 1\r\n");
                } else {
                    w.write("autosteelMode = 2\r\n");
                }
            }
            if (classic) {
                if (extra1 != 0) {
                    w.write("superlemming = true\r\n");
                }
                if ((extra1 != 0 || extra2 != 0)
                        && (extra1 != -1 || extra2 != -1)) {
                    w.write("#byte30Value = " + extra1 + "\r\n");
                    w.write("#byte31Value = " + extra2 + "\r\n");
                }
                w.write("forceNormalTimerSpeed = true\r\n");
                w.write("classicSteel = true\r\n");
            } else {
                if (format >= 1 || toBoolean(optionFlags & OPTION_FLAG_CUSTOM_GIMMICKS)) {
                    if (toBoolean(gimmickFlags & GIMMICK_FLAG_SUPERLEMMING)) {
                        w.write("superlemming = true\r\n");
                    }
                    if (toBoolean(gimmickFlags & GIMMICK_FLAG_CHEAPO_FALL_DISTANCE)) {
                        w.write("maxFallDistance = 152\r\n");
                    }
                } else {
                    switch (extra1) {
                        case -1:
                            w.write("superlemming = true\r\n");
                            break;
                        case 66:
                            switch (extra2) {
                                case 1:
                                case 9:
                                case 10:
                                    w.write("superlemming = true\r\n");
                                    break;
                                default:
                                    break;
                            }
                            break;
                        default:
                            break;
                    }
                }
                w.write("width = " + width + "\r\n");
                w.write("height = " + height + "\r\n");
            }
            // write objects
            w.write("\r\n# Objects\r\n");
            w.write("# ID, X position, Y position, paint mode, flags, object-specific modifier (optional)\r\n");
            w.write("# Paint modes: 0 = full, 2 = invisible, 4 = don't overwrite, 8 = visible only on terrain (only one value possible)\r\n");
            w.write("# Flags: 1 = upside down, 2 = fake, 4 = upside-down mask, 8 = horizontally flipped (combining allowed)\r\n");
            int maxObjectID = -1;
            for (int i = objects.length - 1; i >= 0; i--) {
                if (objects[i].exists) {
                    maxObjectID = i;
                    break;
                }
            }
            for (int i = 0; i <= maxObjectID; i++) {
                LvlObject obj = objects[i];
                if (obj.exists) {
                    w.write("object_" + i + " = " + obj.id + ", " + obj.xPos + ", " + obj.yPos + ", " + obj.paintMode + ", " + obj.flags);
                    if (!classic) {
                        if (obj.id == LvlObject.ENTRANCE_ID && obj.leftFacing) {
                            w.write(", 1");
                        } else {
                            w.write(", 0");
                        }
                    }
                    w.write("\r\n");
                    if (classic) {
                        if (toBoolean(objBytes[i][4] & 0x80)) {
                            w.write("#object_" + i + "_byte4Value = " + objBytes[i][4] + "\r\n");
                        }
                        if ((objBytes[i][6] & 0x3f) != 0) {
                            w.write("#object_" + i + "_byte6Value = " + objBytes[i][6] + "\r\n");
                        }
                        if ((objBytes[i][7] & 0x7f) != 0x0f) {
                            w.write("#object_" + i + "_byte7Value = " + objBytes[i][7] + "\r\n");
                        }
                    }
                } else {
                    w.write("object_" + i + " = -1, 0, 0, 0, 0\r\n");
                }
            }
            // write terrain
            w.write("\r\n# Terrain\r\n");
            w.write("# ID, X position, Y position, modifier\r\n");
            w.write("# Modifier: 1 = invisible, 2 = remove, 4 = upside down, 8 = don't overwrite,\r\n");
            w.write("#           16 = fake, 32 = horizontally flipped, 64 = no one-way arrows (combining allowed)\r\n");
            int maxTerrainID = -1;
            for (int i = terrain.length - 1; i >= 0; i--) {
                if (terrain[i].exists) {
                    maxTerrainID = i;
                    break;
                }
            }
            int maxValidTerrainID = -1;
            if (!classic) {
                maxValidTerrainID = maxTerrainID;
            } else if (specialStyle < 0) {
                for (int i = 0; i <= maxTerrainID; i++) {
                    if (terrain[i].exists) {
                        maxValidTerrainID = i;
                    } else {
                        break;
                    }
                }
            }
            for (int i = 0; i <= maxTerrainID; i++) {
                if (i > maxValidTerrainID) {
                    w.write("#");
                }
                Terrain ter = terrain[i];
                if (ter.exists) {
                    w.write("terrain_" + i + " = " + ter.id + ", " + ter.xPos + ", " + ter.yPos + ", " + ter.modifier + "\r\n");
                    if (classic && toBoolean(terBytes[i][3] & 0x40)) {
                        w.write("#terrain_" + i + "_byte3Value = " + terBytes[i][3] + "\r\n");
                    }
                } else {
                    w.write("terrain_" + i + " = -1, 0, 0, 0\r\n");
                }
            }
            // write steel blocks
            w.write("\r\n# Steel\r\n");
            w.write("# X position, Y position, width, height, flags (optional)\r\n");
            w.write("# Flags: 1 = remove existing steel\r\n");
            int maxSteelID = -1;
            if (!toBoolean(optionFlags & OPTION_FLAG_IGNORE_STEEL)) {
                for (int i = stlBytes.length - 1; i >= 0; i--) {
                    if (steel[i].exists) {
                        maxSteelID = i;
                        break;
                    }
                }
            }
            for (int i = 0; i <= maxSteelID; i++) {
                Steel stl = steel[i];
                if (stl.exists) {
                    w.write("steel_" + i + " = " + stl.xPos + ", " + stl.yPos + ", " + stl.width + ", " + stl.height);
                    if (!classic) {
                        w.write(", " + (stl.negative ? "1" : "0"));
                    }
                    w.write("\r\n");
                    if (classic && stlBytes[i][3] != 0) {
                        w.write("#steel_" + i + "_byte3Value = " + stlBytes[i][3] + "\r\n");
                    }
                } else {
                    w.write("steel_" + i + " = 0, 0, 0, 0\r\n");
                }
            }
            // write name
            w.write("\r\n# Name and author\r\n");
            if (origLvlName != null) {
                w.write("#origName = " + origLvlName + "\r\n");
            }
            w.write("name = " + lvlName + "\r\n");
            if (!author.isEmpty()) {
                w.write("author = " + author + "\r\n");
            }
        }
    }
}

/**
 * Storage class for level objects.
 * @author Volker Oth
 */
class LvlObject {
    
    /** paint mode: only visible on a terrain pixel */
    static final int MODE_VIS_ON_TERRAIN = 8;
    /** paint mode: don't overwrite terrain pixel in the original foreground image */
    static final int MODE_NO_OVERWRITE = 4;
    static final int MODE_INVISIBLE = 2;
    
    /** flag: paint object upside down */
    static final int FLAG_UPSIDE_DOWN = 1;
    static final int FLAG_FAKE = 2;
    static final int FLAG_UPSIDE_DOWN_MASK = 4;
    static final int FLAG_HORIZONTALLY_FLIPPED = 8;
    
    static final int ENTRANCE_ID = 1;
    static final int GREEN_FLAG_ID = 2;

    /** x position in pixels */
    int xPos;
    /** y position in pixels */
    int yPos;
    /** identifier */
    int id;
    /** paint mode */
    int paintMode;
    int flags;
    boolean leftFacing;
    boolean exists;

    /**
     * Constructor.
     * @param b buffer
     * @param classic
     * @param scale Scale (to convert lowres levels into hires levels)
     */
    LvlObject(final byte[] b, final int scale, final boolean classic, final int format) throws Exception {
        switch (format) {
            case 0:
                int sum = 0;
                for (byte b1 : b) {
                    sum += b1 & 0xff;
                }
                exists = sum != 0;
                // x pos  : min 0xFFF8, max 0x0638.  0xFFF8 = -24, 0x0000 = -16, 0x0008 = -8
                // 0x0010 = 0, 0x0018 = 8, ... , 0x0638 = 1576    note: should be multiples of 8
                xPos = ((b[0] << 8) | (b[1] & 0xff)) - 16;
                xPos *= scale;
                // y pos  : min 0xFFD7, max 0x009F.  0xFFD7 = -41, 0xFFF8 = -8, 0xFFFF = -1
                // 0x0000 = 0, ... , 0x009F = 159.  note: can be any value in the specified range
                yPos = (b[2] << 8) | (b[3] & 0xff);
                yPos *= scale;
                // obj id : min 0x0000, max 0x000F.  the object id is different in each
                // graphics set, however 0x0000 is always an exit and 0x0001 is always a start.
                if (classic) {
                    id = ((b[4] & 0x7f) << 8) | (b[5] & 0xff);
                } else {
                    id = b[5] & 0xff;
                }
                // modifier : first byte can be 80 (do not overwrite existing terrain) or 40
                // (must have terrain underneath to be visible). 00 specifies always draw full graphic.
                // second byte can be 8F (display graphic upside-down) or 0F (display graphic normally)
                paintMode = 0;
                flags = 0;
                if (toBoolean(b[6] & 0x80)) {
                    paintMode |= MODE_NO_OVERWRITE;
                }
                if (toBoolean(b[6] & 0x40)) {
                    paintMode |= MODE_VIS_ON_TERRAIN;
                }
                if (toBoolean(b[6] & 0x10)) {
                    flags |= FLAG_UPSIDE_DOWN;
                    if (!classic) {
                        flags |= FLAG_UPSIDE_DOWN_MASK;
                    }
                }
                if (!classic && toBoolean(b[6] & 0x10)) {
                    flags |= FLAG_FAKE;
                }
                break;
            case 1:
            case 2:
            case 3:
                xPos = (b[0] & 0xff) | (b[1] << 8);
                xPos *= scale;
                yPos = (b[2] & 0xff) | (b[3] << 8);
                yPos *= scale;
                id = b[4] & 0xff;
                paintMode = 0;
                flags = 0;
                if (toBoolean(b[7] & 0x01)) {
                    paintMode |= MODE_NO_OVERWRITE;
                }
                if (toBoolean(b[7] & 0x02)) {
                    paintMode |= MODE_VIS_ON_TERRAIN;
                }
                if (toBoolean(b[7] & 0x04)) {
                    flags |= FLAG_UPSIDE_DOWN;
                    flags |= FLAG_UPSIDE_DOWN_MASK;
                }
                leftFacing = toBoolean(b[7] & 0x08);
                if (toBoolean(b[7] & 0x10)) {
                    flags |= FLAG_FAKE;
                }
                if (toBoolean(b[7] & 0x20)) {
                    paintMode |= MODE_INVISIBLE;
                }
                if (toBoolean(b[7] & 0x40)) {
                    flags |= FLAG_HORIZONTALLY_FLIPPED;
                }
                exists = toBoolean(b[7] & 0x80);
                break;
            default:
                throw new Exception(String.format("Unsupported level format: %d", format));
        }
    }
}

/**
 * Storage class for terrain tiles.
 * @author Volker Oth
 */
class Terrain {
    
    static final int FLAG_NO_ONE_WAY = 64;
    static final int FLAG_HORIZONTALLY_FLIPPED = 32;
    static final int FLAG_NO_OVERWRITE = 8;
    static final int FLAG_UPSIDE_DOWN = 4;
    static final int FLAG_ERASE = 2;

    /** identifier */
    int id;
    /** x position in pixels */
    int xPos;
    /** y position in pixels */
    int yPos;
    /** modifier - must be one of the above MODEs */
    int modifier;
    boolean exists;

    /**
     * Constructor.
     * @param b buffer
     * @param scale Scale (to convert lowres levels into hires levels)
     */
    Terrain(final byte[] b, final int scale, final boolean classic, final int format) throws Exception {
        switch (format) {
            case 0:
                int mask = 0xff;
                for (byte b1 : b) {
                    mask &= b1 & 0xff;
                }
                exists = mask != 0xff;
                // xpos: 0x0000..0x063F.  0x0000 = -16, 0x0008 = -8, 0x0010 = 0, 0x063f = 1583.
                // note: the xpos also contains modifiers.  the first nibble can be
                // 8 (do no overwrite existing terrain), 4 (display upside-down), or
                // 2 (remove terrain instead of add it). you can add them together.
                // 0 indicates normal.
                // eg: 0xC011 means draw at xpos=1, do not overwrite, upside-down.
                modifier = (b[0] & 0xe0) >> 4;
                xPos = (((b[0] & (classic ? 0x1f : 0x0f)) << 8) | (b[1] & 0xff)) - 16;
                xPos *= scale;
                // y pos : 9-bit value. min 0xEF0, max 0x518.  0xEF0 = -38, 0xEF8 = -37,
                // 0x020 = 0, 0x028 = 1, 0x030 = 2, 0x038 = 3, ... , 0x518 = 159
                // note: the ypos value bleeds into the next value since it is 9 bits.
                yPos = ((b[2] & 0xff) << 1) | ((b[3] & 0x80) >> 7);
                if (toBoolean(yPos & 256)) { // highest bit set -> negative
                    yPos -= 512;
                }
                yPos -= 4;
                yPos *= scale;
                // terrain id: min 0x00, max 0x3F.  not all graphic sets have all 64 graphics.
                id = b[3] & 0x3f;
                if (!classic && toBoolean(b[0] & 0x10)) {
                    id += 64;
                }
                break;
            case 1:
            case 2:
            case 3:
                xPos = (b[0] & 0xff) | (b[1] << 8);
                xPos *= scale;
                yPos = (b[2] & 0xff) | (b[3] << 8);
                yPos *= scale;
                id = b[4] & 0xff;
                modifier = 0;
                if (toBoolean(b[5] & 0x01)) {
                    modifier |= FLAG_NO_OVERWRITE;
                }
                if (toBoolean(b[5] & 0x02)) {
                    modifier |= FLAG_ERASE;
                }
                if (toBoolean(b[5] & 0x04)) {
                    modifier |= FLAG_UPSIDE_DOWN;
                }
                if (toBoolean(b[5] & 0x08)) {
                    modifier |= FLAG_HORIZONTALLY_FLIPPED;
                }
                if (toBoolean(b[5] & 0x10)) {
                    modifier |= FLAG_NO_ONE_WAY;
                }
                exists = toBoolean(b[5] & 0x80);
                break;
            default:
                throw new Exception(String.format("Unsupported level format: %d", format));
        }
    }
}

/**
 *
 * Storage class for steel areas.
 * @author Volker Oth
 */
class Steel {

    /** x position in pixels */
    int xPos;
    /** y position in pixels */
    int yPos;
    /** width in pixels */
    int width;
    /** height in pixels */
    int height;
    boolean negative;
    boolean exists;

    /**
     * Constructor.
     * @param b buffer
     * @param scale Scale (to convert lowres levels into hires levels)
     */
    Steel(final byte[] b, final int scale, final boolean classic, final int format) throws Exception {
        switch (format) {
            case 0:
                int sum = 0;
                for (byte b1 : b) {
                    sum += b1 & 0xff;
                }
                exists = sum != 0;
                // xpos: 9-bit value: 0x000-0x178).  0x000 = -16, 0x178 = 1580
                xPos = (((b[0] & 0xff) << 1) | ((b[1] & 0x80) >> 7)) * 4 - 16;
                if (!classic) {
                    xPos -= (b[3] & 0xc0) >>> 6;
                }
                xPos *= scale;
                // ypos: 0x00-0x27. 0x00 = 0, 0x27 = 156 - each hex value represents 4 pixels
                yPos = (b[1] & 0x7f) * 4;
                if (!classic) {
                    yPos -= (b[3] & 0x30) >>> 4;
                }
                yPos *= scale;
                // area: 0x00-0xFF.  first nibble is the x-size, from 0-F (represents 4 pixels)
                // second nibble is the y-size. 0x00 = (4,4), 0x11 = (8,8), 0x7F = (32,64)
                width = ((b[2] & 0xf0) >> 4) * 4 + 4;
                if (!classic) {
                    width -= (b[3] & 0x0c) >>> 2;
                }
                width *= scale;
                height = (b[2] & 0xf) * 4 + 4;
                if (!classic) {
                    height -= b[3] & 0x03;
                }
                height *= scale;
                negative = false;
                break;
            case 1:
            case 2:
            case 3:
                xPos = (b[0] & 0xff) | (b[1] << 8);
                xPos *= scale;
                yPos = (b[2] & 0xff) | (b[3] << 8);
                yPos *= scale;
                width = (b[4] & 0xff) + 1;
                width *= scale;
                height = (b[5] & 0xff) + 1;
                height *= scale;
                int steelType = b[6] & 0x7f;
                negative = steelType == 1;
                exists = toBoolean(b[6] & 0x80) && (steelType == 0 || steelType == 1);
                break;
            default:
                throw new Exception(String.format("Unsupported level format: %d", format));
        }
    }
}