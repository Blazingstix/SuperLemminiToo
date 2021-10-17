package lemmini.gameutil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import lemmini.game.GameController;
import lemmini.game.Lemming;
import lemmini.graphics.LemmImage;
import lemmini.tools.ToolBox;
import org.apache.commons.lang3.ArrayUtils;

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
 * Simple sprite class.
 *
 * @author Volker Oth
 */
public class Sprite {
    
    /**
     * Animation style.
     */
    public static enum Animation {
        /** not animated *//** not animated */
        NONE,
        /** endless animation loop */
        LOOP,
        /** triggered animation */
        TRIGGERED,
        /** one animation cycle */
        ONCE,
        ONCE_ENTRANCE;
    }
    
    /** number of animation frames */
    protected final int numFrames;
    /** sprite width in pixels */
    protected int width;
    /** sprite height in pixels */
    protected int height;
    private int counter;
    private final int speed;
    /** index of current animation frame */
    private int frameIdx;
    /** animation mode */
    protected Animation animMode;
    /** indexes for sounds */
    protected int[] sound;
    /** boolean flag: animation was triggered */
    private boolean triggered;
    /** list of animation frames */
    protected List<LemmImage> frames;
    protected boolean modifiable;
    protected int[][] origColors;
    private Lemming lemming;
    
    /**
     * Get Animation depending on integer value from INI.
     * @param t integer type
     * @return Animation
     */
    public static Animation getAnimationMode(final int t) {
        switch (t) {
            case 1:
                return Animation.LOOP;
            case 2:
                return Animation.TRIGGERED;
            case 3:
                return Animation.ONCE;
            default:
                return Animation.NONE;
        }
    }
    
    /**
     * Constructor.
     * @param sourceImg Image containing animation frames one above each other.
     * @param animFrames number of frames.
     * @param animSpeed number of game frames per sprite frame.
     * @param modifiable
     */
    public Sprite(final LemmImage sourceImg, final int animFrames, final int animSpeed, final boolean modifiable) {
        numFrames = animFrames;
        width = sourceImg.getWidth();
        height = sourceImg.getHeight() / numFrames;
        counter = 0;
        speed = animSpeed;
        frameIdx = 0;
        animMode = Animation.NONE;
        sound = ArrayUtils.EMPTY_INT_ARRAY;
        triggered = false;
        // animation frames stored one above the other - now separate them into single images
        frames = ToolBox.getAnimation(sourceImg, animFrames);
        this.modifiable = modifiable;
        if (modifiable) {
            origColors = new int[numFrames][];
            for (ListIterator<LemmImage> lit = frames.listIterator(); lit.hasNext(); ) {
                int i = lit.nextIndex();
                LemmImage frame = ToolBox.copyLemmImage(lit.next());
                lit.set(frame);
                origColors[i] = frame.getRGB(0, 0, width, height, null, 0, width);
            }
        } else {
            origColors = null;
        }
        lemming = null;
    }
    
    /**
     * Constructor.
     * @param frames Animation frames.
     * @param animSpeed number of game frames per sprite frame.
     * @param modifiable
     */
    public Sprite(final List<LemmImage> frames, final int animSpeed, final boolean modifiable) {
        numFrames = frames.size();
        width = frames.get(0).getWidth();
        height = frames.get(0).getHeight();
        counter = 0;
        speed = animSpeed;
        frameIdx = 0;
        animMode = Animation.NONE;
        sound = ArrayUtils.EMPTY_INT_ARRAY;
        triggered = false;
        this.modifiable = modifiable;
        if (modifiable) {
            this.frames = new ArrayList<>(frames);
            origColors = new int[numFrames][];
            for (ListIterator<LemmImage> lit = this.frames.listIterator(); lit.hasNext(); ) {
                int i = lit.nextIndex();
                LemmImage frame = ToolBox.copyLemmImage(lit.next());
                lit.set(frame);
                origColors[i] = frame.getRGB(0, 0, width, height, null, 0, width);
            }
        } else {
            this.frames = frames;
            origColors = null;
        }
        lemming = null;
    }
    
    /**
     * Constructor. Create a Sprite from another Sprite.
     * @param src Sprite to clone.
     */
    public Sprite(final Sprite src) {
        numFrames = src.numFrames;
        width = src.width;
        height = src.height;
        counter = src.counter;
        speed = src.speed;
        frameIdx = src.frameIdx;
        animMode = src.animMode;
        sound = src.sound.clone();
        triggered = src.triggered;
        frames = new ArrayList<>(src.frames);
        for (ListIterator<LemmImage> lit = frames.listIterator(); lit.hasNext(); ) {
            LemmImage frame = new LemmImage(lit.next());
            lit.set(frame);
        }
        modifiable = src.modifiable;
        origColors = ArrayUtils.clone(src.origColors);
        if (origColors != null) {
            for (int i = 0; i < origColors.length; i++) {
                origColors[i] = origColors[i].clone();
            }
        }
        lemming = src.lemming;
    }
    
    /**
     * Get given animation frame.
     * @param idx index of animation frame
     * @return animation frame at position idx.
     */
    public LemmImage getImage(final int idx) {
        return frames.get(idx);
    }
    
    /**
     * Get current animation frame.
     * @return current animation frame.
     */
    public LemmImage getImage() {
        return getImage(frameIdx);
    }
    
    /**
     * Replace one animation frame.
     * Note: replacing with an image of different size will create problems.
     * @param idx index of frame to replace
     * @param img image to use for this animation frame
     */
    public void setImage(final int idx, final LemmImage img) {
        frames.set(idx, img);
    }
    
    /**
     * Get current animation frame and animate.
     * @return current animation frame (before increasing the animation step).
     */
    public LemmImage getImageAnim() {
        switch (animMode) {
            case LOOP:
                if (++counter >= speed) {
                    counter = 0;
                    if (++frameIdx >= numFrames) {
                        frameIdx = 0;
                    }
                }
                break;
            case ONCE:
                if (++counter >= speed) {
                    counter = 0;
                    if (frameIdx < numFrames - 1) {
                        frameIdx++;
                    }
                }
                break;
            case TRIGGERED:
                if (triggered) {
                    if (++counter >= speed) {
                        counter = 0;
                        if (++frameIdx >= numFrames) {
                            frameIdx = 0;
                        }
                    }
                    if (frameIdx > 0) {
                        if (counter == 0 && lemming != null) {
                        	GameController.sound.playVisualSFX(sound[frameIdx], lemming.midX(), lemming.midY());
                        }
                    } else {
                        frameIdx = 0;
                        if (counter >= speed - 1) {
                            triggered = false;
                        }
                    }
                } else {
                    frameIdx = 0;
                    counter = 0;
                }
                break;
            default:
                break;
        }
        return getImage(frameIdx);
    }
    
    /**
     * Set visibility of pixel in all animation frames.
     * @param x x position
     * @param y y position
     * @param visible visible if true, transparent otherwise
     */
    public void setPixelVisibility(final int x, final int y, final boolean visible) {
        if (!modifiable) {
            return;
        }
        if (x >= 0 && x < width && y >= 0 && y < height) {
            for (ListIterator<LemmImage> lit = frames.listIterator(); lit.hasNext(); ) {
                int i = lit.nextIndex();
                lit.next().setRGB(x, y, visible ? origColors[i][y * width + x] : 0);
            }
        }
    }
    
    public void flipSprite(boolean horizontal, boolean vertical) {
        if (!modifiable || (!horizontal && !vertical)) {
            return;
        }
        for (ListIterator<LemmImage> lit = frames.listIterator(); lit.hasNext(); ) {
            int i = lit.nextIndex();
            LemmImage imgSpr = lit.next().transform(false, horizontal, vertical);
            lit.set(imgSpr);
            
            int[] buffer = origColors[i].clone();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    origColors[i][y * width + x] = buffer[(vertical ? (height - y - 1) : y) * width + (horizontal ? (width - x - 1) : x)];
                }
            }
        }
    }
    
    public void applyTint(int tint) {
        if (!modifiable || (tint & 0xff000000) == 0) {
            return;
        }
        
        for (ListIterator<LemmImage> lit = frames.listIterator(); lit.hasNext(); ) {
            int i = lit.nextIndex();
            lit.next().applyTint(tint);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    origColors[i][y * width + x] = LemmImage.applyTint(origColors[i][y * width + x], tint);
                }
            }
        }
    }
    
    /**
     * Get animation mode.
     * @return animation mode.
     */
    public Animation getAnimMode() {
        return animMode;
    }
    
    /**
     * Set animation mode.
     * @param mode animation mode
     */
    public void setAnimMode(final Animation mode) {
        animMode = mode;
    }
    
    /**
     * Check if the animation is a triggered type.
     * @return true if the animation is Animation.TRIGGERED.
     */
    public boolean canBeTriggered() {
        return (animMode == Animation.TRIGGERED);
    }
    
    public boolean isTriggered() {
        return triggered;
    }
    
    /**
     * Trigger a triggered animation
     * @param l
     * @return true if not yet triggered, false if already triggered
     */
    public boolean trigger(Lemming l) {
        if (triggered == true) {
            return false;
        }
        lemming = l;
        if (sound.length == 1) {
            GameController.sound.playVisualSFX(sound[0], l.midX(), l.midY());
        } else if (sound.length > 1) {
            GameController.sound.playVisualSFX(sound[1], l.midX(), l.midY());
        }
        triggered = true;
        frameIdx = 0;
        counter = speed - 1;
        return true;
    }
    
    /**
     * Get current animation frame index.
     * @return current animation frame index
     */
    public int getFrameIdx() {
        return frameIdx;
    }
    
    /** Set current animation frame index.
     * @param frameIdx current animation frame index
     */
    public void setFrameIdx(final int frameIdx) {
        this.frameIdx = frameIdx;
    }
    
    /**
     * Get number of animation frames.
     * @return number of animation frames
     */
    public int getNumFrames() {
        return numFrames;
    }
    
    /**
     * Get sound index.
     * @return sound index
     */
    public int getSound() {
        if (ArrayUtils.isEmpty(sound)) {
            return -1;
        } else {
            return sound[0];
        }
    }
    
    /**
     * Set sound index.
     * @param s sound index
     */
    public void setSound(final int[] s) {
        if (s.length >= 2) {
            sound = new int[numFrames];
            Arrays.fill(sound, -1);
            for (int i = 0; i < s.length - 1; i += 2) {
                if (s[i + 1] >= 0) {
                    sound[s[i + 1]] = s[i];
                }
            }
        } else if (s.length == 1) {
            sound = new int[1];
            sound[0] = s[0];
        } else {
            sound = null;
        }
    }
    
    /**
     * Get width of Sprite in pixels.
     * @return width of Sprite in pixels
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * Get height of Sprite in pixels.
     * @return height of Sprite in pixels
     */
    public int getHeight() {
        return height;
    }
}
