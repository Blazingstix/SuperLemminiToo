/*
 * Copyright 2016 Ryan Sakowski.
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
package lemmini.game;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import lemmini.gameutil.Sprite;
import lemmini.graphics.LemmImage;
import lemmini.tools.Props;
import lemmini.tools.ToolBox;
import org.apache.commons.lang3.ArrayUtils;

/**
 *
 * @author Ryan Sakowski
 */
public class GraphicSet {
    
    public enum Orientation {
        NORMAL (false, false, false),
        ROTATED_90 (true, false, false),
        FLIPPED_HORIZ (false, true, false),
        ROTATED_90_FLIPPED_HORIZ (true, true, false),
        FLIPPED_VERT (false, false, true),
        ROTATED_90_FLIPPED_VERT (true, false, true),
        ROTATED_180 (false, true, true),
        ROTATED_270 (true, true, true);
        
        private final boolean rotate;
        private final boolean flipHoriz;
        private final boolean flipVert;
        
        private Orientation(boolean rotate, boolean flipHoriz, boolean flipVert) {
            this.rotate = rotate;
            this.flipHoriz = flipHoriz;
            this.flipVert = flipVert;
        }
        
        public boolean rotate() {
            return rotate;
        }
        
        public boolean flipHoriz() {
            return flipHoriz;
        }
        
        public boolean flipVert() {
            return flipVert;
        }
        
        public static Orientation getOrientation(boolean flipHoriz, boolean flipVert, boolean rotate) {
            if (rotate) {
                if (flipHoriz) {
                    if (flipVert) {
                        return ROTATED_270;
                    } else {
                        return ROTATED_90_FLIPPED_HORIZ;
                    }
                } else {
                    if (flipVert) {
                        return ROTATED_90_FLIPPED_VERT;
                    } else {
                        return ROTATED_90;
                    }
                }
            } else {
                if (flipHoriz) {
                    if (flipVert) {
                        return ROTATED_180;
                    } else {
                        return FLIPPED_HORIZ;
                    }
                } else {
                    if (flipVert) {
                        return FLIPPED_VERT;
                    } else {
                        return NORMAL;
                    }
                }
            }
        }
    }
    
    /** array of default ARGB colors for particle effects */
    public static final int[] DEFAULT_PARTICLE_COLORS = {0xff00ff00, 0xff0000ff, 0xffffffff, 0xffffffff, 0xffff0000};
    
    /** list of default styles */
    private static final List<String> DEFAULT_STYLES = Arrays.asList("dirt", "fire", "marble", "pillar", "crystal",
        "brick", "rock", "snow", "bubble", "xmas");
    private static final int DEFAULT_ANIMATION_SPEED = 2;
    private static final int[] SPECIAL_STYLE_OBJECT_ORDER = {0, 1, 7};
    private static final String[] SPECIAL_STYLE_NAMES = {"awesome", "beasti", "beastii", "menace"};
    
    private final String name;
    private final Props props;
    private final Color bgColor;
    private final int debrisColor;
    private final int debrisColor2;
    private final int[] particleColor;
    
    private final List<LvlObject> objects;
    private final List<Terrain> terrain;
    
    public GraphicSet(String name) throws LemmException, ResourceException {
        this.name = name;
        props = new Props();
        
        if (name.toLowerCase(Locale.ROOT).equals("special")) {
            Resource res = Core.findResource("styles/dirt/dirt.ini", true);
            if (!props.load(res)) {
                throw new LemmException("Unable to read dirt.ini.");
            }
            
            // first some global settings
            int bgCol = props.getInt("bgColor", props.getInt("bgColor", 0x000000)) | 0xff000000;
            bgColor = new Color(bgCol);
            debrisColor = props.getInt("debrisColor", 0xffffff) | 0xff000000;
            debrisColor2 = props.getInt("debrisColor2", debrisColor) | 0xff000000;
            particleColor = props.getIntArray("particleColor", props.getIntArray("particleColor", DEFAULT_PARTICLE_COLORS));
            for (int i = 0; i < particleColor.length; i++) {
                particleColor[i] |= 0xff000000;
            }
            
            // load the object data
            objects = new ArrayList<>(SPECIAL_STYLE_OBJECT_ORDER.length);
            for (int idx : SPECIAL_STYLE_OBJECT_ORDER) {
                LvlObject obj = new LvlObject(props, "styles/dirt/dirt", idx);
                if (obj.numFrames < 0) {
                    break;
                }
                objects.add(obj);
            }
            
            // load the terrain data
            terrain = new ArrayList<>(SPECIAL_STYLE_NAMES.length);
            for (String name2 : SPECIAL_STYLE_NAMES) {
                terrain.add(new Terrain("styles/special/" + name2 + "/" + name2, -1, false));
            }
        } else {
            String pathPrefix = "styles/" + name + "/" + name;
            Resource res = Core.findResource(pathPrefix + ".ini", true);
            if (!props.load(res)) {
                throw new LemmException("Unable to read " + name + ".ini.");
            }
            
            // first some global settings
            int bgCol = props.getInt("bgColor", props.getInt("bgColor", 0x000000)) | 0xff000000;
            bgColor = new Color(bgCol);
            debrisColor = props.getInt("debrisColor", 0xffffff) | 0xff000000;
            debrisColor2 = props.getInt("debrisColor2", debrisColor) | 0xff000000;
            particleColor = props.getIntArray("particleColor", props.getIntArray("particleColor", DEFAULT_PARTICLE_COLORS));
            for (int i = 0; i < particleColor.length; i++) {
                particleColor[i] |= 0xff000000;
            }
            
            // load the object data
            objects = new ArrayList<>(64);
            for (int idx = 0; true; idx++) {
                LvlObject obj = new LvlObject(props, pathPrefix, idx);
                if (obj.numFrames < 0) {
                    break;
                }
                objects.add(obj);
            }
            
            // load the terrain data
            int tiles = props.getInt("tiles", 0);
            int[] steelTiles = props.getIntArray("steelTiles", ArrayUtils.EMPTY_INT_ARRAY);
            terrain = new ArrayList<>(tiles);
            for (int idx = 0; idx < tiles; idx++) {
                terrain.add(new Terrain(pathPrefix, idx, ArrayUtils.contains(steelTiles, idx)));
            }
        }
    }
    
    public void unloadImages() {
        objects.stream().forEach(obj -> {
            obj.images.clear();
            obj.mask = null;
        });
        terrain.stream().forEach(ter -> {
            ter.image = null;
            ter.mask = null;
        });
    }
    
    public String getName() {
        return name;
    }
    
    public Color getBgColor() {
        return bgColor;
    }
    
    public int getDebrisColor() {
        return debrisColor;
    }
    
    public int getDebrisColor2() {
        return debrisColor2;
    }
    
    public int[] getParticleColor() {
        return particleColor;
    }
    
    public LvlObject getObject(int idx) {
        return objects.get(idx);
    }
    
    public Terrain getTerrain(int idx) {
        return terrain.get(idx);
    }
    
    public class LvlObject {
        
        private final int index;
        private final String pathPrefix;
        private final int numFrames;
        private final int speed;
        private final Sprite.Animation animMode;
        private final SpriteObject.Type type;
        private final int maskOffsetX;
        private final int maskOffsetY;
        private final int[] sound;
        private final Map<Orientation, List<LemmImage>> images = new EnumMap<>(Orientation.class);
        private boolean[][] mask = null;
        
        private LvlObject(Props props, String pathPrefix, int idx) {
            index = idx;
            this.pathPrefix = pathPrefix;
            // get number of animation frames
            numFrames = props.getInt("frames_" + index, -1);
            // get animation speed
            speed = props.getInt("speed_" + index, DEFAULT_ANIMATION_SPEED);
            // get object type
            type = SpriteObject.getType(props.getInt("type_" + index, 0));
            // get animation mode
            Sprite.Animation animModeTemp = Sprite.getAnimationMode(props.getInt("anim_" + index, 0));
            if (type == SpriteObject.Type.ENTRANCE && animModeTemp == Sprite.Animation.ONCE) {
                animMode = Sprite.Animation.ONCE_ENTRANCE;
            } else {
                animMode = animModeTemp;
            }
            // get mask offsets
            maskOffsetX = props.getInt("maskOffsetX_" + index, 0);
            maskOffsetY = props.getInt("maskOffsetY_" + index, 0);
            // get sound
            int[] soundTemp = props.getIntArray("sound_" + index, ArrayUtils.EMPTY_INT_ARRAY);
            if (soundTemp.length >= 2 && numFrames > 0) {
                sound = new int[numFrames];
                Arrays.fill(sound, -1);
                for (int i = 0; i < soundTemp.length - 1; i += 2) {
                    if (soundTemp[i + 1] >= 0) {
                        sound[soundTemp[i + 1]] = soundTemp[i];
                    }
                }
            } else if (ArrayUtils.isNotEmpty(soundTemp)) {
                sound = new int[1];
                sound[0] = soundTemp[0];
            } else {
                sound = ArrayUtils.EMPTY_INT_ARRAY;
            }
        }
        
        public int getNumFrames() {
            return numFrames;
        }
        
        public int getSpeed() {
            return speed;
        }
        
        public Sprite.Animation getAnimationMode() {
            return animMode;
        }
        
        public SpriteObject.Type getType() {
            return type;
        }
        
        public int getMaskOffsetX() {
            return maskOffsetX;
        }
        
        public int getMaskOffsetY() {
            return maskOffsetY;
        }
        
        public int[] getSound() {
            return sound;
        }
        
        public List<LemmImage> getImages(Orientation orientation) throws ResourceException {
            if (!images.containsKey(Orientation.NORMAL)) {
                Resource res = Core.findResource(
                        pathPrefix + "o_" + index + ".png",
                        true, Core.IMAGE_EXTENSIONS);
                LemmImage sourceImage = Core.loadLemmImage(res);
                images.put(Orientation.NORMAL, ToolBox.getAnimation(sourceImage, numFrames));
            }
            if (!images.containsKey(orientation)) {
                List<LemmImage> newImages = new ArrayList<>(images.get(Orientation.NORMAL));
                for (ListIterator<LemmImage> lit = newImages.listIterator(); lit.hasNext(); ) {
                    LemmImage image = lit.next().transform(orientation.rotate(), orientation.flipHoriz(), orientation.flipVert());
                    lit.set(image);
                }
                images.put(orientation, newImages);
            }
            return images.get(orientation);
        }
        
        public boolean[][] getMask() throws ResourceException {
            if (mask == null) {
                switch (type) {
                    case EXIT:
                    case TURN_LEFT:
                    case TURN_RIGHT:
                    case ONE_WAY_RIGHT:
                    case ONE_WAY_LEFT:
                    case TRAP_DIE:
                    case TRAP_REPLACE:
                    case TRAP_DROWN:
                    case STEEL:
                        Resource res = Core.findResource(
                                pathPrefix + "om_" + index + ".png",
                                true, Core.IMAGE_EXTENSIONS);
                        LemmImage sourceImage = Core.loadLemmImage(res);
                        mask = new boolean[sourceImage.getHeight()][sourceImage.getWidth()];
                        for (int y = 0; y < mask.length; y++) {
                            for (int x = 0; x < mask[y].length; x++) {
                                mask[y][x] = (sourceImage.getRGB(x, y) >>> 24) >= 0x80;
                            }
                        }
                        break;
                    default:
                        mask = new boolean[0][0];
                        break;
                }
            }
            return mask;
        }
    }
    
    public class Terrain {
        
        private final int index;
        private final String pathPrefix;
        private final boolean steel;
        private LemmImage image = null;
        private boolean[][] mask = null;
        private boolean[][] steelMask = null;
        
        private Terrain(String pathPrefix, int idx, boolean stl) {
            index = idx;
            steel = stl;
            this.pathPrefix = pathPrefix;
        }
        
        public boolean isSteel() {
            return steel;
        }
        
        public LemmImage getImage() throws ResourceException {
            if (image == null) {
                Resource res;
                if (index >= 0) {
                    res = Core.findResource(
                            pathPrefix + "_" + index + ".png",
                            true, Core.IMAGE_EXTENSIONS);
                } else {
                    res = Core.findResource(pathPrefix + ".png",
                            true, Core.IMAGE_EXTENSIONS);
                }
                image = Core.loadLemmImage(res);
            }
            return image;
        }
        
        public boolean[][] getMask() throws ResourceException {
            if (mask == null) {
                LemmImage sourceImage;
                try {
                    Resource res;
                    if (index >= 0) {
                        res = Core.findResource(
                                pathPrefix + "m_" + index + ".png",
                                true, Core.IMAGE_EXTENSIONS);
                    } else {
                        res = Core.findResource(pathPrefix + "m.png",
                                true, Core.IMAGE_EXTENSIONS);
                    }
                    sourceImage = Core.loadLemmImage(res);
                } catch (ResourceException ex) {
                    sourceImage = getImage();
                }
                mask = new boolean[sourceImage.getHeight()][sourceImage.getWidth()];
                for (int y = 0; y < mask.length; y++) {
                    for (int x = 0; x < mask[y].length; x++) {
                        mask[y][x] = (sourceImage.getRGB(x, y) >>> 24) >= 0x80;
                    }
                }
            }
            return mask;
        }
        
        public boolean[][] getSteelMask() throws ResourceException {
            if (steelMask == null) {
                if (steel) {
                    LemmImage sourceImage = getImage();
                    steelMask = new boolean[sourceImage.getHeight()][sourceImage.getWidth()];
                    for (int y = 0; y < steelMask.length; y++) {
                        for (int x = 0; x < steelMask[y].length; x++) {
                            steelMask[y][x] = (sourceImage.getRGB(x, y) >>> 24) >= 0x80;
                        }
                    }
                } else {
                    steelMask = new boolean[0][0];
                }
            }
            return steelMask;
        }
    }
}
