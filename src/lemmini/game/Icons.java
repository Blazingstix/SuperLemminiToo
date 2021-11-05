package lemmini.game;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
//import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;

import lemmini.game.GameController.SuperLemminiTooOption;
import lemmini.gameutil.Sprite;
import lemmini.graphics.GraphicsContext;
import lemmini.graphics.LemmImage;
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
 * Handle the control icons.
 *
 * @author Volker Oth
 */
public class Icons {
    
    private static final int DEFAULT_PITCH = 4;
    
    /**
     * icon width in pixels (of currently selected icon bar)
     * @return
     */
    public static int getIconWidth() {
    	if (GameController.isOptionEnabled(SuperLemminiTooOption.ENHANCED_ICONBAR)) {
    		return ENHANCED_WIDTH;
    	} else {
    		return ORIGINAL_WIDTH;
    	}
    }
    
    /**
     * icon height in pixels (of currently selected icon bar)
     * @return
     */
    public static int getIconHeight() {
    	if (GameController.isOptionEnabled(SuperLemminiTooOption.ENHANCED_ICONBAR)) {
    		return ENHANCED_HEIGHT;
    	} else {
    		return ORIGINAL_HEIGHT;
    	}
    }
    
    private static final int ORIGINAL_WIDTH = 32;
    private static final int ORIGINAL_HEIGHT = 40;
    
    private static final int ENHANCED_WIDTH = 34;
    private static final int ENHANCED_HEIGHT = 54;
    
    
    /** Icon types */
    public static enum Type {
        /** minus icon */
        MINUS (DEFAULT_PITCH),
        /** plus icon */
        PLUS (DEFAULT_PITCH),
        /** climber icon */
        CLIMB (0),
        /** floater icon */
        FLOAT (1),
        /** bomber icon */
        BOMB (2),
        /** blocker icon */
        BLOCK (3),
        /** builder icon */
        BUILD (4),
        /** basher icon */
        BASH (5),
        /** miner icon */
        MINE (6),
        /** digger icon */
        DIG (7),
        /** pause icon */
        PAUSE (8),
        /** nuke icon */
        NUKE (9),
        /** fast forward icon */
        FFWD (10),
        /** vertical scroll lock icon */
        VLOCK (11),
        /** restart icon */
        RESTART (12),
        /** an empty icon (not used) */
        EMPTY (DEFAULT_PITCH);
        
        private static final Map<Integer, Type> LOOKUP = new HashMap<>();
        
        private final int pitch;
        
        static {
            EnumSet.allOf(Type.class).stream().forEach(s -> LOOKUP.put(s.ordinal(), s));
        }
        
        private Type(int newPitch) {
            pitch = newPitch;
        }
        
        /**
         * Reverse lookup implemented via HashMap.
         * @param val Ordinal value
         * @return Parameter with ordinal value val
         */
        public static Type get(final int val) {
            return LOOKUP.get(val);
        }
        
        public int getPitch() {
            return pitch;
        }
    }
    
    /** 1st radio button */
    static final int FIRST_RADIO = Type.CLIMB.ordinal();
    /** last radio button */
    static final int LAST_RADIO = Type.DIG.ordinal();
    /** number of radio buttons */
    private static final int NUM_RADIO = LAST_RADIO - FIRST_RADIO + 1;
    /** last icon to be drawn */
    private static final int LAST_DRAWN = Type.RESTART.ordinal();
    
    /** list of Sprites that contains the icons */
    private static final List<Sprite> icons = new ArrayList<>(Type.values().length);
    /** buffered image that contains the whole icon bar in its current state */
    private static LemmImage iconImg;
    /** graphics object used to draw on iconImg */
    private static GraphicsContext iconGfx = null;
    private static Type pressedIcon = null;
    
    /** list of Sprites that contains the standard-sized background icons */
    private static final List<Sprite> bgIcons = new ArrayList<>(Type.values().length);
    /** list of Sprites that contains the larger enhanced-size background icons */
    private static final List<Sprite> bgIconsLarge = new ArrayList<>(Type.values().length);
    /** list of Sprites the contain the icon labels */
    private static final List<Sprite> iconLabels = new ArrayList<>(Type.values().length);
    
    
    /** 
     * Initialization. 
     * @throws ResourceException 
     */   
    public static void init() throws ResourceException { 
        bgIcons.clear(); 
    	icons.clear(); 
    	iconLabels.clear();
        if (iconGfx != null) {
            iconGfx.dispose();
        }
        iconImg = ToolBox.createLemmImage(getIconWidth() * (1 + LAST_DRAWN), getIconHeight());
        iconGfx = iconImg.createGraphicsContext();
        Type[] iconTypes = Type.values();
        //get the background image we're going to use...
        for (int i = 0; i <= LAST_DRAWN; i++) {
        	LemmImage sourceImg;
        	Resource res;
        	Sprite icon;
        	
        	//load the individual icon 
        	res = Core.findResource(
                    "gfx/icons/icon_" + iconTypes[i].name().toLowerCase(Locale.ROOT) + ".png",
                    true, Core.IMAGE_EXTENSIONS);
            sourceImg = Core.loadLemmImage(res);
            icon = new Sprite(sourceImg, 2, 1, false);
            icons.add(icon);

            //load standard size backgrounds
            //TODO: allow for multiple different background objects
            res = Core.findResource("gfx/icons/icon_empty.png", true, Core.IMAGE_EXTENSIONS);
            sourceImg = Core.loadLemmImage(res);
            icon = new Sprite(sourceImg, 2, 1, false);
            bgIcons.add(icon);

            //load larger background icons
            //TODO: allow for multiple different background objects
            res = Core.findResource("gfx/iconbar/icon_empty_large.png", true, Core.IMAGE_EXTENSIONS);
            sourceImg = Core.loadLemmImage(res);
            icon = new Sprite(sourceImg, 2, 1, false);
            bgIconsLarge.add(icon);

            //load the label overlays
            res = Core.findResource(
                    "gfx/icon_labels/label_" + iconTypes[i].name().toLowerCase(Locale.ROOT) + ".png",
                    true, Core.IMAGE_EXTENSIONS);
            sourceImg = Core.loadLemmImage(res);
            icon = new Sprite(sourceImg, 2, 1, false);
            iconLabels.add(icon);
            
        }
        //reset the icon bar to draw it fresh
        reset();
    }
    
    /**
     * Get icon type by x position.
     * @param x x position inside bar in pixels
     * @return Icon type
     */
    public static Type getType(final int x) {
        if (x < 0 || x >= (LAST_DRAWN + 1) * getIconWidth()) {
            return null; // invalid
        }
        return Type.get(x / getIconWidth());
    }
    
    /**
     * Get buffered image that contains the whole icon bar in its current state.
     * @return image of icon bar
     */
    public static LemmImage getImg() {
        return iconImg;
    }
    
    /**
     * Get pressed state of the given icon
     * @param type
     * @return
     */
    static boolean isPressed(Type type) {
        int idx = type.ordinal();
        if (idx > LAST_DRAWN) {
            return false;
        }
        return (icons.get(idx).getFrameIdx() != 0);
    }
    
    /**
     * Press down icon.
     * @param type Icon Type
     */
    static void press(final Type type) {
    	int idx = type.ordinal();
        switch (type) {
            case PAUSE:
            case FFWD:
            case VLOCK:
                //these three icons are toggle icons.
                if (idx <= LAST_DRAWN) {
                	Sprite icon = icons.get(idx);
                    int toggleFrame = (icon.getFrameIdx() == 0) ? 1 : 0;
                	setIconFrame(idx, toggleFrame);
                	drawIcon(idx);
                }
                break;
            case CLIMB:
            case FLOAT:
            case BOMB:
            case BLOCK:
            case BUILD:
            case BASH:
            case MINE:
            case DIG:
            	//update the icons for the "Radio Buttons" i.e. the skills
            	for (int i = FIRST_RADIO; i <= LAST_RADIO; i++) {
                    if (i != idx) {
                    	//reset all the skills *not* selected to show as unselected
                    	//the skill that *is* selected will be handled below.
                    	setIconFrame(i, 0);
                    	drawIcon(i);
                    } 
            	}
                pressedIcon = type;
                /* falls through */
            case MINUS:
            case PLUS:
            case NUKE:
            case RESTART:
            	setIconFrame(idx, 1);
            	drawIcon(idx);
                break;
            default:
                break;
        }
    }
    
    /**
     * Release icon.
     * @param type Icon Type
     */
    static void release(final Type type) {
        int idx = type.ordinal();
        switch (type) {
            case MINUS:
            case PLUS:
            case RESTART:
            	setIconFrame(idx, 0);
            	drawIcon(idx);
                break;
            case NUKE:
                if (!GameController.isNuked()) {
                	setIconFrame(idx, 0);
                	drawIcon(idx);
                }
                break;
            default:
                break;
        }
    }
    
    /**
     * Sets the current Sprite Index for the 
     * @param idx
     */
    private static void setIconFrame(int iconIdx, int frameIdx) {
        if (iconIdx <= LAST_DRAWN) {
            Sprite bgIcon = bgIcons.get(iconIdx);
            Sprite bgIconLarge = bgIconsLarge.get(iconIdx);
            Sprite icon = icons.get(iconIdx);
            Sprite iconLabel = iconLabels.get(iconIdx);
            bgIcon.setFrameIdx(frameIdx);
            bgIconLarge.setFrameIdx(frameIdx);
            icon.setFrameIdx(frameIdx);
            iconLabel.setFrameIdx(frameIdx);
        }
    }
    
    /**
     * Draws the background and icon of the selected icon button.
     * @param idx
     */
    private static void drawIcon(int idx) {
    	if (idx <= LAST_DRAWN) {
            int x = 0;
            int y = 0;
    		Sprite bgIcon; 
            if (GameController.isOptionEnabled(GameController.SuperLemminiTooOption.ENHANCED_ICONBAR)) {
                bgIcon = bgIconsLarge.get(idx);
                x = 1;
                y = 14;
            } else {
                bgIcon = bgIcons.get(idx);
            }
            Sprite icon = icons.get(idx);
            Sprite iconLabel = iconLabels.get(idx);
        	iconGfx.drawImage(bgIcon.getImage(), getIconWidth() * idx, 0);
            iconGfx.drawImage(icon.getImage(), getIconWidth() * idx + x, 0 + y);
            if (GameController.isOptionEnabled(GameController.SuperLemminiTooOption.ICON_LABELS))
            	iconGfx.drawImage(iconLabel.getImage(), getIconWidth() * idx + x, 0 + y);
        }
    }
    
    /**
     * Get the selected skill icon.
     * @return the selected skill icon if one is pressed, or null if none is pressed
     */
    static Type getPressedIcon() {
        return pressedIcon;
    }
    
    static Type getNextRadioIcon(Type type) {
        int ordinal = type.ordinal();
        if (ordinal >= FIRST_RADIO && ordinal < LAST_RADIO) {
            return Type.get(ordinal + 1);
        } else if (ordinal == LAST_RADIO) {
            return Type.get(FIRST_RADIO);
        } else {
            return null;
        }
    }
    
    static Type getPreviousRadioIcon(Type type) {
        int ordinal = type.ordinal();
        if (ordinal > FIRST_RADIO && ordinal <= LAST_RADIO) {
            return Type.get(ordinal - 1);
        } else if (ordinal == FIRST_RADIO) {
            return Type.get(LAST_RADIO);
        } else {
            return null;
        }
    }
    
    /**
     * Reset icon bar.
     */
    static void reset() {
        for (int i = 0; i <= LAST_DRAWN; i++) {
            setIconFrame(i, 0);
            drawIcon(i);
        }
        pressedIcon = null;
    }
    
    /**
     * Redraws the icon bar, with current states.
     */
    static void redraw() {
        for (int i = 0; i <= LAST_DRAWN; i++) {
            drawIcon(i);
        }
        pressedIcon = null;
    }
}
