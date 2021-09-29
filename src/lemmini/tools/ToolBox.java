package lemmini.tools;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import lemmini.graphics.GraphicsContext;
import lemmini.graphics.GraphicsOperation;
import lemmini.graphics.LemmImage;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
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
 * Selection of utility functions.
 *
 * @author Volker Oth
 */
public class ToolBox {
    
    private static final ByteOrderMark[] BYTE_ORDER_MARKS = {
        ByteOrderMark.UTF_32BE, ByteOrderMark.UTF_32LE, ByteOrderMark.UTF_8,
        ByteOrderMark.UTF_16BE, ByteOrderMark.UTF_16LE};
    //private static final GraphicsConfiguration GC = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
    
    /**
     * Creates a graphics operation
     * @return the graphics operation
     */
    public static GraphicsOperation createGraphicsOperation() {
        return new GraphicsOperation();
    }
    
    /**
     * Creates a custom cursor from the image
     * @param image
     * @param centerX
     * @param centerY
     * @return the cursor
     */
    public static Cursor createCursor(LemmImage image, int centerX, int centerY) {
        return Toolkit.getDefaultToolkit().createCustomCursor(image.getImage(), new Point(centerX, centerY), StringUtils.EMPTY);
    }
    
    /**
     * Create a compatible buffered image.
     * @param width width of image in pixels
     * @param height height of image in pixels
     * @param transparency {@link java.awt.Transparency}
     * @return compatible buffered image
     */
    public static BufferedImage createImage(final int width, final int height, final int transparency) {
        //return GC.createCompatibleImage(width, height, transparency);
        return new BufferedImage(width, height, transparency == Transparency.OPAQUE ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
    }

    /**
     * Create a compatible buffered image.
     * @param width width of image in pixels
     * @param height height of image in pixels
     * @return compatible buffered image
     */
    public static LemmImage createBitmaskImage(final int width, final int height) {
        return new LemmImage(createImage(width, height, Transparency.BITMASK));
    }

    /**
     * Create a compatible buffered image.
     * @param width width of image in pixels
     * @param height height of image in pixels
     * @return compatible buffered image
     */
    public static LemmImage createOpaqueImage(final int width, final int height) {
        return new LemmImage(createImage(width, height, Transparency.OPAQUE));
    }

    /**
     * Create a compatible buffered image.
     * @param width width of image in pixels
     * @param height height of image in pixels
     * @return compatible buffered image
     */
    public static LemmImage createTranslucentImage(final int width, final int height) {
        return new LemmImage(createImage(width, height, Transparency.TRANSLUCENT));
    }

    /**
     * Create a compatible buffered image from an image.
     * @param img existing {@link java.awt.Image}
     * @param transparency {@link java.awt.Transparency}
     * @return compatible buffered image
     */
    public static LemmImage imageToBuffered(final Image img, final int transparency) {
        BufferedImage bImg = createImage(img.getWidth(null), img.getHeight(null), transparency);
        Graphics2D g = null;
        try {
            g = bImg.createGraphics();
            g.drawImage(img, 0, 0, null);
        } finally {
            if (g != null) {
                g.dispose();
            }
        }
        return new LemmImage(bImg);
    }

    /**
     * Return a list of buffered images which contain an animation.
     * @param img image containing all the frames one above each other
     * @param frames number of frames
     * @return a list of images which contains an animation
     */
    public static java.util.List<LemmImage> getAnimation(final LemmImage img, final int frames) {
        return getAnimation(img, frames, img.getImage().getColorModel().getTransparency(), img.getWidth());
    }

    /**
     * Return a list of buffered images which contain an animation.
     * @param img image containing all the frames one above each other
     * @param frames number of frames
     * @param width image width
     * @return a list of images which contains an animation
     */
    public static java.util.List<LemmImage> getAnimation(final LemmImage img, final int frames, final int width) {
        return getAnimation(img, frames, img.getImage().getColorModel().getTransparency(), width);
    }

    /**
     * Return a list of buffered images which contain an animation.
     * @param img image containing all the frames one above each other
     * @param frames number of frames
     * @param transparency {@link java.awt.Transparency}
     * @param width image width
     * @return a list of images which contains an animation
     */
    public static java.util.List<LemmImage> getAnimation(final LemmImage img, final int frames, final int transparency, final int width) {
        int height = img.getHeight() / frames;
        // characters stored one above the other - now separate them into single images
        java.util.List<LemmImage> imgList = new ArrayList<>(frames);
        int y0 = 0;
        for (int i = 0; i < frames; i++, y0 += height) {
            LemmImage frame = new LemmImage(createImage(width, height, transparency));
            GraphicsContext g = null;
            try {
                g = frame.createGraphicsContext();
                g.drawImage(img, 0, 0, width, height, 0, y0, width, y0 + height);
            } finally {
                if (g != null) {
                    g.dispose();
                }
            }
            imgList.add(frame);
        }
        return imgList;
    }

    /**
     * Use the Loader to find a file.
     * @param fname file name
     * @return URL of the file
     */
    public static URL findFile(final String fname) {
        ClassLoader loader = ToolBox.class.getClassLoader();
        return loader.getResource(fname);
    }

    /**
     * Open file dialog.
     * @param parent parent frame
     * @param path default file name
     * @param load true: load, false: save
     * @param allowDirectories allow directories to be selected if true
     * @param ext allowed extensions
     * @return absolute file name of selected file or null
     */
    public static Path getFileName(final Component parent, final Path path,
            final boolean load, final boolean allowDirectories,
            final String... ext) {
        JFileChooser jf = new JFileChooser(path.toFile());
        if (ext != null) {
            StringBuilder sb = new StringBuilder(32);
            for (int i = 0; i < ext.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append("*.").append(ext[i]);
            }
            FileNameExtensionFilter filter = new FileNameExtensionFilter(sb.toString(), ext);
            jf.setFileFilter(filter);
        }
        jf.setFileSelectionMode(allowDirectories ? JFileChooser.FILES_AND_DIRECTORIES : JFileChooser.FILES_ONLY);
        if (!load) {
            jf.setDialogType(JFileChooser.SAVE_DIALOG);
        }
        int returnVal = jf.showDialog(parent, null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File f = jf.getSelectedFile();
            if (f != null) {
                return f.toPath().toAbsolutePath();
            }
        }
        return null;
    }
    
    /**
     * Open file dialog.
     * @param parent parent frame
     * @param path default file name
     * @param load true: load, false: save
     * @param allowDirectories allow directories to be selected if true
     * @param ext allowed extensions
     * @return absolute file names of selected files or null
     */
    public static java.util.List<Path> getFileNames(final Component parent, final Path path,
            final boolean load, final boolean allowDirectories,
            final String... ext) {
        JFileChooser jf = new JFileChooser(path.toFile());
        if (ext != null) {
            StringBuilder sb = new StringBuilder(32);
            for (int i = 0; i < ext.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append("*.").append(ext[i]);
            }
            FileNameExtensionFilter filter = new FileNameExtensionFilter(sb.toString(), ext);
            jf.setFileFilter(filter);
        }
        jf.setFileSelectionMode(allowDirectories ? JFileChooser.FILES_AND_DIRECTORIES : JFileChooser.FILES_ONLY);
        jf.setMultiSelectionEnabled(true);
        if (!load) {
            jf.setDialogType(JFileChooser.SAVE_DIALOG);
        }
        int returnVal = jf.showDialog(parent, null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            return Arrays.stream(jf.getSelectedFiles())
                    .map(f -> f.toPath().toAbsolutePath())
                    .collect(Collectors.toList());
        }
        return null;
    }
     
    /**
     * Checks whether the first few characters of the given file matches the given header.
     * @param r Reader for the file
     * @param header 
     * @return True if file begins with the given header, false otherwise
     */
    public static boolean checkFileID(final Reader r, final String header) {
        try {
            char[] buf = new char[header.length()];
            r.mark(header.length());
            int charsRead = r.read(buf);
            r.reset();
            String s = new String(buf, 0, charsRead);
            return s.equals(header);
        } catch (IOException ex) {
            return false;
        }
    }
    
    public static BufferedReader getBufferedReader(final Path fname) throws IOException {
        InputStream in = null;
        try {
            in = Files.newInputStream(fname);
            return getBufferedReader(in);
        } catch (IOException | SecurityException | UnsupportedCharsetException ex) {
            if (in != null) {
                in.close();
            }
            throw ex;
        }
    }
    
    public static BufferedReader getBufferedReader(final URL file) throws IOException {
        InputStream in = null;
        try {
            in = file.openStream();
            return getBufferedReader(in);
        } catch (IOException | SecurityException | UnsupportedCharsetException ex) {
            if (in != null) {
                in.close();
            }
            throw ex;
        }
    }
    
    public static BufferedReader getBufferedReader(final InputStream in) throws IOException {
        BOMInputStream in2 = new BOMInputStream(in, BYTE_ORDER_MARKS);
        Charset encoding;
        if (in2.hasBOM()) {
            encoding = Charset.forName(in2.getBOMCharsetName());
        } else {
            encoding = StandardCharsets.UTF_8;
        }
        
        BufferedReader r = new BufferedReader(new InputStreamReader(in2, encoding));
        return r;
    }
    
    public static String addBackslashes(String s, final boolean addBackslashesToAllSpaces) {
        s = s.replace("\\", "\\\\");
        s = s.replace("#", "\\#");
        s = s.replace("=", "\\=");
        s = s.replace(":", "\\:");
        s = s.replace("!", "\\!");
        if (addBackslashesToAllSpaces) {
            s = s.replace(" ", "\\ ");
        } else {
            if (!s.isEmpty() && s.charAt(0) == ' ') {
                s = "\\" + s;
            }
        }
        
        return s;
    }

    /**
     * Show exception message box.
     * @param ex exception
     */
    public static void showException(final Throwable ex) {
        String m = "<html>" + ex.getClass().getName() + "<p>";
        if (ex.getMessage() != null) {
            m += ex.getMessage() + "<p>";
        }
        StackTraceElement[] ste = ex.getStackTrace();
        for (StackTraceElement ste1 : ste) {
            m += ste1.toString() + "<p>";
        }
        m += "</html>";
        ex.printStackTrace();
        JOptionPane.showMessageDialog(null, m, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    public static int cap(int min, int value, int max) {
        if (min > max) {
            throw new IllegalArgumentException("max must be >= min");
        }
        return Math.max(min, Math.min(value, max));
    }
    
    public static long cap(long min, long value, long max) {
        if (min > max) {
            throw new IllegalArgumentException("max must be >= min");
        }
        return Math.max(min, Math.min(value, max));
    }
    
    public static float cap(float min, float value, float max) {
        if (min > max) {
            throw new IllegalArgumentException("max must be >= min");
        }
        return Math.max(min, Math.min(value, max));
    }
    
    public static double cap(double min, double value, double max) {
        if (min > max) {
            throw new IllegalArgumentException("max must be >= min");
        }
        return Math.max(min, Math.min(value, max));
    }
    
    public static int roundToInt(double a) {
        return (int) cap(Integer.MIN_VALUE, Math.round(a), Integer.MAX_VALUE);
    }
    
    public static int scale(int n, double s) {
        return (int) roundToInt(n * s);
    }
    
    public static double scale(double n, double s) {
        return n * s;
    }
    
    public static int unscale(int n, double s) {
        return (int) roundToInt(n / s);
    }
    
    public static double unscale(double n, double s) {
        return n / s;
    }

    /**
     * Parse decimal, hex, binary or octal number as int
     * @param s String that contains one number
     * @return Integer value of string
     */
    public static int parseInt(final String s) {
        if (s.equalsIgnoreCase("Infinity") || s.equalsIgnoreCase("+Infinity")) {
            return Integer.MAX_VALUE;
        } else if (s.equalsIgnoreCase("-Infinity")) {
            return Integer.MIN_VALUE;
        }
        
        int index = 0;
        boolean hasSign = isSign(s.charAt(index));
        if (hasSign) {
            index++;
        }
        
        if (s.charAt(index) == '0') {
            index++;
            if (s.length() <= index) {
                return 0;
            }
            if (s.charAt(0) == '-') {
                throw new NumberFormatException(String.format("Illegal leading minus sign on unsigned string %s.", s));
            }
            int radix;
            switch (s.charAt(index)) {
                case 'X':
                case 'x':
                    // hex
                    radix = 16;
                    index++;
                    break;
                case 'B':
                case 'b':
                    // binary
                    radix = 2;
                    index++;
                    break;
                default:
                    // octal
                    radix = 8;
                    break;
            }
            
            if (isSign(s.charAt(index))) {
                throw new NumberFormatException("Sign character is not permitted after the radix prefix.");
            }
            
            return Integer.parseUnsignedInt((hasSign ? s.substring(0, 1) : StringUtils.EMPTY) + s.substring(index), radix);
        } else {
            // decimal
            return Integer.parseInt(s);
        }
    }
    
    /**
     * Parse decimal, hex, binary or octal number as long
     * @param s String that contains one number
     * @return Long value of string
     */
    public static long parseLong(final String s) {
        if (s.equalsIgnoreCase("Infinity") || s.equalsIgnoreCase("+Infinity")) {
            return Long.MAX_VALUE;
        } else if (s.equalsIgnoreCase("-Infinity")) {
            return Long.MIN_VALUE;
        }
        
        int index = 0;
        boolean hasSign = isSign(s.charAt(index));
        if (hasSign) {
            index++;
        }
        
        if (s.charAt(index) == '0') {
            index++;
            if (s.length() <= index) {
                return 0;
            }
            if (s.charAt(0) == '-') {
                throw new NumberFormatException(String.format("Illegal leading minus sign on unsigned string %s.", s));
            }
            int radix;
            switch (s.charAt(index)) {
                case 'X':
                case 'x':
                    // hex
                    radix = 16;
                    index++;
                    break;
                case 'B':
                case 'b':
                    // binary
                    radix = 2;
                    index++;
                    break;
                default:
                    // octal
                    radix = 8;
                    break;
            }
            
            if (isSign(s.charAt(index))) {
                throw new NumberFormatException("Sign character is not permitted after the radix prefix.");
            }
            
            return Long.parseUnsignedLong((hasSign ? s.substring(0, 1) : StringUtils.EMPTY) + s.substring(index), radix);
        } else {
            // decimal
            return Long.parseLong(s);
        }
    }
    
    public static String intToString(int number, boolean useInfinitySymbol) {
        // \u221e = infinity symbol
        switch (number) {
            case Integer.MAX_VALUE:
                return useInfinitySymbol ? "\u221e" : "Infinity";
            case Integer.MIN_VALUE:
                return useInfinitySymbol ? "-\u221e" : "-Infinity";
            default:
                return Integer.toString(number);
        }
    }
    
    /**
     * Checks whether the given character is a sign.
     * @param c Character to check
     * @return True if c is an ASCII plus or minus sign, false otherwise
     */
    public static boolean isSign(char c) {
        return c == '+' || c == '-';
    }
    
    public static boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f');
    }
    
    /**
     * Checks whether two strings are equal after trimming, converting to
     * lowercase, and applying NFKC normalization.
     * @param s1 first string to check
     * @param s2 second string to check
     * @return True if s1 and s2 are equal after conversion, false otherwise
     */
    public static boolean looselyEquals(String s1, String s2) {
        return Normalizer.normalize(s1.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFKC)
                .equals(Normalizer.normalize(s2.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFKC));
    }
    
    public static void deleteFileTree(Path path) throws IOException {
        Files.walkFileTree(path, new DeleteTreeFileVisitor());
    }
}

class DeleteTreeFileVisitor extends SimpleFileVisitor<Path> {
    
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        if (exc == null) {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        } else {
            throw exc;
        }
    }
}
