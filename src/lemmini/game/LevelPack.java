package lemmini.game;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.stream.Collectors;
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
    /** list containing names of ratings (easiest first, hardest last) */
    private final List<String> ratings = new ArrayList<>(16);
    /** list of list of level info - [rating][level number] */
    private final List<List<LevelInfo>> lvlInfo = new ArrayList<>(16);
    /** path of level pack - where the INI files for the level pack are located */
    private final String path;
    /** maximum number of pixels a Lemming can fall before he dies */
    private int maxFallDistance;
    /** offset to apply in level code algorithm */
    private int codeOffset;
    
    private final List<String> debriefings = new ArrayList<>(9);
    private final List<String> mods;
    
    /**
     * Constructor for dummy level pack. Needed for loading single levels.
     */
    public LevelPack() {
        name = "External Levels";
        allLevelsUnlocked = true;
        path = StringUtils.EMPTY;
        codeSeed = StringUtils.EMPTY;
        maxFallDistance = 126;
        codeOffset = 0;
        mods = Collections.emptyList();
        
        ratings.add("Single Levels");
        
        lvlInfo.add(new ArrayList<>(64));
        
        debriefings.add(FAILURE_A_DEF);
        debriefings.add(FAILURE_B_DEF);
        debriefings.add(FAILURE_C_DEF);
        debriefings.add(FAILURE_D_DEF);
        debriefings.add(FAILURE_E_DEF);
        debriefings.add(SUCCESS_A_DEF);
        debriefings.add(SUCCESS_B_DEF);
        debriefings.add(SUCCESS_C_DEF);
        debriefings.add(SUCCESS_D_DEF);
    }
    
    /**
     * Constructor for loading a level pack.
     * @param res resource object for level pack INI
     * @throws ResourceException
     */
    public LevelPack(final Resource res) throws ResourceException {
        // extract path from descriptor file
        path = ToolBox.getParent(res.getOriginalPath());
        // load the descriptor file
        Props props = new Props();
        if (!props.load(res)) {
            throw new ResourceException(res.toString());
        }
        // read name
        name = props.get("name", StringUtils.EMPTY);
        allLevelsUnlocked = props.getBoolean("allLevelsUnlocked", false);
        // read mods
        String[] modsStr = props.getArray("mods", ArrayUtils.EMPTY_STRING_ARRAY);
        mods = Arrays.stream(modsStr).map(modStr -> "mods/" + modStr).collect(Collectors.toList());
        // read code seed
        codeSeed = props.get("codeSeed", StringUtils.EMPTY).trim().toUpperCase(Locale.ROOT);
        // read code level offset
        codeOffset = props.getInt("codeOffset", 0);
        // read max falling distance
        maxFallDistance = props.getInt("maxFallDistance", 126);
        // read music files
        List<String> music = new ArrayList<>(64);
        String track;
        int idx = 0;
        do {
            track = props.get("music_" + (idx++), StringUtils.EMPTY);
            if (!track.isEmpty()) {
                music.add(track);
            }
        } while (!track.isEmpty());
        // read debriefings
        debriefings.add(props.get("failureA", FAILURE_A_DEF));
        debriefings.add(props.get("failureB", FAILURE_B_DEF));
        debriefings.add(props.get("failureC", FAILURE_C_DEF));
        debriefings.add(props.get("failureD", FAILURE_D_DEF));
        debriefings.add(props.get("failureE", FAILURE_E_DEF));
        debriefings.add(props.get("successA", SUCCESS_A_DEF));
        debriefings.add(props.get("successB", SUCCESS_B_DEF));
        debriefings.add(props.get("successC", SUCCESS_C_DEF));
        debriefings.add(props.get("successD", SUCCESS_D_DEF));
        // read ratings
        idx = 0;
        String rating;
        do {
            rating = props.get("level_" + idx, StringUtils.EMPTY);
            idx++;
            if (!rating.isEmpty()) {
                ratings.add(rating);
            }
        } while (!rating.isEmpty());
        // read levels
        String[] levelStr;
        for (ListIterator<String> lit = ratings.listIterator(); lit.hasNext(); ) {
            int r = lit.nextIndex();
            idx = 0;
            List<LevelInfo> levels = new ArrayList<>(64);
            rating = lit.next().trim().toLowerCase(Locale.ROOT);
            do {
                levelStr = props.getArray("level_" + r + "_" + idx, null);
                if (levelStr == null) {
                    levelStr = props.getArray(rating + "_" + idx, null);
                }
                // filename, music number
                if (levelStr != null && levelStr.length >= 2) {
                    LevelInfo info = new LevelInfo(path + levelStr[0],
                            music.get(ToolBox.parseInt(levelStr[1])));
                    levels.add(info);
                }
                idx++;
            } while (levelStr != null && levelStr.length >= 2);
            lvlInfo.add(levels);
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
     * Return ratings as string list.
     * @return ratings as string list
     */
    public List<String> getRatings() {
        return Collections.unmodifiableList(ratings);
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
    
    public List<String> getDebriefings() {
        return Collections.unmodifiableList(debriefings);
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
        return lvlInfo.get(rating).get(level);
    }
    
    /**
     * Set level info for a certain level.
     * @param rating rating
     * @param level level number
     * @param li new LevelInfo for the given level
     */
    public void setInfo(final int rating, final int level, final LevelInfo li) {
        lvlInfo.get(rating).set(level, li);
    }
    
    /**
     * Add a level to the end of a rating.
     * @param rating rating
     * @param li LevelInfo for the new level
     */
    public void addLevel(final int rating, final LevelInfo li) {
        lvlInfo.get(rating).add(li);
    }
    
    /**
     * Remove a level from a rating.
     * @param rating rating
     * @param level index of the level to remove
     */
    public void removeLevel(final int rating, final int level) {
        lvlInfo.get(rating).remove(level);
    }
    
    /**
     * Add a rating to this level pack.
     * @param rating name of rating
     * @param li LevelInfo for each level
     */
    public void addRating(final String rating, final List<LevelInfo> li) {
        ratings.add(rating);
        lvlInfo.add(li);
    }
    
    /**
     * Remove a rating from this level pack.
     * @param rating index of rating to remove
     */
    public void removeRating(final int rating) {
        ratings.remove(rating);
        lvlInfo.remove(rating);
    }
    
    /**
     * Return all levels for a rating
     * @param rating index of rating
     * @return level names as string list
     */
    public List<String> getLevels(final int rating) {
        return lvlInfo.get(rating).stream().map(li -> li.getName().trim()).collect(Collectors.toList());
    }
    
    public int getRatingCount() {
        return lvlInfo.size();
    }
    
    public int getLevelCount(final int rating) {
        return lvlInfo.get(rating).size();
    }
    
    public List<String> getModPaths() {
        return Collections.unmodifiableList(mods);
    }
}