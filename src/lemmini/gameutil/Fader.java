package lemmini.gameutil;

import java.awt.Color;
import lemmini.graphics.GraphicsContext;
import lemmini.graphics.Image;
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
 * Simple fader class.
 * instead of doing painfully slow pixel wise gamma manipulation,
 * use a square with transparency with is drawn over the whole graphic context.
 *
 * @author Volker Oth
 */
public class Fader {
    
    public static final int FADE_STEP_FAST = 15;
    public static final int FADE_STEP_SLOW = 8;
    
    /** width of square to use for fading */
    private static final int WIDTH = 64;
    /** height of square to use for fading */
    private static final int HEIGHT = 64;
    /** maximum alpha (opaque) */
    private static final int MAX_ALPHA = 0xff;

    /** Fader state */
    public static enum State {
        /** don't fade */
        OFF,
        /** fade in */
        IN,
        /** fade out */
        OUT
    }

    /** current alpha value */
    private static int fadeValue;
    /** current fade state */
    private static State fadeState = State.OFF;
    /** step size for fading */
    private static int fadeStep = FADE_STEP_FAST;
    /** color of the fading rectangle */
    private static int color = 0; // black
    /** alpha value of the fading rectangle */
    private static int alpha = 0x80; // half transparent
    /** width of faded area */
    private static int width;
    /** height of faded area */
    private static int height;
    /** the image used as fading rectangle */
    private static Image alphaImg = null;
    /** the graphics used as fading rectangle (static to avoid multiple allocation) */
    private static GraphicsContext alphaGfx;

    /**
     * Set color to be used for fading.
     * @param c RGB color
     */
    public static synchronized void setColor(final int c) {
        color = c & 0xffffff;
        init();
    }

    /**
     * Set alpha value to be used for fading.
     * @param a 8bit alpha value
     */
    public static synchronized void setAlpha(final int a) {
        alpha = Math.min(Math.max(a, 0), 0xff);
        init();
    }

    /**
     * Set bounds of fading area.
     * @param w width in pixels
     * @param h height pixels
     */
    public static synchronized void setBounds(final int w, final int h) {
        width = w;
        height = h;
    }

    /**
     * Initialize fader.
     */
    private static void init() {
        Color fillColor; /* ARGB color of the fading rectangle composed from alpha and color */
        // create alpha image if needed
        if (alphaImg == null) {
            alphaImg = ToolBox.createTranslucentImage(WIDTH, HEIGHT);
            alphaGfx = alphaImg.createGraphicsContext();
        }
        // fill with alpha blended color
        fillColor = new Color((color >> 16) & 0xff, (color >> 8) & 0xff, color & 0xff, alpha);
        alphaGfx.setBackground(fillColor);
        alphaGfx.clearRect(0, 0, WIDTH, HEIGHT);
    }

    /**
     * Apply fader without changing the fader state.
     * @param g graphics to apply fader to
     */
    public static synchronized void apply(final GraphicsContext g) {
        for (int y = 0; y < height; y += HEIGHT) {
            for (int x = 0; x < width; x += WIDTH) {
                g.drawImage(alphaImg, x, y);
            }
        }
    }

    /**
     * Set fader state.
     * @param s state
     */
    public static synchronized void setState(final State s) {
        fadeState = s;
        switch (fadeState) {
            case IN:
                fadeValue = MAX_ALPHA; // opaque
                setAlpha(fadeValue);
                break;
            case OUT:
                fadeValue = 0; // transparent
                setAlpha(fadeValue);
                break;
            default:
                break;
        }
    }
    
    /**
     * Set fader state.
     * @param s state
     * @param g graphics to fade
     */
    public static synchronized void setState(final State s, final GraphicsContext g) {
        setState(s);
        setAlpha(fadeValue);
        apply(g);
    }

    /**
     * Get fader state.
     * @return fader state.
     */
    public static synchronized State getState() {
        return fadeState;
    }

    /**
     * Set step size.
     * @param step
     */
    public static void setStep(final int step) {
        fadeStep = step;
    }
    
    public static int getStep() {
        return fadeStep;
    }

    /**
     * Fade.
     * @param g graphics to fade
     */
    public static synchronized void fade(final GraphicsContext g) {
        switch (fadeState) {
            case IN:
                if (fadeValue >= fadeStep) {
                    fadeValue -= fadeStep;
                } else {
                    fadeValue = 0;
                    fadeState = State.OFF;
                }
                Fader.setAlpha(fadeValue);
                Fader.apply(g);
                // System.out.println(fadeValue);
                break;
            case OUT:
                if (fadeValue <= MAX_ALPHA-fadeStep) {
                    fadeValue += fadeStep;
                } else {
                    fadeValue = MAX_ALPHA;
                    fadeState = State.OFF;
                }
                Fader.setAlpha(fadeValue);
                Fader.apply(g);
                // System.out.println(fadeValue);
                break;
            default:
                break;
        }
    }
}
