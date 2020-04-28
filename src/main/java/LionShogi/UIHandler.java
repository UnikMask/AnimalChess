package LionShogi;
import java.util.Scanner;

/** Interface for handling GUI or TUI */
public interface UIHandler
{
	/**
	* Updates/prints the board on the screen.
	* @param showState the state of the game to show
	* @param currentRules the game rules object for piece symbols to show.
	 */
	public void showBoard(GameState showState, GameRules currentRules);

	/** Put cursor/screen back to original state. */
	public void reset(); 

	/**
	* Prints a message from the UI
	* @param msg the message to print.
	 */
	public void print(String msg);

	/**
	* Prints a line from the UI.
	* @param msg the line to print.
	 */
	public void println(String msg);

	/**
	* Getter for input line that uses the UI.
	* @param inputScanner the scanner for input.
	* @return the input line string.
	 */
	public String getInputLine(Scanner inputScanner);

	/**
	* Keeps a message in a print list to print afterwards.
	* @param msg the message to keep.
	 */
	public void keepToPrintList (String msg);
}
