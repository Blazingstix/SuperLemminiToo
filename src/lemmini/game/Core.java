package lemmini.game;

import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
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
    public static final String ROOT_ZIP_NAME = "root.lzp";
    /** The revision string for resource compatibility - not necessarily the version number */
    public static final String RES_REVISION = "0.102";
    
    public static final Path[] EMPTY_PATH_ARRAY = {};
    
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
    public static List<ZipFile> zipFiles;
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
        
        bilinear = Core.programProps.getBoolean("bilinear", true);
        String resourcePathStr = programProps.get("resourcePath", StringUtils.EMPTY);
        resourcePath = Paths.get(resourcePathStr);
        
        Path sourcePath = Paths.get(programProps.get("sourcePath", StringUtils.EMPTY));
        String rev = programProps.get("revision", "zip-invalid");
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
        if (rev.equalsIgnoreCase("zip")) {
            try (ZipFile zip = new ZipFile(resourcePath.resolve(ROOT_ZIP_NAME).toFile())) {
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
                boolean deleteOldFiles = !rev.isEmpty() && !(rev.equalsIgnoreCase("zip") || rev.equalsIgnoreCase("zip-invalid"));
                Extract.extract(sourcePath, resourcePath, Paths.get("reference"), Paths.get("patch"), createPatches, deleteOldFiles);
                resourcePath = Extract.getResourcePath();
                programProps.set("revision", "zip");
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
        tempPath = resourcePath.resolve("temp");
        Files.createDirectories(tempPath);
        
        // create folder for external level cache
        externalLevelCachePath = resourcePath.resolve("levels").resolve(EXTERNAL_LEVELS_CACHE_FOLDER);
        Files.createDirectories(externalLevelCachePath);
        
        // scan for and open zip files in resource folder, being sure to open root.lzp last
        zipFiles = new ArrayList<>(16);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(resourcePath,
                file -> FilenameUtils.isExtension(file.toString().toLowerCase(Locale.ROOT), "lzp"))) {
            for (Path file : stream) {
                if (!file.getFileName().toString().equals(ROOT_ZIP_NAME)) {
                    ZipFile zipFile = new ZipFile(file.toFile());
                    zipFiles.add(zipFile);
                }
            }
        }
        zipFiles.add(new ZipFile(resourcePath.resolve(ROOT_ZIP_NAME).toFile()));
        
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
                Path file = resourcePath.resolve(resString);
                if (Files.isRegularFile(file)) {
                    return new FileResource(fname, file);
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
        Path file = resourcePath.resolve(fname);
        if (Files.isRegularFile(file)) {
            return new FileResource(fname, file);
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
                    Path file = resourcePath.resolve(mod).resolve(fnameNoExt + "." + ext);
                    if (Files.isRegularFile(file)) {
                        return new FileResource(fname, file);
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
            Path file = resourcePath.resolve(fnameNoExt + "." + ext);
            if (Files.isRegularFile(file)) {
                return new FileResource(fname, file);
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
        DirectoryStream.Filter<Path> filter = entry -> {
            if (!Files.isRegularFile(entry)) {
                return false;
            }
            for (String ext : extensions) {
                String lowercaseName = entry.getFileName().toString().toLowerCase(Locale.ROOT);
                if (lowercaseName.endsWith("." + ext)) {
                    return true;
                }
            }
            return false;
        };
        
        if (searchMods) {
            GameController.getModPaths().stream().forEachOrdered(mod -> {
                try (DirectoryStream<Path> files = Files.newDirectoryStream(resourcePath.resolve("mods").resolve(mod).resolve(folder), filter)) {
                    for (Path file : files) {
                        resources.add(file.getFileName().toString());
                    }
                } catch (IOException ex) {
                }
                zipFiles.stream().forEachOrdered(zipFile -> {
                    zipFile.stream()
                            .filter(entry -> !entry.isDirectory())
                            .filter(entry -> {
                                String name = entry.getName();
                                for (String ext : extensions) {
                                    if (FilenameUtils.getPath(name).equals("mods/" + mod + "/" + folder) && name.endsWith("." + ext)) {
                                        return true;
                                    }
                                }
                                return false;
                            }).forEachOrdered(entry -> resources.add(FilenameUtils.getName(entry.getName())));
                });
            });
        }
        try (DirectoryStream<Path> files = Files.newDirectoryStream(resourcePath.resolve(folder), filter)) {
            for (Path file : files) {
                resources.add(file.getFileName().toString());
            }
        } catch (IOException ex) {
        }
        zipFiles.stream().forEachOrdered(zipFile -> {
            zipFile.stream()
                    .filter(entry -> !entry.isDirectory())
                    .filter(entry -> {
                        String name = entry.getName();
                        for (String ext : extensions) {
                            if (FilenameUtils.getPath(name).equals(folder) && name.endsWith("." + ext)) {
                                return true;
                            }
                        }
                        return false;
                    }).forEachOrdered(entry -> {
                        resources.add(FilenameUtils.getName(entry.getName()));
                    });
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
        playerProps.save(playerPropsFilePath);
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
    public static LemmImage loadOpaqueImage(final Resource res) throws ResourceException {
        return ToolBox.imageToBuffered(loadImage(res), Transparency.OPAQUE);
    }
    
    /**
     * Loads an image from the given resource.
     * @param res resource
     * @return Image
     * @throws ResourceException
     */
    public static LemmImage loadBitmaskImage(final Resource res) throws ResourceException {
        return ToolBox.imageToBuffered(loadImage(res), Transparency.BITMASK);
    }
    
    /**
     * Loads an image from the given resource.
     * @param res resource
     * @return Image
     * @throws ResourceException
     */
    public static LemmImage loadTranslucentImage(final Resource res) throws ResourceException {
        return ToolBox.imageToBuffered(loadImage(res), Transparency.TRANSLUCENT);
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
