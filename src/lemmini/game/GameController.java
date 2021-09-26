package lemmini.game;

import java.awt.Color;
import java.io.File;
import java.util.*;
import javax.swing.JOptionPane;
import lemmini.Lemmini;
import lemmini.gameutil.Fader;
import lemmini.gameutil.KeyRepeat;
import lemmini.gameutil.Sprite;
import lemmini.graphics.GraphicsContext;
import lemmini.graphics.Image;
import lemmini.sound.Music;
import lemmini.sound.Sound;
import lemmini.tools.NanosecondTimer;
import lemmini.tools.ToolBox;


/**
 * Game controller. Contains all the game logic.
 * @author Volker Oth
 */
public class GameController {
    /** game state */
    public static enum State {
        /** init state */
        INIT,
        /** display intro screen */
        INTRO,
        /** display level briefing screen */
        BRIEFING,
        /** display level */
        LEVEL,
        /** display debriefing screen */
        DEBRIEFING,
        /** fade out after level was finished */
        LEVEL_END
    }

    /** Transition states */
    public static enum TransitionState {
        /** no fading */
        NONE,
        /** restart level: fade out, fade in briefing */
        RESTART_LEVEL,
        /** replay level: fade out, fade in briefing */
        REPLAY_LEVEL,
        /** load level: fade out, fade in briefing */
        LOAD_LEVEL,
        /** load replay: fade out, fade in briefing */
        LOAD_REPLAY,
        /** level finished: fade out */
        END_LEVEL,
        /** go to intro: fade in intro */
        TO_INTRO,
        /** go to briefing: fade in briefing */
        TO_BRIEFING,
        /** go to debriefing: fade in debriefing */
        TO_DEBRIEFING,
        /** go to level: fade in level */
        TO_LEVEL
    }

    /** key repeat bitmask for icons */
    public static final int KEYREPEAT_ICON = 1;
    /** key repeat bitmask for keys */
    public static final int KEYREPEAT_KEY = 2;

    /** updates 3 frames instead of 1 in fast forward mode */
    public static final int FAST_FWD_MULTI = 3;
    /** updates 6 frames instead of 1 in faster fast forward mode */
    public static final int FASTER_FAST_FWD_MULTI = 6;
    /** updates 3 frames instead of 1 in Superlemming mode */
    public static final int SUPERLEMM_MULTI = 3;
    /** time per frame in nanoseconds - this is the timing everything else is based on */
    public static final long NANOSEC_PER_FRAME = 30_000_000;
    /** redraw animated level objects every 2nd frame (about 60ms) */
    public static final int MAX_ANIM_CTR = 2;
    
    /** color used to erase the foreground */
    public static final Color BLANK_COLOR = new Color(0, 0, 0, 0);
    
    private static final int MAX_START_SOUND_CTR = 28;
    /** open entrance after about 2 seconds */
    private static final int MAX_ENTRANCE_OPEN_CTR = 72;
    /** one second is 33.333 ticks */
    private static final int[] MAX_SECOND_CTR = {34, 33, 33};
    /** one second in superlemming mode is 100 ticks */
    private static final int[] MAX_SUPERLEMMING_SECOND_CTR = {100};
    /** maximum release rate */
    private static final int MAX_RELEASE_RATE = 99;

    /** nuke icon: maximum time between two mouse clicks for double click detection (in nanoseconds) */
    private static final long NANOSEC_NUKE_DOUBLE_CLICK = 240_000_000;
    /** +/- icons: maximum time between two mouse clicks for double click detection (in nanoseconds) */
    private static final long NANOSEC_RELEASE_DOUBLE_CLICK = 200_000_000;
    /** +/- icons: time for key repeat to kick in */
    private static final long NANOSEC_KEYREPEAT_START = 250_000_000;
    /** +/- icons: time for key repeat rate */
    private static final long NANOSEC_KEYREPEAT_REPEAT = 67_000_000;

    /** sound object */
    public static Sound sound;

    /** the foreground stencil */
    private static Stencil stencil;
    /** the foreground image */
    private static Image fgImage;
    /** the background images */
    private static List<Image> bgImages;
    /** flag: play music */
    private static boolean musicOn;
    /** flag: play sounds */
    private static boolean soundOn;
    /** flag: use advanced mouse selection methods */
    private static boolean advancedSelect;
    private static boolean swapButtons;
    private static boolean fasterFastForward;
    /** graphics object for the foreground image */
    private static GraphicsContext fgGfx;
    /** flag: fast forward mode is active */
    private static boolean fastForward;
    /** flag: Superlemming mode is active */
    private static boolean superLemming;
    private static boolean forceNormalTimerSpeed;
    /** game state */
    private static State gameState;
    /** transition (fading) state */
    private static TransitionState transitionState;
    /** skill to assign to lemming (skill icon) */
    private static Lemming.Type lemmSkill;
    /** flag: entrances is opened */
    private static boolean entranceOpened;
    /** flag: nuke was activated */
    private static boolean nuke;
    /** flag: game is paused */
    private static boolean paused;
    /** flag: cheat/debug mode is activated */
    private static boolean cheat = false;
    /** flag: cheat mode was activated during play */
    private static boolean wasCheated = false;
    /** frame counter for handling opening of entrances */
    private static int entranceOpenCtr;
    private static int startSoundCtr;
    private static boolean startSoundPlayed;
    private static final Set<Integer> entranceSounds = new HashSet<>(8);
    /** frame counter for handling time */
    private static int secondCtr;
    /** frame counter used to handle release of new Lemmings */
    private static int releaseCtr;
    /** threshold to release a new Lemming */
    private static int releaseBase;
    /** frame counter used to update animated sprite objects */
    private static int animCtr;
    /** level object */
    private static Level level;
    /** index of current rating */
    private static int curRating;
    /** index of current level pack */
    private static int curLevelPack;
    /** index of current level */
    private static int curLevelNumber;
    /** index of next rating */
    private static int nextRating;
    /** index of next level pack */
    private static int nextLevelPack;
    /** index of next level */
    private static int nextLevelNumber;
    private static String[] mods = new String[0];
    /** list of all active Lemmings in the Level */
    private static final List<Lemming> lemmings = new LinkedList<>();
    /** list of all active explosions */
    private static final List<Explosion> explosions = new LinkedList<>();
    /** list of all Lemmings under the mouse cursor */
    private static final Deque<Lemming> lemmsUnderCursor = new ArrayDeque<>(128);
    /** array of available level packs */
    private static LevelPack[] levelPack;
    /** small preview version of level used in briefing screen */
    private static Image mapPreview;
    /** timer used for nuking */
    private static NanosecondTimer timerNuke;
    /** key repeat object for plus key/icon */
    private static KeyRepeat plus;
    /** key repeat object for minus key/icon */
    private static KeyRepeat minus;
    /** Lemming for which skill change is requested */
    private static Lemming lemmSkillRequest;
    /** horizontal scrolling offset for level */
    private static int xPos;
    private static int xPosCenter;
    /** replay stream used for handling replays */
    private static ReplayStream replay;
    /** frame counter used for handling replays */
    private static int replayFrame;
    /** old value of release rate */
    private static int releaseRateOld;
    /** old value of nuke flag */
    private static boolean nukeOld;
    /** old value of horizontal scrolling position */
    private static int xPosOld;
    /** old value of selected skill */
    private static Lemming.Type lemmSkillOld;
    /** flag: replay mode is active */
    private static boolean replayMode;
    /** flag: replay mode should be stopped */
    private static boolean stopReplayMode;
    /** listener to inform GUI of player's progress */
    private static UpdateListener levelMenuUpdateListener;
    /** number of Lemmings which left the level */
    private static int numLeft;
    /** release rate 0-99 */
    private static int releaseRate;
    /** number of Lemmings available */
    private static int numLemmingsMax;
    /** number of Lemmings who entered the level */
    private static int numLemmingsOut;
    /** number of Lemmings which have to be rescued to finish the level */
    private static int numToRescue;
    /** time left in seconds */
    private static int time;
    private static boolean timed;
    /** number of climber skills left to be assigned */
    private static int numClimbers;
    /** number of floater skills left to be assigned */
    private static int numFloaters;
    /** number of bomber skills left to be assigned */
    private static int numBombers;
    /** number of blocker skills left to be assigned */
    private static int numBlockers;
    /** number of builder skills left to be assigned */
    private static int numBuilders;
    /** number of basher skills left to be assigned */
    private static int numBashers;
    /** number of miner skills left to be assigned */
    private static int numMiners;
    /** number of digger skills left to be assigned */
    private static int numDiggers;
    /** free running update counter */
    private static int updateCtr;
    /** gain for sound 0-2.0 */
    private static double soundGain = 1.0;
    /** gain for music 0-2.0 */
    private static double musicGain = 1.0;
    private static int width = Level.DEFAULT_WIDTH;
    private static int[] bgWidths;
    private static int[] bgHeights;
    private static int timesFailed;

    /**
     * Initialization.
     * @throws ResourceException
     */
    public static void init() throws ResourceException {
        fgImage = ToolBox.createTranslucentImage(width, Level.DEFAULT_HEIGHT);
        fgGfx = fgImage.createGraphicsContext();
        bgImages = new ArrayList<>(4);
        
        bgWidths = new int[0];
        bgHeights = new int[0];

        gameState = State.INIT;

        plus  = new KeyRepeat(NANOSEC_KEYREPEAT_START, NANOSEC_KEYREPEAT_REPEAT, NANOSEC_RELEASE_DOUBLE_CLICK);
        minus = new KeyRepeat(NANOSEC_KEYREPEAT_START, NANOSEC_KEYREPEAT_REPEAT, NANOSEC_RELEASE_DOUBLE_CLICK);
        timerNuke = new NanosecondTimer();
        
        level = new Level();
        // read level packs

        File dir = new File(Core.getResourcePath(), "levels");
        File[] files = dir.listFiles();
        // now get the names of the directories
        List<String> dirs = new ArrayList<>(32);
        for (File file : files) {
            if (file.isDirectory()) {
                dirs.add(file.getName());
            }
        }
        Collections.sort(dirs);

        List<LevelPack> levelPackList = new ArrayList<>(32);
        levelPackList.add(new LevelPack()); // dummy
        for (String lvlName : dirs) {
            String lp = Core.findResource("levels/" + ToolBox.addSeparator(lvlName) + "levelpack.ini");
            if (lp != null) {
                levelPackList.add(new LevelPack(lp));
            }
        }
        levelPack = levelPackList.toArray(new LevelPack[0]);
        curRating = 0;
        curLevelPack = 1; // since 0 is dummy
        curLevelNumber = 0;
        
        mods = levelPack[curLevelPack].getMods();
        
        sound = new Sound();
        sound.setGain(soundGain);
        Icons.init();
        Explosion.init();
        Lemming.loadLemmings();
        lemmSkillRequest = null;

        LemmFont.init();
        NumFont.init();
        LemmCursor.init();
        Music.init();
        Music.setGain(musicGain);
        MiscGfx.init(Level.DEFAULT_WIDTH / 16);
        
        timesFailed = 0;

        replayFrame = 0;
        replay = new ReplayStream();
        replayMode = false;
        stopReplayMode = false;

        wasCheated = isCheat();
    }

    /**
     * Calculate absolute level number from rating and relative level number
     * @param lvlPack level pack
     * @param rating rating
     * @param level relative level number
     * @return absolute level number (0-127)
     */
    static int absLevelNum(final int lvlPack, final int rating, final int level) {
        LevelPack lpack = levelPack[lvlPack];
        // calculate absolute level number
        int absLvl = level;
        for (int i = 0; i < rating; i++) {
            absLvl += lpack.getLevels(i).length;
        }
        return absLvl;
    }

    /**
     * Calculate rating and relative level number from absolute level number
     * @param lvlPack level pack
     * @param lvlAbs absolute level number
     * @return {rating, relative level number}
     */
    public static int[] relLevelNum(final int lvlPack, final int lvlAbs) {
        int[] retval = new int[2];
        LevelPack lpack = levelPack[lvlPack];
        int ratings = lpack.getRatings().length;
        int lvl = -1;
        int rating = -1;
        for (int i = 0, ls = 0; i < ratings; i++) {
            int lsOld = ls;
            // add number of levels existing in this rating
            ls += lpack.getLevels(i).length;
            if (lvlAbs < ls) {
                rating = i;
                lvl = lvlAbs - lsOld; // relative level mumber
                break;
            }
        }
        retval[0] = rating;
        retval[1] = lvl;
        return retval;
    }

    /**
     * Proceed to next level.
     * @return true: OK, false: no more levels in this rating
     */
    public static synchronized boolean nextLevel() {
        int num = curLevelNumber + 1;

        if (num < levelPack[curLevelPack].getLevels(curRating).length) {
            curLevelNumber = num;
            return true;
        } else {
            return false; // congrats - rating done
        }
    }
    
    /**
     * Proceed to next rating.
     * @return true: OK, false: no more ratings in this level pack
     */
    public static synchronized boolean nextRating() {
        int num = curRating + 1;

        if (num < levelPack[curLevelPack].getRatings().length) {
            curRating = num;
            curLevelNumber = 0;
            return true;
        } else {
            return false; // congrats - level pack done
        }
    }

    /**
     * Fade out at end of level.
     */
    public static synchronized void endLevel() {
        if (!replayMode && !wasCheated) {
            replay.addEndEvent(replayFrame);
        }
        transitionState = TransitionState.END_LEVEL;
        gameState = State.LEVEL_END;
        Fader.setState(Fader.State.OUT);
    }

    /**
     * Level successfully finished, enter debriefing and tell GUI to enable next level.
     */
    static synchronized void finishLevel() {
        Music.close();
        setFastForward(false);
        setSuperLemming(false);

        if (!wasLost() && curLevelPack != 0) {
            levelMenuUpdateListener.update();
            Core.player.store();
        }
        
        replayMode = false;
        gameState = State.DEBRIEFING;
    }

    /**
     * Hook for GUI to get informed when a level was successfully finished.
     * @param l UpdateListener
     */
    public static void setLevelMenuUpdateListener(final UpdateListener l) {
        levelMenuUpdateListener = l;
    }

    /**
     * Restart level.
     * @param doReplay true: replay, false: play
     */
    private static synchronized void restartLevel(final boolean doReplay) {
        if (!replayMode && wasLost() && (gameState == State.LEVEL
                || gameState == State.LEVEL_END
                || gameState == State.DEBRIEFING)) {
            timesFailed++;
        }
        initLevel();
        if (doReplay) {
            replayMode = true;
            replay.save(Core.getResourcePath() + "/replay.rpl");
            replay.rewind();
        } else {
            replayMode = false;
            replay.clear();
        }
    }
    
    /**
     * Initialize a level after it was loaded.
     */
    private static void initLevel() {
        Music.stop();
        
        setFastForward(false);
        setPaused(false);
        nuke = false;
        
        lemmSkillRequest = null;
        
        TextScreen.setMode(TextScreen.Mode.INIT);
        
        lemmings.clear();
        explosions.clear();
        Icons.reset();
        
        plus.init();
        minus.init();
        
        int oldWidth = width;
        
        numLeft = 0;
        releaseRate = StrictMath.min(StrictMath.max(level.getReleaseRate(), 0), MAX_RELEASE_RATE);
        numLemmingsMax = level.getNumLemmings();
        numLemmingsOut = 0;
        numToRescue  = level.getNumToRescue();
        time = level.getTimeLimitSeconds();
        numClimbers = level.getNumClimbers();
        numFloaters = level.getNumFloaters();
        numBombers = level.getNumBombers();
        numBlockers = level.getNumBlockers();
        numBuilders = level.getNumBuilders();
        numBashers = level.getNumBashers();
        numMiners = level.getNumMiners();
        numDiggers = level.getMumDiggers();
        xPosCenter = level.getXPosCenter();
        width = level.getWidth();
        bgWidths = level.getBgWidths();
        bgHeights = level.getBgHeights();
        if (time <= 0) {
            timed = false;
            time = 0;
        } else {
            timed = true;
        }
        
        if (width != oldWidth) {
            fgGfx.dispose();
            fgImage = ToolBox.createTranslucentImage(width, Level.DEFAULT_HEIGHT);
            fgGfx = fgImage.createGraphicsContext();
            MiscGfx.setMinimapWidth(width / 16);
        }
        fgGfx.setBackground(BLANK_COLOR);
        fgGfx.clearRect(0, 0, fgImage.getWidth(), fgImage.getHeight());
        
        bgImages.clear();
        int numBackgrounds = Math.min(bgWidths.length, bgHeights.length);
        for (int i = 0; i < numBackgrounds; i++) {
            Image bgImage = ToolBox.createTranslucentImage(bgWidths[i], bgHeights[i]);
            GraphicsContext bgGfx = bgImage.createGraphicsContext();
            bgGfx.setBackground(BLANK_COLOR);
            bgGfx.clearRect(0, 0, bgImage.getWidth(), bgImage.getHeight());
            bgGfx.dispose();
            bgImages.add(bgImage);
        }
        
        stencil = level.paintLevel(fgImage, bgImages, stencil);
        
        TrapDoor.reset(level.getNumEntrances());
        startSoundPlayed = false;
        startSoundCtr = 0;
        entranceOpened = false;
        entranceOpenCtr = 0;
        secondCtr = 0;
        releaseCtr = 0;
        lemmSkill = Lemming.Type.UNDEFINED;
        
        entranceSounds.clear();
        for (int i = 0; i < level.getNumEntrances(); i++) {
            SpriteObject spr = level.getSprObject(level.getEntrance(i).id);
            if (spr != null) {
                entranceSounds.add(spr.getSound());
            }
        }
        
        calcReleaseBase();
        
        int scaleFactor = width / 800;
        if (width % 800 != 0) {
            scaleFactor++;
        }
        if (scaleFactor < 4) {
            scaleFactor = 4;
        }
        mapPreview = level.createMinimap(mapPreview, fgImage, scaleFactor, scaleFactor, false, true);
        
        setSuperLemming(level.isSuperLemming());
        forceNormalTimerSpeed = level.getForceNormalTimerSpeed();
        
        replayFrame = 0;
        stopReplayMode = false;
        releaseRateOld = releaseRate;
        lemmSkillOld = lemmSkill;
        nukeOld = false;
        //xPosOld = level.getXPos();
        
        try {
            Music.load("music/" + GameController.levelPack[GameController.curLevelPack].getInfo(GameController.curRating,
                    GameController.curLevelNumber).getMusic());
        } catch (ResourceException ex) {
            Core.resourceError(ex.getMessage());
        } catch (LemmException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        
        gameState = State.BRIEFING;
        
        wasCheated = isCheat();
    }
    
    /**
     * Request the restart of this level.
     * @param doReplay
     */
    public static synchronized void requestRestartLevel(final boolean doReplay) {
        if (doReplay || replayMode) {
            transitionState = TransitionState.REPLAY_LEVEL;
        } else {
            transitionState = TransitionState.RESTART_LEVEL;
        }
        if (gameState == State.LEVEL) {
            gameState = State.LEVEL_END;
        }
        Fader.setState(Fader.State.OUT);
    }

    /**
     * Request a new level.
     * @param lPack index of level pack
     * @param rating index of rating
     * @param lNum level number
     * @param doReplay true: replay, false: play
     */
    public static synchronized void requestChangeLevel(final int lPack, final int rating, final int lNum, final boolean doReplay) {
        nextLevelPack = lPack;
        nextRating = rating;
        nextLevelNumber = lNum;

        if (doReplay) {
            transitionState = TransitionState.LOAD_REPLAY;
        } else {
            transitionState = TransitionState.LOAD_LEVEL;
        }
        if (gameState == State.LEVEL) {
            gameState = State.LEVEL_END;
        }
        Fader.setState(Fader.State.OUT);
    }

    /**
     * Start a new level.
     * @param lPack index of level pack
     * @param rating index of rating
     * @param lNum level number
     * @param doReplay true: replay, false: play
     */
    private static synchronized Level changeLevel(final int lPack, final int rating, final int lNum, final boolean doReplay) throws ResourceException, LemmException {
        //gameState = GAME_ST_INIT;
        timesFailed = 0;
        
        curLevelPack = lPack;
        curRating = rating;
        curLevelNumber = lNum;
        
        String[] oldMods = mods;
        mods = levelPack[curLevelPack].getMods();
        if (!Arrays.equals(mods, oldMods)) {
            sound.load();
            Icons.init();
            Explosion.init();
            LemmFont.init();
            NumFont.init();
            LemmCursor.init();
            MiscGfx.init(Level.DEFAULT_WIDTH / 16);
        }
        
        String lvlPath = levelPack[curLevelPack].getInfo(curRating, curLevelNumber).getFileName();
        // lemmings need to be reloaded to contain pink color
        Lemming.loadLemmings();
        // loading the level will patch pink lemmings pixels to correct color
        level.loadLevel(lvlPath);
        
        // if width and height would be stored inside the level, the fgImage etc. would have to
        // be recreated here
        // fgImage = gc.createCompatibleImage(Level.width, Level.height, Transparency.BITMASK);
        // fgGfx = fgImage.createGraphics();
        
        initLevel();
        
        if (doReplay) {
            replayMode = true;
            replay.rewind();
        } else {
            replayMode = false;
            replay.clear();
        }
        
        return level;
    }
    
    /**
     * Get level lost state.
     * @return true if level was lost, false otherwise
     */
    static synchronized boolean wasLost() {
        return gameState == State.LEVEL || numLeft < numToRescue;
    }
    
    /**
     * Get current replay image.
     * @return current replay image
     */
    public static synchronized Image getReplayImage() {
        if (!replayMode) {
            return null;
        }
        if ((replayFrame & 0x3f) > 0x20) {
            return MiscGfx.getImage(MiscGfx.Index.REPLAY_1);
        } else {
            return MiscGfx.getImage(MiscGfx.Index.REPLAY_2);
        }
    }

    /**
     * Get a Lemming under the selection cursor.
     * @param type cursor type
     * @return fitting Lemming or null if none found
     */
    public static synchronized Lemming lemmUnderCursor(final LemmCursor.CursorType type) {
        // search for level without the skill
        if (type != LemmCursor.CursorType.WALKER
                && type != LemmCursor.CursorType.WALKER_LEFT
                && type != LemmCursor.CursorType.WALKER_RIGHT) {
            for (Lemming l : lemmsUnderCursor) {
                if (type == LemmCursor.CursorType.LEFT && l.getDirection() != Lemming.Direction.LEFT) {
                    continue;
                }
                if (type == LemmCursor.CursorType.RIGHT && l.getDirection() != Lemming.Direction.RIGHT) {
                    continue;
                }
                switch (l.getSkill()) {
                    case BLOCKER:
                        if (/*l.canChangeSkill() && */l.getSkill() != lemmSkill && !l.getName().isEmpty()) {
                            switch (lemmSkill) {
                                case FLAPPER:
                                    if (!l.hasTimer()) {
                                        return l;
                                    }
                                    break;
                                default:
                                    break;
                            }
                        }
                        break;
                    case BUILDER:
                    case SHRUGGER:
                    case BASHER:
                    case MINER:
                    case DIGGER:
                        if (/*l.canChangeSkill() && */l.getSkill() != lemmSkill && !l.getName().isEmpty()) {
                            switch (lemmSkill) {
                                case CLIMBER:
                                    if (!l.canClimb()) {
                                        return l;
                                    }
                                    break;
                                case FLOATER:
                                    if (!l.canFloat()) {
                                        return l;
                                    }
                                    break;
                                case FLAPPER:
                                    if (!l.hasTimer()) {
                                        return l;
                                    }
                                    break;
                                default:
                                    return l;
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        for (Lemming l : lemmsUnderCursor) {
            // Walker-only cursor: ignore non-walkers
            if ((type == LemmCursor.CursorType.WALKER
                    || type == LemmCursor.CursorType.WALKER_LEFT
                    || type == LemmCursor.CursorType.WALKER_RIGHT) && l.getSkill() != Lemming.Type.WALKER) {
                continue;
            }
            if ((type == LemmCursor.CursorType.LEFT
                    || type == LemmCursor.CursorType.WALKER_LEFT) && l.getDirection() != Lemming.Direction.LEFT) {
                continue;
            }
            if ((type == LemmCursor.CursorType.RIGHT
                    || type == LemmCursor.CursorType.WALKER_RIGHT) && l.getDirection() != Lemming.Direction.RIGHT) {
                continue;
            }
            switch (l.getSkill()) {
                case WALKER:
                    if (/*l.canChangeSkill() && */l.getSkill() != lemmSkill && !l.getName().isEmpty()) {
                        switch (lemmSkill) {
                            case CLIMBER:
                                if (!l.canClimb()) {
                                    return l;
                                }
                                break;
                            case FLOATER:
                                if (!l.canFloat()) {
                                    return l;
                                }
                                break;
                            case FLAPPER:
                                if (!l.hasTimer()) {
                                    return l;
                                }
                                break;
                            default:
                                return l;
                        }
                    }
                    break;
                case FALLER:
                case CLIMBER:
                case FLIPPER:
                case FLOATER:
                case JUMPER:
                    if (/*l.canChangeSkill() && */l.getSkill() != lemmSkill && !l.getName().isEmpty()) {
                        switch (lemmSkill) {
                            case CLIMBER:
                                if (!l.canClimb()) {
                                    return l;
                                }
                                break;
                            case FLOATER:
                                if (!l.canFloat()) {
                                    return l;
                                }
                                break;
                            case FLAPPER:
                                if (!l.hasTimer()) {
                                    return l;
                                }
                                break;
                            case UNDEFINED:
                                return l;
                            default:
                                break;
                        }
                    }
                    break;
                default:
                    break;
            }
        }
        if (type == LemmCursor.CursorType.NORMAL) {
            for (Lemming l : lemmsUnderCursor) {
                if (!l.getName().isEmpty()) {
                    return l;
                }
            }
        }
        return null;
    }

    /**
     * Lemming has left the Level.
     */
    static synchronized void increaseLeft() {
        numLeft += 1;
    }

    /**
     * Stop replay.
     */
    public static void stopReplayMode() {
        if (replayMode) {
            stopReplayMode = true;
        }
    }

    /**
     * Return time as String "minutes-seconds"
     * @return time as String "minutes-seconds"
     */
    public static synchronized String getTimeString() {
        return String.format("%d-%02d", time / 60, time % 60);
    }

    /**
     * Update the whole game state by one frame.
     */
    public static synchronized void update() {
        if (gameState != State.LEVEL) {
            return;
        }

        updateCtr++;

        if (!replayMode) {
            assignSkill(false); // first try to assign skill
        }

        // check +/- buttons also if paused
        KeyRepeat.Event fired = plus.fired();
        if (fired != KeyRepeat.Event.NONE) {
            if (releaseRate < MAX_RELEASE_RATE) {
                if (fired == KeyRepeat.Event.DOUBLE_CLICK) {
                    releaseRate = MAX_RELEASE_RATE;
                } else {
                    releaseRate += 1;
                }
                calcReleaseBase();
                sound.playPitched(Sound.PitchedEffect.RELEASE_RATE, releaseRate);
            } else {
                sound.play(Sound.Effect.INVALID);
            }
        }

        fired = minus.fired();
        if (fired != KeyRepeat.Event.NONE) {
            if (releaseRate > level.getReleaseRate()) {
                if (fired == KeyRepeat.Event.DOUBLE_CLICK) {
                    releaseRate = level.getReleaseRate();
                } else {
                    releaseRate -= 1;
                }
                calcReleaseBase();
                sound.playPitched(Sound.PitchedEffect.RELEASE_RATE, releaseRate);
            } else {
                sound.play(Sound.Effect.INVALID);
            }
        }


        if (isPaused()) {
            return;
        }

        // test for end of replay mode
        if (replayMode && stopReplayMode) {
            replay.clearFrom(replayFrame);
            replayMode = false;
            stopReplayMode = false;
        }

        if (!replayMode) {
            if (!wasCheated) {
                // replay: release rate changed?
                if (releaseRate != releaseRateOld) {
                    replay.addReleaseRateEvent(replayFrame, releaseRate);
                    releaseRateOld = releaseRate;
                }
                // replay: nuked?
                if (nuke != nukeOld) {
                    replay.addNukeEvent(replayFrame);
                    nukeOld = nuke;
                }
                // replay: xPos changed?
                if (getXPos() != xPosOld) {
                    replay.addPosEvent(replayFrame, getXPos() + Lemmini.getPaneWidth() / 2, 0, 0);
                    xPosOld = getXPos();
                }
                // skill changed
                if (lemmSkill != lemmSkillOld) {
                    replay.addSelectSkillEvent(replayFrame, lemmSkill, 0);
                    lemmSkillOld = lemmSkill;
                }
            } else {
                replay.clear();
            }
        } else {
            // replay mode
            ReplayEvent r;
            while ((r = replay.getNext(replayFrame)) != null) {
                switch (r.type) {
                    case ReplayStream.ASSIGN_SKILL: {
                        ReplayAssignSkillEvent rs = (ReplayAssignSkillEvent) r;
                        double pan;
                        synchronized (lemmings) {
                            Lemming l = lemmings.get(rs.lemming);
                            l.setSkill(rs.skill, false);
                            l.setSelected();
                            pan = l.getPan();
                        }
                        switch (rs.skill) {
                            case CLIMBER:
                                numClimbers -= 1;
                                break;
                            case FLOATER:
                                numFloaters -= 1;
                                break;
                            case FLAPPER:
                                numBombers -= 1;
                                break;
                            case BLOCKER:
                                numBlockers -= 1;
                                break;
                            case BUILDER:
                                numBuilders -= 1;
                                break;
                            case BASHER:
                                numBashers -= 1;
                                break;
                            case MINER:
                                numMiners -= 1;
                                break;
                            case DIGGER:
                                numDiggers -= 1;
                                break;
                            default:
                                break;
                        }
                        sound.play(Sound.Effect.SELECT_SKILL, pan);
                        break;
                    }
                    case ReplayStream.SET_RELEASE_RATE:
                        ReplayReleaseRateEvent rr = (ReplayReleaseRateEvent) r;
                        releaseRate = rr.releaseRate;
                        calcReleaseBase();
                        sound.playPitched(Sound.PitchedEffect.RELEASE_RATE, releaseRate);
                        releaseRateOld = releaseRate;
                        break;
                    case ReplayStream.NUKE:
                        //nuke = true;
                        nuke();
                        Icons.press(Icons.Type.NUKE);
                        nukeOld = nuke;
                        break;
                    case ReplayStream.MOVE_POS: {
                        ReplayMovePosEvent rx = (ReplayMovePosEvent) r;
                        if (rx.player == 0) {
                            setXPos(rx.xPos - Lemmini.getPaneWidth() / 2);
                            xPosOld = xPos;
                        }
                        break;
                    }
                    case ReplayStream.SELECT_SKILL: {
                        ReplaySelectSkillEvent rs = (ReplaySelectSkillEvent) r;
                        if (rs.player == 0) {
                            lemmSkill = rs.skill;
                            switch (lemmSkill) {
                                case CLIMBER:
                                    Icons.press(Icons.Type.CLIMB);
                                    break;
                                case FLOATER:
                                    Icons.press(Icons.Type.FLOAT);
                                    break;
                                case FLAPPER:
                                    Icons.press(Icons.Type.BOMB);
                                    break;
                                case BLOCKER:
                                    Icons.press(Icons.Type.BLOCK);
                                    break;
                                case BUILDER:
                                    Icons.press(Icons.Type.BUILD);
                                    break;
                                case BASHER:
                                    Icons.press(Icons.Type.BASH);
                                    break;
                                case MINER:
                                    Icons.press(Icons.Type.MINE);
                                    break;
                                case DIGGER:
                                    Icons.press(Icons.Type.DIG);
                                    break;
                                default:
                                    break;
                            }
                            lemmSkillOld = lemmSkill;
                        }
                        break;
                    }
                    case ReplayStream.END:
                        stopReplayMode = true;
                        break;
                    default:
                        break;
                }
            }
        }

        // replay: xpos changed

        // store locally to avoid it's overwritten amidst function
        boolean nukeTemp = nuke;

        // time
        secondCtr++;
        if (secondCtr > ((superLemming && forceNormalTimerSpeed)
                ? MAX_SUPERLEMMING_SECOND_CTR[time % MAX_SUPERLEMMING_SECOND_CTR.length]
                : MAX_SECOND_CTR[time % MAX_SECOND_CTR.length])) {
            // one second passed
            secondCtr -= (superLemming && forceNormalTimerSpeed)
                    ? MAX_SUPERLEMMING_SECOND_CTR[time % MAX_SUPERLEMMING_SECOND_CTR.length]
                    : MAX_SECOND_CTR[time % MAX_SECOND_CTR.length];
            
            if (timed) {
                if (time > 0) {
                    time--;
                }
            } else {
                time++;
            }
            if (!isCheat() && time <= 0 && timed) {
                // level failed
                endLevel();
            }
        }
        // release
        if (entranceOpened && !nukeTemp && !isPaused() && numLemmingsOut < getNumLemmingsMax() && --releaseCtr <= 0) {
            releaseCtr = releaseBase;
            if (level.getNumEntrances() != 0) {
                Entrance e = level.getEntrance(TrapDoor.getNext());
                Lemming l = new Lemming(e.xPos + 2, e.yPos + 20, Lemming.Direction.RIGHT);
                synchronized (lemmings) {
                    lemmings.add(l);
                }
                numLemmingsOut++;
            }
        }
        // nuking
        if (nukeTemp && ((updateCtr & 1) == 1)) {
            synchronized (lemmings) {
                for (Lemming l : lemmings) {
                    if (!l.nuke() && !l.hasDied() && !l.hasLeft()) {
                        l.setSkill(Lemming.Type.NUKE, false);
                        //System.out.println("nuked!");
                        break;
                    }
                }
            }
        }
        
        if (!startSoundPlayed) {
            if (++startSoundCtr == MAX_START_SOUND_CTR) {
                sound.play(Sound.Effect.START);
                startSoundPlayed = true;
            }
        }
        // open trap doors?
        if (!entranceOpened) {
            if (++entranceOpenCtr == MAX_ENTRANCE_OPEN_CTR) {
                for (int i = 0; i < level.getSprObjectNum(); i++) {
                    SpriteObject spr = level.getSprObject(i);
                    if (spr != null && spr.getAnimMode() == Sprite.Animation.ONCE_ENTRANCE) {
                        spr.setAnimMode(Sprite.Animation.ONCE);
                    }
                }
                level.openBackgroundEntrances();
                for (int i : entranceSounds) {
                    sound.play(i);
                }
            } else if (entranceOpenCtr == MAX_ENTRANCE_OPEN_CTR + 15 * MAX_ANIM_CTR) {
                //System.out.println("opened");
                entranceOpened = true;
                releaseCtr = 0; // first lemming to enter at once
                if (musicOn) {
                    Music.play();
                }
            }
        }
        // end of game conditions
        if ((nukeTemp || numLemmingsOut == getNumLemmingsMax()) && lemmings.isEmpty()) {
            // End the level only if no objects are triggered.
            boolean endLevel = true;
            for (int i = 0; i < level.getSprObjectNum(); i++) {
                SpriteObject sprite = level.getSprObject(i);
                if (sprite != null && sprite.isTriggered()) {
                    endLevel = false;
                    break;
                }
            }
            if (endLevel) {
                endLevel();
            }
        }

        synchronized (lemmings) {
            Iterator<Lemming> it = lemmings.iterator();
            while (it.hasNext()) {
                Lemming l = it.next();
                l.animate();
                if (l.hasDied() || l.hasLeft()) {
                    it.remove();
                }
            }
        }

        synchronized (explosions) {
            Iterator<Explosion> it = explosions.iterator();
            while (it.hasNext()) {
                Explosion e = it.next();
                if (e.isFinished()) {
                    it.remove();
                } else {
                    e.update();
                }
            }
        }

        // animate level objects
        if (animCtr >= MAX_ANIM_CTR) {
            animCtr -= MAX_ANIM_CTR;
            for (int n = 0; n < level.getSprObjectNum(); n++) {
                SpriteObject spr = level.getSprObject(n);
                if (spr != null) {
                    spr.getImageAnim(); // just to animate
                }
            }
            level.advanceBackgroundFrame();
        }
        animCtr++;

        if (!replayMode) {
            assignSkill(true); // 2nd try to assign skill
        }

        replayFrame++;
    }

    /**
     * Request a skill change for a Lemming (currently selected skill).
     * @param lemm Lemming
     */
    public static synchronized void requestSkill(final Lemming lemm) {
        if (lemmSkill != Lemming.Type.UNDEFINED) {
            lemmSkillRequest = lemm;
        }
        stopReplayMode();
    }

    /**
     * Assign the selected skill to the selected Lemming.
     * @param delete flag: reset the current skill request
     */
    private static synchronized void assignSkill(final boolean delete) {
        if (lemmSkillRequest == null || lemmSkill == Lemming.Type.UNDEFINED) {
            return;
        }

        Lemming lemm = lemmSkillRequest;
        if (delete) {
            lemmSkillRequest = null;
        }

        boolean canSet = false;
        stopReplayMode();

        if (isCheat()) {
            canSet = lemm.setSkill(lemmSkill, true);
        } else {
            switch (lemmSkill) {
                case CLIMBER:
                    if (numClimbers > 0) {
                        if (lemm.setSkill(lemmSkill, true)) {
                            numClimbers -= 1;
                            canSet = true;
                        }
                    } else {
                        sound.play(Sound.Effect.INVALID, lemm.getPan());
                    }
                    break;
                case FLOATER:
                    if (numFloaters > 0) {
                        if (lemm.setSkill(lemmSkill, true)) {
                            numFloaters -= 1;
                            canSet = true;
                        }
                    } else {
                        sound.play(Sound.Effect.INVALID, lemm.getPan());
                    }
                    break;
                case FLAPPER:
                    if (numBombers > 0) {
                        if (lemm.setSkill(lemmSkill, true)) {
                            numBombers -= 1;
                            canSet = true;
                        }
                    } else {
                        sound.play(Sound.Effect.INVALID, lemm.getPan());
                    }
                    break;
                case BLOCKER:
                    if (numBlockers > 0) {
                        if (lemm.setSkill(lemmSkill, true)) {
                            numBlockers -= 1;
                            canSet = true;
                        }
                    } else {
                        sound.play(Sound.Effect.INVALID, lemm.getPan());
                    }
                    break;
                case BUILDER:
                    if (numBuilders > 0) {
                        if (lemm.setSkill(lemmSkill, true)) {
                            numBuilders -= 1;
                            canSet = true;
                        }
                    } else {
                        sound.play(Sound.Effect.INVALID, lemm.getPan());
                    }
                    break;
                case BASHER:
                    if (numBashers > 0) {
                        if (lemm.setSkill(lemmSkill, true)) {
                            numBashers -= 1;
                            canSet = true;
                        }
                    } else {
                        sound.play(Sound.Effect.INVALID, lemm.getPan());
                    }
                    break;
                case MINER:
                    if (numMiners > 0) {
                        if (lemm.setSkill(lemmSkill, true)) {
                            numMiners -= 1;
                            canSet = true;
                        }
                    } else {
                        sound.play(Sound.Effect.INVALID, lemm.getPan());
                    }
                    break;
                case DIGGER:
                    if (numDiggers > 0) {
                        if (lemm.setSkill(lemmSkill, true)) {
                            numDiggers -= 1;
                            canSet = true;
                        }
                    } else {
                        sound.play(Sound.Effect.INVALID, lemm.getPan());
                    }
                    break;
                default:
                    break;
            }
        }
        if (canSet) {
            lemmSkillRequest = null; // erase request
            if (isPaused()) {
                setPaused(false);
                Icons.press(Icons.Type.PAUSE);
            }
            // add to replay stream
            if (!wasCheated) {
                synchronized (lemmings) {
                    for (int i = 0; i < lemmings.size(); i++) {
                        if (lemmings.get(i) == lemm) { // if 2nd try (delete==true) assign to next frame
                            replay.addAssignSkillEvent(replayFrame + ((delete) ? 1 : 0), lemmSkill, i);
                        }
                    }
                }
            }
        } else if (!delete) {
            lemmSkillRequest = null;
        }
    }

    /**
     * Calculate the counter threshold for releasing a new Lemmings.
     */
    private static void calcReleaseBase() {
        // the original formula is: release lemming every 4+(99-speed)/2 time steps
        // where one step is 60ms (3s/50) or 66ms (4s/60).
        // Lemmini runs at 30ms/33ms, so the term has to be multiplied by 2
        // 8+(99-releaseRate) should be correct
        releaseBase = 8 + (99 - releaseRate);
    }

    /**
     * Handle pressing of an icon button.
     * @param type icon type
     */
    public static synchronized void handleIconButton(final Icons.Type type) {
        Lemming.Type lemmSkillOld = lemmSkill;
        boolean ok = false;
        switch (type) {
            case PLUS:
                ok = true; // supress sound
                plus.pressed(KEYREPEAT_ICON);
                stopReplayMode();
                break;
            case MINUS:
                ok = true; // supress sound
                minus.pressed(KEYREPEAT_ICON);
                stopReplayMode();
                break;
            case CLIMB:
                if (isCheat() || numClimbers > 0) {
                    lemmSkill = Lemming.Type.CLIMBER;
                }
                stopReplayMode();
                break;
            case FLOAT:
                if (isCheat() || numFloaters > 0) {
                    lemmSkill = Lemming.Type.FLOATER;
                }
                stopReplayMode();
                break;
            case BOMB:
                if (isCheat() || numBombers > 0) {
                    lemmSkill = Lemming.Type.FLAPPER;
                }
                stopReplayMode();
                break;
            case BLOCK:
                if (isCheat() || numBlockers > 0) {
                    lemmSkill = Lemming.Type.BLOCKER;
                }
                stopReplayMode();
                break;
            case BUILD:
                if (isCheat() || numBuilders > 0) {
                    lemmSkill = Lemming.Type.BUILDER;
                }
                stopReplayMode();
                break;
            case BASH:
                if (isCheat() || numBashers > 0) {
                    lemmSkill = Lemming.Type.BASHER;
                }
                stopReplayMode();
                break;
            case MINE:
                if (isCheat() || numMiners > 0) {
                    lemmSkill = Lemming.Type.MINER;
                }
                stopReplayMode();
                break;
            case DIG:
                if (isCheat() || numDiggers > 0) {
                    lemmSkill = Lemming.Type.DIGGER;
                }
                stopReplayMode();
                break;
            case PAUSE:
                setPaused(!isPaused());
                ok = true;
                break;
            case NUKE:
                {
                    if (!nuke) {
                        ok = true;
                    }
                    stopReplayMode();
                    if (timerNuke.delta() < NANOSEC_NUKE_DOUBLE_CLICK) {
                        if (!nuke) {
                            nuke();
                        }
                    } else {
                        timerNuke.deltaUpdate();
                    }
                    break;
                }
            case FFWD:
                setFastForward(!isFastForward());
                ok = true;
                break;
            default:
                break;
        }
        if (ok || lemmSkill != lemmSkillOld) {
            switch (type) {
                case PLUS:
                case MINUS:
                    break; // supress sound
                default:
                    sound.playPitched(Sound.PitchedEffect.SKILL, type.getPitch());
                    break;
            }
            Icons.press(type);
        } else {
            sound.play(Sound.Effect.INVALID);
        }
    }
    
    private static void nuke() {
        nuke = true;
        sound.play(Sound.Effect.NUKE);
    }
    
    public static boolean isNuked() {
        return nuke;
    }

    /**
     * Fade in/out.
     * @param g graphics object
     */
    public static void fade(final GraphicsContext g) {
        if (Fader.getState() == Fader.State.OFF && transitionState != TransitionState.NONE) {
            switch (transitionState) {
                case END_LEVEL:
                    finishLevel();
                    Lemmini.setCursor(LemmCursor.CursorType.NORMAL);
                    break;
                case TO_BRIEFING:
                    gameState = State.BRIEFING;
                    break;
                case TO_DEBRIEFING:
                    gameState = State.DEBRIEFING;
                    break;
                case TO_INTRO:
                    gameState = State.INTRO;
                    break;
                case TO_LEVEL:
                    setXPos(xPosCenter - Lemmini.getPaneWidth() / 2);
                    xPosOld = xPos;
                    gameState = State.LEVEL;
                    break;
                case RESTART_LEVEL:
                case REPLAY_LEVEL:
                    restartLevel(transitionState == TransitionState.REPLAY_LEVEL);
                    Lemmini.setCursor(LemmCursor.CursorType.NORMAL);
                    break;
                case LOAD_LEVEL:
                case LOAD_REPLAY:
                    try {
                        changeLevel(nextLevelPack, nextRating, nextLevelNumber, transitionState == TransitionState.LOAD_REPLAY);
                        int numLemmings = level.getNumLemmings();
                        String lemmingWord = (numLemmings == 1) ? "Lemming" : "Lemmings";
                        if (level.getNumLemmings() <= 100) {
                            Core.setTitle(String.format("SuperLemmini - %s - Save %d%% of %d %s",
                                    level.getLevelName().trim(),
                                    level.getNumToRescue() * 100 / numLemmings,
                                    numLemmings,
                                    lemmingWord));
                        } else {
                            Core.setTitle(String.format("SuperLemmini - %s - Save %d of %d %s",
                                    level.getLevelName().trim(),
                                    level.getNumToRescue(),
                                    numLemmings,
                                    lemmingWord));
                        }
                    } catch (ResourceException ex) {
                        Core.resourceError(ex.getMessage());
                    } catch (LemmException ex) {
                        JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        System.exit(1);
                    }
                    Lemmini.setCursor(LemmCursor.CursorType.NORMAL);
                    break;
                default:
                    break;
            }
            if (transitionState == TransitionState.TO_LEVEL) {
                Fader.setStep(Fader.FADE_STEP_SLOW);
            } else {
                Fader.setStep(Fader.FADE_STEP_FAST);
            }
            Fader.setState(Fader.State.IN, g);
            transitionState = TransitionState.NONE;
        } else {
            Fader.fade(g);
        }
        if (gameState == State.LEVEL_END
                || gameState == State.LEVEL
                && transitionState != TransitionState.NONE) {
            fadeSound();
        }
    }

    /**
     * Draw the explosions
     * @param g graphics object
     * @param width width of screen in pixels
     * @param height height of screen in pixels
     * @param xOfs horizontal level offset in pixels
     */
    public static void drawExplosions(final GraphicsContext g, final int width, final int height, final int xOfs) {
        synchronized (explosions) {
            for (Explosion e : explosions) {
                e.draw(g, width, height, xOfs);
            }
        }
    }

    /**
     * Add a new explosion.
     * @param x x coordinate in pixels.
     * @param y y coordinate in pixels.
     */
    public static void addExplosion(final int x, final int y) {
        // create particle explosion
        synchronized (GameController.explosions) {
            GameController.explosions.add(new Explosion(x, y));
        }
    }

    /**
     * Draw icon bar.
     * @param g graphics object
     * @param x x coordinate in pixels
     * @param y y coordinate in pixels
     */
    public static void drawIcons(final GraphicsContext g, final int x, final int y) {
        g.drawImage(Icons.getImg(), x, y);
    }

    /**
     * Draw the skill/release rate values
     * @param g graphics object
     * @param y y offset in pixels
     */
    public static void drawCounters(final GraphicsContext g, final int y) {
        // draw counters
        int val = 0;
        for (int i = 0; i < 10; i++) {
            switch (i) {
                case 0:
                    val = level.getReleaseRate();
                    break;
                case 1:
                    val = releaseRate;
                    break;
                case 2:
                    val = numClimbers;
                    break;
                case 3:
                    val = numFloaters;
                    break;
                case 4:
                    val = numBombers;
                    break;
                case 5:
                    val = numBlockers;
                    break;
                case 6:
                    val = numBuilders;
                    break;
                case 7:
                    val = numBashers;
                    break;
                case 8:
                    val = numMiners;
                    break;
                case 9:
                    val = numDiggers;
                    break;
                default:
                    break;
            }
            if (i >= 2 && val <= 0) {
                continue;
            }
            if (val < 0) {
                val = 0;
            }
            if (val > 999) {
                val = 999;
            }
            //g.drawImage(NumFont.numImage(val),Icons.WIDTH*i+8,y);
            if (val >= 100) {
                g.drawImage(NumFont.numImage(val / 100),
                        Icons.WIDTH * i + Icons.WIDTH / 2 - (int) (NumFont.getWidth() * 1.5), y);
                g.drawImage(NumFont.numImage(val % 100 / 10),
                        Icons.WIDTH * i + Icons.WIDTH / 2 - NumFont.getWidth() / 2, y);
                g.drawImage(NumFont.numImage(val % 10),
                        Icons.WIDTH * i + Icons.WIDTH / 2 + NumFont.getWidth() / 2, y);
            } else {
                g.drawImage(NumFont.numImage(val / 10),
                        Icons.WIDTH * i + Icons.WIDTH / 2 - NumFont.getWidth(), y);
                g.drawImage(NumFont.numImage(val % 10),
                        Icons.WIDTH * i + Icons.WIDTH / 2, y);
            }
        }

    }

    /**
     * Get index of current level pack.
     * @return index of current level pack
     */
    public static int getCurLevelPackIdx() {
        return curLevelPack;
    }

    /**
     * Get current level pack.
     * @return current level pack
     */
    public static LevelPack getCurLevelPack() {
        return levelPack[curLevelPack];
    }

    /**
     * get number of level packs
     * @return number of level packs
     */
    public static int getLevelPackCount() {
        return levelPack.length;
    }

    /**
     * Get level pack via index.
     * @param i index of level pack
     * @return LevelPack
     */
    public static LevelPack getLevelPack(final int i) {
        return levelPack[i];
    }

    /**
     * Get index of current rating.
     * @return index of current rating
     */
    public static int getCurRating() {
        return curRating;
    }

    /**
     * Get number of current level.
     * @return number of current level
     */
    public static int getCurLevelNumber() {
        return curLevelNumber;
    }


    /**
     * Set horizontal scrolling offset.
     * @param x horizontal scrolling offset in pixels
     */
    public static void setXPos(final int x) {
        xPos = fixXPos(x);
    }

    /**
     * Get horizontal scrolling offset.
     * @return horizontal scrolling offset in pixels
     */
    public static int getXPos() {
        return xPos;
    }
    
    private static int fixXPos(int x) {
        if (width < Lemmini.getPaneWidth()) {
            return (width - Lemmini.getPaneWidth()) / 2;
        } else {
            if (x >= width - Lemmini.getPaneWidth()) {
                return width - Lemmini.getPaneWidth();
            } else if (x < 0) {
                return 0;
            } else {
                return x;
            }
        }
    }

    /**
     * Set game state.
     * @param s new game state
     */
    public static void setGameState(final State s) {
        gameState = s;
    }

    /**
     * Get game state.
     * @return game state
     */
    public static State getGameState() {
        return gameState;
    }

    /**
     * Enable/disable cheat mode.
     * @param c true: enable, false: disable
     */
    public static void setCheat(final boolean c) {
        cheat = c;
    }

    /**
     * Get state of cheat mode.
     * @return true if cheat mode enabled, false otherwise
     */
    public static boolean isCheat() {
        return cheat;
    }

    /**
     * Set transition state.
     * @param ts TransitionState
     */
    public static void setTransition(final TransitionState ts) {
        transitionState = ts;
    }


    /**
     * Load a replay.
     * @param fn file name
     * @return replay level info object
     */
    public static ReplayLevelInfo loadReplay(final String fn) {
        return replay.load(fn);
    }

    /**
     * Save a replay.
     * @param fn file name
     * @return true if saved successfully, false otherwise
     */
    public static boolean saveReplay(final String fn) {
        return replay.save(fn);
    }

    /**
     * Activate/deactivate Superlemming mode.
     * @param sl true: activate, false: deactivate
     */
    public static void setSuperLemming(final boolean sl) {
        superLemming = sl;
    }

    /**
     * Get Superlemming state.
     * @return true is Superlemming mode is active, false otherwise
     */
    public static boolean isSuperLemming() {
        return superLemming;
    }

    /**
     * Set cheated detection.
     * @param c true: cheat mode was activated, false otherwise
     */
    public static void setWasCheated(final boolean c) {
        wasCheated = c;
    }
    
    public static boolean getWasCheated() {
        return wasCheated;
    }

    /**
     * Enable pause mode.
     * @param p true: pause is active, false otherwise
     */
    public static void setPaused(final boolean p) {
        paused = p;
    }

    /**
     * Get pause state.
     * @return true if pause is active, false otherwise
     */
    public static boolean isPaused() {
        return paused;
    }

    /**
     * Enable fast forward mode.
     * @param ff true: fast forward is active, false otherwise
     */
    public static void setFastForward(final boolean ff) {
        fastForward = ff;
    }

    /**
     * Get fast forward state.
     * @return true if fast forward is active, false otherwise
     */
    public static boolean isFastForward() {
        return fastForward;
    }

    /** get number of lemmings left in the game
     * @return number of lemmings left in the game
     */
    public static int getNumLeft() {
        return numLeft;
    }

    /**
     * Set number of Lemmings left in the game.
     * @param n number of Lemmings left in the game
     */
    public static void setNumLeft(final int n) {
        numLeft = n;
    }

    /**
     * Get level object.
     * @return level object
     */
    public static Level getLevel() {
        return level;
    }

    /**
     * Get maximum number of Lemmings for this level.
     * @return maximum number of Lemmings for this level
     */
    public static int getNumLemmingsMax() {
        return numLemmingsMax;
    }

    /**
     * Get icon type from x position.
     * @param x x position in pixels
     * @return icon type
     */
    public static Icons.Type getIconType(final int x) {
        return Icons.getType(x);
    }

    /**
     * Icon was pressed.
     * @param t icon type
     */
    public static void pressIcon(final Icons.Type t) {
        Icons.press(t);
    }

    /**
     * Icon was released.
     * @param t icon type
     */
    public static void releaseIcon(final Icons.Type t) {
        Icons.release(t);
    }

    /**
     * Plus was pressed.
     * @param d bitmask: key or icon
     */
    public static void pressPlus(final int d) {
        stopReplayMode();
        plus.pressed(d);
    }

    /**
     * Plus was released.
     * @param d bitmask: key or icon
     */
    public static void releasePlus(final int d) {
        plus.released(d);
    }

    /**
     * Minus was pressed.
     * @param d bitmask: key or icon
     */
    public static void pressMinus(final int d) {
        stopReplayMode();
        minus.pressed(d);
    }

    /**
     * Minus was released.
     * @param d bitmask: key or icon
     */
    public static void releaseMinus(final int d) {
        minus.released(d);
    }

    /**
     * Get deque of all Lemmings under the mouse cursor.
     * @return deque of all Lemmings under the mouse cursor
     */
    public static Deque<Lemming> getLemmsUnderCursor() {
        return lemmsUnderCursor;
    }

    /**
     * Get list of all Lemmings in this level.
     * @return list of all Lemmings in this level
     */
    public static List<Lemming> getLemmings() {
        return lemmings;
    }

    /**
     * Set sound gain.
     * @param g gain (0-2.0)
     */
    public static void setSoundGain(final double g) {
        soundGain = g;
        if (sound != null) {
            sound.setGain(soundGain);
        }
        Core.getProgramProps().setDouble("soundGain", g);
    }
    
    /**
     * Get sound gain.
     * @return sound gain
     */
    public static double getSoundGain() {
        return soundGain;
    }

    /**
     * Set music gain.
     * @param g gain (0-2.0)
     */
    public static void setMusicGain(final double g) {
        musicGain = g;
        if (Music.getType() != null) {
            Music.setGain(musicGain);
        }
        Core.getProgramProps().setDouble("musicGain", g);
    }
    
    /**
     * Get music gain.
     * @return music gain
     */
    public static double getMusicGain() {
        return musicGain;
    }
    
    public static void resetGain() {
        if (sound != null) {
            sound.setGain(soundGain);
        }
        if (Music.getType() != null) {
            Music.setGain(musicGain);
        }
    }
    
    private static void fadeSound() {
        if (sound != null) {
            sound.setGain(sound.getGain() - Fader.getStep() / 255.0 * soundGain * 1.5);
        }
        if (Music.getType() != null) {
            Music.setGain(Music.getGain() - Fader.getStep() / 255.0 * musicGain * 1.5);
        }
    }

    /**
     * Set advanced mouse selection mode.
     * @param sel true: advanced selection mode active, false otherwise
     */
    public static void setAdvancedSelect(final boolean sel) {
        advancedSelect = sel;
    }

    /**
     * Get state of advanced mouse selection mode.
     * @return true if advanced selection mode activated, false otherwise
     */
    public static boolean isAdvancedSelect() {
        return advancedSelect;
    }
    
    public static void setSwapButtons(boolean sb) {
        swapButtons = sb;
    }
    
    public static boolean doSwapButtons() {
        return swapButtons;
    }
    
    public static void setFasterFastForward(boolean faster) {
        fasterFastForward = faster;
    }
    
    public static boolean isFasterFastForward() {
        return fasterFastForward;
    }

    /**
     * Get foreground image of level.
     * @return foreground image of level
     */
    public static Image getFgImage() {
        return fgImage;
    }
    
    /**
     * Get background image of level.
     * @return foreground image of level
     */
    public static List<Image> getBgImages() {
        return bgImages;
    }

    /**
     * Get foreground stencil of level.
     * @return foreground stencil of level
     */
    public static Stencil getStencil() {
        return stencil;
    }

    /**
     * Enable music.
     * @param on true: music on, false otherwise
     */
    public static void setMusicOn(final boolean on) {
        musicOn = on;
    }

    /**
     * Get music enable state.
     * @return true: music is on, false otherwise
     */
    public static boolean isMusicOn() {
        return musicOn;
    }

    /**
     * Enable sound.
     * @param on true: sound on, false otherwise
     */
    public static void setSoundOn(final boolean on) {
        soundOn = on;
    }

    /**
     * Get sound enable state.
     * @return true: sound is on, false otherwise
     */
    public static boolean isSoundOn() {
        return soundOn;
    }

    /**
     * Get small preview image of level.
     * @return small preview image of level
     */
    public static Image getMapPreview() {
        return mapPreview;
    }

    /**
     * Get number of Lemmings to rescue.
     * @return number of Lemmings to rescue
     */
    public static int getNumToRescue() {
        return numToRescue;
    }

    /**
     * Get time left in seconds.
     * @return time left in seconds
     */
    public static int getTime() {
        return time;
    }
    
    public static boolean isTimed() {
        return timed;
    }
    
    public static void setTimed(boolean isTimed) {
        timed = isTimed;
    }
    
    public static int getScore() {
        return numLeft * 100 / numLemmingsMax * 100 + numClimbers + numFloaters + numBombers +
                numBlockers + numBuilders + numBashers + numMiners + numDiggers;
    }
    
    public static int getTimesFailed() {
        return timesFailed;
    }
    
    public static int getWidth() {
        return width;
    }
    
    public static String[] getMods() {
        return mods;
    }
}


/**
 * Trapdoor/Entrance class
 * Trapdoor logic: for numbers >1, just take the next door for each lemming and wrap around to 1 when
 * the last one is reached.
 * Special rule for 3 trapdoors: the order is 1, 2, 3, 2 (loop), not 1, 2, 3 (loop)
 *
 * @author Volker Oth
 */
class TrapDoor {
    /** pattern for three entrances */
    private static final int[] PATTERN3 = {0, 1, 2, 1};

    /** number of entrances */
    private static int entrances;
    /** entrance counter */
    private static int counter;

    /**
     * Reset to new number of entrances.
     * @param e number of entrances
     */
    static void reset(final int e) {
        entrances = e;
        counter = 0;
    }

    /**
     * Get index of next entrance.
     * @return index of next entrance
     */
    static int getNext() {
        int retVal = counter;
        counter++;
        if (entrances != 3) {
            if (counter >= entrances) {
                counter = 0;
            }
            return retVal;
        }
        // special case: 3
        if (counter >= 4) {
            counter = 0;
        }
        return PATTERN3[retVal];
    }
}