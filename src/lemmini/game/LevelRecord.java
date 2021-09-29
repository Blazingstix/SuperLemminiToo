/*
 * Copyright 2014 Ryan Sakowski.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package lemmini.game;

/**
 * Stores records for a level.
 * @author Ryan Sakowski
 */
public class LevelRecord {
    
    public static final LevelRecord BLANK_LEVEL_RECORD = new LevelRecord(false, 0, Integer.MAX_VALUE, Integer.MAX_VALUE, 0);
    
    private final boolean completed;
    private final int lemmingsSaved;
    private final int skillsUsed;
    private final int timeElapsed;
    private final int score;

    public LevelRecord(boolean completed, int lemmingsSaved, int skillsUsed, int timeElapsed, int score) {
        this.completed = completed;
        this.lemmingsSaved = lemmingsSaved;
        this.skillsUsed = skillsUsed;
        this.timeElapsed = timeElapsed;
        this.score = score;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public int getLemmingsSaved() {
        return lemmingsSaved;
    }
    
    public int getSkillsUsed() {
        return skillsUsed;
    }
    
    public int getTimeElapsed() {
        return timeElapsed;
    }
    
    public int getScore() {
        return score;
    }
}
