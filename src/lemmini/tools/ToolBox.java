package lemmini.tools;


import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import lemmini.graphics.GraphicsContext;
import lemmini.graphics.GraphicsOperation;
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
 * Selection of utility functions.
 *
 * @author Volker Oth
 */
public class ToolBox {
    
    private static final GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
    
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
     * @param width
     * @param height
     * @return the cursor
     */
    public static Cursor createCursor(Image image, int width, int height) {
        return Toolkit.getDefaultToolkit().createCustomCursor(image.getImage(), new Point(width, height), "");
    }
    
    /**
     * Create a compatible buffered image.
     * @param width width of image in pixels
     * @param height height of image in pixels
     * @param transparency {@link java.awt.Transparency}
     * @return compatible buffered image
     */
    public static BufferedImage createImage(final int width, final int height, final int transparency) {
        BufferedImage b = gc.createCompatibleImage(width, height, transparency);
        return b;
    }

    /**
     * Create a compatible buffered image.
     * @param width width of image in pixels
     * @param height height of image in pixels
     * @return compatible buffered image
     */
    public static Image createBitmaskImage(final int width, final int height) {
        return new Image(createImage(width, height, Transparency.BITMASK));
    }

    /**
     * Create a compatible buffered image.
     * @param width width of image in pixels
     * @param height height of image in pixels
     * @return compatible buffered image
     */
    public static Image createOpaqueImage(final int width, final int height) {
        return new Image(createImage(width, height, Transparency.OPAQUE));
    }

    /**
     * Create a compatible buffered image.
     * @param width width of image in pixels
     * @param height height of image in pixels
     * @return compatible buffered image
     */
    public static Image createTranslucentImage(final int width, final int height) {
        return new Image(createImage(width, height, Transparency.TRANSLUCENT));
    }

    /**
     * Create a compatible buffered image from an image.
     * @param img existing {@link java.awt.Image}
     * @param transparency {@link java.awt.Transparency}
     * @return compatible buffered image
     */
    public static Image imageToBuffered(final java.awt.Image img, final int transparency) {
        BufferedImage bImg = createImage(img.getWidth(null), img.getHeight(null), transparency);
        Graphics2D g = bImg.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return new Image(bImg);
    }

    /**
     * Return an array of buffered images which contain an animation.
     * @param img image containing all the frames one above each other
     * @param frames number of frames
     * @return an array of buffered images which contain an animation
     */
    public static Image[] getAnimation(final Image img, final int frames) {
        return getAnimation(img, frames, img.getWidth());
    }

    /**
     * Return an array of buffered images which contain an animation.
     * @param img image containing all the frames one above each other
     * @param frames number of frames
     * @param width image width
     * @return an array of buffered images which contain an animation
     */
    public static Image[] getAnimation(final Image img, final int frames, final int width) {
        return getAnimation(img, frames, img.getImage().getColorModel().getTransparency(), width);
    }

    /**
     * Return an array of buffered images which contain an animation.
     * @param img image containing all the frames one above each other
     * @param frames number of frames
     * @param transparency {@link java.awt.Transparency}
     * @param width image width
     * @return an array of buffered images which contain an animation
     */
    public static Image[] getAnimation(final Image img, final int frames, final int transparency, final int width) {
        int height = img.getHeight() / frames;
        // characters stored one above the other - now separate them into single images
        Image[] arrImg = new Image[frames];
        int y0 = 0;
        for (int i = 0; i < frames; i++, y0 += height) {
            Image frame = new Image(createImage(width, height, transparency));
            GraphicsContext g = frame.createGraphicsContext();
            g.drawImage(img, 0, 0, width, height, 0, y0, width, y0 + height);
            arrImg[i] = frame;
            g.dispose();
        }
        return arrImg;
    }

    /**
     * Flip image in X direction.
     * @param source image to flip
     * @return flipped image
     */
    public static Image flipImageX(final Image source) {
        Image target = createBitmaskImage(source.getWidth(), source.getHeight());
        GraphicsOperation operation = createGraphicsOperation();
        operation.setScale(-1, 1);
        operation.translate(-source.getWidth(), 0);
        operation.execute(source, target);
        return target;
    }

    /**
     * Flip image in X direction and preserve translucency.
     * @param source image to flip
     * @return flipped image
     */
    public static Image flipImageXTranslucent(final Image source) {
        Image target = createTranslucentImage(source.getWidth(), source.getHeight());
        GraphicsOperation operation = createGraphicsOperation();
        operation.setScale(-1, 1);
        operation.translate(-source.getWidth(), 0);
        operation.execute(source, target);
        return target;
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
     * @param ext array of allowed extensions
     * @param load true: load, false: save
     * @param allowDirectories allow directories to be selected if true
     * @return absolute file name of selected file or null
     */
    public static Path getFileName(final Component parent, final Path path, final String[] ext,
            final boolean load, final boolean allowDirectories) {
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
     * @param ext array of allowed extensions
     * @param load true: load, false: save
     * @param allowDirectories allow directories to be selected if true
     * @return absolute file names of selected files or null
     */
    public static Path[] getFileNames(final Component parent, final Path path, final String[] ext,
            final boolean load, final boolean allowDirectories) {
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
            File[] f = jf.getSelectedFiles();
            Path[] ret = new Path[f.length];
            for (int i = 0; i < f.length; i++) {
                ret[i] = f[i].toPath().toAbsolutePath();
            }
            return ret;
        }
        return null;
    }

    /**
     * Returns the extension (".XXX") of a filename without the dot.
     * @param path String containing file name
     * @return String containing only the extension (without the dot) or null (if no extension found)
     */
    public static String getExtension(final String path) {
        int p1 = path.lastIndexOf("/");
        int p2 = path.lastIndexOf("\\");
        int p = path.lastIndexOf(".");
        if (p == -1 || p < p1 || p < p2) {
            return "";
        }
        return path.substring(p + 1);
    }
    
    /**
     * Returns the file name without the extension (".XXX").
     * @param path String containing file name
     * @return String containing file name without the extension
     */
    public static String removeExtension(String fname) {
        int slashIndex = Math.max(fname.lastIndexOf("/"), fname.lastIndexOf("\\"));
        int dotIndex = fname.lastIndexOf(".");
        if (slashIndex < dotIndex) {
            return fname.substring(0, dotIndex);
        } else {
            return fname;
        }
    }
     
    /**
     * Returns the first few characters from the first line of a file to check its type.
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
    
    private static BufferedReader getBufferedReader(final InputStream in) throws IOException {
        Charset encoding = StandardCharsets.UTF_8;
        PushbackInputStream pin = new PushbackInputStream(in, 4);
        byte[] b = new byte[4];
        int count = pin.read(b);
        pin.unread(b, 0, count);
        switch (count) {
            case 4:
                if ((b[0] & 0xFF) == 0x00 && (b[1] & 0xFF) == 0x00 && (b[2] & 0xFF) == 0xFE && (b[3] & 0xFF) == 0xFF) {
                    encoding = Charset.forName("UTF-32BE");
                    pin.skip(4);
                    break;
                } else if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xFE && (b[2] & 0xFF) == 0x00 && (b[3] & 0xFF) == 0x00) {
                    encoding = Charset.forName("UTF-32LE");
                    pin.skip(4);
                    break;
                }
                /* falls through */
            case 3:
                if ((b[0] & 0xFF) == 0xEF && (b[1] & 0xFF) == 0xBB && (b[2] & 0xFF) == 0xBF) {
                    // Skip the UTF-8 BOM since Java doesn't do this automatically
                    pin.skip(3);
                    break;
                }
                /* falls through */
            case 2:
                if ((b[0] & 0xFF) == 0xFE && (b[1] & 0xFF) == 0xFF) {
                    encoding = StandardCharsets.UTF_16BE;
                    pin.skip(2);
                    break;
                } else if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xFE) {
                    encoding = StandardCharsets.UTF_16LE;
                    pin.skip(2);
                    break;
                }
                break;
            default:
                break;
        }
        
        BufferedReader r = new BufferedReader(new InputStreamReader(pin, encoding));
        return r;
    }

    /**
     * Get path name from absolute file name.
     * @param path absolute file name
     * @return path name without the separator
     */
    public static String getPathName(final String path) {
        int p1 = path.lastIndexOf("/");
        int p2 = path.lastIndexOf("\\");
        if (p2 > p1) {
            p1 = p2;
        }
        if (p1 < 0) {
            p1 = 0;
        }
        return path.substring(0, p1);
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

    /**
     * Parse decimal, hex, binary or octal number
     * @param s String that contains one number
     * @return Integer value of string
     */
    public static int parseInt(final String s) {
        switch (s) {
            case "Infinity":
                return Integer.MAX_VALUE;
            case "-Infinity":
                return Integer.MIN_VALUE;
            default:
                break;
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
            
            long retval = Long.parseLong((hasSign ? s.substring(0, 1) : "") + s.substring(index), radix);
            if ((retval & 0xFFFF_FFFF_0000_0000L) == 0) {
                return (int) retval;
            } else {
                throw new NumberFormatException(String.format("String value %s exceeds range of unsigned int.", s));
            }
        } else {
            // decimal
            return Integer.parseInt(s);
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
    private static boolean isSign(char c) {
        return c == '+' || c == '-';
    }
}
