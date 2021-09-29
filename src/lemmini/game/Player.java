package lemmini.game;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
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
    private String iniFileStr;
    /** used to store level progress */
    private Map<String, BigInteger> lvlGroup;
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
        lvlGroup = new HashMap<>();
        // read main ini file
        props = new Props();
        // create players directory if it doesn't exist
        Path dest = Paths.get(Core.resourcePath, "players");
        try {
            Files.createDirectories(dest);
        } catch (IOException ex) {
        }
        iniFileStr = Core.resourcePath + "players/" + name + ".ini";

        if (props.load(iniFileStr)) { // might exist or not - if not, it's created
            // file exists, now extract entries
            for (int idx = 0; true; idx++) {
                String[] s = props.getArray("group" + idx, null);
                if (s == null || s.length != 2 || s[0] == null) {
                    break;
                }
                // first string is the level group key identifier
                // second string is a BigInteger used as bitfield to store available levels
                lvlGroup.put(s[0], new BigInteger(s[1]));
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
        Set<String> k = lvlGroup.keySet();
        int idx = 0;
        for (String s : k) {
            BigInteger bf = lvlGroup.get(s);
            String sout = s + ", " + bf.toString();
            props.set("group" + (idx++), sout);
        }
        props.save(iniFileStr);
    }

    /**
     * Allow a level to be played.
     * @param pack level pack
     * @param rating rating
     * @param num level number
     * @return updated bitfield
     */
    public BigInteger setAvailable(final String pack, final String rating, final int num) {
        // get current bitfield
        String id = LevelPack.getID(pack, rating);
        if (!Normalizer.isNormalized(id, Normalizer.Form.NFKC)) {
            id = Normalizer.normalize(id, Normalizer.Form.NFKC);
        }
        BigInteger bf = lvlGroup.get(id);
        if (bf == null) {
            bf = BigInteger.ONE; // first level is always available
        }
        bf = bf.setBit(num); // set bit in bitfield (just overwrite existing bit)
        // store new value
        lvlGroup.put(id, bf);
        return bf;
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
        // get current bitfield
        String id = LevelPack.getID(pack, rating);
        if (!Normalizer.isNormalized(id, Normalizer.Form.NFKC)) {
            id = Normalizer.normalize(id, Normalizer.Form.NFKC);
        }
        BigInteger bf = lvlGroup.get(id);
        if (bf == null) {
            bf = BigInteger.ONE; // first level is always available
        }
        return (bf.testBit(num));
    }

    /**
     * Check if player is allowed to play a level.
     * @param bf bitfield containing the approval information for all levels of this pack/rating
     * @param num number of level
     * @return true if allowed, false if not
     */
    public boolean isAvailable(final BigInteger bf, final int num) {
        if (isCheat()) {
            return true;
        }
        return (bf.testBit(num));
    }

    /**
     * Get bitfield containing the approval information for all levels of this pack/rating.
     * @param pack level pack
     * @param rating rating
     * @return bitfield containing the approval information for all levels of this pack/rating
     */
    public BigInteger getBitField(final LevelPack pack, final String rating) {
        if (pack.getAllLevelsUnlocked() || isCheat()) {
            return new BigInteger("18446744073709551615"); // 0xffffffffffffffff (8 bytes with all bits set)
        }

        String id = LevelPack.getID(pack.getName(), rating);
        if (!Normalizer.isNormalized(id, Normalizer.Form.NFKC)) {
            id = Normalizer.normalize(id, Normalizer.Form.NFKC);
        }
        BigInteger bf = lvlGroup.get(id);
        if (bf == null) {
            return BigInteger.ONE;
        }
        return bf;
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
}