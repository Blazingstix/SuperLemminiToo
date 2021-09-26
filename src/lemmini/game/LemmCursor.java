package lemmini.game;

import java.awt.Cursor;
import lemmini.graphics.Image;
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
        NORMAL,
        /** select left cursor */
        LEFT,
        /** select right cursor */
        RIGHT,
        /** select walkers cursor */
        WALKER,
        WALKER_LEFT,
        WALKER_RIGHT
    }
    
    /** box type */
    public enum BoxType {
        /** normal cursor with selection box */
        NORMAL,
        /** select left cursor with selection box */
        LEFT,
        /** select right cursor with selection box */
        RIGHT,
        /** select walkers cursor with selection box */
        WALKER,
        WALKER_LEFT,
        WALKER_RIGHT
    }

    /** x position in pixels */
    private static int x;
    /** y position in pixels */
    private static int y;
    /** current cursor type */
    private static CursorType type;
    /** array of images - one for each cursor type */
    private static Image[] cursorImg;
    /** array of images - one for each box type */
    private static Image[] boxImg;
    /** array of AWT cursor Objects */
    private static Cursor[] cursor;

    /**
     * Initialization.
     * @throws ResourceException
     */
    public static void init() throws ResourceException {
        String fn = Core.findResource("gfx/misc/cursor.png", Core.IMAGE_EXTENSIONS);
        if (fn == null) {
            throw new ResourceException("gfx/misc/cursor.png");
        }
        cursorImg = ToolBox.getAnimation(Core.loadTranslucentImage(fn), 6);
        fn = Core.findResource("gfx/misc/box.png", Core.IMAGE_EXTENSIONS);
        if (fn == null) {
            throw new ResourceException("gfx/misc/box.png");
        }
        boxImg = ToolBox.getAnimation(Core.loadTranslucentImage(fn), 6);
        cursor = new Cursor[6];
        int w = getImage(CursorType.NORMAL).getWidth() / 2;
        int h = getImage(CursorType.NORMAL).getHeight() / 2;
        cursor[CursorType.NORMAL.ordinal()] = ToolBox.createCursor(getImage(CursorType.NORMAL), w, h);
        cursor[CursorType.LEFT.ordinal()] = ToolBox.createCursor(getImage(CursorType.LEFT), w, h);
        cursor[CursorType.RIGHT.ordinal()] = ToolBox.createCursor(getImage(CursorType.RIGHT), w, h);
        cursor[CursorType.WALKER.ordinal()] = ToolBox.createCursor(getImage(CursorType.WALKER), w, h);
        cursor[CursorType.WALKER_LEFT.ordinal()] = ToolBox.createCursor(getImage(CursorType.WALKER_LEFT), w, h);
        cursor[CursorType.WALKER_RIGHT.ordinal()] = ToolBox.createCursor(getImage(CursorType.WALKER_RIGHT), w, h);

        type = CursorType.NORMAL;
        setX(0);
        setY(0);
    }

    /**
     * Get image for a certain cursor type.
     * @param t cursor type
     * @return image for the given cursor type
     */
    public static Image getImage(final CursorType t) {
        return cursorImg[t.ordinal()];
    }
    
    /**
     * Get image for a certain cursor type.
     * @param t cursor type
     * @return image for the given cursor type
     */
    public static Image getImage(final BoxType t) {
        return boxImg[t.ordinal()];
    }

    /**
     * Get image for current cursor type.
     * @return image for current cursor type
     */
    public static Image getImage() {
        return getImage(type);
    }

    /**
     * Get boxed version of image for the current cursor type.
     * @return boxed version of image for the current cursor type
     */
    public static Image getBoxImage() {
        BoxType t;
        switch (type) {
            case NORMAL:
                t = BoxType.NORMAL;
                break;
            case LEFT:
                t = BoxType.LEFT;
                break;
            case RIGHT:
                t = BoxType.RIGHT;
                break;
            case WALKER:
                t = BoxType.WALKER;
                break;
            case WALKER_LEFT:
                t = BoxType.WALKER_LEFT;
                break;
            case WALKER_RIGHT:
                t = BoxType.WALKER_RIGHT;
                break;
            default:
                t = BoxType.NORMAL; // should never happen
                break;
        }
        return getImage(t);
    }

    /**
     * Get current cursor as AWT cursor object.
     * @return current cursor as AWT cursor object
     */
    public static Cursor getCursor() {
        return cursor[type.ordinal()];
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
     * Check if a Lemming is under the cursor.
     * @param l Lemming to check
     * @param xOfs screen x offset
     * @return true if the Lemming is under the Cursor, else false.
     */
    public static boolean doesCollide(final Lemming l, final int xOfs) {
        // get center of lemming
        int lx = l.midX() - xOfs;
        int ly = l.midY();

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
