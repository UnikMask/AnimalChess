package LionShogi;

import java.util.ArrayList;
import java.util.Scanner;

/** Class implementing UIHandler */
public class TermUI implements UIHandler
{
	static int flushSize = 0;
	static ArrayList<String> printList = new ArrayList<>();

	// Grid bars for output design.
	private static final char GRID_BR_LEFT = '─';
	private static final char GRID_BR_UP = '│';
	private static final char GRID_UP_L = '┐';
	private static final char GRID_UP_R = '┌';
	private static final char GRID_DN_R = '└';
	private static final char GRID_DN_L = '┘';
	private static final char GRID_SD_R = '├';
	private static final char GRID_SD_L = '┤';
	private static final char GRID_SD_U = '┬';
	private static final char GRID_SD_D = '┴';
	private static final char GRID_CROS = '┼';

	// ANSI escape codes.
	private static final String ANSI_NORMAL_COL = "\033[0m";
	private static final String ANSI_MOVE_TO_END_COL = "\033[5000C";
	private static final String ANSI_UP_INCREMENT = "\033[1A";
	private static final String ANSI_UP_ALL = "\033[5000A";
	private static final String ANSI_CLEAR = "\033[2J";
	private static final String ANSI_ERASE_LINE = "\033[2K";

	// ANSI colors
	private static final String WHITE = "15m";
	private static final String BLACK = "0m";
	private static final String YELLOW = "130m";
	private static final String GREEN = "34m";
	private static final String RED = "196m";
	private static final String ANSI_BG = "\033[48;5;";
	private static final String ANSI_FG = "\033[38;5;";

	// Board text
	private static final String WHITE_CAPTURE = "White's captures: ";
	private static final String BLACK_CAPTURE = "Black's captures: ";
	private static final byte BOARD_SIZE_LIM = 99;

	@Override
	public void showBoard(GameState showState, GameRules currentRules)
	{
		// Get content for white and black captures
		String[] captureContents = new String[] {"", ""};
		for (byte piece: showState.capturesP0)
			captureContents[0] += currentRules.getSymbol(piece) + " ";
		for (byte piece: showState.capturesP1)
			captureContents[1] += currentRules.getSymbol(piece) + " ";

		// Print white capture box
		printBox(captureContents[1], WHITE_CAPTURE, this);
		println("");

		// Skip line and print column numbers.

		// Get array of column number string sizes.
		int lim_len = String.valueOf(BOARD_SIZE_LIM).length();
		char[][] lenCharArray = new char[showState.boardArray[0].length][lim_len];

		// Fill array of column numbers.
		for (int i = 0; i < lenCharArray.length; i++)
		{
			char[] currIntCharArray = String.valueOf(i).toCharArray();
			for (int j = 0; j < lim_len; j++)
			{
				if (currIntCharArray.length <= j) lenCharArray[i][j] = ' ';
				else lenCharArray[i][j] = currIntCharArray[j];
			}
		}

		// Loop print column number seperated by space.
		for (int i = 0; i < lim_len; i++)
		{
			print(" ");
			for (int j = 0; j < lim_len; j++) print(" ");
			for (int j = 0; j < showState.boardArray[0].length; j++)
			{
				String iStr = lenCharArray[j][i] + " ";
				print(iStr);
			}
			if (i < lim_len - 1) println("");
		}

		// Loop board printing
		for (int y = 0; y <= showState.boardArray.length; y++)
		{
			// Boolean to know if it loop should print another row.
			boolean endReached = showState.boardArray.length <= y;

			// Go to new line to print next row.
			println("");
			for (int i = 0; i < lim_len; i++) print(" ");
			print(String.valueOf(ANSI_BG) + YELLOW);

			// Determine which characters to pick for grid making.
			char edge_left = GRID_SD_R;
			char edge_right = GRID_SD_L;
			char mid = GRID_CROS;
			if (y == 0){ // Beginning of grid.
				
				edge_left = GRID_UP_R;
				edge_right = GRID_UP_L;
				mid = GRID_SD_U;
			}
			else if (y == showState.boardArray.length) // End of grid.
			{
				edge_left = GRID_DN_R;
				edge_right = GRID_DN_L;
				mid = GRID_SD_D;
			}

			// Print line for row.
			print(edge_left + "");
			for(int i = 0; i < showState.boardArray[0].length; i++)
			{
				print(GRID_BR_LEFT + "");
				if (i < showState.boardArray[0].length - 1) 
					print(mid + "");
				else print(edge_right + "");
			}
			println(ANSI_NORMAL_COL); // Return color to normal.

			// Print row number and set board color.
			if (!endReached) 
			{
				String rowNumStr = String.valueOf(y);
				int trailing = lim_len - rowNumStr.length();
				for (int i = 0; i < trailing; i++) rowNumStr += " ";
				
				print(rowNumStr + ANSI_BG + YELLOW + GRID_BR_UP);
			}

			// Print pieces of current row.
			for (int x = 0; x < showState.boardArray[0].length && !endReached; x++)
			{
				byte[] pieceInfo = showState.boardArray[y][x]; // Get info for piece
				String showString = "";
				char pieceChar = currentRules.getSymbol(pieceInfo[0]);

				// Determine color of the piece.
				if (pieceInfo[1] > 0) // Piece belongs to a camp
				{
					// Get piece color from faction
					if (showState.stateMode == GameState.StateMode.MOVE_SELECT)
					{
						if (pieceInfo[2] > 0)
							showString += ANSI_BG + RED + ANSI_FG + WHITE;
						else if (pieceInfo[1] == 1)
							showString += ANSI_BG + BLACK + ANSI_FG + WHITE;
						else if (pieceInfo[1] == 2)
							showString += ANSI_BG + WHITE + ANSI_FG + BLACK;
					}
					else if (pieceInfo[1] == 1)
						showString += ANSI_BG + BLACK + ANSI_FG + WHITE;
					else if (pieceInfo[1] == 2)
						showString += ANSI_BG + WHITE + ANSI_FG + BLACK;
						

					showString += pieceChar + ANSI_NORMAL_COL + ANSI_BG + YELLOW;
				}
				// Move select phase --> color possible moves into empty spaces green.
				else if (showState.stateMode == GameState.StateMode.MOVE_SELECT)
				{
					if (pieceInfo[2] != 0)
						showString += ANSI_BG + GREEN + pieceChar 
							+ ANSI_NORMAL_COL + ANSI_BG + YELLOW;
					else showString += pieceChar;
				}
				// Piece does not belong to a camp or is empty.
				else showString += pieceChar;

				// Add grid to the string
				showString += GRID_BR_UP;

				// Print result
				print(showString);
			}
			print(ANSI_NORMAL_COL); // Return color to normal.

		}

		// Add space between board and captures for clarity.
		println("\n");

		// Print black captures box
		printBox(captureContents[0], BLACK_CAPTURE, this);
		println("");

		// Print the print list.
		for (String nextPrint : printList)
		{
			print(nextPrint);
		}
		printList = new ArrayList<>();
		println("");

		// Prints user input if game hasn't ended.
		if (showState.victor == 0)
		{
			// Print what action should user do
			String playerStr = " Black ";
			if (!showState.P0Turn) playerStr = "White ";
			if (showState.stateMode == GameState.StateMode.MOVE_SELECT)
				println(playerStr + "selects a case to move piece to: ");
			else 
				println(playerStr + "plays:");

			// Print input line
			print (">>");
		}

	}

	/**
	* Prints a simple box with title and content.
	* @param content the content of a box - single line only.
	* @param title the title of the box.
	* @param termUI which termUI project prints it.
	 */
	public static void printBox(String content, String title, TermUI termUI)
	{
		// Print top layer 
		String baseLine = GRID_UP_R + title;
		int size = title.length();
		termUI.print(baseLine);
		for (int i = content.length() - size; i > 0; i--)
		{
			termUI.print(GRID_BR_LEFT + "");
			size++;
		}
		termUI.println(GRID_UP_L + "");

		// Print content layer
		termUI.print(GRID_BR_UP + "");
		String printContent = "";
		printContent += content;
		for (int i = content.length(); i < size; i++)
			printContent += ' ';
		termUI.println(printContent + GRID_BR_UP + "");

		// Print bottom layer
		termUI.print(GRID_DN_R + "");
		for (int i = 0; i < size; i++) termUI.print(GRID_BR_LEFT + "");
		termUI.println(GRID_DN_L + "");
	}

	@Override
	public void reset()
	{
		while(flushSize > 0) {
			System.out.print(ANSI_ERASE_LINE + ANSI_UP_INCREMENT);
			flushSize--;
		}
	}

	/**
	* Input getter passing through the UI handler.
	* @return the line that has been passed as input by the user.
	 */
	@Override
	public String getInputLine(Scanner inputScanner)
	{
		String inputLine = inputScanner.nextLine();
		flushSize++;
		return inputLine;
	}

	/**
	* Printing which passes through UI handler for flush handling
	* @param msg the message to print out.
	 */
	@Override
	public void println(String msg)
	{
		for(char msg_char : msg.toCharArray())
			if (msg_char == '\n') flushSize++;
		System.out.println(msg);
		flushSize++;
	}

	@Override
	public void keepToPrintList (String msg)
	{
		// Add the message to the print list.
		printList.add(msg);
	}

	/**
	* Printing which passes through UI handler for flush handling.
	* @param msg the message to print out.
	 */
	@Override
	public void print(String msg)
	{
		for(char msg_char : msg.toCharArray())
			if (msg_char == '\n') flushSize++;
		System.out.print(msg);
	}
}
