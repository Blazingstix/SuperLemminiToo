package lemmini.sound;

import ibxm.Channel;
import ibxm.IBXM;
import ibxm.Module;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import lemmini.game.GameController;
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
 * Class to play MOD music.
 *
 * @author Volker Oth
 */
public class ModMusic implements Runnable, MusicPlayer {
    
    /** object to play MODs */
    private IBXM ibxm;
    /** flag: loop the song */
    private boolean loopSong;
    /** flag: currently playing */
    private boolean play;
    /** thread for playing */
    private Thread modThread;
    /** data line used to play samples */
    private SourceDataLine line;

    /**
     * Load MOD file, initialize player.
     * @param fn file name
     * @throws ResourceException
     * @throws LemmException
     */
    @Override
    public void load(final Path fn, final boolean loop) throws ResourceException, LemmException {
        if (modThread != null) {
            close();
        }
        loopSong = loop;
        byte[] songData;
        try {
            songData = Files.readAllBytes(fn);
        } catch (FileNotFoundException ex) {
            throw new ResourceException(fn.toString());
        } catch (IOException ex) {
            throw new LemmException(fn + " (IO exception)");
        }
        Module module = new Module(songData);
        int sampleRate = Math.min(Math.max((int) GameController.sound.getSampleRate(), 8000), 128000);
        ibxm = new IBXM(module, sampleRate);
        switch (GameController.sound.getResamplingQuality()) {
            case CUBIC:
                ibxm.setInterpolation(Channel.SINC);
                break;
            case LINEAR:
            default:
                ibxm.setInterpolation(Channel.LINEAR);
                break;
            case NEAREST:
                ibxm.setInterpolation(Channel.NEAREST);
                break;
        }
        modThread = new Thread(this);
        modThread.start();
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     *
     *     Begin playback.
     *    This method will return once the song has finished, or stop has been called.
     */
    @Override
    public void run() {
        try {
            AudioFormat af = new AudioFormat(ibxm.getSampleRate(), 16, 2, true, false);
            int bufferSize = Math.max(GameController.sound.getBufferSize() / 2, ibxm.getMixBufferLength());
            if (bufferSize % 2 > 0) {
                bufferSize += 2 - bufferSize % 2;
            }
            DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, af, bufferSize * 2);
            line = (SourceDataLine) GameController.sound.getLine(lineInfo);
            int[] ibuf = new int[bufferSize];
            byte[] obuf = new byte[bufferSize * 2];
            line.open();
            line.start();
            setGain(Music.getGain());
            int songlen = ibxm.calculateSongDuration();
            int remain = songlen;
            while (remain > 0 && Thread.currentThread() == modThread) {
                if (play) {
                    int count = ibxm.getAudio(ibuf);
                    if (count > remain) {
                        count = remain;
                    }
                    for (int ix = 0; ix < count * 2; ix++) {
                        int ox = ix * 2;
                        if (ibuf[ix] > Short.MAX_VALUE) {
                            ibuf[ix] = Short.MAX_VALUE;
                        } else if (ibuf[ix] < Short.MIN_VALUE) {
                            ibuf[ix] = Short.MIN_VALUE;
                        }
                        obuf[ox]     = (byte)  ibuf[ix];
                        obuf[ox + 1] = (byte) (ibuf[ix] >> 8);
                    }
                    line.write(obuf, 0, count * 4);
                    remain -= count;
                    if (remain == 0){
                        if (loopSong) {
                            remain = songlen;
                        } else {
                            line.drain();
                        }
                    }
                    Thread.yield();
                } else {
                    try {
                        line.flush();
                        Thread.sleep(40);
                    } catch (InterruptedException ex) {
                    }
                }
            }
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        } finally {
            line.stop();
            line.flush();
            line.close();
        }
    }

    /**
     * Instruct the run() method to finish playing and return.
     */
    @Override
    public void stop() {
        if (modThread != null) {
            modThread.interrupt();
        }
        play = false;
    }

    /**
     * Instruct the run() method to resume playing.
     */
    @Override
    public void play() {
        if (modThread != null) {
            modThread.interrupt();
        }
        play = true;
    }

    /**
     * Kills the thread.
     */
    @Override
    public void close() {
        if (modThread == null) {
            return;
        }
        Thread moribund = modThread;
        modThread = null;
        try {
            moribund.interrupt();
            moribund.join();
        } catch (InterruptedException ex) {
        }
    }

    /**
     * Set gain (volume) of MOD output
     * @param gn gain factor: 0.0 (off) - 1.0 (full volume) - 2.0 (double volume)
     */
    @Override
    public void setGain(double gain) {
        if (gain > 2.0) {
            gain = 2.0;
        } else if (gain < 0.0) {
            gain = 0.0;
        }
        if (line != null) {
            Sound.setLineGain(line, gain);
        }
    }
}


