import java.applet.Applet;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.Timer;

/*
 * Main class which manages the game
 */
@SuppressWarnings("serial")
public class Game extends Applet implements Runnable {

	Thread engine = null;
	Timer swingTimer;
	Ball ball;
	ArrayList<BallSmokeParticle> ballSmokeParticles;
	ArrayList<BallSmokeParticle> underlineParticles;
	// Keys which are currently pressed during gameplay
	private final Set<Integer> playingPressed = new HashSet<Integer>();
	Player lplayer, rplayer;
	int paddleLen;
	Dimension winSize;
	Font scorefont, tinyfont, smallfont, largefont;
	Image dbimage, logo;
	SoundManager backingTrack;
	long initialTime;
	
	// Player options
	int numPlayers;
	boolean sound;
	boolean mouse;
	boolean ballTrail;
	 // difficulty => 0=easy, 1=med, 2=hard
	int difficulty; 
	// orientation => true = horizontal, false = vertical
	boolean orientation; 
	
	
	// Game states
	boolean deathMatch;
	boolean deathMatchWinner;
	
	boolean showStats;
	boolean paused;
	
	int state;
	int prevState;
	static final int PLAYING = 0;
	static final int WAITING = 1;
	static final int MAINMENU = 2;
	static final int MODEMENU = 3;
	static final int OPTMENU = 4;
	static final int GAMEINFO = 5;
	static final int CHECK = 6;

	public static final int medPause = 8;
	public static final int easyPause = 10;
	public static final int hardPause = 5;
	int pause;
	
	public String getAppletInfo() {
		return "Pong by Ben Homer";
	}
	
	/*
	 * Initialises pong game
	 */
	public void init() {
		initialTime = System.currentTimeMillis();
		state = MAINMENU;
		prevState = MAINMENU;

		// Default settings
		sound = true;
		mouse = false;
		Player.setWrap(false);
		ballTrail = true;
		orientation = true;
		showStats = false;		
		difficulty = 1; // medium
		pause = medPause;
		deathMatch = false;
		deathMatchWinner = false;
		
		setBackground(Color.BLACK);
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		this.setSize(dim.width - 10, dim.height - 110);
		Dimension d = winSize = this.getSize();
		logo = this.getImage(getDocumentBase(), "pong_logo.gif");

		createPlayers(winSize);
		ball = new Ball(new Point(d.width/2, d.height/2), Color.YELLOW); //change start pos to be random
		ball.setRange(0, d.width-1, 0, d.height-1);
		ballSmokeParticles = new ArrayList<BallSmokeParticle>();
		underlineParticles = new ArrayList<BallSmokeParticle>();
		scorefont = new Font("Digital Dream", Font.BOLD, 30);
		smallfont = new Font("Atomic Clock Radio", Font.BOLD, 16);
		tinyfont = new Font("Arial", Font.BOLD, 10);
		largefont = new Font("Digital Dream", Font.BOLD, 48);
		
		dbimage = createImage(d.width, d.height);
		SoundManager.init();
		backingTrack = SoundManager.selectRandomBackgroundTrack();
		// add key/mouse listeners
		this.addKeyListener(keyListener);
		//this.addKeyListener(leftListener);
		this.addMouseMotionListener(mouseMoveListener);
		this.addMouseListener(mouseClickListener);
	}

	/*
	 * Creates players for game, depending on the game orientation
	 */
	public void createPlayers(Dimension d) {

		paddleLen = 130;
		if (deathMatch) paddleLen += 30;
		
		if(orientation) {
			// Horizontal game
			lplayer = new Player(30, d.height/2, paddleLen, Color.RED, orientation);
			rplayer = new Player(d.width-50, d.height/2, paddleLen, Color.BLUE, orientation);
			lplayer.setRange(0, d.height);
			rplayer.setRange(0, d.height);
		}
		else {
			// Vertical game - make paddles slightly wider as less distance between them
			paddleLen += 30;
			lplayer = new Player(d.width/2, 30 , paddleLen+30, Color.RED, orientation);
			rplayer = new Player(d.width/2, d.height-50, paddleLen+30, Color.BLUE, orientation);
			lplayer.setRange(0, d.width);
			rplayer.setRange(0, d.width);
		}
	}

	/*
	 * Updates players' scores
	 */
	public void updateScore(int p) {
		if (p==0)
			lplayer.incScore();
		else
			rplayer.incScore();
	}

	/*
	 *  Main game loop 
	 */
	@Override
	public void run() {
		SoundManager.INTRO.play();
		while(true) {
			try {
				for(int i=0; i!=5; i++)
					step();
				repaint();
				Thread.currentThread();
				Thread.sleep(pause);  // Change pause to alter game speed
			}
			catch (Exception e) {}
		}

	}
	
	
	/*
	 * Single game step
	 */
	public void step() {
		
		// Background music control
		if(state == PLAYING || state == WAITING )
			backingTrack.play();
		else
			backingTrack.stopLoop();
		// ******************************************************************
		
		
		// Move right player according to mouse/key movement
		if(mouse)
			rplayer.mouseMove();
		else 
			rplayer.keyMove();
		
		if(numPlayers == 1) {
			// Move left player according to where the ball is 
			lplayer.setTarget(ball.getPaddlePos(orientation));
			if (ball.inPlay) {
				lplayer.mouseMove();
			}
		}
		else {
			// Must be 2 players -> must be using keys to control
				lplayer.keyMove();
		}
		
		// Check for bounces against player paddles
		if(ball.bounce(lplayer, this)) {
			SoundManager.SONAR.play();
			lplayer.darken();
			rplayer.brighten();
		}
		if(ball.bounce(rplayer, this)) {
			SoundManager.SONAR.play();
			rplayer.darken();
			lplayer.brighten();
		}
		ball.move(this);
		
		if(ballTrail)
			makeBallTrail(50);
		
		if (state != PLAYING) {
			recalibratePaddles();
			if (state != WAITING) {
				titleUnderlineParticles(500);
				if(state != CHECK)
					deathMatchWinner = false;
			}
		}
		
		// Ensure paddles are not moving after end of a round
		if (state == WAITING) {
			playingPressed.clear();
			rplayer.setVelocity(0);
			lplayer.setVelocity(0);
			
			// Reset ranges if paddle shrinks during a death match
			if (deathMatch)
				recalibrateRanges();
		}
	}
	
	
	/*
	 * Displays game statistics
	 */
	public void displayStats(Graphics g, int s) {
		g.setFont(tinyfont);
		g.setColor(Color.GREEN);
		FontMetrics fm = g.getFontMetrics();

		String time = Long.toString(System.currentTimeMillis() - initialTime);
		String rpl = Integer.toString(rplayer.getLength());
		String lpl = Integer.toString(lplayer.getLength());
		String rpx, rpy, lpx, lpy;
		if(orientation) {
			rpx = Integer.toString(rplayer.getFixedX());
			rpy = Integer.toString(rplayer.getVarY());
			lpx = Integer.toString(lplayer.getFixedX());
			lpy = Integer.toString(lplayer.getVarY());
		}
		else {
			rpx = Integer.toString(rplayer.getVarX());
			rpy = Integer.toString(rplayer.getFixedY());
			lpx = Integer.toString(lplayer.getVarX());
			lpy = Integer.toString(lplayer.getFixedY());
		}
		String bx = Integer.toString(ball.getPos().x);
		String by = Integer.toString(ball.getPos().y);
		String bvx = Integer.toString(ball.getVelocity().x);
		String bvy = Integer.toString(ball.getVelocity().y);
		String st = null;
		switch(state) {
		case 0: st = "PLAYING"; break;
		case 1: st = "WAITING"; break;
		case 2: st = "MAINMENU"; break;
		case 3: st = "MODEMENU"; break;
		case 4: st = "OPTMENU"; break;
		case 5: st = "GAMEINFO"; break;
		case 6: st = "CHECK"; break;
		}
		String snd = (sound) ? "ON" : "OFF";
		String control = (mouse) ? "MOUSE" : "KEYS";
		String wrp = (Player.getWrap()) ? "ON" : "OFF";
		String btrail = (ballTrail) ? "ON" : "OFF";
		String ori = (orientation) ? "HORIZ" : "VERT";
		String diff = null;
		switch(difficulty) {
		case 0: diff = "EASY"; break;
		case 1: diff = "MEDIUM"; break;
		case 2: diff = "HARD"; break;
		}
		String gSpeed = Integer.toString(pause);
		String gType = (deathMatch) ? "Death Match" : "Classic";
		String dmWinner = (deathMatchWinner) ? "YES" : "NO";
		String pause = (paused) ? "YES" : "NO";
		
		
		leftString(g, fm, "Stats", s);
		leftString(g, fm, "Game run-time: " + time , s=space(s)+10);
		leftString(g, fm, "State: " + st, s=space(s));
		leftString(g, fm, "Paused: " + pause, s=space(s));
		leftString(g, fm, "Blue player length: " + rpl, s=space(s)+10);
		leftString(g, fm, "Blue player coordinates: " + rpx + ", " + rpy, s=space(s));
		leftString(g, fm, "Red player length: " + lpl, s=space(s)+10);
		leftString(g, fm, "Red player coordinates: " + lpx + ", " + lpy, s=space(s));
		leftString(g, fm, "Ball coordinates: " + bx + ", " + by, s=space(s)+10);
		leftString(g, fm, "Ball velocity: " + bvx + ", " + bvy, s=space(s));
		leftString(g, fm, "Player Options", s=space(s)+10);
		leftString(g, fm, "Game Type: " + gType, s=space(s)+10);
		leftString(g, fm, "Sound: " + snd, s=space(s));
		leftString(g, fm, "Control: " + control, s=space(s));
		leftString(g, fm, "Wrapping: " + wrp, s=space(s));
		leftString(g, fm, "Ball Trail: " + btrail, s=space(s));
		leftString(g, fm, "Orientation: " + ori, s=space(s));
		leftString(g, fm, "Difficulty: " + diff, s=space(s));
		leftString(g, fm, "Game Speed: " + gSpeed, s=space(s));
		leftString(g, fm, "DeathMatch winner: " + dmWinner, space(s));
	}
	
	/*
	 * Resets ranges if paddle shrinks during a death match
	 */
	private void recalibrateRanges() {
		int max = (orientation) ? winSize.height : winSize.width;
		rplayer.setRange(0, max);
		lplayer.setRange(0, max);
	}
	
	/*
	 * Resets paddles back to central positions
	 */
	private void recalibratePaddles() {
		// Paddles may have been reduced in size if played a death match, therefore recreate if at main menu
		if (state == MAINMENU || state == OPTMENU) {
			rplayer.setLength(paddleLen);
			lplayer.setLength(paddleLen);
		}
		int target = (orientation) ? winSize.height/2 : winSize.width/2;
		rplayer.setTarget(target);
		rplayer.mouseMove();
		rplayer.brighten();
		lplayer.setTarget(target);
		lplayer.mouseMove();
		lplayer.brighten();
	}
	
	/*
	 * Draws and centers a string on the game screen at the specified y-position
	 */
	private void centerString(Graphics g, FontMetrics fm, String str, int ypos) {
		g.drawString(str, (winSize.width - fm.stringWidth(str))/2, ypos);
	}
	
	/*
	 * Draws a string on the left side of the game screen at the specified y-position
	 */
	private void leftString(Graphics g, FontMetrics fm, String str, int ypos) {
		g.drawString(str, (winSize.width)/8, ypos);
	}
	
	/*
	 * Allows for easy formatting of string in a vertical column
	 */
	private int space(int s) {
		s += 20;
		return s;
	}
	
	/*
	 * Draws game banner at the start of a new game
	 */
	public void drawBanner(Graphics g) {
		// Change colours later
		g.setFont(largefont);
		FontMetrics fm = g.getFontMetrics();
		g.setColor(Color.YELLOW);
		g.drawImage(logo, (winSize.width-logo.getWidth(null))/2, 50, null);
		g.setFont(scorefont);
		fm = g.getFontMetrics();
		centerString(g, fm, "by Ben Homer", 160);
		g.setFont(smallfont);
		fm = g.getFontMetrics();

		if(state == GAMEINFO)
			openGameInfo(g, fm, 270);
		else if(state == CHECK)
			openCheck(g, fm);
		else {
			centerString(g, fm, "Type the number of the menu option you want:", 270);

			if (state == MAINMENU)
				openMainMenu(g, fm, 300);

			else if (state == OPTMENU)
				openOptionsMenu(g, fm, 300);

			else if (state == MODEMENU)
				openModeMenu(g, fm, 300);
		}
	}
	
	/* 
	 * Draws game main menu 
	 */
	public void openMainMenu(Graphics g, FontMetrics fm, int s) {
		centerString(g, fm, "1. Single Player", s);
		centerString(g, fm, "2. Two Player", s=space(s));
		centerString(g, fm, "3. Options", s=space(s));
		centerString(g, fm, "4. Quit", space(s));
	}
	
	/*
	 * Creates particle space used to underline the main title
	 */
	public void titleUnderlineParticles(int numParticles) {
		
		// Keep number of particles constant
		if(underlineParticles.size()>numParticles)
			underlineParticles.remove(0);

		// Make particle offset random
		int areax = 300;
		int areay = 50;
		int randx = (int) (Math.random()*areax);
		int randy = (int) (Math.random()*areay);

		// Make the particles stay within specified area 
		Point p = new Point((winSize.width - areax)/2 + randx, 230 - randy);
		BallSmokeParticle b = new BallSmokeParticle(p);
		underlineParticles.add(b);
		
		for(int i = 0; i<numParticles/2; i++)
			underlineParticles.get(i).setColor(Color.YELLOW);
	}
	
	/*
	 * Creates ball particle trail during play
	 */
	public void makeBallTrail(int numParticles) {
		// Keep number of particles constant
		if(ballSmokeParticles.size()>numParticles)
			ballSmokeParticles.remove(0);

		// Make particle offset random
		int randx = (int) (Math.random()*20);
		int randy = (int) (Math.random()*20);

		// make the particles stay behind the ball direction of movement within a band
		Point p = new Point(ball.getPos().x - ball.getVelocity().x*ball.getSize()/2 - ball.getVelocity().x*randx , ball.getPos().y - ball.getVelocity().y*ball.getSize()/2 - ball.getVelocity().y*randy);
		BallSmokeParticle b = new BallSmokeParticle(p);
		ballSmokeParticles.add(b);

		
		for(int i = 0; i<20; i++)
			ballSmokeParticles.get(i).setColor(Color.RED);
	}
		
	
	/*
	 * Draws game mode menu
	 */
	public void openModeMenu(Graphics g, FontMetrics fm, int s) {
		centerString(g, fm, "1. Classic", s);
		centerString(g, fm, "2. Death Match", s=space(s));
		centerString(g, fm, "3. Back", space(s));
	}
	
	/*
	 * Draws game options menu
	 */
	public void openOptionsMenu(Graphics g, FontMetrics fm, int l) {
		String s, c, d, p, t, o;
		if (sound) s = "ON";
		else s = "OFF";
		
		if (mouse) c = "MOUSE";
		else c = "KEYS";
		
		switch(difficulty) {
		case 0: d = "EASY"; break;
		case 2: d = "HARD"; break;
		default: d = "MEDIUM";
		}
		
		if(Player.getWrap()) p = "ON";
		else p = "OFF";

		if(ballTrail) t = "ON";
		else t = "OFF";
		
		if(orientation) o = "HORIZ";
		else o = "VERT";
		
		centerString(g, fm, "1. Game Info", l);
		centerString(g, fm, "2. Sound  " + s , l=space(l));
		centerString(g, fm, "3. Controller  " + c , l=space(l));
		centerString(g, fm, "4. Difficulty  " + d , l=space(l));
		centerString(g, fm, "5. Paddle Wrapping  " + p , l=space(l));
		centerString(g, fm, "6. Ball Trail  " + t , l=space(l));
		centerString(g, fm, "7. Game Orientation  " + o , l=space(l));
		centerString(g, fm, "8. Back", space(l));
	}
	
	/*
	 * Draws game info
	 */
	public void openGameInfo(Graphics g, FontMetrics fm, int s) {
		
		// Paragraph about game, background info, modes and controls
		centerString(g, fm, "An emulation of an old classic with a few extras", s);
		centerString(g, fm, "Go to the options menu to adjust game settings", s=space(s));
		centerString(g, fm, "Please send any questions or comments to bensblogx@gmail.com", s=space(s));
		centerString(g, fm, "Controls for horiontal play:", s=space(s)+20);
		centerString(g, fm, "Right player: LEFT/RIGHT", s=space(s));
		centerString(g, fm, "Left player: A/S", s=space(s));
		centerString(g, fm, "Controls for vertical play:", s=space(s)+20);
		centerString(g, fm, "Right player: UP/DOWN", s=space(s));
		centerString(g, fm, "Left player: A/Z", s=space(s));
		centerString(g, fm, "Press P during play to pause the game", s=space(s)+20);
		centerString(g, fm, "Press BACKSPACE at any time for game stats", s=space(s)+20);
		
		centerString(g, fm, "Press ENTER to go back", s=space(s)+20);
	}

	/*
	 * Draws checker screen (checks if user want to exit)
	 */
	public void openCheck(Graphics g, FontMetrics fm) {
		centerString(g, fm, "Are you sure?", 270);
		centerString(g, fm, "Y/N", 320);
	}
	
	/*
	 * Displays the scores of both players and also checks for a winner in a death match
	 */
	public void displayScores(Graphics g) {
		g.setFont(scorefont);
		g.setColor(Color.YELLOW);
		FontMetrics fm = g.getFontMetrics();
		String rscore = Integer.toString(rplayer.getScore());
		String lscore = Integer.toString(lplayer.getScore());
		centerString(g, fm, "Score" , 100);
		centerString(g, fm, lscore + "    " + rscore , 150);
		g.setFont(smallfont);
		fm = g.getFontMetrics();
		if (rplayer.getScore() == 0 && lplayer.getScore() == 0) {
			if (deathMatch)
				centerString(g, fm, "This is a Death Match: your paddle size will decrease if you lose a point" , 200);
			centerString(g, fm, "You can change your control device in the options menu" , 230);
			centerString(g, fm, "Press the up key or click the mouse to begin" , 260);
		}
		else {
			if (deathMatch) {
				if(rplayer.getLength() <= 0) {
					deathMatchWinner = true;
					centerString(g, fm, "Red player is the winner" , 200);
				}
				else if(lplayer.getLength() <= 0) {
					deathMatchWinner = true;
					centerString(g, fm, "Blue player is the winner" , 200);
				}
				if(deathMatchWinner) {
					centerString(g, fm, "Press the up key or click the mouse to go back to main menu" , 250);
					return;
				}
			}
			// If no death match winner or in classic game
			centerString(g, fm, "Press the up key or click the mouse to continue" , 200);
			centerString(g, fm, "Press escape to go back to the main menu" , 250);
		}
	}
	
	public void drawIngameScores(Graphics g) {
		g.setFont(scorefont);
		g.setColor(Color.YELLOW);
		FontMetrics fm = g.getFontMetrics();
		String rscore = Integer.toString(rplayer.getScore());
		String lscore = Integer.toString(lplayer.getScore());
		
		if (orientation)
			centerString(g, fm, lscore + "    " + rscore , 50);
		else {
			g.drawString(rscore, 10, (winSize.height+25)/2);
			g.drawString(lscore, 10, (winSize.height+25)/2 - 50);
		}
			
	}
	
	/*
	 * Updates graphics every step
	 */
	public void update(Graphics realg) {
		Graphics g = dbimage.getGraphics();
		g.setColor(getBackground());
		g.fillRect(0, 0, winSize.width, winSize.height);
		g.setColor(getForeground());
		if (!ball.inPlay) {

			// Display current game scores and wait for mouse click to continue next round (also checks for winner in a death match)
			if (state == WAITING)
				displayScores(g);
			// If game game has just begun display start banner
			else
				drawBanner(g);
		}

		// Re-draw game screen
		lplayer.draw(g);
		rplayer.draw(g);
		ball.draw(g);
		
		if (ballTrail && state == PLAYING) {
			for (int i = 0; i < ballSmokeParticles.size(); i++)
			{
				BallSmokeParticle particle = ballSmokeParticles.get(i);
				if (particle != null)
					particle.draw(g);
			}
		}
		
		if (ball.inPlay)
			drawIngameScores(g);
		
		if (paused)
			centerString(g, g.getFontMetrics(),"PAUSED" , 300);

		if (showStats)
			displayStats(g, 150);

		if (state != PLAYING && state != WAITING) {
			for (int i = 0; i < underlineParticles.size(); i++)
			{
				BallSmokeParticle particle = underlineParticles.get(i);
				if (particle != null)
					particle.draw(g);
			}
		}
		realg.drawImage(dbimage, 0, 0, this);
	}

	/*
	 * Starts a game thread
	 */
	public void start() {
		if(engine == null) {
			engine = new Thread(this);
			engine.start();
		}
	}
	
	/*
	 * Stops the current game thread
	 */
	@SuppressWarnings("deprecation")
	public void stop() {
		if (engine != null && engine.isAlive())
			engine.stop();
		engine = null;
	}
	
	/*
	 * Allows user to use mouse as game controller
	 */
	MouseMotionListener mouseMoveListener = new MouseMotionListener() {
		@Override
		public void mouseMoved(MouseEvent e) {
			switch(state) {
			case PLAYING:
				if (orientation)
					rplayer.setTarget(e.getY());
				else
					rplayer.setTarget(e.getX());
				break;
			}
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			// Unimplemented
		}
	};

	MouseListener mouseClickListener = new MouseAdapter() {

		@Override
		public void mousePressed(MouseEvent e) {
			switch(state) {
			case WAITING:
				if (deathMatchWinner)
					state = MAINMENU;
				else {
					ball.startPlay();
					state = PLAYING;
				}
				break;
			}
		}
	};
	 
	/*
	 * Allows user to use keyboard as game controller
	*/
	KeyListener keyListener = new KeyAdapter() {
		
	@SuppressWarnings("deprecation")
	@Override
	public void keyPressed(KeyEvent e) {
		int key = e.getKeyCode();
		
		if (key == KeyEvent.VK_BACK_SPACE) {
			if (showStats) showStats = false;
			else showStats = true;
		}
		
		switch(state) {
			case PLAYING:
				if (key == KeyEvent.VK_P) {
					if (paused) { ball.flash(); paused = false; engine.resume(); }
					else { ball.flash(); paused = true; engine.suspend(); }
				}
				
				playingPressed.add(key);
				
				if (playingPressed.size() > 0)
				{
					Iterator<Integer> it = playingPressed.iterator();
					while(it.hasNext())
					{
						int currKey = it.next();

						if (orientation) {

							if (currKey == KeyEvent.VK_DOWN) 
								rplayer.down = true;
							else if (currKey == KeyEvent.VK_UP)
								rplayer.up = true;

							// If in single player computer uses mouseMove() therefore no checks for (numPlayers == 2) required
							if (currKey == KeyEvent.VK_Z)
								lplayer.down = true;
							else if (currKey == KeyEvent.VK_A)
								lplayer.up = true;

						}
						else {
							if (currKey == KeyEvent.VK_RIGHT)
								rplayer.down = true;
							else if (currKey == KeyEvent.VK_LEFT)
								rplayer.up = true;

							// If in single player computer uses mouseMove() therefore no checks for (numPlayers == 2) required
							if (currKey == KeyEvent.VK_S)
								lplayer.down = true;
							else if (currKey == KeyEvent.VK_A)
								lplayer.up = true;
						}
					}
					rplayer.update();
					lplayer.update();
				}
				break;

			case WAITING:
				switch(key) {
					case KeyEvent.VK_UP:
						SoundManager.MENUCLICK.play();
						if (deathMatchWinner)
							state = MAINMENU;
						else {
							ball.startPlay();
							state = PLAYING;
						}
						break;
					case Event.ESCAPE:
						SoundManager.MENUCLICK.play();
						prevState = WAITING;
						state = CHECK;
						break;
				}
			break;
			
			case MAINMENU:
				switch(key) {
					case '1':
						numPlayers = 1;
						SoundManager.MENUCLICK.play();
						state = MODEMENU;
						break;
					case '2':
						numPlayers = 2;
						SoundManager.MENUCLICK.play();
						// Must use keys to control if 2 players
						mouse = false; 
						state = MODEMENU;
						break;
					case '3':
						state = OPTMENU;
						SoundManager.MENUCLICK.play();
						break;
					case '4': case Event.ESCAPE:
						SoundManager.MENUCLICK.play();
						prevState = MAINMENU;
						state = CHECK;
						break;
				}
			break;
			
			case MODEMENU:
				switch(key) {
					case '1':
						// Classic game
						SoundManager.MENUCLICK.play();
						backingTrack = SoundManager.selectRandomBackgroundTrack();
						deathMatch = false;
						createPlayers(winSize);
						state = WAITING;
						break;
					case '2':
						// Death match game
						SoundManager.MENUCLICK.play();
						backingTrack = SoundManager.selectRandomBackgroundTrack();
						deathMatch = true;
						createPlayers(winSize);
						state = WAITING;
						break;
					case '3':
						SoundManager.MENUCLICK.play();
						state = MAINMENU;
						break;
				}
			break;

			case OPTMENU:
				switch(key) {
					case '1':
						SoundManager.MENUCLICK.play();
						// Show game info text
						state = GAMEINFO;
						break;
					case '2':
						SoundManager.unmute();
						SoundManager.MENUCLICK.play();
						// Toggle sound ON/OFF
						if (sound) {
							sound = false;
							SoundManager.mute();
						}
						else {
							sound = true;
							SoundManager.unmute();
						}
						break;
					case '3':
						SoundManager.MENUCLICK.play();
						// Toggle controller MOUSE/KEYS
						if (mouse) {
							mouse = false;
						}
						else {
							mouse = true;
						}
						break;
					case '4':
						SoundManager.MENUCLICK.play();
						// Toggle difficulty EASY/MEDIUM/HARD
						difficulty = (difficulty+1)%3;

						switch(difficulty) {
						case 0: pause = easyPause; break;
						case 1: pause = medPause; break;
						case 2: pause = hardPause; break;
						}
						
						break;
					case '5':
						SoundManager.MENUCLICK.play();
						// Toggle paddle wrapping ON/OFF
						if (Player.getWrap()) {
							Player.setWrap(false);
						}
						else {
							Player.setWrap(true);
						}
						break;
					case '6':
						SoundManager.MENUCLICK.play();
						// Toggle ball trail ON/OFF
						if (ballTrail)
							ballTrail = false;
						else
							ballTrail = true;
						break;
					case '7':
						SoundManager.MENUCLICK.play();
						// Toggle game orientation HORIZ/VERT
						if (orientation) {
							orientation = false;
							createPlayers(winSize);
						}
						else {
							orientation = true;
							createPlayers(winSize);
						}
						break;
					case '8':
						SoundManager.MENUCLICK.play();
						state = MAINMENU;
						break;
				}
			break;
			
			case GAMEINFO:
				switch(key) {
					case Event.ENTER:
						SoundManager.MENUCLICK.play();
						state = OPTMENU;
						break;
				}
			break;
			
			case CHECK:
				switch(key) {
					case 'Y': case 'y':
						SoundManager.MENUCLICK.play();
						if(prevState == WAITING) {
							rplayer.resetScore();
							lplayer.resetScore();
							state = MAINMENU;
						}
						else
							System.exit(0);
						break;
					case 'N': case 'n':
						SoundManager.MENUCLICK.play();
						state = prevState;
						break;
				}
			break;
			}
		}
		

	@Override
	public void keyReleased(KeyEvent e) {
		int key = e.getKeyCode();
		switch(state) {
		case PLAYING:
			playingPressed.remove(key);
			if (orientation) {
				if (key == KeyEvent.VK_DOWN)
					rplayer.down = false;
				if (key == KeyEvent.VK_UP)
					rplayer.up = false;
				if (key == KeyEvent.VK_Z)
					lplayer.down = false;
				if (key == KeyEvent.VK_A)
					lplayer.up = false; 
			}
			else {
				if (key == KeyEvent.VK_RIGHT) 
					rplayer.down = false;
				if (key == KeyEvent.VK_LEFT)
					rplayer.up = false;
				if (key == KeyEvent.VK_A)
					lplayer.down = false;
				if (key == KeyEvent.VK_S)
					lplayer.up = false; 
			}
			rplayer.update();
			lplayer.update();
		}
	}
	};
	
}
	

