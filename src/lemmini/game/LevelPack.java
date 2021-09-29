package lemmini.game;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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
 * Handling of a level pack.
 *
 * @author Volker Oth
 */
public class LevelPack {
    
    public static final Path[] EMPTY_PATH_ARRAY = {};
    
    public static final String FAILURE_A1_DEF = "ROCK BOTTOM! I hope for your sake";
    public static final String FAILURE_A2_DEF = "that you nuked that level.";
    public static final String FAILURE_B1_DEF = "Better rethink your strategy before";
    public static final String FAILURE_B2_DEF = "you try this level again!";
    public static final String FAILURE_C1_DEF = "A little more practice on this level";
    public static final String FAILURE_C2_DEF = "is definitely recommended.";
    public static final String FAILURE_D1_DEF = "You got pretty close that time.";
    public static final String FAILURE_D2_DEF = "Now try again for that few % extra.";
    public static final String FAILURE_E1_DEF = "OH NO, So near and yet so far (teehee)";
    public static final String FAILURE_E2_DEF = "Maybe this time.....";
    public static final String SUCCESS_A1_DEF = "SPOT ON. You can't get much closer";
    public static final String SUCCESS_A2_DEF = "than that. Let's try the next....";
    public static final String SUCCESS_B1_DEF = "That level seemed no problem to you on";
    public static final String SUCCESS_B2_DEF = "that attempt. Onto the next....";
    public static final String SUCCESS_C1_DEF = "You totally stormed that level!";
    public static final String SUCCESS_C2_DEF = "Let's see if you can storm the next...";
    public static final String SUCCESS_D1_DEF = "Superb! You rescued every lemming on";
    public static final String SUCCESS_D2_DEF = "that level. Can you do it again....?";

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
    
    private final String[] debriefings = new String[18];
    private String[] mods;

    /**
     * Constructor for dummy level pack. Needed for loading single levels.
     */
    public LevelPack() {
        name = "External Levels";
        allLevelsUnlocked = true;
        path = Paths.get("");
        mods = new String[0];
        codeSeed = "";
        maxFallDistance = 126;
        codeOffset = 0;

        ratings = new String[1];
        ratings[0] = "Single Levels";

        lvlInfo = new LevelInfo[1][0];
        //lvlInfo[0][0] = new LevelInfo();
        //lvlInfo[0][0].setMusic(Paths.get("tim1.mod"));
        //lvlInfo[0][0].setName("test");
        //lvlInfo[0][0].setFileName(Paths.get(""));
        
        debriefings[0]  = FAILURE_A1_DEF;
        debriefings[1]  = FAILURE_A2_DEF;
        debriefings[2]  = FAILURE_B1_DEF;
        debriefings[3]  = FAILURE_B2_DEF;
        debriefings[4]  = FAILURE_C1_DEF;
        debriefings[5]  = FAILURE_C2_DEF;
        debriefings[6]  = FAILURE_D1_DEF;
        debriefings[7]  = FAILURE_D2_DEF;
        debriefings[8]  = FAILURE_E1_DEF;
        debriefings[9]  = FAILURE_E2_DEF;
        debriefings[10] = SUCCESS_A1_DEF;
        debriefings[11] = SUCCESS_A2_DEF;
        debriefings[12] = SUCCESS_B1_DEF;
        debriefings[13] = SUCCESS_B2_DEF;
        debriefings[14] = SUCCESS_C1_DEF;
        debriefings[15] = SUCCESS_C2_DEF;
        debriefings[16] = SUCCESS_D1_DEF;
        debriefings[17] = SUCCESS_D2_DEF;
    }

    /**
     * Constructor for loading a level pack.
     * @param fname file name of level pack ini
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
        name = props.get("name", "");
        allLevelsUnlocked = props.getBoolean("allLevelsUnlocked", false);
        // read mods
        mods = props.getArray("mods", null);
        if (mods == null) {
            mods = new String[0];
        }
        // read code seed
        codeSeed = props.get("codeSeed", "").trim().toUpperCase(Locale.ROOT);
        // read code level offset
        codeOffset = props.getInt("codeOffset", 0);
        // read max falling distance
        maxFallDistance = props.getInt("maxFallDistance", 126);
        // read music files
        List<Path> music = new ArrayList<>(64);
        String track;
        int idx = 0;
        do {
            track = props.get("music_" + (idx++), "");
            if (!track.isEmpty()) {
                music.add(Paths.get(track));
            }
        } while (!track.isEmpty());
        // read debriefings
        debriefings[0]  = props.get("failureA1", FAILURE_A1_DEF);
        debriefings[1]  = props.get("failureA2", FAILURE_A2_DEF);
        debriefings[2]  = props.get("failureB1", FAILURE_B1_DEF);
        debriefings[3]  = props.get("failureB2", FAILURE_B2_DEF);
        debriefings[4]  = props.get("failureC1", FAILURE_C1_DEF);
        debriefings[5]  = props.get("failureC2", FAILURE_C2_DEF);
        debriefings[6]  = props.get("failureD1", FAILURE_D1_DEF);
        debriefings[7]  = props.get("failureD2", FAILURE_D2_DEF);
        debriefings[8]  = props.get("failureE1", FAILURE_E1_DEF);
        debriefings[9]  = props.get("failureE2", FAILURE_E2_DEF);
        debriefings[10] = props.get("successA1", SUCCESS_A1_DEF);
        debriefings[11] = props.get("successA2", SUCCESS_A2_DEF);
        debriefings[12] = props.get("successB1", SUCCESS_B1_DEF);
        debriefings[13] = props.get("successB2", SUCCESS_B2_DEF);
        debriefings[14] = props.get("successC1", SUCCESS_C1_DEF);
        debriefings[15] = props.get("successC2", SUCCESS_C2_DEF);
        debriefings[16] = props.get("successD1", SUCCESS_D1_DEF);
        debriefings[17] = props.get("successD2", SUCCESS_D2_DEF);
        // read ratings
        List<String> ratingList = new ArrayList<>(8);
        idx = 0;
        String rating;
        do {
            rating = props.get("level_" + idx, "");
            idx++;
            if (!rating.isEmpty()) {
                ratingList.add(rating);
            }
        } while (!rating.isEmpty());
        ratings = new String[ratingList.size()];
        ratings = ratingList.toArray(ratings);
        // read levels
        lvlInfo = new LevelInfo[ratingList.size()][];
        String[] levelStr;
        List<LevelInfo> levels = new ArrayList<>(64);
        for (int r = 0; r < ratingList.size(); r++) {
            idx = 0;
            levels.clear();
            rating = Normalizer.normalize(ratingList.get(r).trim(), Normalizer.Form.NFKC);
            do {
                levelStr = props.getArray(rating.toLowerCase(Locale.ROOT) + "_" + idx, null);
                // filename, music number
                if (levelStr != null && levelStr.length >= 2) {
                    LevelInfo info = new LevelInfo(path.resolve(levelStr[0]),
                            music.get(ToolBox.parseInt(levelStr[1])));
                    levels.add(info);
                }
                idx++;
            } while (levelStr != null && levelStr.length >= 2);
            lvlInfo[r] = new LevelInfo[levels.size()];
            lvlInfo[r] = levels.toArray(lvlInfo[r]);
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
        lvlInfo[rating] = Arrays.copyOf(lvlInfo[rating], lvlInfo[rating].length + 1);
        lvlInfo[rating][lvlInfo[rating].length - 1] = li;
    }
    
    /**
     * Add a rating to this level pack.
     * @param name name of rating
     * @param li LevelInfo for each level
     */
    public void addRating(final String name, final LevelInfo[] li) {
        ratings = Arrays.copyOf(ratings, ratings.length + 1);
        ratings[ratings.length - 1] = name;
        lvlInfo = Arrays.copyOf(lvlInfo, lvlInfo.length + 1);
        lvlInfo[lvlInfo.length - 1] = li;
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
    
    public Path[] getModPaths() {
        if (mods.length == 0) {
            return EMPTY_PATH_ARRAY;
        }
        
        Path[] retArray = new Path[mods.length];
        for (int i = 0; i < mods.length; i++) {
            retArray[i] = Paths.get("mods", mods[i]);
        }
        return retArray;
    }
}