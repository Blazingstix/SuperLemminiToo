/*
 * Copyright 2015 Ryan Sakowski.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package lemmini.graphics;

import java.awt.Color;
import java.awt.Transparency;
import lemmini.tools.ToolBox;

/**
 *
 * @author Ryan Sakowski
 */
public class GraphicsBuffer {
    
    private LemmImage image;
    private LemmImage subimage;
    private GraphicsContext graphics;
    private final int transparency;
    private final boolean resizable;
    
    public GraphicsBuffer(int width, int height, int transparency, boolean resizable) {
        this.transparency = transparency;
        this.resizable = resizable;
        setSize(width, height);
    }
    
    public synchronized final void setSize(int width, int height) {
        if (image != null && !resizable) {
            return;
        }
        
        int w = width;
        int h = height;
        
        if (image != null) {
            w = Math.max(w, image.getWidth());
            h = Math.max(h, image.getHeight());
        }
        
        if (resizable) {
            w = toPowerOf2(w);
            h = toPowerOf2(h);
        }
        
        if (image == null || w > image.getWidth() || h > image.getHeight()) {
            if (graphics != null) {
                graphics.dispose();
            }
            switch (transparency) {
                case Transparency.OPAQUE:
                    image = ToolBox.createOpaqueImage(w, h);
                    graphics = image.createGraphicsContext();
                    graphics.setBackground(new Color(0, 0, 0));
                    break;
                case Transparency.BITMASK:
                    image = ToolBox.createBitmaskImage(w, h);
                    graphics = image.createGraphicsContext();
                    graphics.setBackground(new Color(0, 0, 0, 0));
                    break;
                case Transparency.TRANSLUCENT:
                default:
                    image = ToolBox.createTranslucentImage(w, h);
                    graphics = image.createGraphicsContext();
                    graphics.setBackground(new Color(0, 0, 0, 0));
                    break;
            }
        }
        if (subimage == null || width != subimage.getWidth() || height != subimage.getHeight()) {
            if (resizable) {
                subimage = image.getSubimage(0, 0, width, height);
            } else {
                subimage = image;
            }
        }
    }
    
    public synchronized LemmImage getImage() {
        return subimage;
    }
    
    public synchronized GraphicsContext getGraphicsContext() {
        return graphics;
    }
    
    public synchronized void dispose() {
        graphics.dispose();
    }
    
    private static int toPowerOf2(int n) {
        if (n <= 0) {
            return 1;
        } else if (n > 2) {
            return Integer.highestOneBit((n - 1) << 1);
        } else {
            return n;
        }
    }
}
