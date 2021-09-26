package lemmini.game;

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
 * Masks are used to erase or draw elements in the foreground stencil.
 * E.g. digger, bashers and explosions erase elements from the stencil via a mask.
 * Also stairs created by the builder are handled via a mask.
 *
 * @author Volker Oth
 */
public class Mask {
    /** number of pixels in mask that may be indestructible before action is stopped */
    private final int[] maxMaskPixels;
    /** width of mask in pixels */
    private final int width;
    /** height of mask in pixels */
    private final int height;
    /** array of byte arrays. Note: masks may be animated and thus contain multiple frames. */
    private final byte[][] mask;

    /**
     * Constructor.
     * @param img image which may contain several animation frames one above each other
     * @param frames number of animation frames
     */
    public Mask(final Image img, final int frames) {
        width = img.getWidth();
        height = img.getHeight() / frames;
        mask = new byte[frames][];
        maxMaskPixels = new int[frames];
        for (int i = 0; i < frames; i++) {
            int pos = 0;
            int y0 = i * height;
            maxMaskPixels[i] = 0;
            mask[i] = new byte[width * height];
            for (int y = y0; y < y0 + height; y++, pos += width) {
                for (int x = 0; x < width; x++) {
                    int rgba = img.getRGB(x, y);
                    if ((rgba & 0xff000000) != 0) {
                        mask[i][pos + x] = (byte) 1;
                        maxMaskPixels[i]++;
                    } else {
                        mask[i][pos + x] = (byte) 0;
                    }
                }
            }
            /* now maxMaskPixels[i] contains the exact amount of active pixels in the mask
             * however, it works better to stop a mask action already if there are only about
             * a third of the pixels indestructible, so divide by 3
             * critical: for diggers, the mask is 34 pixels in size but a maximum of
             * 17 pixels is in the mask.
             */
            maxMaskPixels[i] /= 3;
        }
    }

    /**
     * Apply erase mask (to foreground image, minimap and stencil).
     * @param x0 x position in pixels
     * @param y0 y position in pixels
     * @param maskNum index of mask if there are multiple animation frames, else 0
     * @param checkMask Stencil bitmask with attributes that make the pixel indestructible
     * @return true if the number of indestructible pixels was > than maxMaskPixels
     */
    public boolean eraseMask(final int x0, final int y0, final int maskNum, final int checkMask) {
        int ctrIndestructible = 0;
        Image fgImage = GameController.getFgImage();
        Image fgImageSmall = Minimap.getImage();
        Stencil stencil = GameController.getStencil();
        byte[] m = mask[maskNum];
        int sPos = y0 * fgImage.getWidth();
        int pos = 0;
        int scaleX = fgImage.getWidth() / fgImageSmall.getWidth();
        int scaleY = fgImage.getHeight() / fgImageSmall.getHeight();
        int yMax = y0 + height;
        if (yMax >= fgImage.getHeight()) {
            yMax = fgImage.getHeight();
        }
        int xMax = x0 + width;
        if (xMax >= fgImage.getWidth()) {
            xMax = fgImage.getWidth();
        }

        int bgCol = Minimap.isTinted() ? 0 : GameController.getLevel().getBgColor().getRGB();

        for (int y = y0; y < yMax; y++, pos += width, sPos += fgImage.getWidth()) {
            if (y < 0) {
                continue;
            }
            boolean drawSmallY = (y % scaleY) == 0;
            for (int x = x0; x < xMax; x++) {
                if (x < 0) {
                    continue;
                }
                boolean drawSmallX = (x % scaleX) == 0;
                int s = stencil.getMask(sPos + x);
                int[] objects = stencil.getIDs(sPos + x);
                if (m[pos + x - x0] != 0) {
                    if ((s & checkMask) == 0) {
                        // get object
                        for (int obj : objects) {
                            SpriteObject spr = GameController.getLevel().getSprObject(obj);
                            // remove pixel from all object images that are visible only on terrain
                            if (spr != null && spr.getVisOnTerrain()) {
                                spr.setPixelVisibility(x - spr.getX(), y - spr.getY(), false);
                            }
                        }
                        // erase pixel
                        stencil.andMask(sPos + x, ~Stencil.MSK_BRICK); // erase brick in stencil
                        fgImage.setRGB(x, y, 0); // erase pixel in fgImage
                        if (drawSmallX && drawSmallY) {
                            fgImageSmall.setRGB(x / scaleX, y / scaleY, bgCol); // erase pixel in fgImageSmall
                        }
                    } else { // don't erase pixel
                        ctrIndestructible++;
                    }
                }
            }
        }
        return ctrIndestructible > maxMaskPixels[maskNum]; // to be checked
    }

    /**
     * Paint one step (of a stair created by a Builder)
     * @param x0 x position in pixels
     * @param y0 y position in pixels
     * @param maskNum index of mask if there are multiple animation frames, else 0
     * @param color Color to use to paint the step in the foreground image
     */
    public void paintStep(final int x0, final int y0, final int maskNum, final int color) {
        Image fgImage = GameController.getFgImage();
        Image fgImageSmall = Minimap.getImage();
        Stencil stencil = GameController.getStencil();
        byte[] m = mask[maskNum];
        int sPos = y0 * fgImage.getWidth();
        int pos = 0;
        int scaleX = fgImage.getWidth() / fgImageSmall.getWidth();
        int scaleY = fgImage.getHeight() / fgImageSmall.getHeight();
        int yMax = y0 + height;
        if (yMax >= fgImage.getHeight()) {
            yMax = fgImage.getHeight();
        }
        int xMax = x0 + width;
        if (xMax >= fgImage.getWidth()) {
            xMax = fgImage.getWidth();
        }

        for (int y = y0; y < yMax; y++, pos += width, sPos += fgImage.getWidth()) {
            if (y < 0) {
                continue;
            }
            boolean drawSmallY = (y % scaleY) == 0;
            for (int x = x0; x < xMax; x++) {
                if (x < 0) {
                    continue;
                }
                boolean drawSmallX = (x % scaleX) == 0;
                //int s = stencil.getMask(sPos + x);
                int[] objects = stencil.getIDs(sPos + x);
                if (m[pos + x - x0] != 0 /*&& (s & Stencil.MSK_WALK_ON) == 0*/) {
                    // mask pixel set
                    //if ((s & Stencil.MSK_WALK_ON) == 0)
                    //    s |= Stencil.MSK_BRICK;
                    //stencil.setMask(sPos + x, s | Stencil.MSK_STAIR); // set type in stencil
                    // get object
                    for (int obj : objects) {
                        SpriteObject spr = GameController.getLevel().getSprObject(obj);
                        // remove pixel from all object images that are visible only on terrain
                        if (spr != null && spr.getVisOnTerrain()) {
                            spr.setPixelVisibility(x - spr.getX(), y - spr.getY(), true);
                        }
                    }
                    if ((stencil.getMask(sPos + x) & Stencil.MSK_BRICK) == 0) {
                        stencil.orMask(sPos + x, Stencil.MSK_BRICK);
                    }
                    fgImage.setRGB(x, y, color);
                    if (drawSmallX && drawSmallY) {
                        int stepCol;
                        if (Minimap.isTinted()) {
                            stepCol = Minimap.tintColor(color);
                        } else {
                            stepCol = color;
                        }
                        fgImageSmall.setRGB(x / scaleX, y / scaleY, stepCol); // green pixel in fgImageSmall
                    }
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
        Image fgImage = GameController.getFgImage();
        Stencil stencil = GameController.getStencil();
        //byte[] m = mask[0];
        //int sPos = y0 * fgImage.getWidth();
        //int pos = 0;
        int yMax = y0 + height;
        if (yMax >= fgImage.getHeight()) {
            yMax = fgImage.getHeight();
        }
        int xMax = x0 + width;
        if (xMax >= fgImage.getWidth()) {
            xMax = fgImage.getWidth();
        }
        
        for (int i = 0; i < 3; i++)
            for (int y = y0, pos = 0, sPos = y0 * fgImage.getWidth(); y < yMax; y++, pos += width, sPos += fgImage.getWidth()) {
                if (y < 0) {
                    continue;
                }
                for (int x = x0; x < xMax; x++) {
                    if (x < 0) {
                        continue;
                    }
                    if (mask[i][pos + x - x0] != 0) {
                        //if (x < xMid) {
                        //    stencil.orMask(sPos + x, Stencil.MSK_BLOCKER_LEFT); // set type in stencil
                        //} else {
                        //    stencil.orMask(sPos + x, Stencil.MSK_BLOCKER_RIGHT); // set type in stencil
                        //}
                        switch (i) {
                            case 0:
                                stencil.orMask(sPos + x, Stencil.MSK_BLOCKER_LEFT); // set type in stencil
                                break;
                            case 1:
                                stencil.orMask(sPos + x, Stencil.MSK_BLOCKER_CENTER); // set type in stencil
                                break;
                            case 2:
                                stencil.orMask(sPos + x, Stencil.MSK_BLOCKER_RIGHT); // set type in stencil
                                break;
                            default:
                                break;
                        }
                        //fgImage.setRGB(x, y, 0xff00ff00); // debug
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
        byte[] m = mask[maskNum];
        int sPos = y0 * stencil.getWidth();
        int pos = 0;
        int yMax = y0 + height;
        if (yMax >= stencil.getHeight()) {
            yMax = stencil.getHeight();
        }
        int xMax = x0 + width;
        if (xMax >= stencil.getWidth()) {
            xMax = stencil.getWidth();
        }

        for (int y = y0; y < yMax; y++, pos += width, sPos += stencil.getWidth()) {
            if (y < 0) {
                continue;
            }
            for (int x = x0; x < xMax; x++) {
                if (x < 0) {
                    continue;
                }
                if (m[pos + x - x0] != 0) {
                    int s = stencil.getMask(sPos + x);
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
        Image fgImage = GameController.getFgImage();
        Stencil stencil = GameController.getStencil();
        byte[] m = mask[maskNum];
        int sPos = y0 * fgImage.getWidth();
        int pos = 0;
        int yMax = y0 + height;
        if (yMax >= fgImage.getHeight()) {
            yMax = fgImage.getHeight();
        }
        int xMax = x0 + width;
        if (xMax >= fgImage.getWidth()) {
            xMax = fgImage.getWidth();
        }

        for (int y = y0; y < yMax; y++, pos += width, sPos += fgImage.getWidth()) {
            if (y < 0) {
                continue;
            }
            for (int x = x0; x < xMax; x++) {
                if (x < 0) {
                    continue;
                }
                if (m[pos + x - x0] != 0) {
                    int s = stencil.getMask(sPos + x);
                    stencil.andMask(sPos + x, ~type); // erase type in stencil
                    //fgImage.setRGB(x, y, 0xffff0000); // debug
                }
            }
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
