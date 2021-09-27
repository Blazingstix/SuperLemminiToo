package lemmini.game;

import java.awt.Color;
import java.awt.HeadlessException;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.JOptionPane;
import lemmini.Lemmini;
import lemmini.gameutil.Fader;
import lemmini.graphics.GraphicsContext;
import lemmini.graphics.Image;
import lemmini.tools.NanosecondTimer;
import lemmini.tools.ToolBox;

/*
 * FILE MODIFIED BY RYAN SAKOWSKI
 * 
 * 
 * Copyright 2010 Arne Limburg
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

public abstract class AbstractGameEngine implements Runnable {
    /** minimum sleep duration in milliseconds - values too small may cause system clock shift under WinXP etc. */
    static final int MIN_SLEEP = 10;
    /** threshold for sleep - don't sleep if time to wait is shorter than this as sleep might return too late */
    static final int THR_SLEEP = 16;
    /** size of auto scrolling range in pixels (from the left and right border) */
    static final int AUTOSCROLL_RANGE = 20;
    /** step size in pixels for horizontal scrolling */
    public static final int X_STEP = 4;
    /** step size in pixels for fast horizontal scrolling */
    public static final int X_STEP_FAST = 16;
    /** y coordinate of score display in pixels */
    static final int SCORE_Y = Level.DEFAULT_HEIGHT;
    /** y coordinate of counter displays in pixels */
    static final int COUNTER_Y = SCORE_Y + 40;
    /** y coordinate of icons in pixels */
    public static final int ICONS_Y = COUNTER_Y + 14;
    /** x coordinate of minimap in pixels */
    public static final int SMALL_X = 640 - 16 - 200;
    /** y coordinate of minimap in pixels */
    public static final int SMALL_Y = ICONS_Y;
    /** image for information string display */
    private Image outStrImg;
    /** graphics object for information string display */
    private GraphicsContext outStrGfx;
    /** array of offscreen images (one is active, one is passive) */
    private Image[] offImage;
    /** graphics objects for the two offscreen images */
    private GraphicsContext[] offGraphics;
    /** index of the active buffer in the image buffer */
    private int activeBuffer;
    /** monitoring object used for synchronized painting */
    private final Object paintSemaphore = new Object();
    /** start position of mouse drag (for mouse scrolling) */
    private int mouseDragStartX;
    /** x position of cursor in level */
    private int xMouse;
    /** x position of cursor on screen */
    private int xMouseScreen;
    /** y position of cursor in level */
    private int yMouse;
    /** y position of cursor on screen */
    private int yMouseScreen;
    /** mouse drag length in x direction (pixels) */
    private int mouseDx;
    /** mouse drag length in y direction (pixels) */
    private int mouseDy;
    /** flag: left mouse button is currently pressed */
    private boolean leftMousePressed = false;
    /** flag: middle mouse button is currently pressed */
    private boolean middleMousePressed = false;
    /** flag: right mouse button is currently pressed */
    private boolean rightMousePressed = false;
    private boolean mouseButton4Pressed = false;
    private boolean mouseButton5Pressed = false;

    public abstract int getWidth();
    
    public abstract int getHeight();
    
    /**
     * Initialization.
     */
    public void init() {
        offImage = new Image[2];
        offGraphics = new GraphicsContext[2];
        offImage[0] = ToolBox.createOpaqueImage(this.getWidth(), this.getHeight());
        offImage[1] = ToolBox.createOpaqueImage(this.getWidth(), this.getHeight());
        offGraphics[0] = offImage[0].createGraphicsContext();
        offGraphics[1] = offImage[1].createGraphicsContext();

        outStrImg = ToolBox.createTranslucentImage(this.getWidth(), LemmFont.getHeight());
        outStrGfx = outStrImg.createGraphicsContext();
        outStrGfx.setBackground(new Color(0, 0, 0));

        TextScreen.init(this.getWidth(), this.getHeight());
        GameController.setGameState(GameController.State.INTRO);
        GameController.setTransition(GameController.TransitionState.NONE);
        Fader.setBounds(this.getWidth(), this.getHeight());
        Fader.setState(Fader.State.IN);
    }

    @Override
    public void run() {
        Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 1);
        NanosecondTimer timerRepaint = new NanosecondTimer();
        try {
            while (isRunning()) {
                GameController.State gameState = GameController.getGameState();
                if (timerRepaint.timePassedUpdate(GameController.NANOSEC_PER_FRAME)) {
                    // time passed -> redraw necessary
                    // special handling for fast forward or super lemming mode only during real gameplay
                    if (gameState == GameController.State.LEVEL) {
                        // in fast forward or super lemming modes, update the game mechanics
                        // multiple times per (drawn) frame
                        if (GameController.isFastForward()) {
                            int multiplier = (GameController.isFasterFastForward() ? GameController.FASTER_FAST_FWD_MULTI : GameController.FAST_FWD_MULTI);
                            for (int f = 0; f < multiplier - 1; f++) {
                                GameController.update();
                            }
                        } else if (GameController.isSuperLemming()) {
                            for (int f = 0; f < GameController.SUPERLEMM_MULTI - 1; f++) {
                                GameController.update();
                            }
                        }
                    }
                    redraw();
                } else {
                    try {
                        // determine time until next frame
                        long diff = GameController.NANOSEC_PER_FRAME - timerRepaint.delta();
                        if (diff > THR_SLEEP * 1000) {
                            Thread.sleep(MIN_SLEEP);
                        }
                    } catch (InterruptedException ex) {
                    }
                }
            }
        } catch (Exception ex) {
            ToolBox.showException(ex);
            System.exit(1);
        }
    }
    
    public void mousePressed(MouseEvent mouseEvent) {
        int x = mouseEvent.getX();
        int y = mouseEvent.getY();
        mouseDx = 0;
        mouseDy = 0;
        boolean swapButtons = GameController.doSwapButtons();
        switch (mouseEvent.getButton()) {
            case MouseEvent.BUTTON1:
                leftMousePressed = true;
                break;
            case MouseEvent.BUTTON2:
                if (swapButtons) {
                    rightMousePressed = true;
                } else {
                    middleMousePressed = true;
                }
                break;
            case MouseEvent.BUTTON3:
                if (swapButtons) {
                    middleMousePressed = true;
                } else {
                    rightMousePressed = true;
                }
                break;
            case 4:
                mouseButton4Pressed = true;
                break;
            case 5:
                mouseButton5Pressed = true;
                break;
            default:
                break;
        }

        if (Fader.getState() != Fader.State.OFF
                && GameController.getGameState() != GameController.State.LEVEL) {
            return;
        }

        switch (GameController.getGameState()) {
            case BRIEFING:
                Minimap.init(AbstractGameEngine.SMALL_X, AbstractGameEngine.SMALL_Y, 16, 8, true);
                GameController.setTransition(GameController.TransitionState.TO_LEVEL);
                Fader.setState(Fader.State.OUT);
                GameController.resetGain();
                mouseEvent.consume();
                break;
            case DEBRIEFING:
                TextScreen.Button button = TextScreen.getDialog().handleLeftClick(x, y);
                switch (button) {
                    case CONTINUE:
                        GameController.nextLevel(); // continue to next level
                        GameController.requestChangeLevel(GameController.getCurLevelPackIdx(), GameController.getCurRating(),
                                GameController.getCurLevelNumber(), false);
                        break;
                    case RESTART:
                        GameController.requestRestartLevel(false);
                        break;
                    case MENU:
                        GameController.setTransition(GameController.TransitionState.TO_INTRO);
                        Fader.setState(Fader.State.OUT);
                        Core.setTitle("SuperLemmini");
                        break;
                    case REPLAY:
                        GameController.requestRestartLevel(true);
                        break;
                    case SAVE_REPLAY:
                        String replayPath = ToolBox.getFileName(getParent(), Core.getResourcePath(), Core.REPLAY_EXTENSIONS, false);
                        if (replayPath != null) {
                            try {
                                String ext = ToolBox.getExtension(replayPath);
                                if (ext == null || ext.isEmpty()) {
                                    replayPath += '.' + Core.REPLAY_EXTENSIONS[0];
                                }
                                if (GameController.saveReplay(replayPath)) {
                                    return;
                                }
                                // else: no success
                                JOptionPane.showMessageDialog(Core.getCmp(), "Unable to save replay.", "Error", JOptionPane.ERROR_MESSAGE);
                            } catch (HeadlessException ex) {
                                ToolBox.showException(ex);
                            }
                        }
                        break;
                    case NEXT_RATING:
                        GameController.nextRating();
                        GameController.requestChangeLevel(GameController.getCurLevelPackIdx(), GameController.getCurRating(),
                                GameController.getCurLevelNumber(), false);
                        break;
                    default:
                        break;
                }
                mouseEvent.consume();
                break;
            case LEVEL:
                //  debug drawing
                if (leftMousePressed || (swapButtons ? middleMousePressed : rightMousePressed)) {
                    debugDraw(x, y, leftMousePressed);
                }
                if (mouseEvent.getButton() == MouseEvent.BUTTON1) {
                    if (y >= ICONS_Y && y < ICONS_Y + Icons.HEIGHT) {
                        Icons.Type type = GameController.getIconType(x);
                        if (type != Icons.Type.INVALID) {
                            GameController.handleIconButton(type);
                        }
                    } else {
                        if (y < Level.DEFAULT_HEIGHT) {
                            GameController.stopReplayMode();
                        }
                        Lemming l = GameController.lemmUnderCursor(LemmCursor.getType());
                        if (l != null) {
                            GameController.requestSkill(l);
                        }
                    }
                    // check minimap mouse move
                    int ofs = Minimap.move(x, y, this.getWidth());
                    if (ofs != -1) {
                        GameController.setXPos(ofs);
                    }
                    mouseEvent.consume();
                } 
                if (mouseEvent.getButton() == (swapButtons ? MouseEvent.BUTTON2 : MouseEvent.BUTTON3)) {
                    if (LemmCursor.getType() == LemmCursor.CursorType.LEFT) {
                        Lemmini.setCursor(LemmCursor.CursorType.WALKER_LEFT);
                    } else if (LemmCursor.getType() == LemmCursor.CursorType.RIGHT) {
                        Lemmini.setCursor(LemmCursor.CursorType.WALKER_RIGHT);
                    } else if (LemmCursor.getType() == LemmCursor.CursorType.NORMAL) {
                        Lemmini.setCursor(LemmCursor.CursorType.WALKER);
                    }
                    Lemmini.setShiftPressed(true);
                }
                if (mouseEvent.getButton() == 4) {
                    GameController.pressMinus(GameController.KEYREPEAT_KEY);
                }
                if (mouseEvent.getButton() == 5) {
                    GameController.pressPlus(GameController.KEYREPEAT_KEY);
                }
                break;
            default:
                break;
        }
    }
    
    public void mouseReleased(MouseEvent mouseEvent) {
        int x = mouseEvent.getX();
        int y = mouseEvent.getY();
        mouseDx = 0;
        mouseDy = 0;
        boolean swapButtons = GameController.doSwapButtons();
        switch (mouseEvent.getButton()) {
            case MouseEvent.BUTTON1:
                leftMousePressed = false;
                break;
            case MouseEvent.BUTTON2:
                if (swapButtons) {
                    rightMousePressed = false;
                } else {
                    middleMousePressed = false;
                }
            case MouseEvent.BUTTON3:
                if (swapButtons) {
                    middleMousePressed = false;
                } else {
                    rightMousePressed = false;
                }
            case 4:
                mouseButton4Pressed = false;
                break;
            case 5:
                mouseButton5Pressed = false;
                break;
            default:
                break;
        }

        switch (GameController.getGameState()) {
            case LEVEL:
                if (mouseEvent.getButton() == MouseEvent.BUTTON1) {
                    if (y > AbstractGameEngine.ICONS_Y && y < AbstractGameEngine.ICONS_Y + Icons.HEIGHT) {
                        Icons.Type type = GameController.getIconType(x);
                        if (type != Icons.Type.INVALID) {
                            GameController.releaseIcon(type);
                        }
                    }
                    // always release icons which don't stay pressed
                    // this is to avoid the icons get stuck when they're pressed,
                    // the the mouse is dragged out and released outside
                    GameController.releasePlus(GameController.KEYREPEAT_ICON);
                    GameController.releaseMinus(GameController.KEYREPEAT_ICON);
                    GameController.releaseIcon(Icons.Type.MINUS);
                    GameController.releaseIcon(Icons.Type.PLUS);
                    GameController.releaseIcon(Icons.Type.NUKE);
                }
                if (mouseEvent.getButton() == (swapButtons ? MouseEvent.BUTTON2 : MouseEvent.BUTTON3)) {
                    if (LemmCursor.getType() == LemmCursor.CursorType.WALKER) {
                        Lemmini.setCursor(LemmCursor.CursorType.NORMAL);
                    } else if (LemmCursor.getType() == LemmCursor.CursorType.WALKER_LEFT) {
                        Lemmini.setCursor(LemmCursor.CursorType.LEFT);
                    } else if (LemmCursor.getType() == LemmCursor.CursorType.WALKER_RIGHT) {
                        Lemmini.setCursor(LemmCursor.CursorType.RIGHT);
                    }
                    Lemmini.setShiftPressed(false);
                }
                if (mouseEvent.getButton() == 4) {
                    GameController.releaseMinus(GameController.KEYREPEAT_KEY);
                }
                if (mouseEvent.getButton() == 5) {
                    GameController.releasePlus(GameController.KEYREPEAT_KEY);
                }
                mouseEvent.consume();
                break;
            default:
                break;
        }
    }
    
    public void mouseEntered(final MouseEvent mouseEvent) {
        mouseDx = 0;
        mouseDy = 0;
        int x = mouseEvent.getX()/*-LemmCursor.width/2*/;
        int y = mouseEvent.getY()/*-LemmCursor.height/2*/;
        LemmCursor.setX(x/*-LemmCursor.width/2*/);
        LemmCursor.setY(y/*-LemmCursor.height/2*/);
    }

    public void mouseExited(final MouseEvent mouseEvent) {
        int x = xMouseScreen + mouseDx;
        switch (GameController.getGameState()) {
            case BRIEFING:
            case DEBRIEFING:
            case LEVEL:
                if (x >= this.getWidth()) {
                    x = this.getWidth() - 1;
                }
                if (x < 0) {
                    x = 0;
                }
                xMouseScreen = x;
                x += GameController.getXPos();
                //if (x >= GameController.getWidth()) {
                //    x = GameController.getWidth() - 1;
                //}
                //if (x < 0) {
                //    x = 0;
                //}
                xMouse = x;
                LemmCursor.setX(xMouseScreen/*-LemmCursor.width/2*/);

                int y = yMouseScreen + mouseDy;
                if (y >= this.getHeight()) {
                    y = this.getHeight() - 1;
                }
                if (y < 0) {
                    y = 0;
                }
                yMouseScreen = y;

                y = yMouse + mouseDy;
                //if (y >= Level.DEFAULT_HEIGHT) {
                //    y = Level.DEFAULT_HEIGHT - 1;
                //}
                if (y >= this.getHeight()) {
                    y = this.getHeight() - 1;
                }
                if (y < 0) {
                    y = 0;
                }
                yMouse = y;
                LemmCursor.setY(yMouseScreen/*-LemmCursor.height/2*/);
                mouseEvent.consume();
                break;
            default:
                break;
        }
    }

    /* (non-Javadoc)
     * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
     */
    public void mouseDragged(final MouseEvent mouseEvent) {
        mouseDx = 0;
        mouseDy = 0;
        // check minimap mouse move
        switch (GameController.getGameState()) {
            case LEVEL:
                int x = mouseEvent.getX();
                int y = mouseEvent.getY();
                if (leftMousePressed) {
                    int ofs = Minimap.move(x, y, this.getWidth());
                    if (ofs != -1) {
                        GameController.setXPos(ofs);
                    }
                }
                if (middleMousePressed) {
                    int xOfsTemp = GameController.getXPos() + (x - mouseDragStartX);
                    GameController.setXPos(xOfsTemp);
                }
                // debug drawing
                if (leftMousePressed || (GameController.doSwapButtons() ? middleMousePressed : rightMousePressed)) {
                    debugDraw(x, y, leftMousePressed);
                }
                mouseMoved(mouseEvent);
                mouseEvent.consume();
                break;
            default:
                break;
        }
    }

    /* (non-Javadoc)
     * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
     */
    public void mouseMoved(final MouseEvent mouseEvent) {
        //long t = System.currentTimeMillis();
        int x, y;
        int oldX = xMouse;
        int oldY = yMouse;

        x = (mouseEvent.getX() + GameController.getXPos());
        y = mouseEvent.getY();
        //if (x >= GameController.getWidth()) {
        //    x = GameController.getWidth() - 1;
        //} else if (x < 0) {
        //    x = 0;
        //}
        //if (y >= Level.DEFAULT_HEIGHT) {
        //    y = Level.DEFAULT_HEIGHT - 1;
        //} else if (y < 0) {
        //    y = 0;
        //}
        xMouse = x;
        yMouse = y;
        // LemmCursor
        xMouseScreen = mouseEvent.getX();
        if (xMouseScreen >= this.getWidth()) {
            xMouseScreen = this.getWidth();
        } else if (xMouseScreen < 0) {
            xMouseScreen = 0;
        }
        yMouseScreen = mouseEvent.getY();
        if (yMouseScreen >= this.getHeight()) {
            yMouseScreen = this.getHeight() - 1;
        } else if (yMouseScreen < 0) {
            yMouseScreen = 0;
        }
        LemmCursor.setX(xMouseScreen/*-LemmCursor.width/2*/);
        LemmCursor.setY(yMouseScreen/*-LemmCursor.height/2*/);

        switch (GameController.getGameState()) {
            case INTRO:
            case BRIEFING:
            case DEBRIEFING:
                TextScreen.getDialog().handleMouseMove(xMouseScreen, yMouseScreen);
                /* falls through */
            case LEVEL:
                mouseDx = (xMouse - oldX);
                mouseDy = (yMouse - oldY);
                mouseDragStartX = mouseEvent.getX();
                mouseEvent.consume();
                break;
            default:
                break;
        }
    }

    /**
     * redraw the offscreen image, then flip buffers and force repaint.
     */
    private void redraw() {
        int drawBuffer;
        GraphicsContext offGfx;

        synchronized (paintSemaphore) {
            if (offImage == null) {
                init();
            }
            drawBuffer = (activeBuffer == 0) ? 1 : 0;
            offGfx = offGraphics[drawBuffer];
        }

        Image fgImage = GameController.getFgImage();
        switch (GameController.getGameState()) {
            case INTRO:
                TextScreen.setMode(TextScreen.Mode.INTRO);
                TextScreen.update();
                offGfx.drawImage(TextScreen.getScreen(), 0, 0);
                break;
            case BRIEFING:
                TextScreen.setMode(TextScreen.Mode.BRIEFING);
                TextScreen.update();
                offGfx.drawImage(TextScreen.getScreen(), 0, 0);
                break;
            case DEBRIEFING:
                TextScreen.setMode(TextScreen.Mode.DEBRIEFING);
                TextScreen.update();
                offGfx.drawImage(TextScreen.getScreen(), 0, 0);
                TextScreen.getDialog().handleMouseMove(xMouseScreen, yMouseScreen);
                break;
            case LEVEL:
            case LEVEL_END:
                if (fgImage != null) {
                    GameController.update();
                    // mouse movement
                    if (yMouseScreen > 40 && yMouseScreen < SCORE_Y) { // avoid scrolling if menu is selected
                        int xOfsTemp;
                        if (xMouseScreen > this.getWidth() - AUTOSCROLL_RANGE) {
                            xOfsTemp = GameController.getXPos() + (isShiftPressed() ? X_STEP_FAST : X_STEP);
                            if (xOfsTemp < GameController.getWidth() - this.getWidth()) {
                                GameController.setXPos(xOfsTemp);
                            } else {
                                GameController.setXPos(GameController.getWidth() - this.getWidth());
                            }
                        } else if (xMouseScreen < AUTOSCROLL_RANGE) {
                            xOfsTemp = GameController.getXPos() - (isShiftPressed() ? X_STEP_FAST : X_STEP);
                            if (xOfsTemp > 0) {
                                GameController.setXPos(xOfsTemp);
                            } else {
                                GameController.setXPos(0);
                            }
                        }
                    }
                    // store local copy of xOfs to avoid sync problems with AWT threads
                    // (scrolling by dragging changes xOfs as well)
                    int xOfsTemp = GameController.getXPos();

                    int w = this.getWidth();
                    int h = Level.DEFAULT_HEIGHT;
                    if (h > this.getHeight()) {
                        h = this.getHeight();
                    }

                    Level level = GameController.getLevel();
                    if (level != null) {

                        // clear screen
                        offGfx.setClip(0, 0, w, h);
                        offGfx.setBackground(level.getBgColor());
                        offGfx.clearRect(0, 0, w, h);
                        
                        // draw background
                        GameController.getLevel().drawBackground(offGfx, w, xOfsTemp, 1, 1);

                        // draw "behind" objects
                        GameController.getLevel().drawBehindObjects(offGfx, w, xOfsTemp);

                        // draw foreground
                        offGfx.drawImage(fgImage, 0, 0, w, h, xOfsTemp, 0, xOfsTemp + w, h);

                        // draw "in front" objects
                        GameController.getLevel().drawInFrontObjects(offGfx, w, xOfsTemp);
                    }
                    // clear parts of the screen for menu etc.
                    offGfx.setClip(0, Level.DEFAULT_HEIGHT, w, this.getHeight());
                    offGfx.setBackground(Color.BLACK);
                    offGfx.clearRect(0, SCORE_Y, w, this.getHeight());
                    // draw counter, icons, small level pic
                    // draw menu
                    GameController.drawIcons(offGfx, 0, ICONS_Y);
                    offGfx.drawImage(MiscGfx.getMinimapImage(), SMALL_X - 4, SMALL_Y - 4);
                    Minimap.draw(offGfx, SMALL_X, SMALL_Y);
                    // draw counters
                    GameController.drawCounters(offGfx, COUNTER_Y);

                    // draw lemmings
                    offGfx.setClip(0, 0, w, h);
                    GameController.getLemmsUnderCursor().clear();
                    List<Lemming> lemmings = GameController.getLemmings();
                    synchronized (lemmings) {
                        for (Lemming l : lemmings) {
                            int lx = l.screenX();
                            int ly = l.screenY();
                            int mx = l.midX() - 14;
                            if (lx + l.width() > xOfsTemp && lx < xOfsTemp + w) {
                                offGfx.drawImage(l.getImage(), lx - xOfsTemp, ly);
                                if (LemmCursor.doesCollide(l, xOfsTemp)) {
                                    GameController.getLemmsUnderCursor().addFirst(l);
                                }
                                Image cd = l.getCountdown();
                                if (cd != null) {
                                    offGfx.drawImage(cd, mx - xOfsTemp, ly - cd.getHeight());
                                }

                                Image sel = l.getSelectImg();
                                if (sel != null) {
                                    offGfx.drawImage(sel, mx - xOfsTemp, ly - sel.getHeight());
                                }

                            }
                        }
                        // draw pixels in minimap
                        offGfx.setClip(0, 0, w, this.getHeight());
                        for (Lemming l : lemmings) {
                            int lx = l.midX();
                            int ly = l.midY();
                            // draw pixel in minimap
                            Minimap.drawLemming(offGfx, lx, ly);
                        }
                    }
                    Minimap.drawFrame(offGfx, SMALL_X, SMALL_Y, xOfsTemp);
                    Lemming lemmUnderCursor = GameController.lemmUnderCursor(LemmCursor.getType());
                    offGfx.setClip(0, 0, w, h);
                    // draw explosions
                    GameController.drawExplosions(offGfx, offImage[0].getWidth(), Level.DEFAULT_HEIGHT, xOfsTemp);
                    offGfx.setClip(0, 0, w, this.getHeight());

                    // draw info string
                    outStrGfx.clearRect(0, 0, outStrImg.getWidth(), outStrImg.getHeight());
                    if (GameController.isCheat()) {
                        Stencil stencil = GameController.getStencil();
                        if (stencil != null) {
                            int stencilVal = stencil.getMask(xMouse, yMouse);
                            int stencilObject = stencil.getMaskObjectID(xMouse, yMouse);
                            String strObj;
                            if (stencilObject >= 0) {
                                strObj = ", Obj: " + stencilObject;
                            } else {
                                strObj = "";
                            }
                            String test = String.format("X: %4d, Y: %3d, Mask: %3d%s", xMouse, yMouse, stencilVal, strObj);
                            if (Lemmini.getDebugDraw()) {
                                test = String.format("%-38s%s", test, "(draw)");
                            }
                            LemmFont.strImage(outStrGfx, test);
                            offGfx.drawImage(outStrImg, 4, Level.DEFAULT_HEIGHT + 8);
                        }
                    } else {
                        String lemmingName;
                        if (lemmUnderCursor != null) {
                            lemmingName = lemmUnderCursor.getName();
                            // display also the total number of lemmings under the cursor
                            int num = GameController.getLemmsUnderCursor().size();
                            if (num > 1) {
                                lemmingName += " " + num;
                            }
                        } else {
                            lemmingName = "";
                        }
                        String in;
                        if (GameController.isNoPercentages() || GameController.getNumLemmingsMax() > 100) {
                            in = Integer.toString(GameController.getNumLeft());
                        } else {
                            int saved = GameController.getNumLeft() * 100 / GameController.getNumLemmingsMax();
                            in = String.format("%02d%%", saved);
                        }
                        String status = String.format("%-15s OUT %-4d IN %-4s TIME %s", lemmingName, GameController.getLemmings().size(), in, GameController.getTimeString());
                        LemmFont.strImage(outStrGfx, status);
                        offGfx.drawImage(outStrImg, 4, Level.DEFAULT_HEIGHT + 8);
                    }
                    // replay icon
                    Image replayImage = GameController.getReplayImage();
                    if (replayImage != null) {
                        offGfx.drawImage(replayImage, this.getWidth() - 2 * replayImage.getWidth(), replayImage.getHeight());
                    }
                    // draw cursor
                    if (lemmUnderCursor != null) {
                        int lx = lemmUnderCursor.midX() - xOfsTemp;
                        int ly = lemmUnderCursor.midY();
                        Image cursorImg = LemmCursor.getBoxImage();
                        lx -= cursorImg.getWidth() / 2;
                        ly -= cursorImg.getHeight() / 2;
                        offGfx.drawImage(cursorImg, lx, ly);
                    }
                }
                break;
            default:
                break;
        }

        // fader
        GameController.fade(offGfx);
        // and all onto screen
        activeBuffer = drawBuffer;

        repaint();
    }
    
    public Object getPaintSemaphore() {
        return paintSemaphore;
    }
    
    public Image getActiveBuffer() {
        if (offImage == null) {
            return null;
        }
        return offImage[activeBuffer];
    }
    
    public int getMouseX() {
        return xMouse;
    }

    public int getMouseY() {
        return yMouse;
    }
    
    protected abstract boolean isRunning();

    protected abstract Object getParent();
    
    protected abstract boolean isShiftPressed();
    
    protected abstract void repaint();

    protected abstract void debugDraw(int x, int y, boolean leftMousePressed);
}
