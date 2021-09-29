package lemmini.game;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import lemmini.graphics.GraphicsContext;
import lemmini.graphics.LemmImage;
import lemmini.tools.Props;
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
 * Handle the main bitmap font.
 *
 * @author Volker Oth
 */
public class LemmFont {

    /** Colors */
    public static enum Color {
        /** green color */
        GREEN,
        /** blue color */
        BLUE,
        /** red color */
        RED,
        /** brown/yellow color */
        BROWN,
        /** turquoise/cyan color */
        TURQUOISE,
        /** violet color */
        VIOLET
    }
    
    private static final String FONT_INI_STR = "gfx/font/font.ini";

    /** width of one character in pixels */
    private static int width;
    /** height of one character in pixels */
    private static int height;
    private static Map<Integer, LemmChar> chars;
    private static Map<String, Subset> subsets;
    private static Glyph missingChar;
    private static Glyph[] missingCharFont;

    /**
     * Initialization.
     * @throws ResourceException
     */
    public static void init() throws ResourceException {
        Path fn = Core.findResource(Paths.get(FONT_INI_STR));
        Props p = new Props();
        if (!p.load(fn)) {
            throw new ResourceException(FONT_INI_STR);
        }
        
        width = p.getInt("width", 0);
        height = p.getInt("height", 0);
        
        if (chars == null) {
            chars = new HashMap<>(512);
        } else {
            chars.clear();
        }
        
        if (subsets == null) {
            subsets = new HashMap<>(4);
        } else {
            subsets.clear();
        }
        
        for (int i = 0; true; i++) {
            String fileNameStr = p.get("subset_" + i + "_fileName", "");
            Path fileName = Paths.get(fileNameStr);
            int numChars = p.getInt("subset_" + i + "_numChars", 0);
            
            if (fileNameStr.isEmpty() | numChars <= 0) {
                break;
            }
            
            fn = Core.findResource(Paths.get("gfx/font").resolve(fileName), Core.IMAGE_EXTENSIONS);
            
            String name = ToolBox.removeExtension(fileNameStr);
            
            LemmImage sourceImg = Core.loadTranslucentImage(fn);
            LemmImage[] glyphImg = ToolBox.getAnimation(sourceImg, numChars, sourceImg.getWidth());
            Glyph[] glyphs = new Glyph[numChars];
            for (int c = 0; c < numChars; c++) {
                glyphs[c] = new Glyph(glyphImg[c]);
                int codePoint = p.getInt("subset_" + i + "_char_" + c + "_codePoint", -1);
                if (Character.isValidCodePoint(codePoint)) {
                    chars.put(codePoint, new LemmChar(name, c));
                }
            }
            subsets.put(name, new Subset(glyphs));
        }
        
        LemmImage img = ToolBox.createTranslucentImage(width, height);
        GraphicsContext g = null;
        try {
            g = img.createGraphicsContext();
            g.setColor(java.awt.Color.GREEN);
            g.drawRect(1, 1, width - 3, height - 3);
        } finally {
            if (g != null) {
                g.dispose();
            }
        }
        missingChar = new Glyph(img);
        
        img = Core.loadTranslucentImageJar("missing_char_font.png");
        LemmImage[] missingGlyphFontImg = ToolBox.getAnimation(img, 16);
        missingCharFont = new Glyph[missingGlyphFontImg.length];
        for (int i = 0; i < missingGlyphFontImg.length; i++) {
            missingCharFont[i] = new Glyph(missingGlyphFontImg[i]);
        }
    }

    /**
     * Draw string into graphics object in given color.
     * @param g graphics object to draw to.
     * @param s string to draw.
     * @param sx X coordinate in pixels
     * @param sy Y coordinate in pixels
     * @param color Color
     */
    public static void strImage(final GraphicsContext g, String s, int x, final int y, final Color color) {
        s = Normalizer.normalize(s, Normalizer.Form.NFC);
        
        for (int c, i = 0; i < s.length(); i += Character.charCount(c)) {
            c = s.codePointAt(i);
            
            if (!Character.isDefined(c)) {
                drawMissingChar(g, c, x, y, color);
                x += width;
                continue;
            }
            
            if (Character.isIdentifierIgnorable(c)) {
                continue;
            }
            
            if (Character.isSpaceChar(c) || Character.isISOControl(c)) {
                x += width;
                continue;
            }
            
            drawCharacter(g, c, x, y, color);
            x += width;
        }
    }

    /**
     * Draw string into graphics object in given color.
     * @param g graphics object to draw to.
     * @param s string to draw.
     * @param color Color
     */
    public static void strImage(final GraphicsContext g, final String s, final Color color) {
        strImage(g, s, 0, 0, color);
    }

    /**
     * Create image of string in given color.
     * @param s string to draw
     * @param color Color
     * @return a buffered image of the needed size that contains an image of the given string
     */
    public static LemmImage strImage(final String s, final Color color) {
        LemmImage image = ToolBox.createTranslucentImage(getCharCount(s) * width, height);
        GraphicsContext g = image.createGraphicsContext();
        try {
            g = image.createGraphicsContext();
            strImage(g, s, 0, 0, color);
        } finally {
            if (g != null) {
                g.dispose();
            }
        }
        return image;
    }

    /**
     * Create image of string in default color (green).
     * @param s string to draw
     * @return a buffered image of the needed size that contains an image of the given string
     */
    public static LemmImage strImage(final String s) {
        return strImage(s, Color.GREEN);
    }

    /**
     * Draw string into graphics object in default color (green).
     * @param g graphics object to draw to.
     * @param s string to draw.
     */
    public static void strImage(final GraphicsContext g, final String s) {
        strImage(g, s, 0, 0, Color.GREEN);
    }
    
    private static void drawCharacter(GraphicsContext g, int c, int x, int y, Color color) {
        if (chars.containsKey(c)) {
            LemmChar lemmChar = chars.get(c);
            g.drawImage(subsets.get(lemmChar.subset).getGlyph(lemmChar.glyphIndex).getColor(color), x, y);
        } else {
            drawMissingChar(g, c, x, y, color);
        }
    }
    
    private static void drawMissingChar(GraphicsContext g, int c, int x, int y, Color color) {
        g.drawImage(missingChar.getColor(color), x, y);
        boolean bmpCodePoint = Character.isBmpCodePoint(c);
        int digitsPerRow = (bmpCodePoint ? 2 : 3);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < digitsPerRow; j++) {
                LemmImage hexDigit = missingCharFont[(c >>> ((digitsPerRow * 2 - 1 - (i * digitsPerRow + j)) * 4)) & 0xF].getColor(color);
                g.drawImage(hexDigit,
                        x + (width / 2 + (j - 1) * hexDigit.getWidth()) - (bmpCodePoint ? 0 : hexDigit.getWidth() / 2),
                        y + (height / 2 + (i - 1) * hexDigit.getHeight()));
            }
        }
    }

    /**
     * Get the width of one character in pixels.
     * @return width of one character in pixels
     */
    public static int getWidth() {
        return width;
    }

    /**
     * Get the height of one character in pixels.
     * @return height of one character in pixels
     */
    public static int getHeight() {
        return height;
    }
    
    /**
     * Get the number of displayable characters in the given string.
     * @param s string
     * @return number of displayable characters
     */
    public static int getCharCount(final String s) {
        int charCount = 0;
        for (int c, i = 0; i < s.length(); i += Character.charCount(c)) {
            c = s.codePointAt(i);
            if (!Character.isIdentifierIgnorable(s.codePointAt(i))) {
                charCount++;
            }
        }
        return charCount;
    }
    
    private static class Subset {
        
        Glyph[] glyphs;
        
        Subset(Glyph[] glyphs) {
            this.glyphs = glyphs;
        }
        
        Glyph getGlyph(int g) {
            return glyphs[g];
        }
    }
    
    private static class Glyph {
        
        private final LemmImage[] glyphColors;
        
        Glyph(LemmImage glyph) {
            glyphColors = new LemmImage[6];
            
            glyphColors[0] = glyph;
            
            int width = glyph.getWidth();
            int height = glyph.getHeight();
            
            glyphColors[1] = ToolBox.createTranslucentImage(width, height);
            glyphColors[2] = ToolBox.createTranslucentImage(width, height);
            glyphColors[3] = ToolBox.createTranslucentImage(width, height);
            glyphColors[4] = ToolBox.createTranslucentImage(width, height);
            glyphColors[5] = ToolBox.createTranslucentImage(width, height);
            
            for (int xp = 0; xp < width; xp++) {
                for (int yp = 0; yp < height; yp++) {
                    int col = glyph.getRGB(xp, yp); // A R G B
                    int a = col & 0xff000000; // transparent part
                    int r = (col >> 16) & 0xff;
                    int g = (col >> 8) & 0xff;
                    int b = col & 0xff;
                    // patch image to blue version by swapping blue and green components
                    col = a | (r << 16) | (b << 8) | g;
                    glyphColors[1].setRGB(xp, yp, col);
                    // patch image to red version by swapping red and green components
                    col = a | (g << 16) | (r << 8) | b;
                    glyphColors[2].setRGB(xp, yp, col);
                    // patch image to brown version by setting red component to value of green component
                    col = a | (g << 16) | (g << 8) | b;
                    glyphColors[3].setRGB(xp, yp, col);
                    // patch image to turquoise version by setting blue component to value of green component
                    col = a | (r << 16) | (g << 8) | g;
                    glyphColors[4].setRGB(xp, yp, col);
                    // patch image to violet version by exchanging red and blue with green
                    col = a | (g << 16) | (((r + b) << 7) & 0xff00) | g;
                    glyphColors[5].setRGB(xp, yp, col);
                }
            }
        }
        
        LemmImage getColor(Color color) {
            return glyphColors[color.ordinal()];
        }
    }
    
    private static class LemmChar {
        
        String subset;
        int glyphIndex;
        
        LemmChar(String subset, int glyphIndex) {
            this.subset = subset;
            this.glyphIndex = glyphIndex;
        }
    }
}
