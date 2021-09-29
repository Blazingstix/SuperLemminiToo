package lemmini.game;

import java.awt.Cursor;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
 * Implementation of the Lemmini selection cursor.
 *
 * @author Volker Oth
 */
public class LemmCursor  {

    /** distance from center of cursor to be used to detect Lemmings under the cursor */
    private static final int HIT_DISTANCE = 12;

    /** cursor type */
    public enum CursorType {
        /** normal cursor */
        NORMAL (false, false, false),
        /** select left cursor */
        LEFT (true, false, false),
        /** select right cursor */
        RIGHT (false, true, false),
        /** select walkers cursor */
        WALKER (false, false, true),
        /** select left walkers cursor */
        WALKER_LEFT (true, false, true),
        /** select right walkers cursor */
        WALKER_RIGHT (false, true, true);
        
        private final boolean leftOnly;
        private final boolean rightOnly;
        private final boolean walkerOnly;
        
        CursorType(boolean leftOnly, boolean rightOnly, boolean walkerOnly) {
            this.leftOnly = leftOnly;
            this.rightOnly = rightOnly;
            this.walkerOnly = walkerOnly;
        }
        
        public boolean isLeftOnly() {
            return leftOnly;
        }
        
        public boolean isRightOnly() {
            return rightOnly;
        }
        
        public boolean isWalkerOnly() {
            return walkerOnly;
        }
    }

    /** x position in pixels */
    private static int x;
    /** y position in pixels */
    private static int y;
    /** current cursor type */
    private static CursorType type;
    /** list of images - one for each cursor type */
    private static List<LemmImage> cursorImg;
    /** list of images - one for each box type */
    private static List<LemmImage> boxImg;
    /** list of AWT cursor Objects */
    private static final List<Cursor> cursor = new ArrayList<>(CursorType.values().length);
    /** list of AWT box cursor Objects */
    private static final List<Cursor> boxCursor = new ArrayList<>(CursorType.values().length);
    private static boolean box;

    /**
     * Initialization.
     * @throws ResourceException
     */
    public static void init() throws ResourceException {
        cursor.clear();
        boxCursor.clear();
        CursorType[] cursorTypes = CursorType.values();
        Resource res = Core.findResource("gfx/misc/cursor.png", true, Core.IMAGE_EXTENSIONS);
        cursorImg = ToolBox.getAnimation(Core.loadTranslucentImage(res), cursorTypes.length);
        res = Core.findResource("gfx/misc/box.png", true, Core.IMAGE_EXTENSIONS);
        boxImg = ToolBox.getAnimation(Core.loadTranslucentImage(res), cursorTypes.length);
        LemmImage firstCursorImg = cursorImg.get(0);
        int cx = firstCursorImg.getWidth() / 2;
        int cy = firstCursorImg.getHeight() / 2;
        //for (int i = 0; i < cursorTypes.length; i++) {
        for (Iterator<LemmImage> cursorIt = cursorImg.iterator(), boxIt = boxImg.iterator(); cursorIt.hasNext() && boxIt.hasNext(); ) {
            cursor.add(ToolBox.createCursor(cursorIt.next(), cx, cy));
            boxCursor.add(ToolBox.createCursor(boxIt.next(), cx, cy));
        }

        type = CursorType.NORMAL;
        box = false;
        setX(0);
        setY(0);
    }

    /**
     * Get image for a certain cursor type.
     * @param t cursor type
     * @return image for the given cursor type
     */
    public static LemmImage getImage(final CursorType t) {
        return cursorImg.get(t.ordinal());
    }

    /**
     * Get image for current cursor type.
     * @return image for current cursor type
     */
    public static LemmImage getImage() {
        return getImage(type);
    }
    
    /**
     * Get box image for a certain cursor type.
     * @param t cursor type
     * @return box image for the given cursor type
     */
    public static LemmImage getBoxImage(final CursorType t) {
        return boxImg.get(t.ordinal());
    }
    
    /**
     * Get box image for current cursor type.
     * @return box image for current cursor type
     */
    public static LemmImage getBoxImage() {
        return getBoxImage(type);
    }

    /**
     * Get current cursor as AWT cursor object.
     * @return current cursor as AWT cursor object
     */
    public static Cursor getCursor() {
        if (box) {
            return boxCursor.get(type.ordinal());
        } else {
            return cursor.get(type.ordinal());
        }
    }

    /**
     * Get current cursor type.
     * @return current cursor type
     */
    public static CursorType getType() {
        return type;
    }

    /**
     * Set current cursor type.
     * @param t cursor type
     */
    public static void setType(final CursorType t) {
        type = t;
    }
    
    /**
     * Returns whether the box version of the cursor is in use.
     * @return true if the box version of the cursor is in use, false otherwise
     */
    public static boolean isBox() {
        return box;
    }
    
    /**
     * Sets whether the box version of the cursor should be used.
     * @param b whether the box version of the cursor should be used
     */
    public static void setBox(final boolean b) {
        box = b;
    }

    /**
     * Check if a Lemming is under the cursor.
     * @param l Lemming to check
     * @param xOfs screen x offset
     * @param yOfs screen y offset
     * @return true if the Lemming is under the Cursor, else false.
     */
    public static boolean doesCollide(final Lemming l, final int xOfs, final int yOfs) {
        // get center of lemming
        int lx = l.midX() - xOfs;
        int ly = l.midY() - yOfs;

        // calculate center of cursor
        int cx = getX();
        int cy = getY();

        // calculate distance
        int dx = Math.abs(lx - cx);
        int dy = Math.abs(ly - cy);

        return dx <= HIT_DISTANCE && dy <= HIT_DISTANCE;
    }

    /**
     * Set x position in pixels.
     * @param x x position in pixels.
     */
    public static void setX(final int x) {
        LemmCursor.x = x;
    }

    /**
     * Get x position in pixels.
     * @return x position in pixels
     */
    public static int getX() {
        return x;
    }

    /**
     * Set y position in pixels.
     * @param y y position in pixels
     */
    public static void setY(final int y) {
        LemmCursor.y = y;
    }

    /**
     * Get y position in pixels.
     * @return y position in pixels
     */
    public static int getY() {
        return y;
    }
}
