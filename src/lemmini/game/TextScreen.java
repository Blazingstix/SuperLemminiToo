package lemmini.game;

import java.awt.Color;
import lemmini.Lemmini;
import static lemmini.game.LemmFont.Color.*;
import lemmini.graphics.GraphicsContext;
import lemmini.graphics.GraphicsOperation;
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
 * Class to print text screens which can be navigated with the mouse.
 * Uses {@link TextDialog}
 *
 * @author Volker Oth
 */
public class TextScreen {

    /** Mode (type of screen to present) */
    public static enum Mode {
        /** initial state */
        INIT,
        /** main introduction screen */
        INTRO,
        /** level briefing screen */
        BRIEFING,
        /** level debriefing screen */
        DEBRIEFING
    }
    
    public static enum Button {
        /** continue */
        CONTINUE,
        /** restart level */
        RESTART,
        /** back to menu */
        MENU,
        /** replay level */
        REPLAY,
        /** save replay */
        SAVE_REPLAY,
        /** continue to next rating */
        NEXT_RATING,
        NONE;
    }

    /** synchronization monitor */
    private static final Object monitor = new Object();
    /** y position of scroll text - pixels relative to center */
    private static final int SCROLL_Y = 150;
    /** width of scroll text in characters */
    private static final int SCROLL_WIDTH = 39;
    /** height of scroll text in pixels */
    private static final int SCROLL_HEIGHT = LemmFont.getHeight() * 2;
    /** step width of scroll text in pixels */
    private static final int SCROLL_STEP = 2;
    /** scroll text */
    private static final String SCROLL_TEXT =
        "                                           "
        + "SuperLemmini - a game engine for Lemmings(tm) in Java. "
        + "Thanks to Martin Cameron for his IBXM library, "
        + "Mindless for his MOD conversions of the original Amiga Lemmings tunes and his Amiga level rips, "
        + "the guys of DMA Design for writing the original Lemmings, "
        + "ccexplore and the other nice folks at the Lemmings Forum for discussion and advice, "
        + "and Oracle for maintaining Java and providing the community with a free development environment.";

    /** TextDialog used as base component */
    private static TextDialog textDialog;
    /** factor used for the rotation animation */
    private static double rotFact = 1.0;
    /** delta used for the rotation animation */
    private static double rotDelta;
    /** source image for rotation animation */
    private static Image imgSrc;
    /** target image for rotation animation */
    private static Image imgTrg;
    /** graphics for rotation animation */
    private static GraphicsContext imgGfx;
    /** flip state for rotation: true - image is flipped in Y direction */
    private static boolean flip;
    /** affine transformation used for rotation animation */
    private static GraphicsOperation at;
    /** counter used to trigger the rotation animation (in animation update frames) */
    private static int rotCtr;
    /** counter threshold used to trigger the rotation animation (in animation update frames) */
    private static final int maxRotCtr = 99;
    /** used to stop the rotation only after it was flipped twice -> original direction */
    private static int flipCtr;
    /** counter for scrolled characters */
    private static int scrollCharCtr;
    /** counter for scrolled pixels */
    private static int scrollPixCtr;
    /** image used for scroller */
    private static Image scrollerImg;
    /** graphics used for scroller */
    private static GraphicsContext scrollerGfx;
    /** screen type to display */
    private static Mode mode;

    /**
     * Set mode.
     * @param m mode.
     */
    public static void setMode(final Mode m) {
        synchronized (monitor) {
            if (mode != m) {
                switch (m) {
                    case INTRO:
                        textDialog.init();
                        textDialog.fillBackground(MiscGfx.getImage(MiscGfx.Index.TILE_BROWN));
                        textDialog.printCentered("A game engine for Lemmings(tm) in Java", -1, RED);
                        textDialog.printCentered("Release " + Lemmini.REVISION + " 05/2014", 0, BLUE);
                        textDialog.printCentered("Coded by Ryan Sakowski 2013-2014", 1, BROWN);
                        textDialog.printCentered("Original Lemmini by Volker Oth 2005-2010", 2, VIOLET);
                        textDialog.printCentered("Original website: www.lemmini.de", 3, GREEN);
                        //textDialog.addTextButton(-4, 4, 1, "  Start ", "Let's go", BLUE, RED);
                        textDialog.copyToBackBuffer();
                        break;
                    case BRIEFING:
                        initBriefing();
                        break;
                    case DEBRIEFING:
                        initDebriefing();
                        break;
                    default:
                        break;
                }
            }
            mode = m;
        }
    }

    /**
     * Initialize the briefing dialog.
     */
    static void initBriefing() {
        textDialog.init();
        textDialog.fillBackground(MiscGfx.getImage(MiscGfx.Index.TILE_GREEN));
        Level level = GameController.getLevel();
        textDialog.restore();
        String rating = GameController.getCurLevelPack().getRatings()[GameController.getCurRating()];
        textDialog.drawImage(GameController.getMapPreview(), -200);
        textDialog.print(String.format("Level %d", GameController.getCurLevelNumber() + 1), -21, -2, RED);
        textDialog.print(level.getLevelName(), -11, -2, RED);
        textDialog.print(String.format("Number of Lemmings %d", level.getNumLemmings()), -9, 0, BLUE);
        if (level.getNumLemmings() <= 100) {
            textDialog.print(String.format("%d%% to be saved", level.getNumToRescue() * 100 / level.getNumLemmings()), -9, 1, GREEN);
        } else {
            textDialog.print(String.format("%d to be saved", level.getNumToRescue()), -9, 1, GREEN);
        }
        textDialog.print(String.format("Release Rate %d", level.getReleaseRate()), -9, 2, BROWN);
        int minutes = level.getTimeLimitSeconds() / 60;
        int seconds = level.getTimeLimitSeconds() % 60;
        if (!GameController.isTimed()) {
            textDialog.print("No time limit", -9, 3, TURQUOISE);
        } else if (seconds == 0) {
            String minuteWord = (minutes == 1) ? "Minute" : "Minutes";
            textDialog.print(String.format("Time         %d %s", minutes, minuteWord), -9, 3, TURQUOISE);
        } else {
            textDialog.print(String.format("Time         %d-%02d", minutes, seconds), -9, 3, TURQUOISE);
        }
        if (GameController.getCurLevelPackIdx() > 0) {
            textDialog.print(String.format("Rating       %s", rating), -9, 4, VIOLET);
        }
        textDialog.copyToBackBuffer(); // though not really needed
    }

    /**
     * Initialize the debriefing dialog.
     */
    static void initDebriefing() {
        textDialog.init();
        textDialog.fillBackground(MiscGfx.getImage(MiscGfx.Index.TILE_GREEN));
        int numLemmings = GameController.getNumLemmingsMax();
        int toRescue = GameController.getNumToRescue();
        int rescued = GameController.getNumLeft();
        int toRescuePercent = toRescue * 100 / numLemmings; // % to rescue of total number
        int rescuedPercent = rescued * 100 / numLemmings; // % rescued of total number
        int rescuedOfToRescue; // % rescued of no. to rescue
        if (toRescue == 0) {
            rescuedOfToRescue = 0;
        } else {
            rescuedOfToRescue = rescued * 100 / toRescue;
        }
        int score = GameController.getScore();
        textDialog.restore();
        if (GameController.getTime() == 0 && GameController.isTimed()) {
            textDialog.printCentered("Time is up.", -7, TURQUOISE);
        } else {
            textDialog.printCentered("All lemmings accounted for.", -7, TURQUOISE);
        }
        if (numLemmings <= 100) {
            textDialog.print(String.format("You needed:  %d%%", toRescuePercent), -7, -5, VIOLET);
            textDialog.print(String.format("You rescued: %d%%", rescuedPercent), -7, -4, VIOLET);
        } else {
            textDialog.print(String.format("You needed:  %d", toRescuePercent), -7, -5, VIOLET);
            textDialog.print(String.format("You rescued: %d", rescuedPercent), -7, -4, VIOLET);
        }
        String pointWord = (score == 1) ? "point" : "points";
        textDialog.print(String.format("Your score: %d %s", score, pointWord), -10, -3, VIOLET);
        LevelPack lp = GameController.getCurLevelPack();
        String[] debriefings = lp.getDebriefings();
        if (GameController.wasLost()) {
            if (GameController.getNumLeft() <= 0) {
                textDialog.printCentered(debriefings[0], -1, RED);
                textDialog.printCentered(debriefings[1], 0, RED);
            } else if (rescuedOfToRescue < 50) {
                textDialog.printCentered(debriefings[2], -1, RED);
                textDialog.printCentered(debriefings[3], 0, RED);
            } else if (rescuedPercent < toRescuePercent - 5) {
                textDialog.printCentered(debriefings[4], -1, RED);
                textDialog.printCentered(debriefings[5], 0, RED);
            } else if (rescuedPercent < toRescuePercent - 1) {
                textDialog.printCentered(debriefings[6], -1, RED);
                textDialog.printCentered(debriefings[7], 0, RED);
            } else {
                textDialog.printCentered(debriefings[8], -1, RED);
                textDialog.printCentered(debriefings[9], 0, RED);
            }
            textDialog.addTextButton(-2, 6, Button.RESTART, "Retry", "Retry", BLUE, BROWN);
        } else {
            if (rescued <= toRescue && rescued < numLemmings) {
                textDialog.printCentered(debriefings[10], -1, RED);
                textDialog.printCentered(debriefings[11], 0, RED);
            } else if (rescuedPercent < toRescuePercent + 20 && rescued < numLemmings) {
                textDialog.printCentered(debriefings[12], -1, RED);
                textDialog.printCentered(debriefings[13], 0, RED);
            } else if (rescued < numLemmings) {
                textDialog.printCentered(debriefings[14], -1, RED);
                textDialog.printCentered(debriefings[15], 0, RED);
            } else {
                textDialog.printCentered(debriefings[16], -1, RED);
                textDialog.printCentered(debriefings[17], 0, RED);
            }
            int ln = GameController.getCurLevelNumber();
            int r = GameController.getCurRating();
            if (lp.getLevels(r).length > ln + 1) {
                int absLevel = GameController.absLevelNum(GameController.getCurLevelPackIdx(), GameController.getCurRating(), ln+1);
                String code = LevelCode.create(lp.getCodeSeed(), absLevel, rescuedPercent,
                        GameController.getTimesFailed(), 0, lp.getCodeOffset());
                if (code != null) {
                    textDialog.printCentered("Your access code for level " + (ln + 2), 2, BROWN);
                    textDialog.printCentered("is " + code, 3, BROWN);
                    textDialog.printCentered(String.format("Your access code for level %d", ln + 2), 2, BROWN);
                    textDialog.printCentered(String.format("is %s", code), 3, BROWN);
                }
                textDialog.addTextButton(-4, 6, Button.CONTINUE, "Continue", "Continue", BLUE, BROWN);
            } else {
                textDialog.printCentered("Congratulations!", 2, BROWN);
                textDialog.printCentered(String.format("You finished all the %s levels!", lp.getRatings()[GameController.getCurRating()]), 3, GREEN);
                if (lp.getLevels(r).length <= ln + 1 && lp.getRatings().length > r + 1) {
                    textDialog.addTextButton(-4, 6, Button.NEXT_RATING, "Continue", "Continue", BLUE, BROWN);
                }
            }
        }
        textDialog.copyToBackBuffer(); // though not really needed
        if (!GameController.getWasCheated()) {
            textDialog.addTextButton(-12, 5, Button.REPLAY, "Replay", "Replay", BLUE, BROWN);
            if (GameController.getCurLevelPackIdx() != 0) { // not for single levels started via "load level"
                textDialog.addTextButton(-4, 5, Button.SAVE_REPLAY, "Save Replay", "Save Replay", BLUE, BROWN);
            }
        }
        textDialog.addTextButton(9, 5, Button.MENU, "Menu", "Menu", BLUE, BROWN);
    }

    /**
     * Get text dialog.
     * @return text dialog.
     */
    public static TextDialog getDialog() {
        synchronized (monitor) {
            return textDialog;
        }
    }

    /**
     * Initialize text screen.
     * @param width width in pixels
     * @param height height in pixels
     */
    public static void init(final int width, final int height) {
        rotFact = 1.0;
        rotDelta = -0.1;
        imgSrc = MiscGfx.getImage(MiscGfx.Index.LEMMINI);
        at = ToolBox.createGraphicsOperation();
        flip = false;
        rotCtr = 0 ;
        flipCtr = 0;
        imgTrg = ToolBox.createTranslucentImage(imgSrc.getWidth(), imgSrc.getHeight());
        imgGfx = imgTrg.createGraphicsContext();
        imgGfx.setBackground(new Color(0, 0, 0, 0)); // invisible
        scrollCharCtr = 0;
        scrollPixCtr = 0;

        scrollerImg = ToolBox.createTranslucentImage(LemmFont.getWidth() * (1 + SCROLL_WIDTH), SCROLL_HEIGHT);
        scrollerGfx = scrollerImg.createGraphicsContext();
        scrollerGfx.setBackground(new Color(0, 0, 0, 0));

        textDialog  = new TextDialog(width, height);

    }

    /**
     * Update the text screen (for animations)
     */
    public static void update() {
        synchronized (monitor) {
            textDialog.restore();
            switch (mode) {
                case INTRO:
                    update_intro();
                    break;
                case BRIEFING:
                    update_briefing();
                    break;
                case DEBRIEFING:
                    update_debriefing();
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Update the into screen.
     */
    private static void update_intro() {
        // manage logo rotation
        if (++rotCtr > maxRotCtr) {
            // animate
            rotFact += rotDelta;
            if (rotFact <= 0.0) {
                // minimum size reached -> flip and increase again
                rotFact = 0.1;
                rotDelta = -rotDelta;
                flip = !flip;
            } else if (rotFact > 1.0) {
                // maximum size reached -> decrease again
                rotFact = 1.0;
                rotDelta = -rotDelta;
                // reset only after two rounds (flipped back)
                if (++flipCtr > 1) {
                    rotCtr = 0;
                }
            }
            if (flip) {
                at.setScale(1, -rotFact);
                at.translate(1, -imgSrc.getHeight());
            } else {
                at.setScale(1, rotFact);
            }
            imgGfx.clearRect(0, 0, imgTrg.getWidth(), imgTrg.getHeight());
            at.execute(imgSrc, imgTrg);
            textDialog.drawImage(imgTrg, -120 - (int) (imgSrc.getHeight() / 2 * Math.abs(rotFact) + 0.5));
        } else {
            // display original image
            flipCtr = 0;
            textDialog.drawImage(imgSrc, -120 - imgSrc.getHeight() / 2);
        }
        // manage scroller
        String out;
        boolean wrapAround = false;
        int endIdx = scrollCharCtr + SCROLL_WIDTH + 1;
        if (endIdx > SCROLL_TEXT.length()) {
            endIdx = SCROLL_TEXT.length();
            wrapAround = true;
        }
        out = SCROLL_TEXT.substring(scrollCharCtr, endIdx);
        if (wrapAround) {
            out += SCROLL_TEXT.substring(0, scrollCharCtr + SCROLL_WIDTH + 1 - SCROLL_TEXT.length());
        }
        scrollerGfx.clearRect(0, 0, scrollerImg.getWidth(), scrollerImg.getHeight());
        LemmFont.strImage(scrollerGfx, out, BLUE);
        int w = SCROLL_WIDTH*LemmFont.getWidth();
        int dx = (textDialog.getScreen().getWidth() - w) / 2;
        int dy = (textDialog.getScreen().getHeight() / 2) + SCROLL_Y;
        textDialog.getScreen().createGraphicsContext().drawImage(
                scrollerImg, dx, dy, dx + w, dy + SCROLL_HEIGHT, scrollPixCtr, 0, scrollPixCtr + w, SCROLL_HEIGHT / 2
                );

        scrollPixCtr += SCROLL_STEP;
        if (scrollPixCtr >= LemmFont.getWidth()) {
            scrollCharCtr++;
            scrollPixCtr = 0;
            if (scrollCharCtr >= SCROLL_TEXT.length()) {
                scrollCharCtr = 0;
            }
        }
    }

    /**
     * Update the briefing screen.
     */
    private static void update_briefing() {
    }

    /**
     * Update the debriefing screen.
     */
    private static void update_debriefing() {
        textDialog.drawButtons();
    }

    /**
     * Get image of text screen
     * @return image of text screen
     */
    public static Image getScreen() {
        synchronized (monitor) {
            return textDialog.getScreen();
        }
    }
}
