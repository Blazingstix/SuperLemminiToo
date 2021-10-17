package lemmini.game;

import lemmini.graphics.LemmImage;

/**
 * Implements Visual SFX
 *  
 * @author Charles Irwin
 *
 */
public class Vsfx {
    /** index of Visual SFX images */ 
    public static enum Vsfx_Index {
    	//standard SFX
    	POP,
    	CHAIN,
    	CHANGEOP,
    	CHINK,
    	DIE,
    	DOOR,
    	ELECTRIC,
    	BOOM,
    	FIRE,
    	GLUG,
    	LETSGO,
    	MANTRAP,
    	MOUSEPRE,
    	OHNO,
    	OING,
    	SCRAPE,
    	SLICER,
    	SPLASH,
    	SPLAT,
    	TENTON,
    	THUD,
    	THUNK,
    	TING,
    	YIPPEE,
    	//from Mac ONML
    	SLURP,
    	VACUUSUX,
    	WEEDGULP,
    	WETSPLAT
    }

	
	//private int frameIdx;
	private int x;
	private int y;
	private int counter;
	private int subCounter;
	private LemmImage image;
	private boolean isFinished;

    /** Lemmini runs at 33.33fps instead of 16.67fps */
	private static final int[] MAX_CTR = {10, 10, 12, 10, 10}; //allows for a maximum of 2.5 seconds. We're using half the time compared to the Bomber Countdown
	
	public static final int IMG_WIDTH = 56;
	public static final int IMG_HEIGHT = 48;
	public static final int VSFX_COUNT = 28;
	
	/**
	 * Constructor: Create Visual SFX 
	 * @param sx x coordinate of sfx source
	 * @param sy y coordinate of sfx source
	 * @param type the SFX being played/shown
	 */
	public Vsfx(final int sx, final int sy, final Vsfx.Vsfx_Index type) {
		x = sx - (IMG_WIDTH/2);
		y = sy - (IMG_HEIGHT);
		counter = 2; //default to being visible for 5 seconds.
		image = MiscGfx.getVsfxImage(type);
		
	}

	/**
	 * Constructor: Create Visual SFX 
	 * @param sx x coordinate of sfx source
	 * @param sy y coordinate of sfx source
	 * @param idx the index of the VSFX being shown
	 */
	public Vsfx(final int sx, final int sy, final int idx) {
		x = sx - (IMG_WIDTH/2);
		y = sy - (IMG_HEIGHT);
		counter = 2; //default to being visible for 5 seconds.
		image = MiscGfx.getVsfxImage(idx);
		
	}

	public void animate() {
		if (counter==0) {
			isFinished=true;
			//dispose the image and delete itself.
		} else {
            if (++subCounter >= MAX_CTR[counter - 1]) {
            	subCounter -= MAX_CTR[counter - 1];
                counter--;
            }
		}
	}
	
	public int screenX() {
		return x;
	}
	
	public int screenY() {
		return y;
	}
	
	public int width() {
		return 54;
	}
	
	public int height() {
		return 48;
	}
	
	public LemmImage getImage() {
		return image;
	}
	
	public boolean hasFinished() {
		return isFinished;
	}
	
}
