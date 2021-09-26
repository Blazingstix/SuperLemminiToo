package lemmini.game;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import lemmini.Lemmini;
import lemmini.graphics.Image;
import lemmini.sound.Sound;
import lemmini.tools.Props;
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
 * Implements a Lemming.
 *
 * @author Volker Oth
 */
public class Lemming {
    
    /** name of the configuration file */
    private static final String LEMM_INI_STR = "gfx/lemming/lemming.ini";
    /** number of resources (animations/names) */
    private static final int NUM_RESOURCES = 18;

    /** Lemming skill type */
    public static enum Type {
        /** the typical Lemming */
        WALKER ("WALKER", 8, true, true, 0, 0),
        /** a falling Lemming */
        FALLER ("FALLER", 4, true, true, 0, 0),
        /** a climbing Lemming */
        CLIMBER ("CLIMBER", 8, true, true, 0, 0),
        /** a climbing Lemming returning to the ground */
        FLIPPER ("FLIPPER", 8, true, false, 0, 0),
        /** a Lemming with a parachute */
        FLOATER ("FLOATER", 16, true, false, 0, 0),
        /** a Lemming blowing itself up */
        FLAPPER ("FLAPPER", 16, false, false, 0, 0),
        /** a Lemming dying from a fall */
        SPLATTER ("SPLATTER", 16, false, false, 0, 0),
        /** a Lemming blocking the way for the other Lemmings */
        BLOCKER ("BLOCKER", 16, false, true, 3, 0),
        /** a Lemming drowning in the water */
        DROWNER ("DROWNER", 16, false, false, 0, 0),
        /** a Lemming killed by a trap */
        FRIER ("FRIER", 16, false, false, 0, 0),
        /** a Lemming exiting the level */
        HOMER ("HOMER", 8, false, false, 0, 0),
        /** a Lemming building stairs */
        BUILDER ("BUILDER", 16, true, true, 1, 9),
        /** a builder Lemmings with no more steps in his backpack */
        SHRUGGER ("SHRUGGER", 8, true, false, 0, 0),
        /** a Lemming digging a hole in the ground */
        DIGGER ("DIGGER", 16, false, true, 1, 8),
        /** a Lemming bashing the ground before it */
        BASHER ("BASHER", 32, true, true, 4, 8),
        /** a Lemming digging a mine with a pick */
        MINER ("MINER", 24, true, true, 2, 12),
        /** a Lemming jumping over a small obstacle */
        JUMPER ("JUMPER", 1, true, true, 0, 0),
        EXPLODER ("EXPLODER", 0, false, false, 1, 0),
        /* types without a separate animation */
        /** a Lemming that is nuked */
        NUKE ("", 0, false, false, 0, 0),
        /** a blocker that is told to explode */
        FLAPPER_BLOCKER ("FLAPPER", 0, false, false, 0, 0),
        /** a floater before the parachute opened completely */
        FLOATER_START ("FLOATER", 0, false, false, 0, 0),
        /** undefined */
        UNDEFINED ("", 0, false, false, 0, 0);
        
        private final String name;
        private final int frames;
        private final boolean bidirectional;
        private final boolean loop;
        private final int maskFrames;
        private final int maskStep;
        
        private Type(String name, int frames, boolean bidirectional, boolean loop,
                int maskFrames, int maskStep) {
            this.name = name;
            this.frames = frames;
            this.bidirectional = bidirectional;
            this.loop = loop;
            this.maskFrames = maskFrames;
            this.maskStep = maskStep;
        }
    }

    /** Lemming heading */
    public static enum Direction {
        RIGHT,
        LEFT,
        NONE;

        private static final Map<Integer, Direction> lookup = new HashMap<>();

        static {
            for (Direction s : EnumSet.allOf(Direction.class)) {
                lookup.put(s.ordinal(), s);
            }
        }

        /**
         * Reverse lookup implemented via hashtable.
         * @param val Ordinal value
         * @return Parameter with ordinal value val
         */
        public static Direction get(final int val) {
            return lookup.get(val);
        }

    }

    /** animation type */
    static enum Animation {
        NONE,
        LOOP,
        ONCE
    }

    /** a walker walks one pixel per frame */
    private static final int WALKER_STEP = 1;
    /** a climber climbs up 1 pixel per frame during the second half of the animation */
    private static final int CLIMBER_STEP = 1;
    /** at this height a walker will turn around */
    private static final int WALKER_OBSTACLE_HEIGHT = 14;
    /** check N pixels above the lemming's feet */
    private static final int BASHER_CHECK_STEP = 12;
    private static final int BASHER_CHECK_STEP_STEEL = 16;
    private static final int BASHER_CHECK_STEP_STEEL_LOW = 3;
    /** from this on a basher will become a faller */
    private static final int BASHER_FALL_DISTANCE = 6;
    private static final int MINER_CHECK_STEP_STEEL = 16;
    /** from this on a miner will become a faller */
    private static final int MINER_FALL_DISTANCE = 1;
    /** a faller falls down three pixels per frame */
    private static final int FALLER_STEP = 3;
    /** a floater falls down two pixels per frame */
    private static final int FLOATER_STEP = 2;
    private static final int FLOATER_STEP_SLOW = 1;
    /** a jumper moves up two pixels per frame */
    private static final int JUMPER_STEP = 2;
    /** if a walker jumps up 6 pixels, it becomes a jumper */
    private static final int JUMPER_JUMP = 6;
    private static final int DIGGER_STEP = 2;
    /** pixels a floater falls before the parachute begins to open */
    private static final int FALL_DISTANCE_FLOAT = 32;
    /** number of free pixels below needed to convert a lemming to a faller */
    private static final int FALL_DISTANCE_FALL  = 8;
    /** used as "free below" value to convert most skills into a faller */
    private static final int FALL_DISTANCE_FORCE_FALL = 2 * FALL_DISTANCE_FALL;
    /** number of steps a builder can build */
    private static final int STEPS_MAX = 12;
    /** number of steps before the warning sound is played */
    private static final int STEPS_WARNING = 9;
    /** Lemmini runs at 33.33fps instead of 16.67fps */
    private static final int TIME_SCALE = 2;
    /** explosion counter is decreased every 31.2 frames */
    private static final int[] MAX_EXPLODE_CTR = {31, 31, 32, 31, 31};
    private static final int EXPLODER_LIFE = 102;
    private static final int TOP_BOUNDARY = 8;
    private static final int BOTTOM_BOUNDARY = 20;
    private static final int LEFT_BOUNDARY = 0;
    private static final int RIGHT_BOUNDARY = -16;
    private static final int DEF_TEMPLATE_COLOR = 0xffff00ff;

    /** resource (animation etc.) for the current Lemming */
    private LemmingResource lemRes;
    /** animation frame */
    private int frameIdx;
    /** x coordinate of foot in pixels */
    private int x;
    /** y coordinate of foot in pixels */
    private int y;
    /** Lemming's heading */
    private Direction dir;
    /** Lemming's skill/type */
    private Type type;
    /** counter used for internal state changes */
    private int counter;
    /** another counter used for internal state changes */
    private int counter2;
    /** explosion counter when nuked */
    private int explodeNumCtr;
    /** Lemming can float */
    private boolean canFloat;
    /** Lemming can climb */
    private boolean canClimb;
    /** Lemming can change its skill */
    private boolean canChangeSkill;
    private boolean flapper;
    private boolean drowner;
    private boolean homer;
    /** Lemming is to be nuked */
    private boolean nuke;
    /** Lemming has died */
    private boolean hasDied;
    /** Lemming has left the level */
    private boolean hasLeft;
    /** counter used to manage the explosion */
    private int explodeCtr;
    /** counter used to display the select image in replay mode */
    private int selectCtr;

    /** static array of resources for each Lemming skill/type */
    private static LemmingResource[] lemmings;
    /** font used for the explosion counter */
    private static ExplodeFont explodeFont;
    private static int templateColor;
    private static int templateColor2;

    /**
     * Constructor: Create Lemming
     * @param sx x coordinate of foot
     * @param sy y coordinate of foot
     * @param d initial direction
     */
    public Lemming(final int sx, final int sy, final Direction d) {
        frameIdx = 0;
        type = Type.FALLER; // always start with a faller
        lemRes = lemmings[getOrdinal(type)];
        counter = 0;
        explodeNumCtr = 0;
        selectCtr = 0;
        dir = d;  // always start walking to the right
        x = sx;
        y = sy;
        canFloat = false; // new lemming can't float
        canClimb = false; // new lemming can't climb
        canChangeSkill = false; // a faller can not change the skill to e.g. builder
        hasDied = false;  // not yet
        hasLeft = false;  // not yet
        flapper = false;
        drowner = false;
        homer = false;
        nuke = false;
    }

    /**
     * Get number of Lemming type in internal resource array.
     * @param t Type
     * @return resource number for type
     */
    public static int getOrdinal(final Type t) {
        switch (t) {
            case FLAPPER_BLOCKER:
                return Type.FLAPPER.ordinal();
            case FLOATER_START:
                return Type.FLOATER.ordinal();
            default:
                return t.ordinal();
        }
    }

    /**
     * Update animation, move Lemming, check state transitions.
     */
    public void animate() {
        int free;
        Type oldType = type;
        Type newType = type;
        int oldX = x;
        boolean explode = false;
        // first check explode state
        if (explodeNumCtr != 0) {
            if (++explodeCtr >= MAX_EXPLODE_CTR[explodeNumCtr - 1]) {
                explodeCtr -= MAX_EXPLODE_CTR[explodeNumCtr - 1];
                explodeNumCtr--;
                if (explodeNumCtr==0) {
                    explode = true;
                }
            }
        }
        if (selectCtr>0) {
            selectCtr--;
        }
        // lemming state machine
        switch (type) {

            case FLIPPER:
                {
                    if (explode) {
                        newType = Type.FLAPPER;
                        break;
                    }
                    int idx = frameIdx + 1;
                    switch (idx) {
                        case 1 * TIME_SCALE:
                        case 2 * TIME_SCALE:
                        case 3 * TIME_SCALE:
                        case 4 * TIME_SCALE:
                            y -= 4;
                            break;
                        default:
                            break;
                    }
                    turnedByBlocker();
                    break;
                }

            case FALLER:
                if (explode) {
                    newType = Type.EXPLODER;
                    break;
                }
                free = freeBelow(FALLER_STEP);
                if (free == FALL_DISTANCE_FORCE_FALL) {
                    y += FALLER_STEP;
                } else {
                    y += free; // max: FALLER_STEP
                }
                if (!crossedLowerBorder()) {
                    counter += free; // fall counter
                    // check conversion to floater
                    if (canFloat && counter >= FALL_DISTANCE_FLOAT) {
                        newType = Type.FLOATER_START;
                        counter2 = 0; // used for parachute opening "jump" up
                    } else if (free == 0) { // check ground hit
                        if (counter > GameController.getLevel().getMaxFallDistance()) {
                            newType = Type.SPLATTER;
                        } else {
                            newType = Type.WALKER;
                            counter = 0;
                        }
                    }
                }
                turnedByBlocker();
                break;

            case JUMPER:
                {
                    if (explode) {
                        newType = Type.FLAPPER;
                        break;
                    }
                    int levitation = aboveGround();
                    if (levitation > JUMPER_STEP) {
                        y -= JUMPER_STEP;
                    } else {
                        // conversion to walker
                        y -= levitation;
                        newType = Type.WALKER;
                    }
                    turnedByBlocker();
                    break;
                }

            case WALKER: 
                {
                    if (explode) {
                        newType = Type.FLAPPER;
                        break;
                    }
                    if (dir == Direction.RIGHT) {
                        x += WALKER_STEP;
                    } else if (dir == Direction.LEFT) {
                        x -= WALKER_STEP;
                    }
                    if (flipDirBorder()) {
                        break;
                    }
                    // check
                    free = freeBelow(FALL_DISTANCE_FALL);
                    if (free >= FALL_DISTANCE_FALL) {
                        y += FALLER_STEP;
                    } else {
                        y += free;
                    }
                    int levitation = aboveGround();
                    // check for flip direction
                    if (levitation < WALKER_OBSTACLE_HEIGHT && y >= 8) {
                        if (levitation >= JUMPER_JUMP) {
                            y -= JUMPER_STEP;
                            newType = Type.JUMPER;
                            break;
                        } else {
                            y -= levitation;
                        }
                    } else {
                        if (canClimb && y >= WALKER_OBSTACLE_HEIGHT) {
                            newType = Type.CLIMBER;
                            break;
                        } else {
                            flipDir();
                        }
                    }
                    if (free > 0) {
                        // check for conversion to faller
                        if (free >= FALL_DISTANCE_FALL) {
                            counter = FALLER_STEP + 1; // @check: is this OK? increasing counter, but using free???
                            newType = Type.FALLER;
                            y += 1;
                        }
                    }
                    turnedByBlocker();
                    break;
                }

            case FLOATER:
                if (explode) {
                    newType = Type.EXPLODER;
                    break;
                }
                free = freeBelow(FLOATER_STEP);
                if (free == FALL_DISTANCE_FORCE_FALL) {
                    y += FLOATER_STEP;
                } else {
                    y += free; // max: FLOATER_STEP
                }
                if (!crossedLowerBorder()) {
                    counter += free; // fall counter
                    // check ground hit
                    if (free == 0) {
                        newType = Type.WALKER;
                        counter = 0;
                    }
                }
                turnedByBlocker();
                break;

            case FLOATER_START:
                if (explode) {
                    newType = Type.EXPLODER;
                    break;
                }
                free = freeBelow(FALLER_STEP);
                switch (counter2++) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                        if (free >= FALLER_STEP) {
                            y += FALLER_STEP;
                        } else {
                            y += free;
                        }
                        break;
                    case 8:
                    case 9:
                        y -= FLOATER_STEP_SLOW;
                        break;
                    case 10:
                    case 11:
                        break;
                    case 12:
                    case 13:
                    case 14:
                    case 15:
                        if (free >= FLOATER_STEP_SLOW) {
                            y += FLOATER_STEP_SLOW;
                        } else {
                            y += free;
                        }
                        if (counter2 - 1 == 15) {
                            type = Type.FLOATER;
                        }
                        break;
                    default:
                        type = Type.FLOATER;
                        break;
                }
                if (!crossedLowerBorder()) {
                    counter += StrictMath.min(FLOATER_STEP, free); // fall counter
                    // check ground hit
                    if (free == 0) {
                        newType = Type.WALKER;
                        counter = 0;
                    }
                }
                turnedByBlocker();
                break;
            case CLIMBER: 
                {
                    if (explode) {
                        newType = Type.FLAPPER;
                        break;
                    }
                    int idx = frameIdx + 1;
                    if (idx >= lemRes.frames * TIME_SCALE) {
                        idx = 0;
                    }
                    switch (idx) {
                        case 0:
                        case 1:
                        case 2:
                        case 3:
                        case 4:
                        case 5:
                        case 6:
                        case 7:
                            if (reachedPlateau(idx + 13)) {
                                counter = 0;
                                y += -idx + 4;
                                newType = Type.FLIPPER;
                            }
                            break;
                        case 8:
                        case 9:
                        case 10:
                        case 11:
                        case 12:
                        case 13:
                        case 14:
                        case 15:
                            y -= CLIMBER_STEP;
                            if (!freeAboveClimber()) {
                                flipDir();
                                if (dir == Direction.LEFT) {
                                    x -= 1;
                                } else {
                                    x += 1;
                                }
                                newType = Type.FALLER;
                                counter = 0;
                            }
                            break;
                        default:
                            break;
                    }
                    turnedByBlocker();
                    break;
                }

            case DIGGER:
                if (explode) {
                    newType = Type.FLAPPER;
                    break;
                }
                turnedByBlocker();
                break;

            case BASHER: 
                {
                    if (explode) {
                        newType = Type.FLAPPER;
                        break;
                    }

                    Mask m;
                    int checkMask;
                    int idx = frameIdx + 1;
                    if (idx >= lemRes.frames * TIME_SCALE) {
                        idx = 0;
                    }
                    switch (idx) {
                        case 2 * TIME_SCALE:
                        case 3 * TIME_SCALE:
                        case 4 * TIME_SCALE:
                        case 5 * TIME_SCALE: {
                            //    bash mask should have the same height as the lemming
                            m = lemRes.getMask(dir);
                            checkMask = 0;
                            if (!GameController.getLevel().getClassicSteel()) {
                                checkMask |= Stencil.MSK_STEEL;
                                checkMask |= (dir == Direction.LEFT) ? Stencil.MSK_NO_BASH_LEFT : Stencil.MSK_NO_BASH_RIGHT;
                            }
                            if (y >= BASHER_CHECK_STEP) {
                                m.eraseMask(screenMaskX(), screenMaskY(), idx / TIME_SCALE - 2, checkMask);
                            }
                            if (idx == 5 * TIME_SCALE) {
                                // check for conversion to walker because there are no bricks left
                                if (!canBash()) {
                                    // no bricks any more
                                    newType = Type.WALKER;
                                }
                            }
                            break;}
                        case 18 * TIME_SCALE:
                        case 19 * TIME_SCALE:
                        case 20 * TIME_SCALE:
                        case 21 * TIME_SCALE: {
                            //    bash mask should have the same height as the lemming
                            m = lemRes.getMask(dir);
                            checkMask = 0;
                            if (!GameController.getLevel().getClassicSteel()) {
                                checkMask |= Stencil.MSK_STEEL;
                                checkMask |= (dir == Direction.LEFT) ? Stencil.MSK_NO_BASH_LEFT : Stencil.MSK_NO_BASH_RIGHT;
                            }
                            if (y >= BASHER_CHECK_STEP) {
                                m.eraseMask(screenMaskX(), screenMaskY(), idx / TIME_SCALE - 18, checkMask);
                            }
                            break; }
                        case 11 * TIME_SCALE:
                        case 12 * TIME_SCALE:
                        case 13 * TIME_SCALE:
                        case 14 * TIME_SCALE:
                        case 15 * TIME_SCALE:
                        case 27 * TIME_SCALE:
                        case 28 * TIME_SCALE:
                        case 29 * TIME_SCALE:
                        case 30 * TIME_SCALE:
                        case 31 * TIME_SCALE:
                            for (int i = 0; i < 2; i++) {
                                if (dir == Direction.RIGHT) {
                                    x += 1;
                                } else {
                                    x -= 1;
                                }
                                // check for conversion to faller
                                free = freeBelow(BASHER_FALL_DISTANCE);
                                if (free == BASHER_FALL_DISTANCE) {
                                    y += FALLER_STEP;
                                    newType = Type.FALLER;
                                    break;
                                } else {
                                    y += free;
                                }
                            }
                            break;
                        default:
                            break;
                    }
                    turnedByBlocker();
                    if (!canBashSteel(true)) {
                        flipDir();
                        newType = Type.WALKER;
                    }
                    if (flipDirBorder()) {
                        newType = Type.WALKER;
                    }
                    break;
                }

            case MINER: 
                {
                    if (explode) {
                        newType = Type.FLAPPER;
                        break;
                    }
                    Mask m;
                    int idx = frameIdx + 1;
                    if (idx >= lemRes.frames * TIME_SCALE) {
                        idx = 0;
                    }
                    switch (idx) {
                        case 0 * TIME_SCALE:
                            y += 2;
                            break;
                        case 1 * TIME_SCALE:
                        case 2 * TIME_SCALE:
                            // check for steel in mask
                            m = lemRes.getMask(dir);
                            int checkMask = 0;
                            if (!GameController.getLevel().getClassicSteel()) {
                                checkMask |= Stencil.MSK_STEEL;
                                checkMask |= (dir == Direction.LEFT) ? Stencil.MSK_NO_BASH_LEFT : Stencil.MSK_NO_BASH_RIGHT;
                            }
                            if (y >= TOP_BOUNDARY + 10) {
                                m.eraseMask(screenMaskX(), screenMaskY(), idx / TIME_SCALE - 1, checkMask);
                            }
                            break;
                        case 3 * TIME_SCALE:
                            y += 2;
                            /* falls through */
                        case 15 * TIME_SCALE:
                            if (dir == Direction.RIGHT) {
                                x += 4;
                            } else {
                                x -= 4;
                            }
                            // check for conversion to faller
                            free = freeBelow(MINER_FALL_DISTANCE);
                            if (free >= MINER_FALL_DISTANCE) {
                                if (free == FALL_DISTANCE_FORCE_FALL) {
                                    y += FALLER_STEP;
                                } else {
                                    y += free;
                                }
                                newType = Type.FALLER;
                                break;
                            }
                            if (!canMine(false, true)) {
                                flipDir();
                                newType = Type.WALKER;
                            }
                            break;
                        default:
                            break;
                    }
                    turnedByBlocker();
                    break;
                }

            case SHRUGGER:
                if (explode) {
                    newType = Type.FLAPPER;
                }
                turnedByBlocker();
                break;
            case BUILDER: 
                {
                    if (explode) {
                        newType = Type.FLAPPER;
                        break;
                    }
                    int idx = frameIdx + 1;
                    if (idx >= lemRes.frames * TIME_SCALE) {
                        // step created -> move up
                        counter++; // step counter;
                        y -= 2; // step up
                        for (int i = 0; i < 4; i++) {
                            if (dir == Direction.RIGHT) {
                                x += 1; // step forward
                            } else {
                                x -= 1;
                            }
                            int levitation = aboveGround(); // should be 0, if not, we built into a wall -> stop
                            // check for conversion to walker
                            if (counter < STEPS_MAX && ((i == 3 && !freeAboveBuilder()) || levitation > 0)) {
                                newType = Type.WALKER;
                                flipDir();
                                break;
                            }
                        }
                        // check for last step used
                        if (counter >= STEPS_MAX) {
                            newType = Type.SHRUGGER;
                            break;
                        }
                        if (y < TOP_BOUNDARY + 2) {
                            newType = Type.WALKER;
                            break;
                        }
                    } else if (idx == 9 * TIME_SCALE) {
                        // stair mask is the same height as a lemming
                        Mask m;
                        m = lemRes.getMask(dir);
                        m.paintStep(screenMaskX(), screenMaskY(), 0, GameController.getLevel().getDebrisColor());
                    } else if (idx == 10 * TIME_SCALE) {
                        if (counter >= STEPS_WARNING) {
                            GameController.sound.play(Sound.Effect.STEP_WARNING, getPan());
                        }
                    }
                    turnedByBlocker();
                    break;
                }

            case BLOCKER:
                {
                    if (explode) {
                        // don't erase blocker mask!
                        newType = Type.FLAPPER_BLOCKER;
                        break;
                    }
                    // check for conversion to faller
                    free = freeBelow(FALLER_STEP);
                    if (free > 0) {
                        if (free == FALL_DISTANCE_FORCE_FALL) {
                            y += FALLER_STEP;
                        } else {
                            y += free;
                        }
                        counter += free;
                        if (counter >= FALL_DISTANCE_FALL) {
                            newType = Type.FALLER;
                        } else {
                            newType = Type.WALKER;
                        }
                        // conversion to faller or walker -> erase blocker mask
                        eraseBlockerMask();
                    } else {
                        counter = 0;
                    }
                    break;
                }

            case FLAPPER_BLOCKER:
                // don't erase blocker mask before blocker finally explodes or falls
                free = freeBelow(FALLER_STEP);
                if (free > 0) {
                    // blocker falls -> erase mask and convert to normal blocker.
                    eraseBlockerMask();
                    type = Type.FLAPPER;
                    // fall through
                } else {
                    int idx = frameIdx + 1;
                    if (idx == 5 * TIME_SCALE && !nuke) { 
                        GameController.sound.play(Sound.Effect.OHNO, getPan());
                    }
                    break;
                }
                /* falls through */
            case FLAPPER: 
                {
                    int idx = frameIdx + 1;
                    if (idx == 5 * TIME_SCALE && !nuke) { 
                        GameController.sound.play(Sound.Effect.OHNO, getPan());
                    }
                    free = freeBelow(FALLER_STEP);
                    if (free == FALL_DISTANCE_FORCE_FALL) {
                        y += FALLER_STEP;
                    } else {
                        y += free;
                    }
                    crossedLowerBorder();
                    break;
                }
            case DROWNER:
                if (explode) {
                    explode();
                    newType = Type.EXPLODER;
                    counter = 0;
                    break;
                }
                if (!flapper) {
                    if (dir == Direction.RIGHT) {
                        if (x < GameController.getWidth() + RIGHT_BOUNDARY - 16
                                && (GameController.getStencil().getMask(x + 16, y) & Stencil.MSK_BRICK) == 0) {
                            x += WALKER_STEP;
                        }
                    } else if (dir == Direction.LEFT) {
                        if (x >= LEFT_BOUNDARY + 16 &&
                                (GameController.getStencil().getMask(x - 16, y) & Stencil.MSK_BRICK) == 0) {
                            x -= WALKER_STEP;
                        }
                    }
                }
                turnedByBlocker();
                break;
            case HOMER:
                if (explode) {
                    newType = Type.FLAPPER;
                }
                break;
            case FRIER:
                {
                    if (explode) {
                        explode();
                        newType = Type.EXPLODER;
                        counter = 0;
                        break;
                    }
                    int idx = frameIdx + 1;
                    if (idx == 3 * TIME_SCALE) {
                        GameController.sound.play(Sound.Effect.DIE, getPan());
                    }
                    break;
                }
            case EXPLODER:
                counter++;
                if (counter == 1 * TIME_SCALE) {
                    Stencil stencil = GameController.getStencil();
                    Mask m = lemRes.getMask(Direction.RIGHT);
                    GameController.sound.play(Sound.Effect.EXPLODE, getPan());
                    if (!GameController.getLevel().getClassicSteel()) {
                        m.eraseMask(screenMaskX(), StrictMath.max(screenMaskY(), -8), 0, Stencil.MSK_STEEL);
                    } else if (x >= LEFT_BOUNDARY && x < GameController.getWidth() + RIGHT_BOUNDARY
                            && y < Level.DEFAULT_HEIGHT
                            && (stencil.getMask(x, y) & Stencil.MSK_STEEL) == 0
                            && (stencil.getMask(x, y) & Stencil.MSK_EXIT) == 0 && !drowner) {
                        m.eraseMask(screenMaskX(), StrictMath.max(screenMaskY(), -8), 0, 0);
                    }
                } else if (counter >= EXPLODER_LIFE) {
                    hasDied = true;
                }
                break;
            default:
                // all cases not explicitly checked above should at least explode
                if (explode) {
                    explode();
                    newType = Type.EXPLODER;
                    counter = 0;
                    break;
                }
                break;

        }
        if (y < TOP_BOUNDARY) {
            y = TOP_BOUNDARY;
        }
        
        if (!hasDied && type != Type.EXPLODER) {
            // check collision with exit and traps
            int s = stencilFoot();
            int object = objectFoot();
            switch (s & (Stencil.MSK_TRAP | Stencil.MSK_EXIT)) {
                case Stencil.MSK_TRAP_LIQUID:
                    if (type != Type.DROWNER) {
                        SpriteObject spr = GameController.getLevel().getSprObject(object);
                        if (spr != null) {
                            if (spr.canBeTriggered()) {
                                if (spr.trigger(this)) {
                                    GameController.sound.play(spr.getSound(), getPan());
                                    drowner = true;
                                    newType = Type.DROWNER;
                                }
                            } else {
                                GameController.sound.play(spr.getSound(), getPan());
                                drowner = true;
                                newType = Type.DROWNER;
                            }
                        }
                        if (type == Type.BLOCKER || type == Type.FLAPPER_BLOCKER) {
                            // erase blocker mask
                            eraseBlockerMask();
                        }
                    }
                    break;
                case Stencil.MSK_TRAP_FIRE:
                    if (type != Type.FRIER) {
                        SpriteObject spr = GameController.getLevel().getSprObject(object);
                        if (spr != null) {
                            if (spr.canBeTriggered()) {
                                if (spr.trigger(this)) {
                                    GameController.sound.play(spr.getSound(), getPan());
                                    newType = Type.FRIER;
                                }
                            } else {
                                GameController.sound.play(spr.getSound(), getPan());
                                newType = Type.FRIER;
                            }
                        }
                        if (type == Type.BLOCKER || type == Type.FLAPPER_BLOCKER) {
                            // erase blocker mask
                            eraseBlockerMask();
                        }
                    }
                    break;
                case Stencil.MSK_TRAP_REMOVE:
                    {
                        SpriteObject spr = GameController.getLevel().getSprObject(object);
                        if (spr != null) {
                            if (spr.canBeTriggered()) {
                                if (spr.trigger(this)) {
                                    GameController.sound.play(spr.getSound(), getPan());
                                    hasDied = true;
                                }
                            } else {
                                GameController.sound.play(spr.getSound(), getPan());
                                hasDied = true;
                            }
                        }
                        if (type == Type.BLOCKER || type == Type.FLAPPER_BLOCKER) {
                            // erase blocker mask
                            eraseBlockerMask();
                        }
                        break;
                    }
                case Stencil.MSK_EXIT:
                    switch (newType) {
                        case FLAPPER:
                            if (homer) {
                                break;
                            }
                            /* falls through */
                        case SPLATTER:
                            if (type == Type.SPLATTER) {
                                break;
                            }
                            /* falls through */
                        case WALKER:
                        case FLOATER:
                        case FLOATER_START:
                        case JUMPER:
                        case BASHER:
                        case MINER:
                        case BUILDER:
                        case DIGGER:
                        case DROWNER:
                            SpriteObject spr = GameController.getLevel().getSprObject(object);
                            if (spr != null) {
                                if (spr.canBeTriggered()) {
                                    if (spr.trigger(this)) {
                                        GameController.sound.play(spr.getSound(), getPan());
                                        homer = true;
                                        newType = Type.HOMER;
                                    }
                                } else {
                                    GameController.sound.play(spr.getSound(), getPan());
                                    homer = true;
                                    newType = Type.HOMER;
                                }
                            }
                            if (type == Type.BLOCKER || type == Type.FLAPPER_BLOCKER) {
                                // erase blocker mask
                                eraseBlockerMask();
                            }
                            break;
                        default:
                            break;
                    }
                    break;
                default:
                    break;
            }
        }
        // animate
        if (oldType == newType) {
            boolean trigger = false;
            switch (lemRes.animMode) {
                case LOOP:
                    if (++frameIdx >= lemRes.frames * TIME_SCALE) {
                        frameIdx = 0;
                    }
                    if (lemRes.maskStep > 0 && frameIdx % (lemRes.maskStep * TIME_SCALE) == 0) {
                        trigger = true;
                    }
                    break;
                case ONCE:
                    if (frameIdx < lemRes.frames * TIME_SCALE - 1) {
                        frameIdx++;
                    } else {
                        trigger = true;
                    }
                    break;
                default:
                    break;
            }
            if (trigger) {
                // Trigger condition reached?
                switch (type) {
                    case FLAPPER_BLOCKER:
                        eraseBlockerMask();
                        /* falls through */
                    case FLAPPER:
                        explode();
                        newType = Type.EXPLODER;
                        counter = 0;
                        break;
                    case DROWNER:
                        GameController.sound.play(Sound.Effect.DROWN, getPan());
                        /* falls through */
                    case FRIER:
                        if (flapper) {
                            explode();
                            newType = Type.EXPLODER;
                            counter = 0;
                            break;
                        }
                        /* falls through */
                    case SPLATTER:
                        hasDied = true;
                        break;
                    case HOMER:
                        if (flapper) {
                            explode();
                            newType = Type.EXPLODER;
                            counter = 0;
                        } else {
                            hasLeft = true;
                            GameController.increaseLeft();
                        }
                        break;
                    case FLOATER_START:
                        type = Type.FLOATER; // should never happen
                        /* falls through */
                    case FLOATER:
                        frameIdx = 16;
                        break;
                    case FLIPPER:
                        newType = Type.WALKER;
                        break;
                    case DIGGER: {
                        // the dig mask must be applied to the bottom of the lemming
                        Mask m = lemRes.getMask(dir);
                        
                        // check for conversion to faller
                        int freeMin = Integer.MAX_VALUE;
                        int xOld = x;
                        for (int i = -4; i < 6; i++) {
                            x = xOld + i;
                            free = freeBelow(DIGGER_STEP);
                            if (free < freeMin) {
                                freeMin = free;
                            }
                        }
                        x = xOld;
                        free = freeMin;
                        if (free > 0) {
                            //convert to faller or walker
                            newType = Type.FALLER;
                        }
                        
                        y += DIGGER_STEP; // move down
                        
                        m.eraseMask(screenMaskX(), screenMaskY(), 0, GameController.getLevel().getClassicSteel() ? 0 : Stencil.MSK_STEEL);
                        
                        // check for conversion to walker when hitting steel
                        if (!canDig(true)) {
                            newType = Type.WALKER;
                        }
                        
                        break;}
                    case SHRUGGER:
                        newType = Type.WALKER;
                        break;
                    default:
                        break;
                }
            }
        }
        changeType(oldType, newType);
    }

    /**
     * Check if a Lemming is to be turned by a blocker.
     * @return true if Lemming is to be turned, false otherwise
     */
    private boolean turnedByBlocker() {
        int s = stencilFoot();

        if ((s & Stencil.MSK_BLOCKER_LEFT) != 0 && dir == Direction.RIGHT) {
            dir = Direction.LEFT;
            return true;
        }
        if ((s & Stencil.MSK_BLOCKER_RIGHT) != 0 && dir == Direction.LEFT) {
            dir = Direction.RIGHT;
            return true;
        }
        if ((s & Stencil.MSK_BLOCKER_CENTER) != 0) {
            return false;
        }
        if ((s & Stencil.MSK_TURN_LEFT) != 0 && dir == Direction.RIGHT) {
            int id = objectFoot();
            if (id >= 0) {
                GameController.sound.play(GameController.getLevel().getSprObject(id).getSound(), getPan());
            }
            dir = Direction.LEFT;
            return true;
        }
        if ((s & Stencil.MSK_TURN_RIGHT) != 0 && dir == Direction.LEFT) {
            int id = objectFoot();
            if (id >= 0) {
                GameController.sound.play(GameController.getLevel().getSprObject(id).getSound(), getPan());
            }
            dir = Direction.RIGHT;
            return true;
        }
        return false;
    }

    /**
     * Change skill/type.
     * @param oldType old skill/type of Lemming
     * @param newType new skill/type of Lemming
     */
    private void changeType(final Type oldType, final Type newType) {
        if (oldType != newType) {
            type = newType;
            lemRes = lemmings[getOrdinal(type)];
            if (newType == Type.DIGGER) {
                frameIdx = lemRes.frames * TIME_SCALE - 1; // start digging immediately
            } else {
                frameIdx = 0;
            }
            switch (newType) {
                case SPLATTER:
                    counter = 0;
                    explodeNumCtr = 0;
                    GameController.sound.play(Sound.Effect.SPLAT, getPan());
                    break;
                case FLAPPER:
                case FLAPPER_BLOCKER:
                    flapper = true;
                    break;
                case EXPLODER:
                    counter = 0;
                    explodeNumCtr = 0;
                    explode();
                    break;
                default:
                    break;
            }

            // some types can't change the skill - check this
            switch (newType) {
                case WALKER:
                case BASHER:
                case BUILDER:
                case SHRUGGER:
                case DIGGER:
                case MINER:
                    canChangeSkill = true;
                    break;
                default:
                    canChangeSkill = false;
                    break;
            }
        }
    }

    /**
     * Let the Lemming explode.
     */
    private void explode() {
        GameController.addExplosion(midX(), midY());
    }


    /**
     * Get stencil value from the foot of the lemming
     * @return stencil value from the foot of the lemming
     */
    private int stencilFoot() {
        int xm = x;
        int ym = y;
        int retval;
        if (xm >= LEFT_BOUNDARY && xm < GameController.getWidth() + RIGHT_BOUNDARY
                && ym >= TOP_BOUNDARY && ym < Level.DEFAULT_HEIGHT) {
            retval = GameController.getStencil().getMask(xm, ym);
        } else {
            retval = Stencil.MSK_EMPTY;
        }
        return retval;
    }
    
    /**
     * Get object ID from the foot of the lemming
     * @return stencil value from the middle of the lemming
     */
    private int objectFoot() {
        int xm = x;
        int ym = y;
        int retval;
        if (xm >= LEFT_BOUNDARY && xm < GameController.getWidth() + RIGHT_BOUNDARY
                && ym >= TOP_BOUNDARY && ym < Level.DEFAULT_HEIGHT) {
            retval = GameController.getStencil().getMaskObjectID(xm, ym);
        } else {
            retval = -1;
        }
        return retval;
    }

    /**
     * Check if bashing is possible.
     * @return true if bashing is possible, false otherwise.
     */
    private boolean canBash() {
        int xm = x;
        int ypos = y - BASHER_CHECK_STEP;
        int xb;
        int bricks = 0;
        for (int i = 18; i < 22; i++) {
            if (dir == Direction.RIGHT) {
                xb = xm + i;
            } else {
                xb = xm - i + 1;
            }
            int sval = GameController.getStencil().getMask(xb, ypos);
            if ((sval & Stencil.MSK_NO_BASH_LEFT) != 0 && dir == Direction.LEFT) {
                return false;
            }
            if ((sval & Stencil.MSK_NO_BASH_RIGHT) != 0 && dir == Direction.RIGHT) {
                return false;
            }
            if ((sval & Stencil.MSK_BRICK) != 0) {
                bricks++;
            }
        }
        return bricks > 0;
    }
    
    /**
     * Check if bashing is possible.
     * @return true if bashing is possible, false otherwise.
     */
    private boolean canBashSteel(final boolean playSound) {
        int yMin = y - BASHER_CHECK_STEP_STEEL;
        int yMax = y - (GameController.getLevel().getClassicSteel()
                ? BASHER_CHECK_STEP_STEEL : BASHER_CHECK_STEP_STEEL_LOW);
        int xMin;
        int xMax;
        if (dir == Direction.RIGHT) {
            xMin = x + 16;
            xMax = x + 16 + 1;
        } else {
            xMin = x - 16;
            xMax = x - 16 + 1;
        }
        for (int yb = yMin; yb <= yMax; yb++) {
            for (int xb = xMin; xb <= xMax; xb++) {
                int sval = GameController.getStencil().getMask(xb, yb);
                if (((sval & Stencil.MSK_NO_BASH_LEFT) != 0 && dir == Direction.LEFT)
                        || ((sval & Stencil.MSK_NO_BASH_RIGHT) != 0 && dir == Direction.RIGHT)
                        || ((sval & Stencil.MSK_STEEL) != 0)) {
                    if (playSound) {
                        int id = GameController.getStencil().getMaskObjectID(xb, yb);
                        if (id >= 0) {
                            GameController.sound.play(GameController.getLevel().getSprObject(id).getSound(), getPan());
                        } else {
                            GameController.sound.play(Sound.Effect.STEEL, getPan());
                        }
                    }
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Check if digging is possible.
     * @return true if digging is possible, false otherwise.
     */
    private boolean canDig(final boolean playSound) {
        boolean classicSteel = GameController.getLevel().getClassicSteel();
        for (int i = 0; i < 2; i++) {
            for (int j = (classicSteel ? 0 : -4); j < (classicSteel ? 1 : 6); j++) {
                int ym = y + i;
                int xm = x + j;
                int sval = GameController.getStencil().getMask(xm, ym);
                if ((sval & Stencil.MSK_BRICK) != 0 && (sval & Stencil.MSK_STEEL) != 0) {
                    if (playSound) {
                        int id = GameController.getStencil().getMaskObjectID(xm, ym);
                        if (id >= 0) {
                            GameController.sound.play(GameController.getLevel().getSprObject(id).getSound(), getPan());
                        } else {
                            GameController.sound.play(Sound.Effect.STEEL, getPan());
                        }
                    }
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Check if mining is possible.
     * @return true if mining is possible, false otherwise.
     */
    private boolean canMine(final boolean start, final boolean playSound) {
        if (x < LEFT_BOUNDARY || x >= GameController.getWidth() + RIGHT_BOUNDARY) {
            if (!start && playSound) {
                GameController.sound.play(Sound.Effect.STEEL, getPan());
            }
            return false;
        }
        
        int yMin = y - MINER_CHECK_STEP_STEEL;
        int yMax = y - MINER_CHECK_STEP_STEEL + 1;
        int xMin;
        int xMax;
        if (dir == Direction.RIGHT) {
            xMin = x + 14;
            xMax = x + 14 + 1;
        } else {
            xMin = x - 14;
            xMax = x - 14 + 1;
        }
        for (int yb = yMin; yb <= yMax; yb++) {
            for (int xb = xMin; xb <= xMax; xb++) {
                int sval = GameController.getStencil().getMask(xb, yb);
                if (((sval & Stencil.MSK_NO_BASH_LEFT) != 0 && dir == Direction.LEFT)
                        || ((sval & Stencil.MSK_NO_BASH_RIGHT) != 0 && dir == Direction.RIGHT)
                        || ((sval & Stencil.MSK_STEEL) != 0)) {
                    if (playSound) {
                        int id = GameController.getStencil().getMaskObjectID(xb, yb);
                        if (id >= 0) {
                            GameController.sound.play(GameController.getLevel().getSprObject(id).getSound(), getPan());
                        } else {
                            GameController.sound.play(Sound.Effect.STEEL, getPan());
                        }
                    }
                    return false;
                }
            }
        }
        yMin = y;
        yMax = y + 1;
        xMin = x;
        xMax = x + 1;
        for (int yb = yMin; yb <= yMax; yb++) {
            for (int xb = xMin; xb <= xMax; xb++) {
                int sval = GameController.getStencil().getMask(xb, yb);
                if ((!start && (((sval & Stencil.MSK_NO_BASH_LEFT) != 0 && dir == Direction.LEFT)
                        || ((sval & Stencil.MSK_NO_BASH_RIGHT) != 0 && dir == Direction.RIGHT)))
                        || ((sval & Stencil.MSK_STEEL) != 0)) {
                    if (playSound) {
                        int id = GameController.getStencil().getMaskObjectID(xb, yb);
                        if (id >= 0) {
                            GameController.sound.play(GameController.getLevel().getSprObject(id).getSound(), getPan());
                        } else {
                            GameController.sound.play(Sound.Effect.STEEL, getPan());
                        }
                    }
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Get number of free pixels below the lemming (max of step is checked).
     * @return number of free pixels below the lemming
     */
    private int freeBelow(final int step) {
        if (x < LEFT_BOUNDARY || x >= GameController.getWidth() + RIGHT_BOUNDARY) {
            return 0;
        }
        int free = 0;
        int pos;
        Stencil stencil = GameController.getStencil();
        int yb = y;
        pos = x + yb * GameController.getWidth(); // line below the lemming
        for (int i = 0; i < step; i++) {
            if (yb + i >= Level.DEFAULT_HEIGHT) {
                return FALL_DISTANCE_FORCE_FALL; // convert most skill to faller
            }
            int s = stencil.getMask(pos);
            if ((s & Stencil.MSK_BRICK) == 0) {
                free++;
            } else {
                break;
            }
            pos += GameController.getWidth();
        }
        return free;
    }
    
    private void flipDir() {
        dir = (dir == Direction.RIGHT) ? Direction.LEFT : Direction.RIGHT;
    }

    /**
     * Check if Lemming reached the left or right border of the level and was turned.
     * @return true if lemming was turned, false otherwise.
     */
    private boolean flipDirBorder() {
        boolean flip = false;
        if (lemRes.dirs > 1) {
            if (x < LEFT_BOUNDARY && dir == Direction.LEFT) {
                x = LEFT_BOUNDARY - 1;
                flip = true;
            } else if (x >= GameController.getWidth() + RIGHT_BOUNDARY && dir == Direction.RIGHT) {
                x = GameController.getWidth() + RIGHT_BOUNDARY;
                flip = true;
            }
        }
        if (flip) {
            flipDir();
        }
        return flip;
    }

    /**
     * Get number of free pixels above the lemming (max of step is checked).
     * @return number of free pixels above the lemming
     */
    private boolean freeAboveBuilder() {
        if (dir == Direction.LEFT && x - 3 < LEFT_BOUNDARY
                || dir == Direction.RIGHT && x + 4 >= GameController.getWidth() + RIGHT_BOUNDARY) {
            return false;
        }

        int yMin = y - 18;
        int yMax = y - 17;
        int xm;
        if (dir == Direction.RIGHT) {
            xm = x + 4;
        } else {
            xm = x - 3;
        }
        Stencil stencil = GameController.getStencil();
        for (int yb = yMin; yb <= yMax; yb++) {
            if ((stencil.getMask(xm, yb) & Stencil.MSK_BRICK) != 0) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Get number of free pixels above the lemming (max of step is checked).
     * @return number of free pixels above the lemming
     */
    private boolean freeAboveClimber() {
        if (x < LEFT_BOUNDARY || x >= GameController.getWidth() + RIGHT_BOUNDARY || x <= TOP_BOUNDARY) {
            return false;
        }
        
        int ym = y - 18;
        int xm = x;
        if (dir == Direction.LEFT) {
            xm += 1;
        } else {
            xm -= 1;
        }
        Stencil stencil = GameController.getStencil();
        return ym >= 0 && ((stencil.getMask(xm, ym) & Stencil.MSK_BRICK) == 0);
    }

    /**
     * Check if Lemming has fallen to/through the bottom of the level.
     * @return true if Lemming has fallen to/through the bottom of the level, false otherwise
     */
    private boolean crossedLowerBorder() {
        if (y >= Level.DEFAULT_HEIGHT + BOTTOM_BOUNDARY) {
            hasDied = true;
            GameController.sound.play(Sound.Effect.DIE, getPan());
            return true;
        }
        return false;
    }

    /**
     * Get the number of pixels of walkable ground above the Lemming's foot.
     * @return number of pixels of walkable ground above the Lemming's foot.
     */
    private int aboveGround() {
        if (x < LEFT_BOUNDARY || x >= GameController.getWidth() + RIGHT_BOUNDARY) {
            return Level.DEFAULT_HEIGHT + 1;
        }

        int ym = y - 1;
        if (ym >= Level.DEFAULT_HEIGHT) {
            return 0;
        }
        int pos = x;
        Stencil stencil = GameController.getStencil();
        pos += ym * GameController.getWidth();
        int levitation;
        for (levitation = 0; levitation < WALKER_OBSTACLE_HEIGHT; levitation++, pos -= GameController.getWidth(), ym--) {
            if (ym < TOP_BOUNDARY - 1) {
                return WALKER_OBSTACLE_HEIGHT + 1; // forbid leaving level to the top
            }
            if ((stencil.getMask(pos) & Stencil.MSK_BRICK) == 0) {
                break;
            }
        }
        return levitation;
    }

    /**
     * Check if climber reached a plateau he can walk on.
     * @return true if climber reached a plateau he can walk on, false otherwise
     */
    private boolean reachedPlateau(final int hand) {
        if (x - 2 < LEFT_BOUNDARY || x + 2 >= GameController.getWidth() + RIGHT_BOUNDARY) {
            return false;
        }
        int ym = y - hand;
        if (ym >= Level.DEFAULT_HEIGHT || ym <= -4) {
            return true;
        } else if (ym < 0) {
            return false;
        }
        int pos = x;
        pos += ym * GameController.getWidth();
        return (GameController.getStencil().getMask(pos) & Stencil.MSK_BRICK) == 0;
    }
    
    private void eraseBlockerMask() {
        Mask m = lemmings[getOrdinal(Type.BLOCKER)].getMask(dir);
        int maskX = x - lemmings[getOrdinal(Type.BLOCKER)].maskX;
        int maskY = y - lemmings[getOrdinal(Type.BLOCKER)].maskY;
        m.clearType(maskX, maskY, 0, Stencil.MSK_BLOCKER_LEFT);
        m.clearType(maskX, maskY, 1, Stencil.MSK_BLOCKER_CENTER);
        m.clearType(maskX, maskY, 2, Stencil.MSK_BLOCKER_RIGHT);
    }

    /**
     * Replace a color in the animation frame with another color.
     * Used to patch the color of debris from pink color to a level specific color.
     * @param replaceCol first color to replace
     * @param replaceCol2 second color to replace
     */
    public static void patchColors(final int replaceCol, final int replaceCol2) {
        for (int l = 0; l < NUM_RESOURCES; l++) {      // go through all the lemmings
            LemmingResource lr = lemmings[l];
            for (int f = 0; f < lr.frames; f++) {      // go through all frames
                for (int d = 0; d < lr.dirs; d++) {    // go though all dirs
                    for (int xp = 0; xp < lr.width; xp++) {
                        for (int yp = 0; yp < lr.height; yp++) {
                            Image i = lr.getImage(Direction.get(d), f);
                            int rgb = i.getRGB(xp, yp);
                            if (rgb == templateColor) {
                                i.setRGB(xp, yp, replaceCol);
                            } else if (rgb == templateColor2) {
                                i.setRGB(xp, yp, replaceCol2);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Load images used for Lemming animations.
     * @throws ResourceException
     */
    public static void loadLemmings() throws ResourceException {
        explodeFont = new ExplodeFont();
        // read lemmings definition file
        String fn = Core.findResource(LEMM_INI_STR);
        Props p = new Props();
        if (fn == null || !p.load(fn)) {
            throw new ResourceException(LEMM_INI_STR);
        }
        lemmings = new LemmingResource[NUM_RESOURCES];
        // read lemmings
        templateColor = p.getInt("templateColor", DEF_TEMPLATE_COLOR) | 0xff000000;
        templateColor2 = p.getInt("templateColor2", templateColor) | 0xff000000;
        Type[] lemmTypes = Type.values();
        for (int i = 0; i < lemmings.length; i++) {
            // frames, directions, animation type
            Type type = lemmTypes[i];
            boolean bidirectional = type.bidirectional;
            if (type.frames > 0) {
                fn = Core.findResource("gfx/lemming/lemm_" + type.name().toLowerCase() + ".png", Core.IMAGE_EXTENSIONS);
                if (fn == null) {
                    throw new ResourceException("gfx/lemming/lemm_" + type.name().toLowerCase() + ".png");
                }
                Image sourceImg = Core.loadTranslucentImage(fn);
                if (bidirectional) {
                    fn = Core.findResource("gfx/lemming/lemm_" + type.name().toLowerCase() + "_left.png", Core.IMAGE_EXTENSIONS);
                    if (fn == null) {
                        throw new ResourceException("gfx/lemming/lemm_" + type.name().toLowerCase() + "_left.png");
                    }
                    Image sourceImgLeft = Core.loadTranslucentImage(fn);
                    lemmings[i] = new LemmingResource(sourceImg, sourceImgLeft, type.frames);
                } else {
                    lemmings[i] = new LemmingResource(sourceImg, type.frames);
                }
            } else {
                lemmings[i] = new LemmingResource();
            }
            lemmings[i].animMode = type.loop ? Animation.LOOP : Animation.ONCE;
            // read mask
            if (type.maskFrames > 0) {
                // mask_Y: frames, directions, step
                fn = Core.findResource("gfx/lemming/mask_" + type.name().toLowerCase() + ".png", Core.IMAGE_EXTENSIONS);
                if (fn == null) {
                    throw new ResourceException("gfx/lemming/mask_" + type.name().toLowerCase() + ".png");
                }
                Image sourceImg = Core.loadBitmaskImage(fn);
                Mask mask = new Mask(sourceImg, type.maskFrames);
                lemmings[i].setMask(Direction.RIGHT, mask);
                if (bidirectional) {
                    fn = Core.findResource("gfx/lemming/mask_" + type.name().toLowerCase() + "_left.png", Core.IMAGE_EXTENSIONS);
                    if (fn == null) {
                        throw new ResourceException("gfx/lemming/mask_" + type.name().toLowerCase() + "_left.png");
                    }
                    Image sourceImgLeft = Core.loadBitmaskImage(fn);
                    Mask maskLeft = new Mask(sourceImgLeft, type.maskFrames);
                    lemmings[i].setMask(Direction.LEFT, maskLeft);
                }
                lemmings[i].maskStep = type.maskStep;
            }
            // read foot position and size
            int[] val = p.getIntArray("pos_" + i, null);
            if (val != null && val.length == 3) {
                lemmings[i].footX = val[0];
                lemmings[i].footY = val[1];
                lemmings[i].size  = val[2];
            }
            val = p.getIntArray("maskPos_" + i, null);
            if (val != null && val.length == 2) {
                lemmings[i].maskX = val[0];
                lemmings[i].maskY = val[1];
            }
        }
    }

    /**
     * Get display name of this Lemming.
     * @return display name of this Lemming
     */
    public String getName() {
        String n = type.name;
        if (!n.isEmpty()) {
            if (canClimb && canFloat) {
                n += " (A)";
            } else if (canClimb && type != Type.CLIMBER && type != Type.FLIPPER) {
                n += " (C)";
            } else if (canFloat && type != Type.FLOATER && type != Type.FLOATER_START) {
                n += " (F)";
            }
        }
        return n;
    }

    /**
     * Get current skill/type of this Lemming.
     * @return current skill/type of this Lemming
     */
    public Type getSkill() {
        return type;
    }

    /**
     * Set new skill/type of this Lemming.
     * @param skill new skill/type
     * @return true if a change was possible, false otherwise
     */
    public boolean setSkill(final Type skill, boolean playSound) {
        int canSet = -1;
        
        if (skill == type || hasDied) {
            canSet = 0;
        }
        
        // check types which can't even get an additional skill anymore
        if (canSet == -1) {
            switch (type) {
                case DROWNER:
                case HOMER:
                case FRIER:
                case FLAPPER:
                    if (skill != Type.NUKE) {
                        canSet = 0;
                    }
                    break;
                case SPLATTER:
                case EXPLODER:
                    if (skill == Type.NUKE) {
                        nuke = true;
                    }
                    canSet = 0;
                    break;
                default:
                    break;
            }
        }
        
        // check additional skills
        if (canSet == -1) {
            switch (skill) {
                case CLIMBER:
                    if (canClimb || type == Type.BLOCKER) {
                        canSet = 0;
                    } else {
                        canClimb = true;
                        canSet = 1;
                    }
                    break;
                case FLOATER:
                    if (canFloat || type == Type.BLOCKER) {
                        canSet = 0;
                    } else {
                        canFloat = true;
                        canSet = 1;
                    }
                    break;
                case NUKE: // special case:  nuke request
                    if (nuke) {
                        canSet = 0;
                        break;
                    }
                    nuke = true;
                    /* falls through */
                case FLAPPER:
                    if (explodeNumCtr == 0) {
                        explodeNumCtr = 5;
                        explodeCtr = 0;
                        canSet = 1;
                    } else {
                        canSet = 0;
                    }
                    break;
                default:
                    break;
            }
        }
        
        // check main skills
        if (canSet == -1 && canChangeSkill) {
            switch (skill) {
                case DIGGER:
                    if (canDig(playSound)) {
                        changeType(type, skill);
                        counter = 0;
                        canSet = 1;
                    } else {
                        playSound = false;
                        canSet = 0;
                    }
                    break;
                case MINER:
                    if (canMine(true, playSound)) {
                        y += 2;
                        changeType(type, skill);
                        counter = 0;
                        canSet = 1;
                    } else {
                        playSound = false;
                        canSet = 0;
                    }
                    break;
                case BASHER:
                    if (canBashSteel(playSound)) {
                        changeType(type, skill);
                        counter = 0;
                        canSet = 1;
                    } else {
                        playSound = false;
                        canSet = 0;
                    }
                    break;
                case BUILDER:
                    if (y < TOP_BOUNDARY + 2) {
                        canSet = 0;
                    } else {
                        changeType(type, skill);
                        counter = 0;
                        canSet = 1;
                    }
                    break;
                case BLOCKER:
                    Mask m = Lemming.getResource(Type.BLOCKER).getMask(Direction.LEFT);
                    int maskX = x - lemmings[getOrdinal(Type.BLOCKER)].maskX;
                    int maskY = y - lemmings[getOrdinal(Type.BLOCKER)].maskY;
                    for (int i = 0; i < m.getNumFrames(); i++) {
                        if (m.checkType(maskX, maskY, i, Stencil.MSK_BLOCKER | Stencil.MSK_EXIT)) {
                            canSet = 0; // overlaps exit or existing blocker
                        }
                    }
                    if (canSet != 0) {
                        changeType(type, skill);
                        counter = 0;
                        // set blocker mask
                        m.setBlockerMask(maskX, maskY);
                        canSet = 1;
                    }
                default:
                    break;
            }
        }
        
        if (canSet == 1) {
            if (playSound) {
                GameController.sound.play(Sound.Effect.ASSIGN_SKILL, getPan());
            }
            return true;
        } else {
            if (playSound) {
                GameController.sound.play(Sound.Effect.INVALID, getPan());
            }
            return false;
        }
    }

    /**
     * Get width of animation frame in pixels.
     * @return width of animation frame in pixels
     */
    public int width() {
        return lemRes.width;
    }

    /**
     * Get height of animation frame in pixels.
     * @return height of animation frame in pixels
     */
    public int height() {
        return lemRes.height;
    }

    /**
     * Get static resource for a skill/type
     * @param type skill/type
     * @return static resource for this skill/type
     */
    private static LemmingResource getResource(final Type type) {
        return lemmings[getOrdinal(type)];
    }

    /**
     * Get X coordinate of upper left corner of animation frame.
     * @return X coordinate of upper left corner of animation frame
     */
    public int screenX() {
        return x - lemRes.footX;
    }

    /**
     * Get Y coordinate of upper left corner of animation frame
     * @return Y coordinate of upper left corner of animation frame
     */
    public int screenY() {
        return y - lemRes.footY;
    }

    public int midX() {
        return x;
    }

    public int midY() {
        return y - lemRes.size;
    }

    public int screenMaskX() {
        return x - lemRes.maskX;
    }

    public int screenMaskY() {
        return y - lemRes.maskY;
    }

    /**
     * Get heading of Lemming.
     * @return heading of Lemming
     */
    public Direction getDirection() {
        return dir;
    }

    /**
     * Get current animation frame for this Lemming.
     * @return current animation frame for this Lemming
     */
    public Image getImage() {
        return lemRes.getImage(dir, frameIdx / TIME_SCALE);
    }

    /**
     * Get image for explosion countdown.
     * @return image for explosion countdown (or null if no explosion countdown)
     */
    public Image getCountdown() {
        if (explodeNumCtr == 0) {
            return null;
        } else {
            return explodeFont.getImage(explodeNumCtr - 1);
        }
    }

    /**
     * Used for replay: start to display the selection image.
     */
    public void setSelected() {
        selectCtr = 20;
    }

    /**
     * Get the selection image for replay.
     * @return the selection image (or null if no selection displayed)
     */
    public Image getSelectImg() {
        if (selectCtr == 0) {
            return null;
        } else {
            return MiscGfx.getImage(MiscGfx.Index.SELECT);
        }
    }

    /**
     * Get: Lemming has died.
     * @return true if Lemming has died, false otherwise
     */
    public boolean hasDied() {
        return hasDied;
    }

    /**
     * Get: Lemming has left the level.
     * @return true if Lemming has left the level, false otherwise
     */
    public boolean hasLeft() {
        return hasLeft;
    }

    /**
     * Get: Lemming is to be nuked.
     * @return true if Lemming is to be nuked, false otherwise
     */
    public boolean nuke() {
        return nuke;
    }

    /**
     * Get: Lemming can float.
     * @return true if Lemming can float, false otherwise
     */
    public boolean canFloat() {
        return canFloat;
    }

    /**
     * Get: Lemming can climb.
     * @return true if Lemming can climb, false otherwise
     */
    public boolean canClimb() {
        return canClimb;
    }

    /**
     * Get: Lemming can get a new skill.
     * @return true if Lemming can get a new skill, false otherwise
     */
    public boolean canChangeSkill() {
        return canChangeSkill;
    }
    
    public boolean hasTimer() {
        return explodeNumCtr > 0;
    }
    
    public double getPan() {
        double panFactor = Lemmini.getPaneWidth();
        double retPan = (x - (GameController.getXPos() + Lemmini.getPaneWidth() / 2.0)) / panFactor;
        return retPan;
    }
}

/**
 * Storage class for a Lemming.
 * @author Volker Oth
 */
class LemmingResource {
    /** relative foot X position in pixels inside bitmap */
    int footX;
    /** relative foot Y position in pixels inside bitmap */
    int footY;
    /** "mid" position above foot in pixels */
    int size;
    int maskX;
    int maskY;
    /** width of image in pixels */
    int width;
    /** height of image in pixels */
    int height;
    /** number of animation frames */
    int frames;
    /** animation mode */
    Lemming.Animation animMode;
    /** number of directions (1 or 2) */
    int dirs;
    int maskStep;
    /** array of images to store the animation [Direction][AnimationFrame] */
    private final Image[][] img;
    /** array of removal masks used for digging/bashing/mining/explosions etc. [Direction] */
    private final Mask[] mask;
    
    /**
     * Constructor.
     */
    LemmingResource() {
        img = new Image[1][1];
        mask = new Mask[1];
        width = 1;
        height = 1;
        dirs = 1;
        animMode = Lemming.Animation.NONE;
        img[Lemming.Direction.RIGHT.ordinal()][0] = ToolBox.createBitmaskImage(width, height);
    }

    /**
     * Constructor.
     * @param sourceImg  image containing animation frames (one above the other)
     * @param animFrames number of animation frames.
     */
    LemmingResource(final Image sourceImg, final int animFrames) {
        img = new Image[1][];
        mask = new Mask[1];
        frames = animFrames;
        width = sourceImg.getWidth();
        height = sourceImg.getHeight() / animFrames;
        dirs = 1;
        animMode = Lemming.Animation.NONE;
        img[Lemming.Direction.RIGHT.ordinal()] = ToolBox.getAnimation(sourceImg, animFrames);
    }
    
    /**
     * Constructor.
     * @param sourceImg  image containing animation frames (one above the other)
     * @param sourceImgLeft 
     * @param animFrames number of animation frames.
     */
    LemmingResource(final Image sourceImg, final Image sourceImgLeft, final int animFrames) {
        img = new Image[2][];
        mask = new Mask[2];
        frames = animFrames;
        width = Math.min(sourceImg.getWidth(), sourceImgLeft.getWidth());
        height = Math.min(sourceImg.getHeight() / animFrames, sourceImgLeft.getHeight() / animFrames);
        dirs = 2;
        animMode = Lemming.Animation.NONE;
        img[Lemming.Direction.RIGHT.ordinal()] = ToolBox.getAnimation(sourceImg, animFrames);
        img[Lemming.Direction.LEFT.ordinal()] = ToolBox.getAnimation(sourceImgLeft, animFrames);
    }

    /**
     * Get the mask for stencil manipulation.
     * @param dir Direction
     * @return mask for stencil manipulation
     */
    Mask getMask(final Lemming.Direction dir) {
        if (dirs > 1) {
            return mask[dir.ordinal()];
        } else {
            return mask[0];
        }
    }

    /**
     * Set the mask for stencil manipulation.
     * @param dir Direction
     * @param m mask for stencil manipulation
     */
    void setMask(final Lemming.Direction dir, final Mask m) {
        if (dirs > 1) {
            mask[dir.ordinal()] = m;
        } else {
            mask[0] = m;
        }
    }

    /**
     * Get specific animation frame.
     * @param dir Direction.
     * @param frame Index of animation frame.
     * @return specific animation frame
     */
    Image getImage(final Lemming.Direction dir, final int frame) {
        if (dirs > 1) {
            return img[dir.ordinal()][frame];
        } else {
            return img[0][frame];
        }
    }
}


/**
 * Used to manage the font for the explosion counter.
 * @author Volker Oth
 */
class ExplodeFont {

    /** array of images for each counter value */
    private Image[] img;

    /**
     * Constructor.
     * @param cmp parent component
     * @throws ResourceException
     */
    ExplodeFont() throws ResourceException {
        String fn = Core.findResource("gfx/lemming/countdown.png", Core.IMAGE_EXTENSIONS);
        if (fn == null) {
            throw new ResourceException("gfx/lemming/countdown.png");
        }
        Image sourceImg = Core.loadTranslucentImage(fn);
        img = ToolBox.getAnimation(sourceImg, 5);
    }

    /**
     * Get image for a counter value (0-9)
     * @param num counter value (0-9)
     * @return
     */
    Image getImage(final int num) {
        return img[num];
    }
}