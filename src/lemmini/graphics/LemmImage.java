package lemmini.graphics;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Hashtable;
import lemmini.tools.ToolBox;


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
        BufferedImage oldBufferedImage = image.getImage();
        this.image = new BufferedImage(oldBufferedImage.getColorModel(),
                oldBufferedImage.copyData(null),
                oldBufferedImage.isAlphaPremultiplied(),
                image.getProperties());
    }
    
    public BufferedImage getImage() {
        return image;
    }
    
    public Hashtable<String, Object> getProperties() {
        String[] propertyNames = image.getPropertyNames();
        if (propertyNames == null) {
            return null;
        } else {
            Hashtable<String, Object> properties = new Hashtable<>(propertyNames.length);
            for (String propertyName : propertyNames) {
                properties.put(propertyName, image.getProperty(propertyName));
            }
            return properties;
        }
    }
    
    public LemmImage getScaledInstance(int targetWidth, int targetHeight,
            Object interpolationHint, boolean multipass) {
        int transparency = image.getTransparency();
        BufferedImage img = image;
        int w;
        int h;
        if (multipass) {
            w = getWidth();
            h = getHeight();
        } else {
            w = targetWidth;
            h = targetHeight;
        }
        
        do {
            if (multipass) {
                w = Math.max(w / 2, targetWidth);
                h = Math.max(h / 2, targetHeight);
            }
            
            BufferedImage tmp = ToolBox.createImage(w, h, transparency);
            Graphics2D g2 = null;
            try {
                g2 = tmp.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolationHint);
                g2.drawImage(img, 0, 0, w, h, null);
            } finally {
                if (g2 != null) {
                    g2.dispose();
                }
            }
            img = tmp;
        } while (w != targetWidth || h != targetHeight);
        
        return new LemmImage(img);
    }
    
    public void flip(boolean horizontal, boolean vertical) {
        int width = image.getWidth();
        int height = image.getHeight();
        if (horizontal && !vertical) {
            for (int y = 0; y < height; y++) {
                for (int x = 0, x2 = width - 1; x < x2; x++, x2--) {
                    int c = image.getRGB(x, y);
                    image.setRGB(x, y, image.getRGB(x2, y));
                    image.setRGB(x2, y, c);
                }
            }
        } else if (!horizontal && vertical) {
            for (int y = 0, y2 = height - 1; y < y2; y++, y2--) {
                for (int x = 0; x < width; x++) {
                    int c = image.getRGB(x, y);
                    image.setRGB(x, y, image.getRGB(x, y2));
                    image.setRGB(x, y2, c);
                }
            }
        } else if (horizontal && vertical) {
            int maxY = (height / 2) + (height % 2) - 1;
            for (int y = 0, y2 = height - 1; y <= maxY; y++, y2--) {
                int maxX = ((y == y2) ? (width / 2) : width) - 1;
                for (int x = 0, x2 = width - 1; x <= maxX; x++, x2--) {
                    int c = image.getRGB(x, y);
                    image.setRGB(x, y, image.getRGB(x2, y2));
                    image.setRGB(x2, y2, c);
                }
            }
        }
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
    
    public void replaceColor(int oldRGB, int newRGB) {
        int w = getWidth();
        int h = getHeight();
        for (int xp = 0; xp < w; xp++) {
            for (int yp = 0; yp < h; yp++) {
                int rgb = getRGB(xp, yp);
                if ((rgb & 0x00ffffff) == oldRGB) {
                    setRGB(xp, yp, (newRGB & 0x00ffffff) | (rgb & 0xff000000));
                }
            }
        }
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
    
    public boolean isPixelOpaque(int x, int y) {
        return (getRGB(x, y) >>> 24) >= 0x80;
    }
    
    public void applyTint(int tint) {
        final double alphaTint = intToDouble((tint >>> 24) & 0xff);
        final double redTint = intToDouble((tint >>> 16) & 0xff);
        final double greenTint = intToDouble((tint >>> 8) & 0xff);
        final double blueTint = intToDouble(tint & 0xff);
        if (alphaTint <= 0.0) {
            return;
        }
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, applyTint(image.getRGB(x, y), alphaTint, redTint, greenTint, blueTint));
            }
        }
    }
    
    public static int applyTint(int original, int tint) {
        final double alphaTint = intToDouble((tint >>> 24) & 0xff);
        final double redTint = intToDouble((tint >>> 16) & 0xff);
        final double greenTint = intToDouble((tint >>> 8) & 0xff);
        final double blueTint = intToDouble(tint & 0xff);

        return applyTint(original, alphaTint, redTint, greenTint, blueTint);
    }
    
    private static int applyTint(int original, double alphaTint, double redTint, double greenTint, double blueTint) {
        if (alphaTint <= 0.0) {
            return original;
        }
        
        int alphaOrig = (original >>> 24) & 0xff;
        if (alphaOrig <= 0) {
            return original;
        }
        double redOrig = intToDouble((original >>> 16) & 0xff);
        double greenOrig = intToDouble((original >>> 8) & 0xff);
        double blueOrig = intToDouble(original & 0xff);

        double redNew = redTint * alphaTint + redOrig * (1.0 - alphaTint);
        double greenNew = greenTint * alphaTint + greenOrig * (1.0 - alphaTint);
        double blueNew = blueTint * alphaTint + blueOrig * (1.0 - alphaTint);

        int rgbNew = (alphaOrig << 24) | (doubleToInt(redNew) << 16)
                | (doubleToInt(greenNew) << 8) | doubleToInt(blueNew);

        return rgbNew;
    }
    
    private static double intToDouble(int i) {
        return ToolBox.cap(0.0, i / 255.0, 1.0);
    }
    
    private static int doubleToInt(double d) {
        return ToolBox.cap(0, ToolBox.roundToInt(d * 255.0), 255);
    }
}
