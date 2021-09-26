package lemmini.game;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
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
 * Class to create text screens which can be navigated with the mouse.
 * Uses {@link LemmFont} as bitmap font.
 *
 * @author Volker Oth
 */
public class TextDialog {
    /** list of buttons */
    private final List<Button> buttons;
    /** image used as screen buffer */
    private final Image screenBuffer;
    /** graphics object to draw in screen buffer */
    private final GraphicsContext gScreen;
    /** image used as 2nd screen buffer for offscreen drawing */
    private final Image backBuffer;
    /** graphics object to draw in 2nd (offscreen) screen buffer */
    private final GraphicsContext gBack;
    /** width of screen in pixels */
    private final int width;
    /** height of screen in pixels */
    private final int height;
    /** horizontal center of the screen in pixels */
    private final int centerX;
    /** vertical center of the screen in pixels */
    private final int centerY;

    /**
     * Create dialog text screen.
     * @param w Width of screen to create
     * @param h Height of screen to create
     */
    public TextDialog(final int w, final int h) {
        width = w;
        height = h;
        centerX = width / 2;
        centerY = height / 2;
        screenBuffer = ToolBox.createOpaqueImage(w, h);
        gScreen = screenBuffer.createGraphicsContext();
        gScreen.setClip(0, 0, width, height);
        backBuffer = ToolBox.createOpaqueImage(w, h);
        gBack = backBuffer.createGraphicsContext();
        gBack.setClip(0, 0, width, height);
        buttons = new ArrayList<>(8);
    }

    /**
     * Initialize/reset the text screen.
     */
    public void init() {
        buttons.clear();
        gScreen.setBackground(Color.BLACK);
        gScreen.clearRect(0, 0, width, height);
    }

    /**
     * Get image containing current (on-screen) screen buffer.
     * @return image containing current (on-screen) screen buffer
     */
    public Image getScreen() {
        return screenBuffer;
    }

    /**
     * Fill background with tiles.
     * @param tile Image used as tile
     */
    public void fillBackground(final Image tile) {
        for (int x = 0; x < width; x += tile.getWidth()) {
            for (int y = 0; y < width; y += tile.getHeight()) {
                gBack.drawImage(tile, x, y);
            }
        }
        gScreen.drawImage(backBuffer, 0, 0);
    }

    /**
     * Copy back buffer to front buffer.
     */
    public void copyToBackBuffer() {
        gBack.drawImage(screenBuffer, 0, 0);
    }

    /**
     * Set Image as background. The image will appear centered.
     * @param image Image to use as background
     */
    public void setBackground(final Image image) {
        int x = (width - image.getWidth()) / 2;
        int y = (height-image.getHeight()) / 2;
        gBack.setBackground(Color.BLACK);
        gBack.clearRect(0, 0, width, height);
        gBack.drawImage(image, x, y);
        gScreen.drawImage(backBuffer, 0, 0);
    }

    /**
     * Restore whole background from back buffer.
     */
    public void restore() {
        gScreen.drawImage(backBuffer, 0, 0);
    }

    /**
     * Restore a rectangle of the background from back buffer.
     * @param x x position of upper left corner of rectangle
     * @param y y position of upper left corner of rectangle
     * @param width width of rectangle
     * @param height height of rectangle
     */
    public void restoreRect(final int x, final int y, final int width, final int height) {
        gScreen.drawImage(backBuffer, x, y, x + width, y + height, x, y, x + width, y + height);
    }

    /**
     * Restore a rectangle of the background from back buffer that might be invalidated by
     * a text starting at x,y and having a length of len characters.
     * @param x0 x position of upper left corner of rectangle expressed in character widths
     * @param y0 y position of upper left corner of rectangle expressed in character heights
     * @param l Length of text
     */
    public void restoreText(final int x0, final int y0, final int l) {
        int x = x0 * LemmFont.getWidth();
        int y = y0 * (LemmFont.getHeight() + 4);
        int len = l * LemmFont.getWidth();
        int h = LemmFont.getHeight() + 4;
        gScreen.drawImage(backBuffer, x, y, x + len, y + h, x, y, x + len, y + h);
    }

    /**
     * Draw string.
     * @param s String
     * @param x0 X position relative to center expressed in character widths
     * @param y0 Y position relative to center expressed in character heights
     * @param col LemmFont color
     */
    public void print(final String s, final int x0, final int y0, final LemmFont.Color col) {
        int x = x0 * LemmFont.getWidth();
        int y = y0 * (LemmFont.getHeight() + 4);
        LemmFont.strImage(gScreen, s, centerX + x, centerY + y, col);
    }

    /**
     * Draw string.
     * @param s String
     * @param x X position relative to center expressed in character widths
     * @param y Y position relative to center expressed in character heights
     */
    public void print(final String s, final int x, final int y) {
        print(s, x, y, LemmFont.Color.GREEN);
    }

    /**
     * Draw string horizontally centered.
     * @param s String
     * @param y0 Y position relative to center expressed in character heights
     * @param col LemmFont color
     * @return Absolute x position
     */
    public int printCentered(final String s, final int y0, final LemmFont.Color col) {
        int charCount = LemmFont.getCharCount(s);
        if (charCount % 2 > 0) {
            charCount = (charCount + 2) - charCount % 2;
        }
        int y = y0 * (LemmFont.getHeight() + 4);
        int x = centerX - charCount * LemmFont.getWidth() / 2;
        LemmFont.strImage(gScreen, s, x, centerY + y, col);
        return x;
    }

    /**
     * Draw string horizontally centered.
     * @param s String
     * @param y Y position relative to center expressed in character heights
     * @return Absolute x position
     */
    public int printCentered(final String s, final int y) {
        return printCentered(s, y, LemmFont.Color.GREEN);
    }

    /**
     * Draw Image.
     * @param img Image
     * @param x X position relative to center
     * @param y Y position relative to center
     */
    public void drawImage(final Image img, final int x, final int y) {
        gScreen.drawImage(img, centerX + x, centerY + y);
    }

    /**
     * Draw Image horizontally centered.
     * @param img Image
     * @param y Y position relative to center
     */
    public void drawImage(final Image img, final int y) {
        int x = centerX - img.getWidth() / 2;
        gScreen.drawImage(img, x, centerY + y);
    }

    /**
     * Add Button.
     * @param x X position relative to center in pixels
     * @param y Y position relative to center in pixels
     * @param img  Button image
     * @param imgSelected Button selected image
     * @param type Button type
     */
    public void addButton(final int x, final int y, final Image img, final Image imgSelected, final TextScreen.Button type) {
        Button b = new Button(centerX + x, centerY + y, type);
        b.SetImage(img);
        b.SetImageSelected(imgSelected);
        buttons.add(b);
    }

    /**
     * Add text button.
     * @param x0 X position relative to center (in characters)
     * @param y0 Y position relative to center (in characters)
     * @param type Button type
     * @param t  Button text
     * @param ts Button selected text
     * @param col Button text color
     * @param cols Button selected text color
     */
    public void addTextButton(final int x0, final int y0, final TextScreen.Button type,
            final String t, final String ts, final LemmFont.Color col, final LemmFont.Color cols) {
        int x = x0 * LemmFont.getWidth();
        int y = y0 * (LemmFont.getHeight() + 4);
        TextButton b = new TextButton(centerX + x, centerY + y, type);
        b.setText(t, col);
        b.setTextSelected(ts, cols);
        buttons.add(b);
    }

    /**
     * React on left click.
     * @param x Absolute x position in pixels
     * @param y Absolute y position in pixels
     * @return Button type if button clicked, else NONE
     */
    public TextScreen.Button handleLeftClick(final int x, final int y) {
        for (Button b : buttons) {
            if (b.inside(x, y)) {
                return b.type;
            }
        }
        return TextScreen.Button.NONE;
    }

    /**
     * React on mouse hover.
     * @param x Absolute x position
     * @param y Absolute y position
     */
    public void handleMouseMove(final int x, final int y) {
        for (Button b : buttons) {
            b.selected = b.inside(x, y);
        }
    }

    /**
     * Draw buttons on screen.
     */
    public void drawButtons() {
        for (Button b : buttons) {
            b.draw(gScreen);
        }
    }

    /**
     * React on right click.
     * @param x Absolute x position
     * @param y Absolute y position
     * @return Button type if button clicked, else NONE
     */
    public TextScreen.Button handleRightClick(final int x, final int y) {
        for (Button b : buttons) {
            if (b.inside(x, y)) {
                return b.type;
            }
        }
        return TextScreen.Button.NONE;
    }

}

/**
 * Button class for TextDialog.
 * @author Volker Oth
 */
class Button {
    /** x coordinate in pixels */
    private final int x;
    /** y coordinate in pixels */
    private final int y;
    /** width in pixels */
    protected int width;
    /** height in pixels */
    protected int height;
    /** button type */
    protected TextScreen.Button type;
    /** true if button is selected */
    protected boolean selected;
    /** normal button image */
    protected Image image;
    /** selected button image */
    protected Image imgSelected;

    /**
     * Constructor
     * @param xi x position in pixels
     * @param yi y position in pixels
     * @param idi button type
     */
    Button(final int xi, final int yi, final TextScreen.Button typei) {
        x = xi;
        y = yi;
        type = typei;
    }

    /**
     * Set normal button image.
     * @param img image
     */
    void SetImage(final Image img) {
        image = img;
        if (image.getHeight() > height) {
            height = image.getHeight();
        }
        if (image.getWidth() > width) {
            width = image.getWidth();
        }
    }

    /**
     * Set selected button image.
     * @param img image
     */
    void SetImageSelected(final Image img) {
        imgSelected = img;
        if (imgSelected.getHeight() > height) {
            height = imgSelected.getHeight();
        }
        if (imgSelected.getWidth() > width) {
            width = imgSelected.getWidth();
        }
    }

    /**
     * Return current button image (normal or selected, depending on state).
     * @return current button image
     */
    Image getImage() {
        if (selected) {
            return imgSelected;
        } else {
            return image;
        }
    }

    /**
     * Draw the button.
     * @param g graphics object to draw on
     */
    void draw(final GraphicsContext g) {
        g.drawImage(getImage(), x, y);
    }

    /**
     * Check if a (mouse) position is inside this button.
     * @param xi
     * @param yi
     * @return true if the coordinates are inside this button, false if not
     */
    boolean inside(final int xi, final int yi) {
        return (xi >= x && xi < x + width && yi >= y && yi < y + height);
    }

}

/**
 * Button class for TextDialog.
 * @author Volker Oth
 */
class TextButton extends Button {
    /**
     * Constructor
     * @param xi x position in pixels
     * @param yi y position in pixels
     * @param typei button type
     */
    TextButton(final int xi, final int yi, final TextScreen.Button typei) {
        super(xi, yi, typei);
    }

    /**
     * Set text which is used as button.
     * @param s String which contains the button text
     * @param color Color of the button (LemmFont color!)
     */
    void setText(final String s, final LemmFont.Color color) {
        image = LemmFont.strImage(s, color);
        if (image.getHeight() > height) {
            height = image.getHeight();
        }
        if (image.getWidth() > width) {
            width = image.getWidth();
        }
    }

    /**
     * Set text for selected button.
     * @param s String which contains the selected button text
     * @param color Color of the button (LemmFont color!)
     */
    void setTextSelected(final String s, final LemmFont.Color color) {
        imgSelected = LemmFont.strImage(s, color);
        if (imgSelected.getHeight() > height) {
            height = imgSelected.getHeight();
        }
        if (imgSelected.getWidth() > width) {
            width = imgSelected.getWidth();
        }
    }
}