package lemmini.sound;

import java.io.File;
import java.io.FileFilter;
import java.text.Normalizer;
import java.text.Normalizer;
import java.util.Locale;
import lemmini.game.Core;
import lemmini.game.LemmException;
import lemmini.game.ResourceException;

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
        /** Vorbis music */
        VORBIS,
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
    /** Vorbis music object */
    //private static VorbisMusic vorbisMusic;
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
        //vorbisMusic = new VorbisMusic();
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
        String fName2 = Core.findResource(fName, Core.MUSIC_EXTENSIONS);
        if (fName2 == null) {
            throw new ResourceException(fName);
        }
        if (fName2.endsWith(".mid")) {
            if (!midiAvailable) {
                throw new LemmException("MIDI not supported.");
            }
            musicPlayer = midiMusic;
            type = Type.MIDI;
        } else if (fName2.endsWith(".xm") || fName2.endsWith(".s3m") || fName2.endsWith(".mod")) {
            musicPlayer = modMusic;
            type = Type.MOD;
        //} else if (fName2.endsWith(".ogg")) {
        //    musicPlayer = vorbisMusic;
        //    type = Type.VORBIS;
        } else {
            musicPlayer = waveMusic;
            type = Type.WAVE;
        }
        musicPlayer.load(fName2, true);
    }

    /**
     * Get file name of a random track.
     * @param style
     * @param specialStyle
     * @return file name of a random track
     */
    public static String getRandomTrack(final String style, final String specialStyle) {
        File dir = new File(Core.getResourcePath(), "music");
        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(final File f) {
                if (!f.isFile()) {
                    return false;
                }
                for (String ext : Core.MUSIC_EXTENSIONS) {
                    String lowercaseName = f.getName().toLowerCase(Locale.ROOT);
                    if (lowercaseName.endsWith("." + ext) && !lowercaseName.endsWith("_intro." + ext)) {
                        return true;
                    }
                }
                return false;
            }
        };
        
        if (specialStyle != null && !specialStyle.isEmpty()) {
            File dir2 = new File(dir, "special");
            File[] files = dir2.listFiles(filter);
            for (File file : files) {
                String name = Core.removeExtension(file.getName());
                if (specialStyle.equalsIgnoreCase(name)) {
                    return "special/" + file.getName();
                }
            }
        }
        
        if (style != null && !style.isEmpty()) {
            File dir2 = new File(dir, style);
            File[] files = dir2.listFiles(filter);
            if (files != null && files.length > 0) {
                double r = Math.random() * files.length;
                return style + "/" + files[(int) r].getName();
            }
        }
        
        File[] files = dir.listFiles(filter);
        double r = Math.random() * files.length;
        return files[(int) r].getName();
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
