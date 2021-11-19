package lemmini.graphics;

import java.awt.Color;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.awt.image.WritableRaster;
import java.util.HashMap;
import java.util.Map;

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

public class GraphicsContext {
    
    private final Graphics2D graphics;
    
    public GraphicsContext(Graphics2D graphics) {
        this.graphics = graphics;
    }
    
    public void setClip(int x, int y, int width, int height) {
        graphics.setClip(x, y, width, height);
    }
    
    public void clearRect(int x, int y, int width, int height) {
        graphics.clearRect(x, y, width, height);
    }
    
    public void drawRect(int x, int y, int width, int height) {
        graphics.drawRect(x, y, width, height);
    }
    
    public void fillRect(int x, int y, int width, int height) {
        graphics.fillRect(x, y, width, height);
    }
    
    public void setBackground(Color bgColor) {
        graphics.setBackground(bgColor);
    }
    
    public void setColor(Color color) {
        graphics.setColor(color);
    }
    
    /**
     * Draws as much of the specified image as is currently available.
     * The image is drawn with its top-left corner at(x, y) in this graphics context's coordinate 
     * space. Transparent pixels in the image do not affect whatever
     * pixels are already there. 
     * @param image the specified LemmImage sprite to be drawn
     * @param x the x coordinate of the top-left corner.
     * @param y the y coordinate of the top-left corner.
     */
    public void drawImage(LemmImage image, int x, int y) {
        graphics.drawImage(image.getImage(), x, y, null);
    }
    
    
    public void drawImage(LemmImage image, int x, int y, double scale) {
    	BufferedImage origImage = image.getImage();
    	BufferedImage newResized = resize(origImage, (int)(image.getWidth() * scale), (int)(image.getHeight() * scale));
    	graphics.drawImage(newResized, x, y, null);
    }
    
    public static BufferedImage resize(BufferedImage image, int width, int height) {
    	BufferedImage newResizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    	Graphics2D g = newResizedImage.createGraphics();

        // background transparent
        g.setComposite(AlphaComposite.Src);
        g.fillRect(0, 0, width, height);
    	
        Map<RenderingHints.Key,Object> hints = new HashMap<>();
        hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.addRenderingHints(hints);

        // puts the original image into the newResizedImage
        g.drawImage(image, 0, 0, width, height, null);
        g.dispose();
        
    	return newResizedImage;
    }
    
    
    /**
     * Draws as much of the specified image as has already been scaled
     * to fit inside the specified rectangle.
     * <p>
     * The image is drawn inside the specified rectangle of this
     * graphics context's coordinate space, and is scaled if
     * necessary. Transparent pixels do not affect whatever pixels
     * are already there.
     * <p>
     * This method returns immediately in all cases, even if the
     * entire image has not yet been scaled, dithered, and converted
     * for the current output device.
     * If the current output representation is not yet complete, then
     * {@code drawImage} returns {@code false}. As more of
     * the image becomes available, the process that loads the image notifies
     * the image observer by calling its {@code imageUpdate} method.
     * <p>
     * A scaled version of an image will not necessarily be
     * available immediately just because an unscaled version of the
     * image has been constructed for this output device.  Each size of
     * the image may be cached separately and generated from the original
     * data in a separate image production sequence.
     * @param    image  the specified image to be drawn. This method does
     *                  nothing if {@code image} is null.
     * @param    x      the <i>x</i> coordinate.
     * @param    y      the <i>y</i> coordinate.
     * @param    width  the width of the rectangle.
     * @param    height the height of the rectangle.
     * @return   {@code false} if the image pixels are still changing;
     *           {@code true} otherwise.
     * @see      java.awt.Image
     * @see      java.awt.image.ImageObserver
     * @see      java.awt.image.ImageObserver#imageUpdate(java.awt.Image, int, int, int, int, int)
     */
    public void drawImage(LemmImage image, int x, int y, int width, int height) {
        graphics.drawImage(image.getImage(), x, y, width, height, null);
    }
    
    /**
     * Draws as much of the specified area of the specified image as is
     * currently available, scaling it on the fly to fit inside the
     * specified area of the destination drawable surface. Transparent pixels
     * do not affect whatever pixels are already there.
     * <p>
     * This method returns immediately in all cases, even if the
     * image area to be drawn has not yet been scaled, dithered, and converted
     * for the current output device.
     * If the current output representation is not yet complete then
     * {@code drawImage} returns {@code false}. As more of
     * the image becomes available, the process that loads the image notifies
     * the specified image observer.
     * <p>
     * This method always uses the unscaled version of the image
     * to render the scaled rectangle and performs the required
     * scaling on the fly. It does not use a cached, scaled version
     * of the image for this operation. Scaling of the image from source
     * to destination is performed such that the first coordinate
     * of the source rectangle is mapped to the first coordinate of
     * the destination rectangle, and the second source coordinate is
     * mapped to the second destination coordinate. The subimage is
     * scaled and flipped as needed to preserve those mappings.
     * @param       image the specified image to be drawn. This method does
     *                    nothing if {@code image} is null.
     * @param       dx1 the <i>x</i> coordinate of the first corner of the
     *                    destination rectangle.
     * @param       dy1 the <i>y</i> coordinate of the first corner of the
     *                    destination rectangle.
     * @param       dx2 the <i>x</i> coordinate of the second corner of the
     *                    destination rectangle.
     * @param       dy2 the <i>y</i> coordinate of the second corner of the
     *                    destination rectangle.
     * @param       sx1 the <i>x</i> coordinate of the first corner of the
     *                    source rectangle.
     * @param       sy1 the <i>y</i> coordinate of the first corner of the
     *                    source rectangle.
     * @param       sx2 the <i>x</i> coordinate of the second corner of the
     *                    source rectangle.
     * @param       sy2 the <i>y</i> coordinate of the second corner of the
     *                    source rectangle.
     * @return   {@code false} if the image pixels are still changing;
     *           {@code true} otherwise.
     * @see         java.awt.Image
     * @see         java.awt.image.ImageObserver
     * @see         java.awt.image.ImageObserver#imageUpdate(java.awt.Image, int, int, int, int, int)
     * @since       1.1
     */
    public void drawImage(LemmImage image, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2) {
        graphics.drawImage(image.getImage(), dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
    }
    
    public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue) {
        graphics.setRenderingHint(hintKey, hintValue);
    }
    
    public void grabPixels(LemmImage image, int x, int y, int w, int h, int[] pix, int off, int scanSize) {
        PixelGrabber pixelgrabber = new PixelGrabber(image.getImage(), x, y, w, h, pix, off, scanSize);
        try {
            pixelgrabber.grabPixels();
        } catch (InterruptedException ex) {
        }
    }
    
    public void copy(LemmImage source, LemmImage target) {
        WritableRaster rImgSpr = target.getImage().getRaster();
        rImgSpr.setRect(source.getImage().getRaster()); // just copy
    }
    
    public void dispose() {
        graphics.dispose();
    }
}
