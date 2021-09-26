package lemmini.game;

import lemmini.graphics.GraphicsContext;
import lemmini.graphics.Image;
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
 * Handle small number font.
 * Meant to print out values between 0 and 99.
 *
 * @author Volker Oth
 */
public class NumFont {

    /** width in pixels */
    private static int width;
    /** height in pixels */
    private static int height;
    /** array of images - one for each cipher 0-9 */
    private static Image[] numImg;
    private static Image[] numImgNew;

    /**
     * Load and initialize the font.
     * @throws ResourceException
     */
    public static void init() throws ResourceException {
        String fn = Core.findResource("gfx/misc/numfont.png", Core.IMAGE_EXTENSIONS);
        if (fn == null) {
            throw new ResourceException("gfx/misc/numfont.png");
        }
        Image sourceImg = Core.loadTranslucentImage(fn);
        numImgNew = ToolBox.getAnimation(sourceImg, 10);
        width = sourceImg.getWidth();
        height = sourceImg.getHeight() / 10;
        numImg = new Image[100];
        for (int i = 0; i < 100; i++) {
            numImg[i] = ToolBox.createTranslucentImage(width * 2, height);
            GraphicsContext g = numImg[i].createGraphicsContext();
            g.drawImage(numImgNew[i / 10], 0, 0);
            g.drawImage(numImgNew[i % 10], width, 0);
            g.dispose();
        }
    }
    
    public static int getWidth() {
        return width;
    }

    /**
     * Get an image for a number between 0 and 9
     * @param n number (0-9)
     * @return image of the number
     */
    public static Image numImage(final int n) {
        int num;
        if (n > 9) {
            num = 9;
        } else if (n < 0) {
            num = 0;
        } else {
            num = n;
        }
        return numImgNew[num];
    }
}
