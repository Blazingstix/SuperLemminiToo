package lemmini.game;

import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import lemmini.extract.Extract;
import lemmini.extract.ExtractException;
import lemmini.graphics.Image;
import lemmini.gui.LegalDialog;
import lemmini.tools.Props;
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
    
    /** The revision string for resource compatibility - not necessarily the version number */
    private static final String REVISION = "0.94";
    /** name of the INI file */
    private static final String INI_NAME = "superlemmini.ini";

    /** program properties */
    public static Props programProps;
    /** path of (extracted) resources */
    public static Path resourcePath;
    /** path for temporary files */
    public static Path tempPath;
    /** current player */
    public static Player player;
    
    /** parent component (main frame) */
    private static JFrame cmp;
    /** name of program properties file */
    private static Path programPropsFilePath;
    /** name of player properties file */
    private static Path playerPropsFilePath;
    /** player properties */
    private static Props playerProps;
    /** list of all players */
    private static List<String> players;
    /** Zoom scale */
    private static double scale;
    private static boolean bilinear;
    /** draw width */
    private static int drawWidth;
    /** draw height */
    private static int drawHeight;
    
    
    /**
     * Initialize some core elements.
     * @param frame parent frame
     * @param isWebstartApp true if this was started via Webstart, false otherwise
     * @param createPatches
     * @return 
     * @throws LemmException
     */
    public static boolean init(final JFrame frame, final boolean isWebstartApp, final boolean createPatches) throws LemmException, IOException  {
        // get ini path
        //if (isWebstartApp) {
            programPropsFilePath = Paths.get(System.getProperty("user.home"));
        //} else {
            /*
            String s = frame.getClass().getName().replace('.', '/') + ".class";
            URL url = frame.getClass().getClassLoader().getResource(s);
            int pos;
            try {
                programPropsFilePath = URLDecoder.decode(url.getPath(), "UTF-8");
            } catch (UnsupportedEncodingException ex) {
            }
            // special handling for JAR
            pos = programPropsFilePath.toLowerCase(Locale.ROOT).indexOf("file:");
            if (pos != -1) {
                programPropsFilePath = programPropsFilePath.substring(pos + 5);
            }
            pos = programPropsFilePath.toLowerCase(Locale.ROOT).indexOf(s.toLowerCase(Locale.ROOT));
            if (pos != -1) {
                programPropsFilePath = programPropsFilePath.substring(0, pos);
            }
            /*
            
            /** @todo doesn't work if JAR is renamed...
             *  Maybe it would be a better idea to search only for ".JAR" and then
             *  for the first path separator...
             */
            
            /*
            s = (frame.getClass().getName().replace('.', '/') + ".jar").toLowerCase(Locale.ROOT);
            pos = programPropsFilePath.toLowerCase().indexOf(s);
            if (pos != -1) {
                programPropsFilePath = programPropsFilePath.substring(0, pos);
            }
            */
        //}
        programPropsFilePath = programPropsFilePath.resolve(INI_NAME);
        // read main ini file
        programProps = new Props();

        if (!programProps.load(programPropsFilePath)) { // might exist or not - if not, it's created
            LegalDialog ld = new LegalDialog(null, true);
            ld.setVisible(true);
            if (!ld.isOK()) {
                return false;
            }
        }

        scale = Core.programProps.getDouble("scale", 1.0);
        bilinear = Core.programProps.getBoolean("bilinear", true);
        String resourcePathStr = programProps.get("resourcePath", "");
        resourcePath = Paths.get(resourcePathStr);
        Path sourcePath = Paths.get(programProps.get("sourcePath", ""));
        String rev = programProps.get("revision", "");
        GameController.setMusicOn(programProps.getBoolean("music", true));
        GameController.setSoundOn(programProps.getBoolean("sound", true));
        double gain;
        gain = programProps.getDouble("musicGain", 1.0);
        GameController.setMusicGain(gain);
        gain = programProps.getDouble("soundGain", 1.0);
        GameController.setSoundGain(gain);
        GameController.setAdvancedSelect(programProps.getBoolean("advancedSelect", true));
        GameController.setSwapButtons(programProps.getBoolean("swapButtons", false));
        GameController.setFasterFastForward(programProps.getBoolean("fasterFastForward", false));
        if (resourcePathStr.isEmpty() || !REVISION.equalsIgnoreCase(rev) || createPatches) {
            // extract resources
            try {
                Extract.extract(null, sourcePath, resourcePath, Paths.get("reference"), Paths.get("patch"), createPatches);
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
        tempPath = resourcePath.resolve("temp");
        Files.createDirectories(tempPath);
        System.gc(); // force garbage collection here before the game starts

        // read player names
        playerPropsFilePath = resourcePath.resolve("players.ini");
        playerProps = new Props();
        playerProps.load(playerPropsFilePath);
        String defaultPlayer = playerProps.get("defaultPlayer", "default");
        players = new ArrayList<>(16);
        for (int idx = 0; true; idx++) {
            String p = playerProps.get("player_" + idx, "");
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

        cmp = frame;
        
        return true;
    }

    /**
     * Get parent component (main frame).
     * @return parent component
     */
    public static JFrame getCmp() {
        return cmp;
    }
    
    public static String appendBeforeExtension(String fname, String suffix) {
        int slashIndex = Math.max(fname.lastIndexOf("/"), fname.lastIndexOf("\\"));
        int dotIndex = fname.lastIndexOf(".");
        if (slashIndex < dotIndex) {
            return fname.substring(0, dotIndex) + suffix + fname.substring(dotIndex);
        } else {
            return fname + suffix;
        }
    }
    
    /**
     * Get Path to resource in resource path.
     * @param fname file name (with or without resource path)
     * @return absolute path to resource
     * @throws ResourceException if file is not found
     */
    public static Path findResource(Path fname) throws ResourceException {
        // remove the resource path if it exists
        if (fname.startsWith(resourcePath)) {
            fname = fname.subpath(resourcePath.getNameCount(), fname.getNameCount());
        }
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
        // remove the resource path and extension if they exist
        if (fname.startsWith(resourcePath)) {
            fname = fname.subpath(resourcePath.getNameCount(), fname.getNameCount());
        }
        String fnameNoExt = ToolBox.removeExtension(fname.toString());
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
        cmp.setTitle(title);
    }
    
    /**
     * Store program properties.
     */
    public static void saveProgramProps() {
        programProps.setDouble("scale", scale);
        programProps.save(programPropsFilePath);
        playerProps.set("defaultPlayer", player.getName());
        playerProps.save(playerPropsFilePath);
        player.store();
    }

    /**
     * Output error message box in case of a missing resource.
     * @param rsrc name missing of resource.
     */
    public static void resourceError(final String rsrc) {
        String out = String.format("The resource %s is missing.%n"
                + "Please restart to extract all resources.", rsrc);
        JOptionPane.showMessageDialog(null, out, "Error", JOptionPane.ERROR_MESSAGE);
        // invalidate resources
        programProps.set("revision", "invalid");
        programProps.save(programPropsFilePath);
        System.exit(1);
    }

    /**
     * Load an image from the resource path.
     * @param tracker media tracker
     * @param fName file name
     * @return Image
     * @throws ResourceException
     */
    private static java.awt.Image loadImage(final MediaTracker tracker, final Path fName) throws ResourceException {
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
    private static java.awt.Image loadImage(final MediaTracker tracker, final String fName, final boolean jar) throws ResourceException {
        java.awt.Image image;
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
    public static java.awt.Image loadImage(final Path fname) throws ResourceException {
        MediaTracker tracker = new MediaTracker(getCmp());
        java.awt.Image img = loadImage(tracker, fname);
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
    public static Image loadOpaqueImage(final Path fname) throws ResourceException {
        return ToolBox.imageToBuffered(loadImage(fname), Transparency.OPAQUE);
    }

    /**
     * Load an image from the resource path.
     * @param fname file name
     * @return Image
     * @throws ResourceException
     */
    public static Image loadBitmaskImage(final Path fname) throws ResourceException {
        return ToolBox.imageToBuffered(loadImage(fname), Transparency.BITMASK);
    }

    /**
     * Load an image from the resource path.
     * @param fname file name
     * @return Image
     * @throws ResourceException
     */
    public static Image loadTranslucentImage(final Path fname) throws ResourceException {
        return ToolBox.imageToBuffered(loadImage(fname), Transparency.TRANSLUCENT);
    }

    /**
     * Load an image from inside the JAR or the directory of the main class.
     * @param fname
     * @return Image
     * @throws ResourceException
     */
    public static java.awt.Image loadImageJar(final String fname) throws ResourceException {
        MediaTracker tracker = new MediaTracker(getCmp());
        java.awt.Image img = loadImage(tracker, fname, true);
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
    public static Image loadOpaqueImageJar(final String fname) throws ResourceException {
        return ToolBox.imageToBuffered(loadImageJar(fname), Transparency.OPAQUE);
    }

    /**
     * Load an image from inside the JAR or the directory of the main class.
     * @param fname
     * @return Image
     * @throws ResourceException
     */
    public static Image loadBitmaskImageJar(final String fname) throws ResourceException {
        return ToolBox.imageToBuffered(loadImageJar(fname), Transparency.BITMASK);
    }

    /**
     * Load an image from inside the JAR or the directory of the main class.
     * @param fname
     * @return Image
     * @throws ResourceException
     */
    public static Image loadTranslucentImageJar(final String fname) throws ResourceException {
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
    public static int getPlayerNum() {
        if (players == null) {
            return 0;
        }
        return players.size();
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
     * Get internal Draw Width
     * @return internal draw width
     */
    public static int getDrawWidth() {
        return drawWidth;
    }
    
    /**
     * Set internal Draw Width
     * @param w draw width
     */
    public static void setDrawWidth(int w) {
        drawWidth = w;
    }
    
    /**
     * Get internal Draw Height
     * @return internal draw width
     */
    public static int getDrawHeight() {
        return drawHeight;
    }
    
    /**
     * Set internal Draw Width
     * @param w draw width
     */
    public static void setDrawHeight(int h) {
        drawHeight = h;
    }
    
    /**
     * Get Zoom scale
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
        return (scale == 1.0) ? n : ((int) Math.round(n * scale));
    }
    
    public static int unscale(int n) {
        return (scale == 1.0) ? n : ((int) Math.round(n / scale));
    }
    
    public static boolean isBilinear() {
        return bilinear;
    }
    
    public static void setBilinear(final boolean b) {
        bilinear = b;
    }
}
