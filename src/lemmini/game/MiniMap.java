package lemmini.game;

import java.awt.Color;
import lemmini.graphics.GraphicsContext;
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
 * Handles the minimap.
 * @author Volker Oth
 */
public class MiniMap {
    
    /** color of Lemmings in minimap */
    private static final Color LEMM_COLOR = Color.RED;
    /** color of screen frame in minimap */
    private static final Color FRAME_COLOR = Color.YELLOW;
    private static final int LEMM_DOT_SCALE = 2;
    private static final int MAX_VISIBLE_WIDTH = 200;
    private static final int MAX_VISIBLE_HEIGHT = 40;
    
    /** image used for minimap */
    private static LemmImage img;
    /** X scale */
    private static double scaleX;
    /** Y scale */
    private static double scaleY;
    /** visible width of minimap */
    private static int visibleWidth;
    /** visible height of minimap */
    private static int visibleHeight;
    private static int xPos;
    private static boolean tinted;
    
    /**
     * init
     * @param sx X Scale
     * @param sy Y Scale
     * @param tint true: apply greenish tint, false: use original colors
     */
    public static void init(final double sx, final double sy, final boolean tint) {
        scaleX = sx;
        scaleY = sy;
        tinted = tint;
        Level level = GameController.getLevel();
        LemmImage fgImage = GameController.getFgImage();
        img = level.createMinimap(fgImage, scaleX, scaleY, false, tint, false);
        visibleWidth = Math.min(img.getWidth(), MAX_VISIBLE_WIDTH);
        visibleHeight = Math.min(img.getHeight(), MAX_VISIBLE_HEIGHT);
        MiscGfx.setMinimapWidth(visibleWidth);
        if (visibleWidth >= img.getWidth()) {
            xPos = 0;
        } else {
            int xPosTemp = ToolBox.scale(GameController.getLevel().getXPosCenter(), scaleX) - visibleWidth / 2;
            xPos = ToolBox.cap(0, xPosTemp, img.getWidth() - visibleWidth);
        }
    }
    
    /**
     * Draw minimap.
     * @param g Graphics object to draw on
     * @param x x position in pixels
     * @param y y position in pixels
     */
    public static void draw(final GraphicsContext g, final int x, final int y) {
        g.drawImage(img, x - xPos, y - ToolBox.scale(GameController.getYPos(), scaleY));
    }
    
    /**
     * Draw Lemming in minimap.
     * @param g Graphics object to draw on
     * @param x x position in pixels
     * @param y y position in pixels
     * @param lx original lemming x position in pixels
     * @param ly original lemming y position in pixels
     */
    public static void drawLemming(final GraphicsContext g, final int x, final int y, final int lx, final int ly) {
        int sx = x + ToolBox.scale(lx, scaleX) - xPos - LEMM_DOT_SCALE / 2;
        int sy = y + ToolBox.scale(ly - GameController.getYPos(), scaleY) - LEMM_DOT_SCALE;
        if (sx + LEMM_DOT_SCALE > x && sx < x + visibleWidth
                && sy + LEMM_DOT_SCALE > y && sy < y + visibleHeight) {
            g.setColor(LEMM_COLOR);
            g.fillRect(sx, sy, LEMM_DOT_SCALE, LEMM_DOT_SCALE);
        }
    }
    
    /**
     * Draw minimap frame.
     * @param g Graphics object to draw on
     * @param x x position in pixels
     * @param y y position in pixels
     */
    public static void drawFrame(final GraphicsContext g, final int x, final int y) {
        int wWidth = ToolBox.scale(Core.getDrawWidth(), scaleX);
        int scaledXPos = ToolBox.scale(GameController.getXPos(), scaleX);
        g.setColor(FRAME_COLOR);
        if (GameController.getWidth() < Core.getDrawWidth()) {
            g.drawRect(x, y, ToolBox.scale(GameController.getWidth(), scaleX) - 1, visibleHeight - 1);
        } else {
            g.drawRect(x + scaledXPos - xPos, y, wWidth - 1, visibleHeight - 1);
        }
    }
    
    /**
     * Return current image.
     * @return current image.
     */
    public static LemmImage getImage() {
        return img;
    }
    
    public static int getVisibleWidth() {
        return visibleWidth;
    }
    
    public static int getVisibleHeight() {
        return visibleHeight;
    }
    
    public static int getXPos() {
        return xPos;
    }
    
    public static double getScaleX() {
        return scaleX;
    }
    
    public static double getScaleY() {
        return scaleY;
    }
    
    /**
     * Move screen frame via minimap.
     * @param x cursor x position relative to minimap in original gfx.
     * @param y cursor y position relative to minimap in original gfx.
     * @return new horizontal screen offset
     */
    public static int move(final int x, final int y) {
        int scaledDrawWidth = ToolBox.scale(Core.getDrawWidth(), scaleX);
        int cappedX = (int) ToolBox.cap(scaledDrawWidth / 2.0, x, Math.max(visibleWidth, scaledDrawWidth) - scaledDrawWidth / 2.0);
        return ToolBox.unscale(cappedX - scaledDrawWidth / 2 + xPos, scaleX);
    }
    
    public static void adjustXPos() {
        if (img.getWidth() <= MAX_VISIBLE_WIDTH) {
            xPos = 0;
        } else {
            int scaledDrawWidth = ToolBox.scale(Core.getDrawWidth(), scaleX);
            int scaledXPos = ToolBox.cap(0, ToolBox.scale(GameController.getXPos(), scaleX), img.getWidth() - scaledDrawWidth);
            if (scaledXPos < xPos) {
                xPos = scaledXPos;
            } else if (scaledXPos > xPos + visibleWidth - scaledDrawWidth) {
                xPos = scaledXPos + scaledDrawWidth - visibleWidth;
            }
        }
    }
    
    public static boolean isTinted() {
        return tinted;
    }
    
    public static int tintColor(int color) {
        int alpha = (color & 0xff000000) >>> 24;
        int sum = 0;
        for (int i = 0; i < 3; i++) {
            sum += ((color >> (8 * i)) & 0xff);
        }
        sum /= 3; // mean value
        sum += 0x60; // make lighter
        if (sum > 0xff) {
            sum = 0xff;
        }
        color = (alpha << 24) | ((sum << 8) & 0xff00);
        return color;
    }
}
