package lemmini.game;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import lemmini.gameutil.Sprite;
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
        /** an empty icon (not used) */
        EMPTY (DEFAULT_PITCH),
        /** invalid (no such icon) */
        INVALID (DEFAULT_PITCH);

        private static final Map<Integer, Type> LOOKUP = new HashMap<>();
        
        private final int pitch;

        static {
            for (Type s : EnumSet.allOf(Type.class)) {
                LOOKUP.put(s.ordinal(), s);
            }
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
    private static final int FIRST_RADIO = Type.CLIMB.ordinal();
    /** last radio button */
    private static final int LAST_RADIO = Type.DIG.ordinal();
    /** last icon to be drawn */
    private static final int LAST_DRAWN = Type.FFWD.ordinal();

    /** array of Sprites that contains the icons */
    private static Sprite[] icons;
    /** buffered image that contains the whole icon bar in its current state */
    private static Image iconImg;
    /** graphics object used to draw on iconImg */
    private static GraphicsContext iconGfx;


    /**
     * Initialization.
     * @throws ResourceException
     */
    public static void init() throws ResourceException {
        iconImg = ToolBox.createTranslucentImage(WIDTH * (1 + LAST_DRAWN), HEIGHT);
        iconGfx = iconImg.createGraphicsContext();
        icons = new Sprite[15];
        Type[] iconTypes = Type.values();
        for (int i = 0; i <= LAST_DRAWN; i++) {
            String fn = Core.findResource("gfx/icons/icon_" + iconTypes[i].name().toLowerCase() + ".png", Core.IMAGE_EXTENSIONS);
            if (fn == null) {
                throw new ResourceException("gfx/icons/icon_" + iconTypes[i].name().toLowerCase() + ".png");
            }
            Image sourceImg = Core.loadTranslucentImage(fn);
            icons[i] = new Sprite(sourceImg, 2);
            iconGfx.drawImage(icons[i].getImage(), WIDTH * i, 0);
        }
    }

    /**
     * Get Icon type by x position.
     * @param x x position inside bar in pixels
     * @return Icon type
     */
    public static Type getType(final int x) {
        if (x >= (LAST_DRAWN + 1) * WIDTH) {
            return Type.INVALID; // invalid
        }
        return Type.get(x / WIDTH);
    }

    /**
     * Get buffered image that contains the whole icon bar in its current state.
     * @return image of icon bar
     */
    public static Image getImg() {
        return iconImg;
    }

    /**
     * Get pressed state of the given Icon
     * @param type
     * @return
     */
    static boolean isPressed(Type type) {
        int idx = type.ordinal();
        if (idx > LAST_DRAWN) {
            return false;
        }
        return (icons[idx].getFrameIdx() == 1);
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
                icons[idx].setFrameIdx((icons[idx].getFrameIdx() == 0) ? 1 : 0); // toggle
                if (idx <= LAST_DRAWN) {
                    iconGfx.drawImage(icons[idx].getImage(), WIDTH * idx, 0);
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
                for (int i = FIRST_RADIO; i <= LAST_RADIO; i++) {
                    if (i != idx) {
                        icons[i].setFrameIdx(0);
                        iconGfx.drawImage(icons[i].getImage(), WIDTH * i, 0);
                    }
                }
                /* falls through */
            case MINUS:
            case PLUS:
            case NUKE:
                icons[idx].setFrameIdx(1); // set "pressed" frame
                if (idx <= LAST_DRAWN) {
                    iconGfx.drawImage(icons[idx].getImage(), WIDTH * idx, 0);
                }
                break;
            default:
                break;
        }
    }

    /**
     * Release Icon.
     * @param type Icon Type
     */
    static void release(final Type type) {
        int idx = type.ordinal();
        switch (type) {
            case MINUS:
            case PLUS:
                icons[idx].setFrameIdx(0); // set "released" frame
                if (idx <= LAST_DRAWN) {
                    iconGfx.drawImage(icons[idx].getImage(), WIDTH * idx, 0);
                }
                break;
            case NUKE:
                if (!GameController.isNuked()) {
                    icons[idx].setFrameIdx(0); // set "released" frame
                    if (idx <= LAST_DRAWN) {
                        iconGfx.drawImage(icons[idx].getImage(), WIDTH * idx, 0);
                    }
                }
                break;
            default:
                break;
        }
    }

    /**
     * Reset Icon bar.
     */
    static void reset() {
        for (int i = 0; i <= LAST_DRAWN; i++) {
            icons[i].setFrameIdx(0);
            iconGfx.drawImage(icons[i].getImage(), WIDTH * i, 0);
        }
    }
}
