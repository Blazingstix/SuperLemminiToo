package lemmini.game;

//import java.awt.Image;
//import java.awt.MediaTracker;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import lemmini.LemminiFrame;
//import lemmini.extract.Extract;
//import lemmini.extract.ExtractException;
import lemmini.graphics.LemmImage;
import lemmini.gui.LegalFrame;
import lemmini.tools.CaseInsensitiveFileTree;
import lemmini.tools.CaseInsensitiveZipFile;
import lemmini.tools.Props;
import lemmini.tools.ToolBox;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

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
 * Well, this started as some kind of core class to collect all global stuff
 * Now lots of the functionality moved to GameController.
 * Would need some cleaning up, maybe remove the whole thing?
 * @author Volker Oth
 */
public class Core {
    
    /** extensions accepted for level files in file dialog */
    public static final String[] LEVEL_EXTENSIONS = {"ini", "lvl", "dat"};
    /** extensions accepted for replay files in file dialog */
    public static final String[] REPLAY_EXTENSIONS = {"rpl"};
    
    public static final String[] IMAGE_EXTENSIONS = {"png", "bmp", "gif", "jpg", "wbmp"};
    public static final String[] MUSIC_EXTENSIONS = {"wav", "aiff", "aifc", "au", "snd",
        "ogg", "xm", "s3m", "mod", "mid"};
    public static final String[] SOUNDBANK_EXTENSIONS = {"sf2", "dls"};
    public static final String[] SOUND_EXTENSIONS = {"wav", "aiff", "aifc", "au", "snd"};
    /** file name of patching configuration */
    public static final String PATCH_INI_NAME = "patch.ini";
    /** file name of main data resource set */
    public static final String ROOT_ZIP_NAME = "root.lzp";
    /** path of external level cache */
    public static final String EXTERNAL_LEVEL_CACHE_PATH = "levels/$external/";
    /** path for temporary files */
    public static final String TEMP_PATH = "temp/";
    /** The revision string for resource compatibility - not necessarily the version number */
    public static final String RES_REVISION = "0.130";
    
    public static final Path[] EMPTY_PATH_ARRAY = {};
    
    /** name of the INI file */
    private static final String PROGRAM_PROPS_FILE_NAME = "superlemmini.ini";
    /** name of player properties file */
    private static final String PLAYER_PROPS_FILE_NAME = "players.ini";
    
    /** program properties */
    public static Props programProps;
    /** path of (extracted) resources */
    public static Path resourcePath;
    /** path of currently run SuperLemminiToo instance */
    public static Path gamePath;
    /** path of the game data, within the SuperLemminiToo folder */
    public static Path gameDataPath;
    /** list of all the game resources in an lzp file. */
    public static CaseInsensitiveFileTree resourceTree;
    public static CaseInsensitiveFileTree gameDataTree;
    public static List<ZipFile> zipFiles;
    /** current player */
    public static Player player;
    
    /** name of program properties file */
    private static Path programPropsFilePath;
    /** player properties */
    private static Props playerProps;
    /** list of all players */
    private static List<String> players;
    private static Set<String> resourceSet;
    /** Zoom scale */
    private static double scale = 1.0;
    private static boolean bilinear;
    /** draw width */
    private static int drawWidth;
    /** draw height */
    private static int drawHeight;
    
    
    /**
     * Initialize some core elements.
     * Loads settings from ini file.
     * @param createPatches
     * @return true if loading was successful
     * @throws LemmException
     * @throws IOException
     */
    public static boolean init(final boolean createPatches, String workingFolder) throws LemmException, IOException  {
    	String tmp;// = java.net.URLDecoder.decode(workingFolder, "UTF-8");
    	tmp = new java.io.File(workingFolder).getPath();
    	gamePath = Paths.get(tmp);
    	//if it's within the .jar file, we want the folder one up from that. 
    	if (gamePath.toString().endsWith(".jar")) {
            gameDataPath = Paths.get(gamePath.getParent().toString(), "data");
    	} else {
            gameDataPath = Paths.get(gamePath.toString(), "data");
    	}
    		
        gameDataTree = new CaseInsensitiveFileTree(gameDataPath); 

        // get ini path
        programPropsFilePath = Paths.get(SystemUtils.USER_HOME);
        programPropsFilePath = programPropsFilePath.resolve(PROGRAM_PROPS_FILE_NAME);
        // read main ini file
        programProps = new Props();
        
        if (!programProps.load(programPropsFilePath)) { 
        	// might exist or not - if not, it's created
        	// show the Legal Disclaimer. And force the user to choose "I Agree."
        	// NOTE: the Legal Discalimer is loaded from "disclaimer.htm"
            LegalFrame ld = new LegalFrame();
            ld.setVisible(true);
            ld.waitUntilClosed();
            if (!ld.isOK()) {
            	// user does not agree, so we exit.
                return false;
            }
        }
        
        bilinear = programProps.getBoolean("bilinear", false);
        String resourcePathStr = programProps.get("resourcePath", Paths.get(SystemUtils.USER_HOME, ".superlemminitoo").toString());
        //resourcePath is the source of your game resources
        resourcePath = Paths.get(resourcePathStr);
        resourceTree = new CaseInsensitiveFileTree(resourcePath);
        
        //SourcePath is the source of your original WinLemm installation
        //Path sourcePath = Paths.get(programProps.get("sourcePath", StringUtils.EMPTY));
        String rev = programProps.get("revision", "zip");
        GameController.setOption(GameController.Option.MUSIC_ON, programProps.getBoolean("music", true));
        GameController.setOption(GameController.Option.SOUND_ON, programProps.getBoolean("sound", true));
        GameController.setMusicGain(programProps.getDouble("musicGain", 1.0));
        GameController.setSoundGain(programProps.getDouble("soundGain", 1.0));
        GameController.setOption(GameController.Option.ADVANCED_SELECT, programProps.getBoolean("advancedSelect", true));
        GameController.setOption(GameController.Option.CLASSIC_CURSOR, programProps.getBoolean("classicalCursor", false));
        GameController.setOption(GameController.Option.SWAP_BUTTONS, programProps.getBoolean("swapButtons", false));
        GameController.setOption(GameController.Option.FASTER_FAST_FORWARD, programProps.getBoolean("fasterFastForward", false));
        GameController.setOption(GameController.Option.PAUSE_STOPS_FAST_FORWARD, programProps.getBoolean("pauseStopsFastForward", false));
        GameController.setOption(GameController.Option.NO_PERCENTAGES, programProps.getBoolean("noPercentages", true));
        GameController.setOption(GameController.Option.REPLAY_SCROLL, programProps.getBoolean("replayScroll", true));
        GameController.setOption(GameController.Option.UNPAUSE_ON_ASSIGNMENT, programProps.getBoolean("unpauseOnAssignment", true));
        // new settings added by SuperLemminiToo
        GameController.setOption(GameController.SuperLemminiTooOption.TIMED_BOMBERS, programProps.getBoolean("timedBombers", true));
        GameController.setOption(GameController.SuperLemminiTooOption.UNLOCK_ALL_LEVELS, programProps.getBoolean("unlockAllLevels", true));
        GameController.setOption(GameController.SuperLemminiTooOption.DISABLE_SCROLL_WHEEL, programProps.getBoolean("disableScrollWheel", true));
        GameController.setOption(GameController.SuperLemminiTooOption.DISABLE_FRAME_STEPPING, programProps.getBoolean("disableFrameStepping", true));
        GameController.setOption(GameController.SuperLemminiTooOption.VISUAL_SFX, programProps.getBoolean("visualSFX", true));
        GameController.setOption(GameController.SuperLemminiTooOption.ENHANCED_STATUS, programProps.getBoolean("enhancedStatus", true));
        GameController.setOption(GameController.SuperLemminiTooOption.SHOW_STATUS_TOTALS, programProps.getBoolean("showStatusTotals", true));
        GameController.setOption(GameController.SuperLemminiTooOption.ENHANCED_ICONBAR, programProps.getBoolean("enhancedIconBar", true));
        GameController.setOption(GameController.SuperLemminiTooOption.ICON_LABELS, programProps.getBoolean("iconLabels", true));
        GameController.setOption(GameController.SuperLemminiTooOption.ANIMATED_ICONS, programProps.getBoolean("animatedIcons", true));

        // check for the existence of root.lzp.
        // if it's not there, then we must exit.
        List<Path> tmpDataFile = gameDataTree.getAllPaths(ROOT_ZIP_NAME);
        if(tmpDataFile.isEmpty() ) {
        	Path rootPath = Paths.get(gameDataPath.toString(), ROOT_ZIP_NAME);
        	throw new LemmException("Could not find main game data file.\nPlease enusure this file exists and is accessible by this user:\n\n" + rootPath.toString());
        }

        // rev corresponds to the version number of the extracted resource files.
        // the revision is now contained within the root.lzp data zip, in a file called revision.ini
        // this is indicated by a revision of zip in the settings file.
        // if that setting doesn't exist, zip-invalid is the default value. 
        // if the rev is empty or equals zip-invalid, or doesn't == zip, then we'll probably have to re-extract the resource files.
		/*
        boolean maybeDeleteOldFiles = !rev.isEmpty() && !(rev.equalsIgnoreCase("zip") || rev.equalsIgnoreCase("zip-invalid"));
        */
        // if rev is "zip" then the actual revision in inside root.lzp->revision.ini
        if (rev.equalsIgnoreCase("zip")) {
        	rev = getRevisionFromRootLzp();
        }
        
      
        //TODO: never try to extract resources... that should be moved to it's own program.
        //the goal of this program should be to have resources already included in the distributable.
        //maybe WilLem's hand-crafted remastered level pack for orig and ohno
        if (resourcePathStr.isEmpty() || !rev.equalsIgnoreCase(RES_REVISION) || createPatches) {
        	throw new LemmException(String.format("Game resources not found.\n Please place a valid copy of root.lzp into " + gameDataPath.toString(), (Object[])null));
        	// extract resources
            /*
        	try {
                Extract.extract(sourcePath, resourceTree, Paths.get("reference"), Paths.get("patch"), createPatches, maybeDeleteOldFiles);
                resourceTree = Extract.getResourceTree();
                resourcePath = resourceTree.getRoot();
                programProps.set("revision", "zip");
            } catch (ExtractException ex) {
                if (ex.isCanceledByUser()) {
                    return false;
                } else {
                    throw new LemmException(String.format("Resource extraction failed.%n%s", ex.getMessage()));
                }
            } finally {
                CaseInsensitiveFileTree resTree = Extract.getResourceTree();
                CaseInsensitiveFileTree srcTree = Extract.getSourceTree();
                if (resTree != null) {
                	//TODO
                	//for now, we're going to comment this out... don't want to overwrite important stuff
                	//programProps.set("resourcePath", resTree.getRoot().toString());
                }
                if (srcTree != null) {
                    //TODO
                	//for now, we're going to comment this out... don't want to overwrite important stuff
                	//programProps.set("sourcePath", srcTree.getRoot().toString());
                }
                programProps.save(programPropsFilePath);
            }
            */
        }

        populateResourceSet();
        
        // create temp folder
        resourceTree.createDirectories(TEMP_PATH);
        
        // create folder for external level cache
        resourceTree.createDirectories(EXTERNAL_LEVEL_CACHE_PATH);
        
        
        loadZipFiles();
       
        System.gc(); // force garbage collection here before the game starts
        
        loadPlayerSettings();
        
        return true;
    }
    
    /**
     * Reads root.lzp->revision.ini and returns the value for the "revision" entry.
     * @return Returns the revision value, or "" if nothing if found.
     */
    private static String getRevisionFromRootLzp() {
        try (ZipFile zip = new CaseInsensitiveZipFile(gameDataTree.getPath(ROOT_ZIP_NAME).toFile())) {
            ZipEntry entry = zip.getEntry("revision.ini");
            try (Reader r = ToolBox.getBufferedReader(zip.getInputStream(entry))) {
                Props p = new Props();
                if (p.load(r)) {
                    return p.get("revision", StringUtils.EMPTY);
                }
            }
        } catch (IOException ex) {
        }
        return StringUtils.EMPTY;
    }
    
    
    /**
     * Reads in the contents of patch.ini into the HashSet resourceSet
     */
    private static void populateResourceSet() {
        //NOTE: getting a list of all file data in the patch.ini file...? why??
        resourceSet = new HashSet<>(2048);
        Props patchINI = new Props();
        if (patchINI.load(ToolBox.findFile(PATCH_INI_NAME))) {
            for (int i = 0; true; i++) {
                String[] entry = patchINI.getArray("extract_" + i, null);
                if (ArrayUtils.isNotEmpty(entry)) {
                    resourceSet.add(entry[0]);
                } else {
                    break;
                }
            }
            for (int i = 0; true; i++) {
                String[] entry = patchINI.getArray("check_" + i, null);
                if (ArrayUtils.isNotEmpty(entry)) {
                    resourceSet.add(entry[0]);
                } else {
                    break;
                }
            }
            for (int i = 0; true; i++) {
                String[] entry = patchINI.getArray("patch_" + i, null);
                if (ArrayUtils.isNotEmpty(entry)) {
                    resourceSet.add(entry[0]);
                } else {
                    break;
                }
            }
        }
    }
    
    /**
     * Loads a list of all lzp zip files into the zipFiles ArrayList 
     * @throws ZipException
     * @throws IOException
     */
    private static void loadZipFiles() throws ZipException, IOException {
        // scan for and open zip files in resource folder, being sure to open root.lzp last
        //TODO: load all these lzp files from a user-specified folder. (or even the user's home folder, maybe?)
        //and save the root.lzp for the Lemmings data folder itself.
        zipFiles = new ArrayList<>(16);
        for (Path file : resourceTree.getAllPathsRegex("[^/]+\\.lzp")) {
            if (!file.getFileName().toString().toLowerCase(Locale.ROOT).equals(ROOT_ZIP_NAME)) {
                zipFiles.add(new CaseInsensitiveZipFile(file.toFile()));
            }
        }
        //load the main root.lzp from the game data folder.
        for (Path file : gameDataTree.getAllPaths(ROOT_ZIP_NAME)) {
            zipFiles.add(new CaseInsensitiveZipFile(file.toFile()));
        }
    }
    
    /***
     *  Reads all player settings from the players.ini file
     */
    private static void loadPlayerSettings() {
        // read player names
        playerProps = new Props();
        playerProps.load(PLAYER_PROPS_FILE_NAME);
        String defaultPlayer = playerProps.get("defaultPlayer", "default");
        players = new ArrayList<>(16);
        for (int idx = 0; true; idx++) {
            String p = playerProps.get("player_" + idx, StringUtils.EMPTY);
            if (p.isEmpty()) {
                break;
            }
            players.add(p);
        }
        if (players.isEmpty()) {
            // no players yet, establish default player
            players.add("default");
            playerProps.set("player_0", "default");
        }
        player = new Player(defaultPlayer);
    }
    
    /***
     * Writes all applicable settings to the settings ini file
     */
    public static void saveSettings() {
        //sound settings
    	programProps.setBoolean("music", GameController.isOptionEnabled(GameController.Option.MUSIC_ON));
        programProps.setBoolean("sound", GameController.isOptionEnabled(GameController.Option.SOUND_ON));
        programProps.set("mixerName", GameController.sound.getMixers()[GameController.sound.getMixerIdx()]);
        //graphic settings
        programProps.setBoolean("bilinear", Core.isBilinear());
        //misc settings
        programProps.setBoolean("advancedSelect", GameController.isOptionEnabled(GameController.Option.ADVANCED_SELECT));
        programProps.setBoolean("classicalCursor", GameController.isOptionEnabled(GameController.Option.CLASSIC_CURSOR));
        programProps.setBoolean("swapButtons", GameController.isOptionEnabled(GameController.Option.SWAP_BUTTONS));
        programProps.setBoolean("fasterFastForward", GameController.isOptionEnabled(GameController.Option.FASTER_FAST_FORWARD));
        programProps.setBoolean("pauseStopsFastForward", GameController.isOptionEnabled(GameController.Option.PAUSE_STOPS_FAST_FORWARD));
        programProps.setBoolean("noPercentages", GameController.isOptionEnabled(GameController.Option.NO_PERCENTAGES));
        programProps.setBoolean("replayScroll", GameController.isOptionEnabled(GameController.Option.REPLAY_SCROLL));
        programProps.setBoolean("unpauseOnAssignment", GameController.isOptionEnabled(GameController.Option.UNPAUSE_ON_ASSIGNMENT));
        // new settings added by SuperLemminiToo
        programProps.setBoolean("timedBombers", GameController.isOptionEnabled(GameController.SuperLemminiTooOption.TIMED_BOMBERS));
        programProps.setBoolean("unlockAllLevels", GameController.isOptionEnabled(GameController.SuperLemminiTooOption.UNLOCK_ALL_LEVELS));
        programProps.setBoolean("disableScrollWheel", GameController.isOptionEnabled(GameController.SuperLemminiTooOption.DISABLE_SCROLL_WHEEL));
        programProps.setBoolean("disableFrameStepping", GameController.isOptionEnabled(GameController.SuperLemminiTooOption.DISABLE_FRAME_STEPPING));
        programProps.setBoolean("visualSFX", GameController.isOptionEnabled(GameController.SuperLemminiTooOption.VISUAL_SFX));
        programProps.setBoolean("enhancedStatus", GameController.isOptionEnabled(GameController.SuperLemminiTooOption.ENHANCED_STATUS));
        programProps.setBoolean("showStatusTotals", GameController.isOptionEnabled(GameController.SuperLemminiTooOption.SHOW_STATUS_TOTALS));
        programProps.setBoolean("enhancedIconBar", GameController.isOptionEnabled(GameController.SuperLemminiTooOption.ENHANCED_ICONBAR));
        programProps.setBoolean("iconLabels", GameController.isOptionEnabled(GameController.SuperLemminiTooOption.ICON_LABELS));
        programProps.setBoolean("animatedIcons", GameController.isOptionEnabled(GameController.SuperLemminiTooOption.ANIMATED_ICONS));

    }
    
    public static String appendBeforeExtension(String fname, String suffix) {
        String extension = FilenameUtils.getExtension(fname);
        if (extension.isEmpty()) {
            return FilenameUtils.removeExtension(fname) + suffix;
        } else {
            return FilenameUtils.removeExtension(fname) + suffix + "." + extension;
        }
    }
    
    /**
     * Get Path to resource in resource path.
     * @param fname file name (without resource path)
     * @param searchMods
     * @return resource object
     * @throws ResourceException if file is not found
     */
    public static Resource findResource(String fname, boolean searchMods) throws ResourceException {
        if (searchMods) {
            // try to load the file from the mod paths
            for (String mod : GameController.getModPaths()) {
                String resString = mod + "/" + fname;
                if (resourceTree.exists(resString)) {
                    return new FileResource(fname, resString, resourceTree);
                }
                for (ZipFile zipFile : zipFiles) {
                    ZipEntry entry = zipFile.getEntry(resString);
                    if (entry != null && !entry.isDirectory()) {
                        return new ZipEntryResource(fname, zipFile, entry);
                    }
                }
            }
        }
        // file not found in mod folders or mods not searched,
        // so look for it in the main folders
        if (resourceTree.exists(fname)) {
            return new FileResource(fname, fname, resourceTree);
        }
        for (ZipFile zipFile : zipFiles) {
            ZipEntry entry = zipFile.getEntry(fname);
            if (entry != null && !entry.isDirectory()) {
                return new ZipEntryResource(fname, zipFile, entry);
            }
        }
        // file still not found, so throw a ResourceException
        throw new ResourceException(fname);
    }
    
    /**
     * Get Path to resource in resource path.
     * @param fname file name (without resource path)
     * @param searchMods
     * @param extensions 
     * @return resource object
     * @throws ResourceException if file is not found
     */
    public static Resource findResource(String fname, boolean searchMods, String... extensions) throws ResourceException {
        String fnameNoExt = FilenameUtils.removeExtension(fname);
        String originalExt = FilenameUtils.getExtension(fname);
        if (searchMods) {
            // try to load the file from the mod paths with each extension
            for (String mod : GameController.getModPaths()) {
                for (String ext : extensions) {
                    String resString = mod + "/" + fnameNoExt + "." + ext;
                    if (resourceTree.exists(resString)) {
                        return new FileResource(fname, resString, resourceTree);
                    }
                }
                for (ZipFile zipFile : zipFiles) {
                    for (String ext : extensions) {
                        ZipEntry entry = zipFile.getEntry(mod + "/" + fnameNoExt + "." + ext);
                        if (entry != null && !entry.isDirectory()) {
                            return new ZipEntryResource(fname, zipFile, entry);
                        }
                    }
                }
            }
        }
        // file not found in mod folders or mods not searched,
        // so look for it in the main folders, again with each extension
        for (String ext : extensions) {
            String resString = fnameNoExt + "." + ext;
            if (resourceTree.exists(resString)) {
                return new FileResource(fname, resString, resourceTree);
            }
        }
        for (ZipFile zipFile : zipFiles) {
            for (String ext : extensions) {
                ZipEntry entry = zipFile.getEntry(fnameNoExt + "." + ext);
                if (entry != null && !entry.isDirectory()) {
                    return new ZipEntryResource(fname, zipFile, entry);
                }
            }
        }
        // file still not found, so throw a ResourceException
        throw new ResourceException(fname);
    }
    
    public static List<String> searchForResources(String folder, boolean searchMods, String... extensions) {
        Set<String> resources = new LinkedHashSet<>(64);
        
        if (searchMods) {
            GameController.getModPaths().stream().forEachOrdered(mod -> {
                String lowercasePath = ("mods/" + mod + "/" + folder).toLowerCase(Locale.ROOT);
                resourceTree.getAllPathsRegex(ToolBox.literalToRegex(lowercasePath) + "[^/]+").stream()
                        .map(file -> file.getFileName().toString())
                        .filter(fileName -> FilenameUtils.isExtension(fileName.toLowerCase(Locale.ROOT), extensions))
                        .forEachOrdered(resources::add);
                zipFiles.stream().forEachOrdered(zipFile -> {
                    zipFile.stream()
                            .map(ZipEntry::getName)
                            .filter(entryName -> ToolBox.getParent(entryName).toLowerCase(Locale.ROOT).equals(lowercasePath))
                            .filter(entryName -> {
                                return FilenameUtils.isExtension(entryName.toLowerCase(Locale.ROOT), extensions);
                            }).forEachOrdered(entryName -> resources.add(ToolBox.getFileName(entryName)));
                });
            });
        }
        String lowercasePath = folder.toLowerCase(Locale.ROOT);
        resourceTree.getAllPathsRegex(ToolBox.literalToRegex(lowercasePath) + "[^/]+").stream()
                .map(file -> file.getFileName().toString())
                .filter(fileName -> FilenameUtils.isExtension(fileName.toLowerCase(Locale.ROOT), extensions))
                .forEachOrdered(resources::add);
        zipFiles.stream().forEachOrdered(zipFile -> {
            zipFile.stream()
                    .map(ZipEntry::getName)
                    .filter(entryName -> ToolBox.getParent(entryName).toLowerCase(Locale.ROOT).equals(lowercasePath))
                    .filter(entryName -> {
                        return FilenameUtils.isExtension(entryName.toLowerCase(Locale.ROOT), extensions);
                    }).forEachOrdered(entryName -> resources.add(ToolBox.getFileName(entryName)));
        });
        
        return new ArrayList<>(resources);
    }
    
    /**
     * Set the title
     * @param title
     */
    public static void setTitle(String title) {
        LemminiFrame.getFrame().setTitle(title);
    }
    
    /**
     * Store program properties.
     */
    public static void saveProgramProps() {
        programProps.save(programPropsFilePath);
        playerProps.set("defaultPlayer", player.getName());
        playerProps.save(PLAYER_PROPS_FILE_NAME);
        player.store();
    }
    
    /**
     * Output error message box in case of a missing resource.
     * @param rsrc name of missing resource.
     */
    public static void resourceError(final String rsrc) {
        if (resourceSet.contains(rsrc)) {
            String out = String.format("The resource %s is missing.%n"
                    + "Please restart to extract all resources.", rsrc);
            JOptionPane.showMessageDialog(null, out, "Error", JOptionPane.ERROR_MESSAGE);
            // invalidate resources
            //programProps.set("revision", "zip-invalid"); //NOTE: we don't want to do that now that we're bundling the root.lzp with the executable.
            programProps.save(programPropsFilePath);
        } else {
            String out = String.format("The resource %s is missing.%n", rsrc);
            JOptionPane.showMessageDialog(null, out, "Error", JOptionPane.ERROR_MESSAGE);
        }
        System.exit(1);
    }
    
    /**
     * Adds the given image to the given tracker.
     * @param tracker media tracker
     * @param image image to add
     * @return given image if operation was successful; null otherwise
     * @throws ResourceException
     */
/*    private static Image addToTracker(final MediaTracker tracker, Image image) throws ResourceException {
        if (image != null) {
            tracker.addImage(image, 0);
            try {
                tracker.waitForID(0);
                if (tracker.isErrorAny()) {
                    image = null;
                }
            } catch (Exception ex) {
                image = null;
            }
        }
        return image;
    }
    */
    /**
     * Loads an image from the given resource.
     * @param res resource
     * @return Image
     * @throws ResourceException
     */
    public static LemmImage loadLemmImage(final Resource res) throws ResourceException {
        BufferedImage img = null;
        if (res != null) {
            try (InputStream in = res.getInputStream()) {
                img = ImageIO.read(in);
            } catch (IOException ex) {
                img = null;
            }
        }
        if (img == null) {
            throw new ResourceException(res);
        }
        return new LemmImage(img);
    }
    
    /**
     * Load an image from inside the JAR or the directory of the main class.
     * @param fname
     * @return Image
     * @throws ResourceException
     */
    public static LemmImage loadLemmImageJar(final String fname) throws ResourceException {
        BufferedImage img;
        try {
            img = ImageIO.read(ToolBox.findFile(fname));
        } catch (IOException ex) {
            throw new ResourceException(fname);
        }
        return new LemmImage(img);
    }
    
    /**
     * Get player name via index.
     * @param idx player index
     * @return player name
     */
    public static String getPlayer(final int idx) {
        return players.get(idx);
    }
    
    /**
     * Get number of players.
     * @return number of player.
     */
    public static int getPlayerCount() {
        if (players == null) {
            return 0;
        }
        return players.size();
    }
    
    /**
     * Delete a player.
     * @param idx index of player to delete
     */
    public static void deletePlayer(final int idx) {
        Player.deletePlayerINIFile(players.get(idx));
        players.remove(idx);
    }
    
    /**
     * Reset list of players.
     */
    public static void clearPlayers() {
        players.clear();
        playerProps.clear();
    }
    
    /**
     * Add player.
     * @param name player name
     */
    public static void addPlayer(final String name) {
        players.add(name);
        playerProps.set("player_" + (players.size() - 1), name);
    }
    
    /**
     * Get internal draw width
     * @return internal draw width
     */
    public static int getDrawWidth() {
        return drawWidth;
    }
    
    /**
     * Get scaled internal draw width
     * @return scaled internal draw width
     */
    public static int getScaledDrawWidth() {
        return (scale == 1.0) ? drawWidth : (int) Math.ceil(drawWidth * scale);
    }
    
    /**
     * Get internal draw height
     * @return internal draw width
     */
    public static int getDrawHeight() {
        return drawHeight;
    }
    
    /**
     * Get scaled internal draw height
     * @return scaled internal draw width
     */
    public static int getScaledDrawHeight() {
        return (scale == 1.0) ? drawHeight : (int) Math.ceil(drawHeight * scale);
    }
    
    /**
     * Set internal draw size
     * @param w draw width
     * @param h draw height
     */
    public static void setDrawSize(int w, int h) {
        drawWidth = w;
        drawHeight = h;
    }
    
    /**
     * Get zoom scale
     * @return zoom scale
     */
    public static double getScale() {
        return scale;
    }
    
    /**
     * Set zoom scale
     * @param s zoom scale
     */
    public static void setScale(double s) {
        scale = s;
    }
    
    public static int scale(int n) {
        return ToolBox.scale(n, scale);
    }
    
    public static int unscale(int n) {
        return ToolBox.unscale(n, scale);
    }
    
    public static boolean isBilinear() {
        return bilinear;
    }
    
    public static void setBilinear(final boolean b) {
        bilinear = b;
    }
}
