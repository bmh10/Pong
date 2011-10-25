import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

/*
 * Player class which manages a players position and motion during the game
 */

public class Player {
	
	// Depending on game orientation either x or y will be fixed while the other varies
	private int fixedX, varY, targetpos;
	private int varX, fixedY;
	
	// length=height, thickness = width, opposite for vertical orientation
	private int length, thickness;
	private int rangemin, rangemax, step;
	private int score;
	private Color color, origColor;
	// User option - if wrap is true then paddles wrap round the screen
	private static boolean wrap; 
	
	public boolean up, down;
	public static final int SPEED = 2;
	private int v;
	
	// true = horizontal, false = vertical
	boolean orientation; 

	/*
	 * Creates a player with specified location, paddle length, colour and orientation
	 */
	public Player(int x, int y, int paddlelength, Color color, boolean orientation) {
		this.orientation = orientation;
		if (orientation) {
			// Horizontal player
			this.fixedX = x;
			this.varY = y;
			this.targetpos = y;
		}
		else {
			// Vertical player
			this.varX = x;
			this.fixedY = y;
			this.targetpos = x; 
		}
		this.length = paddlelength;
		this.thickness = 20;
		this.step = 2;
		this.score = 0;
		this.color = color;
		this.origColor = color;
		this.up = false;
		this.down = false;
	}

	/*
	 * Gets paddles bounding rectangle
	 */
	public Rectangle getRect() {
		if (orientation)
			return new Rectangle(fixedX-thickness/2, varY, thickness, length);
		else
			return new Rectangle(varX, fixedY-thickness/2, length, thickness);
	}
	
	public void setTarget(int n) {
			this.targetpos = n - length/2;
	}
	
	// Horizontal
	public int getFixedX() {
		return fixedX;
	}
	
	public int getVarY() {
		return varY;
	}
	
	// Vertical
	public int getVarX() {
		return varX;
	}
	
	public int getFixedY() {
		return fixedY;
	}
	
	public int getLength() {
		return length;
	}
	
	public void setLength(int length) {
		this.length = length;
	}

	public void setVelocity(int v) {
		this.v = v;
	}
	
	public void incScore() {
		score++;
	}
	
	public void resetScore() {
		score = 0;
	}
	
	public int getScore() {
		return score;
	}

	public static boolean getWrap() {
		return wrap;
	}
	
	public static void setWrap(boolean wrap) {
		Player.wrap = wrap;
	}
	
	public void darken() {
		this.color = origColor.darker();
	}
	
	public void brighten() {
		this.color = origColor;
	}
	
	/* 
	 * Controls movement of both player mouse and computer opponent
	 */
    public void mouseMove() {
    	int d;
    	// Distance to move = targetpos - currposition
    	if (orientation)
    		d = targetpos - varY;
    	else
    		d = targetpos - varX;	
    	// Ensure that distance to move is not too far in one step
    	if (d < -step)     
    	    d = -step;
    	if (d > step)
    	    d = step;
    	// Move paddle 'distance' from currposition
    	if (orientation)
    		mouseMoveAux(varY+d);
    	else
    		mouseMoveAux(varX+d);
   }
    
    /*
     * Moves player taking into account orientation and whether paddle wrapping is enabled
     */
    private void mouseMoveAux(int n) {
    	if (orientation) {
    		varY = n;
    		if (varY < rangemin) {
    			if (wrap)
    				varY = rangemax;
    			else 
    				varY = rangemin;
    		}

    		if (varY > rangemax) {
    			if (wrap)
    				varY = rangemin;
    			else
    				varY = rangemax;
    		}
    	}
    	else {
    		varX = n;
    		if (varX < rangemin) {
    			if (wrap)
    				varX = rangemax;
    			else 
    				varX = rangemin;
    		}

    		if (varX > rangemax) {
    			if (wrap)
    				varX = rangemin;
    			else
    				varX = rangemax;
    		}
    	}
	}
	
	/* 
	 * Controls player movement using the keyboard, taking into account orientation and paddle wrapping
	 */
	public void keyMove() {
		if (orientation) {
			if (varY < rangemin) {
				if (wrap)
					varY = rangemax;
				else {
					varY = rangemin;
					v = 0;
				}
			}
			else if (varY > rangemax) {
				if (wrap)
					varY = rangemin;
				else {
					varY = rangemax;
					v = 0;
				}
			}
			else
				varY += v;
		}
		else {
			if (varX < rangemin) {
				if (wrap)
					varX = rangemax;
				else {
					varX = rangemin;
					v = 0;
				}
			}
			else if (varX > rangemax) {
				if (wrap)
					varX = rangemin;
				else {
					varX = rangemax;
					v = 0;
				}
			}
			else
				varX += v;
		}
	}
	
	/*
	 * Update the paddles speed depending on whether stationary or moving
	 */
	public void update() {  
		 v = 0;
    	 if(down) v = SPEED;   
    	 if(up) v = -SPEED;
    	 down = false; up = false; //this fixed bug
    }
	
	/*
	 * Sets the paddles max and min range (min is top of screen)
	 */
	public void setRange(int min, int max) {
		this.rangemin = min;
		this.rangemax = max-length+1;
	}
	
	/*
	 * Draws the paddle in its correct orientation with colour specified on creation
	 */
    public void draw(Graphics g) {
    	g.setColor(color);
    	if (orientation)
    		g.fillOval(fixedX-thickness/2, varY, thickness, length);
    	else
    		g.fillOval(varX, fixedY-thickness/2, length, thickness);
    }

}