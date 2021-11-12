package lemmini.game;

import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
//import java.util.ListIterator;
import java.util.Locale;
//import java.util.Map;

//import org.apache.commons.lang3.math.NumberUtils;

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
    
    public static enum IconType {
    	MINUS,
    	PLUS,
    	CLIMB,
    	FLOAT,
    	BOMB,
    	BLOCK,
    	BUILD,
    	BASH,
    	MINE,
    	DIG,
    	PAUSE,
    	FFWD,
    	NUKE,
    	RESTART,
    	VLOCK,
    	EMPTY
    }
    
    /**
     * The currently chosen order of icons
     * @return
     */
    public static List<IconType> CurrentIconOrder() {
        if (GameController.isOptionEnabled(GameController.SuperLemminiTooOption.ENHANCED_ICONBAR)) {
        	return EnhancedIconOrder();
        } else {
        	return StandardIconOrder();
        }
    }
    
    /**
     * List of all icons, in the classic SuperLemmini order
     * @return
     */
    private static List<IconType> StandardIconOrder() {
    	List<IconType> rslt;
    	rslt = new ArrayList<IconType>();
    	rslt.add(IconType.MINUS);
    	rslt.add(IconType.PLUS);
    	rslt.addAll(SkillIconOrder());
    	rslt.add(IconType.PAUSE);
    	rslt.add(IconType.NUKE);
    	rslt.add(IconType.FFWD);
    	rslt.add(IconType.VLOCK);
    	rslt.add(IconType.RESTART);
    	return rslt;
    }

    /**
     * List of all icons, in the Enhanced SuperLemminiToo order
     * @return
     */
    private static List<IconType> EnhancedIconOrder() {
    	List<IconType> rslt;
    	rslt = new ArrayList<IconType>();
    	rslt.add(IconType.MINUS);
    	rslt.add(IconType.PLUS);
    	rslt.addAll(SkillIconOrder());
    	rslt.add(IconType.PAUSE);
    	rslt.add(IconType.FFWD);
    	rslt.add(IconType.NUKE);
    	rslt.add(IconType.RESTART);
    	rslt.add(IconType.VLOCK);
    	return rslt;
    }
    
	/**
	 * List of basic skillset icons, in order. 
	 * @return
	 */
    public static List<IconType> SkillIconOrder() {
    	List<IconType> rslt;
    	rslt = new ArrayList<IconType>();
    	rslt.add(IconType.CLIMB);
    	rslt.add(IconType.FLOAT);
    	rslt.add(IconType.BOMB);
    	rslt.add(IconType.BLOCK);
    	rslt.add(IconType.BUILD);
    	rslt.add(IconType.BASH);
    	rslt.add(IconType.MINE);
    	rslt.add(IconType.DIG);
    	return rslt;
    }

    /** buffered image that contains the whole icon bar in its current state */
    private static LemmImage iconImg;
    /** graphics object used to draw on iconImg */
    private static GraphicsContext iconGfx = null;
    private static IconType pressedIcon = null;
    
    /** list of Sprites that contains the icons */
    private static final List<Sprite> icons = new ArrayList<>(IconType.values().length);
    /** list of Sprites that contains the standard-sized background icons */
    private static final List<Sprite> bgIcons = new ArrayList<>(IconType.values().length);
    /** list of Sprites that contains the larger enhanced-size background icons */
    private static final List<Sprite> bgIconsLarge = new ArrayList<>(IconType.values().length);
    /** list of Sprites the contain the icon labels */
    private static final List<Sprite> iconLabels = new ArrayList<>(IconType.values().length);
       
    private static final HashMap<IconType, Integer> iconFrame = new HashMap<>(IconType.values().length);
    
    /** 
     * Initialization. 
     * @throws ResourceException 
     */   
    public static void init() throws ResourceException { 
    	//load the hashmap with initial values for each of the IconTypes
    	if (iconFrame.isEmpty()) {
	    	for(IconType x : IconType.values()) {
	    		iconFrame.put(x, 0);
	    	}
    	}
    	LoadIconResources();
        //reset the icon bar to draw it fresh
        reset();
    }
    
    public static void LoadIconResources() throws ResourceException {
    	bgIcons.clear(); 
        bgIconsLarge.clear();
    	icons.clear(); 
    	iconLabels.clear();

    	if (iconGfx != null) {
            iconGfx.dispose();
        }
        List<IconType> iconOrder = CurrentIconOrder();
        iconImg = ToolBox.createLemmImage(getIconWidth() * (iconOrder.size()), getIconHeight());
        iconGfx = iconImg.createGraphicsContext();

        //get the background image we're going to use...
        for (int i = 0; i < iconOrder.size(); i++) {
        	LemmImage sourceImg;
        	Resource res;
        	Sprite icon;
        	
        	//load the individual icon 
        	res = Core.findResource(
                    "gfx/icons/icon_" + iconOrder.get(i).toString().toLowerCase(Locale.ROOT) + ".png",
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
                    "gfx/icon_labels/label_" + iconOrder.get(i).toString().toLowerCase(Locale.ROOT) + ".png",
                    true, Core.IMAGE_EXTENSIONS);
            sourceImg = Core.loadLemmImage(res);
            icon = new Sprite(sourceImg, 2, 1, false);
            iconLabels.add(icon);
        }
    }
    
    
    /**
     * Get icon type by x position.
     * @param x x position inside bar in pixels
     * @return Icon type
     */
    public static IconType getType(final int x) {
        List<IconType> iconOrder = CurrentIconOrder();
    	if (x < 0 || x >= iconOrder.size() * getIconWidth()) {
            return null; // invalid
        }
        return iconOrder.get(x / getIconWidth());
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
    static boolean isPressed(IconType type) {
        List<IconType> iconOrder = CurrentIconOrder();
    	int idx = iconOrder.indexOf(type);
        if (idx == -1) {
            return false;
        }
        return (icons.get(idx).getFrameIdx() != 0);
    }
    
    /**
     * Press down icon.
     * @param type Icon Type
     */
    static void press(final IconType type) {
        List<IconType> iconOrder = CurrentIconOrder();
    	int idx = iconOrder.indexOf(type);
    	if (idx == -1) {
    		return;
    	}

    	switch (type) {
            case PAUSE:
            case FFWD:
            case VLOCK:
                //these three icons are toggle icons.
                if (idx < iconOrder.size()) {
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
            	for (IconType x : SkillIconOrder()) {
            		int i = iconOrder.indexOf(x);
            		if (i != -1 && i != idx) {
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
    static void release(final IconType type) {
        List<IconType> iconOrder = CurrentIconOrder();
    	int idx = iconOrder.indexOf(type);
    	if (idx == -1) {
    		return;
    	}

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
        if (iconIdx < CurrentIconOrder().size()) {
            IconType type = CurrentIconOrder().get(iconIdx);
            iconFrame.replace(type, frameIdx);
            /*
            Sprite bgIcon = bgIcons.get(iconIdx);
            Sprite bgIconLarge = bgIconsLarge.get(iconIdx);
            Sprite icon = icons.get(iconIdx);
            Sprite iconLabel = iconLabels.get(iconIdx);
            bgIcon.setFrameIdx(frameIdx);
            bgIconLarge.setFrameIdx(frameIdx);
            icon.setFrameIdx(frameIdx);
            iconLabel.setFrameIdx(frameIdx);
            */
        }
    }
    
    /**
     * Draws the background and icon of the selected icon button.
     * @param idx
     */
    private static void drawIcon(int idx) {
    	if (idx <= CurrentIconOrder().size()) {
            IconType type = CurrentIconOrder().get(idx);
    		int x = 0;
            int y = 0;
    		Sprite bgIcon; 
            if (GameController.isOptionEnabled(GameController.SuperLemminiTooOption.ENHANCED_ICONBAR)) {
                bgIcon = bgIconsLarge.get(idx);
                x = 1; //the larger icons have an added pixel on the side of padding.
                y = 14; //the larger icons have 14 pixels more headroom (for the numbers)
            } else {
                bgIcon = bgIcons.get(idx);
            }
            Sprite iconLabel = iconLabels.get(idx);
            Sprite icon = icons.get(idx);

            //set the frame to use for each Sprite
            int frameIdx = iconFrame.get(type);
            int frameIdx2 = (frameIdx > 0) ? 1 : 0;
            bgIcon.setFrameIdx(frameIdx2);
            iconLabel.setFrameIdx(frameIdx2);
            icon.setFrameIdx(frameIdx);
            
        	iconGfx.drawImage(bgIcon.getImage(), getIconWidth() * idx, 0);

        	//these 5 icon types don't have numbers with them, so we can move the icons up a tad
        	int yIcon = 0;
        	int yLabel = 0;
            if (GameController.isOptionEnabled(GameController.SuperLemminiTooOption.ENHANCED_ICONBAR)) {
	        	switch(type) {
		        	case FFWD:
		        	case PAUSE:
		        	case RESTART:
		        	case VLOCK:
		        	case NUKE:
		        		yIcon = -8;
		        		yLabel=-10;
		        		break;
		    		default:
	        	}
            }
	        	
        	iconGfx.drawImage(icon.getImage(), getIconWidth() * idx + x, 0 + y + yIcon);
            if (GameController.isOptionEnabled(GameController.SuperLemminiTooOption.ICON_LABELS))
            	iconGfx.drawImage(iconLabel.getImage(), getIconWidth() * idx + x, 0 + y + yLabel);
        }
    }
    
    /**
     * Get the selected skill icon.
     * @return the selected skill icon if one is pressed, or null if none is pressed
     */
    static IconType getPressedIcon() {
        return pressedIcon;
    }
    
    static IconType getNextRadioIcon(IconType type) {
    	List<IconType> skillOrder = SkillIconOrder();
    	int skillIdx = skillOrder.indexOf(type);
    	if (skillIdx == -1) {
    		//outside the range of skills
    		return null;
    	}
    	
    	//get the next index up (if we're at the end, get the 1st index)
    	int nextSkillIdx;
    	if (skillIdx == skillOrder.size() - 1) {
    		nextSkillIdx = 0;
    	} else {
    		nextSkillIdx = skillIdx + 1;
    	}
    	
    	return skillOrder.get(nextSkillIdx);
    }
    
    static IconType getPreviousRadioIcon(IconType type) {
    	List<IconType> skillOrder = SkillIconOrder();
    	int skillIdx = skillOrder.indexOf(type);
    	if (skillIdx == -1) {
    		//outside the range of skills
    		return null;
    	}
    	
    	//get the next index up (if we're at the start, get the last index)
    	int nextSkillIdx;
    	if (skillIdx == 0) {
    		nextSkillIdx = skillOrder.size() - 1;
    	} else {
    		nextSkillIdx = skillIdx - 1;
    	}
    	
    	return skillOrder.get(nextSkillIdx);
    }
    
    /**
     * Reset icon bar.
     */
    static void reset() {
        for (int i = 0; i < CurrentIconOrder().size(); i++) {
            setIconFrame(i, 0);
            drawIcon(i);
        }
        pressedIcon = null;
    }
    
    /**
     * Redraws the icon bar, with current states.
     */
    static void redraw() {
        if (iconGfx != null) {
            iconGfx.dispose();
        }
        List<IconType> iconOrder = CurrentIconOrder();
        iconImg = ToolBox.createLemmImage(getIconWidth() * (iconOrder.size()), getIconHeight());
        iconGfx = iconImg.createGraphicsContext();
        for (int i = 0; i < iconOrder.size(); i++) {
            drawIcon(i);
        }
        pressedIcon = null;
    }
    
    /**
     * Returns the pitch of the tone to play when clicking the button.
     * @param type
     * @return
     */
    static int GetPitch(IconType type) {
		List<IconType> iconOrder = CurrentIconOrder();
		int idx = iconOrder.indexOf(type);
		if (idx == -1)
			return DEFAULT_PITCH;
		
		switch (type) {
    		case MINUS:
    		case PLUS:
    		case EMPTY:
    			return DEFAULT_PITCH;
    		default:
    			int pitch = 0;
    			for (int i = 0; i < CurrentIconOrder().size(); i++) {
    				IconType tmpType = iconOrder.get(i);
    				if (tmpType == type) {
    					return pitch;
    				} else if (tmpType != IconType.MINUS && tmpType != IconType.PLUS && tmpType != IconType.EMPTY) {
    					pitch++;
    				}
    			}
    	}
    	
    	return DEFAULT_PITCH;
    }
    
}
