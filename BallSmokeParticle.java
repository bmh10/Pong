import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;

/*
 * Class which implements the ball's smoke trail
 */
public class BallSmokeParticle {

	  Point pos;
	  Color color;
	  int size;

	  /*
	   * Creates a ball smoke particle at specified position
	   */
	  BallSmokeParticle(Point pos)
	  {
	    this.pos = pos;
	    this.size = 2;
	    color = Color.WHITE;
	  }
	  
	  public void setColor(Color color) {
		  this.color = color;
	  }

	  /*
	   * Draws a ball smoke particle to screen
	   */
	  void draw(Graphics g, Ball b) {
		if(!b.inPlay)
			return;
		g.setColor(color);
		g.drawOval(pos.x, pos.y, size, size);
	  }
	  
	  /*
	   * Draws a general smoke particle to screen (not dependent on ball)
	   */
	  void draw(Graphics g) {
			g.setColor(color);
			g.drawOval(pos.x, pos.y, size, size);
		  }
	}
	
