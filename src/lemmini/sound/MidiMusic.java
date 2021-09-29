package lemmini.sound;

import java.io.*;
import java.nio.charset.StandardCharsets;
import javax.sound.midi.*;
import lemmini.game.Core;
import lemmini.game.LemmException;
import lemmini.game.Resource;
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
 * Class to play MIDI music.
 *
 * @author Volker Oth
 */
public class MidiMusic implements MusicPlayer {
    
    /** MIDI sequencer */
    private Sequencer sequencer;
    private Synthesizer synthesizer;
    private Transmitter transmitter;
    private Receiver receiver;
    /** flag: initialization is finished and MIDI file can be played */
    private boolean canPlay;
    private final byte[] gainSysexData = {(byte) 0xF0, 0x7F, 0x7F, 0x04, 0x01, 0x7F, 0x7F, (byte) 0xF7};
    private final SysexMessage gainSysex = new SysexMessage();
    
    /**
     * Constructor.
     * @throws LemmException
     */
    public MidiMusic() throws LemmException {
        try {
            canPlay = false;
            sequencer = MidiSystem.getSequencer(false);
            synthesizer = MidiSystem.getSynthesizer();
            if (sequencer == null || synthesizer == null) {
                throw new LemmException("MIDI not supported.");
            }
        } catch (MidiUnavailableException ex) {
            throw new LemmException("MIDI not supported.");
        }
    }
    
    @Override
    public void load(final Resource res, final boolean loop) throws ResourceException, LemmException {
	close();
        try {
            synthesizer = MidiSystem.getSynthesizer();
            transmitter = sequencer.getTransmitter();
            receiver = synthesizer.getReceiver();
            transmitter.setReceiver(receiver);
            sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
            Sequence mySeq;
            try (InputStream in = new BufferedInputStream(res.getInputStream())) {
                mySeq = MidiSystem.getSequence(in);
            }
            Soundbank soundbank = getSoundbank(res);
            if (sequencer != null) {
                sequencer.setSequence(mySeq);
                if (loop) {
                    long[] loopPoints = findLoopPoints(mySeq);
                    sequencer.setLoopStartPoint(loopPoints[0]);
                    sequencer.setLoopEndPoint(loopPoints[1]);
                    sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
                }
                sequencer.open();
                synthesizer.open();
                if (soundbank != null && synthesizer.isSoundbankSupported(soundbank)) {
                    synthesizer.loadAllInstruments(soundbank);
                }
                setGain(Music.getGain());
                canPlay = true;
                //sequencer.addMetaEventListener(new MetaEventListener() {
                //    public void meta(MetaMessage event) {
                //        int type = event.getType();
                //        //System.out.println("MIDI message: " + type + StringUtils.SPACE + event.toString());
                //        if (type == 47) {
                //            sequencer.setTickPosition(0);
                //            sequencer.start();
                //        }
                //    }
                //});
            }
        } catch (InvalidMidiDataException ex) {
            throw new LemmException(res.getFileName() + " (Invalid MIDI data)");
        } catch (FileNotFoundException ex) {
            throw new ResourceException(res);
        } catch (IOException ex) {
            throw new LemmException(res.getFileName() + " (IO exception)");
        } catch (MidiUnavailableException ex) {
            throw new LemmException("MIDI not supported.");
        }
    }
    
    /**
     * Play current MIDI file.
     */
    @Override
    public void play() {
        if (canPlay && sequencer != null) {
            sequencer.start();
        }
    }
    
    /**
     * Stop current MIDI file.
     */
    @Override
    public void stop() {
        if (canPlay && sequencer != null) {
            sequencer.stop();
        }
    }
    
    /**
     * Close current MIDI file.
     */
    @Override
    public void close() {
        stop();
        if (sequencer != null) {
            sequencer.close();
        }
        if (synthesizer != null) {
            synthesizer.close();
        }
        canPlay = false;
    }
    
    /**
     * Set gain (volume) of MIDI output
     * @param gn gain factor: 0.0 (off) - 1.0 (full volume)
     */
    @Override
    public void setGain(final double gn) {
        if (synthesizer != null && receiver != null) {
            int gain;
            if (gn > 1.0) {
                gain = 16383;
            } else if (gn < 0) {
                gain = 0;
            } else {
                gain = (int) (gn * 16383);
            }
            
            byte gainLsb = (byte) (gain & 0x7F);
            byte gainMsb = (byte) ((gain >> 7) & 0x7F);
            gainSysexData[5] = gainLsb;
            gainSysexData[6] = gainMsb;
            try {
                gainSysex.setMessage(gainSysexData, gainSysexData.length);
                receiver.send(gainSysex, -1);
            } catch (InvalidMidiDataException ex) {
            }
        }
    }
    
    private static long[] findLoopPoints(Sequence seq) throws InvalidMidiDataException {
        long loopStart = -1;
        long loopEnd = -1;
        long controller111Pos = -1;
        Track[] tracks = seq.getTracks();
        loopSearch:
        for (int i = Math.min(tracks.length, 2) - 1; i >= 0; i--) {
            Track track = tracks[i];
            for (int j = track.size() - 1; j >= 0; j--) {
                MidiEvent me = track.get(j);
                MidiMessage mm = me.getMessage();
                if (mm instanceof MetaMessage) {
                    MetaMessage meta = (MetaMessage) mm;
                    if (meta.getType() == 0x06) {
                        String markerText;
                        markerText = new String(meta.getData(), StandardCharsets.US_ASCII);
                        switch (markerText) {
                            case "loopStart":
                                if (loopStart == -1) {
                                    loopStart = me.getTick();
                                }
                                break;
                            case "loopEnd":
                                if (loopEnd == -1) {
                                    loopEnd = me.getTick();
                                }
                                break;
                            default:
                                break;
                        }
                    }
                } else if (mm instanceof ShortMessage) {
                    ShortMessage sm = (ShortMessage) mm;
                    if (sm.getCommand() == ShortMessage.CONTROL_CHANGE
                            && sm.getData1() == 111
                            && controller111Pos == -1) {
                        controller111Pos = me.getTick();
                    }
                }
                if (loopStart != -1 && loopEnd != -1) {
                    break loopSearch;
                }
            }
        }
        if (loopStart < 0) {
            loopStart = controller111Pos;
        }
        if (loopStart < 0) {
            loopStart = 0;
        }
        if (loopEnd < loopStart) {
            loopEnd = -1;
        }
        return new long[]{loopStart, loopEnd};
    }
    
    private static Soundbank getSoundbank(Resource res) {
        try {
            Resource res2 = Core.findResource(res.getOriginalPath(), true, Core.SOUNDBANK_EXTENSIONS);
            if (res2 == null) {
                return null;
            }
            Soundbank sb;
            try (InputStream in = new BufferedInputStream(res2.getInputStream())) {
                sb = MidiSystem.getSoundbank(in);
            }
            return sb;
        } catch (InvalidMidiDataException | IOException | ResourceException ex) {
            return null;
        }
    }
}
