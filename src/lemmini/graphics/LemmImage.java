package lemmini.graphics;

import java.awt.image.BufferedImage;


/*
 * FILE MODIFIED BY RYAN SAKOWSKI
 * 
 * 
 * Copyright 2010 Arne Limburg
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

public class LemmImage {

    private final BufferedImage image;
    
    public LemmImage(BufferedImage image) {
        this.image = image;
    }
    
    public LemmImage(LemmImage image) {
        this.image = new BufferedImage(image.getWidth(), image.getHeight(), image.getImage().getType());
        this.image.setData(image.getImage().getData());
    }
    
    public BufferedImage getImage() {
        return image;
    }
    
    public GraphicsContext createGraphicsContext() {
        return new GraphicsContext(image.createGraphics());
    }
    
    public LemmImage getSubimage(int x, int y, int w, int h) {
        return new LemmImage(image.getSubimage(x, y, w, h));
    }

    public int getWidth() {
        return image.getWidth();
    }

    public int getHeight() {
        return image.getHeight();
    }

    public int getRGB(int x, int y) {
        if (x >= 0 && x < getWidth() && y >= 0 && y < getHeight()) {
            return image.getRGB(x, y);
        } else {
            return 0;
        }
    }
    
    public int[] getRGB(int startX, int startY, int w, int h, int[] rgbArray, int offset, int scansize) {
        return image.getRGB(startX, startY, w, h, rgbArray, offset, scansize);
    }

    public void setRGB(int x, int y, int rgb) {
        if (x >= 0 && x < getWidth() && y >= 0 && y < getHeight()) {
            image.setRGB(x, y, rgb);
        }
    }

    public void addRGB(int x, int y, int rgb) {
        if (x < 0 || x >= getWidth() || y < 0 || y >= getHeight()) {
            return;
        }
        
        final double alphaA = intToDouble((image.getRGB(x, y) >>> 24) & 0xff);
        final double alphaB = intToDouble((rgb >>> 24) & 0xff);
        if (alphaB <= 0.0) {
            return;
        } else if (alphaA <= 0.0 || alphaB >= 1.0) {
            image.setRGB(x, y, rgb);
            return;
        }
        final double redA = intToDouble((image.getRGB(x, y) >>> 16) & 0xff);
        final double greenA = intToDouble((image.getRGB(x, y) >>> 8) & 0xff);
        final double blueA = intToDouble(image.getRGB(x, y) & 0xff);
        final double redB = intToDouble((rgb >>> 16) & 0xff);
        final double greenB = intToDouble((rgb >>> 8) & 0xff);
        final double blueB = intToDouble(rgb & 0xff);
        
        double alphaNew = alphaB + alphaA * (1.0 - alphaB);
        double redNew = (redB * alphaB + redA * alphaA * (1.0 - alphaB)) / alphaNew;
        double greenNew = (greenB * alphaB + greenA * alphaA * (1.0 - alphaB)) / alphaNew;
        double blueNew = (blueB * alphaB + blueA * alphaA * (1.0 - alphaB)) / alphaNew;
        
        int rgbNew = (doubleToInt(alphaNew) << 24) | (doubleToInt(redNew) << 16)
                | (doubleToInt(greenNew) << 8) | doubleToInt(blueNew);
        
        image.setRGB(x, y, rgbNew);
    }
    
    public void addRGBBehind(int x, int y, int rgb) {
        if (x < 0 || x >= getWidth() || y < 0 || y >= getHeight()) {
            return;
        }
        
        final double alphaA = intToDouble((image.getRGB(x, y) >>> 24) & 0xff);
        final double alphaB = intToDouble((rgb >>> 24) & 0xff);
        if (alphaA >= 1.0 || alphaB <= 0.0) {
            return;
        } else if (alphaA <= 0.0) {
            image.setRGB(x, y, rgb);
            return;
        }
        final double redA = intToDouble((image.getRGB(x, y) >>> 16) & 0xff);
        final double greenA = intToDouble((image.getRGB(x, y) >>> 8) & 0xff);
        final double blueA = intToDouble(image.getRGB(x, y) & 0xff);
        final double redB = intToDouble((rgb >>> 16) & 0xff);
        final double greenB = intToDouble((rgb >>> 8) & 0xff);
        final double blueB = intToDouble(rgb & 0xff);
        
        double alphaNew = alphaA + alphaB * (1.0 - alphaA);
        double redNew = (redA * alphaA + redB * alphaB * (1.0 - alphaA)) / alphaNew;
        double greenNew = (greenA * alphaA + greenB * alphaB * (1.0 - alphaA)) / alphaNew;
        double blueNew = (blueA * alphaA + blueB * alphaB * (1.0 - alphaA)) / alphaNew;
        
        int rgbNew = (doubleToInt(alphaNew) << 24) | (doubleToInt(redNew) << 16)
                | (doubleToInt(greenNew) << 8) | doubleToInt(blueNew);
        
        image.setRGB(x, y, rgbNew);
    }
    
    public void removeAlpha(int x, int y, int alpha) {
        if (x < 0 || x >= getWidth() || y < 0 || y >= getHeight()) {
            return;
        }
        
        final double alphaA = intToDouble((image.getRGB(x, y) >>> 24) & 0xff);
        final double alphaB = intToDouble(alpha);
        if (alphaB >= 1.0) {
            image.setRGB(x, y, 0);
            return;
        } else if (alphaB <= 0.0) {
            return;
        }
        
        int alphaNew = doubleToInt(alphaA * (1.0 - alphaB));
        if (alphaNew > 0) {
            image.setRGB(x, y, image.getRGB(x, y) & 0xffffff | alphaNew << 24);
        } else {
            image.setRGB(x, y, 0);
        }
    }
    
    public void applyTint(int tint) {
        final double alphaB = intToDouble((tint >>> 24) & 0xff);
        final double redB = intToDouble((tint >>> 16) & 0xff);
        final double greenB = intToDouble((tint >>> 8) & 0xff);
        final double blueB = intToDouble(tint & 0xff);
        if (alphaB <= 0.0) {
            return;
        }
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alphaA = (image.getRGB(x, y) >>> 24) & 0xff;
                if (alphaA <= 0) {
                    continue;
                }
                double redA = intToDouble((image.getRGB(x, y) >>> 16) & 0xff);
                double greenA = intToDouble((image.getRGB(x, y) >>> 8) & 0xff);
                double blueA = intToDouble(image.getRGB(x, y) & 0xff);
                
                double redNew = redB * alphaB + redA * (1.0 - alphaB);
                double greenNew = greenB * alphaB + greenA * (1.0 - alphaB);
                double blueNew = blueB * alphaB + blueA * (1.0 - alphaB);
                
                int rgbNew = (alphaA << 24) | (doubleToInt(redNew) << 16)
                        | (doubleToInt(greenNew) << 8) | doubleToInt(blueNew);
                
                image.setRGB(x, y, rgbNew);
            }
        }
    }
    
    private static double intToDouble(int i) {
        return Math.max(Math.min(i / 255.0, 1.0), 0.0);
    }
    
    private static int doubleToInt(double d) {
        return Math.max(Math.min((int) Math.round(d * 255.0), 255), 0);
    }
}
