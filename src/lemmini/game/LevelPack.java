package lemmini.game;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lemmini.tools.Props;
import lemmini.tools.ToolBox;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

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
 * Handling of a level pack.
 *
 * @author Volker Oth
 */
public class LevelPack {
    
    public static final String FAILURE_A_DEF =
            "ROCK BOTTOM! I hope for your sake\nthat you nuked that level.";
    public static final String FAILURE_B_DEF =
            "Better rethink your strategy before\nyou try this level again!";
    public static final String FAILURE_C_DEF =
            "A little more practice on this level\nis definitely recommended.";
    public static final String FAILURE_D_DEF =
            "You got pretty close that time.\nNow try again for that few % extra.";
    public static final String FAILURE_E_DEF =
            "OH NO, So near and yet so far (teehee)\nMaybe this time.....";
    public static final String SUCCESS_A_DEF =
            "SPOT ON. You can't get much closer\nthan that. Let's try the next....";
    public static final String SUCCESS_B_DEF =
            "That level seemed no problem to you on\nthat attempt. Onto the next....";
    public static final String SUCCESS_C_DEF =
            "You totally stormed that level!\nLet's see if you can storm the next...";
    public static final String SUCCESS_D_DEF =
            "Superb! You rescued every lemming on\nthat level. Can you do it again....?";

    /** name of the level pack */
    private String name;
    private boolean allLevelsUnlocked;
    /** seed used to generate the level codes */
    private String codeSeed;
    /** array containing names of ratings (easiest first, hardest last) */
    private String[] ratings;
    /** array of array of level info - [rating][level number] */
    private LevelInfo[][] lvlInfo;
    /** path of level pack - where the INI files for the level pack are located */
    private final Path path;
    /** maximum number of pixels a Lemming can fall before he dies */
    private int maxFallDistance;
    /** offset to apply in level code algorithm */
    private int codeOffset;
    
    private final String[] debriefings = new String[9];
    private Path[] mods;

    /**
     * Constructor for dummy level pack. Needed for loading single levels.
     */
    public LevelPack() {
        name = "External Levels";
        allLevelsUnlocked = true;
        path = Paths.get(StringUtils.EMPTY);
        mods = Core.EMPTY_PATH_ARRAY;
        codeSeed = StringUtils.EMPTY;
        maxFallDistance = 126;
        codeOffset = 0;

        ratings = new String[1];
        ratings[0] = "Single Levels";

        lvlInfo = new LevelInfo[1][0];
        
        debriefings[0] = FAILURE_A_DEF;
        debriefings[1] = FAILURE_B_DEF;
        debriefings[2] = FAILURE_C_DEF;
        debriefings[3] = FAILURE_D_DEF;
        debriefings[4] = FAILURE_E_DEF;
        debriefings[5] = SUCCESS_A_DEF;
        debriefings[6] = SUCCESS_B_DEF;
        debriefings[7] = SUCCESS_C_DEF;
        debriefings[8] = SUCCESS_D_DEF;
    }

    /**
     * Constructor for loading a level pack.
     * @param fname file name of level pack INI
     * @throws ResourceException
     */
    public LevelPack(final Path fname) throws ResourceException {
        // extract path from descriptor file
        path = fname.getParent();
        // load the descriptor file
        Props props = new Props();
        if (!props.load(fname)) {
            throw new ResourceException(fname.toString());
        }
        // read name
        name = props.get("name", StringUtils.EMPTY);
        allLevelsUnlocked = props.getBoolean("allLevelsUnlocked", false);
        // read mods
        String[] modsStr = props.getArray("mods", ArrayUtils.EMPTY_STRING_ARRAY);
        if (modsStr.length == 0) {
            mods = Core.EMPTY_PATH_ARRAY;
        } else {
            mods = new Path[modsStr.length];
            for (int i = 0; i < modsStr.length; i++) {
                mods[i] = Paths.get("mods", modsStr[i]);
            }
        }
        // read code seed
        codeSeed = props.get("codeSeed", StringUtils.EMPTY).trim().toUpperCase(Locale.ROOT);
        // read code level offset
        codeOffset = props.getInt("codeOffset", 0);
        // read max falling distance
        maxFallDistance = props.getInt("maxFallDistance", 126);
        // read music files
        List<Path> music = new ArrayList<>(64);
        String track;
        int idx = 0;
        do {
            track = props.get("music_" + (idx++), StringUtils.EMPTY);
            if (!track.isEmpty()) {
                music.add(Paths.get(track));
            }
        } while (!track.isEmpty());
        // read debriefings
        debriefings[0] = props.get("failureA", FAILURE_A_DEF);
        debriefings[1] = props.get("failureB", FAILURE_B_DEF);
        debriefings[2] = props.get("failureC", FAILURE_C_DEF);
        debriefings[3] = props.get("failureD", FAILURE_D_DEF);
        debriefings[4] = props.get("failureE", FAILURE_E_DEF);
        debriefings[5] = props.get("successA", SUCCESS_A_DEF);
        debriefings[6] = props.get("successB", SUCCESS_B_DEF);
        debriefings[7] = props.get("successC", SUCCESS_C_DEF);
        debriefings[8] = props.get("successD", SUCCESS_D_DEF);
        // read ratings
        List<String> ratingList = new ArrayList<>(8);
        idx = 0;
        String rating;
        do {
            rating = props.get("level_" + idx, StringUtils.EMPTY);
            idx++;
            if (!rating.isEmpty()) {
                ratingList.add(rating);
            }
        } while (!rating.isEmpty());
        ratings = ratingList.toArray(ArrayUtils.EMPTY_STRING_ARRAY);
        // read levels
        lvlInfo = new LevelInfo[ratings.length][];
        String[] levelStr;
        List<LevelInfo> levels = new ArrayList<>(64);
        for (int r = 0; r < ratings.length; r++) {
            idx = 0;
            levels.clear();
            rating = ratings[r].trim().toLowerCase(Locale.ROOT);
            do {
                levelStr = props.getArray("level_" + r + "_" + idx, null);
                if (levelStr == null) {
                    levelStr = props.getArray(rating + "_" + idx, null);
                }
                // filename, music number
                if (levelStr != null && levelStr.length >= 2) {
                    LevelInfo info = new LevelInfo(path.resolve(levelStr[0]),
                            music.get(ToolBox.parseInt(levelStr[1])));
                    levels.add(info);
                }
                idx++;
            } while (levelStr != null && levelStr.length >= 2);
            lvlInfo[r] = levels.toArray(new LevelInfo[levels.size()]);
        }
    }

    /**
     * Assemble level pack and rating to string.
     * @param pack level pack
     * @param rating name of rating
     * @return String formed from level pack and rating
     */
    public static String getID(String pack, String rating) {
        pack = Normalizer.normalize(pack.toLowerCase(Locale.ROOT), Normalizer.Form.NFKC);
        rating = Normalizer.normalize(rating.toLowerCase(Locale.ROOT), Normalizer.Form.NFKC);
        
        return pack + "-" + rating;
    }

    /**
     * Return ratings as string array.
     * @return ratings as string array
     */
    public String[] getRatings() {
        return ratings;
    }

    /**
     * Get name of level pack.
     * @return name of level pack
     */
    public String getName() {
        return name;
    }

    /**
     * Get code seed.
     * @return code seed.
     */
    public String getCodeSeed() {
        return codeSeed;
    }

    /**
     * Get maximum fall distance.
     * @return maximum fall distance
     */
    public int getMaxFallDistance() {
        return maxFallDistance;
    }
    
    public String[] getDebriefings() {
        return debriefings;
    }
    
    public boolean getAllLevelsUnlocked() {
        return allLevelsUnlocked;
    }

    /**
     * Get offset to apply in level code algorithm.
     * @return offset to apply in level code algorithm
     */
    public int getCodeOffset() {
        return codeOffset;
    }

    /**
     * Get level info for a certain level.
     * @param rating rating
     * @param level level number
     * @return LevelInfo for the given level
     */
    public LevelInfo getInfo(final int rating, final int level) {
        return lvlInfo[rating][level];
    }
    
    /**
     * Set level info for a certain level.
     * @param rating rating
     * @param level level number
     * @param li new LevelInfo for the given level
     */
    public void setInfo(final int rating, final int level, final LevelInfo li) {
        lvlInfo[rating][level] = li;
    }
    
    /**
     * Add a level to the end of a rating.
     * @param rating rating
     * @param li LevelInfo for the new level
     */
    public void addLevel(final int rating, final LevelInfo li) {
        lvlInfo[rating] = ArrayUtils.add(lvlInfo[rating], li);
    }
    
    /**
     * Remove a level from a rating.
     * @param rating rating
     * @param level index of the level to remove
     */
    public void removeLevel(final int rating, final int level) {
        lvlInfo[rating] = ArrayUtils.remove(lvlInfo[rating], level);
    }
    
    /**
     * Add a rating to this level pack.
     * @param rating name of rating
     * @param li LevelInfo for each level
     */
    public void addRating(final String rating, final LevelInfo[] li) {
        ratings = ArrayUtils.add(ratings, rating);
        lvlInfo = ArrayUtils.add(lvlInfo, li);
    }
    
    /**
     * Remove a rating from this level pack.
     * @param rating index of rating to remove
     */
    public void removeRating(final int rating) {
        ratings = ArrayUtils.remove(ratings, rating);
        lvlInfo = ArrayUtils.remove(lvlInfo, rating);
    }

    /**
     * Return all levels for a rating
     * @param rating index of rating
     * @return level names as string array
     */
    public String[] getLevels(final int rating) {
        String[] names = new String[lvlInfo[rating].length];
        for (int i = 0; i < lvlInfo[rating].length; i++) {
            names[i] = lvlInfo[rating][i].getName().trim();
        }
        return names;
    }
    
    public int getRatingCount() {
        return lvlInfo.length;
    }
    
    public int getLevelCount(final int rating) {
        return lvlInfo[rating].length;
    }
    
    public Path[] getModPaths() {
        return mods;
    }
}