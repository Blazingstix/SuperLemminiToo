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

import java.io.*;
import javax.sound.sampled.*;
import lemmini.game.GameController;
import lemmini.game.LemmException;
import lemmini.game.ResourceException;

public class WaveMusic implements Runnable, MusicPlayer {
    
    private boolean loopSong;
    private boolean play;
    private SourceDataLine line;
    private DataLine.Info info;
    private File file;
    private AudioInputStream in;
    private AudioFormat format;
    private Thread waveThread;

    @Override
    public void load(final String fn, final boolean loop) throws ResourceException, LemmException {
        if (waveThread != null) {
            close();
        }
        loopSong = loop;
        try {
            file = new File(fn);
            in = AudioSystem.getAudioInputStream(new BufferedInputStream(new FileInputStream(file)));
            if (in != null) {
                AudioFormat baseFormat = in.getFormat();
                if (baseFormat.getEncoding() == AudioFormat.Encoding.PCM_SIGNED
                        || baseFormat.getEncoding() == AudioFormat.Encoding.PCM_UNSIGNED) {
                    format = baseFormat;
                } else {
                    format = new AudioFormat(
                            baseFormat.getSampleRate(),
                            16,
                            baseFormat.getChannels(),
                            true,
                            false);
                }
                info = new DataLine.Info(SourceDataLine.class, format, GameController.sound.getBufferSize());
            }
            in = AudioSystem.getAudioInputStream(format, in);
            in.mark(Integer.MAX_VALUE);
        } catch (FileNotFoundException ex) {
            throw new ResourceException(fn);
        } catch (IOException ex) {
            throw new LemmException(fn + " (IO exception)");
        } catch (UnsupportedAudioFileException ex) {
            throw new LemmException(fn + " (Unsupported Audio File)");
        }
        waveThread = new Thread(this);
        waveThread.start();
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
                    bytesRead = in.read(data);
                    if (bytesRead != -1) {
                        line.write(data, 0, bytesRead);
                    } else if (loopSong) {
                        if (in.markSupported()) {
                            in.reset();
                        } else {
                            in.close();
                            in = AudioSystem.getAudioInputStream(new BufferedInputStream(new FileInputStream(file)));
                            in = AudioSystem.getAudioInputStream(format, in);
                            in.mark(Integer.MAX_VALUE);
                        }
                        bytesRead = 0;
                    } else {
                        line.drain();
                    }
                } else {
                    try {
                        line.flush();
                        Thread.sleep(40);
                    } catch (InterruptedException ex) {
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            line.stop();
            line.flush();
            line.close();
        }
    }

    @Override
    public void stop() {
        if (waveThread != null) {
            waveThread.interrupt();
        }
        play = false;
    }

    @Override
    public void play() {
        if (waveThread != null) {
            waveThread.interrupt();
        }
        play = true;
    }

    @Override
    public void close() {
        if (waveThread == null) {
            return;
        }
        play = false;
        try {
            in.close();
        } catch (IOException ex) {
        }
        Thread moribund = waveThread;
        waveThread = null;
        try {
            moribund.interrupt();
            moribund.join();
        } catch (InterruptedException ex) {
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
