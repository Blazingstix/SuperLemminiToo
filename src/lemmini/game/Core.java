package lemmini.game;

import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JOptionPane;
import lemmini.LemminiFrame;
import lemmini.extract.Extract;
import lemmini.extract.ExtractException;
import lemmini.graphics.LemmImage;
import lemmini.gui.LegalFrame;
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
    public static final String EXTERNAL_LEVELS_CACHE_FOLDER = "$external";
    
    public static final Path[] EMPTY_PATH_ARRAY = {};
    
    /** The revision string for resource compatibility - not necessarily the version number */
    private static final String REVISION = "0.100";
    /** name of the INI file */
    private static final String INI_NAME = "superlemmini.ini";
    
    /** program properties */
    public static Props programProps;
    /** path of (extracted) resources */
    public static Path resourcePath;
    /** path of external level cache */
    public static Path externalLevelCachePath;
    /** path for temporary files */
    public static Path tempPath;
    /** current player */
    public static Player player;
    
    /** name of program properties file */
    private static Path programPropsFilePath;
    /** name of player properties file */
    private static Path playerPropsFilePath;
    /** player properties */
    private static Props playerProps;
    /** list of all players */
    private static List<String> players;
    private static Set<Path> resourceSet;
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
        programPropsFilePath = programPropsFilePath.resolve(INI_NAME);
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

        //scale = Core.programProps.getDouble("scale", 1.0);
        bilinear = Core.programProps.getBoolean("bilinear", true);
        String resourcePathStr = programProps.get("resourcePath", StringUtils.EMPTY);
        resourcePath = Paths.get(resourcePathStr);
        
        Path sourcePath = Paths.get(programProps.get("sourcePath", StringUtils.EMPTY));
        String rev = programProps.get("revision", StringUtils.EMPTY);
        GameController.setMusicOn(programProps.getBoolean("music", true));
        GameController.setSoundOn(programProps.getBoolean("sound", true));
        double gain;
        gain = programProps.getDouble("musicGain", 1.0);
        GameController.setMusicGain(gain);
        gain = programProps.getDouble("soundGain", 1.0);
        GameController.setSoundGain(gain);
        GameController.setAdvancedSelect(programProps.getBoolean("advancedSelect", true));
        GameController.setClassicCursor(programProps.getBoolean("classicalCursor", false));
        GameController.setSwapButtons(programProps.getBoolean("swapButtons", false));
        GameController.setFasterFastForward(programProps.getBoolean("fasterFastForward", false));
        GameController.setNoPercentages(programProps.getBoolean("noPercentages", false));
        if (resourcePathStr.isEmpty() || !REVISION.equalsIgnoreCase(rev) || createPatches) {
            // extract resources
            try {
                Extract.extract(sourcePath, resourcePath, Paths.get("reference"), Paths.get("patch"), createPatches);
                resourcePath = Extract.getResourcePath();
                programProps.set("revision", REVISION);
            } catch (ExtractException ex) {
                if (ex.isCanceledByUser()) {
                    return false;
                } else {
                    throw new LemmException(String.format("Resource extraction failed.%n%s", ex.getMessage()));
                }
            } finally {
                programProps.set("resourcePath", Extract.getResourcePath().toString());
                programProps.set("sourcePath", Extract.getSourcePath().toString());
                programProps.save(programPropsFilePath);
            }
        }
        resourceSet = new HashSet<>(2048);
        Props patchINI = new Props();
        if (patchINI.load(ToolBox.findFile(PATCH_INI_NAME))) {
            for (int i = 0; true; i++) {
                String[] entry = patchINI.getArray("extract_" + i, null);
                if (ArrayUtils.isNotEmpty(entry)) {
                    resourceSet.add(Paths.get(entry[0]));
                } else {
                    break;
                }
            }
            for (int i = 0; true; i++) {
                String[] entry = patchINI.getArray("check_" + i, null);
                if (ArrayUtils.isNotEmpty(entry)) {
                    resourceSet.add(Paths.get(entry[0]));
                } else {
                    break;
                }
            }
            for (int i = 0; true; i++) {
                String[] entry = patchINI.getArray("patch_" + i, null);
                if (ArrayUtils.isNotEmpty(entry)) {
                    resourceSet.add(Paths.get(entry[0]));
                } else {
                    break;
                }
            }
        }
        // create temp folder
        tempPath = resourcePath.resolve("temp");
        Files.createDirectories(tempPath);
        
        // create folder for external level cache
        externalLevelCachePath = resourcePath.resolve("levels").resolve(EXTERNAL_LEVELS_CACHE_FOLDER);
        Files.createDirectories(externalLevelCachePath);
        
        System.gc(); // force garbage collection here before the game starts

        // read player names
        playerPropsFilePath = resourcePath.resolve("players.ini");
        playerProps = new Props();
        playerProps.load(playerPropsFilePath);
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
    
    public static Path removeResourcePath(Path fname) {
        if (fname.startsWith(resourcePath)) {
            fname = fname.subpath(resourcePath.getNameCount(), fname.getNameCount());
        }
        return fname;
    }
    
    /**
     * Get Path to resource in resource path.
     * @param fname file name (with or without resource path)
     * @return absolute path to resource
     * @throws ResourceException if file is not found
     */
    public static Path findResource(Path fname) throws ResourceException {
        fname = removeResourcePath(fname);
        // check for the file in the mod folder
        for (Path mod : GameController.getModPaths()) {
            Path file = resourcePath.resolve(mod).resolve(fname);
            if (Files.isRegularFile(file)) {
                return file;
            }
        }
        // file not found in mod folders, so look for it in the main folders
        Path file = resourcePath.resolve(fname);
        if (Files.isRegularFile(file)) {
            return file;
        }
        // file still not found, so throw a ResourceException
        throw new ResourceException(fname.toString());
    }
    
    /**
     * Get Path to resource in resource path.
     * @param fname file name (with or without resource path)
     * @param extensions 
     * @return absolute path to resource
     * @throws ResourceException if file is not found
     */
    public static Path findResource(Path fname, String[] extensions) throws ResourceException {
        fname = removeResourcePath(fname);
        String fnameNoExt = FilenameUtils.removeExtension(fname.toString());
        // try to load the file from the mod paths with each extension
        for (Path mod : GameController.getModPaths()) {
            for (String ext : extensions) {
                Path file = resourcePath.resolve(mod).resolve(fnameNoExt + "." + ext);
                if (Files.isRegularFile(file)) {
                    return file;
                }
            }
        }
        // file not found in mod folders, so look for it in the main folders, again with each extension
        for (String ext : extensions) {
            Path file = resourcePath.resolve(fnameNoExt + "." + ext);
            if (Files.isRegularFile(file)) {
                return file;
            }
        }
        // file still not found, so throw a ResourceException
        throw new ResourceException(fname.toString());
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
        playerProps.save(playerPropsFilePath);
        player.store();
    }

    /**
     * Output error message box in case of a missing resource.
     * @param rsrc name of missing resource.
     */
    public static void resourceError(final String rsrc) {
        Path rsrcPath = removeResourcePath(Paths.get(rsrc));
        if (resourceSet.contains(rsrcPath)) {
            String out = String.format("The resource %s is missing.%n"
                    + "Please restart to extract all resources.", rsrc);
            JOptionPane.showMessageDialog(null, out, "Error", JOptionPane.ERROR_MESSAGE);
            // invalidate resources
            programProps.set("revision", "invalid");
            programProps.save(programPropsFilePath);
        } else {
            String out = String.format("The resource %s is missing.%n", rsrc);
            JOptionPane.showMessageDialog(null, out, "Error", JOptionPane.ERROR_MESSAGE);
        }
        System.exit(1);
    }

    /**
     * Load an image from the resource path.
     * @param tracker media tracker
     * @param fName file name
     * @return Image
     * @throws ResourceException
     */
    private static Image loadImage(final MediaTracker tracker, final Path fName) throws ResourceException {
        //String fileLoc = findResource(fName);
        if (fName == null) {
            return null;
        }
        return loadImage(tracker, fName.toString(), false);
    }

    /**
     * Load an image from either the resource path or from inside the JAR (or the directory of the main class).
     * @param tracker media tracker
     * @param fName file name
     * @param jar true: load from the jar/class path, false: load from resource path
     * @return Image
     * @throws ResourceException
     */
    private static Image loadImage(final MediaTracker tracker, final String fName, final boolean jar) throws ResourceException {
        Image image;
        if (jar) {
            image = Toolkit.getDefaultToolkit().createImage(ToolBox.findFile(fName));
        } else {
            image = Toolkit.getDefaultToolkit().createImage(fName);
        }
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
        if (image == null) {
            throw new ResourceException(fName);
        }
        return image;
    }

    /**
     * Load an image from the resource path.
     * @param fname file name
     * @return Image
     * @throws ResourceException
     */
    public static Image loadImage(final Path fname) throws ResourceException {
        MediaTracker tracker = new MediaTracker(LemminiFrame.getFrame());
        Image img = loadImage(tracker, fname);
        if (img == null) {
            throw new ResourceException(fname.toString());
        }
        return img;
    }

    /**
     * Load an image from the resource path.
     * @param fname file name
     * @return Image
     * @throws ResourceException
     */
    public static LemmImage loadOpaqueImage(final Path fname) throws ResourceException {
        return ToolBox.imageToBuffered(loadImage(fname), Transparency.OPAQUE);
    }

    /**
     * Load an image from the resource path.
     * @param fname file name
     * @return Image
     * @throws ResourceException
     */
    public static LemmImage loadBitmaskImage(final Path fname) throws ResourceException {
        return ToolBox.imageToBuffered(loadImage(fname), Transparency.BITMASK);
    }

    /**
     * Load an image from the resource path.
     * @param fname file name
     * @return Image
     * @throws ResourceException
     */
    public static LemmImage loadTranslucentImage(final Path fname) throws ResourceException {
        return ToolBox.imageToBuffered(loadImage(fname), Transparency.TRANSLUCENT);
    }

    /**
     * Load an image from inside the JAR or the directory of the main class.
     * @param fname
     * @return Image
     * @throws ResourceException
     */
    public static Image loadImageJar(final String fname) throws ResourceException {
        MediaTracker tracker = new MediaTracker(LemminiFrame.getFrame());
        Image img = loadImage(tracker, fname, true);
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
    public static LemmImage loadOpaqueImageJar(final String fname) throws ResourceException {
        return ToolBox.imageToBuffered(loadImageJar(fname), Transparency.OPAQUE);
    }

    /**
     * Load an image from inside the JAR or the directory of the main class.
     * @param fname
     * @return Image
     * @throws ResourceException
     */
    public static LemmImage loadBitmaskImageJar(final String fname) throws ResourceException {
        return ToolBox.imageToBuffered(loadImageJar(fname), Transparency.BITMASK);
    }

    /**
     * Load an image from inside the JAR or the directory of the main class.
     * @param fname
     * @return Image
     * @throws ResourceException
     */
    public static LemmImage loadTranslucentImageJar(final String fname) throws ResourceException {
        return ToolBox.imageToBuffered(loadImageJar(fname), Transparency.TRANSLUCENT);
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
