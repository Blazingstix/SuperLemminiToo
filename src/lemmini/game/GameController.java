package lemmini.game;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import javax.swing.JOptionPane;
import java.awt.Color;

import lemmini.LemminiFrame;
import lemmini.extract.ExtractDAT;
import lemmini.extract.ExtractLevel;
import lemmini.gameutil.Fader;
import lemmini.gameutil.KeyRepeat;
import lemmini.gameutil.Sprite;
import lemmini.graphics.GraphicsContext;
import lemmini.graphics.LemmImage;
import lemmini.sound.Music;
import lemmini.sound.Sound;
import lemmini.tools.NanosecondTimer;
import lemmini.tools.Props;
import lemmini.tools.ToolBox;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;


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
        /** replay level: fade out, fade in level */
        REPLAY_LEVEL_NO_BRIEFING,
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
    
    public static enum Option {
        /** flag: play music */
        MUSIC_ON,
        /** flag: play sounds */
        SOUND_ON,
        /** flag: use advanced mouse selection methods (i.e. holding left/right)*/
        ADVANCED_SELECT,
        /** flag: use classic mouse cursor behavior */
        CLASSIC_CURSOR,
        SWAP_BUTTONS,
        FASTER_FAST_FORWARD,
        PAUSE_STOPS_FAST_FORWARD,
        NO_PERCENTAGES,
        REPLAY_SCROLL,
        UNPAUSE_ON_ASSIGNMENT
    }
    
    /**
     * Options exclusive to SuperLemminiToo
     * @author Charles
     *
     */
    public static enum SuperLemminiTooOption {
        TIMED_BOMBERS,
        UNLOCK_ALL_LEVELS,
        DISABLE_SCROLL_WHEEL,
        DISABLE_FRAME_STEPPING,
        /** flag: show Visual SFX */
        VISUAL_SFX,
        /** flag: use new status with icons */
        ENHANCED_STATUS,
        /** flag: use new icon bar. Has better spacing, and supports animated icons. */
        ENHANCED_ICONBAR,
        ICON_LABELS
    }
    
    public static enum LevelFormat {
        INI,
        LVL,
        DAT
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
    
    private static final int MAX_START_SOUND_CTR = 28;
    /** open entrance after about 2 seconds */
    private static final int MAX_ENTRANCE_OPEN_CTR = 72;
    /** one second is 33.333 ticks */
    private static final int[] MAX_SECOND_CTR = {34, 33, 33};
    /** one second in superlemming mode is 100 ticks */
    private static final int[] MAX_SUPERLEMMING_SECOND_CTR = {100};
    
    /** nuke icon: maximum time between two mouse clicks for double click detection (in nanoseconds) */
    private static final long NANOSEC_NUKE_DOUBLE_CLICK = 240_000_000;
    /** restart icon: maximum time between two mouse clicks for double click detection (in nanoseconds) */
    private static final long NANOSEC_RESTART_DOUBLE_CLICK = 240_000_000;
    /** +/- icons: maximum time between two mouse clicks for double click detection (in nanoseconds) */
    private static final long NANOSEC_RELEASE_DOUBLE_CLICK = 200_000_000;
    /** +/- icons: time for key repeat to kick in */
    private static final long NANOSEC_KEYREPEAT_START = 250_000_000;
    /** +/- icons: time for key repeat rate */
    private static final long NANOSEC_KEYREPEAT_REPEAT = 67_000_000;
    
    private static final String LEVEL_DIR_REGEX = "levels/[^/]+/levelpack.ini";
    private static final String LEVEL_CACHE_INI = "$levelcache.ini";
    
    /** sound object */
    public static Sound sound;
    
    /** the foreground stencil */
    private static Stencil stencil;
    /** the foreground image */
    private static LemmImage fgImage;
    private static final Set<Option> options = EnumSet.noneOf(Option.class);
    /** SuperLemminiToo Exclusive options*/
    private static final Set<SuperLemminiTooOption> SLToptions = EnumSet.noneOf(SuperLemminiTooOption.class);

    /** flag: fast forward mode is active */
    private static boolean fastForward;
    private static boolean verticalLock;
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
    private static boolean forceAdvanceFrame = false;
    /** frame counter for handling opening of entrances */
    private static int entranceOpenCtr;
    private static int startSoundCtr;
    private static boolean startSoundPlayed;
    private static final Set<SpriteObject> entranceSprites = new HashSet<>();
    /** frame counter for handling time */
    private static int secondCtr;
    /** frame counter used to handle release of new Lemmings */
    private static int releaseCtr;
    /** threshold to release a new Lemming */
    private static int releaseBase;
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
    private static List<String> modPaths;
    /** list of all active Lemmings in the Level */
    private static final List<Lemming> lemmings = new LinkedList<>();
    /** list of all active explosions */
    private static final List<Explosion> explosions = new LinkedList<>();
    /** list of all Lemmings under the mouse cursor */
    private static final Queue<Lemming> lemmsUnderCursor = Collections.asLifoQueue(new ArrayDeque<Lemming>(128));
    /** list of all active Visual SFX */
    private static final List<Vsfx> vsfxs = new LinkedList<>();
    /** array of available level packs */
    private static List<LevelPack> levelPacks;
    private static Set<ExternalLevelEntry> externalLevelList;
    /** small preview version of level used in briefing screen */
    private static LemmImage mapPreview;
    /** timer used for nuking */
    private static NanosecondTimer timerNuke;
    /** timer used for restart icon */
    private static NanosecondTimer timerRestart;
    /** key repeat object for plus key/icon */
    private static KeyRepeat plus;
    /** key repeat object for minus key/icon */
    private static KeyRepeat minus;
    /** Lemming for which skill change is requested */
    private static Lemming lemmSkillRequest;
    /** horizontal scrolling offset for level */
    private static int xPos;
    private static int xPosCenter;
    /** vertical scrolling offset for level */
    private static int yPos;
    private static int yPosCenter;
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
    /** old value of vertical scrolling position */
    private static int yPosOld;
    /** old value of selected skill */
    private static Lemming.Type lemmSkillOld;
    /** flag: replay mode is active */
    private static boolean replayMode;
    /** flag: replay mode should be stopped */
    private static boolean stopReplayMode;
    /** number of Lemmings which exited the level */
    private static int numExited;
    /** release rate */
    private static int releaseRate;
    private static boolean lockReleaseRate;
    /** number of Lemmings available */
    private static int numLemmingsMax;
    /** number of Lemmings who entered the level */
    private static int numLemmingsOut;
    /** number of Lemmings which have to be rescued to finish the level */
    private static int numToRescue;
    /** time left in seconds */
    private static int time;
    private static int timeLimit;
    private static int timeElapsedTillLastExited;
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
    private static int numSkillsUsed;
    /** free running update counter */
    private static int updateCtr;
    /** gain for sound 0-2.0 */
    private static double soundGain = 1.0;
    /** gain for music 0-2.0 */
    private static double musicGain = 1.0;
    private static int width = Level.DEFAULT_WIDTH;
    private static int height = Level.DEFAULT_HEIGHT;
    private static int timesFailed;
    
    /**
     * Initialization.
     * @throws ResourceException
     */
    public static void init() throws ResourceException {
        width = Level.DEFAULT_WIDTH;
        height = Level.DEFAULT_HEIGHT;
        
        fgImage = ToolBox.createLemmImage(width, height);
        
        gameState = State.INIT;
        
        plus  = new KeyRepeat(NANOSEC_KEYREPEAT_START, NANOSEC_KEYREPEAT_REPEAT, NANOSEC_RELEASE_DOUBLE_CLICK);
        minus = new KeyRepeat(NANOSEC_KEYREPEAT_START, NANOSEC_KEYREPEAT_REPEAT, NANOSEC_RELEASE_DOUBLE_CLICK);
        timerNuke = new NanosecondTimer();
        timerRestart = new NanosecondTimer();
        
        // read level packs
        levelPacks = new ArrayList<>(32);
        externalLevelList = new LinkedHashSet<>();
        LevelPack externalLevels = new LevelPack();
        Props externalLevelsINI = new Props();
        modPaths = Collections.emptyList();
        if (externalLevelsINI.load(Core.resourceTree.getPath(Core.EXTERNAL_LEVEL_CACHE_PATH + LEVEL_CACHE_INI))) {
            boolean updateINI = false;
            for (int i = 0; true; i++) {
                String[] levelData = externalLevelsINI.getArray("level_" + i, null);
                if (levelData != null) {
                    if (levelData.length >= 2) {
                        Path lvlPath = Paths.get(levelData[1]);
                        if (Files.isReadable(lvlPath)) {
                            addExternalLevel(lvlPath, externalLevels, false);
                        } else {
                            updateINI = true;
                        }
                    }
                } else {
                    break;
                }
            }
            if (updateINI) {
                saveExternalLevelList();
            }
        }
        levelPacks.add(externalLevels);
        
        // now get the names of the directories
        Set<String> dirs = new TreeSet<>();
        Core.resourceTree.getAllPathsRegex(LEVEL_DIR_REGEX).stream()
                .map(file -> file.getParent().getFileName().toString().toLowerCase(Locale.ROOT))
                .forEach(dirs::add);
        Pattern levelDirPattern = Pattern.compile(LEVEL_DIR_REGEX);
        Core.zipFiles.stream().forEach(zipFile -> {
           zipFile.stream().map(ZipEntry::getName)
                   .filter(entryName -> levelDirPattern.matcher(entryName).matches())
                   .forEach(entryName -> {
                dirs.add(entryName.substring(entryName.indexOf('/') + 1, entryName.lastIndexOf('/')).toLowerCase(Locale.ROOT));
           });
        });
        dirs.stream().sorted().forEachOrdered(lvlName -> {
            try {
                Resource res = Core.findResource("levels/" + lvlName + "/levelpack.ini", false);
                levelPacks.add(new LevelPack(res));
            } catch (ResourceException ex) {
            }
        });
        curRating = 0;
        curLevelPack = 0;
        curLevelNumber = 0;
        modPaths = levelPacks.get(curLevelPack).getModPaths();
        
        sound = new Sound();
        sound.setGain(soundGain);
        Icons.init();
        Explosion.init();
        Lemming.loadLemmings();
        lemmSkillRequest = null;
        
        MiscGfx.init(ToolBox.scale(width, 1.0 / 16.0));
        LemmFont.init();
        TextScreen.init();
        NumFont.init();
        LemmCursor.init();
        Music.init();
        Music.setGain(musicGain);
        
        timesFailed = 0;
        numSkillsUsed = 0;
        
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
        LevelPack lpack = levelPacks.get(lvlPack);
        // calculate absolute level number
        int absLvl = level;
        for (int i = 0; i < rating; i++) {
            absLvl += lpack.getLevelCount(i);
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
        LevelPack lpack = levelPacks.get(lvlPack);
        int ratings = lpack.getRatings().size();
        int lvl = -1;
        int rating = -1;
        for (int i = 0, ls = 0; i < ratings; i++) {
            int lsOld = ls;
            // add number of levels existing in this rating
            ls += lpack.getLevelCount(i);
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
        
        if (num < levelPacks.get(curLevelPack).getLevelCount(curRating)) {
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
        
        if (num < levelPacks.get(curLevelPack).getRatings().size()) {
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
     * Level successfully finished, enter debriefing and enable next level.
     */
    static synchronized void finishLevel() {
        Music.close();
        setFastForward(false);
        setVerticalLock(false);
        setSuperLemming(false);
        
        if (!wasLost() && curLevelPack != 0) {
            LevelPack lvlPack = getCurLevelPack();
            String curRatingString = lvlPack.getRatings().get(curRating);
            Core.player.setLevelRecord(lvlPack.getName(), curRatingString, curLevelNumber, getLevelRecord());
            if (curLevelNumber + 1 < lvlPack.getLevelCount(curRating)) {
                Core.player.setAvailable(lvlPack.getName(), curRatingString, curLevelNumber + 1);
            }
            Core.player.store();
        }
        
        replayMode = false;
        gameState = State.DEBRIEFING;
    }
    
    /**
     * Restart level.
     * @param doReplay true: replay, false: play
     * @param showBriefing
     */
    private static synchronized void restartLevel(final boolean doReplay, final boolean showBriefing) throws LemmException, ResourceException {
        if (!replayMode && wasLost() && (gameState == State.LEVEL
                || gameState == State.LEVEL_END
                || gameState == State.DEBRIEFING)) {
            timesFailed++;
        }
        initLevel(showBriefing);
        if (doReplay) {
            replayMode = true;
            replay.save(Core.TEMP_PATH + "/replay.rpl");
            replay.rewind();
        } else {
            replayMode = false;
            replay.clear();
        }
    }
    
    /**
     * Initialize a level after it was loaded.
     */
    private static synchronized void initLevel(boolean showBriefing) throws LemmException, ResourceException {
        Music.stop();
        
        setFastForward(false);
        setVerticalLock(false);
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
        int oldHeight = height;
        
        numExited = 0;
        releaseRate = level.getReleaseRate();
        lockReleaseRate = level.isReleaseRateLocked();
        numLemmingsMax = level.getNumLemmings();
        numLemmingsOut = 0;
        numToRescue = level.getNumToRescue();
        time = level.getTimeLimitSeconds();
        numClimbers = level.getNumClimbers();
        numFloaters = level.getNumFloaters();
        numBombers = level.getNumBombers();
        numBlockers = level.getNumBlockers();
        numBuilders = level.getNumBuilders();
        numBashers = level.getNumBashers();
        numMiners = level.getNumMiners();
        numDiggers = level.getNumDiggers();
        numSkillsUsed = 0;
        xPosCenter = level.getXPosCenter();
        yPosCenter = level.getYPosCenter();
        width = level.getWidth();
        height = level.getHeight();
        if (time <= 0) {
            timed = false;
            time = 0;
        } else {
            timed = true;
        }
        timeLimit = time;
        timeElapsedTillLastExited = 0;
        
        level.paintLevel();
        stencil = level.getStencil();
        fgImage = level.getFgImage();
        
        if (width != oldWidth || height != oldHeight) {
            MiscGfx.setMinimapWidth(ToolBox.scale(width, 1.0 / 16.0));
        }
        
        TrapDoor.reset(level.getNumEntrances(), level.getEntranceOrder());
        startSoundPlayed = false;
        startSoundCtr = 0;
        entranceOpened = false;
        entranceOpenCtr = 0;
        secondCtr = 0;
        releaseCtr = 0;
        lemmSkill = null;
        
        entranceSprites.clear();
        for (int i = 0; i < level.getNumEntrances(); i++) {
            SpriteObject spr = level.getSprObject(level.getEntrance(i).id);
            if (spr != null) {
                entranceSprites.add(spr);
            }
        }
        
        calcReleaseBase();
        
        int scaleFactorWidth = width / 800;
        if (width % 800 != 0) {
            scaleFactorWidth++;
        }
        int scaleFactorHeight = height / 80;
        if (height % 80 != 0) {
            scaleFactorHeight++;
        }
        int scaleFactor = NumberUtils.max(4, scaleFactorWidth, scaleFactorHeight);
        mapPreview = level.createMinimap(fgImage, 1.0 / scaleFactor, 1.0 / scaleFactor, true, false, true);
        
        setSuperLemming(level.isSuperLemming());
        forceNormalTimerSpeed = level.getForceNormalTimerSpeed();
        
        replayFrame = 0;
        stopReplayMode = false;
        releaseRateOld = releaseRate;
        lemmSkillOld = lemmSkill;
        nukeOld = false;
        
        String music = level.getMusic();
        try {
            if (music == null) {
                music = levelPacks.get(curLevelPack).getInfo(curRating, curLevelNumber).getMusic();
            }
            if (music == null) {
                music = Music.getRandomTrack(level.getStyleName(), level.getSpecialStyleName());
            }
            Music.load("music/" + music);
        } catch (ResourceException ex) {
            Core.resourceError(ex.getMessage());
        } catch (LemmException ex) {
        	if (music==null) {
            	music="";
            }
        	//get the "real" file, from the requested resource:
        	Resource res = Core.findResource("music/" + music, true, Core.MUSIC_EXTENSIONS);
        	String ext = FilenameUtils.getExtension(res.getFileName()).toLowerCase(Locale.ROOT);
            //only show the error if it's not an .ogg file
        	// .ogg files not playing properly is the result of missing dependencies.
        	if(!ext.equals("ogg")) {
            	JOptionPane.showMessageDialog(null, "Unable to load music resource:\n" + ex.getMessage() + "\n\nAttempting midi fallback.", "Error Loading Music", JOptionPane.ERROR_MESSAGE);
            }

            //TODO: Clean this up
            //Ugh, this feels incredibly hacky... 
            //but this is our fallback (and we must error catch here too, in case disaster strikes)
            try {
                music = Music.getRandomTrack(level.getStyleName(), level.getSpecialStyleName());
                Music.load("music/" + music);
            } catch (ResourceException ex2) {
                Core.resourceError(ex2.getMessage());
            } catch (LemmException ex2) {
                JOptionPane.showMessageDialog(null, "Unable to load music resource:\n" + ex2.getMessage() + "\n\nNo music will play for this level.", "Error Loading Music", JOptionPane.ERROR_MESSAGE);
            }
        }
        
        sound.setGain(soundGain);
        Music.setGain(musicGain);
        
        if (showBriefing) {
            gameState = State.BRIEFING;
        } else {
            gameState = State.LEVEL;
            transitionState = TransitionState.TO_LEVEL;
        }
        
        wasCheated = isCheat();
    }
    
    /**
     * Request the restart of this level.
     * @param doReplay
     * @param showBriefing
     */
    public static synchronized void requestRestartLevel(final boolean doReplay, final boolean showBriefing) {
        if (doReplay && !replayMode) {
            replay.addEndEvent(replayFrame);
        }
        if (doReplay || replayMode) {
            if (showBriefing) {
                transitionState = TransitionState.REPLAY_LEVEL;
            } else {
                transitionState = TransitionState.REPLAY_LEVEL_NO_BRIEFING;
            }
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
    private static synchronized Level changeLevel(final int lPack, final int rating, final int lNum, final boolean doReplay) throws LemmException, ResourceException {
        timesFailed = 0;
        
        curLevelPack = lPack;
        curRating = rating;
        curLevelNumber = lNum;
        
        List<String> oldMods = modPaths;
        modPaths = levelPacks.get(curLevelPack).getModPaths();
        if (!modPaths.equals(oldMods)) {
            sound.load();
            MiscGfx.init(ToolBox.scale(width, 1.0 / 16.0));
            Icons.init();
            Explosion.init();
            LemmFont.init();
            TextScreen.init();
            NumFont.init();
            LemmCursor.init();
            Lemming.loadLemmings();
        }
        
        Resource lvlRes = levelPacks.get(curLevelPack).getInfo(curRating, curLevelNumber).getLevelResource();
        // loading the level will patch appropriate lemmings pixels to the correct colors
        level = new Level(lvlRes, level);
        
        initLevel(true);
        
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
        return gameState == State.LEVEL || numExited < numToRescue;
    }
    
    /**
     * Get current replay image.
     * @return current replay image
     */
    public static synchronized LemmImage getReplayImage() {
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
        if (lemmSkill != null && !type.isWalkerOnly()) {
            for (Lemming l : lemmsUnderCursor) {
                if (type.isLeftOnly() && l.getDirection() != Lemming.Direction.LEFT) {
                    continue;
                }
                if (type.isRightOnly() && l.getDirection() != Lemming.Direction.RIGHT) {
                    continue;
                }
                switch (l.getSkill()) {
                    case BLOCKER:
                        if (l.getSkill() != lemmSkill && !l.getName().isEmpty()) {
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
                        if (l.getSkill() != lemmSkill && !l.getName().isEmpty()) {
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
            if (type.isWalkerOnly() && l.getSkill() != Lemming.Type.WALKER) {
                continue;
            }
            if (type.isLeftOnly() && l.getDirection() != Lemming.Direction.LEFT) {
                continue;
            }
            if (type.isRightOnly() && l.getDirection() != Lemming.Direction.RIGHT) {
                continue;
            }
            switch (l.getSkill()) {
                case WALKER:
                    if (l.getSkill() != lemmSkill && !l.getName().isEmpty()) {
                        if (lemmSkill == null) {
                            return l;
                        } else {
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
                    }
                    break;
                case FALLER:
                case CLIMBER:
                case FLIPPER:
                case FLOATER:
                case JUMPER:
                    if (lemmSkill != null && l.getSkill() != lemmSkill && !l.getName().isEmpty()) {
                        if (lemmSkill == null) {
                            return l;
                        } else {
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
                                    break;
                            }
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
     * Lemming has exited the Level.
     */
    static synchronized void increaseExited() {
        numExited++;
        timeElapsedTillLastExited = time;
    }
    
    /**
     * Stop replay.
     */
    public static void stopReplayMode() {
        if (replayMode) {
            replay.clearFrom(replayFrame);
            replayMode = false;
            stopReplayMode = false;
        }
    }
    
    /**
     * Return time as String "minutes-seconds"
     * @return time as String "minutes-seconds"
     */
    public static synchronized String getTimeString() {
        return String.format(Locale.ROOT, "%d-%02d", time / 60, time % 60);
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
            if (!lockReleaseRate && releaseRate < level.getMaxReleaseRate()) {
                if (fired == KeyRepeat.Event.DOUBLE_CLICK) {
                    releaseRate = level.getMaxReleaseRate();
                } else {
                    releaseRate += 1;
                }
                calcReleaseBase();
                sound.playPitched(Sound.PitchedEffect.RELEASE_RATE, releaseRate + 99);
            } else {
                sound.play(Sound.Effect.INVALID);
            }
        }
        
        fired = minus.fired();
        if (fired != KeyRepeat.Event.NONE) {
            if (!lockReleaseRate && releaseRate > level.getReleaseRate()) {
                if (fired == KeyRepeat.Event.DOUBLE_CLICK) {
                    releaseRate = level.getReleaseRate();
                } else {
                    releaseRate -= 1;
                }
                calcReleaseBase();
                sound.playPitched(Sound.PitchedEffect.RELEASE_RATE, releaseRate + 99);
            } else {
                sound.play(Sound.Effect.INVALID);
            }
        }
        
        
        if (forceAdvanceFrame) {
            forceAdvanceFrame = false;
        } else if (isPaused()) {
            return;
        }
        
        // test for end of replay mode
        if (replayMode && stopReplayMode) {
            stopReplayMode();
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
                // replay: position changed?
                if (getXPos() != xPosOld || getYPos() != yPosOld) {
                    replay.addPosEvent(replayFrame, getXPos() + Core.getDrawWidth() / 2, getYPos() + LemminiFrame.LEVEL_HEIGHT / 2, 0);
                    xPosOld = getXPos();
                    yPosOld = getYPos();
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
                        Lemming l = lemmings.get(rs.lemming);
                        l.setSkill(rs.skill, false);
                        l.setSelected();
                        pan = l.getPan();
                        switch (rs.skill) {
                            case CLIMBER:
                                numClimbers--;
                                numSkillsUsed++;
                                break;
                            case FLOATER:
                                numFloaters--;
                                numSkillsUsed++;
                                break;
                            case FLAPPER:
                                numBombers--;
                                numSkillsUsed++;
                                break;
                            case BLOCKER:
                                numBlockers--;
                                numSkillsUsed++;
                                break;
                            case BUILDER:
                                numBuilders--;
                                numSkillsUsed++;
                                break;
                            case BASHER:
                                numBashers--;
                                numSkillsUsed++;
                                break;
                            case MINER:
                                numMiners--;
                                numSkillsUsed++;
                                break;
                            case DIGGER:
                                numDiggers--;
                                numSkillsUsed++;
                                break;
                            default:
                                break;
                        }
                        sound.play(Sound.Effect.SELECT_SKILL, pan);
                        break;
                    }
                    case ReplayStream.SET_RELEASE_RATE:
                        if (!lockReleaseRate) {
                            ReplayReleaseRateEvent rr = (ReplayReleaseRateEvent) r;
                            releaseRate = rr.releaseRate;
                            calcReleaseBase();
                            sound.playPitched(Sound.PitchedEffect.RELEASE_RATE, releaseRate + 99);
                            releaseRateOld = releaseRate;
                        }
                        break;
                    case ReplayStream.NUKE:
                        nuke();
                        pressIcon(Icons.IconType.NUKE);
                        nukeOld = nuke;
                        break;
                    case ReplayStream.MOVE_POS: {
                        ReplayMovePosEvent rx = (ReplayMovePosEvent) r;
                        if (isOptionEnabled(Option.REPLAY_SCROLL) && rx.player == 0) {
                            setXPos(rx.xPos - Core.getDrawWidth() / 2);
                            setYPos(rx.yPos - LemminiFrame.LEVEL_HEIGHT / 2);
                            xPosOld = xPos;
                            yPosOld = yPos;
                        }
                        break;
                    }
                    case ReplayStream.SELECT_SKILL: {
                        ReplaySelectSkillEvent rs = (ReplaySelectSkillEvent) r;
                        if (rs.player == 0) {
                            lemmSkill = rs.skill;
                            switch (lemmSkill) {
                                case CLIMBER:
                                    pressIcon(Icons.IconType.CLIMB);
                                    break;
                                case FLOATER:
                                    pressIcon(Icons.IconType.FLOAT);
                                    break;
                                case FLAPPER:
                                    pressIcon(Icons.IconType.BOMB);
                                    break;
                                case BLOCKER:
                                    pressIcon(Icons.IconType.BLOCK);
                                    break;
                                case BUILDER:
                                    pressIcon(Icons.IconType.BUILD);
                                    break;
                                case BASHER:
                                    pressIcon(Icons.IconType.BASH);
                                    break;
                                case MINER:
                                    pressIcon(Icons.IconType.MINE);
                                    break;
                                case DIGGER:
                                    pressIcon(Icons.IconType.DIG);
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
        if (entranceOpened && !nukeTemp /*&& !isPaused()*/ && numLemmingsOut < getNumLemmingsMax() && --releaseCtr <= 0) {
            releaseCtr = releaseBase;
            if (level.getNumEntrances() != 0) {
                Level.Entrance e = level.getEntrance(TrapDoor.getNext());
                Lemming l = new Lemming(e.xPos + 2, e.yPos + Lemming.HEIGHT, e.leftEntrance ? Lemming.Direction.LEFT : Lemming.Direction.RIGHT);
                lemmings.add(l);
                numLemmingsOut++;
            }
        }
        // nuking
        if (nukeTemp && ((updateCtr & 1) == 1)) {
            for (Lemming l : lemmings) {
                if (!l.nuke() && !l.hasDied() && !l.hasExited()) {
                    l.setSkill(Lemming.Type.NUKE, false);
                    //System.out.println("nuked!");
                    break;
                }
            }
        }
        
        if (!startSoundPlayed) {
            if (++startSoundCtr == MAX_START_SOUND_CTR) {
                //show the Let's Go graphic several times
                for( SpriteObject spr : entranceSprites) {
                	//display the graphic right below the opening sprite.
                	int y = spr.getY() + spr.getHeight() + (Vsfx.IMG_HEIGHT/2);
                	sound.playVisualSFXSilent(Sound.Effect.START, spr.midX(), y);
                }
                //play the actual sfx only once
            	sound.play(Sound.Effect.START);
                startSoundPlayed = true;
            }
        }
        // open trap doors?
        if (!entranceOpened) {
            if (++entranceOpenCtr == MAX_ENTRANCE_OPEN_CTR) {
                for (int i = 0; i < level.getNumSprObjects(); i++) {
                    SpriteObject spr = level.getSprObject(i);
                    if (spr != null && spr.getAnimMode() == Sprite.Animation.ONCE_ENTRANCE) {
                        spr.setAnimMode(Sprite.Animation.ONCE);
                    }
                }
                level.openBackgroundEntrances();
                //this is the *creak* sound of the doors opening.
                //for now let's not play the "creak" sound as the entrance doors open.
                //entranceSprites.stream().forEach(sound::playVisualSFX);
                entranceSprites.stream().forEach(sound::play);
            } else if (entranceOpenCtr == MAX_ENTRANCE_OPEN_CTR + 30) {
                //System.out.println("opened");
                entranceOpened = true;
                releaseCtr = 0; // first lemming to enter at once
                if (isOptionEnabled(Option.MUSIC_ON)) {
                    Music.play();
                }
            }
        }
        // end of game conditions
        if ((nukeTemp || numLemmingsOut == getNumLemmingsMax()) && lemmings.isEmpty()) {
            // End the level only if no objects are triggered.
            boolean endLevel = true;
            for (int i = 0; i < level.getNumSprObjects(); i++) {
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
        
        //animate or remove Lemmings
        for (Iterator<Lemming> it = lemmings.iterator(); it.hasNext(); ) {
            Lemming l = it.next();
            l.animate();
            if (l.hasDied() || l.hasExited()) {
                it.remove();
            }
        }
        
        //animate or remove Explosions
        for (Iterator<Explosion> it = explosions.iterator(); it.hasNext(); ) {
            Explosion e = it.next();
            if (e.isFinished()) {
                it.remove();
            } else {
                e.update();
            }
        }

        //animate or remove visual sfx
        for (Iterator<Vsfx> it = vsfxs.iterator(); it.hasNext(); ) {
        	Vsfx l = it.next();
            l.animate();
            if (l.hasFinished()) {
                it.remove();
            }
        }
        
        
        // animate level objects
        for (int n = 0; n < level.getNumSprObjects(); n++) {
            SpriteObject spr = level.getSprObject(n);
            if (spr != null) {
                spr.getImageAnim(); // just to animate
            }
        }
        level.advanceBackgroundFrame();
        
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
        if (lemmSkill != null) {
            lemmSkillRequest = lemm;
        }
        stopReplayMode();
        if (!isOptionEnabled(SuperLemminiTooOption.DISABLE_FRAME_STEPPING)) {
        	advanceFrame();
        }
    }
    
    /**
     * Assign the selected skill to the selected Lemming.
     * @param delete flag: reset the current skill request
     */
    private static synchronized void assignSkill(final boolean delete) {
        if (lemmSkillRequest == null || lemmSkill == null) {
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
                            if (numClimbers != Integer.MAX_VALUE) {
                                numClimbers--;
                                numSkillsUsed++;
                            }
                            canSet = true;
                        }
                    } else {
                        sound.play(Sound.Effect.INVALID, lemm.getPan());
                    }
                    break;
                case FLOATER:
                    if (numFloaters > 0) {
                        if (lemm.setSkill(lemmSkill, true)) {
                            if (numFloaters != Integer.MAX_VALUE) {
                                numFloaters--;
                                numSkillsUsed++;
                            }
                            canSet = true;
                        }
                    } else {
                        sound.play(Sound.Effect.INVALID, lemm.getPan());
                    }
                    break;
                case FLAPPER:
                    if (numBombers > 0) {
                        if (lemm.setSkill(lemmSkill, true)) {
                            if (numBombers != Integer.MAX_VALUE) {
                                numBombers--;
                                numSkillsUsed++;
                            }
                            canSet = true;
                        }
                    } else {
                        sound.play(Sound.Effect.INVALID, lemm.getPan());
                    }
                    break;
                case BLOCKER:
                    if (numBlockers > 0) {
                        if (lemm.setSkill(lemmSkill, true)) {
                            if (numBlockers != Integer.MAX_VALUE) {
                                numBlockers--;
                                numSkillsUsed++;
                            }
                            canSet = true;
                        }
                    } else {
                        sound.play(Sound.Effect.INVALID, lemm.getPan());
                    }
                    break;
                case BUILDER:
                    if (numBuilders > 0) {
                        if (lemm.setSkill(lemmSkill, true)) {
                            if (numBuilders != Integer.MAX_VALUE) {
                                numBuilders--;
                                numSkillsUsed++;
                            }
                            canSet = true;
                        }
                    } else {
                        sound.play(Sound.Effect.INVALID, lemm.getPan());
                    }
                    break;
                case BASHER:
                    if (numBashers > 0) {
                        if (lemm.setSkill(lemmSkill, true)) {
                            if (numBashers != Integer.MAX_VALUE) {
                                numBashers--;
                                numSkillsUsed++;
                            }
                            canSet = true;
                        }
                    } else {
                        sound.play(Sound.Effect.INVALID, lemm.getPan());
                    }
                    break;
                case MINER:
                    if (numMiners > 0) {
                        if (lemm.setSkill(lemmSkill, true)) {
                            if (numMiners != Integer.MAX_VALUE) {
                                numMiners--;
                                numSkillsUsed++;
                            }
                            canSet = true;
                        }
                    } else {
                        sound.play(Sound.Effect.INVALID, lemm.getPan());
                    }
                    break;
                case DIGGER:
                    if (numDiggers > 0) {
                        if (lemm.setSkill(lemmSkill, true)) {
                            if (numDiggers != Integer.MAX_VALUE) {
                                numDiggers--;
                                numSkillsUsed++;
                            }
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
            if (isPaused() && isOptionEnabled(Option.UNPAUSE_ON_ASSIGNMENT)) {
                setPaused(false);
                pressIcon(Icons.IconType.PAUSE);
            }
            // add to replay stream
            if (!wasCheated) {
                int idx = lemmings.indexOf(lemm);
                if (idx != StringUtils.INDEX_NOT_FOUND) {
                    // if 2nd try (delete == true) assign to next frame
                    replay.addAssignSkillEvent(replayFrame + ((delete) ? 1 : 0), lemmSkill, idx);
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
        // the original formula is: release lemming every (107-releaseRate)/2 time steps
        // where one step is 60ms (3s/50) or 66ms (4s/60).
        // Lemmini runs at 30ms/33ms, so the term has to be multiplied by 2
        // 107-releaseRate should be correct
        releaseBase = 107 - releaseRate;
    }
    
    /**
     * Handle pressing of an icon button.
     * @param type icon type
     */
    public static synchronized void handleIconButton(final Icons.IconType type) {
        //Lemming.Type lemmSkillOld = lemmSkill;
        if (testIcon(type)) {
            switch (type) {
                case PLUS:
                    plus.pressed(KEYREPEAT_ICON);
                    stopReplayMode();
                    break;
                case MINUS:
                    minus.pressed(KEYREPEAT_ICON);
                    stopReplayMode();
                    break;
                case CLIMB:
                    lemmSkill = Lemming.Type.CLIMBER;
                    stopReplayMode();
                    break;
                case FLOAT:
                    lemmSkill = Lemming.Type.FLOATER;
                    stopReplayMode();
                    break;
                case BOMB:
                    lemmSkill = Lemming.Type.FLAPPER;
                    stopReplayMode();
                    break;
                case BLOCK:
                    lemmSkill = Lemming.Type.BLOCKER;
                    stopReplayMode();
                    break;
                case BUILD:
                    lemmSkill = Lemming.Type.BUILDER;
                    stopReplayMode();
                    break;
                case BASH:
                    lemmSkill = Lemming.Type.BASHER;
                    stopReplayMode();
                    break;
                case MINE:
                    lemmSkill = Lemming.Type.MINER;
                    stopReplayMode();
                    break;
                case DIG:
                    lemmSkill = Lemming.Type.DIGGER;
                    stopReplayMode();
                    break;
                case PAUSE:
                    if (isOptionEnabled(Option.PAUSE_STOPS_FAST_FORWARD) && !paused && fastForward) {
                        fastForward = false;
                        pressIcon(Icons.IconType.FFWD);
                    }
                    setPaused(!isPaused());
                    break;
                case NUKE:
                    stopReplayMode();
                    if (timerNuke.delta() < NANOSEC_NUKE_DOUBLE_CLICK) {
                        if (!nuke) {
                            nuke();
                        }
                    } else {
                        timerNuke.deltaUpdate();
                    }
                    break;
                case FFWD:
                    setFastForward(!isFastForward());
                    break;
                case VLOCK:
                    setVerticalLock(!isVerticalLock());
                    break;
                case RESTART:
                    if (timerRestart.delta() < NANOSEC_RESTART_DOUBLE_CLICK) {
                        requestRestartLevel(true, false);
                    } else {
                        timerRestart.deltaUpdate();
                    }
                    break;
                default:
                    break;
            }
            switch (type) {
                case CLIMB:
                case FLOAT:
                case BOMB:
                case BLOCK:
                case BUILD:
                case BASH:
                case MINE:
                case DIG:
                case PAUSE:
                case NUKE:
                case FFWD:
                case VLOCK:
                case RESTART:
                    sound.playPitched(Sound.PitchedEffect.SKILL, Icons.GetPitch(type));
                    break;
                default:
                    break; // supress sound
            }
            pressIcon(type);
        } else {
            sound.play(Sound.Effect.INVALID);
        }
    }
   
    /**
     * Selects the next skill.
     */
    public static synchronized void nextSkill() {
        Icons.IconType pressedIcon = Icons.getPressedIcon();
        Icons.IconType testIcon;
        if (pressedIcon == null) {
            testIcon = Icons.SkillIconOrder().get(0);
        } else {
            testIcon = Icons.getNextRadioIcon(pressedIcon);
        }
        for ( ; testIcon != pressedIcon; testIcon = Icons.getNextRadioIcon(testIcon)) {
            if (testIcon(testIcon)) {
                break;
            }
        }
        handleIconButton(testIcon);
    }
    
    /**
     * Selects the previous skill.
     */
    public static synchronized void previousSkill() {
        Icons.IconType pressedIcon = Icons.getPressedIcon();
        Icons.IconType testIcon;
        if (pressedIcon == null) {
            testIcon = Icons.SkillIconOrder().get(Icons.SkillIconOrder().size()-1);
        } else {
            testIcon = Icons.getPreviousRadioIcon(pressedIcon);
        }
        for ( ; testIcon != pressedIcon; testIcon = Icons.getPreviousRadioIcon(testIcon)) {
            if (testIcon(testIcon)) {
                break;
            }
        }
        handleIconButton(testIcon);
    }
    
    /**
     * Checks whether the given icon button can be pressed.
     * @param type icon type
     * @return true if the icon button can be pressed, false otherwise
     */
    private static boolean testIcon(final Icons.IconType type) {
        switch (type) {
            case PLUS:
            case MINUS:
            case PAUSE:
            case FFWD:
            case VLOCK:
            case RESTART:
                return true;
            case CLIMB:
                return (isCheat() || numClimbers > 0) && Icons.getPressedIcon() != type;
            case FLOAT:
                return (isCheat() || numFloaters > 0) && Icons.getPressedIcon() != type;
            case BOMB:
                return (isCheat() || numBombers > 0) && Icons.getPressedIcon() != type;
            case BLOCK:
                return (isCheat() || numBlockers > 0) && Icons.getPressedIcon() != type;
            case BUILD:
                return (isCheat() || numBuilders > 0) && Icons.getPressedIcon() != type;
            case BASH:
                return (isCheat() || numBashers > 0) && Icons.getPressedIcon() != type;
            case MINE:
                return (isCheat() || numMiners > 0) && Icons.getPressedIcon() != type;
            case DIG:
                return (isCheat() || numDiggers > 0) && Icons.getPressedIcon() != type;
            case NUKE:
                return !nuke;
            default:
                return false;
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
     */
    public static void fade() {
        if (Fader.getState() == Fader.State.BLACK && transitionState != TransitionState.NONE) {
            switch (transitionState) {
                case END_LEVEL:
                    finishLevel();
                    LemmCursor.setBox(false);
                    LemminiFrame.getFrame().setCursor(LemmCursor.CursorType.NORMAL);
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
                case REPLAY_LEVEL_NO_BRIEFING:
                    setXPos(xPosCenter - Core.getDrawWidth() / 2);
                    setYPos(yPosCenter - LemminiFrame.LEVEL_HEIGHT / 2);
                    xPosOld = xPos;
                    yPosOld = yPos;
                    gameState = State.LEVEL;
                    if (transitionState != TransitionState.REPLAY_LEVEL_NO_BRIEFING) {
                        break;
                    }
                    /* falls through */
                case RESTART_LEVEL:
                case REPLAY_LEVEL:
                    try {
                        boolean doReplay = transitionState == TransitionState.REPLAY_LEVEL
                                || transitionState == TransitionState.REPLAY_LEVEL_NO_BRIEFING;
                        boolean showBriefing = transitionState != TransitionState.REPLAY_LEVEL_NO_BRIEFING;
                        restartLevel(doReplay, showBriefing);
                    } catch (ResourceException ex) {
                        Core.resourceError(ex.getMessage());
                    } catch (LemmException ex) {
                        JOptionPane.showMessageDialog(LemminiFrame.getFrame(), ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        System.exit(1);
                    }
                    LemmCursor.setBox(false);
                    LemminiFrame.getFrame().setCursor(LemmCursor.CursorType.NORMAL);
                    break;
                case LOAD_LEVEL:
                case LOAD_REPLAY:
                    try {
                        changeLevel(nextLevelPack, nextRating, nextLevelNumber, transitionState == TransitionState.LOAD_REPLAY);
                    } catch (ResourceException ex) {
                        Core.resourceError(ex.getMessage());
                    } catch (LemmException ex) {
                        JOptionPane.showMessageDialog(LemminiFrame.getFrame(), ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        System.exit(1);
                    }
                    setTitle();
                    LemminiFrame.getFrame().setCursor(LemmCursor.CursorType.NORMAL);
                    break;
                default:
                    break;
            }
            if (transitionState == TransitionState.TO_LEVEL) {
                Fader.setStep(Fader.FADE_STEP_SLOW);
            } else {
                Fader.setStep(Fader.FADE_STEP_FAST);
            }
            Fader.setState(Fader.State.IN);
            transitionState = TransitionState.NONE;
        } else {
            Fader.fade();
        }
        if (gameState == State.LEVEL_END
                || (gameState == State.LEVEL && transitionState != TransitionState.NONE)) {
            fadeSound();
        }
    }
    
    private static void setTitle() {
        int numLemmings = level.getNumLemmings();
        String lemmingWord = (numLemmings == 1) ? "Lemming" : "Lemmings";
        if (isOptionEnabled(Option.NO_PERCENTAGES) || numLemmings > 100) {
            Core.setTitle(String.format("SuperLemminiToo - %s - Save %d of %d %s",
                    level.getLevelName().trim(),
                    level.getNumToRescue(),
                    numLemmings,
                    lemmingWord));
        } else {
            Core.setTitle(String.format("SuperLemminiToo - %s - Save %d%% of %d %s",
                    level.getLevelName().trim(),
                    level.getNumToRescue() * 100 / numLemmings,
                    numLemmings,
                    lemmingWord));
        }
    }
    
    /**
     * Draw the explosions
     * @param g graphics object
     * @param width width of screen in pixels
     * @param height height of screen in pixels
     * @param xOfs horizontal level offset in pixels
     * @param yOfs vertical level offset in pixels
     */
    public static synchronized void drawExplosions(final GraphicsContext g,
            final int width, final int height, final int xOfs, final int yOfs) {
        explosions.stream().forEachOrdered(e -> e.draw(g, width, height, xOfs, yOfs));
    }
    
    /**
     * Add a new explosion.
     * @param x x coordinate in pixels.
     * @param y y coordinate in pixels.
     */
    public static synchronized void addExplosion(final int x, final int y) {
        // create particle explosion
        explosions.add(new Explosion(x, y));
    }
    
    public static synchronized void drawVisualSfx(final GraphicsContext g) {
    	//draw the visual sfx
    	vsfxs.stream().forEachOrdered(v -> {
    		int vx = v.screenX();
    		int vy = v.screenY();
    		if (vx + v.width() > xPos && vx < xPos + Core.getDrawWidth()
            && vy + v.height() > yPos && vy < yPos + LemminiFrame.LEVEL_HEIGHT) {
		        g.drawImage(v.getImage(), vx - xPos, vy - yPos);
		    }
    	});
    }
    
    public static synchronized void drawLemmings(final GraphicsContext g) {
        lemmings.stream().forEachOrdered(l -> {
            //draw lemming.
        	int lx = l.screenX();
            int ly = l.screenY();
            int mx = l.midX(); //used for countdown graphics, and selection indicators furing replay (lightbulbs)
            if (lx + l.width() > xPos && lx < xPos + Core.getDrawWidth()
                    && ly + l.height() > yPos && ly < yPos + LemminiFrame.LEVEL_HEIGHT) {
                g.drawImage(l.getImage(), lx - xPos, ly - yPos);
            }
            
            //draws any countdown graphics if necessary
            LemmImage cd = l.getCountdown();
            if (cd != null) {
                int x = mx - xPos - cd.getWidth() / 2;
                int y = ly - yPos - cd.getHeight();
                if (x + cd.getHeight() > 0 && x < Core.getDrawWidth()
                        && y + cd.getHeight() > 0 && y < LemminiFrame.LEVEL_HEIGHT) {
                    g.drawImage(cd, x, y);
                }
            }
            
            //draws any selection indicators in replays
            LemmImage sel = l.getSelectImg();
            if (sel != null) {
                int x = mx - xPos - sel.getWidth() / 2;
                int y = ly - yPos - sel.getHeight();
                if (x + sel.getHeight() > 0 && x < Core.getDrawWidth()
                        && y + sel.getHeight() > 0 && y < LemminiFrame.LEVEL_HEIGHT) {
                    g.drawImage(sel, x, y);
                }
            }
        });
    }
    
    public static synchronized void drawMinimapLemmings(final GraphicsContext g, final int x, final int y) {
        lemmings.stream().forEachOrdered(l -> {
            int lx = l.footX();
            int ly = l.footY();
            // draw pixel in minimap
            Minimap.drawLemming(g, x, y, lx, ly);
        });
    }

    /**
     * Draw the original SuperLemmini icon bar, and accompanying skill/release rate values
     * @param g graphics object
     * @param x x coordinate in pixels
     * @param y y coordinate in pixels
     */
    public static void drawIconsAndCounters(final GraphicsContext g, final int iconsX, final int iconsY, final int countersX, final int countersY) {
    	drawIcons(g, iconsX, iconsY);
    	drawCounters(g, countersX, countersY);
/*
    	if (!GameController.isOptionEnabled(GameController.SuperLemminiTooOption.ENHANCED_ICONBAR)) {
        	//the enhanced icon bar should have the counters built into it.
        	drawCounters(g, countersX, countersY);
        }
        */
    }
    
    /**
     * Draw icon bar.
     * @param g graphics object
     * @param x x coordinate in pixels
     * @param y y coordinate in pixels
     */
    private static void drawIcons(final GraphicsContext g, final int x, final int y) {
        //System.out.println("drawing IconBar: " + x + "/" + y);
    	g.drawImage(Icons.getImg(), x, y);
    }
    
    /**
     * Draw the skill/release rate values
     * @param g graphics object
     * @param x x offset in pixels
     * @param y y offset in pixels
     */
    private static void drawCounters(final GraphicsContext g, final int x, final int y) {
        // draw counters
        Integer val = NumberUtils.INTEGER_ZERO;
        //TODO: match these up with wherever the actual 
        List<Icons.IconType> iconOrder = Icons.CurrentIconOrder();
        for (int i = 0; i < iconOrder.size(); i++) {
        	Icons.IconType type = iconOrder.get(i);
        	switch (type) {
        		case MINUS:
                    val = level.getReleaseRate();
                    break;
        		case PLUS:
                    val = lockReleaseRate ? null : releaseRate;
                    break;
        		case CLIMB:
                    val = numClimbers;
                    break;
        		case FLOAT:
                    val = numFloaters;
                    break;
        		case BOMB:
                    val = numBombers;
                    break;
        		case BLOCK:
                    val = numBlockers;
                    break;
        		case BUILD:
                    val = numBuilders;
                    break;
        		case BASH:
                    val = numBashers;
                    break;
        		case MINE:
                    val = numMiners;
                    break;
        		case DIG:
                    val = numDiggers;
                    break;
        		default:
        			val = 0;
        			break;
        	}
            if (val != null) {
                if ((type != Icons.IconType.MINUS || type != Icons.IconType.PLUS) && val.compareTo(NumberUtils.INTEGER_ZERO) <= 0) {
                    //don't show any numbers for skills that are below (or equal to) 0.
                	continue;
                }
            }
            LemmImage numImage = NumFont.numImage(val);
            int centerX = x + Icons.getIconWidth() * i + Icons.getIconWidth() / 2;
            g.setColor(Color.BLACK);
            g.fillRect(centerX-8, y, 16, 11);
            g.fillRect(centerX-9, y+1, 18, 9);
            g.drawImage(numImage, centerX - numImage.getWidth() / 2, y);
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
        return levelPacks.get(curLevelPack);
    }
    
    /**
     * get number of level packs
     * @return number of level packs
     */
    public static int getLevelPackCount() {
        return levelPacks.size();
    }
    
    /**
     * Get level pack via index.
     * @param i index of level pack
     * @return LevelPack
     */
    public static LevelPack getLevelPack(final int i) {
        return levelPacks.get(i);
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
    
    public static int[] addExternalLevel(Path lvlPath, LevelPack lp, boolean showErrors) {
        if (lp == null) {
            lp = levelPacks.get(0);
        }
        if (lvlPath != null) {
            try {
                String fNameStr = lvlPath.getFileName().toString();
                String fNameStrNoExt = FilenameUtils.removeExtension(fNameStr);
                try {
                    LevelFormat format = LevelFormat.valueOf(FilenameUtils.getExtension(fNameStr).toUpperCase(Locale.ROOT));
                    ExternalLevelEntry entry = new ExternalLevelEntry(format, lvlPath);
                    if (externalLevelList.contains(entry)) {
                        switch (format) {
                            case DAT:
                                List<String> ratings = lp.getRatings();
                                for (ListIterator<String> lit = ratings.listIterator(1); lit.hasNext(); ) {
                                    int i = lit.nextIndex();
                                    if (lit.next().toLowerCase(Locale.ROOT).equals(fNameStrNoExt.toLowerCase(Locale.ROOT))) {
                                        return new int[]{0, i, 0};
                                    }
                                }
                                break;
                            case LVL:
                            case INI:
                                int numLevels = lp.getLevelCount(0);
                                for (int i = 0; i < numLevels; i++) {
                                    if (FilenameUtils.removeExtension(lp.getInfo(0, i).getLevelResource().getFileName().toLowerCase(Locale.ROOT))
                                            .equals(fNameStrNoExt.toLowerCase(Locale.ROOT))) {
                                        return new int[]{0, 0, i};
                                    }
                                }
                                break;
                            default:
                                break;
                        }
                    } else {
                        switch (format) {
                            case DAT:
                                List<byte[]> levels = ExtractDAT.decompress(lvlPath);
                                if (levels.isEmpty()) {
                                    if (showErrors) {
                                        JOptionPane.showMessageDialog(LemminiFrame.getFrame(), "DAT file is empty.", "Load Level", JOptionPane.ERROR_MESSAGE);
                                    }
                                    return null;
                                }
                                List<LevelInfo> liList = new ArrayList<>(levels.size());
                                for (ListIterator<byte[]> lit = levels.listIterator(); lit.hasNext(); ) {
                                    int i = lit.nextIndex();
                                    String outName = Core.EXTERNAL_LEVEL_CACHE_PATH + fNameStrNoExt + "_" + i + ".ini";
                                    try (Writer w = Core.resourceTree.newBufferedWriter(outName)) {
                                        ExtractLevel.convertLevel(lit.next(),
                                                fNameStr.toLowerCase(Locale.ROOT) + " (section " + i + ")", w, false, false);
                                    }
                                    LevelInfo li = new LevelInfo(new FileResource(outName, outName, Core.resourceTree), null);
                                    if (!li.isValidLevel()) {
                                        return null;
                                    }
                                    liList.add(li);
                                }
                                lp.addRating(fNameStrNoExt, liList);
                                externalLevelList.add(entry);
                                saveExternalLevelList();
                                return new int[]{0, lp.getRatings().size() - 1, 0};
                            case LVL:
                                String outName = Core.EXTERNAL_LEVEL_CACHE_PATH + fNameStrNoExt + ".ini";
                                try (Writer w = Core.resourceTree.newBufferedWriter(Core.EXTERNAL_LEVEL_CACHE_PATH + fNameStrNoExt + ".ini")) {
                                    ExtractLevel.convertLevel(lvlPath, w, false, false);
                                }
                                lvlPath = Core.resourceTree.getPath(outName);
                                /* falls through */
                            case INI:
                                LevelInfo li = new LevelInfo(new FileResource(lvlPath), null);
                                if (li.isValidLevel()) {
                                    lp.addLevel(0, li);
                                    externalLevelList.add(entry);
                                    saveExternalLevelList();
                                    return new int[]{0, 0, lp.getLevelCount(0) - 1};
                                }
                                break;
                            default:
                                break;
                        }
                    }
                } catch (IllegalArgumentException ex) {
                }
                if (showErrors) {
                    JOptionPane.showMessageDialog(LemminiFrame.getFrame(), "Wrong format!", "Load Level", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                ToolBox.showException(ex);
            }
        }
        return null;
    }
    
    public static void clearExternalLevelList() {
        externalLevelList.clear();
        levelPacks.set(0, new LevelPack());
        saveExternalLevelList();
    }
    
    private static void saveExternalLevelList() {
        int idx = 0;
        Props output = new Props();
        for (ExternalLevelEntry extLvl : externalLevelList) {
            output.set("level_" + (idx++), extLvl.toString());
        }
        output.save(Core.EXTERNAL_LEVEL_CACHE_PATH + LEVEL_CACHE_INI);
    }
    
    
    /**
     * Set horizontal scrolling offset.
     * @param x horizontal scrolling offset in pixels
     */
    public static void setXPos(final int x) {
        if (width < Core.getDrawWidth()) {
            xPos = (width - Core.getDrawWidth()) / 2;
        } else if (x >= width - Core.getDrawWidth()) {
            xPos = width - Core.getDrawWidth();
        } else if (x < 0) {
            xPos = 0;
        } else {
            xPos = x;
        }
    }
    
    /**
     * Set vertical scrolling offset.
     * @param y vertical scrolling offset in pixels
     */
    public static void setYPos(final int y) {
        if (height < LemminiFrame.LEVEL_HEIGHT) {
            yPos = height - LemminiFrame.LEVEL_HEIGHT;
        } else if (y >= height - LemminiFrame.LEVEL_HEIGHT) {
            yPos = height - LemminiFrame.LEVEL_HEIGHT;
        } else if (y < 0) {
            yPos = 0;
        } else {
            yPos = y;
        }
    }
    
    /**
     * Get horizontal scrolling offset.
     * @return horizontal scrolling offset in pixels
     */
    public static int getXPos() {
        return xPos;
    }
    
    /**
     * Get vertical scrolling offset.
     * @return vertical scrolling offset in pixels
     */
    public static int getYPos() {
        return yPos;
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
    
    public static void advanceFrame() {
        forceAdvanceFrame = true;
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
     * @throws LemmException
     */
    public static ReplayLevelInfo loadReplay(final Path fn) throws LemmException {
        return replay.load(fn);
    }
    
    /**
     * Save a replay.
     * @param fn file name
     * @return true if saved successfully, false otherwise
     */
    public static boolean saveReplay(final Path fn) {
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
    
    public static void setVerticalLock(final boolean vl) {
        verticalLock = vl;
    }
    
    public static boolean isVerticalLock() {
        return verticalLock;
    }
    
    /** get number of lemmings that exited the level
     * @return number of lemmings that exited the level
     */
    public static int getNumExited() {
        return numExited;
    }
    
    /**
     * Set number of Lemmings that exited the level.
     * @param n number of Lemmings that exited the level
     */
    public static void setNumExited(final int n) {
        numExited = n;
    }
    
    /**
     * Get level object.
     * @return level object
     */
    public static Level getLevel() {
        return level;
    }
    
    /**
     * Get the number of Lemmings currently in this level.
     * @return number of Lemmings currently in this level
     */
    public static int getNumLemmings() {
        return lemmings.size();
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
    public static Icons.IconType getIconType(final int x) {
        return Icons.getType(x);
    }
    
    /**
     * Icon was pressed.
     * @param t icon type
     */
    public static void pressIcon(final Icons.IconType t) {
        Icons.press(t);
    }
    
    /**
     * Icon was released.
     * @param t icon type
     */
    public static void releaseIcon(final Icons.IconType t) {
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
     * Get the number of Lemmings under the mouse cursor.
     * @return the number of Lemmings under the mouse cursor
     */
    public static int getNumLemmsUnderCursor() {
        return lemmsUnderCursor.size();
    }
    
    public static synchronized void updateLemmsUnderCursor() {
        lemmsUnderCursor.clear();
        lemmings.stream().forEachOrdered(l -> {
            int lx = l.screenX();
            int ly = l.screenY();
            if (lx + l.width() >= xPos && lx < xPos + Core.getDrawWidth()
                    && ly + l.height() >= yPos && ly < yPos + LemminiFrame.LEVEL_HEIGHT) {
                if (LemmCursor.doesCollide(l, xPos, yPos)) {
                    lemmsUnderCursor.add(l);
                }
            }
        });
    }
    
    /**
     * Get list of all Lemmings in this level.
     * @return list of all Lemmings in this level
     */
    public static List<Lemming> getLemmings() {
        return Collections.unmodifiableList(lemmings);
    }
    
    public static synchronized void addLemming(Lemming l) {
        lemmings.add(l);
    }
    
    /**
     * Get list of all Visual SFX in this level.
     * @return list of all Visual SFX in this level
     */
    public static List<Vsfx> getVsfx() {
    	return Collections.unmodifiableList(vsfxs);
    }
    
    public static synchronized void addVsfx(Vsfx v) {
    	vsfxs.add(v);
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
        Core.programProps.setDouble("soundGain", g);
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
        Core.programProps.setDouble("musicGain", g);
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
    
    public static void setOption(Option option, boolean enable) {
        if (enable) {
            options.add(option);
        } else {
            options.remove(option);
        }
        if (option == Option.NO_PERCENTAGES && gameState != null) {
            switch (gameState) {
                case BRIEFING:
                case LEVEL:
                case DEBRIEFING:
                case LEVEL_END:
                    setTitle();
                    break;
                default:
                    break;
            }
        }
    }
    
    public static boolean isOptionEnabled(Option option) {
        return options.contains(option);
    }
    
    public static void setOption(SuperLemminiTooOption option, boolean enable) {
        if (enable) {
            SLToptions.add(option);
        } else {
            SLToptions.remove(option);
        }
        if (option == SuperLemminiTooOption.ICON_LABELS && gameState != null) {
        	Icons.redraw();
        } else if(option == SuperLemminiTooOption.ENHANCED_ICONBAR && gameState != null) {
        	try {
        		Icons.LoadIconResources();
        	}
        	catch (ResourceException e) {
        		System.out.println("error loading resources...");
        	}
        	Icons.redraw();
        }
    }
    
    public static boolean isOptionEnabled(SuperLemminiTooOption option) {
        return SLToptions.contains(option);
    }
    
    /**
     * Get foreground image of level.
     * @return foreground image of level
     */
    public static LemmImage getFgImage() {
        return fgImage;
    }
    
    /**
     * Get foreground stencil of level.
     * @return foreground stencil of level
     */
    public static Stencil getStencil() {
        return stencil;
    }
    
    /**
     * Get small preview image of level.
     * @return small preview image of level
     */
    public static LemmImage getMapPreview() {
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
        return numExited * 100 / numLemmingsMax * 100 + numClimbers + numFloaters + numBombers +
                numBlockers + numBuilders + numBashers + numMiners + numDiggers;
    }
    
    public static LevelRecord getLevelRecord() {
        if (!wasLost() && !wasCheated) {
            return new LevelRecord(true, numExited, numSkillsUsed,
                    timed ? (timeLimit - timeElapsedTillLastExited) : time, getScore());
        } else {
            return LevelRecord.BLANK_LEVEL_RECORD;
        }
    }
    
    public static int getTimesFailed() {
        return timesFailed;
    }
    
    public static int getWidth() {
        return width;
    }
    
    public static int getHeight() {
        return height;
    }
    
    public static List<String> getModPaths() {
        return modPaths;
    }
}


class ExternalLevelEntry {
    
    private final GameController.LevelFormat format;
    private final Path lvlPath;
    
    ExternalLevelEntry(GameController.LevelFormat newFormat, Path newPath) throws IOException {
        format = newFormat;
        lvlPath = newPath;
    }
    
    GameController.LevelFormat getFormat() {
        return format;
    }
    
    Path getPath() {
        return lvlPath;
    }
    
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s, %s", format.name().toLowerCase(Locale.ROOT), lvlPath);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ExternalLevelEntry)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        
        ExternalLevelEntry obj2 = (ExternalLevelEntry) obj;
        return obj2.getFormat() == format && obj2.getPath().equals(lvlPath);
    }
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.format);
        hash = 97 * hash + Objects.hashCode(this.lvlPath);
        return hash;
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
    
    /** order of entrances */
    private static int[] entranceOrder;
    /** entrance counter */
    private static int counter;
    
    /**
     * Reset to new number of entrances.
     * @param e number of entrances
     * @param eOrder order of entrances if not null; otherwise, use default
     */
    static void reset(final int e, final int[] eOrder) throws LemmException {
        if (e == 0) {
            throw new LemmException("Level does not have any entrances.");
        }
        if (eOrder == null) {
            if (e == 3) {
                // special case: 3 entrances
                entranceOrder = PATTERN3;
            } else {
                entranceOrder = new int[e];
                for (int i = 0; i < entranceOrder.length; i++) {
                    entranceOrder[i] = i;
                }
            }
        } else {
            int newSize = 0;
            for (int en : eOrder) {
                if (en >= 0 && en < e) {
                    newSize++;
                }
            }
            if (newSize == 0) {
                throw new LemmException("No entrances in this level are used.");
            }
            if (newSize == eOrder.length) {
                entranceOrder = eOrder.clone();
            } else {
                entranceOrder = new int[newSize];
                for (int i = 0, j = 0; i < eOrder.length && j < entranceOrder.length; i++) {
                    int en = eOrder[i];
                    if (en >= 0 && en < e) {
                        entranceOrder[j++] = eOrder[i];
                    }
                }
            }
        }
        counter = 0;
    }
    
    /**
     * Get index of next entrance.
     * @return index of next entrance
     */
    static int getNext() {
        int retVal = entranceOrder[counter++];
        if (counter >= entranceOrder.length) {
            counter = 0;
        }
        return retVal;
    }
}