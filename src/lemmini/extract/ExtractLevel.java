package lemmini.extract;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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
    private static final int FAKE_OBJECT_CUTOFF = 16;
    private static final int MAX_ENTRANCES = 4;
    private static final int MAX_ENTRANCES_MULTI = 2;
    private static final int MAX_GREEN_FLAGS = 1;
    /** names for default styles */
    private static final String[] STYLES = {"dirt", "fire", "marble", "pillar", "crystal",
        "brick", "rock", "snow", "bubble", "xmas"};
    /** names for default special styles */
    private static final String[] SPECIAL_STYLES = {"awesome", "menace", "beastii", "beasti",
        "covox", "prima", "apple"};

    /** release rate : 0 is slowest, 0x0FA (250) is fastest */
    private static int releaseRate;
    /** number of Lemmings in this level (maximum 0x0072 in original LVL format) */
    private static int numLemmings;
    /** number of Lemmings to rescue : should be less than or equal to number of Lemmings */
    private static int numToRescue;
    /** Time Limit: max 0x00FF, 0x0001 to 0x0009 works best */
    private static int timeLimit;
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
    /**
     * 0x0000 is dirt,  <br>0x0001 is fire,   <br>0x0002 is marble,  <br>
     * 0x0003 is pillar,<br>0x0004 is crystal,<br>0x0005 is brick,   <br>
     * 0x0006 is rock,  <br>0x0007 is snow,   <br>0x0008 is bubble,  <br>
     * 0x0009 is xmas
     */
    private static int style;
    /** special style */
    private static int specialStyle;
    /** superlemming mode used if value is 0xff */
    private static int superlemming;
    private static int dummy;
    /** objects like doors - 32 objects each consists of 8 bytes */
    private static List<LvlObject> objects;
    /** terrain the Lemmings walk on etc. - 400 tiles, 4 bytes each */
    private static List<Terrain> terrain;
    /** steel areas which are indestructible - 32 objects, 4 bytes each */
    private static List<Steel> steel;
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
            if (Files.size(f) != 2048L) {
                throw new Exception("Lemmings level files must be 2048 bytes in size!");
            }
            byte[] buffer = Files.readAllBytes(f);
            b = ByteBuffer.wrap(buffer).asReadOnlyBuffer();
        } catch (IOException e) {
            throw new Exception(String.format("I/O error while reading %s.", fnIn));
        }
        // output file
        try (FileOutputStream f = new FileOutputStream(fnOut);
                OutputStreamWriter w = new OutputStreamWriter(f, StandardCharsets.UTF_8)) {
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
            superlemming = b.get();
            if ((classic && superlemming != 0) || superlemming == -1) {
                w.write("superlemming = true\r\n");
            }
            if (superlemming != 0 && superlemming != -1) {
                w.write("#byte14Value = " + superlemming + "\r\n");
            }
            dummy = b.get();
            if (!(superlemming == 0 && dummy == 0)
                    && !(superlemming == -1 && dummy == -1)) {
                w.write("#byte15Value = " + dummy + "\r\n");
            }
            if (classic) {
                w.write("forceNormalTimerSpeed = true\r\n");
                w.write("classicSteel = true\r\n");
            }
            // read objects
            w.write("\r\n# Objects\r\n");
            w.write("# ID, X position, Y position, paint mode, flags\r\n");
            w.write("# Paint modes: 0 = full, 2 = invisible, 4 = don't overwrite, 8 = visible only on terrain (only one value possible)\r\n");
            w.write("# Flags: 1 = upside down, 2 = fake (combining allowed)\r\n");
            byte[][] bytes = new byte[32][8];
            objects = new ArrayList<>(32);
            for (byte[] bytes1 : bytes) {
                for (int i = 0; i < bytes1.length; i++) {
                    bytes1[i] = b.get();
                }
            }
            int maxObjectID = -1;
            for (int i = bytes.length - 1; i >= 0; i--) {
                int sum = 0;
                for (int j = 0; j < bytes[i].length; j++) {
                    sum += bytes[i][j] & 0xff;
                }
                if (sum != 0) {
                    maxObjectID = i;
                    break;
                }
            }
            for (int i = 0; i <= maxObjectID; i++) {
                int sum = 0;
                for (int j = 0; j < bytes[i].length; j++) {
                    sum += bytes[i][j] & 0xff;
                }
                if (sum != 0) {
                    LvlObject obj = new LvlObject(bytes[i], SCALE);
                    objects.add(obj);
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
                    w.write("object_" + i + " = " + obj.id + ", " + obj.xPos + ", " + obj.yPos + ", " + obj.paintMode + ", " + flags + "\r\n");
                    if ((bytes[i][6] & 0x3f) != 0) {
                        w.write("object_" + i + "_byte6Value = " + bytes[i][6] + "\r\n");
                    }
                    if ((bytes[i][7] & 0x7f) != 0x0f) {
                        w.write("object_" + i + "_byte7Value = " + bytes[i][7] + "\r\n");
                    }
                } else {
                    w.write("object_" + i + " = -1, 0, 0, 0, 0\r\n");
                }
            }
            // read terrain
            w.write("\r\n# Terrain\r\n");
            w.write("# ID, X position, Y position, modifier\r\n");
            w.write("# Modifier: 1 = invisible, 2 = remove, 4 = upside down, 8 = don't overwrite, 16 = fake (combining allowed, 0 = full)\r\n");
            bytes = new byte[400][4];
            terrain = new ArrayList<>(512);
            for (byte[] bytes1 : bytes) {
                for (int i = 0; i < bytes1.length; i++) {
                    bytes1[i] = b.get();
                }
            }
            int maxTerrainID = -1;
            for (int i = bytes.length - 1; i >= 0; i--) {
                int mask = 0xff;
                for (int j = 0; j < bytes[i].length; j++) {
                    mask &= bytes[i][j];
                }
                if (mask != 0xff) {
                    maxTerrainID = i;
                    break;
                }
            }
            int maxValidTerrainID = -1;
            if (!classic) {
                maxValidTerrainID = maxTerrainID;
            } else if (specialStyle < 0) {
                for (int i = 0; i <= maxTerrainID; i++) {
                    int mask = 0xff;
                    for (int j = 0; j < bytes[i].length; j++) {
                        mask &= bytes[i][j];
                    }
                    if (mask != 0xff) {
                        maxValidTerrainID = i;
                    } else {
                        break;
                    }
                }
            }
            for (int i = 0; i <= maxTerrainID; i++) {
                int mask = 0xff;
                for (int j = 0; j < bytes[i].length; j++) {
                    mask &= bytes[i][j];
                }
                if (i > maxValidTerrainID) {
                    w.write("#");
                }
                if (mask != 0xff) {
                    Terrain ter = new Terrain(bytes[i], SCALE);
                    terrain.add(ter);
                    w.write("terrain_" + i + " = " + ter.id + ", " + ter.xPos + ", " + ter.yPos + ", " + ter.modifier + "\r\n");
                } else {
                    w.write("terrain_" + i + " = -1, 0, 0, 0\r\n");
                }
            }
            // read steel blocks
            w.write("\r\n# Steel\r\n");
            w.write("# X position, Y position, width, height\r\n");
            bytes = new byte[32][4];
            steel = new ArrayList<>(32);
            for (byte[] bytes1 : bytes) {
                for (int i = 0; i < bytes1.length; i++) {
                    bytes1[i] = b.get();
                }
            }
            int maxSteelID = -1;
            for (int i = bytes.length - 1; i >= 0; i--) {
                int sum = 0;
                for (int j = 0; j < bytes[i].length; j++) {
                    sum += bytes[i][j] & 0xff;
                }
                if (sum != 0) {
                    maxSteelID = i;
                    break;
                }
            }
            for (int i = 0; i <= maxSteelID; i++) {
                int sum = 0;
                for (int j = 0; j < bytes[i].length; j++) {
                    sum += bytes[i][j] & 0xff;
                }
                if (sum != 0) {
                    Steel stl = new Steel(bytes[i], SCALE);
                    steel.add(stl);
                    w.write("steel_" + i + " = " + stl.xPos + ", " + stl.yPos + ", " + stl.width + ", " + stl.height + "\r\n");
                    if (bytes[i][3] != 0) {
                        w.write("#steel_" + i + "_byte3Value = " + bytes[i][3] + "\r\n");
                    }
                } else {
                    w.write("steel_" + i + " = 0, 0, 0, 0\r\n");
                }
            }
            // read name
            w.write("\r\n# Name" + "\r\n");
            byte[] bName = new byte[32];
            b.get(bName);
            lvlName = new String(bName, StandardCharsets.US_ASCII);
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

    private static final long serialVersionUID = 0x01;

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

    /**
     * Constructor.
     * @param b buffer
     * @param scale Scale (to convert lowres levels into hires levels)
     */
    LvlObject(final byte[] b, final int scale) {
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
        id = ((b[4] & 0xff) << 8) | (b[5] & 0xff);
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
        fake = false;
    }
}

/**
 * Storage class for terrain tiles.
 * @author Volker Oth
 */
class Terrain {
    private static final long serialVersionUID = 0x01;

    /** identifier */
    int id;
    /** x position in pixels */
    int xPos;
    /** y position in pixels */
    int yPos;
    /** modifier - must be one of the above MODEs */
    int modifier;

    /**
     * Constructor.
     * @param b buffer
     * @param scale Scale (to convert lowres levels into hires levels)
     */
    Terrain(final byte[] b, final int scale) {
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
        // terrain id: min 0x00, max 0x7F.  not all graphic sets have all 64 graphics.
        id = b[3] & 0x7f;
    }
}

/**
 *
 * Storage class for steel areas.
 * @author Volker Oth
 */
class Steel {
    private static final long serialVersionUID = 0x01;

    /** x position in pixels */
    int xPos;
    /** y position in pixels */
    int yPos;
    /** width in pixels */
    int width;
    /** height in pixels */
    int height;

    /**
     * Constructor.
     * @param b buffer
     * @param scale Scale (to convert lowres levels into hires levels)
     */
    Steel(final byte[] b, final int scale) { // note: last byte is always 0
        // xpos: 9-bit value: 0x000-0x178).  0x000 = -16, 0x178 = 1580
        xPos = (((b[0] & 0xff) << 1) | ((b[1] & 0x80) >> 7)) * 4 - 16;
        xPos *= scale;
        // ypos: 0x00-0x27. 0x00 = 0, 0x27 = 156 - each hex value represents 4 pixels
        yPos = (b[1] & 0x7f) * 4;
        yPos *= scale;
        // area: 0x00-0xFF.  first nibble is the x-size, from 0-F (represents 4 pixels)
        // second nibble is the y-size. 0x00 = (4,4), 0x11 = (8,8), 0x7F = (32,64)
        width = ((b[2] & 0xf0) >> 4) * 4 + 4;
        width *= scale;
        height = (b[2] & 0xf) * 4 + 4;
        height *= scale;
    }
}