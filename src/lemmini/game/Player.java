package lemmini.game;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lemmini.tools.Props;

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
 * Stores player progress.
 * @author Volker Oth
 */
public class Player {

    /** property class to store player settings persistently */
    private Props props;
    /** name of the INI file used for persistence */
    private Path iniFilePath;
    /** used to store level progress */
    private Map<String, LevelGroup> lvlGroups;
    /** cheat mode enabled? */
    private boolean cheat;
    /** player's name */
    private String name;

    /**
     * Constructor.
     * @param n player's name
     */
    public Player(final String n) {
        name = n;
        lvlGroups = new HashMap<>();
        // read main ini file
        props = new Props();
        // create players directory if it doesn't exist
        Path dest = Core.resourcePath.resolve("players");
        try {
            Files.createDirectories(dest);
        } catch (IOException ex) {
        }
        iniFilePath = Core.resourcePath.resolve("players").resolve(name + ".ini");

        if (props.load(iniFilePath)) { // might exist or not - if not, it's created
            // file exists, now extract entries
            for (int idx = 0; true; idx++) {
                // first string is the level group key identifier
                // second string is a BigInteger used as bitfield to store available levels
                String[] s = props.getArray("group" + idx, null);
                if (s == null || s.length != 2 || s[0] == null) {
                    break;
                }
                
                BigInteger unlockedLevels = new BigInteger(s[1]);
                Map<Integer, LevelRecord> levelRecords = new HashMap<>();
                for (int j = 0; j < unlockedLevels.bitLength(); j++) {
                    if (j == 0 || unlockedLevels.testBit(j)) {
                        boolean completed = props.getBoolean("group" + idx + "_level" + j + "_completed", false);
                        if (completed) {
                            int lemmingsSaved = props.getInt("group" + idx + "_level" + j + "_lemmingsSaved", -1);
                            int skillsUsed = props.getInt("group" + idx + "_level" + j + "_skillsUsed", -1);
                            int timeElapsed = props.getInt("group" + idx + "_level" + j + "_timeElapsed", -1);
                            int score = props.getInt("group" + idx + "_level" + j + "_score", -1);
                            levelRecords.put(j, new LevelRecord(completed, lemmingsSaved, skillsUsed, timeElapsed, score));
                        } else {
                            levelRecords.put(j, LevelRecord.BLANK_LEVEL_RECORD);
                        }
                    }
                }
                lvlGroups.put(s[0], new LevelGroup(levelRecords));
            }
        }

        // cheat mode
        cheat = false;
    }

    /**
     * Enable cheat mode for this player.
     */
    public void enableCheatMode() {
        cheat = true;
    }

    /**
     * Store player's progress.
     */
    public void store() {
        Set<String> groupKeys = lvlGroups.keySet();
        int idx = 0;
        for (String s : groupKeys) {
            LevelGroup lg = lvlGroups.get(s);
            String sout = s + ", " + lg.getBitField();
            props.set("group" + idx, sout);
            Set<Integer> availableLevels = lg.levelRecords.keySet();
            for (Integer lvlNum : availableLevels) {
                LevelRecord lr = lg.levelRecords.get(lvlNum);
                if (lr.isCompleted()) {
                    props.setBoolean("group" + idx + "_level" + lvlNum + "_completed", true);
                    props.setInt("group" + idx + "_level" + lvlNum + "_lemmingsSaved", lr.getLemmingsSaved());
                    props.setInt("group" + idx + "_level" + lvlNum + "_skillsUsed", lr.getSkillsUsed());
                    props.setInt("group" + idx + "_level" + lvlNum + "_timeElapsed", lr.getTimeElapsed());
                    props.setInt("group" + idx + "_level" + lvlNum + "_score", lr.getScore());
                }
            }
            idx++;
        }
        props.save(iniFilePath);
    }

    /**
     * Allow a level to be played.
     * @param pack level pack
     * @param rating rating
     * @param num level number
     */
    public void setAvailable(final String pack, final String rating, final int num) {
        String id = LevelPack.getID(pack, rating);
        LevelGroup lg = lvlGroups.get(id);
        if (lg == null) {
            // first level is always available
            Map<Integer, LevelRecord> records = new HashMap<>();
            records.put(0, LevelRecord.BLANK_LEVEL_RECORD);
            lg = new LevelGroup(records);
            lvlGroups.put(id, lg);
        }
        if (!lg.levelRecords.containsKey(num)) {
            // add level record to level group
            lg.levelRecords.put(num, LevelRecord.BLANK_LEVEL_RECORD);
        }
    }

    /**
     * Check if player is allowed to play a level.
     * @param pack level pack
     * @param rating rating
     * @param num level number
     * @return true if allowed, false if not
     */
    public boolean isAvailable(final String pack, final String rating, final int num) {
        if (isCheat()) {
            return true;
        }
        String id = LevelPack.getID(pack, rating);
        LevelGroup lg = lvlGroups.get(id);
        if (lg == null) {
            return num == 0; // first level is always available
        }
        return (lg.levelRecords.containsKey(num));
    }
    
    public void setLevelRecord(final String pack, final String rating, final int num, final LevelRecord record) {
        String id = LevelPack.getID(pack, rating);
        LevelGroup lg = lvlGroups.get(id);
        if (lg == null) {
            // first level is always available
            Map<Integer, LevelRecord> records = new HashMap<>();
            records.put(0, LevelRecord.BLANK_LEVEL_RECORD);
            lg = new LevelGroup(records);
            lvlGroups.put(id, lg);
        }
        LevelRecord oldRecord = lg.levelRecords.get(num);
        if (oldRecord != null && record.isCompleted()) {
            if (oldRecord.isCompleted()) {
                lg.levelRecords.put(num, new LevelRecord(
                        true,
                        StrictMath.max(oldRecord.getLemmingsSaved(), record.getLemmingsSaved()),
                        StrictMath.min(oldRecord.getSkillsUsed(), record.getSkillsUsed()),
                        StrictMath.min(oldRecord.getTimeElapsed(), record.getTimeElapsed()),
                        StrictMath.max(oldRecord.getScore(), record.getScore())));
            } else {
                lg.levelRecords.put(num, record);
            }
        }
    }
    
    public LevelRecord getLevelRecord(final String pack, final String rating, final int num) {
        String id = LevelPack.getID(pack, rating);
        LevelGroup lg = lvlGroups.get(id);
        if (lg == null || !lg.levelRecords.containsKey(num)) {
            return LevelRecord.BLANK_LEVEL_RECORD;
        } else {
            return lg.levelRecords.get(num);
        }
    }

    /**
     * Get player's name.
     * @return player's name
     */
    public String getName() {
        return name;
    }

    /**
     * Get cheat state.
     * @return true if cheat is enabled
     */
    public boolean isCheat() {
        return cheat;
    }
    
    private class LevelGroup {

        private final Map<Integer, LevelRecord> levelRecords;
        
        private LevelGroup(Map<Integer, LevelRecord> levelRecords) {
            this.levelRecords = levelRecords;
        }
        
        private BigInteger getBitField() {
            Set<Integer> availableLevels = levelRecords.keySet();
            BigInteger bf = BigInteger.ZERO;
            for (Integer lvlNum : availableLevels) {
                bf = bf.setBit(lvlNum);
            }
            return bf;
        }
    }
}