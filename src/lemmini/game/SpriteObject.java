package lemmini.game;

import lemmini.gameutil.Sprite;
import lemmini.graphics.Image;

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
 * Extension of {@link Sprite} to define animated level objects as exits etc.
 * @author Volker Oth
 */
public class SpriteObject extends Sprite {

    /** Type of level object */
    public static enum Type {
        /** no influence on gameplay *//** no influence on gameplay */
        PASSIVE,
        /** Makes a lemming turn left. */
        TURN_LEFT,
        /** Makes a lemming turn right. */
        TURN_RIGHT,
        /** right arrows - no digging to the left */
        NO_DIG_LEFT,
        /** left arrows - no digging to the right */
        NO_DIG_RIGHT,
        /** trap triggering drowning animation */
        TRAP_DROWN,
        /** trap triggering a replacement with special death animation */
        TRAP_REPLACE,
        /** trap triggering default death animation */
        TRAP_DIE,
        /** level exit (active part!) */
        EXIT,
        /** steel */
        STEEL,
        /** level entrance */
        ENTRANCE
    }

    /** x position in pixels */
    private int x;
    /** y position in pixels */
    private int y;
    /** Type of level object */
    private Type type;
    /** collision mask - only this part is copied into the stencil */
    private int[] mask;
    private int maskOffsetX;
    private int maskOffsetY;
    private int maskWidth;
    private int maskHeight;
    private boolean visOnTerrain;

    /**
     * Get Type depending on integer value from INI.
     * @param t integer type
     * @return Type
     */
    public static Type getType(final int t) {
        switch (t) {
            case 1:
                return Type.TURN_LEFT;
            case 2:
                return Type.TURN_RIGHT;
            case 3:
                return Type.NO_DIG_LEFT;
            case 4:
                return Type.NO_DIG_RIGHT;
            case 5:
                return Type.TRAP_DROWN;
            case 6:
                return Type.TRAP_REPLACE;
            case 7:
                return Type.TRAP_DIE;
            case 8:
                return Type.EXIT;
            case 9:
                return Type.STEEL;
            case 32:
                return Type.ENTRANCE;
            default:
                return Type.PASSIVE;
        }
    }

    /**
     * Constructor.
     * @param sourceImg Image containing animation frames one above each other.
     * @param animFrames number of frames.
     */
    public SpriteObject(final Image sourceImg, final int animFrames) {
        super(sourceImg, animFrames);
        type = Type.PASSIVE;
        setX(0);
        setY(0);
    }

    /**
     * Constructor. Create Sprite from other Sprite.
     * @param src Sprite to clone.
     */
    public SpriteObject(final SpriteObject src) {
        super(src);
        setX(src.getX());
        setY(src.getY());
        maskOffsetX = src.getMaskOffsetX();
        maskOffsetY = src.getMaskOffsetY();
        maskWidth = src.getMaskWidth();
        maskHeight = src.getMaskHeight();
        type = src.type;
        mask = src.mask; // flat copy - no deep copy needed!
    }

    /**
     * Set the collision mask.
     * @param imgMask image containing the collision mask.
     */
    void setMask(final Image imgMask) {
        maskWidth = imgMask.getWidth();
        maskHeight = imgMask.getHeight();
        mask = new int[maskWidth * maskHeight];
        maskOffsetX = 0;
        maskOffsetY = 0;
        imgMask.createGraphicsContext().grabPixels(imgMask, 0, 0, maskWidth, maskHeight, mask, 0, maskWidth);
    }
    
    /**
     * Set the collision mask.
     * @param imgMask image containing the collision mask.
     */
    void setMask(final Image imgMask, final int xOffset, final int yOffset) {
        if (imgMask != null) {
            maskWidth = imgMask.getWidth();
            maskHeight = imgMask.getHeight();
            mask = new int[maskWidth * maskHeight];
            imgMask.createGraphicsContext().grabPixels(imgMask, 0, 0, maskWidth, maskHeight, mask, 0, maskWidth);
        }
        maskOffsetX = xOffset;
        maskOffsetY = yOffset;
    }

    /**
     * Get type of level object.
     * @return mask as used in Stencil
     */
    int getMaskType() {
        switch (type) {
            case TURN_LEFT:
                return Stencil.MSK_TURN_LEFT;
            case TURN_RIGHT:
                return Stencil.MSK_TURN_RIGHT;
            case NO_DIG_LEFT:
                return Stencil.MSK_NO_BASH_LEFT;
            case NO_DIG_RIGHT:
                return Stencil.MSK_NO_BASH_RIGHT;
            case TRAP_DROWN:
                return Stencil.MSK_TRAP_LIQUID;
            case TRAP_REPLACE:
                return Stencil.MSK_TRAP_REMOVE;
            case TRAP_DIE:
                return Stencil.MSK_TRAP_FIRE;
            case EXIT:
                return Stencil.MSK_EXIT;
            case STEEL:
                return Stencil.MSK_STEEL;
            default:
                return -1;
        }
    }

    /**
     * Set x position.
     * @param xi x position in pixels
     */
    public final void setX(final int xi) {
        x = xi;
    }

    /**
     * Get x position.
     * @return x position in pixels
     */
    public int getX() {
        return x;
    }

    /**
     * Set y position.
     * @param yi y position in pixels
     */
    public final void setY(final int yi) {
        y = yi;
    }

    /**
     * Get y position.
     * @return y position in pixels
     */
    public int getY() {
        return y;
    }
    
    public int getMaskOffsetX() {
        return maskOffsetX;
    }
    
    public int getMaskOffsetY() {
        return maskOffsetY;
    }
    
    public int getMaskWidth() {
        return maskWidth;
    }
    
    public int getMaskHeight() {
        return maskHeight;
    }
    
    /**
     * Set whether this object is visible only on terrain.
     * @param vis whether this object is visible only on terrain
     */
    public void setVisOnTerrain(boolean vis) {
        visOnTerrain = vis;
    }

    /**
     * Set type of level object.
     * @param t type of level object
     */
    public void setType(final Type t) {
        type = t;
    }

    /**
     * Get type of level object.
     * @return type of level object
     */
    public Type getType() {
        return type;
    }
    
    /**
     * Get whether this object is visible only on terrain.
     * @return whether this object is visible only on terrain
     */
    public boolean getVisOnTerrain() {
        return visOnTerrain;
    }

    /**
     * Get mask value.
     * @param x x position in pixels
     * @param y y position in pixels
     * @return mask value (ARGB).
     */
    public int getMask(final int x, final int y) {
        int tempX = x - maskOffsetX;
        int tempY = y - maskOffsetY;
        if (type != Type.PASSIVE && type != Type.ENTRANCE &&
                tempY * maskWidth + tempX >= 0 && tempY * maskWidth + tempX < mask.length) {
            return mask[tempY * maskWidth + tempX];
        } else {
            return 0;
        }
    }
}
