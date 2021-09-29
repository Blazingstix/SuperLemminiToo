package lemmini.sound;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lemmini.game.Core;
import lemmini.game.LemmException;
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
    public static void load(final Path fName) throws ResourceException, LemmException {
        close();
        playing = false;
        Path fName2 = Core.findResource(fName, Core.MUSIC_EXTENSIONS);
        switch (FilenameUtils.getExtension(fName2.getFileName().toString()).toLowerCase(Locale.ROOT)) {
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
        musicPlayer.load(fName2, true);
    }

    /**
     * Get file name of a random track.
     * @param style
     * @param specialStyle
     * @return file name of a random track
     */
    public static Path getRandomTrack(final String style, final String specialStyle) {
        Path dir = Core.resourcePath.resolve("music");
        DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {
            @Override
            public boolean accept(Path entry) {
                if (!Files.isRegularFile(entry)) {
                    return false;
                }
                for (String ext : Core.MUSIC_EXTENSIONS) {
                    String lowercaseName = entry.getFileName().toString().toLowerCase(Locale.ROOT);
                    if (lowercaseName.endsWith("." + ext) && !lowercaseName.endsWith("_intro." + ext)) {
                        return true;
                    }
                }
                return false;
            }
        };
        
        if (specialStyle != null && !specialStyle.isEmpty()) {
            Path dir2 = dir.resolve("special");
            try (DirectoryStream<Path> files = Files.newDirectoryStream(dir2, filter)) {
                for (Path file : files) {
                    String name = FilenameUtils.removeExtension(file.getFileName().toString());
                    if (specialStyle.equalsIgnoreCase(name)) {
                        return Paths.get("special").resolve(file.getFileName());
                    }
                }
            } catch (IOException ex) {
            }
        }
        
        List<Path> fileList = new ArrayList<>(128);
        if (style != null && !style.isEmpty()) {
            Path dir2 = dir.resolve(style);
            try (DirectoryStream<Path> files = Files.newDirectoryStream(dir2, filter)) {
                for (Path file : files) {
                    fileList.add(file);
                }
            } catch (IOException ex) {
            }
            if (fileList.size() > 0) {
                double r = Math.random() * fileList.size();
                return Paths.get(style).resolve(fileList.get((int) r).getFileName());
            }
        }
        
        fileList.clear();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, filter)) {
            for (Path file : files) {
                fileList.add(file);
            }
        } catch (IOException ex) {
        }
        double r = Math.random() * fileList.size();
        return fileList.get((int) r).getFileName();
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
