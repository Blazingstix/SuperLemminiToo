package lemmini.sound;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lemmini.game.Core;
import lemmini.game.LemmException;
import lemmini.game.Resource;
import lemmini.game.ResourceException;
import org.apache.commons.io.FilenameUtils;

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
 * Play background music. Abstraction layer for ModMusic and MidiMusic.
 *
 * @author Volker Oth
 */
public class Music {
    
    /** music type */
    public static enum Type {
        /** no type */
        NONE,
        /** MIDI music */
        MIDI,
        /** MOD music */
        MOD,
        /** Wave music */
        WAVE
    }
    
    /** music type */
    private static Type type;
    /** currently playing? */
    private static boolean playing;
    /** MOD music object */
    private static ModMusic modMusic;
    /** MIDI music object */
    private static MidiMusic midiMusic;
    /** Wave music object */
    private static WaveMusic waveMusic;
    /** music gain */
    private static double gain = 1.0;
    private static MusicPlayer musicPlayer;
    private static boolean midiAvailable;
    
    
    /**
     * Initialization.
     */
    public static void init() {
        type = Type.NONE;
        playing = false;
        modMusic = new ModMusic();
        waveMusic = new WaveMusic();
        try {
            midiMusic = new MidiMusic();
            midiAvailable = true;
        } catch (LemmException e) {
            midiAvailable = false;
        }
    }
    
    /**
     * Load music file.
     * @param fName file name
     * @throws ResourceException
     * @throws LemmException
     */
    public static void load(final String fName) throws ResourceException, LemmException {
        close();
        playing = false;
        Resource res = Core.findResource(fName, true, Core.MUSIC_EXTENSIONS);
        switch (FilenameUtils.getExtension(res.getFileName()).toLowerCase(Locale.ROOT)) {
            case "mid":
                if (!midiAvailable) {
                    throw new LemmException("MIDI not supported.");
                }
                musicPlayer = midiMusic;
                type = Type.MIDI;
                break;
            case "mod":
            case "s3m":
            case "xm":
                musicPlayer = modMusic;
                type = Type.MOD;
                break;
            default:
                musicPlayer = waveMusic;
                type = Type.WAVE;
                break;
        }
        musicPlayer.load(res, true);
    }
    
    /**
     * Get file name of a random track.
     * @param style
     * @param specialStyle
     * @return file name of a random track
     */
    public static String getRandomTrack(final String style, final String specialStyle) {
        if (!specialStyle.isEmpty()) {
            List<String> musicList = Core.searchForResources("music/special/", true, Core.MUSIC_EXTENSIONS);
            for (String music : musicList) {
                if (specialStyle.toLowerCase(Locale.ROOT).equals(FilenameUtils.removeExtension(music).toLowerCase(Locale.ROOT))) {
                    return "special/" + music;
                }
            }
        }
        
        if (!style.isEmpty()) {
            List<String> musicList = Core.searchForResources("music/" + style + "/", true, Core.MUSIC_EXTENSIONS).stream()
                    .map(FilenameUtils::removeExtension).map(music -> music.toLowerCase(Locale.ROOT))
                    .filter(music -> !music.endsWith("_intro")).distinct().collect(Collectors.toList());
            if (musicList.size() > 0) {
                double r = Math.random() * musicList.size();
                return style + "/" + musicList.get((int) r);
            }
        }
        
        List<String> musicList = Core.searchForResources("music/", true, Core.MUSIC_EXTENSIONS).stream()
                .map(FilenameUtils::removeExtension).map(music -> music.toLowerCase(Locale.ROOT))
                .filter(music -> !music.endsWith("_intro")).distinct().collect(Collectors.toList());
        double r = Math.random() * musicList.size();
        return musicList.get((int) r);
    }
    
    /**
     * Play music.
     */
    public static void play() {
        if (musicPlayer != null) {
            musicPlayer.play();
            playing = true;
        }
    }
    
    /**
     * Stop music.
     */
    public static void stop() {
        if (musicPlayer != null) {
            musicPlayer.stop();
        }
        playing = false;
    }
    
    /**
     * Close music.
     */
    public static void close() {
        if (musicPlayer != null) {
            musicPlayer.close();
        }
        playing = false;
    }
    
    /**
     * Check if music is currently playing
     * @return true if music is currently playing, else false
     */
    public static boolean isPlaying() {
        return playing;
    }
    
    /**
     * Get current music gain (1.0=100%)
     * @return current music gain (1.0=100%)
     */
    public static double getGain() {
        return gain;
    }
    
    /**
     * Set music gain
     * @param gn gain (1.0=100%)
     */
    public static void setGain(final double gn) {
        gain = gn;
        if (musicPlayer != null) {
            musicPlayer.setGain(gain);
        }
    }
    
    /**
     * Get current music type.
     * @return music type
     */
    public static Type getType() {
        return type;
    }
}
