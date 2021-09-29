package lemmini.game;

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
 * Defines the stencil which defines the properties of each pixel of the level.
 * @author Volker Oth
 */
public class Stencil {
    
    /** empty space - background is visible */
    public static final int MSK_EMPTY = 0;
    /** brick - Lemmings can walk on it */
    public static final int MSK_BRICK = 1;
    /** steel - can't be destroyed */
    public static final int MSK_STEEL = 1 << 1;
    public static final int MSK_NO_ONE_WAY = 1 << 2;
    public static final int MSK_NO_ONE_WAY_DRAW = 1 << 3;
    public static final int MSK_TURN_LEFT = 1 << 4;
    public static final int MSK_TURN_RIGHT = 1 << 5;
    /** right side of blocker mask - reflects to the right */
    public static final int MSK_BLOCKER_LEFT = 1 << 6;
    /** center of blocker mask */
    public static final int MSK_BLOCKER_CENTER = 1 << 7;
    /** left side of blocker mask - reflects to the left */
    public static final int MSK_BLOCKER_RIGHT = 1 << 8;
    /** blocker mask (either left, center, or right) */
    public static final int MSK_BLOCKER = MSK_BLOCKER_LEFT | MSK_BLOCKER_CENTER | MSK_BLOCKER_RIGHT;
    /** left arrows - no bashing to the right */
    public static final int MSK_ONE_WAY_LEFT = 1 << 9;
    /** right arrows - no bashing to the left */
    public static final int MSK_ONE_WAY_RIGHT = 1 << 10;
    /** no bashing - either left or right */
    public static final int MSK_ONE_WAY = MSK_ONE_WAY_LEFT | MSK_ONE_WAY_RIGHT;

    /** a trap that triggers the drowning animation - i.e. water */
    public static final int MSK_TRAP_LIQUID = 1 << 11;
    /** a trap that removes the Lemming */
    public static final int MSK_TRAP_REMOVE = 1 << 12;
    /** a trap that triggers the normal death animation */
    public static final int MSK_TRAP_FIRE = 1 << 13;
    /** a trap (either LIQUID, REMOVE or FIRE) */
    public static final int MSK_TRAP = MSK_TRAP_LIQUID | MSK_TRAP_REMOVE | MSK_TRAP_FIRE;
    /** the level exit */
    public static final int MSK_EXIT = 1 << 14;

    /** array which represents the stencil buffer */
    private final StencilPixel[] stencil;
    /** width of stencil (=width of level) */
    private final int width;
    /** height of stencil (=height of level) */
    private final int height;


    /**
     * Constructor.
     * @param w width in pixels
     * @param h height in pixels
     */
    public Stencil(final int w, final int h) {
        width = w;
        height = h;
        stencil = new StencilPixel[width * height];
        for (int i = 0; i < stencil.length; i++) {
            stencil[i] = new StencilPixel();
        }
    }

    /**
     * Clear stencil (fill with MSK_EMPTY).
     */
    public void clear() {
        int size = width * height;
        for (int idx = 0; idx < size; idx++) {
            stencil[idx].clear();
        }
    }
        
     /**
     * Set given value at given position.
     * @param x x position in pixels
     * @param y y position in pixels
     * @param val stencil value
     */
    public void setMask(final int x, final int y, final int val) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return;
        }
        
        int pos = x + y * width;
        stencil[pos].setMask(val);
    }

    /**
     * Set given value at given position.
     * @param pos position (x+y*width)
     * @param val stencil value
     */
    public void setMask(final int pos, final int val) {
        int y = pos / width;
        int x = pos % width;
        setMask(x, y, val);
    }
    
    /**
     * AND given value with existing value at given position.
     * @param x x position in pixels
     * @param y y position in pixels
     * @param val stencil value
     */
    public void andMask(final int x, final int y, final int val) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return;
        }
        
        int pos = x + y * width;
        stencil[pos].andMask(val);
    }
    
    /**
     * AND given value with existing value at given position.
     * @param pos position (x*width+y)
     * @param val stencil value
     */
    public void andMask(final int pos, final int val) {
        int y = pos / width;
        int x = pos % width;
        andMask(x, y, val);
    }
    
    /**
     * OR given value with existing value at given position.
     * @param x x position in pixels
     * @param y y position in pixels
     * @param val stencil value
     */
    public void orMask(final int x, final int y, final int val) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return;
        }
        
        int pos = x + y * width;
        stencil[pos].orMask(val);
    }
    
    /**
     * OR given value with existing value at given position.
     * @param pos position (x*width+y)
     * @param val stencil value
     */
    public void orMask(final int pos, final int val) {
        int y = pos / width;
        int x = pos % width;
        orMask(x, y, val);
    }
    
    /**
     * Sets the mask object ID of this stencil pixel.
     * @param x x position in pixels
     * @param y y position in pixels
     * @param id identifier
     */
    public void setMaskObjectID(final int x, final int y, final int id) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return;
        }
        
        int pos = x + y * width;
        stencil[pos].setMaskObjectID(id);
    }

    /**
     * Sets the mask object ID of this stencil pixel.
     * @param pos position (x+y*width)
     * @param id identifier
     */
    public void setMaskObjectID(final int pos, final int id) {
        int y = pos / width;
        int x = pos % width;
        setMaskObjectID(x, y, id);
    }
    
    /**
     * Get stencil value at given position.
     * @param x x position in pixels
     * @param y y position in pixels
     * @return stencil value
     */
    public int getMask(final int x, final int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return 0;
        }
        
        int pos = x + y * width;
        return stencil[pos].getMask();
    }

    /**
     * Get stencil value at given position.
     * @param pos position (x+y*width)
     * @return stencil value
     */
    public int getMask(final int pos) {
        int y = pos / width;
        int x = pos % width;
        return getMask(x, y);
    }
    
    /**
     * Adds an object ID to the stencil.
     * @param x x position in pixels
     * @param y y position in pixels
     * @param id identifier
     */
    public void addID(final int x, final int y, final int id) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return;
        }
        
        int pos = x + y * width;
        stencil[pos].addObjectID(id);
    }

    /**
     * Adds an object ID to the stencil.
     * @param pos position (x+y*width)
     * @param id identifier
     */
    public void addID(final int pos, final int id) {
        int y = pos / width;
        int x = pos % width;
        addID(x, y, id);
    }
    
    /**
     * Gets all object IDs from the stencil.
     * @param x x position in pixels
     * @param y y position in pixels
     * @return identifier
     */
    public int[] getIDs(final int x, final int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return ArrayUtils.EMPTY_INT_ARRAY;
        }
        
        int pos = x + y * width;
        return stencil[pos].getObjectIDs();
    }

    /**
     * Gets all object IDs from the stencil.
     * @param pos position (x+y*width)
     * @return identifier
     */
    public int[] getIDs(final int pos) {
        int y = pos / width;
        int x = pos % width;
        return getIDs(x, y);
    }
    
    public int getMaskObjectID(final int x, final int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return -1;
        }
        
        int pos = x + y * width;
        return stencil[pos].getMaskObjectID();
    }
    
    public int getMaskObjectID(final int pos) {
        int y = pos / width;
        int x = pos % width;
        return getMaskObjectID(x, y);
    }

    /** Get width of stencil.
     * @return width of stencil
     */
    public int getWidth() {
        return width;
    }

    /**
     * Get height of stencil.
     * @return height of stencil
     */
    public int getHeight() {
        return height;
    }
}

class StencilPixel {
    
    private int mask;
    private int maskObjectID;
    private int[] objectIDs;
    
    public StencilPixel() {
        mask = 0;
        maskObjectID = -1;
        objectIDs = ArrayUtils.EMPTY_INT_ARRAY;
    }
    
    public void clear() {
        mask = 0;
        maskObjectID = -1;
        objectIDs = ArrayUtils.EMPTY_INT_ARRAY;
    }
    
    public void setMask(int newMask) {
        mask = newMask;
    }
    
    public void andMask(int newMask) {
        mask &= newMask;
    }
    
    public void orMask(int newMask) {
        mask |= newMask;
    }
    
    public void setMaskObjectID(int mo) {
        maskObjectID = mo;
    }
    
    public void addObjectID(int newID) {
        objectIDs = ArrayUtils.add(objectIDs, newID);
    }
    
    public int getMask() {
        return mask;
    }
    
    public int getMaskObjectID() {
        return maskObjectID;
    }
    
    public int[] getObjectIDs() {
        return objectIDs;
    }
}
