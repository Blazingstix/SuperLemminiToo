package lemmini.game;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import lemmini.graphics.LemmImage;
import lemmini.tools.ToolBox;
import org.apache.commons.lang3.math.NumberUtils;

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
        MINIMAP_ARROW_LEFT,
        MINIMAP_ARROW_UP,
        MINIMAP_ARROW_RIGHT,
        MINIMAP_ARROW_DOWN,
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
    private static LemmImage[] image;
    private static LemmImage minimap;
    private static int minimapWidth;

    /**
     * Initialization.
     * @param mmWidth Minimap width
     * @throws ResourceException
     */
    public static void init(int mmWidth) throws ResourceException {
        List<LemmImage> images = new ArrayList<>(16);
        /* 0: MINIMAP_LEFT */
        Path fn = Core.findResource(Paths.get("gfx/misc/minimap_left.png"), Core.IMAGE_EXTENSIONS);
        LemmImage img = Core.loadTranslucentImage(fn);
        images.add(img);
        /* 1: MINIMAP_CENTER */
        fn = Core.findResource(Paths.get("gfx/misc/minimap_center.png"), Core.IMAGE_EXTENSIONS);
        img = Core.loadTranslucentImage(fn);
        images.add(img);
        /* 2: MINIMAP_RIGHT */
        fn = Core.findResource(Paths.get("gfx/misc/minimap_right.png"), Core.IMAGE_EXTENSIONS);
        img = Core.loadTranslucentImage(fn);
        images.add(img);
        /* 3: MINIMAP_ARROW_LEFT */
        fn = Core.findResource(Paths.get("gfx/misc/minimap_arrows.png"), Core.IMAGE_EXTENSIONS);
        LemmImage[] anim = ToolBox.getAnimation(Core.loadTranslucentImage(fn), 4);
        images.add(anim[0]);
        /* 4: MINIMAP_ARROW_UP */
        images.add(anim[1]);
        /* 5: MINIMAP_ARROW_RIGHT */
        images.add(anim[2]);
        /* 6: MINIMAP_ARROW_DOWN */
        images.add(anim[3]);
        /* 7: LEMMINI */
        img = Core.loadTranslucentImageJar("lemmini.png");
        images.add(img);
        /* 8: TILE_GREEN */
        fn = Core.findResource(Paths.get("gfx/misc/background_level.png"), Core.IMAGE_EXTENSIONS);
        img = Core.loadOpaqueImage(fn);
        images.add(img);
        /* 9: TILE_BROWN */
        fn = Core.findResource(Paths.get("gfx/misc/background_main.png"), Core.IMAGE_EXTENSIONS);
        img = Core.loadOpaqueImage(fn);
        images.add(img);
        /* 10: REPLAY_1 */
        fn = Core.findResource(Paths.get("gfx/misc/replay.png"), Core.IMAGE_EXTENSIONS);
        anim = ToolBox.getAnimation(Core.loadTranslucentImage(fn), 2);
        images.add(anim[0]);
        /* 11: REPLAY_2 */
        images.add(anim[1]);
        /* 12: SELECT */
        fn = Core.findResource(Paths.get("gfx/misc/select.png"), Core.IMAGE_EXTENSIONS);
        img = Core.loadTranslucentImage(fn);
        images.add(img);

        image = images.toArray(new LemmImage[images.size()]);
        
        /* Assemble minimap */
        minimapWidth = -1;
        setMinimapWidth(mmWidth);
    }

    /**
     * Get image.
     * @param idx Index
     * @return image of the given index
     */
    public static LemmImage getImage(Index idx) {
        return image[idx.ordinal()];
    }
    
    public static LemmImage getMinimapImage() {
        return minimap;
    }
    
    public static int getMinimapWidth() {
        return minimapWidth;
    }
    
    public static void setMinimapWidth(int width) {
        if (width == minimapWidth) {
            return;
        }
        
        LemmImage minimapLeft = image[Index.MINIMAP_LEFT.ordinal()];
        LemmImage minimapCenter = image[Index.MINIMAP_CENTER.ordinal()];
        LemmImage minimapRight = image[Index.MINIMAP_RIGHT.ordinal()];
        
        int leftWidth = Math.min(minimapLeft.getWidth(), 4 + width);
        int centerWidth = width + 4 - leftWidth;
        int rightWidth = minimapRight.getWidth();
        
        LemmImage tempMinimap = ToolBox.createTranslucentImage(leftWidth + centerWidth + rightWidth,
                NumberUtils.max(minimapLeft.getHeight(), minimapCenter.getHeight(), minimapRight.getHeight()));
        
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
