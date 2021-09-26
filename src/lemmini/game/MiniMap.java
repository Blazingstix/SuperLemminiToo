package lemmini.game;

import java.awt.Color;
import lemmini.Lemmini;
import lemmini.graphics.GraphicsContext;
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
 * Handles the minimap.
 * @author Volker Oth
 */
public class Minimap {

    /** color of Lemmings in minimap */
    private static final Color LEMM_COLOR = Color.RED;
    /** color of screen frame in minimap */
    private static final Color FRAME_COLOR = Color.YELLOW;
    private static final int LEMM_DOT_SCALE = 2;

    /** image used for minimap */
    private static Image img;
    /** X position in main gfx */
    private static int xPos;
    /** Y position in main gfx */
    private static int yPos;
    /** X scale */
    private static int scaleX;
    /** Y scale */
    private static int scaleY;
    /** height of minimap */
    private static int height;
    /** width of minimap */
    private static int width;
    private static boolean tinted;

    /**
     * init
     * @param x X position in main gfx used in drawLemming() and move()
     * @param y Y position in main gfx used in drawLemming() and move()
     * @param sx X Scale (2 -> 0.5)
     * @param sy Y Scale (3 -> 0.333)
     * @param tint true: apply greenish tint, false: use original colors
     */
    public static void init(final int x, final int y, final int sx, final int sy, final boolean tint) {
        xPos = x;
        yPos = y;
        scaleX = sx;
        scaleY = sy;
        tinted = tint;
        Level level = GameController.getLevel();
        Image fgImage = GameController.getFgImage();
        img = level.createMinimap(img, fgImage, scaleX, scaleY, tint, false);
        width = img.getWidth();
        height = img.getHeight();
    }

    /**
     * Draw minimap.
     * @param g Graphics object to draw on
     * @param x x position in pixels
     * @param y y position in pixels
     */
    public static void draw(final GraphicsContext g, final int x, final int y) {
        g.drawImage(img, x, y);
    }

    /**
     * Draw Lemming in minimap.
     * @param g Graphics object to draw on
     * @param lx original lemming x position in pixels
     * @param ly original lemming y position in pixels
     */
    public static void drawLemming(final GraphicsContext g, final int lx, final int ly) {
        int x = xPos + lx / scaleX;
        int y = yPos + ly / scaleY;
        if (x > xPos + width - LEMM_DOT_SCALE) {
            x = xPos + width - LEMM_DOT_SCALE;
        } 
        if (x < xPos) {
            x = xPos;
        }
        if (y > yPos + height - LEMM_DOT_SCALE) {
            y = yPos + height - LEMM_DOT_SCALE;
        }
        if (y < yPos) {
            y = yPos;
        }
        g.setColor(LEMM_COLOR);
        g.fillRect(x, y, LEMM_DOT_SCALE, LEMM_DOT_SCALE);
    }
    
    /**
     * Draw minimap frame.
     * @param g Graphics object to draw on
     * @param x x position in pixels
     * @param y y position in pixels
     * @param xOfs horizontal level offset
     */
    public static void drawFrame(final GraphicsContext g, final int x, final int y, final int xOfs) {
        int wWidth = Lemmini.getPaneWidth();
        g.setColor(FRAME_COLOR);
        if (GameController.getWidth() < Lemmini.getPaneWidth()) {
            g.drawRect(x, y, GameController.getWidth() / scaleX, img.getHeight() - 1);
        } else {
            g.drawRect(x + xOfs / scaleX, y, wWidth / scaleX, img.getHeight() - 1);
        }
    }

    /**
     * Return current image.
     * @return current image.
     */
    public static Image getImage() {
        return img;
    }

    /**
     * Move screen frame via minimap.
     * @param x cursor x position in original gfx.
     * @param y cursor y position in original gfx.
     * @param swidth
     * @return new horizontal screen offset
     */
    public static int move(final int x, final int y, final int swidth) {
        if (y < yPos || y >= yPos + height || x < xPos || x >= xPos + width) {
            return -1; // cursor outside the minimap
        }
        int xOfs;
        if (swidth > GameController.getWidth()) {
            xOfs = (GameController.getWidth() - swidth) / 2;
        } else {
            xOfs = (x - xPos) * scaleX - swidth / 2;
            if (xOfs > GameController.getWidth() - swidth) {
                xOfs = GameController.getWidth() - swidth;
            }
            if (xOfs < 0) {
                xOfs = 0;
            }
        }
        return xOfs;
    }
    
    public static boolean isTinted() {
        return tinted;
    }
    
    public static int tintColor(int color) {
        //if ((color & 0xff000000) == 0) {
        //    return 0;
        //}
        int alpha = (color & 0xff000000) >>> 24;
        int sum = 0;
        for (int i = 0; i < 3; i++, color >>= 8) {
            sum += (color & 0xff);
        }
        sum /= 3; // mean value
        //if (sum != 0) {
            sum += 0x60; // make lighter
        //}
        if (sum > 0xff) {
            sum = 0xff;
        }
        color = (alpha << 24) | ((sum << 8) & 0xff00);
        return color;
    }
}
