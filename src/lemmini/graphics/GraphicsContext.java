package lemmini.graphics;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.PixelGrabber;
import java.awt.image.WritableRaster;
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

    public void drawImage(Image image, int x, int y) {
        graphics.drawImage(image.getImage(), x, y, null);
    }

    public void drawImage(Image image, int x, int y, int width, int height) {
        graphics.drawImage(image.getImage(), x, y, width, height, null);
    }

    public void drawImage(Image image, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2) {
        graphics.drawImage(image.getImage(), dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
    }
    
    public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue) {
        graphics.setRenderingHint(hintKey, hintValue);
    }

    public void grabPixels(Image image, int x, int y, int w, int h, int[] pix, int off, int scanSize) {
        PixelGrabber pixelgrabber = new PixelGrabber(image.getImage(), x, y, w, h, pix, off, scanSize);
        try {
            pixelgrabber.grabPixels();
        } catch (InterruptedException ex) {
        }
    }

    public void copy(Image source, Image target) {
        WritableRaster rImgSpr = target.getImage().getRaster();
        rImgSpr.setRect(source.getImage().getRaster()); // just copy
    }

    public void dispose() {
        graphics.dispose();
    }
}
