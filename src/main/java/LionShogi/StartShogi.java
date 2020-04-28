package LionShogi;
import java.util.Random;
import java.util.Scanner;

import java.io.File;

/** Main class of the shogi program */
public class StartShogi
{
	// Constant variables for string displays and misc.
	public static final String LION_PATH = "variants/lionShogi.json";
	public static final String MODERN_PATH = "variants/modernShogi.json";
	public static final String HELP_MESSAGE = 
		"Help on launching LionShogi: \n"
		+ "-Arguments - only 1 argument. Possible arguments: \n"
		+ "\t term - Use the TextUI interface"
		+ "\t fx - Use the JavaFX GUI"
		+ "\t help - Display this help window";

	// Main components of program.
	public static StartShogi instance;
	private UIHandler systemUI;
	private String filepath = "variants/lionShogi.json";
	boolean game = true;
	boolean bot = false;
	boolean playerTurn = false;


	/**
	* Main method of the program
	* @param args useless here.
	 */
	public static void main(String[] args)
	{
		// Create main StartShogi object.
		Scanner mainScanner = new Scanner(System.in);
		instance = new StartShogi();
		instance.systemUI = new TermUI();
		instance.selectVariant(mainScanner);
		instance.chooseMode(mainScanner);

		// Initialize logic handler.
		LogicHandler mainHandler = new LogicHandler();

		// If bot is true, create a new shogi bot.
		ShogiBot mainBot = null;
		if (instance.bot) mainBot = new ShogiBot(mainHandler);

		// Try loading a variant.
		if (new File(instance.filepath).exists())
		{
			mainHandler.loadVariant(instance.filepath, mainBot);

			while (instance.game) // Game loop
			{
				// If it's not player's turn, have bot move.
				if (!instance.playerTurn && instance.bot) 
				{
					mainHandler.setState( 
						mainBot.chooseBestState(mainHandler.currentState));
					println("Bot played! ");
					instance.playerTurn = !instance.playerTurn;
				}

				// Reset the GUI and show board.
				instance.systemUI.reset();
				mainHandler.plugAndPrintBoard(instance.systemUI);

				// Note evolution in turn
				boolean currentRound = mainHandler.currentState.P0Turn; 

				//Process round
				mainHandler.processRound(instance.systemUI.getInputLine(mainScanner));

				// Notify change in turn
				if (mainHandler.currentState.P0Turn != currentRound)
					instance.playerTurn = !instance.playerTurn;

			}
			// Reset the GUI and show board.
			instance.systemUI.reset();
			mainHandler.plugAndPrintBoard(instance.systemUI);
		}
		else
			System.out.println("ERROR: File non-existant. Try something else.");
	}

	/** Asks user for variant selection. 
	 * @param variantScanner the scanner to get input from.
	 */
	public void selectVariant(Scanner variantScanner)
	{
		systemUI.print("Choose variant:\n\t1- Lion Shogi\n\t2- Modern Shogi\n>>");
		int varNum = 0;

		// Get positive integer input
		while (varNum == 0)
		{
			String systemInput = systemUI.getInputLine(variantScanner);
			if (systemInput.matches("[0-9]*?"))
				if (Integer.valueOf(systemInput) <= 2)
					varNum = Integer.valueOf(systemInput);
			else
				systemUI.println("Input has to be lower or equal to 2. Your input: "
						+ systemInput
						+ "\nTry again: >>");
			else
				systemUI.println("Input has to be positive number. Your input: "
						+ systemInput
						+ "\nTry again: >>");
		}

		// Get variant number from input.
		switch (varNum)
		{
			case 1:
				filepath = LION_PATH;
				break;
			case 2:
				filepath = MODERN_PATH;
		}

		// Reset UI.
		systemUI.reset();
	}

	/**
	* Asks user for game mode (PvP or PvE)
	* @param modeScanner the scanner for user input.
	 */
	public void chooseMode (Scanner modeScanner)
	{
		systemUI.println("Choose mode :\n\t1-player vs player\n\t2-player vs bot");
		systemUI.print(">>");
		int modeNum = 0;

		while (modeNum == 0)
		{
			String systemInput = systemUI.getInputLine(modeScanner);
			if (systemInput.matches("[1-2]")) // Is a number between 1 and 2.
				modeNum = Integer.valueOf(systemInput);
			else // In case input is incorrect.
				systemUI.println("Input incorrect: " + systemInput
						+ "\nTry again! >>");

		}

		// Get mode from system input value.
		switch (modeNum)
		{
			case 1:
				bot = false;
				break;
			case 2:
			{
				bot = true;
				systemUI.println("player versus bot mode on! Side will be chosen " 
						+ "randomly. Please wait..." );
				int randInt = new Random().nextInt(2);
				playerTurn = randInt % 2 == 0;
				try
				{
					Thread.sleep(1000); // Let the user read the message above.
				}
				catch (InterruptedException e)
				{
					systemUI.println("Oops. Who interrupted the ogre in his sleep?");
				}
			}
		}
	}

	/**
	* Ends the game with a winning message.
	* @param winningSide which side is victor.
	 */
	public static void endGame(byte winningSide)
	{
		// Signal that game has ended.
		String victor = "Black";
		if (winningSide == 2) victor = "White";
		println(victor + " player wins the game!");
		instance.game = !instance.game;
	}

	/**
	* Uses UI handler to print a line.
	* @param msg the message to print.
	 */
	public static void println(String msg)
	{
		instance.systemUI.keepToPrintList(msg + "\n");
	}
	
	/**
	* Uses UIHandler to print message.
	* @param msg the message to print.
	 */
	public static void print(String msg)
	{
		instance.systemUI.keepToPrintList(msg);
	}
}
