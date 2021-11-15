/*
 * Copyright 2016 Ryan Sakowski.
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
package lemmini.game;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import lemmini.graphics.LemmImage;
import lemmini.tools.Props;

/**
 *
 * @author Ryan Sakowski
 */
public class SpecialGraphicSet {
    
    /** list of default special styles */
    @SuppressWarnings("unused")
	private static final List<String> DEFAULT_SPECIAL_STYLES = Arrays.asList("awesome", "menace", "beastii", "beasti",
        "covox", "prima", "apple");
    
    private final String name;
    private final Props props;
    private final Color bgColor;
    private final int debrisColor;
    private final int debrisColor2;
    private final int[] particleColor;
    private final int positionX;
    private final int positionY;
    private LemmImage image = null;
    private boolean[][] mask = null;
    private boolean[][] steelMask = null;
    
    public SpecialGraphicSet(String name) throws LemmException, ResourceException {
        this.name = name;
        props = new Props();
        Resource res = Core.findResource("styles/special/" + name + "/" + name + ".ini", true);
        if (!props.load(res)) {
            throw new LemmException("Unable to read " + name + ".ini.");
        }
        
        int bgCol = props.getInt("bgColor", props.getInt("bgColor", 0x000000)) | 0xff000000;
        bgColor = new Color(bgCol);
        debrisColor = props.getInt("debrisColor", 0xffffff) | 0xff000000;
        debrisColor2 = props.getInt("debrisColor2", debrisColor) | 0xff000000;
        particleColor = props.getIntArray("particleColor", props.getIntArray("particleColor", GraphicSet.DEFAULT_PARTICLE_COLORS));
        for (int i = 0; i < particleColor.length; i++) {
            particleColor[i] |= 0xff000000;
        }
        positionX = props.getInt("positionX", 0);
        positionY = props.getInt("positionY", 0);
        
        // load main image
        res = Core.findResource(
                "styles/special/" + name + "/" + name + ".png",
                Core.IMAGE_EXTENSIONS);
        image = Core.loadLemmImage(res);
        
        // load mask
        LemmImage sourceImage;
        try {
            res = Core.findResource(
                    "styles/special/" + name + "/" + name + "m.png",
                    Core.IMAGE_EXTENSIONS);
            sourceImage = Core.loadLemmImage(res);
        } catch (ResourceException ex) {
            sourceImage = image;
        }
        mask = new boolean[sourceImage.getHeight()][sourceImage.getWidth()];
        for (int y = 0; y < mask.length; y++) {
            for (int x = 0; x < mask[y].length; x++) {
                mask[y][x] = (sourceImage.getRGB(x, y) >>> 24) >= 0x80;
            }
        }
        
        // load steel mask
        try {
            res = Core.findResource(
                    "styles/special/" + name + "/" + name + "s.png",
                    Core.IMAGE_EXTENSIONS);
            sourceImage = Core.loadLemmImage(res);
            steelMask = new boolean[sourceImage.getHeight()][sourceImage.getWidth()];
            for (int y = 0; y < steelMask.length; y++) {
                for (int x = 0; x < steelMask[y].length; x++) {
                    steelMask[y][x] = (sourceImage.getRGB(x, y) >>> 24) >= 0x80;
                }
            }
        } catch (ResourceException ex) {
            steelMask = new boolean[0][0];
        }
    }
    
    public String getName() {
        return name;
    }
    
    public Color getBgColor() {
        return bgColor;
    }
    
    public int getDebrisColor() {
        return debrisColor;
    }
    
    public int getDebrisColor2() {
        return debrisColor2;
    }
    
    public int[] getParticleColor() {
        return particleColor;
    }
    
    public int getPositionX() {
        return positionX;
    }
    
    public int getPositionY() {
        return positionY;
    }
    
    public LemmImage getImage() {
        return image;
    }
    
    public boolean[][] getMask() {
        return mask;
    }
    
    public boolean[][] getSteelMask() {
        return steelMask;
    }
}
