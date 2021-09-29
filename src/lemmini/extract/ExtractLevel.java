package lemmini.extract;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
    static final int FAKE_OBJECT_CUTOFF = 16;
    private static final int MAX_ENTRANCES = 4;
    private static final int MAX_ENTRANCES_MULTI = 2;
    private static final int MAX_GREEN_FLAGS = 1;
    
    /** names for default styles */
    private static final String[] STYLES = {"dirt", "fire", "marble", "pillar", "crystal",
        "brick", "rock", "snow", "bubble", "xmas"};
    /** names for default special styles */
    private static final String[] SPECIAL_STYLES = {"awesome", "menace", "beastii", "beasti",
        "covox", "prima", "apple"};
    
    private static final int GIMMICK_FLAG_SUPERLEMMING = 1;
    
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

    private static long fileSize;
    private static int format;
    /** release rate : 0 is slowest, 0x0FA (250) is fastest */
    private static int releaseRate;
    /** number of Lemmings in this level (maximum 0x0072 in original LVL format) */
    private static int numLemmings;
    /** number of Lemmings to rescue : should be less than or equal to number of Lemmings */
    private static int numToRescue;
    private static int timeLimitSeconds;
    /** Time Limit: max 0x00FF, 0x0001 to 0x0009 works best */
    private static int timeLimit;
    private static int gimmickFlags;
    private static int skillFlags;
    private static int[] skillCounts;
    /** number of climbers in this level : max 0xfa (250) */
    private static int numClimbers;
    /** number of floaters in this level : max 0xfa (250) */
    private static int numFloaters;
    /** number of bombers in this level : max 0xfa (250) */
    private static int numBombers;
    /** number of blockers in this level : max 0xfa (250) */
    private static int numBlockers;
    /** number of builders in this level : max 0xfa (250) */
    private static int numBuilders;
    /** number of bashers in this level : max 0xfa (250) */
    private static int numBashers;
    /** number of miners in this level : max 0xfa (250) */
    private static int numMiners;
    /** number of diggers in this level : max 0xfa (250) */
    private static int numDiggers;
    /** start screen x pos : 0 - 0x04f0 (1264) rounded to modulo 8 */
    private static int xPos;
    private static int optionFlags;
    /**
     * 0x0000 is dirt,  <br>0x0001 is fire,   <br>0x0002 is marble,  <br>
     * 0x0003 is pillar,<br>0x0004 is crystal,<br>0x0005 is brick,   <br>
     * 0x0006 is rock,  <br>0x0007 is snow,   <br>0x0008 is bubble,  <br>
     * 0x0009 is xmas
     */
    private static int style;
    /** special style */
    private static int specialStyle;
    private static int extra1;
    private static int extra2;
    /** objects like doors - 32 objects each consists of 8 bytes */
    private static LvlObject[] objects;
    /** terrain the Lemmings walk on etc. - 400 tiles, 4 bytes each */
    private static Terrain[] terrain;
    /** steel areas which are indestructible - 32 objects, 4 bytes each */
    private static Steel[] steel;
    /** 32 byte level name - filled with whitespaces */
    private static String lvlName;
    private static int entranceCount;
    private static int greenFlagCount;

    /**
     * Convert one binary LVL file into text file
     * @param fnIn Name of binary LVL file
     * @param fnOut Name of target text file
     * @param multi Whether this is a multiplayer level
     * @param classic Whether to convert in classic mode
     * @throws Exception
     */
    public static void convertLevel(final String fnIn, final String fnOut, final boolean multi, final boolean classic) throws Exception {
        entranceCount = 0;
        greenFlagCount = 0;
        // read file into buffer
        ByteBuffer b;
        try {
            Path f = Paths.get(fnIn);
            if (Files.notExists(f)) {
                throw new Exception(String.format("File %s not found.", fnIn));
            }
            fileSize = Files.size(f);
            if (fileSize != 2048L && fileSize != 10240L) {
                throw new Exception("Lemmings level files must be 2,048 or 10,240 bytes in size!");
            }
            byte[] buffer = Files.readAllBytes(f);
            b = ByteBuffer.wrap(buffer).asReadOnlyBuffer();
        } catch (IOException e) {
            throw new Exception(String.format("I/O error while reading %s.", fnIn));
        }
        // output file
        try (Writer w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fnOut), StandardCharsets.UTF_8))) {
            // add only file name without the path in the first line
            int p1 = fnIn.lastIndexOf("/");
            int p2 = fnIn.lastIndexOf("\\");
            if (p2 > p1) {
                p1 = p2;
            }
            if (p1 < 0) {
                p1 = 0;
            } else {
                p1++;
            }
            String fn = fnIn.substring(p1);
            // analyze buffer
            w.write("# LVL extracted by SuperLemmini # " + fn + "\r\n");
            if (classic) {
                format = 0;
                if (fileSize != 2048L) {
                    throw new Exception("Format 0 level files must be 2,048 bytes in size!");
                }
                gimmickFlags = 0;
                skillFlags = 0;
                optionFlags = 0;
                // read configuration in big endian word
                releaseRate = b.getShort();
                if (releaseRate >= 100) {
                    releaseRate -= 65536;
                }
                w.write("releaseRate = " + releaseRate + "\r\n");
                numLemmings = b.getShort() & 0xffff;
                w.write("numLemmings = " + numLemmings + "\r\n");
                numToRescue = b.getShort() & 0xffff;
                w.write("numToRescue = " + numToRescue + "\r\n");
                timeLimitSeconds = 0;
                timeLimit = b.getShort() & 0xffff;
                w.write("timeLimit = " + timeLimit + "\r\n");
                numClimbers = b.getShort() & 0xffff;
                w.write("numClimbers = " + numClimbers + "\r\n");
                numFloaters = b.getShort() & 0xffff;
                w.write("numFloaters = " + numFloaters + "\r\n");
                numBombers = b.getShort() & 0xffff;
                w.write("numBombers = " + numBombers + "\r\n");
                numBlockers = b.getShort() & 0xffff;
                w.write("numBlockers = " + numBlockers + "\r\n");
                numBuilders = b.getShort() & 0xffff;
                w.write("numBuilders = " + numBuilders + "\r\n");
                numBashers = b.getShort() & 0xffff;
                w.write("numBashers = " + numBashers + "\r\n");
                numMiners = b.getShort() & 0xffff;
                w.write("numMiners = " + numMiners + "\r\n");
                numDiggers = b.getShort() & 0xffff;
                w.write("numDiggers = " + numDiggers + "\r\n");
                xPos = b.getShort() & 0xffff;
                xPos += multi ? 72 : 160;
                xPos *= SCALE;
                w.write("xPosCenter = " + xPos + "\r\n");
                style = b.getShort() & 0xffff;
                if (style >= STYLES.length) {
                    throw new Exception(fnIn + " uses an invalid style.");
                }
                w.write("style = " + STYLES[style] + "\r\n");
                specialStyle = (b.getShort() & 0xffff) - 1;
                if (specialStyle >= SPECIAL_STYLES.length && specialStyle != 101) {
                    throw new Exception(fnIn + " uses an invalid special style.");
                }
                if (specialStyle == 101) {
                    w.write("specialStyle = " + SPECIAL_STYLES[6] + "\r\n");
                } else if (specialStyle > -1) {
                    w.write("specialStyle = " + SPECIAL_STYLES[specialStyle] + "\r\n");
                }
                extra1 = b.get();
                extra2 = b.get();
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
                format = b.get();
                numClimbers = 0;
                numFloaters = 0;
                numBombers = 0;
                numBlockers = 0;
                numBuilders = 0;
                numBashers = 0;
                numMiners = 0;
                numDiggers = 0;
                switch (format) {
                    case 0:
                        if (fileSize != 2048L) {
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
                            if (skillCounts[i] == 200) {
                                skillCounts[i] = Integer.MAX_VALUE;
                            }
                        }
                        xPos = b.getShort() & 0xffff;
                        xPos += multi ? 72 : 160;
                        xPos *= SCALE;
                        b.get();
                        style = b.get() & 0xff;
                        if (style >= STYLES.length) {
                            throw new Exception(fnIn + " uses an invalid style.");
                        }
                        optionFlags = b.get();
                        specialStyle = (b.get() & 0xff) - 1;
                        if (specialStyle >= SPECIAL_STYLES.length && specialStyle != 101) {
                            throw new Exception(fnIn + " uses an invalid special style.");
                        }
                        extra1 = b.get();
                        extra2 = b.get();
                        break;
                    case 1:
                        if (fileSize != 10240L) {
                            throw new Exception("Format 1 level files must be 10,240 bytes in size!");
                        }
                        extra1 = 0;
                        extra2 = 0;
                        b.order(ByteOrder.LITTLE_ENDIAN);
                        b.get();
                        numLemmings = b.getShort() & 0xffff;
                        numToRescue = b.getShort() & 0xffff;
                        timeLimitSeconds = b.getShort() & 0xffff;
                        releaseRate = b.get();
                        if (releaseRate >= 100) {
                            releaseRate -= 256;
                        }
                        optionFlags = b.get();
                        style = b.get() & 0xff;
                        if (style >= STYLES.length) {
                            throw new Exception(fnIn + " uses an invalid style.");
                        }
                        specialStyle = (b.get() & 0xff) - 1;
                        if (specialStyle >= SPECIAL_STYLES.length && specialStyle != 101) {
                            throw new Exception(fnIn + " uses an invalid special style.");
                        }
                        b.get();
                        b.get();
                        xPos = b.getShort() & 0xffff;
                        xPos += multi ? 72 : 160;
                        xPos *= SCALE;
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
                            if (skillCounts[i] == 200) {
                                skillCounts[i] = Integer.MAX_VALUE;
                            }
                        }
                        gimmickFlags = b.getInt();
                        skillFlags = b.getShort() & 0xffff;
                        b.position(b.position() + 26);
                        byte[] bName = new byte[32];
                        b.get(bName);
                        lvlName = new String(bName, StandardCharsets.US_ASCII);
                        b.position(b.position() + 96);
                        break;      
                    default:
                        throw new Exception(String.format("Unsupported level format: %d", format));
                }
                if (format > 0 || (optionFlags & OPTION_FLAG_CUSTOM_SKILL_SET) != 0) {
                    int skillIndex = 15;
                    int numSkills = 0;
                    int skillCountIndex;
                    while (skillIndex >= 0 && numSkills < 8) {
                        switch (format) {
                            case 0:
                                skillCountIndex = numSkills;
                                break;
                            case 1:
                                skillCountIndex = skillCounts.length - 1 - skillIndex;
                                break;
                            default:
                                throw new Exception(String.format("Unsupported level format: %d", format));
                        }
                        switch (1 << skillIndex) {
                            case SKILL_FLAG_CLIMBER:
                                if ((skillFlags & SKILL_FLAG_CLIMBER) != 0) {
                                    numClimbers = skillCounts[skillCountIndex];
                                    numSkills++;
                                } else {
                                    numClimbers = 0;
                                }
                                break;
                            case SKILL_FLAG_FLOATER:
                                if ((skillFlags & SKILL_FLAG_FLOATER) != 0) {
                                    numFloaters = skillCounts[skillCountIndex];
                                    numSkills++;
                                } else {
                                    numFloaters = 0;
                                }
                                break;
                            case SKILL_FLAG_BOMBER:
                                if ((skillFlags & SKILL_FLAG_BOMBER) != 0) {
                                    numBombers = skillCounts[skillCountIndex];
                                    numSkills++;
                                } else {
                                    numBombers = 0;
                                }
                                break;
                            case SKILL_FLAG_BLOCKER:
                                if ((skillFlags & SKILL_FLAG_BLOCKER) != 0) {
                                    numBlockers = skillCounts[skillCountIndex];
                                    numSkills++;
                                } else {
                                    numBlockers = 0;
                                }
                                break;
                            case SKILL_FLAG_BUILDER:
                                if ((skillFlags & SKILL_FLAG_BUILDER) != 0) {
                                    numBuilders = skillCounts[skillCountIndex];
                                    numSkills++;
                                } else {
                                    numBuilders = 0;
                                }
                                break;
                            case SKILL_FLAG_BASHER:
                                if ((skillFlags & SKILL_FLAG_BASHER) != 0) {
                                    numBashers = skillCounts[skillCountIndex];
                                    numSkills++;
                                } else {
                                    numBashers = 0;
                                }
                                break;
                            case SKILL_FLAG_MINER:
                                if ((skillFlags & SKILL_FLAG_MINER) != 0) {
                                    numMiners = skillCounts[skillCountIndex];
                                    numSkills++;
                                } else {
                                    numMiners = 0;
                                }
                                break;
                            case SKILL_FLAG_DIGGER:
                                if ((skillFlags & SKILL_FLAG_DIGGER) != 0) {
                                    numDiggers = skillCounts[skillCountIndex];
                                    numSkills++;
                                } else {
                                    numDiggers = 0;
                                }
                                break;
                            default:
                                if ((skillFlags & (1 << skillIndex)) != 0) {
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
                w.write("releaseRate = " + releaseRate + "\r\n");
                w.write("numLemmings = " + numLemmings + "\r\n");
                w.write("numToRescue = " + numToRescue + "\r\n");
                w.write("timeLimitSeconds = " + (timeLimit * 60 + timeLimitSeconds) + "\r\n");
                w.write("numClimbers = " + ((numClimbers == Integer.MAX_VALUE) ? "Infinity" : numClimbers) + "\r\n");
                w.write("numFloaters = " + ((numFloaters == Integer.MAX_VALUE) ? "Infinity" : numFloaters) + "\r\n");
                w.write("numBombers = " + ((numBombers == Integer.MAX_VALUE) ? "Infinity" : numBombers) + "\r\n");
                w.write("numBlockers = " + ((numBlockers == Integer.MAX_VALUE) ? "Infinity" : numBlockers) + "\r\n");
                w.write("numBuilders = " + ((numBuilders == Integer.MAX_VALUE) ? "Infinity" : numBuilders) + "\r\n");
                w.write("numBashers = " + ((numBashers == Integer.MAX_VALUE) ? "Infinity" : numBashers) + "\r\n");
                w.write("numMiners = " + ((numMiners == Integer.MAX_VALUE) ? "Infinity" : numMiners) + "\r\n");
                w.write("numDiggers = " + ((numDiggers == Integer.MAX_VALUE) ? "Infinity" : numDiggers) + "\r\n");
                w.write("xPosCenter = " + xPos + "\r\n");
                w.write("style = " + STYLES[style] + "\r\n");
                if (specialStyle == 101) {
                    w.write("specialStyle = " + SPECIAL_STYLES[6] + "\r\n");
                } else if (specialStyle > -1) {
                    w.write("specialStyle = " + SPECIAL_STYLES[specialStyle] + "\r\n");
                }
                if ((optionFlags & OPTION_FLAG_AUTOSTEEL) != 0) {
                    if ((optionFlags & OPTION_FLAG_SIMPLE_AUTOSTEEL) != 0) {
                        w.write("autosteelMode = 1\r\n");
                    } else {
                        w.write("autosteelMode = 2\r\n");
                    }
                }
                if (format > 0 || (optionFlags & OPTION_FLAG_CUSTOM_GIMMICKS) != 0) {
                    if ((gimmickFlags & GIMMICK_FLAG_SUPERLEMMING) != 0) {
                        w.write("superlemming = true\r\n");
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
            }
            // read objects
            w.write("\r\n# Objects\r\n");
            w.write("# ID, X position, Y position, paint mode, flags, object-specific modifier (optional)\r\n");
            w.write("# Paint modes: 0 = full, 2 = invisible, 4 = don't overwrite, 8 = visible only on terrain (only one value possible)\r\n");
            w.write("# Flags: 1 = upside down, 2 = fake (combining allowed)\r\n");
            byte[][] bytes;
            switch (format) {
                case 0:
                    bytes = new byte[32][8];
                    break;
                case 1:
                    bytes = new byte[64][16];
                    break;
                default:
                    throw new Exception(String.format("Unsupported level format: %d", format));
            }
            objects = new LvlObject[bytes.length];
            for (int i = 0; i < bytes.length; i++) {
                for (int j = 0; j < bytes[i].length; j++) {
                    bytes[i][j] = b.get();
                }
                objects[i] = new LvlObject(bytes[i], SCALE, classic, format);
            }
            int maxObjectID = -1;
            for (int i = objects.length - 1; i >= 0; i--) {
                if (objects[i].exists) {
                    maxObjectID = i;
                    break;
                }
            }
            for (int i = 0; i <= maxObjectID; i++) {
                if (objects[i].exists) {
                    LvlObject obj = objects[i];
                    if (classic) {
                        if (obj.id == LvlObject.ENTRANCE_ID) {
                            if (++entranceCount > (multi ? MAX_ENTRANCES_MULTI : MAX_ENTRANCES)) {
                                obj.fake = true;
                            }
                        } else if (obj.id == LvlObject.GREEN_FLAG_ID) {
                            if (++greenFlagCount > MAX_GREEN_FLAGS) {
                                obj.fake = true;
                            }
                        } else if (i >= FAKE_OBJECT_CUTOFF) {
                            obj.fake = true;
                        }
                    }
                    int flags = obj.upsideDown ? 1 : 0;
                    flags |= obj.fake ? 2 : 0;
                    w.write("object_" + i + " = " + obj.id + ", " + obj.xPos + ", " + obj.yPos + ", " + obj.paintMode + ", " + flags);
                    if (!classic) {
                        if (obj.id == LvlObject.ENTRANCE_ID && obj.leftFacing) {
                            w.write(", 1");
                        } else {
                            w.write(", 0");
                        }
                    }
                    w.write("\r\n");
                    if (classic) {
                        if ((bytes[i][4] & 0x80) != 0) {
                            w.write("#object_" + i + "_byte4Value = " + bytes[i][4] + "\r\n");
                        }
                        if ((bytes[i][6] & 0x3f) != 0) {
                            w.write("#object_" + i + "_byte6Value = " + bytes[i][6] + "\r\n");
                        }
                        if ((bytes[i][7] & 0x7f) != 0x0f) {
                            w.write("#object_" + i + "_byte7Value = " + bytes[i][7] + "\r\n");
                        }
                    }
                } else {
                    w.write("object_" + i + " = -1, 0, 0, 0, 0\r\n");
                }
            }
            // read terrain
            w.write("\r\n# Terrain\r\n");
            w.write("# ID, X position, Y position, modifier\r\n");
            w.write("# Modifier: 1 = invisible, 2 = remove, 4 = upside down, 8 = don't overwrite, 16 = fake (combining allowed, 0 = full)\r\n");
            switch (format) {
                case 0:
                    bytes = new byte[400][4];
                    break;
                case 1:
                    bytes = new byte[1000][8];
                    break;
                default:
                    throw new Exception(String.format("Unsupported level format: %d", format));
            }
            terrain = new Terrain[bytes.length];
            for (int i = 0; i < bytes.length; i++) {
                for (int j = 0; j < bytes[i].length; j++) {
                    bytes[i][j] = b.get();
                }
                terrain[i] = new Terrain(bytes[i], SCALE, format);
            }
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
                if (terrain[i].exists) {
                    Terrain ter = terrain[i];
                    w.write("terrain_" + i + " = " + ter.id + ", " + ter.xPos + ", " + ter.yPos + ", " + ter.modifier + "\r\n");
                    if (classic && (bytes[i][3] & 0x40) != 0) {
                        w.write("#terrain_" + i + "_byte3Value = " + bytes[i][3] + "\r\n");
                    }
                } else {
                    w.write("terrain_" + i + " = -1, 0, 0, 0\r\n");
                }
            }
            // read steel blocks
            w.write("\r\n# Steel\r\n");
            w.write("# X position, Y position, width, height, flags (optional)\r\n");
            w.write("# Flags: 1 = remove existing steel\r\n");
            switch (format) {
                case 0:
                    bytes = new byte[32][4];
                    break;
                case 1:
                    bytes = new byte[128][8];
                    break;
                default:
                    throw new Exception(String.format("Unsupported level format: %d", format));
            }
            steel = new Steel[bytes.length];
            for (int i = 0; i < bytes.length; i++) {
                for (int j = 0; j < bytes[i].length; j++) {
                    bytes[i][j] = b.get();
                }
                steel[i] = new Steel(bytes[i], SCALE, classic, format);
            }
            if (format == 0 && (optionFlags & OPTION_FLAG_NEGATIVE_STEEL) != 0) {
                b.position(b.position() + 64);
            }
            int maxSteelID = -1;
            if ((optionFlags & OPTION_FLAG_IGNORE_STEEL) == 0) {
                for (int i = bytes.length - 1; i >= 0; i--) {
                    if (steel[i].exists) {
                        maxSteelID = i;
                        break;
                    }
                }
            }
            for (int i = 0; i <= maxSteelID; i++) {
                if (steel[i].exists) {
                    Steel stl = steel[i];
                    if (format == 0 && i >= 16 && (optionFlags & OPTION_FLAG_NEGATIVE_STEEL) != 0) {
                        stl.negative = true;
                    }
                    w.write("steel_" + i + " = " + stl.xPos + ", " + stl.yPos + ", " + stl.width + ", " + stl.height);
                    if (!classic) {
                        w.write(", " + (stl.negative ? "1" : "0"));
                    }
                    w.write("\r\n");
                    if (classic && bytes[i][3] != 0) {
                        w.write("#steel_" + i + "_byte3Value = " + bytes[i][3] + "\r\n");
                    }
                } else {
                    w.write("steel_" + i + " = 0, 0, 0, 0\r\n");
                }
            }
            // read name
            w.write("\r\n# Name" + "\r\n");
            if (format == 0) {
                byte[] bName = new byte[32];
                b.get(bName);
                lvlName = new String(bName, StandardCharsets.US_ASCII);
            }
            lvlName = lvlName.replace("\\", "\\\\");
            lvlName = lvlName.replace("#", "\\#");
            lvlName = lvlName.replace("=", "\\=");
            lvlName = lvlName.replace(":", "\\:");
            lvlName = lvlName.replace("!", "\\!");
            if (lvlName.charAt(0) == ' ') {
                lvlName = "\\" + lvlName;
            }
            if (classic && lvlName.indexOf('`') != -1) {
                // replace wrong apostrophes
                w.write("#origName = " + lvlName + "\r\n");
                lvlName = lvlName.replace('`', '\'');
            }
            w.write("name = " + lvlName + "\r\n");
        }
    }
}

/**
 * Storage class for level objects.
 * @author Volker Oth
 */
class LvlObject {
    /** paint mode: only visible on a terrain pixel */
    private static final int MODE_VIS_ON_TERRAIN = 8;
    /** paint mode: don't overwrite terrain pixel in the original foreground image */
    private static final int MODE_NO_OVERWRITE = 4;
    
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
    /** flag: paint object upside down */
    boolean upsideDown;
    boolean fake;
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
                if ((b[6] & 0x80) != 0) {
                    paintMode |= MODE_NO_OVERWRITE;
                }
                if ((b[6] & 0x40) != 0) {
                    paintMode |= MODE_VIS_ON_TERRAIN;
                }
                upsideDown = (b[7] & 0x80) != 0;
                fake = classic ? false : ((b[6] & 0x10) != 0);
                break;
            case 1:
                xPos = (b[0] & 0xff) | (b[1] << 8);
                xPos *= scale;
                yPos = (b[2] & 0xff) | (b[3] << 8);
                yPos *= scale;
                id = b[4] & 0xff;
                paintMode = 0;
                if ((b[7] & 0x01) != 0) {
                    paintMode |= MODE_NO_OVERWRITE;
                }
                if ((b[7] & 0x02) != 0) {
                    paintMode |= MODE_VIS_ON_TERRAIN;
                }
                upsideDown = (b[7] & 0x04) != 0;
                leftFacing = (b[7] & 0x08) != 0;
                fake = (b[7] & 0x10) != 0;
                exists = (b[7] & 0x80) != 0;
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
    private static final int FLAG_NO_OVERWRITE = 8;
    private static final int FLAG_UPSIDE_DOWN = 4;
    private static final int FLAG_ERASE = 2;

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
    Terrain(final byte[] b, final int scale, final int format) throws Exception {
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
                xPos = (((b[0] & 0x1f) << 8) | (b[1] & 0xff)) - 16;
                xPos *= scale;
                // y pos : 9-bit value. min 0xEF0, max 0x518.  0xEF0 = -38, 0xEF8 = -37,
                // 0x020 = 0, 0x028 = 1, 0x030 = 2, 0x038 = 3, ... , 0x518 = 159
                // note: the ypos value bleeds into the next value since it is 9 bits.
                yPos = ((b[2] & 0xff) << 1) | ((b[3] & 0x80) >> 7);
                if ((yPos & 256) != 0) { // highest bit set -> negative
                    yPos -= 512;
                }
                yPos -= 4;
                yPos *= scale;
                // terrain id: min 0x00, max 0x3F.  not all graphic sets have all 64 graphics.
                id = b[3] & 0x3f;
                break;
            case 1:
                xPos = (b[0] & 0xff) | (b[1] << 8);
                xPos *= scale;
                yPos = (b[2] & 0xff) | (b[3] << 8);
                yPos *= scale;
                id = b[4] & 0xff;
                modifier = 0;
                if ((b[5] & 0x01) != 0) {
                    modifier |= FLAG_NO_OVERWRITE;
                }
                if ((b[5] & 0x02) != 0) {
                    modifier |= FLAG_ERASE;
                }
                if ((b[5] & 0x04) != 0) {
                    modifier |= FLAG_UPSIDE_DOWN;
                }
                exists = (b[5] & 0x80) != 0;
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
                xPos = (b[0] & 0xff) | (b[1] << 8);
                xPos *= scale;
                yPos = (b[2] & 0xff) | (b[3] << 8);
                yPos *= scale;
                width = (b[4] & 0xff) + 1;
                width *= scale;
                height = (b[5] & 0xff) + 1;
                height *= scale;
                negative = (b[6] & 0x01) != 0;
                exists = (b[6] & 0x80) != 0;
                break;
            default:
                throw new Exception(String.format("Unsupported level format: %d", format));
        }
    }
}