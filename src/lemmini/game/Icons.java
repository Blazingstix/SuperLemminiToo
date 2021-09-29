package lemmini.game;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
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

    /** icon width in pixels */
    public static final int WIDTH = 32;
    /** icon height in pixels */
    public static final int HEIGHT = 40;

    /** Icon types */
    public static enum Type {
        /** minus icon */
        MINUS (DEFAULT_PITCH),
        /** plus icon */
        PLUS (DEFAULT_PITCH),
        /** climber icon */
        CLIMB (0),
        /** floater icon */
        FLOAT (2),
        /** bomber icon */
        BOMB (4),
        /** blocker icon */
        BLOCK (5),
        /** builder icon */
        BUILD (7),
        /** basher icon */
        BASH (9),
        /** miner icon */
        MINE (11),
        /** digger icon */
        DIG (12),
        /** pause icon */
        PAUSE (14),
        /** nuke icon */
        NUKE (16),
        /** fast forward icon */
        FFWD (17),
        /** vertical scroll lock icon */
        VLOCK (19),
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
    static final int NUM_RADIO = LAST_RADIO - FIRST_RADIO + 1;
    /** last icon to be drawn */
    static final int LAST_DRAWN = Type.VLOCK.ordinal();

    /** list of Sprites that contains the icons */
    private static final List<Sprite> icons = new ArrayList<>(Type.values().length);
    /** buffered image that contains the whole icon bar in its current state */
    private static LemmImage iconImg;
    /** graphics object used to draw on iconImg */
    private static GraphicsContext iconGfx = null;
    private static Type pressedIcon = null;


    /**
     * Initialization.
     * @throws ResourceException
     */
    public static void init() throws ResourceException {
        icons.clear();
        if (iconGfx != null) {
            iconGfx.dispose();
        }
        iconImg = ToolBox.createTranslucentImage(WIDTH * (1 + LAST_DRAWN), HEIGHT);
        iconGfx = iconImg.createGraphicsContext();
        Type[] iconTypes = Type.values();
        for (int i = 0; i <= LAST_DRAWN; i++) {
            Resource res = Core.findResource(
                    "gfx/icons/icon_" + iconTypes[i].name().toLowerCase(Locale.ROOT) + ".png",
                    true, Core.IMAGE_EXTENSIONS);
            LemmImage sourceImg = Core.loadTranslucentImage(res);
            Sprite icon = new Sprite(sourceImg, 2, 1);
            icons.add(icon);
            iconGfx.drawImage(icon.getImage(), WIDTH * i, 0);
        }
    }

    /**
     * Get icon type by x position.
     * @param x x position inside bar in pixels
     * @return Icon type
     */
    public static Type getType(final int x) {
        if (x < 0 || x >= (LAST_DRAWN + 1) * WIDTH) {
            return null; // invalid
        }
        return Type.get(x / WIDTH);
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
        return (icons.get(idx).getFrameIdx() == 1);
    }

    /**
     * Press down icon.
     * @param type Icon Type
     */
    static void press(final Type type) {
        int idx = type.ordinal();
        Sprite icon;
        switch (type) {
            case PAUSE:
            case FFWD:
            case VLOCK:
                icon = icons.get(idx);
                icon.setFrameIdx((icon.getFrameIdx() == 0) ? 1 : 0); // toggle
                if (idx <= LAST_DRAWN) {
                    iconGfx.drawImage(icon.getImage(), WIDTH * idx, 0);
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
                for (ListIterator<Sprite> lit = icons.listIterator(FIRST_RADIO); lit.nextIndex() <= LAST_RADIO; ) {
                    int i = lit.nextIndex();
                    icon = lit.next();
                    if (i != idx) {
                        icon.setFrameIdx(0);
                        iconGfx.drawImage(icon.getImage(), WIDTH * i, 0);
                    }
                }
                pressedIcon = type;
                /* falls through */
            case MINUS:
            case PLUS:
            case NUKE:
                icon = icons.get(idx);
                icon.setFrameIdx(1); // set "pressed" frame
                iconGfx.drawImage(icon.getImage(), WIDTH * idx, 0);
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
        Sprite icon;
        switch (type) {
            case MINUS:
            case PLUS:
                icon = icons.get(idx);
                icon.setFrameIdx(0); // set "released" frame
                if (idx <= LAST_DRAWN) {
                    iconGfx.drawImage(icon.getImage(), WIDTH * idx, 0);
                }
                break;
            case NUKE:
                if (!GameController.isNuked()) {
                    icon = icons.get(idx);
                    icon.setFrameIdx(0); // set "released" frame
                    if (idx <= LAST_DRAWN) {
                        iconGfx.drawImage(icon.getImage(), WIDTH * idx, 0);
                    }
                }
                break;
            default:
                break;
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
        for (ListIterator<Sprite> lit = icons.listIterator(); lit.hasNext(); ) {
            int i = lit.nextIndex();
            Sprite icon = lit.next();
            icon.setFrameIdx(0);
            iconGfx.drawImage(icon.getImage(), WIDTH * i, 0);
        }
        pressedIcon = null;
    }
}
