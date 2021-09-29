package lemmini.game;

import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.util.*;
import lemmini.gameutil.Sprite;
import lemmini.graphics.GraphicsBuffer;
import lemmini.graphics.GraphicsContext;
import lemmini.graphics.LemmImage;
import lemmini.tools.Props;
import lemmini.tools.ToolBox;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
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
 * Load a level, paint level, create foreground stencil.
 *
 * @author Volker Oth
 */
public class Level {
    
    private static enum AutosteelMode {
        NONE,
        SIMPLE,
        ADVANCED;
    }
    
    /** default width of level */
    public static final int DEFAULT_WIDTH = 1600 * 2;
    /** default height of level */
    public static final int DEFAULT_HEIGHT = 160 * 2;
    /** array of default ARGB colors for particle effects */
    public static final int[] DEFAULT_PARTICLE_COLORS = {0xff00ff00, 0xff0000ff, 0xffffffff, 0xffffffff, 0xffff0000};
    
    /** color used to erase the foreground */
    public static final Color BLANK_COLOR = new Color(0, 0, 0, 0);
    
    /** list of default styles */
    private static final List<String> STYLES = Arrays.asList("dirt", "fire", "marble", "pillar", "crystal",
        "brick", "rock", "snow", "bubble", "xmas");
    /** list of default special styles */
    private static final List<String> SPECIAL_STYLES = Arrays.asList("awesome", "menace", "beastii", "beasti",
        "covox", "prima", "apple");
    private static final int DEFAULT_TOP_BOUNDARY = 8;
    private static final int DEFAULT_BOTTOM_BOUNDARY = 20;
    private static final int DEFAULT_LEFT_BOUNDARY = 0;
    private static final int DEFAULT_RIGHT_BOUNDARY = -16;
    private static final int BG_BUFFER_PADDING = 4;
    private static final int BG_BUFFER_UNSCALED_INDEX = 0;
    private static final int BG_BUFFER_SCALED_INDEX = 1;
    
    /** default maximum release rate */
    private static final int DEFAULT_MAX_RELEASE_RATE = 99;
    private static final int MAX_MAX_RELEASE_RATE = 106;
    private static final int MIN_RELEASE_RATE = -99;
    
    private final List<Props> levelProps;
    private Map<String, GraphicSet> styles;
    /** the foreground stencil */
    private Stencil stencil;
    /** the foreground image */
    private LemmImage fgImage;
    /** the background images */
    private LemmImage[] bgImages;
    /** array of normal sprite objects - drawn behind foreground image */
    private SpriteObject[] sprObjBehind;
    /** array of special sprite objects - drawn in front of foreground image */
    private SpriteObject[] sprObjFront;
    /** array of all sprite objects (in front and behind) */
    private SpriteObject[] sprObjects;
    /** list of level entrances */
    private List<Entrance> entrances;
    /** release rate: 0 is slowest, 99 is fastest */
    private final int releaseRate;
    private final int maxReleaseRate;
    private final boolean lockReleaseRate;
    /** number of Lemmings in this level (maximum 0x0072 in original LVL format) */
    private final int numLemmings;
    /** number of Lemmings to rescue: should be less than or equal to number of Lemmings */
    private final int numToRescue;
    /** time limit in seconds */
    private final int timeLimitSeconds;
    /** number of climbers in this level */
    private final int numClimbers;
    /** number of floaters in this level */
    private final int numFloaters;
    /** number of bombers in this level */
    private final int numBombers;
    /** number of blockers in this level */
    private final int numBlockers;
    /** number of builders in this level */
    private final int numBuilders;
    /** number of bashers in this level */
    private final int numBashers;
    /** number of miners in this level */
    private final int numMiners;
    /** number of diggers in this level */
    private final int numDiggers;
    private final int[] entranceOrder;
    /** start screen x position */
    private final int xPosCenter;
    /** start screen y position */
    private final int yPosCenter;
    /** array of ARGB colors used for particle effects */
    //private int[] particleCol;
    /** maximum safe fall distance */
    private final int maxFallDistance;
    private final boolean classicSteel;
    private final AutosteelMode autosteelMode;
    /** this level is a SuperLemming level (runs faster) */
    private final boolean superlemming;
    private final boolean forceNormalTimerSpeed;
    /** objects like doors - originally 32 objects where each consists of 8 bytes */
    private final List<LvlObject> objects;
    private final GraphicSet mainStyle;
    private final SpecialGraphicSet specialStyle;
    private final String music;
    /** terrain the Lemmings walk on etc. - originally 400 tiles, 4 bytes each */
    private final List<Terrain> terrain;
    /** steel areas which are indestructible - originally 32 objects, 4 bytes each */
    private final List<Steel> steel;
    private final Background[] backgrounds;
    private final GraphicsBuffer[][] bgBuffers;
    /** level name - originally 32 bytes ASCII filled with whitespace */
    private final String lvlName;
    private final String author;
    private final List<String> hints;
    private final int levelWidth;
    private final int levelHeight;
    private final int topBoundary;
    private final int bottomBoundary;
    private final int leftBoundary;
    private final int rightBoundary;
    
    
    /**
     * Load a level and all level resources.
     * @param res resource object
     * @param level2 level with resources to reuse
     * @throws ResourceException
     * @throws LemmException
     */
    public Level(final Resource res, final Level level2) throws ResourceException, LemmException {
        levelProps = new ArrayList<>(4);
        hints = new ArrayList<>(4);
        // read level properties from file
        Props p = new Props();
        if (!p.load(res)) {
            throw new ResourceException(res);
        }
        levelProps.add(p);
        
        String mainLevel = p.get("mainLevel", StringUtils.EMPTY);
        while (!mainLevel.isEmpty()) {
            Resource res2 = res.getSibling(mainLevel);
            p = new Props();
            if (!p.load(res2)) {
                throw new ResourceException(res2);
            }
            levelProps.add(p);
            mainLevel = p.get("mainLevel", StringUtils.EMPTY);
        }
        
        // read name and author
        lvlName = Props.get(levelProps, "name", StringUtils.EMPTY);
        //out(fname + " - " + lvlName);
        author = Props.get(levelProps, "author", StringUtils.EMPTY);
        // read hints
        for (int i = 0; true; i++) {
            String hint = levelProps.get(0).get("hint_" + i, null);
            if (hint != null) {
                hints.add(hint);
            } else {
                break;
            }
        }
        maxFallDistance = Props.getInt(levelProps, "maxFallDistance", GameController.getCurLevelPack().getMaxFallDistance());
        classicSteel = Props.getBoolean(levelProps, "classicSteel", false);
        switch (p.getInt("autosteelMode", 0)) {
            case 0:
            default:
                autosteelMode = AutosteelMode.NONE;
                break;
            case 1:
                autosteelMode = AutosteelMode.SIMPLE;
                break;
            case 2:
                autosteelMode = AutosteelMode.ADVANCED;
                break;
        }
        levelWidth = p.getInt("width", DEFAULT_WIDTH);
        levelHeight = p.getInt("height", DEFAULT_HEIGHT);
        topBoundary = p.getInt("topBoundary", DEFAULT_TOP_BOUNDARY);
        bottomBoundary = p.getInt("bottomBoundary", DEFAULT_BOTTOM_BOUNDARY);
        leftBoundary = p.getInt("leftBoundary", DEFAULT_LEFT_BOUNDARY);
        rightBoundary = p.getInt("rightBoundary", DEFAULT_RIGHT_BOUNDARY);
        releaseRate = ToolBox.cap(MIN_RELEASE_RATE, Props.getInt(levelProps, "releaseRate", 0), MAX_MAX_RELEASE_RATE);
        //out("releaseRate = " + releaseRate);
        maxReleaseRate = ToolBox.cap(releaseRate, Props.getInt(levelProps, "maxReleaseRate", DEFAULT_MAX_RELEASE_RATE), MAX_MAX_RELEASE_RATE);
        if (releaseRate == maxReleaseRate) {
            lockReleaseRate = true;
        } else {
            lockReleaseRate = Props.getBoolean(levelProps, "lockReleaseRate", false);
        }
        numLemmings = Props.getInt(levelProps, "numLemmings", 1);
        // sanity check: ensure that there are lemmings in the level to avoid division by 0
        if (numLemmings <= 0) {
            throw new LemmException("No lemmings in level.");
        }
        //out("numLemmings = " + numLemmings);
        numToRescue = Props.getInt(levelProps, "numToRescue", 0);
        //out("numToRescue = " + numToRescue);
        int timeLimitSecondsTemp = Integer.MIN_VALUE;
        for (Props p2 : levelProps) {
            timeLimitSecondsTemp = p2.getInt("timeLimitSeconds", Integer.MIN_VALUE);
            if (timeLimitSecondsTemp != Integer.MIN_VALUE) {
                break;
            }
            int timeLimit = p2.getInt("timeLimit", Integer.MIN_VALUE);
            if (timeLimit != Integer.MIN_VALUE) {
                // prevent integer overflow upon conversion to seconds
                if (timeLimit >= Integer.MAX_VALUE / 60 || timeLimit <= Integer.MIN_VALUE / 60) {
                    timeLimit = 0;
                }
                timeLimitSecondsTemp = timeLimit * 60;
                break;
            }
        }
        if (timeLimitSecondsTemp == Integer.MAX_VALUE || timeLimitSecondsTemp < 0) {
            timeLimitSecondsTemp = 0;
        }
        timeLimitSeconds = timeLimitSecondsTemp;
        
        //out("timeLimit = " + timeLimit);
        numClimbers = Props.getInt(levelProps, "numClimbers", 0);
        //out("numClimbers = " + numClimbers);
        numFloaters = Props.getInt(levelProps, "numFloaters", 0);
        //out("numFloaters = " + numFloaters);
        numBombers = Props.getInt(levelProps, "numBombers", 0);
        //out("numBombers = " + numBombers);
        numBlockers = Props.getInt(levelProps, "numBlockers", 0);
        //out("numBlockers = " + numBlockers);
        numBuilders = Props.getInt(levelProps, "numBuilders", 0);
        //out("numBuilders = " + numBuilders);
        numBashers = Props.getInt(levelProps, "numBashers", 0);
        //out("numBashers = " + numBashers);
        numMiners = Props.getInt(levelProps, "numMiners", 0);
        //out("numMiners = " + numMiners);
        numDiggers = Props.getInt(levelProps, "numDiggers", 0);
        //out("numDiggers = " + numDiggers);
        int[] entranceOrderTemp = Props.getIntArray(levelProps, "entranceOrder", null);
        if (ArrayUtils.isEmpty(entranceOrderTemp)) {
            entranceOrderTemp = null;
        }
        entranceOrder = entranceOrderTemp;
        int xPosCenterTemp = Integer.MIN_VALUE;
        for (Props p2 : levelProps) {
            xPosCenterTemp = p2.getInt("xPosCenter", Integer.MIN_VALUE);
            if (xPosCenterTemp != Integer.MIN_VALUE) {
                break;
            }
            int xPos = p2.getInt("xPos", Integer.MIN_VALUE);
            if (xPos != Integer.MIN_VALUE) {
                xPosCenterTemp = xPos + 400;
                break;
            }
        }
        if (xPosCenterTemp == Integer.MIN_VALUE) {
            xPosCenterTemp = 0;
        }
        xPosCenter = xPosCenterTemp;
        yPosCenter = Props.getInt(levelProps, "yPosCenter", 0);
        //out("xPosCenter = " + xPosCenter);
        String styleName = p.get("style", StringUtils.EMPTY);
        //out("style = " + style);
        String specialStyleName = p.get("specialStyle", StringUtils.EMPTY);
        //out("specialStyle = " + specialStyle);
        music = Props.get(levelProps, "music", null);
        superlemming = Props.getBoolean(levelProps, "superlemming", false);
        forceNormalTimerSpeed = Props.getBoolean(levelProps, "forceNormalTimerSpeed", false);
        
        styles = new HashMap<>(16);
        mainStyle = new GraphicSet(styleName);
        styles.put(styleName.toLowerCase(Locale.ROOT), mainStyle);
        if (!specialStyleName.isEmpty()) {
            specialStyle = new SpecialGraphicSet(specialStyleName);
        } else {
            specialStyle = null;
        }
        
        // read objects
        //out("\n[Objects]");
        objects = new ArrayList<>(64);
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            String[] val = p.getArray("object_" + i, null);
            if (val != null && val.length >= 5) {
                LvlObject obj = new LvlObject(val);
                objects.add(obj);
                //out(obj.id + ", " + obj.xPos + ", " + obj.yPos + ", "+ obj.paintMode + ", " + obj.upsideDown);
            } else {
                break;
            }
        }
        // read terrain
        //out("\n[Terrain]");
        terrain = new ArrayList<>(512);
        if (specialStyle != null) {
            String positionX;
            String positionY;
            positionX = p.get("specialStylePositionX", Integer.toString(specialStyle.getPositionX()));
            positionY = p.get("specialStylePositionY", Integer.toString(specialStyle.getPositionY()));
            Terrain ter = new Terrain(new String[]{"0", positionX, positionY, "0"}, true);
            terrain.add(ter);
        }
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            String[] val = p.getArray("terrain_" + i, null);
            if (val != null && val.length >= 4) {
                Terrain ter = new Terrain(val, false);
                terrain.add(ter);
                //out(ter.id + ", " + ter.xPos + ", " + ter.yPos + ", " + ter.modifier);
            } else {
                break;
            }
        }
        // read steel blocks
        //out("\n[Steel]");
        steel = new ArrayList<>(64);
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            int[] val = p.getIntArray("steel_" + i, null);
            if (val != null && val.length >= 4) {
                Steel stl = new Steel(val);
                steel.add(stl);
                //out(stl.xPos + ", " + stl.yPos + ", " + stl.width + ", " + stl.height);
            } else {
                break;
            }
        }
        // read background
        List<Background> backgroundList = new ArrayList<>(4);
        List<GraphicsBuffer[]> bgBufferList = new ArrayList<>(4);
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            int bgWidth = p.getInt("bg_" + i + "_width", 0);
            int bgHeight = p.getInt("bg_" + i + "_height", 0);
            if (bgWidth <= 0 || bgHeight <= 0) {
                break;
            }
            boolean bgTiled = p.getBoolean("bg_" + i + "_tiled", false);
            int bgTint = p.getInt("bg_" + i + "_tint", 0);
            int bgOffsetX = p.getInt("bg_" + i + "_offsetX", 0);
            int bgOffsetY = p.getInt("bg_" + i + "_offsetY", 0);
            double bgScrollSpeedX = p.getDouble("bg_" + i + "_scrollSpeedX", 0.0);
            double bgScrollSpeedY = p.getDouble("bg_" + i + "_scrollSpeedY", 0.0);
            double bgScale = p.getDouble("bg_" + i + "_scale", 1.0);
            List<LvlObject> bgObjects = new ArrayList<>(16);
            for (int j = 0; i < Integer.MAX_VALUE; j++) {
                String[] val = p.getArray("bg_" + i + "_object_" + j, null);
                if (val != null && val.length >= 5) {
                    LvlObject obj = new LvlObject(val);
                    bgObjects.add(obj);
                } else {
                    break;
                }
            }
            List<Terrain> bgTerrain = new ArrayList<>(256);
            for (int j = 0; i < Integer.MAX_VALUE; j++) {
                String[] val = p.getArray("bg_" + i + "_terrain_" + j, null);
                if (val != null && val.length >= 4) {
                    Terrain ter = new Terrain(val, false);
                    bgTerrain.add(ter);
                } else {
                    break;
                }
            }
            backgroundList.add(new Background(bgWidth, bgHeight, bgObjects, bgTerrain, bgTiled, bgTint,
                    bgOffsetX, bgOffsetY, bgScrollSpeedX, bgScrollSpeedY, bgScale));
            GraphicsBuffer[] bgBufferListEntry = new GraphicsBuffer[2];
            bgBufferListEntry[BG_BUFFER_UNSCALED_INDEX] = new GraphicsBuffer(
                    bgWidth + BG_BUFFER_PADDING * 2, bgHeight + BG_BUFFER_PADDING * 2,
                    Transparency.TRANSLUCENT, false);
            bgBufferListEntry[BG_BUFFER_SCALED_INDEX] = new GraphicsBuffer(
                    ToolBox.scale(bgWidth, bgScale), ToolBox.scale(bgHeight, bgScale),
                    Transparency.TRANSLUCENT, false);
            bgBufferList.add(bgBufferListEntry);
        }
        backgrounds = backgroundList.toArray(new Background[backgroundList.size()]);
        bgBuffers = bgBufferList.toArray(new GraphicsBuffer[bgBufferList.size()][]);
        
        if (level2 != null) {
            fgImage = level2.fgImage;
            level2.fgImage = null;
            stencil = level2.stencil;
            level2.stencil = null;
        }
        
        Lemming.replaceColors(getDebrisColor(), getDebrisColor2());
    }
    
    /**
     * Paint a level.
     */
    void paintLevel() throws ResourceException, LemmException {
        // flush all resources
        sprObjFront = null;
        sprObjBehind = null;
        sprObjects = null;
        entrances = null;
        System.gc();
        // create images and stencil
        if (fgImage != null && fgImage.getWidth() == levelWidth && fgImage.getHeight() == levelHeight) {
            GraphicsContext gfx = null;
            try {
                gfx = fgImage.createGraphicsContext();
                gfx.setBackground(BLANK_COLOR);
                gfx.clearRect(0, 0, fgImage.getWidth(), fgImage.getHeight());
            } finally {
                if (gfx != null) {
                    gfx.dispose();
                }
            }
        } else {
            fgImage = ToolBox.createLemmImage(levelWidth, levelHeight);
        }
        bgImages = new LemmImage[backgrounds.length];
        for (int i = 0; i < backgrounds.length; i++) {
            bgImages[i] = ToolBox.createLemmImage(
                    backgrounds[i].width + BG_BUFFER_PADDING * 2, backgrounds[i].height + BG_BUFFER_PADDING * 2);
        }
        if (stencil != null && stencil.getWidth() == levelWidth && stencil.getHeight() == levelHeight) {
            stencil.clear();
        } else {
            stencil = new Stencil(levelWidth, levelHeight);
        }
        // paint terrain
        for (Terrain t : terrain) {
            if (t.id < 0) {
                continue;
            }
            String styleLowerCase = t.style.toLowerCase(Locale.ROOT);
            if (!styles.containsKey(styleLowerCase)) {
                styles.put(styleLowerCase, new GraphicSet(t.style));
            }
            GraphicSet terrainStyle = styles.get(styleLowerCase);
            
            LemmImage i;
            boolean[][] mask;
            boolean[][] steelMask;
            int width;
            int height;
            int maskWidth;
            int maskHeight;
            int steelMaskWidth;
            int steelMaskHeight;
            boolean isSteel;
            if (t.specialGraphic) {
                i = specialStyle.getImage();
                mask = specialStyle.getMask();
                steelMask = specialStyle.getSteelMask();
                width = i.getWidth();
                height = i.getHeight();
                maskWidth = ArrayUtils.isNotEmpty(mask) ? mask[0].length : 0;
                maskHeight = mask.length;
                steelMaskWidth = ArrayUtils.isNotEmpty(steelMask) ? steelMask[0].length : 0;
                steelMaskHeight = steelMask.length;
                isSteel = true;
            } else {
                GraphicSet.Terrain t2 = terrainStyle.getTerrain(t.id);
                i = t2.getImage();
                mask = t2.getMask();
                steelMask = t2.getSteelMask();
                width = i.getWidth();
                height = i.getHeight();
                maskWidth = ArrayUtils.isNotEmpty(mask) ? mask[0].length : 0;
                maskHeight = mask.length;
                steelMaskWidth = ArrayUtils.isNotEmpty(steelMask) ? steelMask[0].length : 0;
                steelMaskHeight = steelMask.length;
                isSteel = t2.isSteel();
            }
            if (autosteelMode == AutosteelMode.NONE) {
                isSteel = false;
            }
            
            int tx = t.xPos;
            int ty = t.yPos;
            boolean rotate = BooleanUtils.toBoolean(t.modifier & Terrain.MODE_ROTATE);
            boolean noOneWay = BooleanUtils.toBoolean(t.modifier & Terrain.MODE_NO_ONE_WAY);
            boolean flipHorizontally = BooleanUtils.toBoolean(t.modifier & Terrain.MODE_FLIP_HORIZONTALLY);
            boolean fake = BooleanUtils.toBoolean(t.modifier & Terrain.MODE_FAKE);
            boolean upsideDown = BooleanUtils.toBoolean(t.modifier & Terrain.MODE_UPSIDE_DOWN);
            boolean noOverwrite = BooleanUtils.toBoolean(t.modifier & Terrain.MODE_NO_OVERWRITE);
            boolean remove = !noOverwrite && BooleanUtils.toBoolean(t.modifier & Terrain.MODE_REMOVE);
            boolean invisible = BooleanUtils.toBoolean(t.modifier & Terrain.MODE_INVISIBLE);
            int width2 = width;
            int height2 = height;
            if (rotate) {
                width = height2;
                height = width2;
            }
            
            for (int y = 0; y < height; y++) {
                if (y + ty < 0 || y + ty >= levelHeight) {
                    continue;
                }
                for (int x = 0; x < width; x++) {
                    if (x + tx < 0 || x + tx >= levelWidth) {
                        continue;
                    }
                    int x2 = x;
                    int y2 = y;
                    if (upsideDown) {
                        y2 = height - 1 - y2;
                    }
                    if (flipHorizontally) {
                        x2 = width - 1 - x2;
                    }
                    if (rotate) {
                        int oldX2 = x2;
                        x2 = y2;
                        y2 = height2 - 1 - oldX2;
                    }
                    int col = i.getRGB(x2, y2);
                    int alpha = (col >>> 24) & 0xff;
                    boolean isPixelOpaque = alpha >= 0x80;
                    // ignore transparent pixels
                    if (!invisible && (col & 0xff000000) != 0) {
                        if (noOverwrite) {
                            if (noOneWay && isPixelOpaque && !fgImage.isPixelOpaque(x + tx, y + ty)) {
                                stencil.orMask(x + tx, y + ty, Stencil.MSK_NO_ONE_WAY_DRAW);
                            }
                            fgImage.addRGBBehind(x + tx, y + ty, col);
                        } else if (remove) {
                            if (noOneWay && isPixelOpaque) {
                                stencil.andMask(x + tx, y + ty, ~Stencil.MSK_NO_ONE_WAY_DRAW);
                            }
                            fgImage.removeAlpha(x + tx, y + ty, alpha);
                        } else {
                            if (isPixelOpaque) {
                                if (noOneWay) {
                                    stencil.orMask(x + tx, y + ty, Stencil.MSK_NO_ONE_WAY_DRAW);
                                } else {
                                    stencil.andMask(x + tx, y + ty, ~Stencil.MSK_NO_ONE_WAY_DRAW);
                                }
                            }
                            fgImage.addRGB(x + tx, y + ty, col);
                        }
                    }
                    if (!fake && y2 < maskHeight && x2 < maskWidth && mask[y2][x2]) {
                        int newMask;
                        if (remove) {
                            newMask = stencil.getMask(x + tx, y + ty) & Stencil.MSK_NO_ONE_WAY_DRAW;
                        } else if (noOverwrite) {
                            newMask = stencil.getMask(x + tx, y + ty) | Stencil.MSK_BRICK;
                            if (noOneWay) {
                                newMask |= Stencil.MSK_NO_ONE_WAY;
                            }
                        } else {
                            newMask = stencil.getMask(x + tx, y + ty) | Stencil.MSK_BRICK;
                            if (noOneWay) {
                                newMask |= Stencil.MSK_NO_ONE_WAY;
                            } else {
                                newMask &= ~Stencil.MSK_NO_ONE_WAY;
                            }
                        }
                        stencil.setMask(x + tx, y + ty, newMask);
                    }
                    if (!fake && y2 < steelMaskHeight && x2 < steelMaskWidth && steelMask[y2][x2]) {
                        int newMask = stencil.getMask(x + tx, y + ty);
                        if (remove) {
                        } else if (noOverwrite) {
                            switch (autosteelMode) {
                                case NONE:
                                default:
                                    break;
                                case SIMPLE:
                                    if (isSteel) {
                                        newMask |= Stencil.MSK_STEEL_BRICK;
                                    }
                                    break;
                                case ADVANCED:
                                    if (isSteel && !BooleanUtils.toBoolean(stencil.getMask(x + tx, y + ty) & Stencil.MSK_BRICK)) {
                                        newMask |= Stencil.MSK_STEEL_BRICK;
                                    }
                                    break;
                            }
                        } else {
                            switch (autosteelMode) {
                                case NONE:
                                default:
                                    break;
                                case SIMPLE:
                                    if (isSteel) {
                                        newMask |= Stencil.MSK_STEEL_BRICK;
                                    }
                                    break;
                                case ADVANCED:
                                    if (isSteel) {
                                        newMask |= Stencil.MSK_STEEL_BRICK;
                                    } else {
                                        newMask &= ~Stencil.MSK_STEEL_BRICK;
                                    }
                                    break;
                            }
                        }
                        stencil.setMask(x + tx, y + ty, newMask);
                    }
                }
            }
        }
        
        // paint steel tiles into stencil
        steel.stream().forEachOrdered(stl -> {
            int sx = stl.xPos;
            int sy = stl.yPos;
            for (int y = 0; y < stl.height; y++) {
                if (y + sy < 0 || y + sy >= levelHeight) {
                    continue;
                }
                for (int x = 0; x < stl.width; x++) {
                    if ((!classicSteel && !BooleanUtils.toBoolean(stencil.getMask(x + sx, y + sy) & Stencil.MSK_BRICK))
                            || x + sx < 0 || x + sx >= levelWidth) {
                        continue;
                    }
                    if (stl.negative) {
                        stencil.andMask(x + sx, y + sy, ~Stencil.MSK_STEEL_BRICK);
                    } else {
                        stencil.orMask(x + sx, y + sy, Stencil.MSK_STEEL_BRICK);
                    }
                }
            }
        });
        
        // now for the animated objects
        List<SpriteObject> oCombined = new ArrayList<>(64);
        List<SpriteObject> oBehind = new ArrayList<>(64);
        List<SpriteObject> oFront = new ArrayList<>(64);
        entrances = new ArrayList<>(8);
        for (ListIterator<LvlObject> lit = objects.listIterator(); lit.hasNext(); ) {
            int n = lit.nextIndex();
            LvlObject o = lit.next();
            if (o.id < 0) {
                oCombined.add(null);
                continue;
            }
            String styleLowerCase = o.style.toLowerCase(Locale.ROOT);
            if (!styles.containsKey(styleLowerCase)) {
                styles.put(styleLowerCase, new GraphicSet(o.style));
            }
            GraphicSet objectStyle = styles.get(styleLowerCase);
            
            // flags
            boolean rotate = BooleanUtils.toBoolean(o.flags & LvlObject.FLAG_ROTATE);
            boolean upsideDown = BooleanUtils.toBoolean(o.flags & LvlObject.FLAG_UPSIDE_DOWN);
            boolean fake = BooleanUtils.toBoolean(o.flags & LvlObject.FLAG_FAKE);
            boolean upsideDownMask = BooleanUtils.toBoolean(o.flags & LvlObject.FLAG_UPSIDE_DOWN_MASK);
            boolean flipHorizontally = BooleanUtils.toBoolean(o.flags & LvlObject.FLAG_FLIP_HORIZONTALLY);
            // create sprite object
            GraphicSet.LvlObject o2 = objectStyle.getObject(o.id);
            SpriteObject spr = new SpriteObject(o2, GraphicSet.Orientation.getOrientation(flipHorizontally, upsideDown, rotate), false);
            spr.setX(o.xPos);
            spr.setY(o.yPos);
            // check for entrances
            if (spr.getType() == SpriteObject.Type.ENTRANCE && !fake) {
                Entrance e = new Entrance(o.xPos + spr.getWidth() / 2 + spr.getMaskOffsetX(),
                        o.yPos + spr.getMaskOffsetY(), BooleanUtils.toBoolean(o.objSpecificModifier & LvlObject.OPTION_ENTRANCE_LEFT));
                e.id = oCombined.size();
                entrances.add(e);
            }
            // animated
            boolean invisible = BooleanUtils.toBoolean(o.paintMode & LvlObject.MODE_INVISIBLE);
            boolean drawOnVis = !invisible && BooleanUtils.toBoolean(o.paintMode & LvlObject.MODE_VIS_ON_TERRAIN);
            boolean noOverwrite = !drawOnVis && BooleanUtils.toBoolean(o.paintMode & LvlObject.MODE_NO_OVERWRITE);
            boolean inFront = !invisible && !noOverwrite;
            boolean drawFull = inFront && !drawOnVis;
            
            spr.setVisOnTerrain(drawOnVis);
            
            if (!invisible) {
                if (inFront) {
                    oFront.add(spr);
                } else {
                    oBehind.add(spr);
                }
            }
            oCombined.add(spr);
            
            for (int y = 0; y < spr.getHeight(); y++) {
                if (y + spr.getY() < 0 || y + spr.getY() >= levelHeight) {
                    continue;
                }
                for (int x = 0; x < spr.getWidth(); x++) {
                    if (x + spr.getX() < 0 || x + spr.getX() >= levelWidth) {
                        continue;
                    }
                    stencil.addID(spr.getX() + x, spr.getY() + y, n);
                }
            }
            
            // draw stencil
            if (!fake) {
                for (int y = spr.getMaskOffsetY(); y < spr.getMaskHeight() + spr.getMaskOffsetY(); y++) {
                    for (int x = spr.getMaskOffsetX(); x < spr.getMaskWidth() + spr.getMaskOffsetX(); x++) {
                        int xDest = x;
                        int yDest = y;
                        if (rotate) {
                            int oldXDest = xDest;
                            xDest = spr.getWidth() - 1 - yDest + Lemming.HEIGHT / 2;
                            yDest = oldXDest + Lemming.HEIGHT / 2;
                        }
                        if (flipHorizontally) {
                            xDest = spr.getWidth() - xDest - 1;
                        }
                        if (upsideDownMask) {
                            yDest = spr.getHeight() - yDest - 1;
                            if (spr.getType().isTriggeredByFoot()) {
                                yDest += Lemming.HEIGHT;
                            }
                        }
                        int stencilMask = stencil.getMask(xDest + spr.getX(), yDest + spr.getY());
                        int maskType = spr.getMaskType();
                        if ((!classicSteel
                                        && !spr.getType().isTriggeredByFoot()
                                        && !BooleanUtils.toBoolean(stencilMask & Stencil.MSK_BRICK))
                                || (spr.getType().isOneWay()
                                        && BooleanUtils.toBoolean(stencilMask & Stencil.MSK_NO_ONE_WAY))
                                || xDest + spr.getX() < 0 || xDest + spr.getX() >= levelWidth) {
                            continue;
                        }
                        // manage collision mask
                        // now read stencil
                        if (spr.getMask(x, y)) { // not transparent
                            stencilMask &= Stencil.MSK_BRICK
                                    | Stencil.MSK_STEEL_BRICK
                                    | Stencil.MSK_NO_ONE_WAY
                                    | Stencil.MSK_NO_ONE_WAY_DRAW;
                            stencilMask |= maskType;
                            stencil.setMask(spr.getX() + xDest, yDest + spr.getY(), stencilMask);
                            stencil.setMaskObjectID(spr.getX() + xDest, yDest + spr.getY(), n);
                        }
                    }
                }
            }
            if (!invisible) {
                // get flipped or normal version
                for (int y = 0; y < spr.getHeight(); y++) {
                    for (int x = 0; x < spr.getWidth(); x++) {
                        boolean paint = true;
                        if (x + spr.getX() < 0 || x + spr.getX() >= levelWidth
                                || y + spr.getY() < 0 || y + spr.getY() >= levelHeight) {
                            paint = false;
                        } else if (inFront) {
                            // now read terrain image
                            boolean opaque = fgImage.isPixelOpaque(x + spr.getX(), y + spr.getY());
                            int stencilMask = stencil.getMask(x + spr.getX(), y + spr.getY());
                            paint = (drawFull || (opaque && drawOnVis))
                                    && !(spr.getType().isOneWay() && BooleanUtils.toBoolean(stencilMask & Stencil.MSK_NO_ONE_WAY_DRAW));
                        }
                        if (!paint) {
                            spr.setPixelVisibility(x, y, false); // set transparent
                        }
                    }
                }
            }
        }
        
        // paint background
        if (ArrayUtils.isNotEmpty(bgImages)) {
            List<SpriteObject> bgOCombined = new ArrayList<>(32);
            List<SpriteObject> bgOFront = new ArrayList<>(32);
            List<SpriteObject> bgOBehind = new ArrayList<>(32);
            for (int m = 0; m < backgrounds.length; m++) {
                Background bg = backgrounds[m];
                LemmImage targetBg = bgImages[m];
                LemmImage unpaddedBg = ToolBox.createLemmImage(
                        targetBg.getWidth() - BG_BUFFER_PADDING * 2,
                        targetBg.getHeight() - BG_BUFFER_PADDING * 2);
                bgOCombined.clear();
                bgOBehind.clear();
                bgOFront.clear();
                
                for (Terrain t : terrain) {
                    if (t.id < 0) {
                        continue;
                    }
                    String styleLowerCase = t.style.toLowerCase(Locale.ROOT);
                    if (!styles.containsKey(styleLowerCase)) {
                        styles.put(styleLowerCase, new GraphicSet(t.style));
                    }
                    GraphicSet terrainStyle = styles.get(styleLowerCase);
                    
                    LemmImage i = terrainStyle.getTerrain(t.id).getImage();
                    int width = i.getWidth();
                    int height = i.getHeight();
                    
                    int tx = t.xPos;
                    int ty = t.yPos;
                    boolean rotate = BooleanUtils.toBoolean(t.modifier & Terrain.MODE_ROTATE);
                    boolean flipHorizontally = BooleanUtils.toBoolean(t.modifier & Terrain.MODE_FLIP_HORIZONTALLY);
                    boolean upsideDown = BooleanUtils.toBoolean(t.modifier & Terrain.MODE_UPSIDE_DOWN);
                    boolean overwrite = !BooleanUtils.toBoolean(t.modifier & Terrain.MODE_NO_OVERWRITE);
                    boolean remove = BooleanUtils.toBoolean(t.modifier & Terrain.MODE_REMOVE);
                    int width2 = width;
                    int height2 = height;
                    if (rotate) {
                        width = height2;
                        height = width2;
                    }
                    
                    for (int y = 0; y < height; y++) {
                        if (y + ty < 0 || y + ty >= unpaddedBg.getHeight()) {
                            continue;
                        }
                        for (int x = 0; x < width; x++) {
                            if (x + tx < 0 || x + tx >= unpaddedBg.getWidth()) {
                                continue;
                            }
                            int x2 = x;
                            int y2 = y;
                            if (upsideDown) {
                                y2 = height - 1 - y2;
                            }
                            if (flipHorizontally) {
                                x2 = width - 1 - x2;
                            }
                            if (rotate) {
                                int oldX2 = x2;
                                x2 = y2;
                                y2 = height2 - 1 - oldX2;
                            }
                            int col = i.getRGB(x2, y2);
                            // ignore transparent pixels
                            if ((col & 0xff000000) != 0) {
                                if (!overwrite) {
                                    unpaddedBg.addRGBBehind(x + tx, y + ty, col);
                                } else if (remove) {
                                    unpaddedBg.removeAlpha(x + tx, y + ty, (col >>> 24) & 0xff);
                                } else {
                                    unpaddedBg.addRGB(x + tx, y + ty, col);
                                }
                            }
                        }
                    }
                }
                unpaddedBg.applyTint(bg.tint);
                
                GraphicsContext targetBgGfx = null;
                try {
                    targetBgGfx = targetBg.createGraphicsContext();
                    for (int y = BG_BUFFER_PADDING - (bg.tiled ? unpaddedBg.getHeight() : 0), j = 0; j < (bg.tiled ? 3 : 1); y += unpaddedBg.getHeight(), j++) {
                        for (int x = BG_BUFFER_PADDING - (bg.tiled ? unpaddedBg.getWidth() : 0), k = 0; k < (bg.tiled ? 3 : 1);  x += unpaddedBg.getWidth(), k++) {
                            targetBgGfx.drawImage(unpaddedBg, x, y);
                        }
                    }
                } finally {
                    if (targetBgGfx != null) {
                        targetBgGfx.dispose();
                    }
                }
                
                for (LvlObject o : objects) {
                    if (o.id < 0) {
                        oCombined.add(null);
                        continue;
                    }
                    String styleLowerCase = o.style.toLowerCase(Locale.ROOT);
                    if (!styles.containsKey(styleLowerCase)) {
                        styles.put(styleLowerCase, new GraphicSet(o.style));
                    }
                    GraphicSet objectStyle = styles.get(styleLowerCase);
                    
                    // flags
                    boolean rotate = BooleanUtils.toBoolean(o.flags & LvlObject.FLAG_ROTATE);
                    boolean upsideDown = BooleanUtils.toBoolean(o.flags & LvlObject.FLAG_UPSIDE_DOWN);
                    boolean flipHorizontally = BooleanUtils.toBoolean(o.flags & LvlObject.FLAG_FLIP_HORIZONTALLY);
                    // create sprite object
                    GraphicSet.LvlObject o2 = objectStyle.getObject(o.id);
                    SpriteObject spr = new SpriteObject(o2, GraphicSet.Orientation.getOrientation(flipHorizontally, upsideDown, rotate), false);
                    spr.setX(o.xPos);
                    spr.setY(o.yPos);
                    // animated
                    boolean invisible = BooleanUtils.toBoolean(o.paintMode & LvlObject.MODE_INVISIBLE);
                    boolean drawOnVis = !invisible && BooleanUtils.toBoolean(o.paintMode & LvlObject.MODE_VIS_ON_TERRAIN);
                    boolean noOverwrite = !drawOnVis && BooleanUtils.toBoolean(o.paintMode & LvlObject.MODE_NO_OVERWRITE);
                    boolean inFront = !invisible && !noOverwrite;
                    boolean drawFull = inFront && !drawOnVis;
                    
                    spr.setVisOnTerrain(drawOnVis);
                    
                    if (inFront) {
                        bgOFront.add(spr);
                    } else {
                        bgOBehind.add(spr);
                    }
                    bgOCombined.add(spr);
                    
                    if (!invisible) {
                        // get flipped or normal version
                        for (int y = 0; y < spr.getHeight(); y++) {
                            for (int x = 0; x < spr.getWidth(); x++) {
                                boolean paint = true;
                                if (x + spr.getX() < 0 || x + spr.getX() >= unpaddedBg.getWidth()
                                        || y + spr.getY() < 0 || y + spr.getY() >= unpaddedBg.getHeight()) {
                                    paint = false;
                                } else if (inFront) {
                                    // now read terrain image
                                    boolean opaque = unpaddedBg.isPixelOpaque(x + spr.getX(), y + spr.getY());
                                    paint = drawFull || (opaque && drawOnVis);
                                }
                                if (!paint) {
                                    spr.setPixelVisibility(x, y, false); // set transparent
                                }
                            }
                        }
                    }
                    
                    spr.applyTint(bg.tint);
                }
                
                bg.sprObjects = bgOCombined.toArray(new SpriteObject[bgOCombined.size()]);
                bg.sprObjFront = bgOFront.toArray(new SpriteObject[bgOFront.size()]);
                bg.sprObjBehind = bgOBehind.toArray(new SpriteObject[bgOBehind.size()]);
            }
        }
        
        sprObjects = oCombined.toArray(new SpriteObject[oCombined.size()]);
        sprObjFront = oFront.toArray(new SpriteObject[oFront.size()]);
        sprObjBehind = oBehind.toArray(new SpriteObject[oBehind.size()]);
        System.gc();
    }
    
    public LemmImage getFgImage() {
        return fgImage;
    }
    
    public Stencil getStencil() {
        return stencil;
    }
    
    /**
     * Draw opaque objects behind foreground image.
     * @param g graphics object to draw on
     * @param width width of screen
     * @param height height of screen
     * @param xOfs level offset x position
     * @param yOfs level offset y position
     */
    public void drawBehindObjects(final GraphicsContext g, final int width, final int height,
            final int xOfs, final int yOfs) {
        // draw "behind" objects
        if (sprObjBehind != null) {
            for (int n = sprObjBehind.length - 1; n >= 0; n--) {
                SpriteObject spr = sprObjBehind[n];
                LemmImage img = spr.getImage();
                if (spr.getX() + spr.getWidth() > xOfs && spr.getX() < xOfs + width
                        && spr.getY() + spr.getHeight() > yOfs && spr.getY() < yOfs + height) {
                    g.drawImage(img, spr.getX() - xOfs, spr.getY() - yOfs);
                }
            }
        }
    }
    
    /**
     * Draw transparent objects in front of foreground image.
     * @param g graphics object to draw on
     * @param width width of screen
     * @param height height of screen
     * @param xOfs level offset x position
     * @param yOfs level offset y position
     */
    public void drawInFrontObjects(final GraphicsContext g, final int width, final int height,
            final int xOfs, final int yOfs) {
        // draw "in front" objects
        if (sprObjFront != null) {
            for (SpriteObject spr : sprObjFront) {
                LemmImage img = spr.getImage();
                if (spr.getX() + spr.getWidth() > xOfs && spr.getX() < xOfs + width
                        && spr.getY() + spr.getHeight() > yOfs && spr.getY() < yOfs + height) {
                    g.drawImage(img, spr.getX() - xOfs, spr.getY() - yOfs);
                }
            }
        }
    }
    
    /**
     * Draw the background layers behind the foreground image.
     * @param g graphics object to draw on
     * @param width width of screen
     * @param height height of screen
     * @param xOfs level offset x position
     * @param yOfs level offset y position
     */
    public void drawBackground(final GraphicsContext g, final int width, final int height,
            final int xOfs, final int yOfs) {
        if (ArrayUtils.isEmpty(bgImages)) {
            return;
        }
        
        for (int i = backgrounds.length - 1; i >= 0; i--) {
            LemmImage bgImage = bgImages[i];
            Background bg = backgrounds[i];
            GraphicsBuffer[] buffers = bgBuffers[i];
            
            int bgImageWidth = bgImage.getWidth() - BG_BUFFER_PADDING * 2;
            int bgImageWidthScaled = ToolBox.scale(bgImageWidth, bg.scale);
            int bgImageHeight = bgImage.getHeight() - BG_BUFFER_PADDING * 2;
            int bgImageHeightScaled = ToolBox.scale(bgImageHeight, bg.scale);
            int bgBufferPaddingScaled = ToolBox.scale(BG_BUFFER_PADDING, bg.scale);
            
            if (bgImageWidthScaled <= 0 || bgImageHeightScaled <= 0) {
                continue;
            }
            
            LemmImage unscaledBufferImg = buffers[BG_BUFFER_UNSCALED_INDEX].getImage();
            GraphicsContext unscaledBufferGfx = buffers[BG_BUFFER_UNSCALED_INDEX].getGraphicsContext();
            unscaledBufferGfx.clearRect(0, 0, unscaledBufferImg.getWidth(), unscaledBufferImg.getHeight());
            
            for (int y = BG_BUFFER_PADDING - (bg.tiled ? bgImageHeight : 0), j = 0; j < (bg.tiled ? 3 : 1); y += bgImageHeight, j++) {
                for (int x = BG_BUFFER_PADDING - (bg.tiled ? bgImageWidth : 0), k = 0; k < (bg.tiled ? 3 : 1);  x += bgImageWidth, k++) {
                    // draw "behind" objects
                    if (bg.sprObjBehind != null) {
                        for (int n = bg.sprObjBehind.length - 1; n >= 0; n--) {
                            SpriteObject spr = bg.sprObjBehind[n];
                            LemmImage img = spr.getImage();
                            unscaledBufferGfx.drawImage(img, x + spr.getX(), y + spr.getY());
                        }
                    }
                }
            }
            
            unscaledBufferGfx.drawImage(bgImage, 0, 0);
            
            for (int y = BG_BUFFER_PADDING - (bg.tiled ? bgImageHeight : 0), j = 0; j < (bg.tiled ? 3 : 1); y += bgImageHeight, j++) {
                for (int x = BG_BUFFER_PADDING - (bg.tiled ? bgImageWidth : 0), k = 0; k < (bg.tiled ? 3 : 1);  x += bgImageWidth, k++) {
                    // draw "in front" objects
                    if (bg.sprObjFront != null) {
                        for (SpriteObject spr : bg.sprObjFront) {
                            LemmImage img = spr.getImage();
                            unscaledBufferGfx.drawImage(img, x + spr.getX(), y + spr.getY());
                        }
                    }
                }
            }
            
            LemmImage scaledBufferImg = buffers[BG_BUFFER_SCALED_INDEX].getImage();
            GraphicsContext scaledBufferGfx = buffers[BG_BUFFER_SCALED_INDEX].getGraphicsContext();
            scaledBufferGfx.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    Core.isBilinear()
                            ? RenderingHints.VALUE_INTERPOLATION_BILINEAR
                            : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            scaledBufferGfx.clearRect(0, 0, scaledBufferImg.getWidth(), scaledBufferImg.getHeight());
            scaledBufferGfx.drawImage(unscaledBufferImg,
                    -bgBufferPaddingScaled, -bgBufferPaddingScaled,
                    bgImageWidthScaled + bgBufferPaddingScaled * 2, bgImageHeightScaled + bgBufferPaddingScaled * 2);
            
            int xOfsNew = (int) (-xOfs * bg.scrollSpeedX) + bg.offsetX;
            int yOfsNew = (int) (-yOfs * bg.scrollSpeedY) + bg.offsetY;
            if (bg.tiled) {
                xOfsNew %= bgImageWidthScaled;
                xOfsNew -= (xOfsNew > 0) ? bgImageWidthScaled : 0;
                yOfsNew %= bgImageHeightScaled;
                yOfsNew -= (yOfsNew > 0) ? bgImageHeightScaled : 0;
            }
            
            for (int y = yOfsNew; y < height; y += bgImageHeightScaled) {
                for (int x = xOfsNew; x < width; x += bgImageWidthScaled) {
                    g.drawImage(scaledBufferImg, x, y);
                    if (!bg.tiled) {
                        break;
                    }
                }
                if (!bg.tiled) {
                    break;
                }
            }
        }
    }
    
    public void advanceBackgroundFrame() {
        for (Background bg : backgrounds) {
            for (SpriteObject spr : bg.sprObjects) {
                if (spr != null) {
                    spr.getImageAnim(); // just to animate
                }
            }
        }
    }
    
    public void openBackgroundEntrances() {
        for (Background bg : backgrounds) {
            for (SpriteObject spr : bg.sprObjects) {
                if (spr != null && spr.getAnimMode() == Sprite.Animation.ONCE_ENTRANCE) {
                    spr.setAnimMode(Sprite.Animation.ONCE);
                }
            }
        }
    }
    
    ///**
    // * Debug output.
    // * @param o string to print
    // */
    //private static void out(final String o) {
    //    System.out.println(o);
    //}
    

    /**
     * Create a minimap for this level.
     * @param fgImage foreground image used as source for the minimap
     * @param scaleX integer X scaling factor
     * @param scaleY integer Y scaling factor
     * @param tint apply a greenish color tint
     * @param drawBackground
     * @param highQuality
     * @return image with minimap
     */
    public LemmImage createMinimap(final LemmImage fgImage, final double scaleX, final double scaleY,
            final boolean highQuality, final boolean tint, final boolean drawBackground) {
        Level level = GameController.getLevel();
        LemmImage img = ToolBox.createLemmImage(fgImage.getWidth(), fgImage.getHeight());
        
        GraphicsContext gx = null;
        try {
            gx = img.createGraphicsContext();
            // clear background
            if (tint) {
                gx.setBackground(BLANK_COLOR);
            } else {
                gx.setBackground(getBgColor());
            }
            gx.clearRect(0, 0, img.getWidth(), img.getHeight());
            // draw background image
            if (drawBackground) {
                drawBackground(gx, fgImage.getWidth(), fgImage.getHeight(), 0, 0);
            }
            // draw "behind" objects
            if (level != null && level.sprObjBehind != null) {
                for (int n = level.sprObjBehind.length - 1; n >= 0; n--) {
                    SpriteObject spr = level.sprObjBehind[n];
                    LemmImage sprImg = spr.getImage();
                    gx.drawImage(sprImg, spr.getX(), spr.getY());
                }
            }
            
            gx.drawImage(fgImage, 0, 0);
            // draw "in front" objects
            if (level != null && level.sprObjFront != null) {
                for (SpriteObject spr : level.sprObjFront) {
                    LemmImage sprImg = spr.getImage();
                    gx.drawImage(sprImg, spr.getX(), spr.getY());
                }
            }
        } finally {
            if (gx != null) {
                gx.dispose();
            }
        }
        
        // scale the image if necessary
        if (scaleX != 1.0 || scaleY != 1.0) {
            int width = ToolBox.scale(fgImage.getWidth(), scaleX);
            int height = ToolBox.scale(fgImage.getHeight(), scaleY);
            Object interpolationHint = highQuality
                    ? RenderingHints.VALUE_INTERPOLATION_BILINEAR
                    : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
            img = img.getScaledInstance(width, height, interpolationHint, highQuality);
        }
        
        // now tint green
        if (tint) {
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    int c = img.getRGB(x, y);
                    c = Minimap.tintColor(c);
                    img.setRGB(x, y, c);
                }
            }
        }
        
        return img;
    }
    
    /**
     * Get level sprite object via index.
     * @param idx index
     * @return level sprite object
     */
    public SpriteObject getSprObject(final int idx) {
        if (idx >= 0 && idx < sprObjects.length) {
            return sprObjects[idx];
        } else {
            return null;
        }
    }

    /**
     * Get number of level sprite objects.
     * @return number of level sprite objects
     */
    public int getNumSprObjects() {
        if (sprObjects == null) {
            return 0;
        }
        return sprObjects.length;
    }
    
    /**
     * Get level Entrance via idx.
     * @param idx index
     * @return level Entrance
     */
    Entrance getEntrance(final int idx) {
        return entrances.get(idx);
    }
    
    /**
     * Get number of entrances for this level.
     * @return number of entrances.
     */
    public int getNumEntrances() {
        if (entrances == null) {
            return 0;
        }
        return entrances.size();
    }
    
    public int[] getEntranceOrder() {
        return entranceOrder;
    }
    
    /**
     * Get background color.
     * @return background color.
     */
    public Color getBgColor() {
        Color bgColor = BLANK_COLOR;
        if (specialStyle != null) {
            bgColor = specialStyle.getBgColor();
        }
        if (bgColor.equals(BLANK_COLOR)) {
            bgColor = mainStyle.getBgColor();
        }
        return bgColor;
    }
    
    /**
     * Get maximum safe fall distance.
     * @return maximum safe fall distance
     */
    public int getMaxFallDistance() {
        return maxFallDistance;
    }
    
    public boolean getClassicSteel() {
        return classicSteel;
    }
    
    /**
     * Get array of ARGB colors used for particle effects.
     * @return array of ARGB colors used for particle effects
     */
    public int[] getParticleCol() {
        return mainStyle.getParticleColor();
    }
    
    /**
     * Get start screen x position.
     * @return start screen x position
     */
    public int getXPosCenter() {
        return xPosCenter;
    }
    
    /**
     * Get start screen y position.
     * @return start screen y position
     */
    public int getYPosCenter() {
        return yPosCenter;
    }
    
    /**
     * Get number of climbers in this level.
     * @return number of climbers in this level
     */
    public int getNumClimbers() {
        return numClimbers;
    }
    
    /**
     * Get number of floaters in this level.
     * @return number of floaters in this level
     */
    public int getNumFloaters() {
        return numFloaters;
    }
    
    /**
     * Get number of bombers in this level.
     * @return number of bombers in this level
     */
    public int getNumBombers() {
        return numBombers;
    }
    
    /**
     * Get number of blockers in this level.
     * @return number of blockers in this level
     */
    public int getNumBlockers() {
        return numBlockers;
    }
    
    /**
     * Get number of builders in this level.
     * @return number of builders in this level
     */
    public int getNumBuilders() {
        return numBuilders;
    }
    
    /**
     * Get number of bashers in this level.
     * @return number of bashers in this level
     */
    public int getNumBashers() {
        return numBashers;
    }
    
    /**
     * Get number of miners in this level.
     * @return number of miners in this level
     */
    public int getNumMiners() {
        return numMiners;
    }
    
    /**
     * Get number of diggers in this level.
     * @return number of diggers in this level
     */
    public int getNumDiggers() {
        return numDiggers;
    }
    
    /**
     * Get time limit in seconds
     * @return time limit in seconds
     */
    public int getTimeLimitSeconds() {
        return timeLimitSeconds;
    }
    
    /**
     * Get number of Lemmings to rescue: should be less than or equal to number of Lemmings.
     * @return number of Lemmings to rescue
     */
    public int getNumToRescue() {
        return numToRescue;
    }
    
    /**
     * Get number of Lemmings in this level (maximum 0x0072 = 114 in original LVL format).
     * @return number of Lemmings in this level
     */
    public int getNumLemmings() {
        return numLemmings;
    }
    
    /**
     * Get color of debris pixels (to be replaced with level color).
     * @return color of debris pixels as ARGB
     */
    public final int getDebrisColor() {
        int debrisColor = 0;
        if (specialStyle != null) {
            debrisColor = specialStyle.getDebrisColor();
        }
        if (debrisColor == 0) {
            debrisColor = mainStyle.getDebrisColor();
        }
        return debrisColor;
    }
    
    /**
     * Get second color of debris pixels (to be replaced with level color).
     * @return second color of debris pixels as ARGB
     */
    public final int getDebrisColor2() {
        int debrisColor2 = 0;
        if (specialStyle != null) {
            debrisColor2 = specialStyle.getDebrisColor2();
        }
        if (debrisColor2 == 0) {
            debrisColor2 = mainStyle.getDebrisColor2();
        }
        return debrisColor2;
    }
    
    /**
     * Get release rate: -99 is slowest, 106 is fastest
     * @return release rate: -99 is slowest, 106 is fastest
     */
    public int getReleaseRate() {
        return releaseRate;
    }
    
    public int getMaxReleaseRate() {
        return maxReleaseRate;
    }
    
    public boolean isReleaseRateLocked() {
        return lockReleaseRate;
    }
    
    public int getWidth() {
        return levelWidth;
    }
    
    public int getHeight() {
        return levelHeight;
    }
    
    public int getTopBoundary() {
        return topBoundary;
    }
    
    public int getBottomBoundary() {
        return bottomBoundary;
    }
    
    public int getLeftBoundary() {
        return leftBoundary;
    }
    
    public int getRightBoundary() {
        return rightBoundary;
    }
    
    public String getMusic() {
        return music;
    }
    
    /**
     * Check if this is a SuperLemming level (runs faster).
     * @return true if this is a SuperLemming level, false otherwise
     */
    public boolean isSuperLemming() {
        return superlemming;
    }
    
    /**
     * Check whether the timer should run at normal speed, even if this is a SuperLemming level.
     * @return true if the timer should run at normal speed, false otherwise
     */
    public boolean getForceNormalTimerSpeed() {
        return forceNormalTimerSpeed;
    }
    
    public String getStyleName() {
        return mainStyle.getName();
    }
    
    public String getSpecialStyleName() {
        return (specialStyle != null) ? specialStyle.getName() : StringUtils.EMPTY;
    }
    
    /**
     * Get level name.
     * @return level name
     */
    public String getLevelName() {
        return lvlName;
    }
    
    /**
     * Get level author.
     * @return level author
     */
    public String getAuthor() {
        return author;
    }
    
    public int getNumHints() {
        return hints.size();
    }
    
    public String getHint(int index) {
        if (index < hints.size()) {
            return hints.get(index);
        } else {
            return null;
        }
    }
    
    /**
     * Storage class for a level object.
     * @author Volker Oth
     */
    public class LvlObject {
        
        /** paint mode: only visible on terrain pixels */
        static final int MODE_VIS_ON_TERRAIN = 8;
        /** paint mode: don't overwrite terrain pixels in the original foreground image */
        static final int MODE_NO_OVERWRITE = 4;
        /** paint mode: don't draw the object */
        static final int MODE_INVISIBLE = 2;
        
        /** flag: paint the object upside down */
        static final int FLAG_UPSIDE_DOWN = 1;
        static final int FLAG_FAKE = 2;
        static final int FLAG_UPSIDE_DOWN_MASK = 4;
        static final int FLAG_FLIP_HORIZONTALLY = 8;
        static final int FLAG_ROTATE = 16;
        
        static final int OPTION_ENTRANCE_LEFT = 1;
        
        /** identifier */
        int id;
        /** x position in pixels */
        int xPos;
        /** y position in pixels */
        int yPos;
        /** paint mode - must be one of the MODEs above */
        int paintMode;
        int flags;
        int objSpecificModifier;
        String style;
        
        /**
         * Constructor
         * @param val five or more values as array [identifier, x position, y position, paint mode, flags, object-specific modifier, style]
         */
        public LvlObject(final String[] val) {
            id = ToolBox.parseInt(val[0]);
            xPos = ToolBox.parseInt(val[1]);
            yPos = ToolBox.parseInt(val[2]);
            paintMode = ToolBox.parseInt(val[3]);
            flags = ToolBox.parseInt(val[4]);
            objSpecificModifier = (val.length >= 6) ? ToolBox.parseInt(val[5]) : 0;
            style = (val.length >= 7) ? val[6] : mainStyle.getName();
        }
    }
    
    /**
     * Storage class for a terrain/background tile.
     * @author Volker Oth
     */
    public class Terrain {
        
        static final int MODE_ROTATE = 128;
        static final int MODE_NO_ONE_WAY = 64;
        static final int MODE_FLIP_HORIZONTALLY = 32;
        static final int MODE_FAKE = 16;
        /** paint mode: don't overwrite existing terrain pixels */
        static final int MODE_NO_OVERWRITE = 8;
        /** paint mode: upside down */
        static final int MODE_UPSIDE_DOWN = 4;
        /** paint mode: remove existing terrain pixels instead of overdrawing them */
        static final int MODE_REMOVE = 2;
        /** paint mode: don't draw terrain pixels */
        static final int MODE_INVISIBLE = 1;
        
        /** identifier */
        int id;
        /** x position in pixels */
        int xPos;
        /** y position in pixels */
        int yPos;
        /** modifier - must be one of the above MODEs */
        int modifier;
        boolean specialGraphic;
        String style;
        
        /**
         * Constructor.
         * @param val four or more values as array [identifier, x position, y position, modifier, style]
         * @param special
         */
        public Terrain(final String[] val, final boolean special) {
            id = ToolBox.parseInt(val[0]);
            xPos = ToolBox.parseInt(val[1]);
            yPos = ToolBox.parseInt(val[2]);
            modifier = ToolBox.parseInt(val[3]);
            specialGraphic = special;
            style = (val.length >= 5) ? val[4] : mainStyle.getName();
        }
    }
    
    /**
     * Storage class for steel tiles.
     * @author Volker Oth
     */
    public class Steel {
        
        /** x position in pixels */
        int xPos;
        /** y position in pixels */
        int yPos;
        /** width in pixels */
        int width;
        /** height in pixels */
        int height;
        boolean negative;
        
        /**
         * Constructor.
         * @param val four or five values as array [x position, y position, width, height, flags]
         */
        public Steel(final int[] val) {
            xPos = val[0];
            yPos = val[1];
            width = val[2];
            height = val[3];
            negative = (val.length >= 5) ? BooleanUtils.toBoolean(val[4] & 0x01) : false;
        }
    }
    
    /**
     * Storage class for level Entrances.
     * @author Volker Oth
     */
    public class Entrance {
        
        /** identifier */
        int id;
        /** x position in pixels */
        int xPos;
        /** y position in pixels */
        int yPos;
        boolean leftEntrance;
        
        /**
         * Constructor.
         * @param x x position in pixels
         * @param y y position in pixels
         * @param left
         */
        Entrance(final int x, final int y, final boolean left) {
            xPos = x;
            yPos = y;
            leftEntrance = left;
        }
    }
    
    public class Background {
        
        int width;
        int height;
        List<LvlObject> objects;
        List<Terrain> terrain;
        SpriteObject[] sprObjects;
        SpriteObject[] sprObjFront;
        SpriteObject[] sprObjBehind;
        boolean tiled;
        int tint;
        int offsetX;
        int offsetY;
        double scrollSpeedX;
        double scrollSpeedY;
        double scale;
        
        Background(int width, int height, List<LvlObject> objects, List<Terrain> terrain,
                boolean tiled, int tint, int offsetX, int offsetY,
                double scrollSpeedX, double scrollSpeedY, double scale) {
            this.width = width;
            this.height = height;
            this.objects = objects;
            this.terrain = terrain;
            this.tiled = tiled;
            this.tint = tint;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.scrollSpeedX = scrollSpeedX;
            this.scrollSpeedY = scrollSpeedY;
            this.scale = scale;
        }
    }
}
