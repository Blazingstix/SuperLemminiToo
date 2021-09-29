package lemmini.gameutil;

import java.util.Arrays;
import lemmini.game.GameController;
import lemmini.game.Lemming;
import lemmini.graphics.GraphicsOperation;
import lemmini.graphics.LemmImage;
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

    /** sprite width in pixels */
    protected int width;
    /** sprite height in pixels */
    protected int height;
    /** number of animation frames */
    private int numFrames;
    /** index of current animation frame */
    private int frameIdx;
    /** animation mode */
    private Animation animMode;
    /** index for a sound */
    private int[] sound;
    /** boolean flag: animation was triggered */
    private boolean triggered;
    private int counter;
    /** array of animation frames */
    private LemmImage[] frames;
    private int[][] origColors;
    private Lemming lemming;

    /**
     * Constructor.
     * @param sourceImg Image containing animation frames one above each other.
     * @param animFrames number of frames.
     */
    public Sprite(final LemmImage sourceImg, final int animFrames) {
        init(sourceImg, animFrames);
    }

    /**
     * Constructor. Create Sprite from other Sprite.
     * @param src Sprite to clone.
     */
    public Sprite(final Sprite src) {
        copyFrom(src);
    }

    /**
     * Copy all class attributes from another Sprite to this one.
     * @param src Sprite to copy from.
     */
    private void copyFrom(final Sprite src) {
        numFrames = src.numFrames;
        width = src.width;
        height = src.height;
        frameIdx = src.frameIdx;
        animMode = src.animMode;
        sound = src.sound;
        triggered = false;
        frames = src.frames.clone();
        for (int i = 0; i < frames.length; i++) {
            frames[i] = new LemmImage(frames[i]);
        }
        origColors = src.origColors.clone();
        for (int i = 0; i < origColors.length; i++) {
            origColors[i] = origColors[i].clone();
        }
    }

    /**
     * Initialized sprite with new animation.
     * @param sourceImg Image containing animation frames one above each other.
     * @param animFrames number of frames.
     */
    private void init(final LemmImage sourceImg, final int animFrames) {
        numFrames = animFrames;
        width = sourceImg.getWidth();
        height = sourceImg.getHeight() / numFrames;
        frameIdx = 0;
        animMode = Animation.NONE;
        triggered = false;
        counter = 0;
        // animation frames stored one above the other - now separate them into single images
        frames = ToolBox.getAnimation(sourceImg, animFrames);
        origColors = new int[numFrames][];
        for (int i = 0; i < origColors.length; i++) {
            origColors[i] = frames[i].getRGB(0, 0, width, height, null, 0, width);
        }
    }

    /**
     * Get given animation frame.
     * @param idx index of animation frame
     * @return animation frame at position idx.
     */
    public LemmImage getImage(final int idx) {
        return frames[idx];
    }

    /**
     * Get current animation frame.
     * @return current animation frame.
     */
    public LemmImage getImage() {
        return frames[frameIdx];
    }

    /**
     * Replace one animation frame.
     * Note: replacing with an image of different size will create problems.
     * @param idx index of frame to replace
     * @param img image to use for this animation frame
     */
    public void setImage(final int idx, final LemmImage img) {
        frames[idx] = img;
    }

    /**
     * Get current animation frame and animate.
     * @return current animation frame (before increasing the animation step).
     */
    public LemmImage getImageAnim() {
        LemmImage i = frames[frameIdx];
        switch (animMode) {
            case LOOP:
                if (++frameIdx >= numFrames) {
                    frameIdx = 0;
                }
                break;
            case ONCE:
                if (frameIdx < numFrames - 1) {
                    frameIdx++;
                }
                break;
            case TRIGGERED:
                if (triggered) {
                    if (counter == 0 && frameIdx < numFrames - 1) {
                        if (frameIdx < sound.length && lemming != null) {
                            GameController.sound.play(sound[frameIdx + 1], lemming.getPan());
                        }
                        frameIdx++;
                    } else {
                        frameIdx = 0;
                        if (counter++ >= GameController.MAX_ANIM_CTR - 1) {
                            counter = 0;
                            triggered = false;
                        }
                    }
                } else {
                    frameIdx = 0;
                }
                break;
            default:
                break;
        }
        return i;
    }

    /**
     * Set pixel in all animation frames.
     * @param x x position
     * @param y y position
     * @param color color as TYPE_INT_ARGB
     */
    public void setPixel(final int x, final int y, final int color) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            for (int i = 0; i < numFrames; i++) {
                frames[i].setRGB(x, y, color);
            }
        }
    }

    /**
     * Set visibility of pixel in all animation frames.
     * @param x x position
     * @param y y position
     * @param visible visible if true, transparent otherwise
     */
    public void setPixelVisibility(final int x, final int y, final boolean visible) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            for (int i = 0; i < numFrames; i++) {
                if (visible) {
                    frames[i].setRGB(x, y, origColors[i][y * width + x]);
                } else {
                    frames[i].setRGB(x, y, 0);
                }
            }
        }
    }
    
    public void flipSprite(boolean horizontal, boolean vertical) {
        if (!horizontal && !vertical) {
            return;
        }
        GraphicsOperation go = ToolBox.createGraphicsOperation();
        go.setToScale(horizontal ? -1 : 1, vertical ? -1 : 1);
        go.translate(horizontal ? -width : 0, vertical ? -height : 0);
        for (int frame = 0; frame < frames.length; frame++) {
            LemmImage imgSpr = ToolBox.createTranslucentImage(width, height);
            go.execute(frames[frame], imgSpr);
            frames[frame] = imgSpr;
            
            int[] buffer = origColors[frame].clone();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    origColors[frame][y * width + x] = buffer[(vertical ? (height - y - 1) : y) * width + (horizontal ? (width - x - 1) : x)];
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
            GameController.sound.play(sound[0], l.getPan());
        } else if (sound.length > 1) {
            GameController.sound.play(sound[1], l.getPan());
        }
        triggered = true;
        frameIdx = 1;
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
        if (sound == null || sound.length == 0) {
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
