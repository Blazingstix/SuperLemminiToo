package lemmini;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.*;
import lemmini.game.*;
import lemmini.gameutil.Fader;
import lemmini.graphics.Image;
import lemmini.gui.GainDialog;
import lemmini.gui.LevelCodeDialog;
import lemmini.gui.PlayerDialog;
import lemmini.sound.Music;
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
 * Lemmini - a game engine for Lemmings.<br>
 * This is the main window including input handling. The game logic is located in
 * {@link GameController}, some core components are in {@link Core}.<br>
 * <br>
 * Note: this was developed for JRE1.4 and only ported to JRE1.5 after it was finished.
 * Also the design evolved during two years of development and thus isn't nearly as clean
 * as it should be. During the porting to 1.5, I cleaned up some things here and there,
 * but didn't want to redesign the whole thing from scratch.
 *
 * @author Volker Oth
 */
public class Lemmini extends JFrame implements KeyListener, ComponentListener {
    
    /** height of menu and icon bar in pixels */
    private static final int WIN_OFS = 120;
    private static final boolean ALLOW_LEVEL_PATCHING = false;
    public static final String REVISION = "0.90a";
    
    private static final long serialVersionUID = 0x01;
    
    /** flag: started as Webstart application */
    private static boolean isWebstartApp = true;
    private static boolean createPatches = false;

    /** self reference */
    static Lemmini thisFrame;
    
    /** path for loading single level files */
    private String lvlPath;
    /** HashMap to store menu items for ratings */
    private Map<String, List<LvlMenuItem>> ratingMenus;
    /** panel for the game graphics */
    private static GraphicsPane gp;

    // Swing stuff

    private JMenuBar jMenuBar = null;
    private JMenu jMenuLevel = null;
    private JMenuItem jMenuItemRestart = null;
    private JMenuItem jMenuItemLevelCode = null;
    private JMenuItem jMenuSelect = null;
    private JMenu jMenuFile = null;
    private JMenu jMenuPlayer = null;
    private JMenu jMenuSelectPlayer = null;
    private JMenu jMenuSound = null;
    private JMenu jMenuSFX = null;
    private JMenuItem jMenuItemVolume = null;
    private JMenu jMenuOptions = null;
    private JMenuItem jMenuItemCursor = null;
    private JMenuItem jMenuItemSwap = null;
    private JMenuItem jMenuItemFaster = null;
    private JMenuItem jMenuItemExit = null;
    private JMenuItem jMenuItemManagePlayer = null;
    private JMenuItem jMenuItemLoad = null;
    private JMenuItem jMenuItemReplay = null;
    private JCheckBoxMenuItem jMenuItemMusic = null;
    private JCheckBoxMenuItem jMenuItemSound = null;
    private ButtonGroup playerGroup = null;


    /**
     * Initialize the main frame.
     */
    void init() {
        addKeyListener(this);
        addComponentListener(this);
        try {
            boolean successful = Core.init(this, isWebstartApp, createPatches);      // initialize Core object
            if (!successful) {
                System.exit(0);
            }
            GameController.init();
            GameController.setLevelMenuUpdateListener(new LevelMenuUpdateListener());
        } catch (ResourceException ex) {
            Core.resourceError(ex.getMessage());
        } catch (LemmException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        } catch (Exception ex) {
            ToolBox.showException(ex);
            System.exit(1);
        }
        // read frame props
        int width, height, posX, posY;
        width = Core.getProgramProps().getInt("frameWidth", 800);
        if (width < 800) {
            width = 800;
        }
        height = Level.DEFAULT_HEIGHT + WIN_OFS + 60; //Core.getProgramProps().get("frameHeight", Level.height + winOfs + 60);
        this.setSize(width, height);
        this.setMinimumSize(new Dimension(width, height));
        this.setResizable(false); // at least for the moment: forbid resize
        Point p = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
        p.x -= this.getWidth() / 2;
        p.y -= this.getHeight() / 2;
        posX = Core.getProgramProps().getInt("framePosX", p.x > 0 ? p.x : 0);
        posY = Core.getProgramProps().getInt("framePosY", p.y > 0 ? p.y : 0);
        this.setLocation(posX, posY);
        //this.validate(); // force redraw
        //this.setTitle("SuperLemmini");

        ClassLoader loader = Lemmini.class.getClassLoader();
        java.awt.Image img = Toolkit.getDefaultToolkit().getImage(loader.getResource("icon_32.png"));
        setIconImage(img);

        // set component pane
        gp = new GraphicsPane();
        gp.setDoubleBuffered(false);
        this.setContentPane(gp);

        this.validate(); // force redraw
        this.setTitle("SuperLemmini");


        // create Menu
        jMenuItemExit = new JMenuItem("Exit");
        jMenuItemExit.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                exit();
            }
        });

        jMenuFile = new JMenu("File");
        jMenuFile.add(jMenuItemExit);

        // Player Menu
        jMenuItemManagePlayer = new JMenuItem("Manage Players...");
        jMenuItemManagePlayer.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                Core.player.store(); // save player in case it is changed
                PlayerDialog d = new PlayerDialog((JFrame)Core.getCmp(), true);
                d.setVisible(true);
                // blocked until dialog returns
                List<String> players = d.getPlayers();
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
                            Path f = Paths.get(Core.getResourcePath(), "players", p + ".ini");
                            try {
                                Files.delete(f);
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
                        thisFrame.setTitle("SuperLemmini");
                    }
                    jMenuItemRestart.setEnabled(false);
                    Core.player = new Player(player);

                    // rebuild players menu
                    playerGroup = new ButtonGroup();
                    jMenuSelectPlayer.removeAll();
                    for (int idx = 0; idx < Core.getPlayerNum(); idx++) {
                        JRadioButtonMenuItem item = addPlayerItem(Core.getPlayer(idx));
                        if (Core.player.getName().equals(Core.getPlayer(idx))) {
                            item.setSelected(true);
                        }
                    }
                    updateLevelMenus();
                }
            }
        });


        jMenuSelectPlayer = new JMenu("Select Player");
        playerGroup = new ButtonGroup();
        for (int idx = 0; idx < Core.getPlayerNum(); idx++) {
            JRadioButtonMenuItem item = addPlayerItem(Core.getPlayer(idx));
            if (Core.player.getName().equals(Core.getPlayer(idx))) {
                item.setSelected(true);
            }
        }
        jMenuPlayer = new JMenu("Player");
        jMenuPlayer.add(jMenuItemManagePlayer);
        jMenuPlayer.add(jMenuSelectPlayer);


        // load level packs and create Level menu
        java.awt.event.ActionListener lvlListener = new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                LvlMenuItem item = (LvlMenuItem)e.getSource();
                GameController.requestChangeLevel(item.levelPack, item.rating, item.level, false);
                jMenuItemRestart.setEnabled(true);
            }
        };
        ratingMenus = new HashMap<>(); // store menus to access them later
        jMenuSelect = new JMenu("Select Level");
        for (int lp = 1; lp < GameController.getLevelPackCount(); lp++) { // skip dummy level pack
            LevelPack lPack = GameController.getLevelPack(lp);
            JMenu jMenuPack = new JMenu(lPack.getName());
            String[] ratings = lPack.getRatings();
            for (int i = 0; i < ratings.length; i++) {
                // get activated levels for this group
                BigInteger bf = Core.player.getBitField(lPack, ratings[i]);
                String[] names = lPack.getLevels(i);
                JMenu jMenuRating = new JMenu(ratings[i]);
                // store menus to access them later
                List<LvlMenuItem> menuItems = new ArrayList<>(64);
                for (int n = 0; n < names.length; n++) {
                    LvlMenuItem jMenuLvl = new LvlMenuItem(names[n], lp, i, n);
                    jMenuLvl.addActionListener(lvlListener);
                    if (Core.player.isAvailable(bf, n)) {
                        jMenuLvl.setEnabled(true);
                    } else {
                        jMenuLvl.setEnabled(false);
                    }
                    jMenuRating.add(jMenuLvl);
                    menuItems.add(jMenuLvl);
                }
                jMenuPack.add(jMenuRating);
                // store menus to access them later
                ratingMenus.put(LevelPack.getID(lPack.getName(), ratings[i]), menuItems);
            }
            jMenuSelect.add(jMenuPack);
        }

        jMenuItemRestart = new JMenuItem();
        jMenuItemRestart.setText("Restart Level");
        jMenuItemRestart.setEnabled(false);
        jMenuItemRestart.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (!GameController.getLevel().isReady()) {
                    GameController.requestChangeLevel(GameController.getCurLevelPackIdx(), GameController.getCurRating(), GameController.getCurLevelNumber(), false);
                } else {
                    GameController.requestRestartLevel(false);
                }
            }
        });


        jMenuItemLoad = new JMenuItem();
        jMenuItemLoad.setText("Load Level...");
        jMenuItemLoad.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                String p = ToolBox.getFileName(thisFrame, lvlPath, Core.LEVEL_EXTENSIONS, true);
                loadLevel(p);
            }
        });

        jMenuItemReplay = new JMenuItem();
        jMenuItemReplay.setText("Load Replay...");
        jMenuItemReplay.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                String replayPath = ToolBox.getFileName(thisFrame, Core.getResourcePath(),Core.REPLAY_EXTENSIONS, true);
                if (replayPath != null) {
                    try {
                        if (ToolBox.getExtension(replayPath).equalsIgnoreCase("rpl")) {
                            ReplayLevelInfo rli = GameController.loadReplay(replayPath);
                            if (rli != null) {
                                int lpn = -1;
                                for (int i = 0; i < GameController.getLevelPackCount(); i++) {
                                    String packName = GameController.getLevelPack(i).getName();
                                    if (!Normalizer.isNormalized(packName, Normalizer.Form.NFKC)) {
                                        packName = Normalizer.normalize(packName, Normalizer.Form.NFKC);
                                    }
                                    if (packName.equals(rli.getLevelPack())) {
                                        lpn = i;
                                    }
                                }
                                if (lpn > -1) {
                                    GameController.requestChangeLevel(lpn, rli.getRating(), rli.getLvlNumber(), true);
                                    jMenuItemRestart.setEnabled(true);
                                    return; // success
                                }
                            }
                        }
                        // else: no success
                        JOptionPane.showMessageDialog(Core.getCmp(), "Wrong format!", "Loading replay failed.", JOptionPane.ERROR_MESSAGE);
                    } catch (Exception ex) {
                        ToolBox.showException(ex);
                    }
                }
            }
        });


        jMenuItemLevelCode = new JMenuItem("Enter Level Code...");
        jMenuItemLevelCode.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                LevelCodeDialog lcd = new LevelCodeDialog((JFrame) Core.getCmp(), true);
                lcd.setVisible(true);
                String levelCode = lcd.getCode();
                int lvlPack = lcd.getLevelPack();
                if (levelCode != null && !levelCode.isEmpty() && lvlPack > 0) {

                    levelCode = levelCode.trim();
                    // cheat mode
                    if (levelCode.equalsIgnoreCase("0xdeadbeef")) {
                        JOptionPane.showMessageDialog(Core.getCmp(), "All levels and debug mode enabled.", "Cheater!", JOptionPane.INFORMATION_MESSAGE);
                        Core.player.enableCheatMode();
                        updateLevelMenus();
                        return;
                    }

                    // real level code -> get absolute level
                    levelCode = levelCode.toUpperCase();
                    LevelPack lpack = GameController.getLevelPack(lvlPack);
                    int[] codeInfo = LevelCode.getLevel(lpack.getCodeSeed(), levelCode, lpack.getCodeOffset());
                    if (codeInfo != null) {
                        if (Core.player.isCheat()) {
                            JOptionPane.showMessageDialog(Core.getCmp(),
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
                            jMenuItemRestart.setEnabled(true);
                            updateLevelMenus();
                            return;
                        }
                    }
                }
                // not found
                if (lvlPack != -1) {
                    JOptionPane.showMessageDialog(Core.getCmp(), "Invalid Level Code.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        jMenuLevel = new JMenu("Level");
        jMenuLevel.add(jMenuSelect);
        jMenuLevel.add(jMenuItemRestart);
        jMenuLevel.add(jMenuItemLoad);
        jMenuLevel.add(jMenuItemReplay);
        jMenuLevel.add(jMenuItemLevelCode);

        jMenuItemMusic = new JCheckBoxMenuItem("Music", false);
        jMenuItemMusic.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                boolean selected = jMenuItemMusic.isSelected();
                //jMenuItemMusic.setSelected(selected);
                if (selected) {
                    GameController.setMusicOn(true);
                } else {
                    GameController.setMusicOn(false);
                }
                Core.getProgramProps().setBoolean("music", GameController.isMusicOn());
                if (GameController.getLevel() != null) {
                    if (GameController.isMusicOn() && GameController.getGameState() == GameController.State.LEVEL) {
                        Music.play();
                    } else {
                        Music.stop();
                    }
                }
            }
        });
        jMenuItemMusic.setSelected(GameController.isMusicOn());

        jMenuItemSound = new JCheckBoxMenuItem("Sound", false);
        jMenuItemSound.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                boolean selected = jMenuItemSound.isSelected();
                if (selected) {
                    GameController.setSoundOn(true);
                } else {
                    GameController.setSoundOn(false);
                }
                Core.getProgramProps().setBoolean("sound", GameController.isSoundOn());
            }
        });
        jMenuItemSound.setSelected(GameController.isSoundOn());

        jMenuSFX = new JMenu("SFX Mixer");
        String[] mixerNames = GameController.sound.getMixers();
        ButtonGroup mixerGroup = new ButtonGroup();
        String lastMixerName = Core.getProgramProps().get("mixerName", "Java Sound Audio Engine");

        // special handling of mixer from INI that doesn't exist (any more)
        boolean found = false;
        for (String mixerName : mixerNames) {
            if (mixerName.equals(lastMixerName)) {
                found = true;
                break;
            }
        }
        if (!found) {
            lastMixerName = "Java Sound Audio Engine";
        }

        for (int i = 0; i < mixerNames.length; i++) {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem();
            item.setText(mixerNames[i]);
            item.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    String[] mixerNames = GameController.sound.getMixers();
                    String mixerName = e.getActionCommand();
                    for (int i = 0; i < mixerNames.length; i++) {
                        if (mixerNames[i].equals(mixerName)) {
                            GameController.sound.setMixer(i);
                            Core.getProgramProps().set("mixerName", mixerName);
                            break;
                        }
                    }
                }
            });
            if (mixerNames[i].equals(lastMixerName)) { // default setting
                item.setState(true);
                GameController.sound.setMixer(i);
            }

            jMenuSFX.add(item);
            mixerGroup.add(item);
        }

        jMenuItemVolume = new JMenuItem("Volume Control...");
        jMenuItemVolume.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                GainDialog v = new GainDialog((JFrame)Core.getCmp(), true);
                v.setVisible(true);
            }
        });


        jMenuSound = new JMenu();
        jMenuSound.setText("Sound");
        jMenuSound.add(jMenuItemVolume);
        jMenuSound.add(jMenuItemMusic);
        jMenuSound.add(jMenuItemSound);
        jMenuSound.add(jMenuSFX);

        jMenuItemCursor = new JCheckBoxMenuItem("Advanced select", false);
        jMenuItemCursor.addActionListener(new java.awt.event.ActionListener() {
            /* (non-Javadoc)
             * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
             */
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                boolean selected = jMenuItemCursor.isSelected();
                if (selected) {
                    GameController.setAdvancedSelect(true);
                } else {
                    GameController.setAdvancedSelect(false);
                    gp.setCursor(LemmCursor.CursorType.NORMAL);
                }
                Core.getProgramProps().setBoolean("advancedSelect", GameController.isAdvancedSelect());
            }
        });
        jMenuItemCursor.setSelected(GameController.isAdvancedSelect());
        
        jMenuItemSwap = new JCheckBoxMenuItem("Swap middle/right mouse buttons", false);
        jMenuItemSwap.addActionListener(new java.awt.event.ActionListener() {
            /* (non-Javadoc)
             * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
             */
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                GameController.setSwapButtons(jMenuItemSwap.isSelected());
                Core.getProgramProps().setBoolean("swapButtons", GameController.isAdvancedSelect());
            }
        });
        jMenuItemSwap.setSelected(GameController.doSwapButtons());
        
        jMenuItemFaster = new JCheckBoxMenuItem("Increase fast-forward speed", false);
        jMenuItemFaster.addActionListener(new java.awt.event.ActionListener() {
            /* (non-Javadoc)
             * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
             */
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                GameController.setFasterFastForward(jMenuItemFaster.isSelected());
                Core.getProgramProps().setBoolean("fasterFastForward", GameController.isFasterFastForward());
            }
        });
        jMenuItemFaster.setSelected(GameController.isFasterFastForward());

        jMenuOptions = new JMenu();
        jMenuOptions.setText("Options");
        jMenuOptions.add(jMenuItemCursor);
        jMenuOptions.add(jMenuItemSwap);
        jMenuOptions.add(jMenuItemFaster);

        jMenuBar = new JMenuBar();
        jMenuBar.add(jMenuFile);
        jMenuBar.add(jMenuPlayer);
        jMenuBar.add(jMenuLevel);
        jMenuBar.add(jMenuSound);
        jMenuBar.add(jMenuOptions);
        this.setJMenuBar(jMenuBar);

        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                exit();
            }

            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                exit();
            }
        });
        this.setVisible(true);
        gp.init();
        Thread t = new Thread(gp);

        lvlPath = ".";

        t.start();
    }
    
    private void loadLevel(String name) {
        if (name != null) {
            try {
                boolean changeLvlPath = true;
                if (ToolBox.getExtension(name).equalsIgnoreCase("lvl")) {
                    lemmini.extract.ExtractLevel.convertLevel(name, Core.getResourcePath() + "/temp.ini", false, false);
                    lvlPath = name;
                    changeLvlPath = false;
                    name = Core.getResourcePath() + "/temp.ini";
                }
                if (ToolBox.getExtension(name).equalsIgnoreCase("ini")) {
                    String id = ToolBox.getFileIDString(name, 5);
                    if (id.equalsIgnoreCase("# LVL")) {
                        // this is a hack - maybe find a better way
                        GameController.getLevelPack(0).getInfo(0, 0).setFileName(name);
                        GameController.getLevelPack(0).getInfo(0, 0).setMusic(Music.getRandomTrack());
                        GameController.requestChangeLevel(0, 0, 0, false);
                        jMenuItemRestart.setEnabled(true);
                        if (changeLvlPath) {
                            lvlPath = name;
                        }
                        return;
                    }
                }
                JOptionPane.showMessageDialog(Core.getCmp(), "Wrong format!", "Unable to load level.", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                ToolBox.showException(ex);
            }
        }
    }

    /**
     * Add a menu item for a player.
     * @param name player name
     * @return JCheckBoxMenuItem
     */
    private JRadioButtonMenuItem addPlayerItem(final String name) {
        JRadioButtonMenuItem item = new JRadioButtonMenuItem(name);
        item.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                Core.player.store(); // save player in case it is changed
                JMenuItem item = (JMenuItem) e.getSource();
                String player = item.getText();
                Player p = new Player(player);
                if (!Core.player.getName().equals(player)
                        && GameController.getGameState() != GameController.State.INTRO) {
                    jMenuItemRestart.setEnabled(false);
                    if (GameController.getGameState() == GameController.State.LEVEL) {
                        GameController.setGameState(GameController.State.LEVEL_END);
                    }
                    GameController.setTransition(GameController.TransitionState.TO_INTRO);
                    Fader.setState(Fader.State.OUT);
                    thisFrame.setTitle("SuperLemmini");
                }
                Core.player = p; // default player
                item.setSelected(true);
                updateLevelMenus();
            }
        });
        playerGroup.add(item);
        jMenuSelectPlayer.add(item);
        return item;
    }

    /**
     * Convert String to int.
     * @param s String with decimal integer value
     * @return integer value (0 if no valid number)
     */
    private static int getInt(final String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    /**
     * The main function. Entry point of the program.
     * @param args
     */
    public static void main(final String[] args) {
        String level = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i].toLowerCase()) {
                case "-l":
                    i++;
                    if (i < args.length) {
                        level = args[i];
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
        } catch (ClassNotFoundException
                | IllegalAccessException
                | InstantiationException
                | UnsupportedLookAndFeelException
                e) { 
            /* don't care */
        }
        /*
         * Apple menu bar for MacOS
         */
        System.setProperty("com.apple.macos.useScreenMenuBar", "true");

        /*
         * Check JVM version
         */
        String jreStr = System.getProperty("java.version");
        String[] vs = jreStr.split("[._]");
        if (vs.length >= 3) {
            if (!((getInt(vs[0]) == 1 && getInt(vs[1]) >= 7) || getInt(vs[0]) >= 2)) {
                JOptionPane.showMessageDialog(null, "SuperLemmini requires JVM 1.7 or later.", "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        }

        // check free memory
        long free = Runtime.getRuntime().maxMemory();
        if (free < 96 * 1024 * 1024) {
            JOptionPane.showMessageDialog(null, "You need at least 96MB of heap.", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // detect webstart
        //try {
        //    ServiceManager.lookup("javax.jnlp.BasicService");
        //} catch (UnavailableServiceException ex) {
        //    isWebstartApp = false;
        //}

        // workaround to adjust time base to 1ms under XP
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
                        Thread.sleep(Integer.MAX_VALUE);
                    } catch(InterruptedException ex) {
                    }
                }
            }
        };

        Toolkit.getDefaultToolkit().setDynamicLayout(true);
        thisFrame = new Lemmini();
        thisFrame.init();
        if (level != null) {
            thisFrame.loadLevel(level);
        }
    }

    /**
     * Update the level menus according to the progress of the current player.
     */
    private void updateLevelMenus() {
        // update level menus
        for (int lp = 1; lp < GameController.getLevelPackCount(); lp++) { // skip dummy level pack
            LevelPack lPack = GameController.getLevelPack(lp);
            String[] ratings = lPack.getRatings();
            for (String rating : ratings) {
                // get activated levels for this group
                BigInteger bf = Core.player.getBitField(lPack, rating);
                updateLevelMenu(lPack.getName(), rating, bf);
            }
        }
    }

    /**
     * Update the level menus according to the given progress information.
     * @param pack name of level pack
     * @param rating name of rating
     * @param bf bitmap containing availability flags for each level
     */
    private void updateLevelMenu(final String pack, final String rating, final BigInteger bf) {
        List<LvlMenuItem> menuItems = ratingMenus.get(LevelPack.getID(pack, rating));
        for (int k = 0; k < menuItems.size(); k++) {
            // select level, e.g. "All fall down"
            JMenuItem level = menuItems.get(k);
            if (k == 0 || Core.player.isAvailable(bf, k)) {
                level.setEnabled(true);
            } else {
                level.setEnabled(false);
            }
        }

    }


    /**
     * Development function: patch current level x offset in the level configuration file.
     * Works only in cheat mode.
     * @param lvlPath path of level configuration files
     */
    private void patchLevel(final String lvlPath) {
        if (!ALLOW_LEVEL_PATCHING) {
            return;
        }
        List<String> lines = new ArrayList<>(512);
        try (BufferedReader r = new BufferedReader(new FileReader(lvlPath))) {
            String l;
            while ((l = r.readLine()) != null) {
                lines.add(l);
            }
        } catch (FileNotFoundException ex) {
        } catch (IOException ex) {
        }
        try (FileWriter sw = new FileWriter(lvlPath)) {
            for (String s : lines) {
                if (s.startsWith("xPos =")) {
                    sw.write("xPos = " + GameController.getXPos() + "\r\n");
                } else {
                    sw.write(s + "\r\n");
                }
            }
        } catch (FileNotFoundException ex) {
        } catch (IOException ex) {
        }
    }

    /* (non-Javadoc)
     * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
     */
    @Override
    public void keyPressed(final KeyEvent keyevent) {
        int code = keyevent.getKeyCode();
        switch (GameController.getGameState()) {
            case LEVEL:
                switch (code) {
                    case KeyEvent.VK_1:
                    case KeyEvent.VK_F3:
                        GameController.handleIconButton(Icons.Type.CLIMB);
                        break;
                    case KeyEvent.VK_2:
                    case KeyEvent.VK_F4:
                        GameController.handleIconButton(Icons.Type.FLOAT);
                        break;
                    case KeyEvent.VK_3:
                    case KeyEvent.VK_F5:
                        GameController.handleIconButton(Icons.Type.BOMB);
                        break;
                    case KeyEvent.VK_4:
                    case KeyEvent.VK_F6:
                        GameController.handleIconButton(Icons.Type.BLOCK);
                        break;
                    case KeyEvent.VK_5:
                    case KeyEvent.VK_F7:
                        GameController.handleIconButton(Icons.Type.BUILD);
                        break;
                    case KeyEvent.VK_6:
                    case KeyEvent.VK_F8:
                        GameController.handleIconButton(Icons.Type.BASH);
                        break;
                    case KeyEvent.VK_7:
                    case KeyEvent.VK_F9:
                        GameController.handleIconButton(Icons.Type.MINE);
                        break;
                    case KeyEvent.VK_8:
                    case KeyEvent.VK_F10:
                        GameController.handleIconButton(Icons.Type.DIG);
                        break;
                    case KeyEvent.VK_D:
                        if (GameController.isCheat()) {
                            gp.setDebugDraw(!gp.getDebugDraw());
                        }
                        break;
                    case KeyEvent.VK_W:
                        if (GameController.isCheat()) {
                            GameController.setNumLeft(GameController.getNumLemmingsMax());
                            GameController.endLevel();
                        }
                        break;
                    case KeyEvent.VK_L: // print current level on the console
                        if (GameController.isCheat()) {
                            System.out.println(GameController.getLevelPack(GameController.getCurLevelPackIdx()).getInfo(GameController.getCurRating(), GameController.getCurLevelNumber()).getFileName());
                        }
                        break;
                    case KeyEvent.VK_S: // superlemming on/off
                        if (GameController.isCheat()) {
                            GameController.setSuperLemming(!GameController.isSuperLemming());
                        } else {
                            try {
                                File file = new File(Core.getResourcePath(), "level.png");
                                Image tmp = GameController.getLevel().createMinimap(null, GameController.getFgImage(), 1, 1, false, true);
                                ImageIO.write(ToolBox.getBufferedImage(tmp), "png", file);
                            } catch (IOException ex) {
                            }
                        }
                        break;
                    case KeyEvent.VK_C:
                        if (Core.player.isCheat()) {
                            GameController.setCheat(!GameController.isCheat());
                            if (GameController.isCheat()) {
                                GameController.setWasCheated(true);
                            }
                        } else {
                            GameController.setCheat(false);
                        }
                        break;
                    case KeyEvent.VK_F11:
                    case KeyEvent.VK_P:
                        GameController.setPaused(!GameController.isPaused());
                        GameController.pressIcon(Icons.Type.PAUSE);
                        break;
                    case KeyEvent.VK_F:
                    case KeyEvent.VK_ENTER:
                        GameController.setFastForward(!GameController.isFastForward());
                        GameController.pressIcon(Icons.Type.FFWD);
                        break;
                    case KeyEvent.VK_X:
                        if (GameController.isCheat()) {
                            patchLevel(GameController.getLevelPack(GameController.getCurLevelPackIdx()).getInfo(GameController.getCurRating(), GameController.getCurLevelNumber()).getFileName());
                        }
                        break;
                    case KeyEvent.VK_T:
                        if (GameController.isCheat()) {
                            GameController.setTimed(!GameController.isTimed());
                        }
                        break;
                    case KeyEvent.VK_RIGHT /*39*/: {
                        if (GameController.isAdvancedSelect()) {
                            if (LemmCursor.getType() == LemmCursor.CursorType.WALKER
                                    || LemmCursor.getType() == LemmCursor.CursorType.WALKER_LEFT
                                    || LemmCursor.getType() == LemmCursor.CursorType.WALKER_RIGHT) {
                                gp.setCursor(LemmCursor.CursorType.WALKER_RIGHT);
                            } else {
                                gp.setCursor(LemmCursor.CursorType.RIGHT);
                            }
                        } else {
                            int xOfsTemp = GameController.getXPos() + ((gp.isShiftPressed()) ? AbstractGameEngine.X_STEP_FAST : AbstractGameEngine.X_STEP);
                            GameController.setXPos(xOfsTemp);
                        }
                        break; }
                    case KeyEvent.VK_LEFT /*37*/: {
                        if (GameController.isAdvancedSelect()) {
                            if (LemmCursor.getType() == LemmCursor.CursorType.WALKER
                                    || LemmCursor.getType() == LemmCursor.CursorType.WALKER_LEFT
                                    || LemmCursor.getType() == LemmCursor.CursorType.WALKER_RIGHT) {
                                gp.setCursor(LemmCursor.CursorType.WALKER_LEFT);
                            } else {
                                gp.setCursor(LemmCursor.CursorType.LEFT);
                            }
                        } else {
                            int xOfsTemp = GameController.getXPos() - ((gp.isShiftPressed()) ? AbstractGameEngine.X_STEP_FAST : AbstractGameEngine.X_STEP);
                            GameController.setXPos(xOfsTemp);
                        }
                        break; }
                    case KeyEvent.VK_UP: {
                        if (LemmCursor.getType() == LemmCursor.CursorType.LEFT) {
                            gp.setCursor(LemmCursor.CursorType.WALKER_LEFT);
                        } else if (LemmCursor.getType() == LemmCursor.CursorType.RIGHT) {
                            gp.setCursor(LemmCursor.CursorType.WALKER_RIGHT);
                        } else if (LemmCursor.getType() == LemmCursor.CursorType.NORMAL) {
                            gp.setCursor(LemmCursor.CursorType.WALKER);
                        }
                        break;}
                    case KeyEvent.VK_SHIFT:
                        gp.setShiftPressed(true);
                        break;
                    case KeyEvent.VK_SPACE:
                        if (GameController.isCheat()) {
                            Lemming l = new Lemming(gp.getCursorX(), gp.getCursorY(), Lemming.Direction.RIGHT);
                            synchronized (GameController.getLemmings()) {
                                GameController.getLemmings().add(l);
                            }
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
                        GameController.handleIconButton(Icons.Type.NUKE);
                        break;
                    case KeyEvent.VK_ESCAPE:
                        if (GameController.getGameState() == GameController.State.LEVEL) {
                            GameController.endLevel();
                        }
                        break;
                    default:
                        break;
                }
                keyevent.consume();
                break;
            case BRIEFING:
            case DEBRIEFING:
            case LEVEL_END:
                switch (code) {
                    case KeyEvent.VK_S:
                        try {
                            File file = new File(Core.getResourcePath(), "level.png");
                            Image tmp = GameController.getLevel().createMinimap(null, GameController.getFgImage(), 1, 1, false, true);
                            ImageIO.write(ToolBox.getBufferedImage(tmp), "png", file);
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
        //System.out.println(keyevent.getKeyCode());
    }

    /* (non-Javadoc)
     * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
     */
    @Override
    public void keyReleased(final KeyEvent keyevent) {
        int code = keyevent.getKeyCode();
        if (GameController.getGameState() == GameController.State.LEVEL) {
            switch (code) {
                case KeyEvent.VK_SHIFT:
                    gp.setShiftPressed(false);
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
                    GameController.releaseIcon(Icons.Type.NUKE);
                    break;
                case KeyEvent.VK_LEFT:
                    if (LemmCursor.getType() == LemmCursor.CursorType.LEFT) {
                        gp.setCursor(LemmCursor.CursorType.NORMAL);
                    } else if (LemmCursor.getType() == LemmCursor.CursorType.WALKER_LEFT) {
                        gp.setCursor(LemmCursor.CursorType.WALKER);
                    }
                    break;
                case KeyEvent.VK_RIGHT:
                    if (LemmCursor.getType() == LemmCursor.CursorType.RIGHT) {
                        gp.setCursor(LemmCursor.CursorType.NORMAL);
                    } else if (LemmCursor.getType() == LemmCursor.CursorType.WALKER_RIGHT) {
                        gp.setCursor(LemmCursor.CursorType.WALKER);
                    }
                    break;
                case KeyEvent.VK_UP:
                    if (LemmCursor.getType() == LemmCursor.CursorType.WALKER) {
                        gp.setCursor(LemmCursor.CursorType.NORMAL);
                    } else if (LemmCursor.getType() == LemmCursor.CursorType.WALKER_LEFT) {
                        gp.setCursor(LemmCursor.CursorType.LEFT);
                    } else if (LemmCursor.getType() == LemmCursor.CursorType.WALKER_RIGHT) {
                        gp.setCursor(LemmCursor.CursorType.RIGHT);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /* (non-Javadoc)
     * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
     */
    @Override
    public void keyTyped(final KeyEvent keyevent) {
    }

    @Override
    public void componentResized(ComponentEvent e) {
    }

    @Override
    public void componentMoved(ComponentEvent e) {
    }

    @Override
    public void componentShown(ComponentEvent e) {
    }

    @Override
    public void componentHidden(ComponentEvent e) {
    }
    
    public static void setCursor(final LemmCursor.CursorType c) {
        gp.setCursor(c);
    }
    
    public static void setShiftPressed(boolean shiftPressed) {
        gp.setShiftPressed(shiftPressed);
    }
    
    public static int getPaneWidth() {
        return gp.getWidth();
    }
    
    public static int getPaneHeight() {
        return gp.getHeight();
    }
    
    public static boolean getDebugDraw() {
        return gp.getDebugDraw();
    }

    /**
     * Common exit method to use in exit events.
     */
    private void exit() {
        // store width and height
        Dimension d = this.getSize();
        Core.getProgramProps().setInt("frameWidth", d.width);
        Core.getProgramProps().setInt("frameHeight", d.height);
        // store frame pos
        Point p = this.getLocation();
        Core.getProgramProps().setInt("framePosX", p.x);
        Core.getProgramProps().setInt("framePosY", p.y);
        //
        Core.saveProgramProps();
        System.exit(0);
    }


    /**
     * Listener to inform the GUI of the player's progress.
     * @author Volker Oth
     */
    class LevelMenuUpdateListener implements UpdateListener {
        /* (non-Javadoc)
         * @see Game.UpdateListener#update()
         */
        @Override
        public void update() {
            if (GameController.getCurLevelPackIdx() != 0) { // 0 is the dummy pack
                LevelPack lvlPack = GameController.getLevelPack(GameController.getCurLevelPackIdx());
                String pack = lvlPack.getName();
                String rating = lvlPack.getRatings()[GameController.getCurRating()];
                // get next level
                int num = GameController.getCurLevelNumber() + 1;
                if (num >= lvlPack.getLevels(GameController.getCurRating()).length) {
                    num = GameController.getCurLevelNumber();
                }
                // set next level as available
                BigInteger bf = Core.player.setAvailable(pack, rating, num);
                // update the menu
                updateLevelMenu(pack, rating, bf);
            }
        }
    }


    /**
     * Specialized menu item for level selection menus.
     * @author Volker Oth
     */
    class LvlMenuItem extends JMenuItem {
        private static final long serialVersionUID = 0x01;

        /** index of level pack */
        int levelPack;
        /** index of rating */
        int rating;
        /** level number */
        int level;

        /**
         * Constructor
         * @param text level name
         * @param pack index level pack
         * @param r index of rating
         * @param lvl level number
         */
        LvlMenuItem(final String text, final int pack, final int r, final int lvl) {
            super(text);
            levelPack = pack;
            rating = r;
            level = lvl;
        }
    }
}
/**
 * A graphics panel in which the actual game contents is displayed.
 * @author Volker Oth
 */
class GraphicsPane extends JPanel implements Runnable, MouseListener, MouseMotionListener {

    private static final long serialVersionUID = 0x01;

    private final GameEngine gameEngine = new GameEngine();
    /** flag: Shift key is pressed */
    private boolean shiftPressed;
    /** flag: debug draw is active */
    private boolean draw;

    /**
     * Constructor.
     */
    public GraphicsPane() {
        super();
        this.requestFocus();
        this.setCursor(LemmCursor.getCursor());
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
    }
    
    void init() {
        gameEngine.init();
        shiftPressed = false;
    }

    /**
     * Set cursor type.
     * @param c Cursor
     */
    public void setCursor(final LemmCursor.CursorType c) {
        LemmCursor.setType(c);
        this.setCursor(LemmCursor.getCursor());
    }

    /* (non-Javadoc)
     * @see javax.swing.JComponent#paint(java.awt.Graphics)
     */
    @Override
    public void paint(final Graphics g) {
        //super.paint(iconGfx);
        synchronized (gameEngine.getPaintSemaphore()) {
            Image activeBuffer = gameEngine.getActiveBuffer();
            if (activeBuffer != null) {
                g.drawImage(ToolBox.getBufferedImage(activeBuffer), 0, 0, null);
            }
        }
    }

    /* (non-Javadoc)
     * @see javax.swing.JComponent#update(java.awt.Graphics)
     */
    @Override
    public void update(final Graphics g) {
        //super.update(iconGfx);
        paint(g);
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        gameEngine.run();
    }

    /* (non-Javadoc)
     * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
     */
    @Override
    public void mouseReleased(final MouseEvent mouseEvent) {
        gameEngine.mouseReleased(mouseEvent);
    }

    /* (non-Javadoc)
     * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
     */
    @Override
    public void mouseClicked(final MouseEvent mouseEvent) {
        //gameEngine.mouseClicked(mouseEvent);
    }

    /* (non-Javadoc)
     * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
     */
    @Override
    public void mousePressed(final MouseEvent mouseEvent) {
        gameEngine.mousePressed(mouseEvent);
    }

    /**
     * Debug routine to draw terrain pixels in stencil and foreground image.
     * @param x x position in pixels
     * @param y y position in pixels
     * @param doDraw true: draw, false: erase
     */
    private void debugDraw(final int x, final int y, final boolean doDraw) {
        if (draw && GameController.isCheat()) {
            int rgbVal = (doDraw) ? 0xffffffff : 0x0;
            int minimapVal = rgbVal;
            if (doDraw && Minimap.isTinted()) {
                minimapVal = Minimap.tintColor(minimapVal);
            }
            int xOfs = GameController.getXPos();
            Image fgImage = GameController.getFgImage();
            Image fgImageSmall = Minimap.getImage();
            Stencil stencil = GameController.getStencil();
            int scaleX = fgImage.getWidth() / fgImageSmall.getWidth();
            int scaleY = fgImage.getHeight() / fgImageSmall.getHeight();
            for (int ya = y; ya < y + 2; ya++) {
                boolean drawSmallY = (ya % scaleY) == 0;
                for (int xa = x; xa < x + 2; xa++) {
                    boolean drawSmallX = (x % scaleX) == 0;
                    if (xa + xOfs >= 0 && xa + xOfs < GameController.getWidth() && ya >= 0 && ya < Level.DEFAULT_HEIGHT) {
                        int[] objects = stencil.getIDs(xa + xOfs, ya);
                        for (int obj : objects) {
                            SpriteObject spr = GameController.getLevel().getSprObject(obj);
                            if (spr != null && spr.getVisOnTerrain()) {
                                spr.setPixelVisibility(xa + xOfs - spr.getX(), ya - spr.getY(), doDraw);
                            }
                        }
                        if (!doDraw) {
                            stencil.andMask(xa + xOfs, ya, ~Stencil.MSK_BRICK);
                        } else {
                            stencil.orMask(xa + xOfs, ya, Stencil.MSK_BRICK);
                        }
                        GameController.getFgImage().setRGB(xa + xOfs, ya, rgbVal);
                        if (drawSmallX && drawSmallY) {
                            fgImageSmall.setRGB((xa + xOfs) / scaleX, ya / scaleY, minimapVal);
                        }
                    }
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
     */
    @Override
    public void mouseEntered(final MouseEvent mouseEvent) {
        gameEngine.mouseEntered(mouseEvent);
    }

    /* (non-Javadoc)
     * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
     */
    @Override
    public void mouseExited(final MouseEvent mouseEvent) {
        gameEngine.mouseExited(mouseEvent);
    }

    /* (non-Javadoc)
     * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
     */
    @Override
    public void mouseDragged(final MouseEvent mouseEvent) {
        gameEngine.mouseDragged(mouseEvent);
    }

    /* (non-Javadoc)
     * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
     */
    @Override
    public void mouseMoved(final MouseEvent mouseEvent) {
        gameEngine.mouseMoved(mouseEvent);
    }

    /**
     * Get cursor x position in pixels.
     * @return cursor x position in pixels
     */
    int getCursorX() {
        return gameEngine.getMouseX();
    }

    /**
     * Get cursor y position in pixels.
     * @return cursor y position in pixels
     */
    int getCursorY() {
        return gameEngine.getMouseY();
    }

    /**
     * Get flag: Shift key is pressed?
     * @return true if Shift key is pressed, false otherwise
     */
    boolean isShiftPressed() {
        return shiftPressed;
    }

    /**
     * Set flag: Shift key is pressed.
     * @param p true: Shift key is pressed,false otherwise
     */
    void setShiftPressed(final boolean p) {
        shiftPressed = p;
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
    
    private class GameEngine extends AbstractGameEngine {

        @Override
        protected boolean isRunning() {
            return true;
        }

        @Override
        public int getWidth() {
            return GraphicsPane.this.getWidth();
        }

        @Override
        public int getHeight() {
            return GraphicsPane.this.getHeight();
        }

        @Override
        protected boolean isShiftPressed() {
            return shiftPressed;
        }

        @Override
        protected void repaint() {
            GraphicsPane.this.repaint();
        }

        @Override
        protected void debugDraw(int x, int y, boolean leftMousePressed) {
            GraphicsPane.this.debugDraw(x, y, leftMousePressed);
        }

        @Override
        protected Object getParent() {
            return GraphicsPane.this;
        }
    }
}
