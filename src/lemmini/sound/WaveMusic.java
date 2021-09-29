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

package lemmini.sound;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.*;
import lemmini.game.Core;
import lemmini.game.GameController;
import lemmini.game.LemmException;
import lemmini.game.Resource;
import lemmini.game.ResourceException;

public class WaveMusic implements Runnable, MusicPlayer {
    
    private boolean loopSong;
    private boolean play;
    private SourceDataLine line;
    private DataLine.Info info;
    private Resource file;
    private Resource introFile;
    private AudioInputStream in;
    private AudioInputStream introIn;
    private AudioInputStream din;
    private boolean playIntro;
    private AudioFormat format;
    private Thread waveThread;
    
    @Override
    public void load(final Resource res, final boolean loop) throws ResourceException, LemmException {
        if (waveThread != null) {
            close();
        }
        loopSong = loop;
        try {
            file = res;
            introFile = res.getSibling(Core.appendBeforeExtension(res.getFileName(), "_intro"));
            InputStream tempIn = file.getInputStream();
            if (tempIn.markSupported()) {
                in = AudioSystem.getAudioInputStream(tempIn);
            } else {
                in = AudioSystem.getAudioInputStream(new BufferedInputStream(tempIn));
            }
            if (in != null) {
                format = getDecodeFormat(in.getFormat());
                if (introFile.exists()) {
                    tempIn = introFile.getInputStream();
                    if (tempIn.markSupported()) {
                        introIn = AudioSystem.getAudioInputStream(tempIn);
                    } else {
                        introIn = AudioSystem.getAudioInputStream(new BufferedInputStream(tempIn));
                    }
                    AudioFormat introFormat = getDecodeFormat(introIn.getFormat());
                    if (introFormat.matches(format)) {
                        playIntro = true;
                    } else {
                        playIntro = false;
                        introIn.close();
                    }
                } else {
                    playIntro = false;
                }
                info = new DataLine.Info(SourceDataLine.class, format, GameController.sound.getBufferSize());
            }
            din = AudioSystem.getAudioInputStream(format, playIntro ? introIn : in);
            if (loopSong && !playIntro) {
                din.mark(Integer.MAX_VALUE);
            }
        } catch (FileNotFoundException ex) {
            throw new ResourceException(res);
        } catch (IOException ex) {
            throw new LemmException(res + " (IO exception)");
        } catch (UnsupportedAudioFileException ex) {
            throw new LemmException(res + " (Unsupported Audio File)");
        }
        waveThread = new Thread(this);
        waveThread.start();
    }
    
    private static AudioFormat getDecodeFormat(AudioFormat baseFormat) {
        if (baseFormat.getEncoding() == AudioFormat.Encoding.PCM_SIGNED
                || baseFormat.getEncoding() == AudioFormat.Encoding.PCM_UNSIGNED) {
            return baseFormat;
        } else {
            return new AudioFormat(
                    baseFormat.getSampleRate(),
                    16,
                    baseFormat.getChannels(),
                    true,
                    false);
        }
    }
    
    @Override
    public void run() {
        try {
            line = (SourceDataLine) GameController.sound.getLine(info);
            byte[] data = new byte[line.getBufferSize()];
            line.open();
            line.start();
            setGain(Music.getGain());
            int bytesRead = 0;
            while (bytesRead != -1 && Thread.currentThread() == waveThread) {
                if (play) {
                    bytesRead = din.read(data);
                    if (bytesRead != -1) {
                        line.write(data, 0, bytesRead);
                    } else if (playIntro) {
                        din.close();
                        din = AudioSystem.getAudioInputStream(format, in);
                        if (loopSong) {
                            din.mark(Integer.MAX_VALUE);
                        }
                        playIntro = false;
                        bytesRead = 0;
                    } else if (loopSong) {
                        if (din.markSupported()) {
                            din.reset();
                        } else {
                            din.close();
                            InputStream tempIn = file.getInputStream();
                            if (tempIn.markSupported()) {
                                in = AudioSystem.getAudioInputStream(tempIn);
                            } else {
                                in = AudioSystem.getAudioInputStream(new BufferedInputStream(tempIn));
                            }
                            din = AudioSystem.getAudioInputStream(format, in);
                            din.mark(Integer.MAX_VALUE);
                        }
                        bytesRead = 0;
                    } else {
                        line.drain();
                    }
                } else {
                    line.stop();
                    synchronized (this) {
                        while (!play && Thread.currentThread() == waveThread) {
                            try {
                                wait();
                            } catch (InterruptedException ex) {
                            }
                        }
                    }
                    line.start();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            line.stop();
            line.flush();
            line.close();
            close();
        }
    }
    
    @Override
    public synchronized void stop() {
        play = false;
    }
    
    @Override
    public synchronized void play() {
        play = true;
        notifyAll();
    }
    
    @Override
    public void close() {
        if (waveThread == null) {
            return;
        }
        Thread moribund = waveThread;
        waveThread = null;
        try {
            moribund.interrupt();
            moribund.join();
        } catch (InterruptedException ex) {
        }
        
        try {
            if (introIn != null) {
                introIn.close();
            }
        } catch (IOException ex) {
        }
        try {
            in.close();
        } catch (IOException ex) {
        }
        try {
            din.close();
        } catch (IOException ex) {
        }
    }
    
    @Override
    public void setGain(double gain) {
        if (gain > 2.0) {
            gain = 2.0;
        } else if (gain < 0) {
            gain = 0;
        }
        if (line != null) {
            Sound.setLineGain(line, gain);
        }
    }
}
