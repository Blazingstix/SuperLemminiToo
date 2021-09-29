package lemmini.game;

import java.text.BreakIterator;
import java.text.Normalizer;
import java.util.*;
import lemmini.graphics.GraphicsContext;
import lemmini.graphics.LemmImage;
import lemmini.tools.Props;
import lemmini.tools.ToolBox;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

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
    private static final Map<Integer, LemmChar> chars = new HashMap<>(512);
    private static final Map<String, Subset> subsets = new HashMap<>(4);
    private static Glyph missingChar;
    private static final List<Glyph> missingCharFont = new ArrayList<>(16);

    /**
     * Initialization.
     * @throws ResourceException
     */
    public static void init() throws ResourceException {
        Resource res = Core.findResource(FONT_INI_STR, true);
        Props p = new Props();
        if (!p.load(res)) {
            throw new ResourceException(FONT_INI_STR);
        }
        
        width = p.getInt("width", 0);
        height = p.getInt("height", 0);
        
        chars.clear();
        subsets.clear();
        missingCharFont.clear();
        
        for (int i = 0; true; i++) {
            String fileName = p.get("subset_" + i + "_fileName", StringUtils.EMPTY);
            int numChars = p.getInt("subset_" + i + "_numChars", 0);
            
            if (fileName.isEmpty() | numChars <= 0) {
                break;
            }
            
            res = Core.findResource("gfx/font/" + fileName, true, Core.IMAGE_EXTENSIONS);
            
            String name = FilenameUtils.removeExtension(fileName);
            
            LemmImage sourceImg = Core.loadLemmImage(res);
            List<LemmImage> glyphImg = ToolBox.getAnimation(sourceImg, numChars, sourceImg.getWidth());
            List<Glyph> glyphs = new ArrayList<>(numChars);
            for (ListIterator<LemmImage> lit = glyphImg.listIterator(); lit.hasNext(); ) {
                int c = lit.nextIndex();
                glyphs.add(new Glyph(lit.next()));
                int codePoint = p.getInt("subset_" + i + "_char_" + c + "_codePoint", -1);
                if (Character.isValidCodePoint(codePoint)) {
                    chars.put(codePoint, new LemmChar(name, c));
                }
            }
            subsets.put(name, new Subset(glyphs));
        }
        
        LemmImage img = ToolBox.createLemmImage(width, height);
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
        
        img = Core.loadLemmImageJar("missing_char_font.png");
        List<LemmImage> missingGlyphFontImg = ToolBox.getAnimation(img, 16);
        missingGlyphFontImg.stream().forEachOrdered(missingGlyphImg -> {
            missingCharFont.add(new Glyph(missingGlyphImg));
        });
    }

    /**
     * Draw string into graphics object in given color.
     * @param g graphics object to draw to.
     * @param s string to draw.
     * @param x X coordinate in pixels
     * @param y Y coordinate in pixels
     * @param color Color
     */
    public static void strImage(final GraphicsContext g, String s, int x, final int y, final Color color) {
        s = Normalizer.normalize(s, Normalizer.Form.NFC);
        
        for (int c, i = 0; i < s.length(); i += Character.charCount(c)) {
            c = s.codePointAt(i);
            
            if (!isLegalChar(c) || Character.isIdentifierIgnorable(c)) {
                // do nothing
            } else if (Character.isSpaceChar(c) || Character.isISOControl(c)) {
                x += width;
            } else {
                drawCharacter(g, c, x, y, color);
                x += width;
            }
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
        LemmImage image = ToolBox.createLemmImage(getCharCount(s) * width, height);
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
                LemmImage hexDigit = missingCharFont.get((c >>> ((digitsPerRow * 2 - 1 - (i * digitsPerRow + j)) * 4)) & 0xF).getColor(color);
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
    public static int getCharCount(String s) {
        s = Normalizer.normalize(s, Normalizer.Form.NFC);
        int charCount = 0;
        for (int c, i = 0; i < s.length(); i += Character.charCount(c)) {
            c = s.codePointAt(i);
            if (isLegalChar(c) && !Character.isIdentifierIgnorable(c)) {
                charCount++;
            }
        }
        return charCount;
    }
    
    /**
     * Split a string into multiple lines at newline characters and, if
     * necessary to prevent the length of a line from exceeding maxLineLength,
     * at legal word-wrapping positions. If word wrapping occurs immediately
     * after a soft hyphen, then the soft hyphen will be converted to a real
     * hyphen.
     * @param s string to split
     * @param maxLineLength maximum number of characters allowed per line; 0 if
     * no word wrapping should be performed
     * @return a list of strings, one string for each line
     */
    public static List<String> split(String s, int maxLineLength) {
        s = Normalizer.normalize(s, Normalizer.Form.NFC);
        
        boolean wordWrap = maxLineLength > 0;
        List<String> sl = new ArrayList<>(4);
        BreakIterator bi = BreakIterator.getLineInstance(Locale.ROOT);
        bi.setText(s);
        int lastBreak = 0;
        int lineLength = 0;
        int i = 0;
        for (int c; i < s.length(); ) {
            c = s.codePointAt(i);
            int type = Character.getType(c);
            boolean breakHere = false;
            int charsToSkip = 0;
            boolean replaceSoftHyphen = false;
            
            // break here if this character is a newline character
            if (type == Character.LINE_SEPARATOR || type == Character.PARAGRAPH_SEPARATOR) {
                breakHere = true;
                charsToSkip = 1;
            } else if (Character.isISOControl(c)) {
                switch (c) {
                    case '\r':
                        breakHere = true;
                        if (i < s.length() - 1 && s.charAt(i + 1) == '\n') {
                            // Windows newline sequence: skip 2 characters rather than 1
                            charsToSkip = 2;
                        } else {
                            charsToSkip = 1;
                        }
                        break;
                    case '\n':
                    case 0x0b:
                    case 0x0c:
                    case 0x85:
                        breakHere = true;
                        charsToSkip = 1;
                        break;
                    default:
                        if (Character.isIdentifierIgnorable(c)) {
                            i += Character.charCount(c);
                            continue;
                        }
                        break;
                }
            } else if (Character.isIdentifierIgnorable(c)) {
                i += Character.charCount(c);
                continue;
            }
            if (!breakHere && wordWrap) {
                // check whether the line should be broken here
                boolean repeatLoop;
                do {
                    repeatLoop = false;
                    if (i > lastBreak && lineLength >= maxLineLength) {
                        if (!bi.isBoundary(i)) {
                            int previous = bi.previous();
                            if (previous != BreakIterator.DONE && previous > lastBreak) {
                                lineLength -= getCharCount(s.substring(previous, i));
                                i = previous;
                            }
                        }
                        if (s.charAt(i - 1) == '\u00ad') {
                            if (lineLength >= maxLineLength) {
                                i--;
                                repeatLoop = true;
                                continue;
                            }
                            replaceSoftHyphen = true;
                        }
                        breakHere = true;
                        charsToSkip = 0;
                    }
                } while (repeatLoop);
            }
            
            if (breakHere) {
                if (replaceSoftHyphen) {
                    // replace soft hyphen with a real hyphen
                    sl.add(s.substring(lastBreak, i - 1) + "-");
                } else {
                    sl.add(s.substring(lastBreak, i));
                }
                for (int j = 0; j < charsToSkip; j++) {
                    i = i + Character.charCount(s.codePointAt(i));
                }
                lastBreak = i;
                lineLength = 0;
            } else {
                i = i + Character.charCount(s.codePointAt(i));
                lineLength++;
            }
        }
        
        if (i > lastBreak) {
            sl.add(s.substring(lastBreak, i));
        }
        return Collections.unmodifiableList(sl);
    }
    
    private static boolean isLegalChar(int c) {
        return Character.isValidCodePoint(c)
                && Character.getType(c) != Character.SURROGATE
                && (c & 0xffff) < 0xfffe
                && (c < 0xfdd0 || c > 0xfdef);
    }
    
    private static class Subset {
        
        List<Glyph> glyphs;
        
        Subset(List<Glyph> glyphs) {
            this.glyphs = glyphs;
        }
        
        Glyph getGlyph(int g) {
            return glyphs.get(g);
        }
    }
    
    private static class Glyph {
        
        private final List<LemmImage> glyphColors;
        
        Glyph(LemmImage glyph) {
            int width = glyph.getWidth();
            int height = glyph.getHeight();
            
            LemmImage[] glyphColorsArray = {
                glyph,
                ToolBox.createLemmImage(width, height),
                ToolBox.createLemmImage(width, height),
                ToolBox.createLemmImage(width, height),
                ToolBox.createLemmImage(width, height),
                ToolBox.createLemmImage(width, height),
            };
            
            for (int xp = 0; xp < width; xp++) {
                for (int yp = 0; yp < height; yp++) {
                    int col = glyph.getRGB(xp, yp); // A R G B
                    int a = col & 0xff000000; // transparent part
                    int r = (col >> 16) & 0xff;
                    int g = (col >> 8) & 0xff;
                    int b = col & 0xff;
                    // patch image to blue version by swapping blue and green components
                    col = a | (r << 16) | (b << 8) | g;
                    glyphColorsArray[1].setRGB(xp, yp, col);
                    // patch image to red version by swapping red and green components
                    col = a | (g << 16) | (r << 8) | b;
                    glyphColorsArray[2].setRGB(xp, yp, col);
                    // patch image to brown version by setting red component to value of green component
                    col = a | (g << 16) | (g << 8) | b;
                    glyphColorsArray[3].setRGB(xp, yp, col);
                    // patch image to turquoise version by setting blue component to value of green component
                    col = a | (r << 16) | (g << 8) | g;
                    glyphColorsArray[4].setRGB(xp, yp, col);
                    // patch image to violet version by exchanging red and blue with green
                    col = a | (g << 16) | (((r + b) << 7) & 0xff00) | g;
                    glyphColorsArray[5].setRGB(xp, yp, col);
                }
            }
            
            glyphColors = Arrays.asList(glyphColorsArray);
        }
        
        LemmImage getColor(Color color) {
            return glyphColors.get(color.ordinal());
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
