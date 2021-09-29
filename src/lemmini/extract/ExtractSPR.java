package lemmini.extract;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
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
 * Extract graphics from "Lemming for Win95" SPR data files.
 * @author Volker Oth
 */
public class ExtractSPR {
    /** palette index of transparent color */
    private static final int TRANSPARENT_INDEX = 0;

    /** array of PNG images to store to disk */
    private PNGImage[] images;
    /** color palette */
    private Palette palette = null;
    /** buffer used to compress the palette (remove double entries) to work around issues on MacOS */
    private int[] lookupBuffer;
    private int paletteSize;

    /**
     * Load palette.
     * @param fname Name of palette file
     * @return ColorModel representation of Palette
     * @throws ExtractException
     */
    Palette loadPalette(final Path fname) throws ExtractException {
        ByteBuffer buffer;

        // read file into buffer
        //int paletteSize;
        try {
            if (!Files.isRegularFile(fname)) {
                throw new ExtractException(String.format("File %s not found.", fname));
            }
            buffer = ByteBuffer.wrap(Files.readAllBytes(fname)).asReadOnlyBuffer();
        } catch (IOException e) {
            throw new ExtractException(String.format("I/O error while reading %s.", fname));
        }
        
        // check header
        if (buffer.getInt() != 0x204c4150) {
            throw new ExtractException(String.format("File %s is not a Lemmings palette file.", fname));
        }
        
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        paletteSize = buffer.getShort() & 0xffff; // number of palette entries

        byte[] r = new byte[paletteSize];
        byte[] g = new byte[paletteSize];
        byte[] b = new byte[paletteSize];

        for (int idx = 0; idx < paletteSize; idx++) {
            r[idx] = buffer.get();
            g[idx] = buffer.get();
            b[idx] = buffer.get();
            buffer.get();
        }

        // search for double entries, create
        // new palette without double entries
        // and lookup table to fix the pixel values
        byte[] compressedR = new byte[paletteSize];
        byte[] compressedG = new byte[paletteSize];
        byte[] compressedB = new byte[paletteSize];
        lookupBuffer = new int[paletteSize];

        Arrays.fill(lookupBuffer, -1); // mark all entries invalid
        Arrays.fill(compressedR, (byte) 0);
        Arrays.fill(compressedG, (byte) 0);
        Arrays.fill(compressedB, (byte) 0);

        int compressedIndex = 0;
        for (int i = 0; i < paletteSize; i++) {
            if (lookupBuffer[i] == -1) {             // if -1, this value is not a duplicate of a lower index
                compressedR[compressedIndex] = r[i]; // copy value to compressed buffer
                compressedG[compressedIndex] = g[i];
                compressedB[compressedIndex] = b[i];
                if (i != TRANSPARENT_INDEX) {       // don't search duplicates of transparent color
                    // search for duplicates at higher indices
                    for (int j = i + 1; j < paletteSize; j++) {
                        if (j == TRANSPARENT_INDEX) { // transparent color can't be a duplicate of another color
                            continue;
                        }
                        if ((r[i] == r[j]) && (g[i] == g[j]) && (b[i] == b[j])) {
                            lookupBuffer[j] = compressedIndex; // mark double entry in lookupBuffer
                        }
                    }
                }
                lookupBuffer[i] = compressedIndex++;
            }
        }

        if (paletteSize != compressedIndex) {
            r = compressedR;
            g = compressedG;
            b = compressedB;
        }

        palette = new Palette(r, g, b);
        palette.size = compressedIndex;
        return palette;
    }

    /**
     * Load SPR file. Load palette first!
     * @param fname Name of SPR file
     * @return Array of Images representing all images stored in the SPR file
     * @throws ExtractException
     */
    PNGImage[] loadSPR(final Path fname) throws ExtractException {
        ByteBuffer buffer;

        if (palette == null) {
            throw new ExtractException("Load palette first!");
        }

        // read file into buffer
        try {
            if (!Files.isRegularFile(fname)) {
                throw new ExtractException(String.format("File %s not found.", fname));
            }
            buffer = ByteBuffer.wrap(Files.readAllBytes(fname)).asReadOnlyBuffer();
        } catch(IOException e) {
            throw new ExtractException(String.format("I/O error while reading %s.", fname));
        }
        // check header
        if (buffer.getInt() != 0x53524c45) {
            throw new ExtractException(String.format("File %s is not a Lemmings sprite file.", fname));
        }
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        // get number of frames
        int frames = buffer.getShort() & 0xffff;
        int ofs = buffer.getShort() & 0xffff;
        buffer.position(ofs);

        images = new PNGImage[frames];
        byte b;
        int lineOfs;

        for (int frame = 0; frame < frames; frame++) {
            // get header info
            int xOfs = buffer.getShort() & 0xffff;   // x offset of data in output image
            int yOfs = buffer.getShort() & 0xffff;   // y offset of data in output image
            int maxLen = buffer.getShort() & 0xffff; // maximum length of a data line
            int lines = buffer.getShort() & 0xffff;  // number of data lines
            int width = buffer.getShort() & 0xffff;  // width of output image
            int height = buffer.getShort() & 0xffff; // height of output image

            byte[] pixels = new byte[width * height];

            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = TRANSPARENT_INDEX;
            }

            int y = yOfs * width;

            int pxOffset = 0; // additional offset for lines broken in several packets

            for (int line = 0; line < lines; ) {
                // read line
                b = buffer.get(); // start character including length (>= 0x80) or line offset (<0x80)
                lineOfs = 0;
                while (b == 0x7f) {   // special line offset for large sprites
                    lineOfs += 0x7f;
                    b = buffer.get();
                }
                if (!BooleanUtils.toBoolean(b & 0x80)) {
                    // additional line offset
                    lineOfs += (b & 0x7f);
                    b = buffer.get(); // start character
                }
                // get line length
                int len = (b & 0xff) - 0x80;
                if (len < 0 || len > 0x7f || len > maxLen) {
                    throw new ExtractException(String.format("Maximum data line length exceeded in line %d of frame %d of %s (offset: %d).", line, frame, fname, ofs));
                }
                if (len > 0) {
                    try {
                        for (int pixel = 0; pixel < len; pixel++) {
                            // none of the extracted images uses more than 128 colors (indeed much less)
                            // but some use higher indices. Instead of mirroring the palette, just modulo every
                            // entry with the palette size.
                            // The lookup table is needed to get new index in compressed palette
                            byte pixelVal = (byte) (lookupBuffer[(buffer.get() & 0xff) % paletteSize] & 0xff);
                            pixels[y + xOfs + lineOfs + pixel + pxOffset] = pixelVal;
                        }
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        throw new ExtractException(String.format("Index out of bounds in line %d of frame %d of %s (offset: %d).", line, frame, fname, ofs));
                    }
                    buffer.mark();
                    b = buffer.get(); // end character must be 0x80
                    if ((b & 0xff) != 0x80) {
                        // if this is not the end character, the line is continued after an offset
                        pxOffset += (lineOfs + len);
                        buffer.reset();
                        continue;
                    }
                }
                pxOffset = 0;
                line++;
                y += width;
            }
            // convert byte array into BufferedImage
            images[frame] = new PNGImage(width, height, pixels, palette);
        }

        return images;
    }
    
    void createMasks() throws ExtractException {
        if (images == null) {
            throw new ExtractException("Load SPR first!");
        }
        
        byte[] tempRed = {0x00, (byte) 0xFF};
        byte[] tempGreen = {0x00, 0x00};
        byte[] tempBlue = {0x00, (byte) 0xFF};
        palette = new Palette(tempRed, tempGreen, tempBlue);
        for (PNGImage img : images) {
            img.createMask();
        }
    }

    /**
     * Save all images of currently loaded SPR file
     * @param fname Filename of PNG files to export. "_N.png" will be appended with N being the image number.
     * @param keepAnims If true, consequently stored imaged with same size will be stored inside one PNG (one beneath the other)
     * @return Array of all the filenames stored
     * @throws ExtractException
     */
    Path[] saveAll(final Path fname, final boolean keepAnims) throws ExtractException {
        int width = images[0].getWidth();
        int height = images[0].getHeight();
        int startIdx = 0;
        int animNum = 0;
        List<Path> files = new ArrayList<>(64);

        for (int idx = 1; idx <= images.length; idx++) {
            // search for first image with different size
            if (keepAnims && idx < images.length && images[idx].getWidth() == width && images[idx].getHeight() == height) {
                continue;
            }
            // now save all the images in one: one above the other
            int num;
            if (keepAnims) {
                num = idx - startIdx;
            } else {
                num = 1;
            }
            byte[] pixels = new byte[width * num * height];
            PNGImage anim = new PNGImage(width, num * height, pixels, palette);
            for (int n = 0; n < num; n++) {
                System.arraycopy(images[startIdx + n].getPixels(), 0, pixels, n * height * width, images[startIdx + n].getPixels().length);
            }

            startIdx = idx;
            // construct filename
            Path fn = fname.resolveSibling(fname.getFileName().toString() + "_" + (animNum++) + ".png");
            // save png
            savePng(anim, fn);
            files.add(fn);
            // remember new size
            if (idx < images.length) {
                width = images[idx].getWidth();
                height = images[idx].getHeight();
            }
        }
        Path[] fileArray = new Path[files.size()];
        return files.toArray(fileArray);
    }

    /**
     * Save a number of images of currently loaded SPR file into one PNG (one image beneath the other)
     * @param fname Name of PNG file to create (".png" will NOT be appended)
     * @param startIdx Index of first image to store
     * @param frames Number of frames to store
     * @throws ExtractException
     */
    void saveAnim(final Path fname, final int startIdx, final int frames) throws ExtractException {
        int width = images[startIdx].getWidth();
        int height = images[startIdx].getHeight();

        byte[] pixels = new byte[width * frames * height];
        PNGImage anim = new PNGImage(width, frames * height, pixels, palette);
        for (int n = 0; n < frames; n++) {
            System.arraycopy(images[startIdx + n].getPixels(), 0, pixels, n * height * width, images[startIdx + n].getPixels().length);
        }
        // save png
        savePng(anim, fname);
    }
    
    /**
     * Save one image as PNG
     * @param img Image object to save
     * @param fname Name of PNG file to create (".png" will NOT be appended)
     * @throws ExtractException
     */
    static void savePng(final PNGImage img, final Path fname) throws ExtractException {
        IndexColorModel colorModel = new IndexColorModel(8, img.palette.size,
                img.palette.getRed(), img.palette.getGreen(), img.palette.getBlue(),
                TRANSPARENT_INDEX);
        BufferedImage image = new BufferedImage(img.getWidth(), img.getHeight(),
                BufferedImage.TYPE_BYTE_INDEXED, colorModel);
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int index = img.getPixels()[y * img.getWidth() + x];
                int pixel = ((img.palette.getRed()[index] & 0xff) << 16)
                        + ((img.palette.getGreen()[index] & 0xff) << 8)
                        + ((img.palette.getBlue()[index] & 0xff));
                if (index != TRANSPARENT_INDEX) {
                    pixel |= 0xff000000;
                }
                image.setRGB(x, y, pixel);
            }
        }
        try (OutputStream out = Files.newOutputStream(fname)) {
            ImageIO.write(image, "png", out);
        } catch(IOException ex) {
            throw new ExtractException(String.format("I/O error while writing file %s.", fname));
        }
    }


    /**
     * Storage class for palettes.
     * @author Volker Oth
     */
    class Palette {
        /** byte array of red components */
        private final byte[] red;
        /** byte array of green components */
        private final byte[] green;
        /** byte array of blue components */
        private final byte[] blue;
        private int size;

        /**
         * Create palette from array of color components
         * @param r byte array of red components
         * @param g byte array of green components
         * @param b byte array of blue components
         */
        Palette(final byte[] r, final byte[] g, final byte[] b) {
            red = r;
            green = g;
            blue = b;
            size = r.length;
        }

        /**
         * Get blue components.
         * @return byte array of blue components
         */
        public byte[] getBlue() {
            return blue;
        }

        /**
         * Get green components.
         * @return byte array of green components
         */
        public byte[] getGreen() {
            return green;
        }

        /**
         * Get red components.
         * @return byte array of red components
         */
        public byte[] getRed() {
            return red;
        }
    }

    /**
     * Stores PNG Image in RAM.
     */
    class PNGImage {
        /** width in pixels */
        private int width;
        /** height in pixels */
        private int height;
        /** pixel data */
        private byte[] pixels;
        /** color palette */
        private Palette palette;

        /**
         * Constructor.
         * @param w width in pixels.
         * @param h height in pixels.
         * @param buf pixel data
         * @param p color palette
         */
        PNGImage(final int w, final int h, final byte[] buf, final Palette p) {
            width = w;
            height = h;
            pixels = buf;
            palette = p;
        }

        /**
         * Get pixel data.
         * @return pixel data as array of bytes
         */
        public byte[] getPixels() {
            return pixels;
        }

        /**
         * Get width in pixels.
         * @return width in pixels
         */
        public int getWidth() {
            return width;
        }

        /**
         * Get height in pixels.
         * @return height in pixels
         */
        public int getHeight() {
            return height;
        }

        /**
         * Get color palette.
         * @return color palette
         */
        public Palette getPalette() {
            return palette;
        }
        
        public void createMask() {
            byte[] tempRed = {0x00, (byte) 0xFF};
            byte[] tempGreen = {0x00, 0x00};
            byte[] tempBlue = {0x00, (byte) 0xFF};
            palette = new Palette(tempRed, tempGreen, tempBlue);
            
            for (int i = 0; i < pixels.length; i++) {
                if (pixels[i] != TRANSPARENT_INDEX) {
                    pixels[i] = TRANSPARENT_INDEX + 1;
                }
            }
            
            byte[] tempPixels = new byte[pixels.length * 4];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    tempPixels[y * width * 4 + x * 2] = pixels[y * width + x];
                    tempPixels[y * width * 4 + x * 2 + 1] = pixels[y * width + x];
                    tempPixels[y * width * 4 + width * 2 + x * 2] = pixels[y * width + x];
                    tempPixels[y * width * 4 + width * 2 + x * 2 + 1] = pixels[y * width + x];
                }
            }
            pixels = tempPixels;
            width *= 2;
            height *= 2;
        }
    }
}
