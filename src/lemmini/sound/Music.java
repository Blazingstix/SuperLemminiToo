package lemmini.sound;

import java.io.File;
import java.io.FileFilter;
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
    /** array of file names */
    private static String[] musicFiles;
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

        // read available music files for random mode
        File dir = new File(Core.getResourcePath(), "music");
        File[] files = dir.listFiles(new MusicFileFilter());
        musicFiles = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            musicFiles[i] = files[i].getName();
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
     * @return file name of a random track
     */
    public static String getRandomTrack() {
        double r = Math.random() * musicFiles.length;
        return musicFiles[(int) r];
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

/**
 * File filter for music files.
 * @author Volker Oth
 */
class MusicFileFilter implements FileFilter {
    /* (non-Javadoc)
     * @see java.io.FileFilter#accept(java.io.File)
     */
    @Override
    public boolean accept(final File f) {
        if (!f.isFile()) {
            return false;
        }
        for (String ext : Core.MUSIC_EXTENSIONS) {
            if (f.getName().toLowerCase(Locale.ENGLISH).endsWith("." + ext)) {
                return true;
            }
        }
        return false;
    }
}

