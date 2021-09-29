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
package lemmini;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.JOptionPane;
import lemmini.game.*;
import lemmini.gameutil.Fader;
import lemmini.graphics.GraphicsContext;
import lemmini.graphics.LemmImage;
import lemmini.gui.LevelCodeDialog;
import lemmini.gui.LevelDialog;
import lemmini.gui.OptionsDialog;
import lemmini.gui.PlayerDialog;
import lemmini.tools.ToolBox;

/**
 * A graphics panel in which the actual game contents is displayed.
 * @author Volker Oth
 */
public class LemminiPanel extends javax.swing.JPanel implements Runnable {
    
    private static final long serialVersionUID = 0x01L;
    
    /** step size in pixels for horizontal scrolling */
    static final int X_STEP = 4;
    /** step size in pixels for fast horizontal scrolling */
    static final int X_STEP_FAST = 16;
    /** size of auto scrolling range in pixels (from the left and right border) */
    static final int AUTOSCROLL_RANGE = 20;
    /** y coordinate of score display in pixels */
    static final int SCORE_Y = LemminiFrame.LEVEL_HEIGHT;
    /** x coordinate of counter displays in pixels */
    static final int COUNTER_X = 32;
    /** y coordinate of counter displays in pixels */
    static final int COUNTER_Y = SCORE_Y + 40;
    /** x coordinate of icons in pixels */
    static final int ICONS_X = COUNTER_X;
    /** y coordinate of icons in pixels */
    static final int ICONS_Y = COUNTER_Y + 14;
    /** x coordinate of minimap in pixels */
    static final int SMALL_X = ICONS_X + 32 * 14 + 16;
    /** y coordinate of minimap in pixels */
    static final int SMALL_Y = ICONS_Y;

    private int menuOffsetX;
    /** start x position of mouse drag (for mouse scrolling) */
    private int mouseDragStartX;
    /** start y position of mouse drag (for mouse scrolling) */
    private int mouseDragStartY;
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
    /** flag: shift key is pressed */
    private boolean shiftPressed;
    /** flag: left key is pressed */
    private boolean leftPressed;
    /** flag: right key is pressed */
    private boolean rightPressed;
    /** flag: up key is pressed */
    private boolean upPressed;
    /** flag: down key is pressed */
    private boolean downPressed;
    /** flag: left mouse button is currently pressed */
    private boolean leftMousePressed = false;
    /** flag: middle mouse button is currently pressed */
    private boolean middleMousePressed = false;
    /** flag: right mouse button is currently pressed */
    private boolean rightMousePressed = false;
    /** flag: mouse button 4 is currently pressed */
    private boolean mouseButton4Pressed = false;
    /** flag: mouse button 5 is currently pressed */
    private boolean mouseButton5Pressed = false;
    /** flag: debug draw is active */
    private boolean draw;
    private boolean isFocused;
    private boolean mouseHasEntered;
    private boolean holdingMinimap;
    /** image for information string display */
    private LemmImage outStrImg;
    /** graphics object for information string display */
    private GraphicsContext outStrGfx;
    /** offscreen image */
    private LemmImage offImage;
    /** graphics object for the offscreen image */
    private GraphicsContext offGraphics;
    /** monitoring object used for synchronized painting */
    private final Object paintSemaphore = new Object();
    private boolean drawNextFrame;
    private int unmaximizedWidth = 0;
    private int unmaximizedHeight = 0;

    /**
     * Creates new form LemminiPanel
     */
    public LemminiPanel() {
        isFocused = true;
        mouseHasEntered = true;
        holdingMinimap = false;
        shiftPressed = false;
        initComponents();
        unmaximizedWidth = getWidth();
        unmaximizedHeight = getHeight();
    }
    
    /**
     * Initialization.
     */
    void init() {
        setBufferSize(Core.unscale(getWidth()), Core.unscale(getHeight()));
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setBackground(new java.awt.Color(0, 0, 0));
        setMinimumSize(new java.awt.Dimension(800, 450));
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                formMouseDragged(evt);
            }
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                formMouseMoved(evt);
            }
        });
        addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                formMouseWheelMoved(evt);
            }
        });
        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                formMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                formMouseExited(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                formMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                formMouseReleased(evt);
            }
        });
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 800, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 450, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void formMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseEntered
        mouseDx = 0;
        mouseDy = 0;
        int x = Core.unscale(evt.getX());
        int y = Core.unscale(evt.getY());
        LemmCursor.setX(x);
        LemmCursor.setY(y);
        if (isFocused) {
            mouseHasEntered = true;
        }
    }//GEN-LAST:event_formMouseEntered

    private void formMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseExited
        switch (GameController.getGameState()) {
            case BRIEFING:
            case DEBRIEFING:
            case LEVEL:
                int x = xMouseScreen + Core.scale(mouseDx);
                if (x >= getWidth()) {
                    x = getWidth() - 1;
                }
                if (x < 0) {
                    x = 0;
                }
                xMouseScreen = x;
                x = Core.unscale(x) + GameController.getXPos();
                xMouse = x;
                LemmCursor.setX(Core.unscale(xMouseScreen));

                int y = yMouseScreen + Core.scale(mouseDy);
                if (y >= getHeight()) {
                    y = getHeight() - 1;
                }
                if (y < 0) {
                    y = 0;
                }
                yMouseScreen = y;
                y = Core.unscale(y) + GameController.getYPos();
                yMouse = y;
                LemmCursor.setY(Core.unscale(yMouseScreen));
                evt.consume();
                break;
            default:
                break;
        }
    }//GEN-LAST:event_formMouseExited

    private void formMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMousePressed
        int x = Core.unscale(evt.getX());
        int y = Core.unscale(evt.getY());
        mouseDx = 0;
        mouseDy = 0;
        boolean swapButtons = GameController.doSwapButtons();
        switch (evt.getButton()) {
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
            case INTRO:
                TextScreen.Button button = TextScreen.getDialog().handleLeftClick(
                        x - Core.getDrawWidth() / 2, y - Core.getDrawHeight() / 2);
                switch (button) {
                    case PLAY_LEVEL:
                        handlePlayLevel();
                        break;
                    case LOAD_REPLAY:
                        handleLoadReplay();
                        break;
                    case ENTER_CODE:
                        handleEnterCode();
                        break;
                    case PLAYERS:
                        handlePlayers();
                        break;
                    case OPTIONS:
                        handleOptions();
                        break;
                    case EXIT:
                        getParentFrame().exit();
                        break;
                    default:
                        break;
                }
                evt.consume();
                break;
            case BRIEFING:
                button = TextScreen.getDialog().handleLeftClick(
                        x - Core.getDrawWidth() / 2, y - Core.getDrawHeight() / 2);
                switch (button) {
                    case START_LEVEL:
                        Minimap.init(16, 8, true);
                        GameController.setTransition(GameController.TransitionState.TO_LEVEL);
                        Fader.setState(Fader.State.OUT);
                        GameController.resetGain();
                        break;
                    case MENU:
                        GameController.setTransition(GameController.TransitionState.TO_INTRO);
                        Fader.setState(Fader.State.OUT);
                        Core.setTitle("SuperLemmini");
                        break;
                    default:
                        break;
                }
                evt.consume();
                break;
            case DEBRIEFING:
                button = TextScreen.getDialog().handleLeftClick(
                        x - Core.getDrawWidth() / 2, y - Core.getDrawHeight() / 2);
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
                        Path replayPath = ToolBox.getFileName(getParent(), Core.resourcePath, Core.REPLAY_EXTENSIONS, false, false);
                        if (replayPath != null) {
                            try {
                                String ext = ToolBox.getExtension(replayPath.getFileName().toString());
                                if (ext == null || ext.isEmpty()) {
                                    replayPath = replayPath.resolveSibling(replayPath.getFileName().toString() + "." + Core.REPLAY_EXTENSIONS[0]);
                                }
                                if (GameController.saveReplay(replayPath)) {
                                    return;
                                }
                                // else: no success
                                JOptionPane.showMessageDialog(getParent(), "Unable to save replay.", "Error", JOptionPane.ERROR_MESSAGE);
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
                evt.consume();
                break;
            case LEVEL:
                //  debug drawing
                if (leftMousePressed || (swapButtons ? middleMousePressed : rightMousePressed)) {
                    debugDraw(x, y, leftMousePressed);
                }
                if (evt.getButton() == MouseEvent.BUTTON1) {
                    if (y >= ICONS_Y && y < ICONS_Y + Icons.HEIGHT) {
                        Icons.Type type = GameController.getIconType(x - menuOffsetX - ICONS_X);
                        if (type != null) {
                            GameController.handleIconButton(type);
                        }
                    } else {
                        if (y < LemminiFrame.LEVEL_HEIGHT) {
                            GameController.stopReplayMode();
                            if (GameController.isCheat()) {
                                GameController.advanceFrame();
                            }
                        }
                        Lemming l = GameController.lemmUnderCursor(LemmCursor.getType());
                        if (l != null) {
                            GameController.requestSkill(l);
                        }
                    }
                    // check minimap mouse move
                    if (x >= SMALL_X + menuOffsetX && x < SMALL_X + menuOffsetX + Minimap.getVisibleWidth()
                            && y >= SMALL_Y && y < SMALL_Y + Minimap.getVisibleHeight()) {
                        holdingMinimap = true;
                    }
                    evt.consume();
                } 
                if (evt.getButton() == (swapButtons ? MouseEvent.BUTTON2 : MouseEvent.BUTTON3)) {
                    if (LemmCursor.getType() == LemmCursor.CursorType.LEFT) {
                        setCursor(LemmCursor.CursorType.WALKER_LEFT);
                    } else if (LemmCursor.getType() == LemmCursor.CursorType.RIGHT) {
                        setCursor(LemmCursor.CursorType.WALKER_RIGHT);
                    } else if (LemmCursor.getType() == LemmCursor.CursorType.NORMAL) {
                        setCursor(LemmCursor.CursorType.WALKER);
                    }
                    shiftPressed = true;
                }
                if (evt.getButton() == 4) {
                    GameController.pressMinus(GameController.KEYREPEAT_KEY);
                }
                if (evt.getButton() == 5) {
                    GameController.pressPlus(GameController.KEYREPEAT_KEY);
                }
                break;
            default:
                break;
        }
    }//GEN-LAST:event_formMousePressed

    private void formMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseReleased
        int x = Core.unscale(evt.getX());
        int y = Core.unscale(evt.getY());
        mouseDx = 0;
        mouseDy = 0;
        boolean swapButtons = GameController.doSwapButtons();
        switch (evt.getButton()) {
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
                if (evt.getButton() == MouseEvent.BUTTON1) {
                    holdingMinimap = false;
                    if (y > ICONS_Y && y < ICONS_Y + Icons.HEIGHT) {
                        Icons.Type type = GameController.getIconType(x - menuOffsetX - ICONS_X);
                        if (type != null) {
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
                if (evt.getButton() == (swapButtons ? MouseEvent.BUTTON2 : MouseEvent.BUTTON3)) {
                    if (LemmCursor.getType() == LemmCursor.CursorType.WALKER) {
                        setCursor(LemmCursor.CursorType.NORMAL);
                    } else if (LemmCursor.getType() == LemmCursor.CursorType.WALKER_LEFT) {
                        setCursor(LemmCursor.CursorType.LEFT);
                    } else if (LemmCursor.getType() == LemmCursor.CursorType.WALKER_RIGHT) {
                        setCursor(LemmCursor.CursorType.RIGHT);
                    }
                    shiftPressed = false;
                }
                if (evt.getButton() == 4) {
                    GameController.releaseMinus(GameController.KEYREPEAT_KEY);
                }
                if (evt.getButton() == 5) {
                    GameController.releasePlus(GameController.KEYREPEAT_KEY);
                }
                evt.consume();
                break;
            default:
                break;
        }
    }//GEN-LAST:event_formMouseReleased

    private void formMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseDragged
        mouseDx = 0;
        mouseDy = 0;
        // check minimap mouse move
        switch (GameController.getGameState()) {
            case LEVEL:
                int x = Core.unscale(evt.getX());
                int y = Core.unscale(evt.getY());
                if (middleMousePressed) {
                    int xOfsTemp = GameController.getXPos() + (x - mouseDragStartX);
                    GameController.setXPos(xOfsTemp);
                    if (!GameController.isVerticalLock()) {
                        int yOfsTemp = GameController.getYPos() + (y - mouseDragStartY);
                        GameController.setYPos(yOfsTemp);
                    }
                    Minimap.adjustXPos();
                }
                // debug drawing
                if (leftMousePressed || (GameController.doSwapButtons() ? middleMousePressed : rightMousePressed)) {
                    debugDraw(x, y, leftMousePressed);
                }
                formMouseMoved(evt);
                evt.consume();
                break;
            default:
                break;
        }
    }//GEN-LAST:event_formMouseDragged

    private void formMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseMoved
        int oldX = xMouse;
        int oldY = yMouse;

        xMouse = Core.unscale(evt.getX()) + GameController.getXPos();
        yMouse = Core.unscale(evt.getY()) + GameController.getYPos();
        // LemmCursor
        xMouseScreen = evt.getX();
        if (xMouseScreen >= getWidth()) {
            xMouseScreen = getWidth();
        } else if (xMouseScreen < 0) {
            xMouseScreen = 0;
        }
        yMouseScreen = evt.getY();
        if (yMouseScreen >= getHeight()) {
            yMouseScreen = getHeight() - 1;
        } else if (yMouseScreen < 0) {
            yMouseScreen = 0;
        }
        LemmCursor.setX(Core.unscale(xMouseScreen));
        LemmCursor.setY(Core.unscale(yMouseScreen));
        
        if (isFocused) {
            mouseHasEntered = true;
        }

        switch (GameController.getGameState()) {
            case INTRO:
            case BRIEFING:
            case DEBRIEFING:
                TextScreen.getDialog().handleMouseMove(
                        Core.unscale(xMouseScreen) - Core.getDrawWidth() / 2,
                        Core.unscale(yMouseScreen) - Core.getDrawHeight() / 2);
                /* falls through */
            case LEVEL:
                mouseDx = (xMouse - oldX);
                mouseDy = (yMouse - oldY);
                mouseDragStartX = Core.unscale(evt.getX());
                mouseDragStartY = Core.unscale(evt.getY());
                evt.consume();
                break;
            default:
                break;
        }
    }//GEN-LAST:event_formMouseMoved

    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
        mouseHasEntered = false;
        setSize(getWidth(), getHeight());
    }//GEN-LAST:event_formComponentResized

    private void formMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_formMouseWheelMoved
        if (GameController.getGameState() == GameController.State.LEVEL) {
            int wheelRotation = evt.getWheelRotation();
            if (wheelRotation > 0) {
                for (int i = 0; i < wheelRotation; i++) {
                    GameController.nextSkill();
                }
            } else if (wheelRotation < 0) {
                for (int i = 0; i > wheelRotation; i--) {
                    GameController.previousSkill();
                }
            }
        }
    }//GEN-LAST:event_formMouseWheelMoved
    
    /**
     * Set cursor type.
     * @param c Cursor
     */
    public void setCursor(final LemmCursor.CursorType c) {
        LemmCursor.setType(c);
        super.setCursor(LemmCursor.getCursor());
    }
    
    @Override
    public void paint(final Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        synchronized (paintSemaphore) {
            if (offImage != null) {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        Core.isBilinear()
                                ? RenderingHints.VALUE_INTERPOLATION_BILINEAR
                                : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g2.drawImage(offImage.getImage(),
                        0, 0, Core.getScaledDrawWidth(), Core.getScaledDrawHeight(),
                        0, 0, Core.getDrawWidth(), Core.getDrawHeight(), null);
            }
        }
    }
    
    @Override
    public void update(final Graphics g) {
        paint(g);
    }
    
    void focusLost() {
        shiftPressed = false;
        leftPressed = false;
        rightPressed = false;
        upPressed = false;
        downPressed = false;
        leftMousePressed = false;
        middleMousePressed = false;
        rightMousePressed = false;
        mouseButton4Pressed = false;
        mouseButton5Pressed = false;
        GameController.releasePlus(GameController.KEYREPEAT_ICON | GameController.KEYREPEAT_KEY);
        GameController.releaseMinus(GameController.KEYREPEAT_ICON | GameController.KEYREPEAT_KEY);
        GameController.releaseIcon(Icons.Type.MINUS);
        GameController.releaseIcon(Icons.Type.PLUS);
        GameController.releaseIcon(Icons.Type.NUKE);
        LemmCursor.setBox(false);
        setCursor(LemmCursor.CursorType.NORMAL);
        isFocused = false;
        mouseHasEntered = false;
        holdingMinimap = false;
    }
    
    void focusGained() {
        isFocused = true;
    }
    
    /**
     * redraw the offscreen image, then flip buffers and force repaint.
     */
    private void redraw() {
        if (offImage == null || offGraphics == null) {
            return;
        }
        
        synchronized (paintSemaphore) {
            GraphicsContext offGfx = offGraphics;

            switch (GameController.getGameState()) {
                case INTRO:
                case BRIEFING:
                case DEBRIEFING:
                    offGfx.setClip(0, 0, Core.getDrawWidth(), Core.getDrawHeight());
                    TextScreen.drawScreen(offGfx, 0, 0, Core.getDrawWidth(), Core.getDrawHeight());
                    break;
                case LEVEL:
                case LEVEL_END:
                    LemmImage fgImage = GameController.getFgImage();
                    if (fgImage != null) {
                        // store local copy of offsets to avoid sync problems with AWT threads
                        // (scrolling by dragging changes offsets as well)
                        int xOfsTemp = GameController.getXPos();
                        int minimapXOfsTemp = Minimap.getXPos();
                        int yOfsTemp = GameController.getYPos();

                        int width = Core.getDrawWidth();
                        int height = Core.getDrawHeight();
                        int levelHeight = Math.min(LemminiFrame.LEVEL_HEIGHT, height);

                        Level level = GameController.getLevel();
                        if (level != null) {

                            // clear screen
                            offGfx.setClip(0, 0, width, levelHeight);
                            offGfx.setBackground(level.getBgColor());
                            offGfx.clearRect(0, 0, width, levelHeight);

                            // draw background
                            GameController.getLevel().drawBackground(offGfx, width, levelHeight, xOfsTemp, yOfsTemp, 1, 1);

                            // draw "behind" objects
                            GameController.getLevel().drawBehindObjects(offGfx, width, height, xOfsTemp, yOfsTemp);

                            // draw foreground
                            offGfx.drawImage(fgImage, 0, 0, width, levelHeight, xOfsTemp, yOfsTemp, xOfsTemp + width, yOfsTemp + levelHeight);

                            // draw "in front" objects
                            GameController.getLevel().drawInFrontObjects(offGfx, width, height, xOfsTemp, yOfsTemp);
                        }
                        // clear parts of the screen for menu etc.
                        offGfx.setClip(0, LemminiFrame.LEVEL_HEIGHT, width, height - LemminiFrame.LEVEL_HEIGHT);
                        offGfx.setBackground(Color.BLACK);
                        offGfx.clearRect(0, SCORE_Y, width, height - SCORE_Y);
                        // draw counter, icons, small level pic
                        // draw menu
                        GameController.drawIcons(offGfx, menuOffsetX + ICONS_X, ICONS_Y);
                        // draw counters
                        GameController.drawCounters(offGfx, menuOffsetX + COUNTER_X, COUNTER_Y);
                        // draw minimap
                        offGfx.drawImage(MiscGfx.getMinimapImage(), menuOffsetX + SMALL_X - 4, SMALL_Y - 4);
                        offGfx.setClip(menuOffsetX + SMALL_X, SMALL_Y, Minimap.getVisibleWidth(), Minimap.getVisibleHeight());
                        Minimap.draw(offGfx, menuOffsetX + SMALL_X, SMALL_Y);
                        GameController.drawMinimapLemmings(offGfx, menuOffsetX + SMALL_X, SMALL_Y);
                        offGfx.setClip(0, 0, width, height);
                        Minimap.drawFrame(offGfx, menuOffsetX + SMALL_X, SMALL_Y);
                        // draw minimap arrows
                        if (minimapXOfsTemp > 0) {
                            LemmImage leftArrow = MiscGfx.getImage(MiscGfx.Index.MINIMAP_ARROW_LEFT);
                            offGfx.drawImage(leftArrow,
                                    SMALL_X - 4 - leftArrow.getWidth(),
                                    SMALL_Y + Minimap.getVisibleHeight() / 2 - leftArrow.getHeight() / 2);
                        }
                        if (minimapXOfsTemp < GameController.getWidth() / Minimap.getScaleX() - Minimap.getVisibleWidth()) {
                            LemmImage rightArrow = MiscGfx.getImage(MiscGfx.Index.MINIMAP_ARROW_RIGHT);
                            offGfx.drawImage(rightArrow,
                                    SMALL_X + Minimap.getVisibleWidth() + 4,
                                    SMALL_Y + Minimap.getVisibleHeight() / 2 - rightArrow.getHeight() / 2);
                        }
                        if (yOfsTemp > 0) {
                            LemmImage upArrow = MiscGfx.getImage(MiscGfx.Index.MINIMAP_ARROW_UP);
                            offGfx.drawImage(upArrow,
                                    menuOffsetX + SMALL_X + Minimap.getVisibleWidth() / 2 - upArrow.getWidth() / 2,
                                    SMALL_Y - 4 - upArrow.getHeight());
                        }
                        if (yOfsTemp < GameController.getHeight() - LemminiFrame.LEVEL_HEIGHT) {
                            LemmImage downArrow = MiscGfx.getImage(MiscGfx.Index.MINIMAP_ARROW_DOWN);
                            offGfx.drawImage(downArrow,
                                    menuOffsetX + SMALL_X + Minimap.getVisibleWidth() / 2 - downArrow.getWidth() / 2,
                                    SMALL_Y + Minimap.getVisibleHeight() + 4);
                        }

                        // draw lemmings
                        offGfx.setClip(0, 0, width, levelHeight);
                        GameController.drawLemmings(offGfx);
                        Lemming lemmUnderCursor = GameController.lemmUnderCursor(LemmCursor.getType());
                        offGfx.setClip(0, 0, width, levelHeight);
                        // draw explosions
                        GameController.drawExplosions(offGfx, width, LemminiFrame.LEVEL_HEIGHT, xOfsTemp, yOfsTemp);
                        offGfx.setClip(0, 0, width, height);

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
                                String test = String.format("X: %4d, Y: %3d, Mask: %4d%s", xMouse, yMouse, stencilVal, strObj);
                                if (draw) {
                                    test = String.format("%-38s%s", test, "(draw)");
                                }
                                LemmFont.strImage(outStrGfx, test);
                                offGfx.drawImage(outStrImg, menuOffsetX + 4, LemminiFrame.LEVEL_HEIGHT + 8);
                            }
                        } else {
                            String lemmingName;
                            if (lemmUnderCursor != null) {
                                lemmingName = lemmUnderCursor.getName();
                                // display also the total number of lemmings under the cursor
                                int num = GameController.getNumLemmsUnderCursor();
                                if (num > 1) {
                                    lemmingName += " " + num;
                                }
                            } else {
                                lemmingName = "";
                            }
                            String in;
                            if (GameController.isNoPercentages() || GameController.getNumLemmingsMax() > 100) {
                                in = Integer.toString(GameController.getNumExited());
                            } else {
                                int saved = GameController.getNumExited() * 100 / GameController.getNumLemmingsMax();
                                in = String.format("%02d%%", saved);
                            }
                            String status = String.format("%-15s OUT %-4d IN %-4s TIME %s", lemmingName, GameController.getNumLemmings(), in, GameController.getTimeString());
                            LemmFont.strImage(outStrGfx, status);
                            offGfx.drawImage(outStrImg, menuOffsetX + 4, LemminiFrame.LEVEL_HEIGHT + 8);
                        }
                        // replay icon
                        LemmImage replayImage = GameController.getReplayImage();
                        if (replayImage != null) {
                            offGfx.drawImage(replayImage, width - 2 * replayImage.getWidth(), replayImage.getHeight());
                        }
                        // draw cursor
                        if (lemmUnderCursor != null) {
                            if (GameController.isClassicCursor()) {
                                if (mouseHasEntered && !LemmCursor.isBox()) {
                                    LemmCursor.setBox(true);
                                    setCursor(LemmCursor.getCursor());
                                }
                            } else {
                                int lx = lemmUnderCursor.midX() - xOfsTemp;
                                int ly = lemmUnderCursor.midY() - yOfsTemp;
                                LemmImage cursorImg = LemmCursor.getBoxImage();
                                lx -= cursorImg.getWidth() / 2;
                                ly -= cursorImg.getHeight() / 2;
                                offGfx.drawImage(cursorImg, lx, ly);
                            }
                        } else {
                            if (GameController.isClassicCursor() && LemmCursor.isBox()) {
                                LemmCursor.setBox(false);
                                setCursor(LemmCursor.getCursor());
                            }
                        }
                    }
                    break;
                default:
                    break;
            }

            // fader
            Fader.apply(offGfx);

            repaint();
        }
    }
    
    private void updateFrame() {
        LemmImage fgImage = GameController.getFgImage();
        switch (GameController.getGameState()) {
            case INTRO:
                TextScreen.setMode(TextScreen.Mode.INTRO);
                TextScreen.update();
                break;
            case BRIEFING:
                TextScreen.setMode(TextScreen.Mode.BRIEFING);
                TextScreen.update();
                break;
            case DEBRIEFING:
                TextScreen.setMode(TextScreen.Mode.DEBRIEFING);
                TextScreen.update();
                TextScreen.getDialog().handleMouseMove(
                        Core.unscale(xMouseScreen) - Core.getDrawWidth() / 2,
                        Core.unscale(yMouseScreen) - Core.getDrawHeight() / 2);
                break;
            case LEVEL:
            case LEVEL_END:
                if (fgImage != null) {
                    GameController.update();
                    // store local copy of xOfs to avoid sync problems with AWT threads
                    // (scrolling by dragging changes xOfs as well)
                    int xOfsTemp = GameController.getXPos();
                    int minimapXOfsTemp = Minimap.getXPos();
                    int yOfsTemp = GameController.getYPos();
                    // mouse movement
                    if (holdingMinimap) {
                        int framePos = xOfsTemp / Minimap.getScaleX() - minimapXOfsTemp;
                        if (xMouseScreen < Core.scale(SMALL_X) && framePos <= 0) {
                            xOfsTemp -= getStepSize();
                            GameController.setXPos(xOfsTemp);
                        } else if (xMouseScreen >= Core.scale(SMALL_X + Minimap.getVisibleWidth()) && framePos >= Minimap.getVisibleWidth() - Core.getDrawWidth() / Minimap.getScaleX()) {
                            xOfsTemp += getStepSize();
                            GameController.setXPos(xOfsTemp);
                        } else {
                            xOfsTemp = Minimap.move(Core.unscale(xMouseScreen) - SMALL_X - menuOffsetX, Core.unscale(yMouse) - SMALL_Y);
                            GameController.setXPos(xOfsTemp);
                        }
                        if (!GameController.isVerticalLock()) {
                            if (yMouseScreen < Core.scale(SMALL_Y)) {
                                yOfsTemp -= getStepSize();
                                GameController.setYPos(yOfsTemp);
                            } else if (yMouseScreen >= Core.scale(SMALL_Y + Minimap.getVisibleHeight())) {
                                yOfsTemp += getStepSize();
                                GameController.setYPos(yOfsTemp);
                            }
                        }
                    } else if (mouseHasEntered) {
                        if (xMouseScreen >= getWidth() - Core.scale(AUTOSCROLL_RANGE)) {
                            xOfsTemp += getStepSize();
                            GameController.setXPos(xOfsTemp);
                        } else if (xMouseScreen < Core.scale(AUTOSCROLL_RANGE)) {
                            xOfsTemp -= getStepSize();
                            GameController.setXPos(xOfsTemp);
                        }
                        if (!GameController.isVerticalLock()) {
                            if (yMouseScreen >= getHeight() - Core.scale(AUTOSCROLL_RANGE)) {
                                yOfsTemp += getStepSize();
                                GameController.setYPos(yOfsTemp);
                            } else if (yMouseScreen < Core.scale(AUTOSCROLL_RANGE)) {
                                yOfsTemp -= getStepSize();
                                GameController.setYPos(yOfsTemp);
                            }
                        }
                    }
                    if (rightPressed) {
                        xOfsTemp += getStepSize();
                        GameController.setXPos(xOfsTemp);
                    }
                    if (leftPressed) {
                        xOfsTemp -= getStepSize();
                        GameController.setXPos(xOfsTemp);
                    }
                    if (!GameController.isVerticalLock()) {
                        if (downPressed) {
                            yOfsTemp += getStepSize();
                            GameController.setYPos(yOfsTemp);
                        }
                        if (upPressed) {
                            yOfsTemp -= getStepSize();
                            GameController.setYPos(yOfsTemp);
                        }
                    }
                    Minimap.adjustXPos();

                    GameController.updateLemmsUnderCursor();
                }
                break;
            default:
                break;
        }

        // fader
        GameController.fade();
    }
    
    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 1);
        final LemminiPanel thisPanel = this;
        ScheduledExecutorService repaintScheduler = Executors.newSingleThreadScheduledExecutor();
        Runnable repaintTask = new Runnable() {
            @Override
            public void run() {
                thisPanel.drawNextFrame();
            }
        };
        
        try {
            drawNextFrame = false;
            repaintScheduler.scheduleAtFixedRate(
                    repaintTask, 0, GameController.NANOSEC_PER_FRAME, TimeUnit.NANOSECONDS);
            while (true) {
                synchronized (this) {
                    while (!drawNextFrame) {
                        try {
                            wait();
                        } catch (InterruptedException ex) {
                        }
                    }
                }
                
                if (drawNextFrame) {
                    drawNextFrame = false;
                    // time passed -> redraw necessary
                    GameController.State gameState = GameController.getGameState();
                    // special handling for fast forward or super lemming mode only during real gameplay
                    if (gameState == GameController.State.LEVEL) {
                        // in fast forward or super lemming modes, update the game mechanics
                        // multiple times per (drawn) frame
                        if (GameController.isFastForward()) {
                            int multiplier = (GameController.isFasterFastForward() ? GameController.FASTER_FAST_FWD_MULTI : GameController.FAST_FWD_MULTI);
                            for (int f = 1; f < multiplier; f++) {
                                GameController.update();
                            }
                        } else if (GameController.isSuperLemming()) {
                            for (int f = 1; f < GameController.SUPERLEMM_MULTI; f++) {
                                GameController.update();
                            }
                        }
                    }
                    updateFrame();
                    redraw();
                }
            }
        } catch (Exception ex) {
            ToolBox.showException(ex);
            System.exit(1);
        }
    }
    
    /**
     * Signals that the next frame should be drawn.
     */
    public synchronized void drawNextFrame() {
        drawNextFrame = true;
        notifyAll();
    }
    
    /**
     * Debug routine to draw terrain pixels in stencil and foreground image.
     * @param x x position in pixels
     * @param y y position in pixels
     * @param doDraw true: draw, false: erase
     */
    private void debugDraw(final int x, final int y, final boolean doDraw) {
        if (draw && GameController.isCheat()) {
            boolean classicSteel = GameController.getLevel().getClassicSteel();
            int rgbVal = (doDraw) ? 0xffffffff : 0x0;
            int minimapVal = rgbVal;
            if (doDraw && Minimap.isTinted()) {
                minimapVal = Minimap.tintColor(minimapVal);
            }
            int xOfs = GameController.getXPos();
            int yOfs = GameController.getYPos();
            LemmImage fgImage = GameController.getFgImage();
            LemmImage fgImageSmall = Minimap.getImage();
            Stencil stencil = GameController.getStencil();
            int scaleX = fgImage.getWidth() / fgImageSmall.getWidth();
            int scaleY = fgImage.getHeight() / fgImageSmall.getHeight();
            for (int ya = y; ya < y + 2; ya++) {
                boolean drawSmallY = (ya % scaleY) == 0;
                for (int xa = x; xa < x + 2; xa++) {
                    boolean drawSmallX = (x % scaleX) == 0;
                    if (xa + xOfs >= 0 && xa + xOfs < GameController.getWidth()
                            && ya + yOfs >= 0 && ya + yOfs < GameController.getHeight()) {
                        int[] objects = stencil.getIDs(xa + xOfs, ya + yOfs);
                        for (int obj : objects) {
                            SpriteObject spr = GameController.getLevel().getSprObject(obj);
                            if (spr != null && spr.getVisOnTerrain()) {
                                spr.setPixelVisibility(xa + xOfs - spr.getX(), ya + yOfs - spr.getY(), doDraw);
                            }
                        }
                        if (!doDraw) {
                            stencil.andMask(xa + xOfs, ya + yOfs,
                                    classicSteel ? ~Stencil.MSK_BRICK
                                    : ~(Stencil.MSK_BRICK | Stencil.MSK_STEEL | Stencil.MSK_NO_BASH));
                        } else {
                            stencil.orMask(xa + xOfs, ya + yOfs, Stencil.MSK_BRICK);
                        }
                        GameController.getFgImage().setRGB(xa + xOfs, ya + yOfs, rgbVal);
                        if (drawSmallX && drawSmallY) {
                            fgImageSmall.setRGB((xa + xOfs) / scaleX, (ya + yOfs) / scaleY, minimapVal);
                        }
                    }
                }
            }
        }
    }
    
    void handlePlayLevel() {
        LevelDialog ld = new LevelDialog(getParentFrame(), true);
        ld.setVisible(true);
        int[] level = ld.getSelectedLevel();
        if (level != null) {
            GameController.requestChangeLevel(level[0], level[1], level[2], false);
            getParentFrame().setRestartEnabled(true);
        }
    }
    
    void handleLoadReplay() {
        Path replayPath = ToolBox.getFileName(getParentFrame(), Core.resourcePath, Core.REPLAY_EXTENSIONS, true, false);
        if (replayPath != null) {
            try {
                if (ToolBox.getExtension(replayPath.getFileName().toString()).equalsIgnoreCase("rpl")) {
                    ReplayLevelInfo rli = GameController.loadReplay(replayPath);
                    if (rli != null) {
                        int lpn = -1;
                        for (int i = 0; i < GameController.getLevelPackCount(); i++) {
                            String packName = Normalizer.normalize(
                                    GameController.getLevelPack(i).getName(), Normalizer.Form.NFKC);
                            if (packName.equals(rli.getLevelPack())) {
                                lpn = i;
                            }
                        }
                        if (lpn > -1) {
                            // success
                            GameController.requestChangeLevel(lpn, rli.getRating(), rli.getLvlNumber(), true);
                            getParentFrame().setRestartEnabled(true);
                        } else {
                            // no success
                            JOptionPane.showMessageDialog(getParent(),
                                    "Level specified in replay file does not exist.",
                                    "Load Replay", JOptionPane.ERROR_MESSAGE);
                        }
                        return;
                    }
                }
                // else: no success
                JOptionPane.showMessageDialog(getParent(), "Wrong format!", "Load Replay", JOptionPane.ERROR_MESSAGE);
            } catch (LemmException ex) {
                JOptionPane.showMessageDialog(getParent(), ex.getMessage(), "Load Replay", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                ToolBox.showException(ex);
            }
        }
    }
    
    void handleEnterCode() {
        LevelCodeDialog lcd = new LevelCodeDialog(getParentFrame(), true);
        lcd.setVisible(true);
        String levelCode = lcd.getCode();
        int lvlPack = lcd.getLevelPack();
        if (levelCode != null && !levelCode.isEmpty() && lvlPack > 0) {
            levelCode = levelCode.trim();
            // cheat mode
            if (levelCode.equalsIgnoreCase("0xdeadbeef")) {
                JOptionPane.showMessageDialog(getParent(), "All levels and debug mode enabled.", "Cheater!", JOptionPane.INFORMATION_MESSAGE);
                Core.player.enableCheatMode();
                return;
            }

            // real level code -> get absolute level
            levelCode = levelCode.toUpperCase();
            LevelPack lpack = GameController.getLevelPack(lvlPack);
            int[] codeInfo = LevelCode.getLevel(lpack.getCodeSeed(), levelCode, lpack.getCodeOffset());
            if (codeInfo != null) {
                if (Core.player.isCheat()) {
                    JOptionPane.showMessageDialog(getParent(),
                            String.format("Level: %d%nPercent Saved: %d%%%nTimes Failed: %d%nUnknown: %d",
                                    codeInfo[0] + 1, codeInfo[1], codeInfo[2], codeInfo[3]),
                            "Code Info",
                            JOptionPane.INFORMATION_MESSAGE);
                }
                // calculate level pack and relative level number from absolute number
                int[] l = GameController.relLevelNum(lvlPack, codeInfo[0]);
                int rating = l[0];
                int lvlRel = l[1];
                if (rating >= 0 && lvlRel >= 0) {
                    Core.player.setAvailable(lpack.getName(), lpack.getRatings()[rating], lvlRel);
                    GameController.requestChangeLevel(lvlPack, rating, lvlRel, false);
                    getParentFrame().setRestartEnabled(true);
                    return;
                }
            }
        }
        // not found
        if (lvlPack != -1) {
            JOptionPane.showMessageDialog(getParent(), "Invalid Level Code.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    void handlePlayers() {
        Core.player.store(); // save player in case it is changed
        PlayerDialog d = new PlayerDialog(getParentFrame(), true);
        d.setVisible(true);
        // blocked until dialog returns
        java.util.List<String> players = d.getPlayers();
        if (players != null) {
            String player = Core.player.getName(); // old player
            int playerIdx = d.getSelection();
            if (playerIdx != -1) {
                player = players.get(playerIdx); // remember selected player
            }
            // check for players to delete
            for (int i = 0; i < Core.getPlayerNum(); i++) {
                String p = Core.getPlayer(i);
                if (!players.contains(p)) {
                    Path f = Core.resourcePath.resolve("players").resolve(p + ".ini");
                    try {
                        Files.deleteIfExists(f);
                    } catch (IOException ex) {
                    }
                    if (p.equals(player)) {
                        player = "default";
                    }
                }
            }
            // rebuild players list
            Core.clearPlayers();
            // add default player if missing
            if (!players.contains("default")) {
                players.add("default");
            }
            // now copy all players and create properties
            for (String p : players) {
                Core.addPlayer(p);
            }

            // select new default player
            if (!Core.player.getName().equals(player)
                    && GameController.getGameState() != GameController.State.INTRO) {
                if (GameController.getGameState() == GameController.State.LEVEL) {
                    GameController.setGameState(GameController.State.LEVEL_END);
                }
                GameController.setTransition(GameController.TransitionState.TO_INTRO);
                Fader.setState(Fader.State.OUT);
                Core.setTitle("SuperLemmini");
            }
            Core.player = new Player(player);
        }
    }
    
    void handleOptions() {
        OptionsDialog d = new OptionsDialog(getParentFrame(), true);
        d.setVisible(true);
    }
    
    @Override
    public void setSize(Dimension d) {
        setSize(d.width, d.height);
    }
    
    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        LemminiFrame parentFrame = getParentFrame();
        int frameState = 0;
        if (parentFrame != null) {
            frameState = parentFrame.getExtendedState();
        }
        if ((frameState & Frame.MAXIMIZED_HORIZ) == 0) {
            unmaximizedWidth = width;
        }
        if ((frameState & Frame.MAXIMIZED_VERT) == 0) {
            unmaximizedHeight = height;
        }
        setScale(getWidth(), getHeight());
        Core.setDrawSize(Core.unscale(getWidth()), Core.unscale(getHeight()));
        setBufferSize(Core.getDrawWidth(), Core.getDrawHeight());
        // if possible, make sure that the screen is not positioned outside the level
        GameController.setXPos(GameController.getXPos());
        GameController.setYPos(GameController.getYPos());
    }
    
    private void setScale(int width, int height) {
        Dimension minSize = getMinimumSize();
        if ((double) width / (double) height >= (double) minSize.width / (double) minSize.height) {
            Core.setScale((double) height / (double) minSize.height);
        } else {
            Core.setScale((double) width / minSize.width);
        }
    }
    
    private void setBufferSize(int width, int height) {
        if (width <= 0 || height <= 0 || LemmFont.getHeight() <= 0) {
            return;
        }
        
        int w = width;
        int h = height;
        
        synchronized (paintSemaphore) {
            if (offImage != null) {
                w = Math.max(w, offImage.getWidth());
                h = Math.max(h, offImage.getHeight());
            }
        }
        if (w > 2) {
            w = Integer.highestOneBit((w - 1) << 1);
        }
        if (h > 2) {
            h = Integer.highestOneBit((h - 1) << 1);
        }
        
        synchronized (paintSemaphore) {
            if (offImage == null || w > offImage.getWidth() || h > offImage.getHeight()) {
                if (offGraphics != null) {
                    offGraphics.dispose();
                }
                offImage = ToolBox.createOpaqueImage(w, h);
                offGraphics = offImage.createGraphicsContext();
                offGraphics.setBackground(new Color(0, 0, 0));
            }
            
            if (outStrImg == null || w > outStrImg.getWidth()) {
                if (outStrGfx != null) {
                    outStrGfx.dispose();
                }
                outStrImg = ToolBox.createTranslucentImage(w, LemmFont.getHeight());
                outStrGfx = outStrImg.createGraphicsContext();
            }

            menuOffsetX = Math.max(0, (width - getMinimumSize().width) / 2);
        }
    }
    
    private LemminiFrame getParentFrame() {
        Container container = getParent();
        while (container != null && !(container instanceof LemminiFrame)) {
            container = container.getParent();
        }
        return (LemminiFrame) container;
    }
    
    int getUnmaximizedWidth() {
        return unmaximizedWidth;
    }
    
    int getUnmaximizedHeight() {
        return unmaximizedHeight;
    }
    
    /**
     * Get cursor x position in pixels.
     * @return cursor x position in pixels
     */
    int getCursorX() {
        return xMouse;
    }

    /**
     * Get cursor y position in pixels.
     * @return cursor y position in pixels
     */
    int getCursorY() {
        return yMouse;
    }

    /**
     * Get flag: Shift key is pressed?
     * @return true if shift key is pressed, false otherwise
     */
    boolean isShiftPressed() {
        return shiftPressed;
    }

    /**
     * Set flag: Shift key is pressed.
     * @param p true: Shift key is pressed, false otherwise
     */
    void setShiftPressed(final boolean p) {
        shiftPressed = p;
    }

    /**
     * Get flag: Left key is pressed?
     * @return true if left key is pressed, false otherwise
     */
    boolean isLeftPressed() {
        return leftPressed;
    }

    /**
     * Set flag: Left key is pressed.
     * @param p true: Left key is pressed, false otherwise
     */
    void setLeftPressed(final boolean p) {
        leftPressed = p;
    }

    /**
     * Get flag: Right key is pressed?
     * @return true if right key is pressed, false otherwise
     */
    boolean isRightPressed() {
        return rightPressed;
    }

    /**
     * Set flag: Right key is pressed.
     * @param p true: Right key is pressed, false otherwise
     */
    void setRightPressed(final boolean p) {
        rightPressed = p;
    }

    /**
     * Get flag: Up key is pressed?
     * @return true if up key is pressed, false otherwise
     */
    boolean isUpPressed() {
        return upPressed;
    }

    /**
     * Set flag: Up key is pressed.
     * @param p true: Up key is pressed, false otherwise
     */
    void setUpPressed(final boolean p) {
        upPressed = p;
    }

    /**
     * Get flag: Down key is pressed?
     * @return true if down key is pressed, false otherwise
     */
    boolean isDownPressed() {
        return downPressed;
    }

    /**
     * Set flag: Down key is pressed.
     * @param p true: Down key is pressed, false otherwise
     */
    void setDownPressed(final boolean p) {
        downPressed = p;
    }

    /**
     * Get state of debug draw option.
     * @return true: debug draw is active, false otherwise
     */
    boolean getDebugDraw() {
        return draw;
    }

    /**
     * Set state of debug draw option.
     * @param d true: debug draw is active, false otherwise
     */
    void setDebugDraw(final boolean d) {
        draw = d;
    }
    
    private int getStepSize() {
        return (shiftPressed ? X_STEP_FAST : X_STEP);
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}