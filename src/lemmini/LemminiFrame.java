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

import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import keyrepeatfix.RepeatingReleasedEventsFixer;
import lemmini.extract.Extract;
import lemmini.extract.ExtractException;
import lemmini.game.*;
import lemmini.gameutil.Fader;
import lemmini.graphics.LemmImage;
import lemmini.sound.Music;
import lemmini.tools.ToolBox;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.JavaVersion;
import org.apache.commons.lang3.SystemUtils;

/**
 * Lemmini - a game engine for Lemmings.<br>
 * This is the main window including input handling. The game logic is located in
 * {@link GameController}, some core components are in {@link Core}.
 *
 * @author Volker Oth
 */
public class LemminiFrame extends JFrame {
    
    public static final int LEVEL_HEIGHT = 320;
    public static final String REVISION = "1.40";
    public static final String REV_DATE = "15 Nov 2021";
    
    private static final long serialVersionUID = 0x01L;
    
    private int unmaximizedPosX;
    private int unmaximizedPosY;
    
    private static boolean createPatches = false;
    
    /** self reference */
    static LemminiFrame thisFrame;
    
    /**
     * Creates new form LemminiFrame
     */
    public LemminiFrame() {
        try {
        	//found at: https://stackoverflow.com/questions/2837263/how-do-i-get-the-directory-that-the-currently-executing-jar-file-is-in
        	String currentFolderStr = URLDecoder.decode(getClass().getProtectionDomain().getCodeSource().getLocation().getFile(), "UTF-8");
        	
        	boolean successful = Core.init(createPatches, currentFolderStr); // initialize Core object
            if (!successful) {
                System.exit(0);
            }
        } catch (LemmException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        } catch (Throwable ex) {
            ToolBox.showException(ex);
            System.exit(1);
        }
        
        initComponents();
        setMinimumSize(getSize());
        RepeatingReleasedEventsFixer.install();
    }
    
    void init() {
        try {
            // initialize the game controller and main panel
            GameController.init();
            lemminiPanelMain.init();
            lemminiPanelMain.setCursor(LemmCursor.getCursor());
            
            // load the panel size
            int w = Math.max(lemminiPanelMain.getWidth(), Core.programProps.getInt("frameWidth", lemminiPanelMain.getWidth()));
            int h = Math.max(lemminiPanelMain.getHeight(), Core.programProps.getInt("frameHeight", lemminiPanelMain.getHeight()));
            lemminiPanelMain.setSize(w, h);
            lemminiPanelMain.setPreferredSize(lemminiPanelMain.getSize()); // needed for pack() to keep this size
            pack();
            // center the window, then load the window position
            setLocationRelativeTo(null);
            int posX = Core.programProps.getInt("framePosX", getX());
            int posY = Core.programProps.getInt("framePosY", getY());
            setLocation(posX, posY);
            // load the maximized state
            int maximizedState = 0;
            if (Core.programProps.getBoolean("maximizedHoriz", false)) {
                maximizedState |= MAXIMIZED_HORIZ;
            }
            if (Core.programProps.getBoolean("maximizedVert", false)) {
                maximizedState |= MAXIMIZED_VERT;
            }
            setExtendedState(getExtendedState() | maximizedState);
            
            GameController.setGameState(GameController.State.INTRO);
            GameController.setTransition(GameController.TransitionState.NONE);
            Fader.setState(Fader.State.IN);
            
            Thread t = new Thread(lemminiPanelMain);
            t.start();
            
            setVisible(true);
        } catch (ResourceException ex) {
            Core.resourceError(ex.getMessage());
        } catch (Throwable ex) {
            ToolBox.showException(ex);
            System.exit(1);
        }
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lemminiPanelMain = new lemmini.LemminiPanel();
        jMenuBarMain = new javax.swing.JMenuBar();
        jMenuFile = new javax.swing.JMenu();
        jMenuItemExit = new javax.swing.JMenuItem();
        jMenuItemFileExtract = new javax.swing.JMenuItem();
        jMenuPlayers = new javax.swing.JMenu();
        jMenuItemManagePlayers = new javax.swing.JMenuItem();
        jMenuLevel = new javax.swing.JMenu();
        jMenuItemPlayLevel = new javax.swing.JMenuItem();
        jMenuItemRestartLevel = new javax.swing.JMenuItem();
        jMenuItemLoadReplay = new javax.swing.JMenuItem();
        jMenuItemEnterLevelCode = new javax.swing.JMenuItem();
        jMenuOptions = new javax.swing.JMenu();
        jMenuItemOptions = new javax.swing.JMenuItem();
        jMenuItemAbout = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("SuperLemminiToo");
        setIconImage(Toolkit.getDefaultToolkit().getImage(LemminiFrame.class.getClassLoader().getResource("icon_32.png")));
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentMoved(java.awt.event.ComponentEvent evt) {
                formComponentMoved(evt);
            }
        });
        addWindowFocusListener(new java.awt.event.WindowFocusListener() {
            public void windowGainedFocus(java.awt.event.WindowEvent evt) {
                formWindowGainedFocus(evt);
            }
            public void windowLostFocus(java.awt.event.WindowEvent evt) {
                formWindowLostFocus(evt);
            }
        });
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                formKeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                formKeyReleased(evt);
            }
        });

        javax.swing.GroupLayout lemminiPanelMainLayout = new javax.swing.GroupLayout(lemminiPanelMain);
        lemminiPanelMain.setLayout(lemminiPanelMainLayout);
        lemminiPanelMainLayout.setHorizontalGroup(
            lemminiPanelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 800, Short.MAX_VALUE)
        );
        lemminiPanelMainLayout.setVerticalGroup(
            lemminiPanelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 450, Short.MAX_VALUE)
        );

        jMenuFile.setText("File");

        jMenuItemExit.setText("Exit");
        jMenuItemExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemExitActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemExit);

        jMenuItemFileExtract.setText("Extract ...");
        jMenuItemFileExtract.setVisible(false);
        jMenuItemFileExtract.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
            	jMenuItemFileExtractActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemFileExtract);
        
        jMenuBarMain.add(jMenuFile);

        jMenuPlayers.setText("Players");

        jMenuItemManagePlayers.setText("Manage Players...");
        jMenuItemManagePlayers.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemManagePlayersActionPerformed(evt);
            }
        });
        jMenuPlayers.add(jMenuItemManagePlayers);

        jMenuBarMain.add(jMenuPlayers);

        jMenuLevel.setText("Level");

        jMenuItemPlayLevel.setText("Select Level...");
        jMenuItemPlayLevel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemPlayLevelActionPerformed(evt);
            }
        });
        jMenuLevel.add(jMenuItemPlayLevel);

        jMenuItemRestartLevel.setText("Restart Level");
        jMenuItemRestartLevel.setEnabled(false);
        jMenuItemRestartLevel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemRestartLevelActionPerformed(evt);
            }
        });
        jMenuLevel.add(jMenuItemRestartLevel);

        jMenuItemLoadReplay.setText("Load Replay...");
        jMenuItemLoadReplay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemLoadReplayActionPerformed(evt);
            }
        });
        jMenuLevel.add(jMenuItemLoadReplay);

        jMenuItemEnterLevelCode.setText("Enter Level Code...");
        jMenuItemEnterLevelCode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemEnterLevelCodeActionPerformed(evt);
            }
        });
        jMenuLevel.add(jMenuItemEnterLevelCode);

        jMenuBarMain.add(jMenuLevel);

        jMenuOptions.setText("Options");

        jMenuItemOptions.setText("Options...");
        jMenuItemOptions.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemOptionsActionPerformed(evt);
            }
        });
        
        jMenuItemAbout.setText("About...");
        jMenuItemAbout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
            	String msg = "";
            	msg += "Java version: " + System.getProperty("java.version").toString();
            	// msg += "\n";
            	// msg += "Current Folder:" + getClass().getProtectionDomain().getCodeSource().getLocation();
            	JOptionPane.showMessageDialog(thisFrame, msg);
            }
        });
        
        
        jMenuOptions.add(jMenuItemOptions);
        jMenuOptions.add(jMenuItemAbout);

        jMenuBarMain.add(jMenuOptions);

        setJMenuBar(jMenuBarMain);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(lemminiPanelMain, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lemminiPanelMain, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    
    private void formComponentMoved(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentMoved
        storeUnmaximizedPos();
    }//GEN-LAST:event_formComponentMoved
    
    private void formKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_formKeyPressed
        int code = evt.getKeyCode();
        switch (GameController.getGameState()) {
            case LEVEL:
                switch (code) {
                    case KeyEvent.VK_1:
                    case KeyEvent.VK_F3:
                        GameController.handleIconButton(Icons.IconType.CLIMB);
                        break;
                    case KeyEvent.VK_2:
                    case KeyEvent.VK_F4:
                        GameController.handleIconButton(Icons.IconType.FLOAT);
                        break;
                    case KeyEvent.VK_3:
                    case KeyEvent.VK_F5:
                        GameController.handleIconButton(Icons.IconType.BOMB);
                        break;
                    case KeyEvent.VK_4:
                    case KeyEvent.VK_F6:
                        GameController.handleIconButton(Icons.IconType.BLOCK);
                        break;
                    case KeyEvent.VK_5:
                    case KeyEvent.VK_F7:
                        GameController.handleIconButton(Icons.IconType.BUILD);
                        break;
                    case KeyEvent.VK_6:
                    case KeyEvent.VK_F8:
                        GameController.handleIconButton(Icons.IconType.BASH);
                        break;
                    case KeyEvent.VK_7:
                    case KeyEvent.VK_F9:
                        GameController.handleIconButton(Icons.IconType.MINE);
                        break;
                    case KeyEvent.VK_8:
                    case KeyEvent.VK_F10:
                        GameController.handleIconButton(Icons.IconType.DIG);
                        break;
                    case KeyEvent.VK_D: //CTRL-SHIFT-D is to enter Debug mode. just D (while in Debug mode) is Draw mode
                    	if (lemminiPanelMain.isControlPressed() && lemminiPanelMain.isShiftPressed() && lemminiPanelMain.isAltPressed()) {
                            GameController.setCheat(!GameController.isCheat());
                        } else if (!lemminiPanelMain.isControlPressed() && !lemminiPanelMain.isShiftPressed() && !lemminiPanelMain.isAltPressed() && GameController.isCheat()) {
                            lemminiPanelMain.setDebugDraw(!lemminiPanelMain.getDebugDraw());
                        }
                        break;
                    case KeyEvent.VK_W:
                        if (GameController.isCheat()) {
                            GameController.setNumExited(GameController.getNumLemmingsMax());
                            GameController.endLevel();
                        }
                        break;
                    case KeyEvent.VK_L: // print current level on the console
                        if (GameController.isCheat()) {
                            System.out.println(GameController.getLevelPack(GameController.getCurLevelPackIdx()).getInfo(GameController.getCurRating(), GameController.getCurLevelNumber()).getLevelResource());
                        }
                        break;
                    case KeyEvent.VK_S:
                        GameController.setVerticalLock(!GameController.isVerticalLock());
                        GameController.pressIcon(Icons.IconType.VLOCK);
                        break;
                    case KeyEvent.VK_V:
                        LemmImage tmp = GameController.getLevel().createMinimap(GameController.getFgImage(), 1.0, 1.0, true, false, true);
                        try (OutputStream out = Core.resourceTree.newOutputStream("level.png")) {
                            ImageIO.write(tmp.getImage(), "png", out);
                        } catch (IOException ex) {
                        }
                        break;
                    case KeyEvent.VK_U: // superlemming on/off
                        if (GameController.isCheat()) {
                            GameController.setSuperLemming(!GameController.isSuperLemming());
                        }
                        break;
                    case KeyEvent.VK_C: //C toggles Cheat Mode, if the debug mode has been entered.
                        if (Core.player.isCheat()) {
                            GameController.setCheat(!GameController.isCheat());
                            if (GameController.isCheat()) {
                                GameController.setWasCheated(true);
                            }
                        } else {
                            GameController.setCheat(false);
                        }
                        break;
                    case KeyEvent.VK_SPACE:
                    case KeyEvent.VK_F11:
                    case KeyEvent.VK_P: //SPACE, F11 or P toggles Pause
                        boolean isPaused = GameController.isPaused();
                        if (GameController.isOptionEnabled(GameController.Option.PAUSE_STOPS_FAST_FORWARD)
                                && !isPaused && GameController.isFastForward()) {
                            GameController.setFastForward(false);
                            GameController.pressIcon(Icons.IconType.FFWD);
                        }
                        GameController.setPaused(!isPaused);
                        GameController.pressIcon(Icons.IconType.PAUSE);
                        break;
                    case KeyEvent.VK_F:
                    case KeyEvent.VK_ENTER: //F or ENTER toggles Fast-Forward
                        GameController.setFastForward(!GameController.isFastForward());
                        GameController.pressIcon(Icons.IconType.FFWD);
                        break;
                    case KeyEvent.VK_T:
                        if (GameController.isCheat()) {
                            GameController.setTimed(!GameController.isTimed());
                        }
                        break;
                    case KeyEvent.VK_R: //CTRL-R restarts the level.
                        if (lemminiPanelMain.isControlPressed() && !lemminiPanelMain.isShiftPressed()) {
                            GameController.requestRestartLevel(true, false);
                        }
                        break;
                    case KeyEvent.VK_RIGHT /*39*/:
                        if (GameController.isOptionEnabled(GameController.Option.ADVANCED_SELECT)) {
                            if (LemmCursor.getType().isWalkerOnly()) {
                                lemminiPanelMain.setCursor(LemmCursor.CursorType.WALKER_RIGHT);
                            } else {
                                lemminiPanelMain.setCursor(LemmCursor.CursorType.RIGHT);
                            }
                        } else {
                            lemminiPanelMain.setRightPressed(true);
                        }
                        break;
                    case KeyEvent.VK_LEFT /*37*/:
                        if (GameController.isOptionEnabled(GameController.Option.ADVANCED_SELECT)) {
                            if (LemmCursor.getType().isWalkerOnly()) {
                                lemminiPanelMain.setCursor(LemmCursor.CursorType.WALKER_LEFT);
                            } else {
                                lemminiPanelMain.setCursor(LemmCursor.CursorType.LEFT);
                            }
                        } else {
                            lemminiPanelMain.setLeftPressed(true);
                        }
                        break;
                    case KeyEvent.VK_UP:
                        if (GameController.isOptionEnabled(GameController.Option.ADVANCED_SELECT)) {
                            switch (LemmCursor.getType()) {
                                case NORMAL:
                                    lemminiPanelMain.setCursor(LemmCursor.CursorType.WALKER);
                                    break;
                                case LEFT:
                                    lemminiPanelMain.setCursor(LemmCursor.CursorType.WALKER_LEFT);
                                    break;
                                case RIGHT:
                                    lemminiPanelMain.setCursor(LemmCursor.CursorType.WALKER_RIGHT);
                                    break;
                                default:
                                    break;
                            }
                        } else {
                            lemminiPanelMain.setUpPressed(true);
                        }
                        break;
                    case KeyEvent.VK_DOWN:
                        if (!GameController.isOptionEnabled(GameController.Option.ADVANCED_SELECT)) {
                            lemminiPanelMain.setDownPressed(true);
                        }
                        break;
                    case KeyEvent.VK_SHIFT:
                        lemminiPanelMain.setShiftPressed(true);
                        break;
                    case KeyEvent.VK_CONTROL:
                        lemminiPanelMain.setControlPressed(true);
                        break;
                    case KeyEvent.VK_ALT:
                    	lemminiPanelMain.setAltPressed(true);
                        break;
                    case KeyEvent.VK_N:
                        if (GameController.isCheat()) {
                            Lemming l = new Lemming(lemminiPanelMain.getCursorX(), lemminiPanelMain.getCursorY(), Lemming.Direction.RIGHT);
                            GameController.addLemming(l);
                            Vsfx v = new Vsfx(lemminiPanelMain.getCursorX(), lemminiPanelMain.getCursorY(), Vsfx.Vsfx_Index.YIPPEE);
                            GameController.addVsfx(v);
                        }
                        break;
                    case KeyEvent.VK_PLUS:
                    case KeyEvent.VK_ADD:
                    case KeyEvent.VK_EQUALS:
                    case KeyEvent.VK_F2:
                        GameController.pressPlus(GameController.KEYREPEAT_KEY);
                        break;
                    case KeyEvent.VK_MINUS:
                    case KeyEvent.VK_SUBTRACT:
                    case KeyEvent.VK_F1:
                        GameController.pressMinus(GameController.KEYREPEAT_KEY);
                        break;
                    case KeyEvent.VK_F12:
                        GameController.handleIconButton(Icons.IconType.NUKE);
                        break;
                    case KeyEvent.VK_ESCAPE:
                        GameController.endLevel();
                        break;
                    default:
                        break;
                }
                evt.consume();
                break;
            case BRIEFING:
                key:
                switch (code) {
                    case KeyEvent.VK_LEFT:
                        if (Fader.getState() == Fader.State.OFF) {
                            LevelPack pack = GameController.getCurLevelPack();
                            String packName = pack.getName();
                            int packIdx = GameController.getCurLevelPackIdx();
                            List<String> ratings = pack.getRatings();
                            int rating = GameController.getCurRating();
                            int lvlNum = GameController.getCurLevelNumber() - 1;
                            while (rating >= 0) {
                                while (lvlNum >= 0) {
                                    if (Core.player.isAvailable(packName, ratings.get(rating), lvlNum)) {
                                        GameController.requestChangeLevel(packIdx, rating, lvlNum, false);
                                        break key;
                                    }
                                    lvlNum--;
                                }
                                rating--;
                                if (rating >= 0) {
                                    lvlNum = pack.getLevelCount(rating) - 1;
                                }
                            }
                        }
                        break;
                    case KeyEvent.VK_RIGHT:
                        if (Fader.getState() == Fader.State.OFF) {
                            LevelPack pack = GameController.getCurLevelPack();
                            String packName = pack.getName();
                            int packIdx = GameController.getCurLevelPackIdx();
                            List<String> ratings = pack.getRatings();
                            int rating = GameController.getCurRating();
                            int lvlCount = pack.getLevelCount(rating);
                            int lvlNum = GameController.getCurLevelNumber() + 1;
                            while (rating < ratings.size()) {
                                while (lvlNum < lvlCount) {
                                    if (Core.player.isAvailable(packName, ratings.get(rating), lvlNum)) {
                                        GameController.requestChangeLevel(packIdx, rating, lvlNum, false);
                                        break key;
                                    }
                                    lvlNum++;
                                }
                                rating++;
                                if (rating < ratings.size()) {
                                    lvlNum = 0;
                                }
                            }
                        }
                        break;
                    case KeyEvent.VK_UP:
                        if (Fader.getState() == Fader.State.OFF) {
                            LevelPack pack = GameController.getCurLevelPack();
                            String packName = pack.getName();
                            int packIdx = GameController.getCurLevelPackIdx();
                            List<String> ratings = pack.getRatings();
                            int rating = GameController.getCurRating() - 1;
                            int lvlNum = GameController.getCurLevelNumber();
                            while (rating >= 0) {
                                while (lvlNum >= 0) {
                                    if (Core.player.isAvailable(packName, ratings.get(rating), lvlNum)) {
                                        GameController.requestChangeLevel(packIdx, rating, lvlNum, false);
                                        break key;
                                    }
                                    lvlNum--;
                                }
                                rating--;
                                if (rating >= 0) {
                                    lvlNum = Math.min(pack.getLevelCount(rating), GameController.getCurLevelNumber());
                                }
                            }
                        }
                        break;
                    case KeyEvent.VK_DOWN:
                        if (Fader.getState() == Fader.State.OFF) {
                            LevelPack pack = GameController.getCurLevelPack();
                            String packName = pack.getName();
                            int packIdx = GameController.getCurLevelPackIdx();
                            List<String> ratings = pack.getRatings();
                            int rating = GameController.getCurRating() + 1;
                            int lvlNum = GameController.getCurLevelNumber();
                            while (rating < ratings.size()) {
                                while (lvlNum >= 0) {
                                    if (Core.player.isAvailable(packName, ratings.get(rating), lvlNum)) {
                                        GameController.requestChangeLevel(packIdx, rating, lvlNum, false);
                                        break key;
                                    }
                                    lvlNum--;
                                }
                                rating++;
                                if (rating < ratings.size()) {
                                    lvlNum = Math.min(pack.getLevelCount(rating), GameController.getCurLevelNumber());
                                }
                            }
                        }
                        break;
                    default:
                        break;
                }
                /* falls through */
            case DEBRIEFING:
            case LEVEL_END:
                switch (code) {
                    case KeyEvent.VK_V:
                        LemmImage tmp = GameController.getLevel().createMinimap(GameController.getFgImage(), 1.0, 1.0, true, false, true);
                        try (OutputStream out = Core.resourceTree.newOutputStream("level.png")) {
                            ImageIO.write(tmp.getImage(), "png", out);
                        } catch (IOException ex) {
                        }
                        break;
                    default:
                        break;
                }
                break;
            default:
                break;
        }
    }//GEN-LAST:event_formKeyPressed
    
    private void formKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_formKeyReleased
        int code = evt.getKeyCode();
        if (GameController.getGameState() == GameController.State.LEVEL) {
            switch (code) {
                case KeyEvent.VK_SHIFT:
                    lemminiPanelMain.setShiftPressed(false);
                    break;
                case KeyEvent.VK_CONTROL:
                    lemminiPanelMain.setControlPressed(false);
                    break;
                case KeyEvent.VK_ALT:
                    lemminiPanelMain.setAltPressed(false);
                    break;
                case KeyEvent.VK_PLUS:
                case KeyEvent.VK_ADD:
                case KeyEvent.VK_EQUALS:
                case KeyEvent.VK_F2:
                    GameController.releasePlus(GameController.KEYREPEAT_KEY);
                    break;
                case KeyEvent.VK_MINUS:
                case KeyEvent.VK_SUBTRACT:
                case KeyEvent.VK_F1:
                    GameController.releaseMinus(GameController.KEYREPEAT_KEY);
                    break;
                case KeyEvent.VK_F12:
                    GameController.releaseIcon(Icons.IconType.NUKE);
                    break;
                case KeyEvent.VK_LEFT:
                    if (GameController.isOptionEnabled(GameController.Option.ADVANCED_SELECT)) {
                        if (LemmCursor.getType() == LemmCursor.CursorType.LEFT) {
                            lemminiPanelMain.setCursor(LemmCursor.CursorType.NORMAL);
                        } else if (LemmCursor.getType() == LemmCursor.CursorType.WALKER_LEFT) {
                            lemminiPanelMain.setCursor(LemmCursor.CursorType.WALKER);
                        }
                    } else {
                        lemminiPanelMain.setLeftPressed(false);
                    }
                    break;
                case KeyEvent.VK_RIGHT:
                    if (GameController.isOptionEnabled(GameController.Option.ADVANCED_SELECT)) {
                        if (LemmCursor.getType() == LemmCursor.CursorType.RIGHT) {
                            lemminiPanelMain.setCursor(LemmCursor.CursorType.NORMAL);
                        } else if (LemmCursor.getType() == LemmCursor.CursorType.WALKER_RIGHT) {
                            lemminiPanelMain.setCursor(LemmCursor.CursorType.WALKER);
                        }
                    } else {
                        lemminiPanelMain.setRightPressed(false);
                    }
                    break;
                case KeyEvent.VK_UP:
                    if (GameController.isOptionEnabled(GameController.Option.ADVANCED_SELECT)) {
                        switch (LemmCursor.getType()) {
                            case WALKER:
                                lemminiPanelMain.setCursor(LemmCursor.CursorType.NORMAL);
                                break;
                            case WALKER_LEFT:
                                lemminiPanelMain.setCursor(LemmCursor.CursorType.LEFT);
                                break;
                            case WALKER_RIGHT:
                                lemminiPanelMain.setCursor(LemmCursor.CursorType.RIGHT);
                                break;
                            default:
                                break;
                        }
                    } else {
                        lemminiPanelMain.setUpPressed(false);
                    }
                    break;
                case KeyEvent.VK_DOWN:
                    if (!GameController.isOptionEnabled(GameController.Option.ADVANCED_SELECT)) {
                        lemminiPanelMain.setDownPressed(false);
                    }
                    break;
                default:
                    break;
            }
        }
    }//GEN-LAST:event_formKeyReleased
    
    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        exit();
    }//GEN-LAST:event_formWindowClosed
    
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        exit();
    }//GEN-LAST:event_formWindowClosing
    
    private void formWindowGainedFocus(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowGainedFocus
        lemminiPanelMain.focusGained();
    }//GEN-LAST:event_formWindowGainedFocus
    
    private void formWindowLostFocus(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowLostFocus
        lemminiPanelMain.focusLost();
    }//GEN-LAST:event_formWindowLostFocus
    
    private void jMenuItemExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemExitActionPerformed
        exit();
    }//GEN-LAST:event_jMenuItemExitActionPerformed
    
    private void jMenuItemFileExtractActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemExitActionPerformed
    	try {
			Extract.extract(Core.resourcePath, Core.resourceTree, Paths.get("reference"), Paths.get("patch"), true, false);
		} catch (ExtractException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }//GEN-LAST:event_jMenuItemExitActionPerformed

    private void jMenuItemManagePlayersActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemManagePlayersActionPerformed
        lemminiPanelMain.handlePlayers();
    }//GEN-LAST:event_jMenuItemManagePlayersActionPerformed
    
    private void jMenuItemPlayLevelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemPlayLevelActionPerformed
        lemminiPanelMain.handlePlayLevel();
    }//GEN-LAST:event_jMenuItemPlayLevelActionPerformed
    
    private void jMenuItemRestartLevelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemRestartLevelActionPerformed
        if (GameController.getLevel() == null) {
            GameController.requestChangeLevel(GameController.getCurLevelPackIdx(), GameController.getCurRating(), GameController.getCurLevelNumber(), false);
        } else {
            GameController.requestRestartLevel(false, true);
        }
    }//GEN-LAST:event_jMenuItemRestartLevelActionPerformed
    
    private void jMenuItemLoadReplayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemLoadReplayActionPerformed
        lemminiPanelMain.handleLoadReplay();
    }//GEN-LAST:event_jMenuItemLoadReplayActionPerformed
    
    private void jMenuItemEnterLevelCodeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemEnterLevelCodeActionPerformed
        lemminiPanelMain.handleEnterCode();
    }//GEN-LAST:event_jMenuItemEnterLevelCodeActionPerformed
    
    private void jMenuItemOptionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemOptionsActionPerformed
        lemminiPanelMain.handleOptions();
    }//GEN-LAST:event_jMenuItemOptionsActionPerformed
    
    /**
     * The main function. Entry point of the program.
     * @param args the command line arguments
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        Path level = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i].toLowerCase(Locale.ROOT)) {
                case "-l":
                    i++;
                    if (i < args.length) {
                        level = Paths.get(args[i]);
                    }
                    break;
                case "-p":
                    createPatches = true;
                    break;
                default:
                    break;
            }
        }
        
        /*
         * Set "Look and Feel" to system default
         */
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            /* don't care */
        }
        
        /*
         * Apple menu bar for MacOS
         */
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        
        /*
         * Check JVM version
         */
        if (!SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_1_8)) {
            JOptionPane.showMessageDialog(null, "SuperLemminiToo requires JVM 1.8 or later.", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        
        // check free memory
        long free = Runtime.getRuntime().maxMemory();
        if (free < 96 * 1024 * 1024) {
            JOptionPane.showMessageDialog(null, "You need at least 96MB of heap.", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        
        // workaround to adjust time base to 1ms under Windows
        // see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6435126
        new Thread() {
            {
                this.setDaemon(true);
                this.start();
            }
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(Long.MAX_VALUE);
                    } catch(InterruptedException ex) {
                    }
                }
            }
        };
        
        /* Create and display the form */
        thisFrame = new LemminiFrame();
        thisFrame.init();
        
        if (level != null) {
            int[] levelPosition = GameController.addExternalLevel(level, null, true);
            GameController.requestChangeLevel(levelPosition[0], levelPosition[1], levelPosition[2], false);
        }
    }
    
    /**
     * Common exit method to use in exit events.
     */
    void exit() {
        // stop the music
        Music.close();
        // store width and height
        Core.programProps.setInt("frameWidth", lemminiPanelMain.getUnmaximizedWidth());
        Core.programProps.setInt("frameHeight", lemminiPanelMain.getUnmaximizedHeight());
        // store frame pos
        Core.programProps.setInt("framePosX", unmaximizedPosX);
        Core.programProps.setInt("framePosY", unmaximizedPosY);
        // store maximized state
        Core.programProps.setBoolean("maximizedHoriz", BooleanUtils.toBoolean(getExtendedState() & MAXIMIZED_HORIZ));
        Core.programProps.setBoolean("maximizedVert", BooleanUtils.toBoolean(getExtendedState() & MAXIMIZED_VERT));
        Core.saveProgramProps();
        // close the zip files
        Core.zipFiles.stream().forEach(zipFile -> {
            try {
                zipFile.close();
            } catch (IOException ex) {
            }
        });
        RepeatingReleasedEventsFixer.remove();
        System.exit(0);
    }
    
    @Override
    public void setLocation(int x, int y) {
        super.setLocation(x, y);
        storeUnmaximizedPos();
    }
    
    private void storeUnmaximizedPos() {
        int frameState = getExtendedState();
        if (!BooleanUtils.toBoolean(frameState & MAXIMIZED_HORIZ)) {
            unmaximizedPosX = getX();
        }
        if (!BooleanUtils.toBoolean(frameState & MAXIMIZED_VERT)) {
            unmaximizedPosY = getY();
        }
    }
    
    public void setCursor(final LemmCursor.CursorType c) {
        lemminiPanelMain.setCursor(c);
    }
    
    void setRestartEnabled(boolean restartEnabled) {
        jMenuItemRestartLevel.setEnabled(restartEnabled);
    }
    
    public static LemminiFrame getFrame() {
        return thisFrame;
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuBar jMenuBarMain;
    private javax.swing.JMenu jMenuFile;
    private javax.swing.JMenuItem jMenuItemEnterLevelCode;
    private javax.swing.JMenuItem jMenuItemExit;
    private javax.swing.JMenuItem jMenuItemFileExtract;
    private javax.swing.JMenuItem jMenuItemLoadReplay;
    private javax.swing.JMenuItem jMenuItemManagePlayers;
    private javax.swing.JMenuItem jMenuItemOptions;
    private javax.swing.JMenuItem jMenuItemAbout;
    private javax.swing.JMenuItem jMenuItemPlayLevel;
    private javax.swing.JMenuItem jMenuItemRestartLevel;
    private javax.swing.JMenu jMenuLevel;
    private javax.swing.JMenu jMenuOptions;
    private javax.swing.JMenu jMenuPlayers;
    private lemmini.LemminiPanel lemminiPanelMain;
    // End of variables declaration//GEN-END:variables
}
