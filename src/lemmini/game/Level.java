package lemmini.game;

import java.awt.Color;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lemmini.gameutil.Sprite;
import lemmini.graphics.GraphicsContext;
import lemmini.graphics.Image;
import lemmini.tools.Props;
import lemmini.tools.ToolBox;

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
    
    /** maximum width of level */
    public static final int DEFAULT_WIDTH = 1600 * 2;
    /** maximum height of level */
    public static final int DEFAULT_HEIGHT = 160 * 2;
    /** array of default ARGB colors for particle effects */
    public static final int[] DEFAULT_PARTICLE_COLORS = {0xff00ff00, 0xff0000ff, 0xffffffff, 0xffffffff, 0xffff0000};

    /** array of default styles */
    private static final String[] STYLES = {"dirt", "fire", "marble", "pillar", "crystal",
        "brick", "rock", "snow", "bubble", "xmas"};
    /** array of default special styles */
    private static final String[] SPECIAL_STYLES = {"awesome", "menace", "beastii", "beasti",
        "covox", "prima", "apple"};
    private static final int DEFAULT_TOP_BOUNDARY = 8;
    private static final int DEFAULT_BOTTOM_BOUNDARY = 20;
    private static final int DEFAULT_LEFT_BOUNDARY = 0;
    private static final int DEFAULT_RIGHT_BOUNDARY = -16;

    /** array of normal sprite objects - no transparency, drawn behind foreground image */
    private SpriteObject[] sprObjBehind;
    /** array of special sprite objects - with transparency, drawn in front of foreground image */
    private SpriteObject[] sprObjFront;
    /** array of all sprite objects (in front and behind) */
    private SpriteObject[] sprObjects;
    /** array of level entrances */
    private Entrance[] entrances;
    /** release rate: 0 is slowest, 99 is fastest */
    private int releaseRate;
    /** number of Lemmings in this level (maximum 0x0072 in original LVL format) */
    private int numLemmings;
    /** number of Lemmings to rescue: should be less than or equal to number of Lemmings */
    private int numToRescue;
    /** time limit in seconds */
    private int timeLimitSeconds;
    /** number of climbers in this level */
    private int numClimbers;
    /** number of floaters in this level */
    private int numFloaters;
    /** number of bombers in this level */
    private int numBombers;
    /** number of blockers in this level */
    private int numBlockers;
    /** number of builders in this level */
    private int numBuilders;
    /** number of bashers in this level */
    private int numBashers;
    /** number of miners in this level */
    private int numMiners;
    /** number of diggers in this level */
    private int numDiggers;
    /** start screen x position */
    private int xPosCenter;
    /** background color */
    private Color bgColor;
    /** color used for steps and debris */
    private int debrisCol;
    private int debrisCol2;
    /** array of ARGB colors used for particle effects */
    private int[] particleCol;
    /** maximum safe fall distance */
    private int maxFallDistance;
    private boolean classicSteel;
    private AutosteelMode autosteelMode;
    /** this level is a SuperLemming level (runs faster) */
    private boolean superlemming;
    private boolean forceNormalTimerSpeed;
    /** level is completely loaded */
    private boolean ready = false;
    /** objects like doors - originally 32 objects where each consists of 8 bytes */
    private final List<LvlObject> objects;
    /** foreground tiles - every pixel in them is interpreted as brick in the stencil */
    private Image[] tiles;
    private Image[] tileMasks;
    private Image special;
    private Image specialMask;
    private final Set<Integer> steelTiles;
    /** sprite objects of all sprite objects available in this style */
    private SpriteObject[] sprObjAvailable;
    /** terrain the Lemmings walk on etc. - originally 400 tiles, 4 bytes each */
    private final List<Terrain> terrain;
    /** steel areas which are indestructible - originally 32 objects, 4 bytes each */
    private final List<Steel> steel;
    private final List<Background> backgrounds;
    /** level name - originally 32 bytes ASCII filled with whitespace */
    private String lvlName;
    /** used to read in the configuration file */
    private Props props;
    private Props props2;
    private int levelWidth;
    private int topBoundary;
    private int bottomBoundary;
    private int leftBoundary;
    private int rightBoundary;

    public Level() {
        objects = new ArrayList<>(64);
        terrain = new ArrayList<>(512);
        steel = new ArrayList<>(64);
        backgrounds = new ArrayList<>(4);
        steelTiles = new HashSet<>(16);
    }

    /**
     * Load a level and all level resources.
     * @param fname file name
     * @throws ResourceException
     * @throws LemmException
     */
    void loadLevel(final Path fname) throws ResourceException, LemmException {
        ready = false;
        special = null;
        specialMask = null;
        // read level properties from file
        Props p = new Props();
        if (!p.load(fname)) {
            throw new ResourceException(fname.toString());
        }
        String mainLevel = p.get("mainLevel", "");
        Props p2;
        if (!mainLevel.isEmpty()) {
            Path fname2 = fname.resolveSibling(mainLevel);
            p2 = new Props();
            if (!p2.load(fname2)) {
                throw new ResourceException(fname2.toString());
            }
        } else {
            p2 = p;
        }

        // read name
        lvlName = p.get("name", p2.get("name", ""));
        //out(fname + " - " + lvlName);
        maxFallDistance = p.getInt("maxFallDistance",
                p2.getInt("maxFallDistance", GameController.getCurLevelPack().getMaxFallDistance()));
        classicSteel = p.getBoolean("classicSteel", p2.getBoolean("classicSteel", false));
        switch (p2.getInt("autosteelMode", 0)) {
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
        levelWidth = p2.getInt("width", DEFAULT_WIDTH);
        topBoundary = p2.getInt("topBoundary", DEFAULT_TOP_BOUNDARY);
        bottomBoundary = p2.getInt("bottomBoundary", DEFAULT_BOTTOM_BOUNDARY);
        leftBoundary = p2.getInt("leftBoundary", DEFAULT_LEFT_BOUNDARY);
        rightBoundary = p2.getInt("rightBoundary", DEFAULT_RIGHT_BOUNDARY);
        releaseRate = p.getInt("releaseRate", p2.getInt("releaseRate", 0));
        //out("releaseRate = " + releaseRate);
        numLemmings = p.getInt("numLemmings", p2.getInt("numLemmings", 1));
        //out("numLemmings = " + numLemmings);
        numToRescue = p.getInt("numToRescue", p2.getInt("numToRescue", 0));
        //out("numToRescue = " + numToRescue);
        timeLimitSeconds = p.getInt("timeLimitSeconds", Integer.MIN_VALUE);
        if (timeLimitSeconds == Integer.MIN_VALUE) {
            int timeLimit = p.getInt("timeLimit", Integer.MIN_VALUE);
            if (p2 != p && timeLimit == Integer.MIN_VALUE) {
                timeLimitSeconds = p2.getInt("timeLimitSeconds", Integer.MIN_VALUE);
                if (timeLimitSeconds == Integer.MIN_VALUE) {
                    timeLimit = p2.getInt("timeLimit", Integer.MIN_VALUE);
                    // prevent integer overflow upon conversion to seconds
                    if (timeLimit >= Integer.MAX_VALUE / 60 || timeLimit <= Integer.MIN_VALUE / 60) {
                        timeLimit = 0;
                    }
                    timeLimitSeconds = timeLimit * 60;
                }
            } else {
                // prevent integer overflow upon conversion to seconds
                if (timeLimit >= Integer.MAX_VALUE / 60 || timeLimit <= Integer.MIN_VALUE / 60) {
                    timeLimit = 0;
                }
                timeLimitSeconds = timeLimit * 60;
            }
        }
        if (timeLimitSeconds == Integer.MAX_VALUE || timeLimitSeconds < 0) {
            timeLimitSeconds = 0;
        }

        //out("timeLimit = " + timeLimit);
        numClimbers = p.getInt("numClimbers", p2.getInt("numClimbers", 0));
        //out("numClimbers = " + numClimbers);
        numFloaters = p.getInt("numFloaters", p2.getInt("numFloaters", 0));
        //out("numFloaters = " + numFloaters);
        numBombers = p.getInt("numBombers", p2.getInt("numBombers", 0));
        //out("numBombers = " + numBombers);
        numBlockers = p.getInt("numBlockers", p2.getInt("numBlockers", 0));
        //out("numBlockers = " + numBlockers);
        numBuilders = p.getInt("numBuilders", p2.getInt("numBuilders", 0));
        //out("numBuilders = " + numBuilders);
        numBashers = p.getInt("numBashers", p2.getInt("numBashers", 0));
        //out("numBashers = " + numBashers);
        numMiners = p.getInt("numMiners", p2.getInt("numMiners", 0));
        //out("numMiners = " + numMiners);
        numDiggers = p.getInt("numDiggers", p2.getInt("numDiggers", 0));
        //out("numDiggers = " + numDiggers);
        xPosCenter = p.getInt("xPosCenter", Integer.MIN_VALUE);
        if (xPosCenter == Integer.MIN_VALUE) {
            int xPos = p.getInt("xPos", Integer.MIN_VALUE);
            if (p2 != p && xPos == Integer.MIN_VALUE) {
                xPosCenter = p2.getInt("xPosCenter", Integer.MIN_VALUE);
                if (xPosCenter == Integer.MIN_VALUE) {
                    xPos = p2.getInt("xPos", Integer.MIN_VALUE);
                    xPosCenter = xPos + 400;
                }
            } else {
                xPosCenter = xPos + 400;
            }
        }
        //out("xPosCenter = " + xPosCenter);
        String strStyle = p2.get("style", "");
        int style = -1;
        for (int i = 0; i < STYLES.length; i++) {
            if (strStyle.equalsIgnoreCase(STYLES[i])) {
                style = i;
                break;
            }
        }
        //out("style = " + STYLES[style]);
        String strSpecialStyle = p2.get("specialStyle", "");
        int specialStyle = -1;
        for (int i = 0; i < SPECIAL_STYLES.length; i++) {
            if (strSpecialStyle.equalsIgnoreCase(SPECIAL_STYLES[i])) {
                specialStyle = i;
                break;
            }
        }
        //out("specialStyle = " + strSpecialStyle);
        superlemming = p.getBoolean("superlemming", p2.getBoolean("superlemming", false));
        forceNormalTimerSpeed = p.getBoolean("forceNormalTimerSpeed", p2.getBoolean("forceNormalTimerSpeed", false));
        
        // load objects
        sprObjAvailable = null;
        // first load the data from object descriptor file xxx.ini
        props = new Props();
        try {
            Path fnames = Paths.get("styles", strStyle, strStyle + ".ini");
            Path fnames2 = Core.findResource(fnames);
            if (!props.load(fnames2)) {
                if (style != -1) {
                    throw new ResourceException(fnames.toString());
                } else {
                    throw new LemmException("Style \"" + strStyle + "\" does not exist.");
                }
            }
        } catch (ResourceException ex) {
            if (style != -1) {
                throw ex;
            } else {
                throw new LemmException("Style \"" + strStyle + "\" does not exist.");
            }
        }
        if (!strSpecialStyle.isEmpty()) {
            try {
                Path fnames = Paths.get("styles/special", strSpecialStyle, strSpecialStyle + ".ini");
                Path fnames2 = Core.findResource(fnames);
                props2 = new Props();
                if (fnames2 == null || !props2.load(fnames2)) {
                    if (specialStyle != -1) {
                        throw new ResourceException(fnames.toString());
                    } else {
                        throw new LemmException("Special style \"" + strSpecialStyle + "\" does not exist.");
                    }
                }
            } catch (ResourceException ex) {
                if (style != -1) {
                    throw ex;
                } else {
                    throw new LemmException("Special style \"" + strSpecialStyle + "\" does not exist.");
                }
            }
        } else {
            props2 = props;
        }
        // load blockset
        if (!strSpecialStyle.isEmpty()) {
            special = loadSpecialSet(strSpecialStyle);
            specialMask = loadSpecialMaskSet(strSpecialStyle);
        }
        tiles = loadTileSet(strStyle);
        tileMasks = loadTileMaskSet(strStyle);
        steelTiles.clear();
        int[] steelTilesArray = props.getIntArray("steelTiles", null);
        if (steelTilesArray != null) {
            for (int steelTile : steelTilesArray) {
                steelTiles.add(steelTile);
            }
        }
        
        sprObjAvailable = loadObjects(strStyle);
        
        // read objects
        //out("\n[Objects]");
        objects.clear();
        for (int i = 0; true; i++) {
            int[] val = p2.getIntArray("object_" + i, null);
            if (val != null && val.length >= 5) {
                LvlObject obj = new LvlObject(val);
                objects.add(obj);
                //out("" + obj.id + ", " + obj.xPos + ", " + obj.yPos + ", "+ obj.paintMode + ", " + obj.upsideDown);
            } else {
                break;
            }
        }
        // read terrain
        //out("\n[Terrain]");
        terrain.clear();
        if (!strSpecialStyle.isEmpty()) {
            int positionX;
            int positionY;
            positionX = props2.getInt("positionX", 0);
            positionY = props2.getInt("positionY", 0);
            Terrain ter = new Terrain(new int[]{0, positionX, positionY, 0}, true);
            terrain.add(ter);
        }
        for (int i = 0; true; i++) {
            int[] val = p2.getIntArray("terrain_" + i, null);
            if (val != null && val.length >= 4) {
                Terrain ter = new Terrain(val, false);
                terrain.add(ter);
                //out("" + ter.id + ", " + ter.xPos + ", " + ter.yPos + ", " + ter.modifier);
            } else {
                break;
            }
        }
        // read steel blocks
        //out("\n[Steel]");
        steel.clear();
        for (int i = 0; true; i++) {
            int[] val = p2.getIntArray("steel_" + i, null);
            if (val != null && val.length >= 4) {
                Steel stl = new Steel(val);
                steel.add(stl);
                //out("" + stl.xPos + ", " + stl.yPos + ", " + stl.width + ", " + stl.height);
            } else {
                break;
            }
        }
        // read background
        backgrounds.clear();
        int numBackgrounds = p2.getInt("numBackgrounds", 0);
        for (int i = 0; i < numBackgrounds; i++) {
            List<LvlObject> bgObjects = new ArrayList<>(16);
            List<Terrain> bgTerrain = new ArrayList<>(256);
            boolean bgTiled = p2.getBoolean("bg_" + i + "_tiled", false);
            int bgWidth = p2.getInt("bg_" + i + "_width", 0);
            int bgHeight = p2.getInt("bg_" + i + "_height", 0);
            int bgOffsetX = p2.getInt("bg_" + i + "_offsetX", 0);
            int bgOffsetY = p2.getInt("bg_" + i + "_offsetY", 0);
            double bgScrollSpeedX = p2.getDouble("bg_" + i + "_scrollSpeedX", 0.0);
            for (int j = 0; true; j++) {
                int[] val = p2.getIntArray("bg_" + i + "_object_" + j, null);
                if (val != null && val.length >= 5) {
                    LvlObject obj = new LvlObject(val);
                    bgObjects.add(obj);
                } else {
                    break;
                }
            }
            for (int j = 0; true; j++) {
                int[] val = p2.getIntArray("bg_" + i + "_terrain_" + j, null);
                if (val != null && val.length >= 4) {
                    Terrain ter = new Terrain(val, false);
                    bgTerrain.add(ter);
                } else {
                    break;
                }
            }
            backgrounds.add(new Background(bgObjects, bgTerrain, bgTiled, bgWidth, bgHeight, bgOffsetX, bgOffsetY, bgScrollSpeedX));
        }
        ready = true;
    }

    /**
     * Paint a level.
     * @param fgImage foreground image to draw into
     * @param s stencil to reuse
     * @return stencil of this level
     */
    Stencil paintLevel(final Image fgImage, final List<Image> bgImages, final Stencil s) {
        // flush all resources
        sprObjFront = null;
        sprObjBehind = null;
        sprObjects = null;
        entrances = null;
        System.gc();
        // the screenBuffer should be big enough to hold the level
        // returns stencil buffer;
        int fgWidth = fgImage.getWidth();
        int fgHeight = fgImage.getHeight();
        // try to reuse old stencil
        Stencil stencil;
        if (s != null && s.getWidth() == fgWidth && s.getHeight() == fgHeight) {
            s.clear();
            stencil = s;
        } else {
            stencil = new Stencil(fgWidth, fgHeight);
        }
        // paint terrain
        for (Terrain t : terrain) {
            if (t.id < 0) {
                continue;
            }
            Image i;
            Image mask;
            if (t.specialGraphic) {
                i = special;
                mask = specialMask;
            } else {
                i = tiles[t.id];
                mask = tileMasks[t.id];
            }
            int width = i.getWidth();
            int height = i.getHeight();
            int maskWidth = mask.getWidth();
            int maskHeight = mask.getHeight();
            boolean isSteel = autosteelMode != AutosteelMode.NONE
                    && !t.specialGraphic && steelTiles.contains(t.id);
            
            int[] source = new int[width * height];
            int[] sourceMask = new int[maskWidth * maskHeight];
            GraphicsContext graphicsContext = i.createGraphicsContext();
            GraphicsContext graphicsContextMask = mask.createGraphicsContext();
            graphicsContext.grabPixels(i, 0, 0, width, height, source, 0, width);
            graphicsContextMask.grabPixels(mask, 0, 0, width, height, sourceMask, 0, width);
            graphicsContext.dispose();
            graphicsContextMask.dispose();
            int tx = t.xPos;
            int ty = t.yPos;
            boolean horizontallyFlipped = (t.modifier & Terrain.MODE_HORIZONTALLY_FLIPPED) != 0;
            boolean fake = (t.modifier & Terrain.MODE_FAKE) != 0;
            boolean upsideDown = (t.modifier & Terrain.MODE_UPSIDE_DOWN) != 0;
            boolean noOverwrite = (t.modifier & Terrain.MODE_NO_OVERWRITE) != 0;
            boolean remove = !noOverwrite && (t.modifier & Terrain.MODE_REMOVE) != 0;
            boolean invisible = (t.modifier & Terrain.MODE_INVISIBLE) != 0;
            for (int y = 0; y < height; y++) {
                if (y + ty < 0 || y + ty >= fgHeight) {
                    continue;
                }
                int yLine;
                if (upsideDown) {
                    yLine = (height - y - 1) * width;
                } else {
                    yLine = y * width;
                }
                for (int x = 0; x < width; x++) {
                    if (x + tx < 0 || x + tx >= fgWidth) {
                        continue;
                    }
                    int xSrc;
                    if (horizontallyFlipped) {
                        xSrc = width - x - 1;
                    } else {
                        xSrc = x;
                    }
                    int col = source[yLine + xSrc];
                    int maskCol = sourceMask[yLine + xSrc];
                    // ignore transparent pixels
                    if (!invisible && (col & 0xff000000) != 0) {
                        //col = (col & 0xffffff) | 0x80000000;
                        if (noOverwrite) {
                            fgImage.addRGBBehind(x + tx, y + ty, col);
                        } else if (remove) {
                            fgImage.removeAlpha(x + tx, y + ty, (col >>> 24) & 0xff);
                        } else {
                            fgImage.addRGB(x + tx, y + ty, col);
                        }
                    }
                    if (!fake && (maskCol & 0xff000000) != 0) {
                        int newMask;
                        if (remove) {
                            newMask = Stencil.MSK_EMPTY;
                        } else if (noOverwrite) {
                            newMask = stencil.getMask(x + tx, y + ty) | Stencil.MSK_BRICK;
                            switch (autosteelMode) {
                                case NONE:
                                default:
                                    break;
                                case SIMPLE:
                                    if (isSteel) {
                                        newMask |= Stencil.MSK_STEEL;
                                    }
                                    break;
                                case ADVANCED:
                                    if (isSteel && (stencil.getMask(x + tx, y + ty) & Stencil.MSK_BRICK) == 0) {
                                        newMask |= Stencil.MSK_STEEL;
                                    }
                                    break;
                            }
                        } else {
                            newMask = stencil.getMask(x + tx, y + ty) | Stencil.MSK_BRICK;
                            switch (autosteelMode) {
                                case NONE:
                                default:
                                    break;
                                case SIMPLE:
                                    if (isSteel) {
                                        newMask |= Stencil.MSK_STEEL;
                                    }
                                    break;
                                case ADVANCED:
                                    if (isSteel) {
                                        newMask |= Stencil.MSK_STEEL;
                                    } else {
                                        newMask &= ~Stencil.MSK_STEEL;
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
        for (Steel stl : steel) {
            int sx = stl.xPos;
            int sy = stl.yPos;
            for (int y = 0; y < stl.height; y++) {
                if (y + sy < 0 || y + sy >= fgHeight) {
                    continue;
                }
                for (int x = 0; x < stl.width; x++) {
                    if ((!classicSteel && (stencil.getMask(x + sx, y + sy) & Stencil.MSK_BRICK) == 0)
                            || x + sx < 0 || x + sx >= fgWidth) {
                        continue;
                    }
                    if (stl.negative) {
                        stencil.andMask(x + sx, y + sy, ~Stencil.MSK_STEEL);
                    } else {
                        stencil.orMask(x + sx, y + sy, Stencil.MSK_STEEL);
                    }
                }
            }
        }
        
        // now for the animated objects
        List<SpriteObject> oCombined = new ArrayList<>(64);
        List<SpriteObject> oBehind = new ArrayList<>(64);
        List<SpriteObject> oFront = new ArrayList<>(64);
        List<Entrance> entrance = new ArrayList<>(8);
        for (int n = 0; n < objects.size() && n >= 0; n++) {
            LvlObject o = objects.get(n);
            if (o.id < 0) {
                oCombined.add(null);
                continue;
            }
            SpriteObject spr = new SpriteObject(sprObjAvailable[o.id]);
            spr.setX(o.xPos);
            spr.setY(o.yPos);
            // flags
            boolean upsideDown = (o.flags & LvlObject.FLAG_UPSIDE_DOWN) != 0;
            boolean fake = (o.flags & LvlObject.FLAG_FAKE) != 0;
            boolean upsideDownMask = (o.flags & LvlObject.FLAG_UPSIDE_DOWN_MASK) != 0;
            boolean horizontallyFlipped = (o.flags & LvlObject.FLAG_HORIZONTALLY_FLIPPED) != 0;
            // check for entrances
            if (spr.getType() == SpriteObject.Type.ENTRANCE && !fake) {
                Entrance e = new Entrance(o.xPos + spr.getWidth() / 2 + spr.getMaskOffsetX(),
                        o.yPos + spr.getMaskOffsetY(), (o.objSpecificModifier & LvlObject.OPTION_ENTRANCE_LEFT) != 0);
                e.id = oCombined.size();
                entrance.add(e);
            }
            // animated
            boolean invisible = (o.paintMode & LvlObject.MODE_INVISIBLE) != 0;
            boolean drawOnVis = !invisible && (o.paintMode & LvlObject.MODE_VIS_ON_TERRAIN) != 0;
            boolean noOverwrite = !drawOnVis && (o.paintMode & LvlObject.MODE_NO_OVERWRITE) != 0;
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
                if (y + spr.getY() < 0 || y + spr.getY() >= fgHeight) {
                    continue;
                }
                for (int x = 0; x < spr.getWidth(); x++) {
                    if (x + spr.getX() < 0 || x + spr.getX() >= fgWidth) {
                        continue;
                    }
                    stencil.addID(spr.getX() + x, spr.getY() + y, n);
                }
            }
            
            // draw stencil
            if (!fake) {
                for (int y = spr.getMaskOffsetY(); y < spr.getMaskHeight() + spr.getMaskOffsetY(); y++) {
                    if (y + spr.getY() < 0 || y + spr.getY() >= fgHeight) {
                        continue;
                    }
                    int yDest;
                    if (upsideDownMask) {
                        yDest = spr.getHeight() - y - 1;
                        if (spr.getType().isTriggeredByFoot()) {
                            yDest += Lemming.HEIGHT;
                        }
                    } else {
                        yDest = y;
                    }
                    for (int x = spr.getMaskOffsetX(); x < spr.getMaskWidth() + spr.getMaskOffsetX(); x++) {
                        int xDest;
                        if (horizontallyFlipped) {
                            xDest = spr.getWidth() - x - 1;
                        } else {
                            xDest = x;
                        }
                        if ((!classicSteel
                                && (!spr.getType().isTriggeredByFoot())
                                && (stencil.getMask(xDest + spr.getX(), yDest + spr.getY()) & Stencil.MSK_BRICK) == 0)
                                || xDest + spr.getX() < 0 || xDest + spr.getX() >= fgWidth) {
                            continue;
                        }
                        // manage collision mask
                        // now read stencil
                        if ((spr.getMask(x, y) & 0xff000000) != 0) { // not transparent
                            stencil.andMask(spr.getX() + xDest, yDest + spr.getY(), Stencil.MSK_BRICK);
                            stencil.orMask(spr.getX() + xDest, yDest + spr.getY(), spr.getMaskType());
                            stencil.setMaskObjectID(spr.getX() + xDest, yDest + spr.getY(), n);
                        }
                    }
                }
            }
            // remove invisible pixels from all object frames that are "in front"
            if (!invisible) {
                // get flipped or normal version
                spr.flipSprite(horizontallyFlipped, upsideDown);
                for (int y = 0; y < spr.getHeight(); y++) {
                    for (int x = 0; x < spr.getWidth(); x++) {
                        boolean paint = true;
                        if (x + spr.getX() < 0 || x + spr.getX() >= fgWidth
                                || y + spr.getY() < 0 || y + spr.getY() >= fgHeight) {
                            paint = false;
                        } else if (inFront) {
                            // now read terrain image
                            int terrainVal = fgImage.getRGB(x + spr.getX(), y + spr.getY());
                            paint = drawFull || ((terrainVal & 0xff000000) >>> 24) >= 0x80 && drawOnVis;
                        }
                        if (!paint) {
                            spr.setPixelVisibility(x, y, false); // set transparent
                        }
                    }
                }
            }
        }
        
        // paint background
        if (bgImages != null) {
            int numBackgrounds = Math.min(backgrounds.size(), bgImages.size());
            List<SpriteObject> bgOCombined = new ArrayList<>(32);
            List<SpriteObject> bgOFront = new ArrayList<>(32);
            List<SpriteObject> bgOBehind = new ArrayList<>(32);
            for (int m = 0; m < numBackgrounds; m++) {
                Background bg = backgrounds.get(m);
                Image targetBg = bgImages.get(m);
                bgOCombined.clear();
                bgOBehind.clear();
                bgOFront.clear();
                
                for (Terrain t : bg.terrain) {
                    if (t.id < 0 || (t.modifier & Terrain.MODE_INVISIBLE) != 0) {
                        continue;
                    }
                    Image i;
                    i = tiles[t.id];
                    int width = i.getWidth();
                    int height = i.getHeight();
                    
                    int[] source = new int[width * height];
                    GraphicsContext graphicsContext = i.createGraphicsContext();
                    graphicsContext.grabPixels(i, 0, 0, width, height, source, 0, width);
                    graphicsContext.dispose();
                    int tx = t.xPos;
                    int ty = t.yPos;
                    boolean horizontallyFlipped = (t.modifier & Terrain.MODE_HORIZONTALLY_FLIPPED) != 0;
                    boolean upsideDown = (t.modifier & Terrain.MODE_UPSIDE_DOWN) != 0;
                    boolean overwrite = (t.modifier & Terrain.MODE_NO_OVERWRITE) == 0;
                    boolean remove = (t.modifier & Terrain.MODE_REMOVE) != 0;
                    for (int y = 0; y < height; y++) {
                        if (y + ty < 0 || y + ty >= fgHeight) {
                            continue;
                        }
                        int yLine;
                        if (upsideDown) {
                            yLine = (height - y - 1) * width;
                        } else {
                            yLine = y * width;
                        }
                        for (int x = 0; x < width; x++) {
                            if (x + tx < 0 || x + tx >= fgWidth) {
                                continue;
                            }
                            int xSrc;
                            if (horizontallyFlipped) {
                                xSrc = width - x - 1;
                            } else {
                                xSrc = x;
                            }
                            int col = source[yLine + xSrc];
                            // ignore transparent pixels
                            if ((col & 0xff000000) != 0) {
                                if (!overwrite) {
                                    targetBg.addRGBBehind(x + tx, y + ty, col);
                                } else if (remove) {
                                    targetBg.removeAlpha(x + tx, y + ty, (col >>> 24) & 0xff);
                                } else {
                                    targetBg.addRGB(x + tx, y + ty, col);
                                }
                            }
                        }
                    }
                }
                
                for (LvlObject o : bg.objects) {
                    if (o.id < 0) {
                        continue;
                    }
                    SpriteObject spr = new SpriteObject(sprObjAvailable[o.id]);
                    spr.setX(o.xPos);
                    spr.setY(o.yPos);
                    // flags
                    boolean upsideDown = (o.flags & LvlObject.FLAG_UPSIDE_DOWN) != 0;
                    boolean horizontallyFlipped = (o.flags & LvlObject.FLAG_HORIZONTALLY_FLIPPED) != 0;
                    // animated
                    boolean invisible = (o.paintMode & LvlObject.MODE_INVISIBLE) != 0;
                    boolean drawOnVis = !invisible && (o.paintMode & LvlObject.MODE_VIS_ON_TERRAIN) != 0;
                    boolean noOverwrite = !drawOnVis && (o.paintMode & LvlObject.MODE_NO_OVERWRITE) != 0;
                    boolean inFront = !invisible && !noOverwrite;
                    boolean drawFull = inFront && !drawOnVis;

                    spr.setVisOnTerrain(drawOnVis);

                    if (inFront) {
                        bgOFront.add(spr);
                    } else {
                        bgOBehind.add(spr);
                    }
                    bgOCombined.add(spr);

                    // remove invisible pixels from all object frames that are "in front"
                    if (!invisible) {
                        // get flipped or normal version
                        spr.flipSprite(horizontallyFlipped, upsideDown);
                        for (int y = 0; y < spr.getHeight(); y++) {
                            for (int x = 0; x < spr.getWidth(); x++) {
                                boolean paint = true;
                                if (x + spr.getX() < 0 || x + spr.getX() >= targetBg.getWidth()
                                        || y + spr.getY() < 0 || y + spr.getY() >= targetBg.getHeight()) {
                                    paint = false;
                                } else if (inFront) {
                                    // now read terrain image
                                    int terrainVal = targetBg.getRGB(x + spr.getX(), y + spr.getY());
                                    paint = drawFull || ((terrainVal & 0xff000000) >>> 24) >= 0x80 && drawOnVis;
                                }
                                if (!paint) {
                                    spr.setPixelVisibility(x, y, false); // set transparent
                                }
                            }
                        }
                    }
                }
                
                bg.sprObjects = new SpriteObject[bgOCombined.size()];
                bg.sprObjects = bgOCombined.toArray(bg.sprObjects);
                bg.sprObjFront = new SpriteObject[bgOFront.size()];
                bg.sprObjFront = bgOFront.toArray(bg.sprObjFront);
                bg.sprObjBehind = new SpriteObject[bgOBehind.size()];
                bg.sprObjBehind = bgOBehind.toArray(bg.sprObjBehind);
            }
        }
        
        entrances = new Entrance[entrance.size()];
        entrances = entrance.toArray(entrances);
        
        sprObjects = new SpriteObject[oCombined.size()];
        sprObjects = oCombined.toArray(sprObjects);
        sprObjFront = new SpriteObject[oFront.size()];
        sprObjFront = oFront.toArray(sprObjFront);
        sprObjBehind = new SpriteObject[oBehind.size()];
        sprObjBehind = oBehind.toArray(sprObjBehind);
        System.gc();
        
        return stencil;
    }
    
    /**
     * Draw opaque objects behind foreground image.
     * @param g graphics object to draw on
     * @param width width of screen
     * @param xOfs level offset position
     */
    public void drawBehindObjects(final GraphicsContext g, final int width, final int xOfs) {
        // draw "behind" objects
        if (sprObjBehind != null) {
            for (int n = sprObjBehind.length - 1; n >= 0; n--) {
                SpriteObject spr = sprObjBehind[n];
                Image img = spr.getImage();
                if (spr.getX() + spr.getWidth() > xOfs && spr.getX() < xOfs + width) {
                    g.drawImage(img, spr.getX() - xOfs, spr.getY());
                }
            }
        }
    }

    /**
     * Draw transparent objects in front of foreground image.
     * @param g graphics object to draw on
     * @param width width of screen
     * @param xOfs level offset position
     */
    public void drawInFrontObjects(final GraphicsContext g, final int width, final int xOfs) {
        // draw "in front" objects
        if (sprObjFront != null) {
            for (SpriteObject spr : sprObjFront) {
                Image img = spr.getImage();
                if (spr.getX() + spr.getWidth() > xOfs && spr.getX() < xOfs + width) {
                    g.drawImage(img, spr.getX() - xOfs, spr.getY());
                }
            }
        }
    }
    
    public void drawBackground(final GraphicsContext g, final int width,
            final int xOfs, final int scaleX, final int scaleY) {
        List<Image> bgImages = GameController.getBgImages();
        if (bgImages == null || scaleX == 0 || scaleY == 0) {
            return;
        }
        
        int numBackgrounds = Math.min(bgImages.size(), backgrounds.size());
        
        for (int i = numBackgrounds - 1; i >= 0; i--) {
            Image bgImage = bgImages.get(i);
            Background bg = backgrounds.get(i);
            
            int bgImageWidth = bgImage.getWidth();
            int bgImageHeight = bgImage.getHeight();
            
            int xOfsNew = (int) (-xOfs * bg.scrollSpeedX) + bg.offsetX;
            int yOfsNew = bg.offsetY;
            if (bg.tiled) {
                xOfsNew %= bgImageWidth;
                xOfsNew -= (xOfsNew > 0) ? bgImageWidth : 0;
                yOfsNew %= bgImageHeight;
                yOfsNew -= (yOfsNew > 0) ? bgImageHeight : 0;
            }
            
            for (int y = yOfsNew; y < DEFAULT_HEIGHT; y += bgImageHeight) {
                for (int x = xOfsNew; x < width; x += bgImageWidth) {
                    // draw "behind" objects
                    if (bg.sprObjBehind != null) {
                        for (int n = bg.sprObjBehind.length - 1; n >= 0; n--) {
                            SpriteObject spr = bg.sprObjBehind[n];
                            Image img = spr.getImage();
                            g.drawImage(img, (x + spr.getX()) / scaleX, (y + spr.getY()) / scaleY,
                                    (x + spr.getX() + img.getWidth()) / scaleX,
                                    (y + spr.getY() + img.getHeight()) / scaleY,
                                    0, 0, img.getWidth(), img.getHeight());
                        }
                    }
                    
                    g.drawImage(bgImage, x / scaleX, y / scaleY,
                            (x + bgImageWidth) / scaleX,
                            (y + bgImageHeight) / scaleY,
                            0, 0, bgImageWidth, bgImageHeight);
                    
                    // draw "in front" objects
                    if (bg.sprObjFront != null) {
                        for (SpriteObject spr : bg.sprObjFront) {
                            Image img = spr.getImage();
                            g.drawImage(img, (x + spr.getX()) / scaleX, (y + spr.getY()) / scaleY,
                                    (x + spr.getX() + img.getWidth()) / scaleX,
                                    (y + spr.getY() + img.getHeight()) / scaleY,
                                    0, 0, img.getWidth(), img.getHeight());
                        }
                    }
                    
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
     * Load tile set from a styles folder.
     * @param set name of the style
     * @return array of images where each image contains one tile
     * @throws ResourceException
     */
    private Image[] loadTileSet(final String set) throws ResourceException {
        List<Image> images = new ArrayList<>(64);
        int tiles = props.getInt("tiles", 0);
        for (int n = 0; n < tiles; n++) {
            Path fName = Core.findResource(
                    Paths.get("styles", set, set + "_" + n + ".png"),
                    Core.IMAGE_EXTENSIONS);
            images.add(Core.loadTranslucentImage(fName));
        }
        Image[] ret = new Image[images.size()];
        ret = images.toArray(ret);
        return ret;
    }
    
    /**
     * Load tile set masks from a styles folder.
     * @param set name of the style
     * @return array of images where each image contains one tile mask
     * @throws ResourceException
     */
    private Image[] loadTileMaskSet(final String set) throws ResourceException {
        List<Image> images = new ArrayList<>(64);
        int tileMasks = props.getInt("tiles", 0);
        for (int n = 0; n < tileMasks; n++) {
            Path fName;
            try {
                fName = Core.findResource(
                        Paths.get("styles", set, set + "m_" + n + ".png"),
                        Core.IMAGE_EXTENSIONS);
            } catch (ResourceException ex) {
                fName = Core.findResource(
                        Paths.get("styles", set, set + "_" + n + ".png"),
                        Core.IMAGE_EXTENSIONS);
            }
            images.add(Core.loadBitmaskImage(fName));
        }
        Image[] ret = new Image[images.size()];
        ret = images.toArray(ret);
        return ret;
    }
    
    /**
     * Load a special graphic from the styles/special folder.
     * @param set name of the style
     * @return array of images where each image contains one tile
     * @throws ResourceException
     */
    private Image loadSpecialSet(final String specialSet) throws ResourceException {
        Path fName = Core.findResource(
                Paths.get("styles/special", specialSet, specialSet + ".png"),
                Core.IMAGE_EXTENSIONS);
        return Core.loadTranslucentImage(fName);
    }
    
    /**
     * Load a special graphic mask from the styles/special folder.
     * @param set name of the style
     * @return array of images where each image contains one tile
     * @throws ResourceException
     */
    private Image loadSpecialMaskSet(final String specialSet) throws ResourceException {
        Path fName;
        try {
            fName = Core.findResource(
                    Paths.get("styles/special", specialSet, specialSet + "m.png"),
                    Core.IMAGE_EXTENSIONS);
        } catch (ResourceException ex) {
            fName = Core.findResource(
                    Paths.get("styles/special", specialSet, specialSet + ".png"),
                    Core.IMAGE_EXTENSIONS);
        }
        return Core.loadBitmaskImage(fName);
    }


    /**
     * Load level sprite objects.
     * @param set name of the style
     * @return array of images where each image contains one tile
     * @throws ResourceException
     */
    private SpriteObject[] loadObjects(final String set) throws ResourceException {
        // first some global settings
        int bgCol = props2.getInt("bgColor", props.getInt("bgColor", 0x000000)) | 0xff000000;
        bgColor = new Color(bgCol);
        debrisCol = props2.getInt("debrisColor", props.getInt("debrisColor", 0xffffff)) | 0xff000000;
        debrisCol2 = props2.getInt("debrisColor2", props.getInt("debrisColor2", debrisCol)) | 0xff000000;
        Lemming.patchColors(debrisCol, debrisCol2);
        particleCol = props2.getIntArray("particleColor", props.getIntArray("particleColor", DEFAULT_PARTICLE_COLORS));
        for (int i = 0; i < particleCol.length; i++) {
            particleCol[i] |= 0xff000000;
        }
        // go through all the entrances
        List<SpriteObject> sprites = new ArrayList<>(64);
        int idx;
        for (idx = 0; true; idx++) {
            // get number of animations
            String sIdx = Integer.toString(idx);
            int frames = props.getInt("frames_" + sIdx, -1);
            if (frames < 0) {
                break;
            }
            // load screen buffer
            Path fName = Core.findResource(
                    Paths.get("styles", set, set + "o_" + idx + ".png"),
                    Core.IMAGE_EXTENSIONS);
            Image img = Core.loadTranslucentImage(fName);
            // load sprite
            int anim = props.getInt("anim_" + sIdx, -1);
            if (anim < 0) {
                break;
            }
            SpriteObject sprite = new SpriteObject(img, frames);
            // get object type
            int type = props.getInt("type_" + sIdx, -1);
            if (type < 0) {
                break;
            }
            sprite.setType(SpriteObject.getType(type));
            switch (sprite.getType()) {
                case EXIT:
                case TURN_LEFT:
                case TURN_RIGHT:
                case NO_BASH_LEFT:
                case NO_BASH_RIGHT:
                case TRAP_DIE:
                case TRAP_REPLACE:
                case TRAP_DROWN:
                case STEEL:
                    // load mask
                    fName = Core.findResource(
                            Paths.get("styles", set, set + "om_" + idx + ".png"),
                            Core.IMAGE_EXTENSIONS);
                    int maskOffsetX = props.getInt("maskOffsetX_" + sIdx, 0);
                    int maskOffsetY = props.getInt("maskOffsetY_" + sIdx, 0);
                    img = Core.loadBitmaskImage(fName);
                    sprite.setMask(img, maskOffsetX, maskOffsetY);
                    break;
                case ENTRANCE:
                    maskOffsetX = props.getInt("maskOffsetX_" + sIdx, 0);
                    maskOffsetY = props.getInt("maskOffsetY_" + sIdx, 0);
                    sprite.setMask(null, maskOffsetX, maskOffsetY);
                    break;
                default:
                    break;
            }
            // get animation mode
            switch (anim) {
                case 0: // don't animate
                    sprite.setAnimMode(Sprite.Animation.NONE);
                    break;
                case 1: // loop mode
                    sprite.setAnimMode(Sprite.Animation.LOOP);
                    break;
                case 2: // triggered animation - for the moment handle like loop
                    sprite.setAnimMode(Sprite.Animation.TRIGGERED);
                    break;
                case 3: // entrance animation
                    if (sprite.getType() == SpriteObject.Type.ENTRANCE) {
                        sprite.setAnimMode(Sprite.Animation.ONCE_ENTRANCE);
                    } else {
                        sprite.setAnimMode(Sprite.Animation.ONCE);
                    }
                    break;
                default:
                    break;
            }
            // get sound
            int[] sound = props.getIntArray("sound_" + sIdx, null);
            if (sound == null) {
                sound = new int[]{-1};
            }
            sprite.setSound(sound);

            sprites.add(sprite);
        }
        SpriteObject[] ret = new SpriteObject[sprites.size()];
        ret = sprites.toArray(ret);
        return ret;
    }

    /**
     * Create a minimap for this level.
     * @param image image to re-use (if null or wrong size, it will be recreated)
     * @param fgImage foreground image used as source for the minimap
     * @param scaleX integer X scaling factor (2 -> half width)
     * @param scaleY integer Y scaling factor (2 -> half height)
     * @param tint apply a greenish color tint
     * @param drawBackground
     * @return image with minimap
     */
    public Image createMinimap(final Image image, final Image fgImage, final int scaleX, final int scaleY,
            final boolean tint, final boolean drawBackground) {
        Level level = GameController.getLevel();
        int width = fgImage.getWidth() / scaleX;
        int height = fgImage.getHeight() / scaleY;
        Image img;

        if (image == null || image.getWidth() != width || image.getHeight() != height) {
            img = ToolBox.createTranslucentImage(width, height);
        } else {
            img = image;
        }
        GraphicsContext gx = img.createGraphicsContext();
        // clear background
        if (tint) {
            gx.setBackground(GameController.BLANK_COLOR);
        } else {
            gx.setBackground(bgColor);
        }
        gx.clearRect(0, 0, width, height);
        // draw background image
        if (drawBackground) {
            drawBackground(gx, fgImage.getWidth(), 0, scaleX, scaleY);
        }
        // draw "behind" objects
        if (level != null && level.sprObjBehind != null) {
            for (int n = level.sprObjBehind.length - 1; n >= 0; n--) {
                SpriteObject spr = level.sprObjBehind[n];
                Image sprImg = spr.getImage();
                gx.drawImage(sprImg, spr.getX() / scaleX, spr.getY() / scaleY,
                        spr.getWidth() / scaleX, spr.getHeight() / scaleY);
            }
        }
        
        gx.drawImage(fgImage, 0, 0, width, height, 0, 0, fgImage.getWidth(), fgImage.getHeight());
        // draw "in front" objects
        if (level != null && level.sprObjFront != null) {
            for (SpriteObject spr : level.sprObjFront) {
                Image sprImg = spr.getImage();
                gx.drawImage(sprImg, spr.getX() / scaleX, spr.getY() / scaleY,
                        spr.getWidth() / scaleX, spr.getHeight() / scaleY);
            }
        }
        gx.dispose();
        // now tint in green
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
    public int getSprObjectNum() {
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
        return entrances[idx];
    }

    /**
     * Get number of entrances for this level.
     * @return number of entrances.
     */
    public int getNumEntrances() {
        if (entrances == null) {
            return 0;
        }
        return entrances.length;
    }

    /**
     * Get background color.
     * @return background color.
     */
    public Color getBgColor() {
        return bgColor;
    }

    /**
     * Get ready state of level.
     * @return true if level is completely loaded.
     */
    public boolean isReady() {
        return ready;
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
        return particleCol;
    }

    /**
     * Get start screen x position.
     * @return start screen x position
     */
    public int getXPosCenter() {
        return xPosCenter;
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
    public int getDebrisColor() {
        return debrisCol;
    }
    
    public int getDebrisColor2() {
        return debrisCol2;
    }

    /**
     * Get release rate: 0 is slowest, 99 is fastest
     * @return release rate: 0 is slowest, 99 is fastest
     */
    public int getReleaseRate() {
        return releaseRate;
    }
    
    public int getWidth() {
        return levelWidth;
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
    
    public int[] getBgWidths() {
        int[] ret = new int[backgrounds.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = backgrounds.get(i).width;
        }
        
        return ret;
    }
    
    public int[] getBgHeights() {
        int[] ret = new int[backgrounds.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = backgrounds.get(i).height;
        }
        
        return ret;
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

    /**
     * Get level name.
     * @return level name
     */
    public String getLevelName() {
        return lvlName;
    }
}

/**
 * Storage class for a level object.
 * @author Volker Oth
 */
class LvlObject {
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
    static final int FLAG_HORIZONTALLY_FLIPPED = 8;
    
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

    /**
     * Constructor
     * @param val five or six values as array [identifier, x position, y position, paint mode, flags, object-specific modifier]
     */
    public LvlObject(final int[] val) {
        id = val[0];
        xPos = val[1];
        yPos = val[2];
        paintMode = val[3];
        flags = val[4];
        objSpecificModifier = (val.length >= 6) ? val[5] : 0;
    }
}

/**
 * Storage class for a terrain/background tile.
 * @author Volker Oth
 */
class Terrain {
    static final int MODE_HORIZONTALLY_FLIPPED = 32;
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

    /**
     * Constructor.
     * @param val four values as array [identifier, x position, y position, modifier]
     * @param special
     */
    public Terrain(final int[] val, final boolean special) {
        id   = val[0];
        xPos = val[1];
        yPos = val[2];
        modifier = val[3];
        specialGraphic = special;
    }
}

/**
 * Storage class for steel tiles.
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

    /**
     * Constructor.
     * @param val four or five values as array [x position, y position, width, height, flags]
     */
    public Steel(final int[] val) {
        xPos = val[0];
        yPos = val[1];
        width = val[2];
        height = val[3];
        negative = (val.length >= 5) ? ((val[4] & 0x01) != 0) : false;
    }
}

/**
 * Storage class for level Entrances.
 * @author Volker Oth
 */
class Entrance {
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

class Background {
    
    List<LvlObject> objects;
    List<Terrain> terrain;
    SpriteObject[] sprObjects;
    SpriteObject[] sprObjFront;
    SpriteObject[] sprObjBehind;
    boolean tiled;
    int width;
    int height;
    int offsetX;
    int offsetY;
    double scrollSpeedX;
    
    Background(final List<LvlObject> o, final List<Terrain> t, final boolean ti,
            final int w, final int h, final int ofsX, final int ofsY, final double ssx) {
        objects = o;
        terrain = t;
        tiled = ti;
        width = w;
        height = h;
        offsetX = ofsX;
        offsetY = ofsY;
        scrollSpeedX = ssx;
    }
}
