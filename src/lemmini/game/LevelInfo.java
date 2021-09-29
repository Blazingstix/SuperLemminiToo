package lemmini.game;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lemmini.sound.Music;
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
 * Storage class to store level info.
 *
 * @author Volker Oth
 */
public class LevelInfo {
    /** level name */
    private String name;
    /** name of music for this level */
    private Path music;
    /** file name of the INI file containing the level information */
    private Path fileName;
    /** release rate: 0 is slowest, 99 is fastest */
    private int releaseRate;
    /** number of Lemmings in this level (maximum 0x0072 in original LVL format) */
    private int numLemmings;
    /** number of Lemmings to rescue: should be less than or equal to number of Lemmings */
    private int numToRescue;
    /** time limit in seconds */
    private int timeLimitSeconds;
    /** number of climbers in this level */
    private int numClimbers;
    /** number of floaters in this level */
    private int numFloaters;
    /** number of bombers in this level */
    private int numBombers;
    /** number of blockers in this level */
    private int numBlockers;
    /** number of builders in this level */
    private int numBuilders;
    /** number of bashers in this level */
    private int numBashers;
    /** number of miners in this level */
    private int numMiners;
    /** number of diggers in this level */
    private int numDiggers;
    private boolean validLevel;
    
    public LevelInfo() {
        name = "";
        music = Paths.get("");
        fileName = Paths.get("");
        releaseRate = 0;
        numLemmings = 0;
        numToRescue = 0;
        timeLimitSeconds = 0;
        numClimbers = 0;
        numFloaters = 0;
        numBombers = 0;
        numBlockers = 0;
        numBuilders = 0;
        numBashers = 0;
        numMiners = 0;
        numDiggers = 0;
        validLevel = false;
    }
    
    public LevelInfo(Path fname, Path newMusic) {
        fileName = fname;
        music = newMusic;
        name = "";
        releaseRate = 0;
        numLemmings = 0;
        numToRescue = 0;
        timeLimitSeconds = 0;
        numClimbers = 0;
        numFloaters = 0;
        numBombers = 0;
        numBlockers = 0;
        numBuilders = 0;
        numBashers = 0;
        numMiners = 0;
        numDiggers = 0;
        validLevel = false;
        
        try (Reader r = ToolBox.getBufferedReader(fname)) {
            r.mark(5);
            if (ToolBox.checkFileID(r, "# LVL")) {
                r.reset();
                Props props = new Props();
                props.load(r);
                String mainLevel = props.get("mainLevel", "");
                Props props2;
                if (!mainLevel.isEmpty()) {
                    props2 = new Props();
                    Path fname2 = fname.resolveSibling(mainLevel);
                    if (!Files.isRegularFile(fname2)) {
                        return;
                    }
                    try (Reader r2 = ToolBox.getBufferedReader(fname2)) {
                        r2.mark(5);
                        if (ToolBox.checkFileID(r2, "# LVL")) {
                            r2.reset();
                            if (!props2.load(fname2)) {
                                return;
                            }
                        }
                    }
                } else {
                    props2 = props;
                }
                name = props.get("name", props2.get("name", ""));
                if (music == null) {
                    String style = props2.get("style", null);
                    String specialStyle = props2.get("specialStyle", null);
                    music = Music.getRandomTrack(style, specialStyle);
                }
                releaseRate = props.getInt("releaseRate", props2.getInt("releaseRate", 0));
                numLemmings = props.getInt("numLemmings", props2.getInt("numLemmings", 1));
                numToRescue = props.getInt("numToRescue", props2.getInt("numToRescue", 0));
                timeLimitSeconds = props.getInt("timeLimitSeconds", Integer.MIN_VALUE);
                if (timeLimitSeconds == Integer.MIN_VALUE) {
                    int timeLimit = props.getInt("timeLimit", Integer.MIN_VALUE);
                    if (props2 != props && timeLimit == Integer.MIN_VALUE) {
                        timeLimitSeconds = props2.getInt("timeLimitSeconds", Integer.MIN_VALUE);
                        if (timeLimitSeconds == Integer.MIN_VALUE) {
                            timeLimit = props2.getInt("timeLimit", Integer.MIN_VALUE);
                            // prevent integer overflow upon conversion to seconds
                            if (timeLimit >= Integer.MAX_VALUE / 60 || timeLimit <= Integer.MIN_VALUE / 60) {
                                timeLimit = 0;
                            }
                            timeLimitSeconds = timeLimit * 60;
                        }
                    } else {
                        // prevent integer overflow upon conversion to seconds
                        if (timeLimit >= Integer.MAX_VALUE / 60 || timeLimit <= Integer.MIN_VALUE / 60) {
                            timeLimit = 0;
                        }
                        timeLimitSeconds = timeLimit * 60;
                    }
                }
                if (timeLimitSeconds == Integer.MAX_VALUE || timeLimitSeconds < 0) {
                    timeLimitSeconds = 0;
                }
                numClimbers = props.getInt("numClimbers", props2.getInt("numClimbers", 0));
                numFloaters = props.getInt("numFloaters", props2.getInt("numFloaters", 0));
                numBombers = props.getInt("numBombers", props2.getInt("numBombers", 0));
                numBlockers = props.getInt("numBlockers", props2.getInt("numBlockers", 0));
                numBuilders = props.getInt("numBuilders", props2.getInt("numBuilders", 0));
                numBashers = props.getInt("numBashers", props2.getInt("numBashers", 0));
                numMiners = props.getInt("numMiners", props2.getInt("numMiners", 0));
                numDiggers = props.getInt("numDiggers", props2.getInt("numDiggers", 0));
                validLevel = true;
            }
        } catch (IOException ex) {
            validLevel = false;
        }
    }

    /**
     * Set the file name
     * @param fileName file name
     */
    public void setFileName(final Path fileName) {
        this.fileName = fileName;
    }

    /**
     * Get the file name.
     * @return file name
     */
    public Path getFileName() {
        return fileName;
    }

    /**
     * Set name of music.
     * @param music name of music
     */
    public void setMusic(final Path music) {
        this.music = music;
    }

    /**
     * Get name of music.
     * @return name of music.
     */
    public Path getMusic() {
        return music;
    }

    /**
     * Set level name.
     * @param name level name
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Get level name.
     * @return level name
     */
    public String getName() {
        return name;
    }
    
    public int getReleaseRate() {
        return releaseRate;
    }
    
    public int getNumLemmings() {
        return numLemmings;
    }
    
    public int getNumToRescue() {
        return numToRescue;
    }
    
    public int getTimeLimit() {
        return timeLimitSeconds;
    }
    
    public int getNumClimbers() {
        return numClimbers;
    }
    
    public int getNumFloaters() {
        return numFloaters;
    }
    
    public int getNumBombers() {
        return numBombers;
    }
    
    public int getNumBlockers() {
        return numBlockers;
    }
    
    public int getNumBuilders() {
        return numBuilders;
    }
    
    public int getNumBashers() {
        return numBashers;
    }
    
    public int getNumMiners() {
        return numMiners;
    }
    
    public int getNumDiggers() {
        return numDiggers;
    }
    
    public boolean isValidLevel() {
        return validLevel;
    }
}
