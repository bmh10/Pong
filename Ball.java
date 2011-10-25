import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;


/*
 * Ball class which manages the balls position and motion during the game
 */

public class Ball {

	private Point pos, startPos;
	private int dx, dy;
	private int xrangemin, xrangemax, yrangemin, yrangemax; //was private
	private int size;
	public boolean inPlay;
	private Color color;
	
	BallSmokeParticle b;
	
	/*
	 * Creates a ball at specified position with specified colour
	 */
	public Ball(Point p, Color c) {
		this.pos = p;
		this.startPos = new Point(pos.x, pos.y);
		this.size = 20;
		this.dx = 1;
		this.dy = 1;
		this.inPlay = false;
		this.color = c;
	}
	
	public Point getPos() {
		return pos;
	}
	
	public void startPlay() {
		// If game already in progress cannot start new game
		if(inPlay)
			return;
		// Otherwise initialise new game round (keeps old scores)
		inPlay = true;
		
		// Make random start movement
		dx = (Math.random()<0.5) ? -1 : 1; 
		dy = (Math.random()<0.5) ? -1 : 1;
		pos = new Point(startPos.x, startPos.y); //?? this mat not work-> new Point(startPos.x, startPos.y) ??
	}
	
	public Point getVelocity() {
		Point p = new Point(dx, dy);
		return p;
	}
	
	public int getSize() {
		return size;
	}
	
	/*
	 * Returns position where the computerised paddle should be depending on game orientation
	 */
	public int getPaddlePos(boolean orientation) {
		if (orientation)
			return pos.y;
		else
			return pos.x;
	}
	
	/*
	 * Manages how the ball bounces on player paddles
	 */
	public boolean bounce(Player p, Game game) {
		int var;
		if(game.orientation) 
			var = p.getVarY();
		else
			var = p.getVarX();
		
		int paddleLen = p.getLength();
		
		// If ball position is either side of paddle -> no bounce
		if (game.orientation) {
			if (pos.y < var || pos.y >= var+paddleLen)
				return false;
		}
		else if (pos.x < var || pos.x >= var+paddleLen)
			return false;
		
		boolean bounced = false;
		Rectangle paddleRect = p.getRect();
		
		// Test for ball hitting side of paddle
		Rectangle YballRect = new Rectangle(pos.x-size/2, pos.y-size/2, size, size);
		YballRect.translate(dx, 0);
		if(paddleRect.intersects(YballRect)) {
			dx = -dx;
			bounced = true;
		}

		// Test for ball hitting top/front of paddle
		Rectangle XballRect = new Rectangle(pos.x-size/2, pos.y-size/2, size, size);
		XballRect.translate(dx, dy);
		if(paddleRect.intersects(XballRect)) {
			dy = -dy;
			bounced = true;
		}
		return bounced;
	}
	
	
	public void move(Game game) {
		// If game not being played cannot move
		if(!inPlay)
			return;
		// Move ball
		pos.x += dx;
		pos.y += dy;
		
		// Check that ball is within playing area and adjust, also monitor when points scored, also check orientation of game
		if(pos.y < yrangemin) {
			if (game.orientation) {
				// Bounce off top wall
				pos.y = yrangemin;
				dy = -dy;
			}
			else {
				// End this game and increment bottom players score
				inPlay = false;
				SoundManager.YOULOSE.play();
				game.rplayer.incScore(); 
				if (game.deathMatch)
					game.lplayer.setLength(game.lplayer.getLength()-20);
				game.state = Game.WAITING;
			}
		}
		
		if(pos.y > yrangemax) {
			if (game.orientation) {
				// Bounce off bottom wall
				pos.y = yrangemax;
				dy = -dy;
			}
			else {
				// End this game and increment top players score
				inPlay = false;
				SoundManager.YOULOSE.play();
				game.lplayer.incScore(); 
				if (game.deathMatch)
					game.rplayer.setLength(game.rplayer.getLength()-20);
				game.state = Game.WAITING;
			}
		}
		
		if(pos.x < xrangemin) {
			if (game.orientation) {
				// End this game and increment right players score
				inPlay = false;
				SoundManager.YOULOSE.play();
				game.rplayer.incScore();
				if (game.deathMatch)
					game.lplayer.setLength(game.lplayer.getLength()-20);
				game.state = Game.WAITING;
			}
			else {
				// Bounce off left wall
				pos.x = xrangemin;
				dx = -dx;
			}
		}
		
		if(pos.x > xrangemax) {
			if (game.orientation) {
				// End this game and increment left players score
				inPlay = false;
				SoundManager.YOULOSE.play();
				game.lplayer.incScore();  
				if (game.deathMatch)
					game.rplayer.setLength(game.rplayer.getLength()-20);
				game.state = Game.WAITING;
			}
			else {
				// Bounce off right wall
				pos.x = xrangemax;
				dx = -dx;
				}
			}
		}
		
	/*
	 * Sets walls with ball is to be contained within
	 */
	public void setRange(int xmin, int xmax, int ymin, int ymax) {
		xrangemin = xmin+size/2;
		xrangemax = xmax-size/2;
		yrangemin = ymin+size/2;
		yrangemax = ymax-size/2;
	}
	
	/*
	 * Change ball colour while game is paused then change colour back when game is resumed
	 */
	public void flash() {
		if (this.color == Color.YELLOW)
			this.color = Color.RED;
		else
			this.color = Color.YELLOW;
	}
	
	/*
	 * Draws the ball to screen
	 */
	public void draw(Graphics g) {
		if(!inPlay)
			return;
		g.setColor(color);
		g.fillOval(pos.x-size/2, pos.y-size/2, size, size);
	}
}
