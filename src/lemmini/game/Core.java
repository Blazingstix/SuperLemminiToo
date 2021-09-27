package lemmini.game;

import java.awt.Component;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
    public static final String[] LEVEL_EXTENSIONS = {"ini", "lvl"};
    /** extensions accepted for replay files in file dialog */
    public static final String[] REPLAY_EXTENSIONS = {"rpl"};
    
    public static final String[] IMAGE_EXTENSIONS = {"png", "gif", "jpg"};
    public static final String[] MUSIC_EXTENSIONS = {"wav", "aiff", "aifc", "au", "snd",
        "ogg", "xm", "s3m", "mod", "mid"};
    public static final String[] SOUNDBANK_EXTENSIONS = {"sf2", "dls"};
    public static final String[] SOUND_EXTENSIONS = {"wav", "aiff", "aifc", "au", "snd"};
    
    /** The revision string for resource compatibility - not necessarily the version number */
    private static final String REVISION = "0.91a";
    /** name of the INI file */
    private static final String INI_NAME = "superlemmini.ini";

    /** program properties */
    static Props programProps;
    /** path of (extracted) resources */
    static String resourcePath;
    /** current player */
    public static Player player;
    /** name of program properties file */
    static String programPropsFileStr;
    /** name of player properties file */
    static String playerPropsFileStr;
    /** player properties */
    static Props playerProps;
    /** list of all players */
    static List<String> players;
    
    /** parent component (main frame) */
    private static JFrame cmp;
    
    /**
     * Initialize some core elements.
     * @param frame parent frame
     * @param isWebstartApp true if this was started via Webstart, false otherwise
     * @param createPatches
     * @return 
     * @throws LemmException
     */
    public static boolean init(final JFrame frame, final boolean isWebstartApp, final boolean createPatches) throws LemmException  {
        // get ini path
        if (isWebstartApp) {
            programPropsFileStr = ToolBox.exchangeSeparators(System.getProperty("user.home"));
            programPropsFileStr = ToolBox.addSeparator(programPropsFileStr);
        } else {
            String s = frame.getClass().getName().replace('.', '/') + ".class";
            URL url = frame.getClass().getClassLoader().getResource(s);
            int pos;
            try {
                programPropsFileStr = URLDecoder.decode(url.getPath(), "UTF-8");
            } catch (UnsupportedEncodingException ex) {
            }
            // special handling for JAR
            pos = programPropsFileStr.toLowerCase(Locale.ROOT).indexOf("file:");
            if (pos != -1) {
                programPropsFileStr = programPropsFileStr.substring(pos + 5);
            }
            pos = programPropsFileStr.toLowerCase(Locale.ROOT).indexOf(s.toLowerCase(Locale.ROOT));
            if (pos != -1) {
                programPropsFileStr = programPropsFileStr.substring(0, pos);
            }
            
            /** @todo doesn't work if JAR is renamed...
             *  Maybe it would be a better idea to search only for ".JAR" and then
             *  for the first path separator...
             */
            
            s = (frame.getClass().getName().replace('.', '/') + ".jar").toLowerCase(Locale.ROOT);
            pos = programPropsFileStr.toLowerCase().indexOf(s);
            if (pos != -1) {
                programPropsFileStr = programPropsFileStr.substring(0, pos);
            }
        }
        programPropsFileStr += INI_NAME;
        // read main ini file
        programProps = new Props();

        if (!programProps.load(programPropsFileStr)) { // might exist or not - if not, it's created
            LegalDialog ld = new LegalDialog(null, true);
            ld.setVisible(true);
            if (!ld.isOk()) {
                return false;
            }
        }

        resourcePath = programProps.get("resourcePath", "");
        String sourcePath = programProps.get("sourcePath", "");
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
        if (resourcePath.isEmpty() || !REVISION.equalsIgnoreCase(rev) || createPatches) {
            // extract resources
            try {
                Extract.extract(null, sourcePath, resourcePath, "reference", "patch", createPatches);
                resourcePath = Extract.getResourcePath();
                programProps.set("revision", REVISION);
            } catch (ExtractException ex) {
                if (ex.isCanceledByUser()) {
                    return false;
                } else {
                    throw new LemmException(String.format("Resource extraction failed.%n%s", ex.getMessage()));
                }
            } finally {
                programProps.set("resourcePath", ToolBox.addSeparator(Extract.getResourcePath()));
                programProps.set("sourcePath", ToolBox.addSeparator(Extract.getSourcePath()));
                programProps.save(programPropsFileStr);
            }
        }
        System.gc(); // force garbage collection here before the game starts

        // read player names
        playerPropsFileStr = resourcePath + "players.ini";
        playerProps = new Props();
        playerProps.load(playerPropsFileStr);
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
    public static Component getCmp() {
        return cmp;
    }

    /**
     * Returns the program properties.
     * @return the program properties
     */
    public static Props getProgramProps() {
        return programProps;
    }

    /**
     * Returns the width of the game
     * @return the width of the game
     */
    public static int getWidth() {
        return cmp.getWidth();
    }
    
    /**
     * Returns the resource path.
     * @return the resource path
     */
    public static String getResourcePath() {
        return resourcePath;
    }
    
    public static String removeExtension(String fname) {
        int slashIndex = Math.max(fname.lastIndexOf("/"), fname.lastIndexOf("\\"));
        int dotIndex = fname.lastIndexOf(".");
        if (slashIndex < dotIndex) {
            return fname.substring(0, dotIndex);
        } else {
            return fname;
        }
    }
    
    /**
     * Get String to resource in resource path.
     * @param fname file name (without resource path)
     * @return absolute path to resource
     */
    public static String findResource(String fname) {
        if (fname.startsWith(resourcePath)) {
            fname = fname.substring(resourcePath.length());
        }
        for (String mod : GameController.getMods()) {
            String file = String.format("%s%s/%s", resourcePath, mod, fname);
            if (new File(file).canRead()) {
                return file;
            }
        }
        String file = resourcePath + fname;
        if (new File(file).canRead()) {
            return file;
        }
        return null;
    }
    
    /**
     * Get String to resource in resource path.
     * @param fname file name (without resource path)
     * @param extensions 
     * @return absolute path to resource
     */
    public static String findResource(String fname, String[] extensions) {
        if (fname.startsWith(resourcePath)) {
            fname = fname.substring(resourcePath.length());
        }
        String fname2 = removeExtension(fname);
        for (String mod : GameController.getMods()) {
            for (String ext : extensions) {
                String file = String.format("%s%s/%s.%s", resourcePath, mod, fname2, ext);
                if (new File(file).canRead()) {
                    return file;
                }
            }
        }
        for (String ext : extensions) {
            String file = String.format("%s/%s.%s", resourcePath, fname2, ext);
            if (new File(file).canRead()) {
                return file;
            }
        }
        String file = resourcePath + fname2;
        if (new File(file).canRead()) {
            return file;
        }
        return null;
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
        programProps.save(programPropsFileStr);
        playerProps.set("defaultPlayer", player.getName());
        playerProps.save(playerPropsFileStr);
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
        programProps.save(programPropsFileStr);
        System.exit(1);
    }

    /**
     * Load an image from the resource path.
     * @param tracker media tracker
     * @param fName file name
     * @return Image
     * @throws ResourceException
     */
    private static java.awt.Image loadImage(final MediaTracker tracker, final String fName) throws ResourceException {
        //String fileLoc = findResource(fName);
        if (fName == null) {
            return null;
        }
        return loadImage(tracker, fName, false);
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
    public static java.awt.Image loadImage(final String fname) throws ResourceException {
        MediaTracker tracker = new MediaTracker(getCmp());
        java.awt.Image img = loadImage(tracker, fname);
        if (img == null) {
            throw new ResourceException(fname);
        }
        return img;
    }

    /**
     * Load an image from the resource path.
     * @param fname file name
     * @return Image
     * @throws ResourceException
     */
    public static Image loadOpaqueImage(final String fname) throws ResourceException {
        return ToolBox.imageToBuffered(loadImage(fname), Transparency.OPAQUE);
    }

    /**
     * Load an image from the resource path.
     * @param fname file name
     * @return Image
     * @throws ResourceException
     */
    public static Image loadBitmaskImage(final String fname) throws ResourceException {
        return ToolBox.imageToBuffered(loadImage(fname), Transparency.BITMASK);
    }

    /**
     * Load an image from the resource path.
     * @param fname file name
     * @return Image
     * @throws ResourceException
     */
    public static Image loadTranslucentImage(final String fname) throws ResourceException {
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
}
