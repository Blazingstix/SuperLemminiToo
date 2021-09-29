package lemmini.tools;

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
 * Wrapper class for direct timer access. Implements a nanosecond timer.
 *
 * @author Volker Oth
 */
public class NanosecondTimer {
    
    private long timeBase;
    
    /**
     * Constructor.
     */
    public NanosecondTimer() {
        timeBase = System.nanoTime();
    }
    
    /**
     * Return delta time since last update.
     * @return delta time since last update.
     */
    public long delta() {
        long t = System.nanoTime();
        long delta = t - timeBase;
        // check for inconsistency: external clock setting (e.g. to internet server)
        // might cause it to jump "back" in time
        if (delta < 0) {
            System.out.printf("NanosecondTimer inconsistency detected: %d ns", -delta);
            timeBase = t;
            delta = 0;
        }
        return delta;
    }
    
    /**
     * Return delta time since last update and perform an update.
     * @return delta time since last update.
     */
    public long deltaUpdate() {
        long t = System.nanoTime();
        long delta = t - timeBase;
        // check for inconsistency: external clock setting (e.g. to internet server)
        // might cause it to jump "back" in time
        if (delta < 0) {
            System.out.printf("NanosecondTimer inconsistency detected: %d ns", -delta);
            delta = 0;
        }
        timeBase = t;
        return delta;
    }
    
    /**
     * Returns true if the given time has passed since the last update.
     * @param dt time delta in nanoseconds
     * @return true if the given time has passed since the last update.
     */
    public boolean timePassed(final long dt) {
        long delta = delta();
        return delta >= dt;
    }
    
    /**
     * Returns true if the given time has passed since the last update and performs an update.
     * @param dt time delta in nanoseconds
     * @return true if the given time has passed since the last update.
     */
    public boolean timePassedUpdate(final long dt) {
        long t = System.nanoTime();
        long delta = delta();
        if (delta >= dt) {
            timeBase = t;
            return true;
        }
        return false;
    }
    
    /**
     * Returns true if the given time has passed since the last update and adds the time delta.
     * @param dt time delta
     * @return true if the given time has passed since the last update.
     */
    public boolean timePassedAdd(final long dt) {
        long delta = delta();
        if (delta >= dt) {
            timeBase += dt;
            return true;
        }
        return false;
    }
    
    /**
     * Updates the internal time base to the current timer value.
     */
    public void update() {
        long t = System.nanoTime();
        timeBase = t;
    }
    
    /**
     * Adds a time delta to the internal time base.
     * @param delta time delta in nanoseconds
     */
    public void update(long delta) {
        timeBase += delta;
    }
    
}
