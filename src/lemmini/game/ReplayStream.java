package lemmini.game;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.JOptionPane;
import lemmini.LemminiFrame;
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
 * Handle replays.
 * @author Volker Oth
 */
public class ReplayStream {
    // event types
    static final int ASSIGN_SKILL = 0;
    static final int MOVE_POS = 1;
    static final int SELECT_SKILL = 2;
    static final int SET_RELEASE_RATE = 3;
    static final int NUKE = 4;
    static final int END = 5;
    
    static final int CURRENT_FORMAT = 1;
    static final String CURRENT_REVISION = "0.98";

    private List<ReplayEvent> events;
    private int replayIndex;
    private int format;
    private String revision;
    private int players;

    /**
     * Constructor.
     */
    public ReplayStream() {
        events = new ArrayList<>(256);
        replayIndex = 0;
        format = 0;
    }

    /**
     * Rewind replay to start position.
     */
    public void rewind() {
        replayIndex = 0;
    }

    /**
     * Get next replay event
     * @param ctr frame counter
     * @return replay event
     */
    ReplayEvent getNext(final int ctr) {
        if (replayIndex >= events.size()) {
            return null;
        }
        ReplayEvent r = events.get(replayIndex);
        /* Note: there can be multiple replay events for one frame.
         * return the next stored event if was stored for a frame
         * smaller or equal to the given frame counter.
         */
        if (ctr >= r.frameCtr) {
            replayIndex++;
            return r;
        }
        return null; /* no more events for this frame */
    }

    /**
     * Clear the replay buffer.
     */
    public void clear() {
        events.clear();
    }

    /**
     * Clear the replay buffer from a certain frame counter.
     * @param ctr frame counter
     */
    public void clearFrom(final int ctr) {
        /* Note: there can be multiple replay events for one frame. */
        for (int i = events.size() - 1; i >= 0; i--) {
            ReplayEvent r = events.get(i);
            if (r.frameCtr > ctr // clearly behind ctr -> erase
                    || r.frameCtr == ctr && i > replayIndex) { // equal to ctr, but after replayIndex -> erase
                events.remove(i);
            } else {
                break;
            }
        }
        replayIndex = 0;
    }

    /**
     * Load replay buffer from file.
     * @param fname file name
     * @return replay information
     * @throws LemmException
     */
    public ReplayLevelInfo load(final Path fname) throws LemmException {
        try (BufferedReader br = ToolBox.getBufferedReader(fname)) {
            List<ReplayEvent> ev = new ArrayList<>(256);
            String line = br.readLine();
            if (!line.equals("#REPLAY NEW")) {
                throw new LemmException("First line of replay does not equal \"#REPLAY NEW\".");
            }
            line = br.readLine();
            if (line.startsWith("#FORMAT ")) {
                format = Integer.parseInt(line.substring(8).trim());
                if (format > CURRENT_FORMAT) {
                    throw new LemmException(String.format("Unsupported replay format: %d", format));
                }
            } else {
                throw new LemmException("Replay file does not specify a format.");
            }
            
            line = br.readLine();
            if (line.startsWith("#REVISION ")) {
                revision = line.substring(10).trim();
            } else {
                throw new LemmException("Replay file does not specify a revision.");
            }
            line = br.readLine();
            if (line.startsWith("#Players ")) {
                players = Integer.parseInt(line.substring(9).trim());
                if (players != 1) {
                    throw new LemmException("Replay file does not contain exactly one player.");
                }
            } else {
                throw new LemmException("Replay file of replay does not specify a player count.");
            }
            // read level info
            line = br.readLine();
            String[] e = line.split(",");
            for (int j = 0; j < e.length; j++) {
                e[j] = e[j].trim();
            }
            if (e.length < 3 || e[0].charAt(0) != '#') {
                throw new LemmException("Replay file of replay does not specify a level.");
            }
            e[0] = Normalizer.normalize(e[0], Normalizer.Form.NFKC);
            ReplayLevelInfo rli = new ReplayLevelInfo();
            rli.setLevelPack(e[0].substring(1));
            rli.setRating(Integer.parseInt(e[1]));
            rli.setLvlNumber(Integer.parseInt(e[2]));
            // read events
            while ((line = br.readLine()) != null) {
                e = line.split(",");
                for (int i = 0; i < e.length; i++) {
                    e[i] = e[i].trim();
                }
                if (e.length < 2) {
                    throw new LemmException("Not enough values in replay event.");
                }

                switch (Integer.parseInt(e[1])) { /* type */
                    case ASSIGN_SKILL:
                        if (e.length < 4) {
                            throw new LemmException("Not enough values in replay event.");
                        }
                        ev.add(new ReplayAssignSkillEvent(Integer.parseInt(e[0]),
                                Lemming.Type.valueOf(e[2]),
                                Integer.parseInt(e[3])));
                        break;
                    case MOVE_POS:
                        if (e.length < 5) {
                            throw new LemmException("Not enough values in replay event.");
                        }
                        ev.add(new ReplayMovePosEvent(Integer.parseInt(e[0]),
                                Integer.parseInt(e[2]),
                                Integer.parseInt(e[3]),
                                Integer.parseInt(e[4])));
                        break;
                    case SELECT_SKILL:
                        if (e.length < 4) {
                            throw new LemmException("Not enough values in replay event.");
                        }
                        ev.add(new ReplaySelectSkillEvent(Integer.parseInt(e[0]),
                                Lemming.Type.valueOf(e[2]),
                                Integer.parseInt(e[3])));
                        break;
                    case SET_RELEASE_RATE:
                        if (e.length < 3) {
                            throw new LemmException("Not enough values in replay event.");
                        }
                        ev.add(new ReplayReleaseRateEvent(Integer.parseInt(e[0]),
                                Integer.parseInt(e[2])));
                        break;
                    case NUKE:
                        ev.add(new ReplayEvent(Integer.parseInt(e[0]), NUKE));
                        break;
                    case END:
                        ev.add(new ReplayEvent(Integer.parseInt(e[0]), END));
                        break;
                    default:
                        throw new LemmException(String.format("Unsupported event found: %s", e[1]));
                }
            }
            events = ev;
            if (!revision.equals(CURRENT_REVISION)) {
                JOptionPane.showMessageDialog(LemminiFrame.getFrame(),
                        "This replay was created with a potentially incompatible version of SuperLemmini. "
                        + "For this reason, the replay might not play properly.",
                        "Load Replay",
                        JOptionPane.WARNING_MESSAGE);
            }
            return rli;
        } catch (IOException | NumberFormatException | ArrayIndexOutOfBoundsException e) {
            throw new LemmException("Error reading replay file.");
        }
    }

    /**
     * Store replay info in a file.
     * @param fname file name
     * @return true if save OK, false otherwise
     */
    public boolean save(final Path fname) {
        try (Writer w = Files.newBufferedWriter(fname, StandardCharsets.UTF_8)) {
            w.write("#REPLAY NEW\r\n");
            w.write("#FORMAT " + CURRENT_FORMAT + "\r\n");
            w.write("#REVISION " + CURRENT_REVISION + "\r\n");
            w.write("#Players 1\r\n");
            LevelPack lp = GameController.getCurLevelPack();
            String name = Normalizer.normalize(lp.getName(), Normalizer.Form.NFKC);
            w.write(String.format("#%s, %d, %d\r\n", name, GameController.getCurRating(), GameController.getCurLevelNumber()));
            for (ReplayEvent r : events) {
                w.write(r + "\r\n"); // will use toString of the correct child object
            }

            return true;
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Add a NUKE event (all lemmings nuked).
     * @param ctr frame counter
     */
    public void addNukeEvent(final int ctr) {
        ReplayEvent event = new ReplayEvent(ctr, NUKE);
        events.add(event);
    }
    
    /**
     * Add an END event.
     * @param ctr frame counter
     */
    public void addEndEvent(final int ctr) {
        removeEndEvent();
        ReplayEvent event = new ReplayEvent(ctr, END);
        events.add(event);
    }

    /**
     * Add ASSIGN_SKILL event (one lemming was assigned a skill).
     * @param ctr frame counter
     * @param skill skill assigned
     * @param lemming Lemming the skill was assigned to
     */
    public void addAssignSkillEvent(final int ctr, final Lemming.Type skill, final int lemming) {
        ReplayAssignSkillEvent event = new ReplayAssignSkillEvent(ctr, skill, lemming);
        events.add(event);
    }

    /**
     * Add SELECT_SKILL event (skill selection button was pressed).
     * @param ctr frame counter
     * @param skill skill selected
     * @param plr player
     */
    public void addSelectSkillEvent(final int ctr, final Lemming.Type skill, final int plr) {
        ReplaySelectSkillEvent event = new ReplaySelectSkillEvent(ctr, skill, plr);
        events.add(event);
    }

    /**
     * Add MOVE_POS event (screen moved left/right).
     * @param ctr frame counter
     * @param xPos new screen X position
     * @param yPos new screen Y position
     * @param plr player
     */
    public void addPosEvent(final int ctr, final int xPos, final int yPos, final int plr) {
        ReplayMovePosEvent event = new ReplayMovePosEvent(ctr, xPos, yPos, plr);
        events.add(event);
    }

    /**
     * Add SET_RELEASE_RATE event (release rate was changed).
     * @param ctr frame counter
     * @param releaseRate new release rate
     */
    public void addReleaseRateEvent(final int ctr, final int releaseRate) {
        ReplayReleaseRateEvent event = new ReplayReleaseRateEvent(ctr, releaseRate);
        events.add(event);
    }
    
    private void removeEndEvent() {
        for (int i = events.size() - 1; i >= 0; i--) {
            ReplayEvent r = events.get(i);
            if (r.type == END) {
                events.remove(i);
            } else {
                break;
            }
        }
    }
}

/**
 * Storage class for one replay event.
 * @author Volker Oth
 */
class ReplayEvent {
    /** frame counter */
    int frameCtr;
    /** event type */
    int type;

    /**
     * Constructor
     * @param ctr frame counter
     * @param t type
     */
    public ReplayEvent(final int ctr, final int t) {
        frameCtr = ctr;
        type = t;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%d, %d", frameCtr, type);
    }
}

/**
 * Storage class for ASSIGN_SKILL event
 * @author Volker Oth
 */
class ReplayAssignSkillEvent extends ReplayEvent {
    /** skill */
    Lemming.Type skill;
    /** Lemming */
    int lemming;

    /**
     * Skill assigned
     * @param ctr Frame counter
     * @param s skill selected
     * @param lem lemming no. that the skill was assigned
     */
    public ReplayAssignSkillEvent(final int ctr, final Lemming.Type s, final int lem) {
        super(ctr, ReplayStream.ASSIGN_SKILL);
        skill = s;
        lemming = lem;
    }

    /* (non-Javadoc)
     * @see Game.ReplayEvent#toString()
     */
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s, %s, %d", super.toString(), skill.name(), lemming);
    }
}

/**
 * Storage class for SELECT_SKILL event.
 * @author Volker Oth
 */
class ReplaySelectSkillEvent extends ReplayEvent {
    Lemming.Type skill;
    int player;

    /**
     * Skill selected
     * @param ctr Frame counter
     * @param s skill selected
     * @param plr player
     */
    public ReplaySelectSkillEvent(final int ctr, final Lemming.Type s, final int plr) {
        super(ctr, ReplayStream.SELECT_SKILL);
        skill = s;
        player = plr;
    }

    /* (non-Javadoc)
     * @see Game.ReplayEvent#toString()
     */
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s, %s, %d", super.toString(), skill.name(), player);
    }
}

/**
 * Storage class for MOVE_POS event.
 * @author Volker Oth
 */
class ReplayMovePosEvent extends ReplayEvent {
    /** screen X position */
    int xPos;
    /** screen Y position */
    int yPos;
    int player;

    /**
     * Screen position changed event
     * @param ctr Frame counter
     * @param x release X position
     * @param y release Y position
     * @param plr player
     */
    public ReplayMovePosEvent(final int ctr, final int x, final int y, final int plr) {
        super(ctr, ReplayStream.MOVE_POS);
        xPos = x;
        yPos = y;
        player = plr;
    }

    /* (non-Javadoc)
     * @see Game.ReplayEvent#toString()
     */
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s, %d, %d, %d", super.toString(), xPos, yPos, player);
    }
}

/**
 * Storage class for SET_RELEASE_RATE event.
 * @author Volker Oth
 */
class ReplayReleaseRateEvent extends ReplayEvent {
    int releaseRate;

    /**
     * Release Rate changed event
     * @param ctr Frame counter
     * @param rate release rate value
     */
    public ReplayReleaseRateEvent(final int ctr, final int rate) {
        super(ctr, ReplayStream.SET_RELEASE_RATE);
        releaseRate = rate;
    }

    /* (non-Javadoc)
     * @see Game.ReplayEvent#toString()
     */
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s, %d", super.toString(), releaseRate);
    }
}