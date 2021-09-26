package lemmini.game;

import java.util.ArrayList;
import java.util.List;
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
 * Wrapper class for additional images which don't fit anywhere else.
 *
 * @author Volker Oth
 */
public class MiscGfx {

    /** Index of images */
    public static enum Index {
        /** border for the minimap */
        MINIMAP_LEFT,
        MINIMAP_CENTER,
        MINIMAP_RIGHT,
        /** Lemmini logo */
        LEMMINI,
        /** green background tile */
        TILE_GREEN,
        /** brown background tile */
        TILE_BROWN,
        /** replay sign 1 */
        REPLAY_1,
        /** replay sign 2 */
        REPLAY_2,
        /** selection marker for replay */
        SELECT
    }

    /** array of images */
    private static Image[] image;
    private static Image minimap;
    private static int minimapWidth;

    /**
     * Initialization.
     * @param mmWidth Minimap width
     * @throws ResourceException
     */
    public static void init(int mmWidth) throws ResourceException {
        List<Image> images = new ArrayList<>(16);
        /* 0: MINIMAP_LEFT */
        String fn = Core.findResource("gfx/misc/minimap_left.png", Core.IMAGE_EXTENSIONS);
        if (fn == null) {
            throw new ResourceException("gfx/misc/minimap_left.png");
        }
        Image img = Core.loadTranslucentImage(fn);
        images.add(img);
        /* 1: MINIMAP_CENTER */
        fn = Core.findResource("gfx/misc/minimap_center.png", Core.IMAGE_EXTENSIONS);
        if (fn == null) {
            throw new ResourceException("gfx/misc/minimap_center.png");
        }
        img = Core.loadTranslucentImage(fn);
        images.add(img);
        /* 2: MINIMAP_RIGHT */
        fn = Core.findResource("gfx/misc/minimap_right.png", Core.IMAGE_EXTENSIONS);
        if (fn == null) {
            throw new ResourceException("gfx/misc/minimap_right.png");
        }
        img = Core.loadTranslucentImage(fn);
        images.add(img);
        /* 3: LEMMINI */
        img = Core.loadTranslucentImageJar("lemmini.png");
        images.add(img);
        /* 4: TILE_GREEN */
        fn = Core.findResource("gfx/misc/background_level.png", Core.IMAGE_EXTENSIONS);
        if (fn == null) {
            throw new ResourceException("gfx/misc/background_level.png");
        }
        img = Core.loadOpaqueImage(fn);
        images.add(img);
        /* 5: TILE_BROWN */
        fn = Core.findResource("gfx/misc/background_main.png", Core.IMAGE_EXTENSIONS);
        if (fn == null) {
            throw new ResourceException("gfx/misc/background_main.png");
        }
        img = Core.loadOpaqueImage(fn);
        images.add(img);
        /* 6: REPLAY_1 */
        fn = Core.findResource("gfx/misc/replay.png", Core.IMAGE_EXTENSIONS);
        if (fn == null) {
            throw new ResourceException("gfx/misc/replay.png");
        }
        Image[] anim = ToolBox.getAnimation(Core.loadTranslucentImage(fn), 2);
        images.add(anim[0]);
        /* 7: REPLAY_2 */
        images.add(anim[1]);
        /* 8: SELECT */
        fn = Core.findResource("gfx/misc/select.png", Core.IMAGE_EXTENSIONS);
        if (fn == null) {
            throw new ResourceException("gfx/misc/select.png");
        }
        img = Core.loadTranslucentImage(fn);
        images.add(img);

        image = new Image[images.size()];
        image = images.toArray(image);
        
        /* Assemble minimap */
        minimapWidth = -1;
        setMinimapWidth(mmWidth);
    }

    /**
     * Get image.
     * @param idx Index
     * @return image of the given index
     */
    public static Image getImage(Index idx) {
        return image[idx.ordinal()];
    }
    
    public static Image getMinimapImage() {
        return minimap;
    }
    
    public static int getMinimapWidth() {
        return minimapWidth;
    }
    
    public static void setMinimapWidth(int width) {
        if (width == minimapWidth) {
            return;
        }
        
        Image minimapLeft = image[Index.MINIMAP_LEFT.ordinal()];
        Image minimapCenter = image[Index.MINIMAP_CENTER.ordinal()];
        Image minimapRight = image[Index.MINIMAP_RIGHT.ordinal()];
        
        int leftWidth = Math.min(minimapLeft.getWidth(), 4 + width);
        int centerWidth = width + 4 - leftWidth;
        int rightWidth = minimapRight.getWidth();
        
        Image tempMinimap = ToolBox.createTranslucentImage(leftWidth + centerWidth + rightWidth,
                Math.max(Math.max(minimapLeft.getHeight(), minimapCenter.getHeight()), minimapRight.getHeight()));
        
        for (int y = 0; y < minimapLeft.getHeight(); y++) {
            for (int x = 0; x < leftWidth; x++) {
                tempMinimap.setRGB(x, y, minimapLeft.getRGB(x, y));
            }
        }
        for (int y = 0; y < minimapCenter.getHeight(); y++) {
            for (int x = 0; x < centerWidth; x++) {
                tempMinimap.setRGB(leftWidth + x, y, minimapCenter.getRGB(x % minimapCenter.getWidth(), y));
            }
        }
        for (int y = 0; y < minimapRight.getHeight(); y++) {
            for (int x = 0; x < rightWidth; x++) {
                tempMinimap.setRGB(4 + width + x, y, minimapRight.getRGB(x, y));
            }
        }
        
        minimapWidth = width;
        minimap = tempMinimap;
    }
}
