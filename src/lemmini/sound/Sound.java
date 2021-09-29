package lemmini.sound;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import javax.sound.sampled.*;
import lemmini.game.Core;
import lemmini.game.GameController;
import lemmini.game.ResourceException;
import lemmini.tools.Props;
import lemmini.tools.ToolBox;
import org.apache.commons.lang3.StringUtils;

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
 * Used to play a number of sounds.
 * Supports resampling and pitched samples.
 * @author Volker Oth
 */
public class Sound {
    
    public enum Effect {
        START ("start"),
        CHANGE_RR ("changeRR"),
        SELECT_SKILL ("selectSkill"),
        ASSIGN_SKILL ("assignSkill"),
        INVALID ("invalid"),
        NUKE ("nuke"),
        OHNO ("ohno"),
        EXPLODE ("explode"),
        SPLAT ("splat"),
        DROWN ("drown"),
        DIE ("die"),
        STEEL ("steel"),
        STEP_WARNING ("stepWarning");
        
        private final String keyName;
        private int sampleID;
        
        private Effect(String newKeyName) {
            keyName = newKeyName;
            sampleID = -1;
        }
        
        private void setSampleID(int newID) {
            sampleID = newID;
        }
        
        public int getSampleID() {
            return sampleID;
        }
        
        public String getKeyName() {
            return keyName;
        }
    }
    
    public enum PitchedEffect {
        RELEASE_RATE (Effect.CHANGE_RR, 2.0, -50.0, 48.0, 100),
        SKILL (Effect.SELECT_SKILL, 2.0, -4.0, 12.0, 20);
        
        private final Effect effect;
        private final double base;
        private final double expNumeratorOffset;
        private final double expDenominator;
        private final int numPitches;
        
        private PitchedEffect(Effect newEffect, double newBase,
                double newExpNumeratorOffset, double newExpDenominator,
                int newNumPitches) {
            effect = newEffect;
            base = newBase;
            expNumeratorOffset = newExpNumeratorOffset;
            expDenominator = newExpDenominator;
            numPitches = newNumPitches;
        }
        
        private Effect getEffect() {
            return effect;
        }
        
        private double getBase() {
            return base;
        }
        
        private double getExpNumeratorOffset() {
            return expNumeratorOffset;
        }
        
        private double getExpDenominator() {
            return expDenominator;
        }
        
        private int getNumPitches() {
            return numPitches;
        }
    }
    
    public enum Quality {
        NEAREST,
        LINEAR,
        CUBIC;
    }

    /** maximum number of sounds played in parallel */
    private static final int MAX_SIMUL_SOUNDS = 7;
    private static final String SOUND_INI_STR = "sound/sound.ini";
    
    private static int lineCounter = 0;

    private boolean loaded = false;
    private final LineHandler[] lineHandlers;
    private Deque<LineHandler> availableLineHandlers;
    private Path[] fNames;
    private final List<Path> sampleNames;
    private final int[] pitchedSampleID;
    /** sound buffers to store the samples */
    private byte[][] soundBuffers;
    /** audio format for samples */
    private final AudioFormat format;
    /** line info for samples */
    private final DataLine.Info info;
    /** pitch buffers to store all pitched samples */
    private byte[][][] pitchBuffers;
    private final byte[][] origPitchBuffers;
    private final AudioFormat[] origPitchFormats;
    /** gain/volume: 1.0 = 100% */
    private double gain;
    /** selected mixer index */
    static int mixerIdx;
    /** array of available mixers */
    static Mixer[] mixers;
    /** number of samples to be used */
    static int sampleNum;
    private final float sampleRate;
    private final int bufferSize;
    private final Quality resamplingQuality;

    /**
     * Constructor.
     * @throws ResourceException
     */
    public Sound() throws ResourceException {
        Props programProps = Core.programProps;
        sampleRate = (float) programProps.getDouble("sampleRate", 44100.0);
        bufferSize = programProps.getInt("bufferSize", 8192);
        Quality[] rqArray = Quality.values();
        int rq = programProps.getInt("resamplingQuality", Quality.CUBIC.ordinal());
        if (rq < 0) {
            resamplingQuality = rqArray[0];
        } else if (rq >= rqArray.length) {
            resamplingQuality = rqArray[rqArray.length - 1];
        } else {
            resamplingQuality = rqArray[rq];
        }
        programProps.setDouble("sampleRate", sampleRate);
        programProps.setInt("bufferSize", bufferSize);
        programProps.setInt("resamplingQuality", resamplingQuality.ordinal());
        
        gain = 1.0;
        sampleNames = new ArrayList<>(64);
        
        format = new AudioFormat(sampleRate, 16, 2, true, false);
        //info = new DataLine.Info(Clip.class, format);
        info = new DataLine.Info(SourceDataLine.class, format, bufferSize);
        
        PitchedEffect[] peValues = PitchedEffect.values();
        pitchBuffers = new byte[peValues.length][][];
        for (int i = 0; i < peValues.length; i++) {
            int numPitches = peValues[i].getNumPitches();
            pitchBuffers[i] = new byte[numPitches][];
        }
        origPitchBuffers = new byte[peValues.length][];
        origPitchFormats = new AudioFormat[peValues.length];
        pitchedSampleID = new int[peValues.length];
        
        load();

        // get all available mixers
        Mixer.Info[] mixInfo = AudioSystem.getMixerInfo();
        List<Mixer> mix = new ArrayList<>(16);
        for (Mixer.Info mixInfo1 : mixInfo) {
            Mixer mixer = AudioSystem.getMixer(mixInfo1);
            Line.Info info = new Line.Info(Clip.class);
            int num = mixer.getMaxLines(info);
            if (num != 0) {
                mix.add(mixer);
            }
        }
        mixers = mix.toArray(new Mixer[mix.size()]);
        
        lineHandlers = new LineHandler[MAX_SIMUL_SOUNDS];
        availableLineHandlers = new LinkedList<>();
        for (int i = 0; i < lineHandlers.length; i++) {
            lineHandlers[i] = new LineHandler((SourceDataLine) getLine(info), availableLineHandlers);
            availableLineHandlers.add(lineHandlers[i]);
            lineHandlers[i].setGain(gain);
            lineHandlers[i].start();
        }
    }
    
    public final void load() throws ResourceException {
        Path fn = Core.findResource(Paths.get(SOUND_INI_STR));
        Props p = new Props();
        if (!p.load(fn)) {
            throw new ResourceException(SOUND_INI_STR);
        }
        sampleNames.clear();
        for (int i = 0; true; i++) {
            String sName = p.get("sound_" + i, null);
            if (sName != null) {
                sampleNames.add(Paths.get(sName));
            } else {
                break;
            }
        }
        
        for (Effect e : Effect.values()) {
            e.setSampleID(p.getInt(e.getKeyName(), -1));
        }
        
        PitchedEffect[] peValues = PitchedEffect.values();
        
        Path fName = Paths.get(StringUtils.EMPTY);
        boolean reloadPitched = false;
        if (!loaded) {
            sampleNum = sampleNames.size();
            fNames = new Path[sampleNum];
            soundBuffers = new byte[sampleNum][];
            reloadPitched = true;
            for (int i = 0; i < peValues.length; i++) {
                pitchedSampleID[i] = peValues[i].getEffect().getSampleID();
            }
        } else {
            if (sampleNames.size() != sampleNum) {
                sampleNum = sampleNames.size();
                fNames = Arrays.copyOf(fNames, sampleNum);
                soundBuffers = Arrays.copyOf(soundBuffers, sampleNum);
            }

            for (int i = 0; i < peValues.length; i++) {
                if (peValues[i].getEffect().getSampleID() != pitchedSampleID[i]) {
                    reloadPitched = true;
                    pitchedSampleID[i] = peValues[i].getEffect().getSampleID();
                }
            }
        }
        
        try {
            for (int i = 0; i < sampleNum; i++) {
                fName = Core.findResource(
                        Paths.get("sound").resolve(sampleNames.get(i)),
                        Core.SOUND_EXTENSIONS);
                if (loaded) {
                    if (fName.equals(fNames[i])) {
                        continue;
                    }
                    if (!reloadPitched) {
                        for (int j = 0; j < peValues.length; j++) {
                            if (i == pitchedSampleID[j]) {
                                reloadPitched = true;
                                break;
                            }
                        }
                    }
                } else {
                    fNames[i] = fName;
                }
                
                AudioFormat currentFormat;
                
                try (InputStream in = new BufferedInputStream(Files.newInputStream(fName));
                        AudioInputStream ais = AudioSystem.getAudioInputStream(in)) {
                    currentFormat = ais.getFormat();
                    soundBuffers[i] = new byte[(int) ais.getFrameLength() * currentFormat.getFrameSize()];
                    ais.read(soundBuffers[i]);
                }
                
                soundBuffers[i] = convert(soundBuffers[i], currentFormat, format.getSampleSizeInBits(), format.getFrameSize(),
                        format.getChannels(), format.getEncoding() == AudioFormat.Encoding.PCM_SIGNED, format.isBigEndian());
                currentFormat = new AudioFormat(currentFormat.getSampleRate(),
                        format.getSampleSizeInBits(),
                        format.getChannels(),
                        format.getEncoding() == AudioFormat.Encoding.PCM_SIGNED,
                        format.isBigEndian());
                for (int j = 0; j < peValues.length; j++) {
                    if (peValues[j].getEffect().getSampleID() == i) {
                        origPitchBuffers[j] = soundBuffers[i];
                        origPitchFormats[j] = currentFormat;
                    }
                }
                soundBuffers[i] = resample(soundBuffers[i], currentFormat, format.getSampleRate(),  resamplingQuality);
            }
        } catch (UnsupportedAudioFileException | IOException ex) {
            throw new ResourceException(fName.toString());
        }

        if (reloadPitched) {
            for (int i = 0; i < peValues.length; i++) {
                if (pitchedSampleID[i] >= 0) {
                    // create buffers for pitching
                    // note that bit size and channels have to be the same for all pitched buffers
                    createPitched(peValues[i], origPitchFormats[i], format.getSampleRate(), resamplingQuality,
                            origPitchBuffers[i], pitchBuffers[i]);
                } else {
                    pitchBuffers = null;
                }
            }
        }
        
        loaded = true;
    }

    /**
     * Get an array of available mixer names.
     * @return array of available mixer names
     */
    public String[] getMixers() {
        if (mixers == null) {
            return null;
        }
        String[] s = new String[mixers.length];
        for (int i = 0; i < mixers.length; i++) {
            s[i] = mixers[i].getMixerInfo().getName();
        }
        return s;
    }

    /**
     * Set mixer to be used for sound output.
     * @param idx index of mixer
     */
    public synchronized void setMixerIdx(final int idx) {
        int oldMixerIdx = mixerIdx;
        
        if (idx > mixers.length) {
            mixerIdx = 0;
        } else {
            mixerIdx = idx;
        }
        
        if (oldMixerIdx != mixerIdx) {
            Deque<LineHandler> tempLineHandlers = new LinkedList<>();
            for (int i = 0; i < lineHandlers.length; i++) {
                lineHandlers[i].close();
                lineHandlers[i] = new LineHandler((SourceDataLine) getLine(info), tempLineHandlers);
                tempLineHandlers.add(lineHandlers[i]);
                lineHandlers[i].setGain(gain);
                lineHandlers[i].start();
            }
            availableLineHandlers = tempLineHandlers;
        }
    }
    
    /**
     * Set the current mixer index.
     * @return index of current mixer
     */
    public synchronized int getMixerIdx() {
        return mixerIdx;
    }

    /**
     * Return a data line to play a sample.
     * @param info line info with requirements
     * @return data line to play a sample
     */
    public final Line getLine(final DataLine.Info info) {
        try {
            return mixers[mixerIdx].getLine(info);
        } catch (LineUnavailableException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Play a given sound.
     * @param idx index of the sound to be played
     * @param pan panning
     */
    public void play(final int idx, final double pan) {
        if (idx < 0 || !GameController.isSoundOn()) {
            return;
        }
        
        Deque<LineHandler> tempLineHandlers = availableLineHandlers;
        LineHandler lh;
        synchronized (tempLineHandlers) {
            lh = tempLineHandlers.pollFirst();
            if (lh != null) {
                tempLineHandlers.addLast(lh);
            }
        }
        if (lh != null) {
            lh.play(soundBuffers[idx], pan);
        }
    }

    /**
     * Play a given sound.
     * @param idx index of the sound to be played
     */
    public void play(final int idx) {
        play(idx, 0.0);
    }

    /**
     * Play a given sound.
     * @param e
     * @param pan panning
     */
    public void play(final Effect e, final double pan) {
        int idx = e.getSampleID();
        play(idx, pan);
    }
    
    /**
     * Play a given sound.
     * @param e
     */
    public void play(final Effect e) {
        int idx = e.getSampleID();
        play(idx, 0.0);
    }

    /**
     * Play a pitched sample.
     * @param pe
     * @param pitch pitch value
     */
    public void playPitched(final PitchedEffect pe, final int pitch) {
        if (!GameController.isSoundOn()) {
            return;
        }
        
        Deque<LineHandler> tempLineHandlers = availableLineHandlers;
        LineHandler lh;
        synchronized (tempLineHandlers) {
            lh = tempLineHandlers.pollFirst();
            if (lh != null) {
                tempLineHandlers.addLast(lh);
            }
        }
        if (lh != null) {
            lh.play(pitchBuffers[pe.ordinal()][pitch], 0.0);
        }
    }

    /**
     * Convert sound to the specified sample rate.
     * @param buffer byte array containing source sample
     * @param af
     * @param newSampleRate
     * @param quality
     * @return sample converted to newSampleRate stored in byte array
     */
    public static byte[] resample(final byte[] buffer, final AudioFormat af, final float newSampleRate, final Quality quality) {
        float sampleRate = af.getSampleRate();
        if (sampleRate == newSampleRate) {
            return buffer;
        }
        
        int sampleSize = af.getSampleSizeInBits();
        int frameSize = af.getFrameSize();
        int numChannels = af.getChannels();
        int bytesPerSample = frameSize / numChannels;
        int numFrames = buffer.length / frameSize;
        boolean bigEndian = af.isBigEndian();
        boolean signed = af.getEncoding() == AudioFormat.Encoding.PCM_SIGNED;
        double scale = (double) newSampleRate / (double) af.getSampleRate();
        int origin = signed ? 0 : (1 << (sampleSize - 1));
        int maxValue;
        int minValue;
        if (signed) {
            maxValue = -((-1 << (sampleSize - 1)) + 1);
            minValue = -1 << (sampleSize - 1);
        } else {
            maxValue = -((-1 << sampleSize) + 1);
            minValue = 0;
        }
        
        // create scaled buffer
        int newNumFrames = (int) (numFrames * scale);
        byte[] newBuffer = new byte[newNumFrames * frameSize];
        for (int f = 0; f < newNumFrames; f++) {
            for (int c = 0; c < numChannels; c++) {
                int pos = (int) (f / scale);
                if (pos >= numFrames) {
                    pos = numFrames - 1;
                }
                double ofs = f / scale - pos;
                if (ofs < 0.0) {
                    ofs = 0.0;
                } else if (ofs > 1.0) {
                    ofs = 1.0;
                }
                // interpolate between sample points
                int sample0 = 0;
                int sample1 = 0;
                int sample2 = 0;
                int sample3 = 0;
                for (int sb = 0; sb < bytesPerSample; sb++) {
                    boolean signBit = signed && sb == (bigEndian ? 0 : bytesPerSample - 1);
                    switch (quality) {
                        case CUBIC:
                            if (pos - 1 >= 0) {
                                sample0 |= (buffer[(pos - 1) * frameSize + c * bytesPerSample + sb]
                                        & (signBit ? ~0 : 0xFF))
                                        << (8 * (bigEndian ? (bytesPerSample - 1 - sb) : sb));
                            } else {
                                sample0 = origin;
                            }
                            if (pos + 2 < numFrames) {
                                sample3 |= (buffer[(pos + 2) * frameSize + c * bytesPerSample + sb]
                                        & (signBit ? ~0 : 0xFF))
                                        << (8 * (bigEndian ? (bytesPerSample - 1 - sb) : sb));
                            } else {
                                sample3 = origin;
                            }
                            /* falls through */
                        case LINEAR:
                        default:
                            if (pos + 1 < numFrames) {
                                sample2 |= (buffer[(pos + 1) * frameSize + c * bytesPerSample + sb]
                                        & (signBit ? ~0 : 0xFF))
                                        << (8 * (bigEndian ? (bytesPerSample - 1 - sb) : sb));
                            } else {
                                sample2 = origin;
                            }
                            /* falls through */
                        case NEAREST:
                            sample1 |= (buffer[pos * frameSize + c * bytesPerSample + sb]
                                    & (signBit ? ~0 : 0xFF))
                                    << (8 * (bigEndian ? (bytesPerSample - 1 - sb) : sb));
                            break;
                    }
                }
                int newSample;
                switch (quality) {
                    case CUBIC:
                        double a0 = -0.5 * sample0 + 1.5 * sample1 - 1.5 * sample2 + 0.5 * sample3;
                        double a1 = sample0 - 2.5 * sample1 + 2 * sample2 - 0.5 * sample3;
                        double a2 = -0.5 * sample0 + 0.5 * sample2;
                        double a3 = sample1;
                        newSample = ToolBox.roundToInt(a0 * Math.pow(ofs, 3) + a1 * Math.pow(ofs, 2) + a2 * ofs + a3);
                        newSample = ToolBox.cap(minValue, newSample, maxValue);
                        break;
                    case LINEAR:
                    default:
                        newSample = ToolBox.roundToInt(sample1 + (sample2 - sample1) * ofs);
                        break;
                    case NEAREST:
                        newSample = sample1;
                        break;
                }
                for (int sb = 0; sb < bytesPerSample; sb++) {
                    newBuffer[f * frameSize + c * numChannels + sb] =
                            (byte) (newSample >>> (8 * (bigEndian ? (bytesPerSample - 1 - sb) : sb)));
                }
            }
        }
        
        return newBuffer;
    }
    
    public static byte[] convert(final byte[] buffer, final AudioFormat af,
            final int newSampleSize, final int newFrameSize, final int newNumChannels,
            final boolean newSigned, final boolean newBigEndian) {
        int sampleSize = af.getSampleSizeInBits();
        int frameSize = af.getFrameSize();
        int numChannels = af.getChannels();
        boolean signed = af.getEncoding() == AudioFormat.Encoding.PCM_SIGNED;
        boolean bigEndian = af.isBigEndian();
        
        if (signed == newSigned
                && sampleSize == newSampleSize
                && frameSize == newFrameSize
                && numChannels == newNumChannels
                && bigEndian == newBigEndian) {
            return buffer;
        }
        
        int numFrames = buffer.length / frameSize;
        int bytesPerSample = frameSize / numChannels;
        
        int newBytesPerSample = newFrameSize / newNumChannels;
        
        int origin = newSigned ? 0 : (1 << (sampleSize - 1));
        int minChannels = Math.min(numChannels, newNumChannels);
        byte[] newBuffer = new byte[numFrames * newFrameSize];
        int[] oldSamples = new int[numChannels];
        int[] newSamples = new int[newNumChannels];
        Arrays.fill(newSamples, origin);

        for (int i = 0; i < numFrames; i++) {
            for (int j = 0; j < numChannels; j++) {
                oldSamples[j] = 0;
                for (int k = 0; k < bytesPerSample; k++) {
                    boolean signBit = signed && k == (bigEndian ? 0 : bytesPerSample - 1);
                    oldSamples[j] |= (int) (buffer[i * frameSize + j * bytesPerSample + k]
                            & (signBit ? ~0 : 0xFF))
                            << (8 * (bigEndian ? (bytesPerSample - 1 - k) : k));
                }

                if (!signed) {
                    oldSamples[j] += -1 << (sampleSize - 1);
                }
                oldSamples[j] = oldSamples[j] << (Integer.SIZE - sampleSize)
                        >> (Integer.SIZE - newSampleSize);
                if (!newSigned) {
                    oldSamples[j] -= -1 << (sampleSize - 1);
                }
            }

            if (numChannels == 1 && newNumChannels >= 2) {
                newSamples[0] = oldSamples[0];
                newSamples[1] = oldSamples[0];
            } else if (numChannels >= 2 && newNumChannels == 1) {
                newSamples[0] = oldSamples[0] / 2 + oldSamples[1] / 2
                        + (oldSamples[0] % 2 + oldSamples[1] % 2) / 2;
            } else {
                System.arraycopy(oldSamples, 0, newSamples, 0, minChannels);
            }

            for (int j = 0; j < newNumChannels; j++) {
                for (int k = 0; k < newBytesPerSample; k++) {
                    newBuffer[i * newFrameSize + j * newBytesPerSample + k] =
                            (byte) (newSamples[j] >>> (8 * (newBigEndian ? (newBytesPerSample - 1 - k) : k)));
                }
            }
        }
        
        return newBuffer;
    }

    /**
     * Create a pitched version of a sample.
     * @param pe
     * @param af
     * @param newSampleRate
     * @param quality
     * @param oldBuffer
     * @param newBuffers
     */
    public static void createPitched(final PitchedEffect pe, final AudioFormat af,
            final float newSampleRate, final Quality quality,
            final byte[] oldBuffer, final byte[][] newBuffers) {
        for (int i = 0; i < pe.getNumPitches(); i++) {
            double dpitch = Math.pow(pe.getBase(), (i + pe.getExpNumeratorOffset()) / pe.getExpDenominator());
            float newSpeed = (float) (newSampleRate / dpitch);
            newBuffers[i] = resample(oldBuffer, af, newSpeed, quality);
        }
    }

    /**
     * Set gain of a line.
     * @param line line
     * @param gn gain (1.0 = 100%)
     */
    public static void setLineGain(final Line line, double gn) {
        if (line != null) {
            try {
                FloatControl control = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                double maxGain = Math.pow(10, control.getMaximum() / 20);
                double minGain = Math.pow(10, control.getMinimum() / 20);
                if (gn < minGain) {
                    gn = minGain;
                } else if (gn > maxGain) {
                    gn = maxGain;
                }
                float fgain = 20 * (float) Math.log10(gn);
                control.setValue(fgain);
            } catch (IllegalArgumentException ex) {
            }
        }
    }

    /**
     * Get gain.
     * @return gain (1.0 == 100%)
     */
    public double getGain() {
        return gain;
    }

    /**
     * Set gain.
     * @param gn gain (1.0 == 100%)
     */
    public void setGain(final double gn) {
        gain = gn;
        for (LineHandler lh : lineHandlers) {
            lh.setGain(gn);
        }
    }
    
    public float getSampleRate() {
        return sampleRate;
    }
    
    public int getBufferSize() {
        return bufferSize;
    }
    
    public Quality getResamplingQuality() {
        return resamplingQuality;
    }
    
    private class LineHandler implements Runnable, Closeable {
        
        private final SourceDataLine line;
        private final Deque<LineHandler> origDeque;
        private final int lineBufferSize;
        private double gain;
        private float pan;
        private byte[] nextBuffer;
        private boolean open;
        
        private final Thread lineThread;
        
        LineHandler(SourceDataLine line, Deque<LineHandler> origDeque) {
            this.line = line;
            this.origDeque = origDeque;
            lineBufferSize = info.getMaxBufferSize();
            gain = 1.0f;
            pan = 0.0f;
            nextBuffer = null;
            open = false;
            
            lineThread = new Thread(null, this, "LineHandler-" + lineCounter++);
        }

        @Override
        public void run() {
            try {
                line.open();
                line.start();
                setLineGain(line, gain);
                open = true;
                top:
                    while (open) {
                        byte[] currentBuffer = null;
                        float currentPan = 0.0f;
                        int bufferIndex = 0;

                        synchronized (this) {
                            while (nextBuffer == null && open) {
                                try {
                                    wait();
                                } catch (InterruptedException ex) {
                                }
                            }
                            if (nextBuffer != null && open) {
                                currentBuffer = nextBuffer;
                                nextBuffer = null;
                                currentPan = pan;
                            }
                        }

                        if (currentBuffer != null && open) {
                            setPan(currentPan);
                            while (bufferIndex < currentBuffer.length) {
                                bufferIndex += line.write(currentBuffer, bufferIndex,
                                        Math.min(lineBufferSize, currentBuffer.length - bufferIndex));
                                if (!open) {
                                    break top;
                                } else if (nextBuffer != null) {
                                    line.flush();
                                    continue top;
                                }
                            }
                            line.drain();
                            line.flush();
                            synchronized (origDeque) {
                                origDeque.remove(this);
                                origDeque.addFirst(this);
                            }
                        }
                    }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                synchronized (origDeque) {
                    origDeque.remove(this);
                }
                line.stop();
                line.flush();
                line.close();
            }
        }
        
        void play(byte[] newBuffer, double newPan) {
            if (!open) {
                return;
            }
            
            synchronized (this) {
                nextBuffer = newBuffer;
                pan = (float) ToolBox.cap(-1.0, newPan, 1.0);
                notifyAll();
            }
        }
        
        void start() {
            lineThread.start();
        }
        
        @Override
        public void close() {
            open = false;
            lineThread.interrupt();
        }
        
        void setGain(double gn) {
            gain = gn;
            if (line.isOpen()) {
                setLineGain(line, gn);
            }
        }
        
        private void setPan(float pan) {
            try {
                FloatControl control = (FloatControl) line.getControl(FloatControl.Type.PAN);
                control.setValue(pan);
            } catch (IllegalArgumentException ex) {
            }
        }
    }
}
