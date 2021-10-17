package lemmini.game;

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
    

    /** list of images */
    private static final List<LemmImage> images = new ArrayList<>(16);
    private static final List<LemmImage> vsfx_images = new ArrayList<>(30);
    private static LemmImage minimap;
    private static int minimapWidth;
    
    /**
     * Initialization.
     * @param mmWidth Minimap width
     * @throws ResourceException
     */
    public static void init(int mmWidth) throws ResourceException {
        images.clear();
        /* 0: MINIMAP_LEFT */
        Resource res = Core.findResource("gfx/misc/minimap_left.png", true, Core.IMAGE_EXTENSIONS);
        LemmImage img = Core.loadLemmImage(res);
        images.add(img);
        /* 1: MINIMAP_CENTER */
        res = Core.findResource("gfx/misc/minimap_center.png", true, Core.IMAGE_EXTENSIONS);
        img = Core.loadLemmImage(res);
        images.add(img);
        /* 2: MINIMAP_RIGHT */
        res = Core.findResource("gfx/misc/minimap_right.png", true, Core.IMAGE_EXTENSIONS);
        img = Core.loadLemmImage(res);
        images.add(img);
        /* 3: MINIMAP_ARROW_LEFT, 4: MINIMAP_ARROW_UP, 5: MINIMAP_ARROW_RIGHT, 6: MINIMAP_ARROW_DOWN */
        res = Core.findResource("gfx/misc/minimap_arrows.png", true, Core.IMAGE_EXTENSIONS);
        List<LemmImage> anim = ToolBox.getAnimation(Core.loadLemmImage(res), 4);
        images.addAll(anim);
        /* 7: LEMMINI title graphic */
        img = Core.loadLemmImageJar("lemmini.png");
        images.add(img);
        /* 8: TILE_GREEN */
        res = Core.findResource("gfx/misc/background_level.png", true, Core.IMAGE_EXTENSIONS);
        img = Core.loadLemmImage(res);
        images.add(img);
        /* 9: TILE_BROWN */
        res = Core.findResource("gfx/misc/background_main.png", true, Core.IMAGE_EXTENSIONS);
        img = Core.loadLemmImage(res);
        images.add(img);
        /* 10: REPLAY_1, 11: REPLAY_2 */
        res = Core.findResource("gfx/misc/replay.png", true, Core.IMAGE_EXTENSIONS);
        anim = ToolBox.getAnimation(Core.loadLemmImage(res), 2);
        images.addAll(anim);
        /* 12: SELECT */
        res = Core.findResource("gfx/misc/select.png", true, Core.IMAGE_EXTENSIONS);
        img = Core.loadLemmImage(res);
        images.add(img);

        /*add visual sfx images */
        res = Core.findResource("gfx/misc/vsfxbig.png", true, Core.IMAGE_EXTENSIONS);
        anim = ToolBox.getAnimation(Core.loadLemmImage(res), Vsfx.VSFX_COUNT);
        vsfx_images.addAll(anim);
        
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
        return images.get(idx.ordinal());
    }

    /**
     * Get Visual SFX image.
     * @param idx Vsfx.Vsfx_Index
     * @return image of given index
     */
    public static LemmImage getVsfxImage(Vsfx.Vsfx_Index idx) {
        return vsfx_images.get(idx.ordinal());
    }

    /**
     * Get Visual SFX image. 
     * @param idx
     * @return
     */
    public static LemmImage getVsfxImage(int idx) {
    	return vsfx_images.get(idx);
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
        
        LemmImage minimapLeft = images.get(Index.MINIMAP_LEFT.ordinal());
        LemmImage minimapCenter = images.get(Index.MINIMAP_CENTER.ordinal());
        LemmImage minimapRight = images.get(Index.MINIMAP_RIGHT.ordinal());
        
        int leftWidth = Math.min(minimapLeft.getWidth(), 4 + width);
        int centerWidth = width + 4 - leftWidth;
        int rightWidth = minimapRight.getWidth();
        
        LemmImage tempMinimap = ToolBox.createLemmImage(leftWidth + centerWidth + rightWidth,
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
