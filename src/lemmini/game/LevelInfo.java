package lemmini.game;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import lemmini.tools.Props;
import lemmini.tools.ToolBox;
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
 * Storage class to store level info.
 *
 * @author Volker Oth
 */
public class LevelInfo {
    /** level name */
    private String name;
    /** level author */
    private String author;
    /** name of music for this level */
    private String music;
    /** resource object for the INI file containing the level information */
    private Resource levelRes;
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
        name = StringUtils.EMPTY;
        author = StringUtils.EMPTY;
        music = StringUtils.EMPTY;
        levelRes = new FileResource(StringUtils.EMPTY, StringUtils.EMPTY, Core.resourceTree);
        releaseRate = 0;
        numLemmings = 1;
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
    
    public LevelInfo(String fname, String newMusic) {
        Resource res;
        try {
            res = Core.findResource(fname, false);
        } catch (ResourceException ex) {
            res = null;
        }
        init(res, newMusic);
    }
    
    public LevelInfo(Resource res, String newMusic) {
        init(res, newMusic);
    }
    
    private void init(Resource res, String newMusic) {
        levelRes = res;
        music = newMusic;
        name = StringUtils.EMPTY;
        author = StringUtils.EMPTY;
        releaseRate = 0;
        numLemmings = 1;
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
        
        if (res != null) {
            try (Reader r = levelRes.getBufferedReader()) {
                if (ToolBox.checkFileID(r, "# LVL")) {
                    List<Props> propsList = new ArrayList<>(4);
                    Props props = new Props();
                    props.load(r);
                    propsList.add(props);
                    String mainLevel = props.get("mainLevel", StringUtils.EMPTY);
                    while (!mainLevel.isEmpty()) {
                        Resource levelRes2 = levelRes.getSibling(mainLevel);
                        if (!levelRes2.exists()) {
                            return;
                        }
                        props = new Props();
                        try (Reader r2 = levelRes2.getBufferedReader()) {
                            if (ToolBox.checkFileID(r2, "# LVL")) {
                                if (!props.load(levelRes2)) {
                                    return;
                                }
                            }
                        }
                        propsList.add(props);
                        mainLevel = props.get("mainLevel", StringUtils.EMPTY);
                    }
                    name = Props.get(propsList, "name", StringUtils.EMPTY);
                    author = Props.get(propsList, "author", StringUtils.EMPTY);
                    releaseRate = Props.getInt(propsList, "releaseRate", 0);
                    numLemmings = Props.getInt(propsList, "numLemmings", 1);
                    // sanity check: ensure that there are lemmings in the level to avoid division by 0
                    if (numLemmings <= 0) {
                        numLemmings = 1;
                        return;
                    }
                    numToRescue = Props.getInt(propsList, "numToRescue", 0);
                    for (Props props2 : propsList) {
                        timeLimitSeconds = props2.getInt("timeLimitSeconds", Integer.MIN_VALUE);
                        if (timeLimitSeconds != Integer.MIN_VALUE) {
                            break;
                        }
                        int timeLimit = props2.getInt("timeLimit", Integer.MIN_VALUE);
                        if (timeLimit != Integer.MIN_VALUE) {
                            // prevent integer overflow upon conversion to seconds
                            if (timeLimit >= Integer.MAX_VALUE / 60 || timeLimit <= Integer.MIN_VALUE / 60) {
                                timeLimit = 0;
                            }
                            timeLimitSeconds = timeLimit * 60;
                            break;
                        }
                    }
                    if (timeLimitSeconds == Integer.MAX_VALUE || timeLimitSeconds < 0) {
                        timeLimitSeconds = 0;
                    }
                    numClimbers = Props.getInt(propsList, "numClimbers", 0);
                    numFloaters = Props.getInt(propsList, "numFloaters", 0);
                    numBombers = Props.getInt(propsList, "numBombers", 0);
                    numBlockers = Props.getInt(propsList, "numBlockers", 0);
                    numBuilders = Props.getInt(propsList, "numBuilders", 0);
                    numBashers = Props.getInt(propsList, "numBashers", 0);
                    numMiners = Props.getInt(propsList, "numMiners", 0);
                    numDiggers = Props.getInt(propsList, "numDiggers", 0);
                    validLevel = true;
                }
            } catch (IOException ex) {
                validLevel = false;
            }
        }
    }
    
    /**
     * Get the level's resource object.
     * @return resource object
     */
    public Resource getLevelResource() {
        return levelRes;
    }
    
    /**
     * Set name of music.
     * @param music name of music
     */
    public void setMusic(final String music) {
        this.music = music;
    }
    
    /**
     * Get name of music.
     * @return name of music.
     */
    public String getMusic() {
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
    
    /**
     * Get level author.
     * @return level author
     */
    public String getAuthor() {
        return author;
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
        return validLevel && levelRes != null;
    }
}
