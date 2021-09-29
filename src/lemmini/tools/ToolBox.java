package lemmini.tools;


import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
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
     * Add (system default) path separator to string (if there isn't one already).
     * @param fName String containing path name
     * @return String that ends with the (system default) path separator for sure
     */
    public static String addSeparator(final String fName) {
        int pos = fName.lastIndexOf(File.separator);
        if (pos != fName.length() - 1) {
            pos = fName.lastIndexOf("/");
        }
        if (pos != fName.length() - 1) {
            return fName + "/";
        } else {
            return fName;
        }
    }

    /**
     * Exchange any DOS style path separator ("\") with a Unix style separator ("/").
     * @param fName String containing file/path name
     * @return String with only Unix style path separators
     */
    public static String exchangeSeparators(final String fName) {
        return fName.replace('\\', '/');
    }

    /**
     * Open file dialog.
     * @param parent parent frame
     * @param path default file name
     * @param ext array of allowed extensions
     * @param load true: load, false: save
     * @return absolute file name of selected file or null
     */
    public static String getFileName(final Object parent, final String path, final String[] ext, final boolean load) {
        String p = path;
        if (p.isEmpty()) {
            p = ".";
        }
        JFileChooser jf = new JFileChooser(p);
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
        jf.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (!load) {
            jf.setDialogType(JFileChooser.SAVE_DIALOG);
        }
        int returnVal = jf.showDialog((Component)parent,null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File f = jf.getSelectedFile();
            if (f != null) {
                return f.getAbsolutePath();
            }
        }
        return null;
    }
     
    /**
     * Return file name from path.
     * @param path String of a path with a file name
     * @return String containing only the file name
     */
    public static String getFileName(final String path) {
        int p1 = path.lastIndexOf("/");
        int p2 = path.lastIndexOf("\\");
        if (p2 > p1) {
            p1 = p2;
        }
        if (p1 < 0) {
            p1 = 0;
        } else {
            p1++;
        }
        return path.substring(p1);
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
     * Returns the first few characters from the first line of a file to check its type.
     * @param fname Filename of the file
     * @param num Number of characters to return
     * @return String from the beginning of the file
     */
    public static String getFileID(final String fname, final int num) {
        try (BufferedReader r = ToolBox.getBufferedReader(fname)) {
            String s = r.readLine();
            if (s != null) {
                if (s.length() <= num) {
                    return s;
                } else {
                    return s.substring(0, num);
                }
            } else {
                return null;
            }
        } catch (IOException ex) {
            return null;
        }
    }
    
    public static BufferedReader getBufferedReader(final String fname) throws IOException {
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(fname));
            return getBufferedReader(in);
        } catch (IOException | SecurityException | UnsupportedCharsetException ex) {
            if (in != null) {
                in.close();
            }
            throw ex;
        }
    }
    
    public static BufferedReader getBufferedReader(final URL file) throws IOException {
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(file.openStream());
            return getBufferedReader(in);
        } catch (IOException | SecurityException | UnsupportedCharsetException ex) {
            if (in != null) {
                in.close();
            }
            throw ex;
        }
    }
    
    private static BufferedReader getBufferedReader(final BufferedInputStream in) throws IOException {
        Charset encoding = StandardCharsets.UTF_8;
        byte[] b = new byte[4];
        in.mark(b.length);
        int count = in.read(b);
        in.reset();
        switch (count) {
            case 4:
                if ((b[0] & 0xFF) == 0x00 && (b[1] & 0xFF) == 0x00 && (b[2] & 0xFF) == 0xFE && (b[3] & 0xFF) == 0xFF) {
                    encoding = Charset.forName("UTF-32BE");
                    in.skip(4);
                    break;
                } else if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xFE && (b[2] & 0xFF) == 0x00 && (b[3] & 0xFF) == 0x00) {
                    encoding = Charset.forName("UTF-32LE");
                    in.skip(4);
                    break;
                }
                /* falls through */
            case 3:
                if ((b[0] & 0xFF) == 0xEF && (b[1] & 0xFF) == 0xBB && (b[2] & 0xFF) == 0xBF) {
                    // Skip the UTF-8 BOM since Java doesn't do this automatically
                    in.skip(3);
                    break;
                }
                /* falls through */
            case 2:
                if ((b[0] & 0xFF) == 0xFE && (b[1] & 0xFF) == 0xFF) {
                    encoding = StandardCharsets.UTF_16BE;
                    in.skip(2);
                    break;
                } else if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xFE) {
                    encoding = StandardCharsets.UTF_16LE;
                    in.skip(2);
                    break;
                }
                break;
            default:
                break;
        }
        
        BufferedReader r = new BufferedReader(new InputStreamReader(in, encoding));
        r.mark(0);
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
}
