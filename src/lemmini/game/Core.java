package lemmini.game;

import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.swing.JOptionPane;
import lemmini.LemminiFrame;
import lemmini.extract.Extract;
import lemmini.extract.ExtractException;
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
    
    public static final String[] IMAGE_EXTENSIONS = {"png", "gif", "jpg"};
    public static final String[] MUSIC_EXTENSIONS = {"wav", "aiff", "aifc", "au", "snd",
        "ogg", "xm", "s3m", "mod", "mid"};
    public static final String[] SOUNDBANK_EXTENSIONS = {"sf2", "dls"};
    public static final String[] SOUND_EXTENSIONS = {"wav", "aiff", "aifc", "au", "snd"};
    /** file name of patching configuration */
    public static final String PATCH_INI_NAME = "patch.ini";
    public static final String ROOT_ZIP_NAME = "root.lzp";
    /** path of external level cache */
    public static final String EXTERNAL_LEVEL_CACHE_PATH = "levels/$external/";
    /** path for temporary files */
    public static final String TEMP_PATH = "temp/";
    /** The revision string for resource compatibility - not necessarily the version number */
    public static final String RES_REVISION = "0.103";
    
    public static final Path[] EMPTY_PATH_ARRAY = {};
    
    /** name of the INI file */
    private static final String PROGRAM_PROPS_FILE_NAME = "superlemmini.ini";
    /** name of player properties file */
    private static final String PLAYER_PROPS_FILE_NAME = "players.ini";
    
    /** program properties */
    public static Props programProps;
    /** path of (extracted) resources */
    public static Path resourcePath;
    public static CaseInsensitiveFileTree resourceTree;
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
     * @param createPatches
     * @return 
     * @throws LemmException
     * @throws IOException
     */
    public static boolean init(final boolean createPatches) throws LemmException, IOException  {
        // get ini path
        programPropsFilePath = Paths.get(SystemUtils.USER_HOME);
        programPropsFilePath = programPropsFilePath.resolve(PROGRAM_PROPS_FILE_NAME);
        // read main ini file
        programProps = new Props();
        
        if (!programProps.load(programPropsFilePath)) { // might exist or not - if not, it's created
            LegalFrame ld = new LegalFrame();
            ld.setVisible(true);
            ld.waitUntilClosed();
            if (!ld.isOK()) {
                return false;
            }
        }
        
        bilinear = Core.programProps.getBoolean("bilinear", true);
        String resourcePathStr = programProps.get("resourcePath", StringUtils.EMPTY);
        resourcePath = Paths.get(resourcePathStr);
        resourceTree = new CaseInsensitiveFileTree(resourcePath);
        
        Path sourcePath = Paths.get(programProps.get("sourcePath", StringUtils.EMPTY));
        String rev = programProps.get("revision", "zip-invalid");
        GameController.setOption(GameController.Option.MUSIC_ON, programProps.getBoolean("music", true));
        GameController.setOption(GameController.Option.SOUND_ON, programProps.getBoolean("sound", true));
        double gain;
        gain = programProps.getDouble("musicGain", 1.0);
        GameController.setMusicGain(gain);
        gain = programProps.getDouble("soundGain", 1.0);
        GameController.setSoundGain(gain);
        GameController.setOption(GameController.Option.ADVANCED_SELECT, programProps.getBoolean("advancedSelect", true));
        GameController.setOption(GameController.Option.CLASSIC_CURSOR, programProps.getBoolean("classicalCursor", false));
        GameController.setOption(GameController.Option.SWAP_BUTTONS, programProps.getBoolean("swapButtons", false));
        GameController.setOption(GameController.Option.FASTER_FAST_FORWARD, programProps.getBoolean("fasterFastForward", false));
        GameController.setOption(GameController.Option.PAUSE_STOPS_FAST_FORWARD, programProps.getBoolean("pauseStopsFastForward", false));
        GameController.setOption(GameController.Option.NO_PERCENTAGES, programProps.getBoolean("noPercentages", false));
        GameController.setOption(GameController.Option.REPLAY_SCROLL, programProps.getBoolean("replayScroll", true));
        GameController.setOption(GameController.Option.UNPAUSE_ON_ASSIGNMENT, programProps.getBoolean("unpauseOnAssignment", false));
        boolean maybeDeleteOldFiles = !rev.isEmpty() && !(rev.equalsIgnoreCase("zip") || rev.equalsIgnoreCase("zip-invalid"));
        if (rev.equalsIgnoreCase("zip")) {
            try (ZipFile zip = new CaseInsensitiveZipFile(resourceTree.getPath(ROOT_ZIP_NAME).toFile())) {
                ZipEntry entry = zip.getEntry("revision.ini");
                try (Reader r = ToolBox.getBufferedReader(zip.getInputStream(entry))) {
                    Props p = new Props();
                    if (p.load(r)) {
                        rev = p.get("revision", StringUtils.EMPTY);
                    }
                }
            } catch (IOException ex) {
            }
        }
        if (resourcePathStr.isEmpty() || !rev.equalsIgnoreCase(RES_REVISION) || createPatches) {
            // extract resources
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
                    programProps.set("resourcePath", resTree.getRoot().toString());
                }
                if (srcTree != null) {
                    programProps.set("sourcePath", srcTree.getRoot().toString());
                }
                programProps.save(programPropsFilePath);
            }
        }
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
        // create temp folder
        resourceTree.createDirectories(TEMP_PATH);
        
        // create folder for external level cache
        resourceTree.createDirectories(EXTERNAL_LEVEL_CACHE_PATH);
        
        // scan for and open zip files in resource folder, being sure to open root.lzp last
        zipFiles = new ArrayList<>(16);
        for (Path file : resourceTree.getAllPathsRegex("[^/]+\\.lzp")) {
            if (!file.getFileName().toString().toLowerCase(Locale.ROOT).equals(ROOT_ZIP_NAME)) {
                zipFiles.add(new CaseInsensitiveZipFile(file.toFile()));
            }
        }
        for (Path file : resourceTree.getAllPaths(ROOT_ZIP_NAME)) {
            zipFiles.add(new CaseInsensitiveZipFile(file.toFile()));
        }
        
        System.gc(); // force garbage collection here before the game starts
        
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
        
        return true;
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
        //programProps.setDouble("scale", scale);
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
            programProps.set("revision", "zip-invalid");
            programProps.save(programPropsFilePath);
        } else {
            String out = String.format("The resource %s is missing.%n", rsrc);
            JOptionPane.showMessageDialog(null, out, "Error", JOptionPane.ERROR_MESSAGE);
        }
        System.exit(1);
    }
    
    /**
     * Loads an image from the given resource.
     * @param tracker media tracker
     * @param res resource
     * @return Image
     * @throws ResourceException
     */
    private static Image loadImage(final MediaTracker tracker, final Resource res) throws ResourceException {
        if (res == null) {
            return null;
        }
        Image image;
        try {
            image = Toolkit.getDefaultToolkit().createImage(res.readAllBytes());
        } catch (IOException ex) {
            return null;
        }
        return addToTracker(tracker, image);
    }
    
    /**
     * Adds the given image to the given tracker.
     * @param tracker media tracker
     * @param image image to add
     * @return given image if operation was successful; null otherwise
     * @throws ResourceException
     */
    private static Image addToTracker(final MediaTracker tracker, Image image) throws ResourceException {
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
    
    /**
     * Loads an image from the given resource.
     * @param res resource
     * @return Image
     * @throws ResourceException
     */
    public static Image loadImage(final Resource res) throws ResourceException {
        MediaTracker tracker = new MediaTracker(LemminiFrame.getFrame());
        Image img = loadImage(tracker, res);
        if (img == null) {
            throw new ResourceException(res);
        }
        return img;
    }
    
    /**
     * Loads an image from the given resource.
     * @param res resource
     * @return Image
     * @throws ResourceException
     */
    public static LemmImage loadLemmImage(final Resource res) throws ResourceException {
        return loadLemmImage(res, Transparency.TRANSLUCENT);
    }
    
    /**
     * Loads an image from the given resource.
     * @param res resource
     * @param transparency
     * @return Image
     * @throws ResourceException
     */
    public static LemmImage loadLemmImage(final Resource res, final int transparency) throws ResourceException {
        return ToolBox.imageToBuffered(loadImage(res), transparency);
    }
    
    /**
     * Load an image from inside the JAR or the directory of the main class.
     * @param fname
     * @return Image
     * @throws ResourceException
     */
    public static Image loadImageJar(final String fname) throws ResourceException {
        MediaTracker tracker = new MediaTracker(LemminiFrame.getFrame());
        Image img = addToTracker(tracker, Toolkit.getDefaultToolkit().createImage(ToolBox.findFile(fname)));
        if (img == null) {
            throw new ResourceException(fname);
        }
        return img;
    }
    
    /**
     * Load an image from inside the JAR or the directory of the main class.
     * @param fname
     * @return Image
     * @throws ResourceException
     */
    public static LemmImage loadLemmImageJar(final String fname) throws ResourceException {
        return loadLemmImageJar(fname, Transparency.TRANSLUCENT);
    }
    
    /**
     * Load an image from inside the JAR or the directory of the main class.
     * @param fname
     * @param transparency
     * @return Image
     * @throws ResourceException
     */
    public static LemmImage loadLemmImageJar(final String fname, final int transparency) throws ResourceException {
        return ToolBox.imageToBuffered(loadImageJar(fname), transparency);
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
