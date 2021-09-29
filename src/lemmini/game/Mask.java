package lemmini.game;

import lemmini.graphics.LemmImage;
import lemmini.tools.ToolBox;
import org.apache.commons.lang3.BooleanUtils;

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
 * Masks are used to erase or draw elements in the foreground stencil.
 * E.g. digger, bashers and explosions erase elements from the stencil via a mask.
 * Also stairs created by the builder are handled via a mask.
 *
 * @author Volker Oth
 */
public class Mask {
    
    /** width of mask in pixels */
    private final int width;
    /** height of mask in pixels */
    private final int height;
    /** array of images. Note: masks may be animated and thus contain multiple frames. */
    private final LemmImage[] mask;
    private final LemmImage[] unpatchedMask;

    /**
     * Constructor.
     * @param img image which may contain several animation frames one above each other
     * @param frames number of animation frames
     */
    public Mask(final LemmImage img, final int frames) {
        width = img.getWidth();
        height = img.getHeight() / frames;
        mask = ToolBox.getAnimation(img, frames);
        unpatchedMask = mask.clone();
    }

    /**
     * Apply erase mask (to foreground image, minimap and stencil).
     * @param x0 x position in pixels
     * @param y0 y position in pixels
     * @param maskNum index of mask if there are multiple animation frames, else 0
     * @param eraseMask Stencil attributes to erase
     * @param checkMask Stencil attributes that make the pixel indestructible
     */
    public void eraseMask(final int x0, final int y0, final int maskNum, final int eraseMask, final int checkMask) {
        LemmImage fgImage = GameController.getFgImage();
        LemmImage fgImageSmall = Minimap.getImage();
        Stencil stencil = GameController.getStencil();
        LemmImage m = mask[maskNum];
        double scaleX = (double) fgImageSmall.getWidth() / (double) fgImage.getWidth();
        double scaleY = (double) fgImageSmall.getHeight() / (double) fgImage.getHeight();
        double scaleXHalf = scaleX / 2.0;
        double scaleYHalf = scaleY / 2.0;
        int yMax = y0 + height;
        if (yMax >= fgImage.getHeight()) {
            yMax = fgImage.getHeight();
        }
        int xMax = x0 + width;
        if (xMax >= fgImage.getWidth()) {
            xMax = fgImage.getWidth();
        }

        int bgCol = Minimap.isTinted() ? 0 : GameController.getLevel().getBgColor().getRGB();

        for (int y = y0; y < yMax; y++) {
            if (y < 0) {
                continue;
            }
            double scaledY = (y + 0.5) * scaleY % 1.0;
            boolean drawSmallY = (scaledY >= (0.5 - scaleYHalf) % 1.0 && scaledY < (0.5 + scaleYHalf) % 1.0)
                    || Math.abs(scaleY) >= 1.0;
            for (int x = x0; x < xMax; x++) {
                if (x < 0) {
                    continue;
                }
                double scaledX = (x + 0.5) * scaleX % 1.0;
                boolean drawSmallX = (scaledX >= (0.5 - scaleXHalf) % 1.0 && scaledX < (0.5 + scaleXHalf) % 1.0)
                        || Math.abs(scaleX) >= 1.0;
                int maskAlpha = m.getRGB(x - x0, y - y0) >>> 24;
                int s = stencil.getMask(x, y);
                if (!BooleanUtils.toBoolean(s & checkMask)) {
                    int[] objects = stencil.getIDs(x, y);
                    // erase pixel
                    fgImage.removeAlpha(x, y, maskAlpha); // erase pixel in fgImage
                    if (drawSmallX && drawSmallY) {
                        // erase pixel in fgImageSmall
                        fgImageSmall.removeAlpha(ToolBox.scale(x, scaleX), ToolBox.scale(y, scaleY), maskAlpha);
                        fgImageSmall.addRGBBehind(ToolBox.scale(x, scaleX), ToolBox.scale(y, scaleY), bgCol);
                    }
                    if (!fgImage.isPixelOpaque(x, y)) {
                        for (int obj : objects) {
                            SpriteObject spr = GameController.getLevel().getSprObject(obj);
                            // remove pixel from all object images that are visible only on terrain
                            if (spr != null && spr.getVisOnTerrain()) {
                                spr.setPixelVisibility(x - spr.getX(), y - spr.getY(), false);
                            }
                        }
                    }
                    if (m.isPixelOpaque(x - x0, y - y0)) {
                        // erase brick in stencil
                        stencil.andMask(x, y, ~eraseMask);
                    }
                }
            }
        }
    }

    /**
     * Paint one step (of a stair created by a Builder)
     * @param x0 x position in pixels
     * @param y0 y position in pixels
     * @param maskNum index of mask if there are multiple animation frames, else 0
     */
    public void paintStep(final int x0, final int y0, final int maskNum) {
        LemmImage fgImage = GameController.getFgImage();
        LemmImage fgImageSmall = Minimap.getImage();
        Stencil stencil = GameController.getStencil();
        LemmImage m = mask[maskNum];
        double scaleX = (double) fgImageSmall.getWidth() / (double) fgImage.getWidth();
        double scaleY = (double) fgImageSmall.getHeight() / (double) fgImage.getHeight();
        double scaleXHalf = scaleX / 2.0;
        double scaleYHalf = scaleY / 2.0;
        int yMax = y0 + height;
        if (yMax >= fgImage.getHeight()) {
            yMax = fgImage.getHeight();
        }
        int xMax = x0 + width;
        if (xMax >= fgImage.getWidth()) {
            xMax = fgImage.getWidth();
        }

        for (int y = y0; y < yMax; y++) {
            if (y < 0) {
                continue;
            }
            double scaledY = (y + 0.5) * scaleY % 1.0;
            boolean drawSmallY = (scaledY >= (0.5 - scaleYHalf) % 1.0
                    && scaledY < (0.5 + scaleYHalf) % 1.0)
                    || Math.abs(scaleY) >= 1.0;
            for (int x = x0; x < xMax; x++) {
                if (x < 0) {
                    continue;
                }
                double scaledX = (x + 0.5) * scaleX % 1.0;
                boolean drawSmallX = (scaledX >= (0.5 - scaleXHalf) % 1.0
                        && scaledX < (0.5 + scaleXHalf) % 1.0)
                        || Math.abs(scaleX) >= 1.0;
                int color = m.getRGB(x - x0, y - y0);
                int[] objects = stencil.getIDs(x, y);
                fgImage.addRGB(x, y, color);
                if (drawSmallX && drawSmallY) {
                    int stepCol;
                    if (Minimap.isTinted()) {
                        stepCol = Minimap.tintColor(color);
                    } else {
                        stepCol = color;
                    }
                    // green pixel in fgImageSmall
                    fgImageSmall.addRGB(ToolBox.scale(x, scaleX), ToolBox.scale(y, scaleY), stepCol);
                }
                if (fgImage.isPixelOpaque(x, y)) {
                    // get object
                    for (int obj : objects) {
                        SpriteObject spr = GameController.getLevel().getSprObject(obj);
                        // add pixel to all object images that are visible only on terrain
                        if (spr != null && spr.getVisOnTerrain()
                                && (GameController.getLevel().getClassicSteel() 
                                        || !spr.getType().isSometimesIndestructible())
                                && !(spr.getType().isSometimesIndestructible()
                                        && BooleanUtils.toBoolean(stencil.getMask(x, y) & Stencil.MSK_NO_ONE_WAY_DRAW))) {
                            spr.setPixelVisibility(x - spr.getX(), y - spr.getY(), true);
                        }
                    }
                }
                if (m.isPixelOpaque(x - x0, y - y0)) {
                    stencil.orMask(x, y, Stencil.MSK_BRICK);
                }
            }
        }
    }

    /**
     * Create blocker mask in the Stencil only (Lemming is assigned a Blocker)
     * @param x0 x position in pixels
     * @param y0 y position in pixels
     */
    public void setBlockerMask(final int x0, final int y0) {
        LemmImage fgImage = GameController.getFgImage();
        Stencil stencil = GameController.getStencil();
        int yMax = y0 + height;
        if (yMax >= fgImage.getHeight()) {
            yMax = fgImage.getHeight();
        }
        int xMax = x0 + width;
        if (xMax >= fgImage.getWidth()) {
            xMax = fgImage.getWidth();
        }
        
        for (int i = 0; i < 3; i++) {
            LemmImage m = mask[i];
            for (int y = y0; y < yMax; y++) {
                if (y < 0) {
                    continue;
                }
                for (int x = x0; x < xMax; x++) {
                    if (x < 0) {
                        continue;
                    }
                    if (m.isPixelOpaque(x - x0, y - y0)) {
                        switch (i) {
                            case 0:
                                stencil.orMask(x, y, Stencil.MSK_BLOCKER_LEFT); // set type in stencil
                                break;
                            case 1:
                                stencil.orMask(x, y, Stencil.MSK_BLOCKER_CENTER); // set type in stencil
                                break;
                            case 2:
                                stencil.orMask(x, y, Stencil.MSK_BLOCKER_RIGHT); // set type in stencil
                                break;
                            default:
                                break;
                        }
                        //fgImage.setRGB(x, y, 0xff00ff00); // debug
                    }
                }
            }
        }
    }
    
    /**
     * Use mask to check bitmask properties of Stencil.
     * @param x0 x position in pixels
     * @param y0 y position in pixels
     * @param maskNum index of mask if there are multiple animation frames, else 0
     * @param type Stencil bitmask to check (may contain several attributes)
     * @return true if at least one pixel with one of the given attributes is found
     */
    public boolean checkType(final int x0, final int y0, final int maskNum, final int type) {
        Stencil stencil = GameController.getStencil();
        LemmImage m = mask[maskNum];
        int yMax = y0 + height;
        if (yMax >= stencil.getHeight()) {
            yMax = stencil.getHeight();
        }
        int xMax = x0 + width;
        if (xMax >= stencil.getWidth()) {
            xMax = stencil.getWidth();
        }

        for (int y = y0; y < yMax; y++) {
            if (y < 0) {
                continue;
            }
            for (int x = x0; x < xMax; x++) {
                if (x < 0) {
                    continue;
                }
                if (m.isPixelOpaque(x - x0, y - y0)) {
                    int s = stencil.getMask(x, y);
                    if ((s & type) != 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Erase certain properties from Stencil bitmask.
     * @param x0 x position in pixels
     * @param y0 y position in pixels
     * @param maskNum index of mask if there are multiple animation frames, else 0
     * @param type Stencil bitmask to erase (may contain several attributes)
     */
    public void clearType(final int x0, final int y0, final int maskNum, final int type) {
        LemmImage fgImage = GameController.getFgImage();
        Stencil stencil = GameController.getStencil();
        LemmImage m = mask[maskNum];
        int yMax = y0 + height;
        if (yMax >= fgImage.getHeight()) {
            yMax = fgImage.getHeight();
        }
        int xMax = x0 + width;
        if (xMax >= fgImage.getWidth()) {
            xMax = fgImage.getWidth();
        }

        for (int y = y0; y < yMax; y++) {
            if (y < 0) {
                continue;
            }
            for (int x = x0; x < xMax; x++) {
                if (x < 0) {
                    continue;
                }
                if (m.isPixelOpaque(x - x0, y - y0)) {
                    stencil.andMask(x, y, ~type); // erase type in stencil
                    //fgImage.setRGB(x, y, 0xffff0000); // debug
                }
            }
        }
    }
    
    void replaceColors(final int templateCol, final int replaceCol,
            final int templateCol2, final int replaceCol2) {
        for (int f = 0; f < mask.length; f++) { // go through all frames
            LemmImage i = new LemmImage(unpatchedMask[f]);
            i.replaceColor(templateCol, replaceCol);
            i.replaceColor(templateCol2, replaceCol2);
            mask[f] = i;
        }
    }

    /**
     * Get width.
     * @return width in pixels.
     */
    public int getWidth() {
        return width;
    }

    /**
     * Get height.
     * @return height in pixels.
     */
    public int getHeight() {
        return height;
    }
    
    /**
     * Get the number of mask frames.
     * @return number of mask frames.
     */
    public int getNumFrames() {
        return mask.length;
    }
}
