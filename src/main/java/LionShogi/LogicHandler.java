package LionShogi;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import javax.json.JsonException;

import LionShogi.json.*;

/** Class handling game logic, rounds, and board. */
public class LogicHandler implements ShogiSelector
{
	// Game rules class for game rules.
	public static GameRules shogiRules;

	// 3D array for board with piece types and piece faction.
	GameState currentState;

	// Chars for command separation
	private static final char NUM_SEP = '-';
	private static final char POS_SEP = ',';
	private static final String QUIT_KW = "quit|exit";
	private static final String TUT_KW = "help";
	private static final String CANCEL_KW = "cancel";
	private static final String FORFEIT_KW = "give up|forfeit";
	private static final String NUM_RX = "[0-9]*?";
	private static final String POS_KW = NUM_RX + NUM_SEP + NUM_RX;
	private static final String MOVE_KW = POS_KW + POS_SEP + POS_KW;
	private static final String CAPT_KW = "c";
	private static final String CAPT_RX = CAPT_KW + NUM_RX + "->" + POS_KW;

	// Variables for piece movement reading
	private static final char PIECE = 'O';
	private static final char ABS_MOVE = 'X';
	private static final char REL_MOVE = '/';
	private static final char NO_MOVE = '-';

	// Information for game state building.
	private int[] boardDims;
	boolean gameEnded;
	char[][] initJsonBoardArr;
	char[][] captures;
	String variantTitle;

	/**
	* Getter for a board position based on a string value (usually entered by command).
	* @param strPosition the board position string.
	* @return the position on the board in bytes.
	 */
	public byte[] getBoardPosition(String strPosition)
	{
		// Check if string is valid (i.e of form "y-x")
		String regex = "[0-9]*?" + NUM_SEP + "[0-9]*?";
		if (strPosition.matches(regex))
		{
			// Initialize position.
			byte[] boardPos = new byte[2];
			String[] strPos = strPosition.split(String.valueOf(NUM_SEP));

			// Check if length is same before translating.
			try
			{
				if (strPos.length == boardPos.length)
					for (int i = 0; i < strPos.length; i++)
						boardPos[i] = Byte.valueOf(strPos[i]);
			}
			catch (NumberFormatException e) // In case number too high for a byte.
			{
				StartShogi.println("ERROR: input sequence not within byte limits: "
						+ strPosition);
				return null;
			}

			// Return completed position.
			return boardPos;
		}
		// If input doesn't match format.
		else StartShogi.println("ERROR: input within standard format: "
				+ strPosition);
		return null;
	}


	/**
	* Processes current user input.
	* @param userInput the input of the user
	 */
	public void processRound (String userInput)
	{
		// Check user input for any keyword.
		userInput = userInput.toLowerCase();
		if (userInput.matches(QUIT_KW)) // If quit keyword found
		{
			StartShogi.println("Goodbye!");
			System.exit(0);
		}
		else if (userInput.matches(CANCEL_KW)) // If cancel keyword found
			currentState = currentState.cancelSelect();
		else if (userInput.matches(POS_KW)) // If matches one position
		{
			// If state is move select, play a move.
			if (currentState.stateMode == GameState.StateMode.MOVE_SELECT)
				currentState = playMove(userInput, currentState);
			else // Set game state to selection mode.
			{
				byte[] userPos = getBoardPosition(userInput);
				if (userPos != null)
				{
					byte[][] selectBoard = getPossibleMoves(userPos, currentState);
					if (selectBoard != null)
						currentState = currentState.setSelectMode(selectBoard, userPos);
				}
			}
		}
		else if (userInput.matches(MOVE_KW)) // If matches a whole move, play that whole move.
			currentState = playMove(userInput, currentState);
		else if (userInput.matches(CAPT_RX)) // In case player wants to place a captured stone.
			currentState = placeCapture(userInput, currentState);
		else if (userInput.matches(FORFEIT_KW)) // In case of a forfeit.
			currentState = currentState.forfeit();
		else // If no valid input is found.
			StartShogi.println("No valid input found! Try again.");

		// Check for victory conditions
		checkVictoryCondition();
	}

	/** Checks if victory has been achieved. If it is, end the game. */
	public void checkVictoryCondition ()
	{
		if (currentState.victor != 0)
			StartShogi.endGame(currentState.victor);
	}

	@Override
	public void onObject(HashMap<String, String> currObject) 
	{
		try
		{
			// Find the board object and get the board properties.
			if (currObject.get("name").equals("board"))
			{

				// Get dimensions of board
				boardDims = new int[]
				{
					intToByte(Integer.valueOf(currObject.get("height"))),
					intToByte(Integer.valueOf(currObject.get("width")))
				};

				// Get board placement
				initJsonBoardArr = new char[boardDims[0]][boardDims[1]];
				for (int i = 0; i < boardDims[0]; i++)
				{
					String currentKey = "placement_" + i;
					initJsonBoardArr[i] = currObject.get(currentKey).toCharArray();
				}

				// Get captures
				if (currObject.containsKey("p0_captures"))
					captures[0] = currObject.get("p0_captures").toCharArray();
				if (currObject.containsKey("p1_captures"))
					captures[1] = currObject.get("p1_captures").toCharArray();
			}

			// Get title of this game variant
			if (currObject.get("name").equals("root") && currObject.containsKey("title"))
				variantTitle = currObject.get("title");
		}
		catch(NullPointerException e)
		{
			System.out.println("ERROR: null pointer on map!");
			e.printStackTrace();
			System.exit(0);
		}
	}

	/**
	* Loads the logic handler and the game rules to a shogi variant script defined by it's path.
	* @param filepath the path to the variants file.
	* @param mainBot the ShogiBot to link to variant for parsing.
	 */
	public void loadVariant(String filepath, ShogiBot mainBot)
	{
		// Load the script parser.
		resetVars();

		// Get plug list.
		ShogiSelector[] plugList;
		if (mainBot == null)
			plugList = new ShogiSelector[] {this, shogiRules};
		else plugList = new ShogiSelector[] {this, shogiRules, mainBot};
		try (ShogiParser variantParser = new ShogiParser(filepath, plugList))

		{
			variantParser.parseToCompletion();
		}
		catch (JsonException e)
		{
			System.err.println("ERROR: On logic handler parsing!\nQuitting");
			e.printStackTrace();
			System.exit(0);
		}

		// Get initial game state
		currentState = loadInitialGameState();
		

	}

	/** Reset maps from script parsing */
	public void resetVars()
	{
		initJsonBoardArr = new char[][] {{'a'}};
		captures = new char[][] {null,null};
	}

	/**
	* Prints the board on a UI handler.
	* @param plugUI the UIHandler to plug to.
	 */
	public void plugAndPrintBoard(UIHandler plugUI)
	{
		plugUI.showBoard(currentState, shogiRules);
	}

	/**
	* Loads initial game state from the game rules.
	* @return the initial game state.
	 */
	public GameState loadInitialGameState()
	{
		// Initialize game board from board in JSON script.
		byte[][][] initBoard = new byte[boardDims[0]][boardDims[1]][2];
		for (int y = 0; y < boardDims[0]; y++)
			for (int x = 0; x < boardDims[1]; x++)
				initBoard[y][x] = shogiRules.getPiece(initJsonBoardArr[y][x]);

		// Initialize captures
		ArrayList<Byte> capturesP0 = new ArrayList<>();
		ArrayList<Byte> capturesP1 = new ArrayList<>();
		for (int i = 0; i < 2; i++)
			if (captures[i] != null) // Check if this capture is not empty. Else ignore.
				for (char piece: captures[i]) // Add capture to capture array.
				{
					if (i == 0) capturesP0.add(shogiRules.getPiece(piece)[0]);
					else capturesP1.add(shogiRules.getPiece(piece)[0]);
				}

		// Create game state, starting on black turn with 0 captures.
		return new GameState(initBoard, capturesP0, capturesP1, true, null);
	}

	/**
	* Plays a move in the given state
	* @param userInput the input given by the user.
	* @param watchState the state to link to.
	* @return the new game state.
	 */
	public GameState playMove (String userInput, GameState watchState)
	{
		// Check if doing a move is possible
		if (userInput.matches(POS_KW) 
				&& watchState.stateMode == GameState.StateMode.MOVE_SELECT)
		{
			// Get user position as integer
			byte[] bytePos = getBoardPosition(userInput);
			if (bytePos == null) // If null, then there is an error in user input.
				return watchState;

			int[] intPos = new int[]{
				getBoardPosition(userInput)[0],
				getBoardPosition(userInput)[1]
			};

			int[] currDims = new int[]
			{
				watchState.boardArray.length,
				watchState.boardArray[0].length
			};
			if (checkBounds(intPos, currDims))
			{
				return watchState.playMove(getBoardPosition(userInput));
			}
			else // If chosen position is out of bounds.
			{
				StartShogi.println("ERROR: Position out of bounds! " + userInput);
				return watchState;
			}
		}
		else if (userInput.matches(MOVE_KW)) // User wants to make a whole move.
		{
			// Get the two given positions.
			String[] posStrArr = userInput.split(","); 
			byte[][] positionArr = new byte[][] {
				getBoardPosition(posStrArr[0]),
				getBoardPosition(posStrArr[1])
			};
			// If input was invalid.
			if (positionArr[0] == null || positionArr[1] == null)
				return watchState;

			byte[][] selectBoard = getPossibleMoves(positionArr[0], watchState);
			if (selectBoard == null ) return watchState; // Means input is invalid.

			// Convert 2nd position array to an int array.
			int[] pos_1 = new int[] {
				positionArr[1][0],
				positionArr[1][1]
			};

			// Check if next position in bounds
			if (checkBounds(pos_1, new int[] {selectBoard.length, selectBoard[0].length}))
			{
				// Check if that move is possible.
				if (selectBoard[pos_1[0]][pos_1[1]] != 0)
					return watchState.playMove(positionArr[0], positionArr[1]);
				else // The chosen move is illegal.
				{
					StartShogi.print("This move is illegal! " + userInput);
					return watchState;
				}
			}
			else
			{
				StartShogi.println("Chosen move is out of bounds! " + pos_1);
				return watchState;
			}
		}
		else 
		{
			StartShogi.println("ERROR: wrong input: " + userInput);
			return watchState;
		}
	}

	/**
	* Sets the logic handler to a new state
	* @param newState the new game state
	 */
	public void setState (GameState newState)
	{
		// Get currentState switched
		currentState = newState;

		// Check winning conditions
		checkVictoryCondition();
	}

	/**
	* Returns a byte array of possible moves according to a piece position
	* and a given game state.
	* @param piecePos the position of the piece to look at.
	* @param watchState the game state to look out for possible moves.
	* @return the byte array of possible moves on the board with current piece.
	 */
	public byte[][] getPossibleMoves(byte[] piecePos, GameState watchState)
	{
		// Initialize variables for possible moves
		byte[] currentPiece = watchState.selectPiece(piecePos); // The current piece array.
		if (currentPiece == null) return null; // Piece is invalid.
		// Piece color invalid.
		else if (currentPiece[1] != watchState.getTurnByte())
		{
			StartShogi.println("ERROR: Cannot select an enemy's piece! ");
			return null;
		}

		char[][] movementProps = shogiRules.getPieceMovement(currentPiece[0]); 

		byte[][][] currBoard = watchState.boardArray; // The board array.
		byte[][] selectionBoard = new byte[currBoard.length][currBoard[0].length];
		int[] currDims = new int[] {currBoard.length, currBoard[0].length}; // Dims of the board.

		// Find piece position in piece movement properties.
		byte[] rel_pos = null;
		for (byte rel_y = 0; rel_y < movementProps.length; rel_y++)
			for (byte rel_x = 0; rel_x < movementProps[0].length; rel_x++)
				if (movementProps[rel_y][rel_x] == PIECE)
				{
					rel_pos = new byte[] {
						rel_y, rel_x
					};
					break;
				}

		// Set selection board full of 0s
		for (int i = 0; i < currDims[0]; i++)
			for (int j = 0; j < currDims[1]; j++)
				selectionBoard[i][j] = 0;

		// Check if piece position in movement properties was indeed found.
		if (rel_pos != null)
		{
			// Loop for movement properties and translate chars to possible moves.
			for (byte i = 0; i < movementProps.length; i++)
				for (byte j = 0; j < movementProps[0].length; j++)
				{
					// Get increment to absolute position.
					int[] increment;
					if (currentPiece[1] == 1)
						increment = new int[] {rel_pos[0] - i, rel_pos[1] - j};
					else 
						increment = new int[] {i - rel_pos[0], j - rel_pos[1]};

					// Get absolute position from increment and piece position.
					int[] absPos = new int[] {
						piecePos[0] - increment[0], 
						piecePos[1] - increment[1]
					};

					// Switch for the movement encountered.
					switch (movementProps[i][j])
					{
						case ABS_MOVE: // If character points to a single move.
							if (checkBounds(absPos, currDims))
							{
								// Add validity to selection board.
								byte posCamp = currBoard[absPos[0]][absPos[1]][1];
								selectionBoard[absPos[0]][absPos[1]] = 
									checkMove(currentPiece[1], posCamp);
							}
							break;
						case REL_MOVE: // If characters points  towards a line.
						{
							// Increment increment to absolute position until line breaks.
							boolean lineBreak = false;
							while (!lineBreak)
							{
								// Check if new absolute position is within bounds.
								if (checkBounds(absPos, currDims))
								{
									// Add validity to selection board.
									byte lineCamp = currBoard[absPos[0]][absPos[1]][1];
									selectionBoard[absPos[0]][absPos[1]] = 
										checkMove(currentPiece[1], lineCamp);

									// Check if line breaks
									if (selectionBoard[absPos[0]][absPos[1]] != 1)
										lineBreak = true;
								}
								else lineBreak = true;

								// Add increment to absolute position.
								for (int l = 0; l < 2; l++)
									absPos[l] -= increment[l];
							}
							break;
						}
							
					}
				}
			// Return final selection board. 
			return selectionBoard;
		}
		else return null;
	}

	/**
	* Places a capture in the specified game state.
	* @param userInput the user's input.
	* @param watchState the game state to link to.
	* @return the new game state.
	 */
	public GameState placeCapture (String userInput, GameState watchState)
	{
		if (userInput.matches(CAPT_RX))
		{
			// Split user input by the arrow and remove capture key letter.
			String[] commandArr = userInput.replaceAll(CAPT_KW, "").split("->");

			// Convert all values to integers.
			byte captIndex = Byte.valueOf(commandArr[0]);
			captIndex -= 1;
			byte[] byteMove = getBoardPosition(commandArr[1]);

			// Get right capture array list according to right
			ArrayList<Byte> captures;
			if (watchState.P0Turn) captures = watchState.capturesP0;
			else captures = watchState.capturesP1;

			// Check if all values are correct
			if (captures.size() <= captIndex) // index is wrong.
			{
				StartShogi.println("[ERROR] index of captured pieces is invalid! " + captIndex);
				return watchState;
			}
			if (byteMove == null) // value not a byte - error already reported.
				return watchState;

			// get move as an integer and board dimensions.
			int[] intMove = new int[] {
				byteMove[0], byteMove[1]
			};
			int[] currDims = new int[] {
				watchState.boardArray.length,
				watchState.boardArray[0].length
			};

			// Check if new position is within bounds.
			if (!checkBounds(intMove, currDims))
			{
				StartShogi.println("[ERROR] position to place piece is out of bounds");
				return watchState;
			}
			else if (watchState.boardArray[intMove[0]][intMove[1]][1] != 0)
			{
				StartShogi.println("Can't place a captured piece in a non-empty space !");
				return watchState;
			}
			else return watchState.placeCapture(captIndex, byteMove);
		}
		else
		{
			StartShogi.println("[ERROR] Wrong string format for capture! ");
			return watchState;
		}
	}

	/**
	* Getter for blank spaces, which is the equivalent of possible moves for captured pieces.
	* @param watchState the state to link to.
	* @return the selection board.
	 */
	public byte[][] getBlankSpaces (GameState watchState)
	{
		// Initialize dimension and blank spaces board.
		int[] currDims = {
			watchState.boardArray.length,
			watchState.boardArray[0].length
		};
		byte[][] selectBoard = new byte[currDims[0]][currDims[1]];

		// Loop through all elements of the board array
		for (int y = 0; y < currDims[0]; y++)
			for (int x = 0; x < currDims[1]; x++)
				if (watchState.boardArray[y][x][0] == 0)
					selectBoard[y][x] = 1;
				else selectBoard[y][x] = 0;

		// Return the board.
		return selectBoard;
	}

	/**
	* Checks what kind of move a piece does and returns it.
	* @param pieceCamp the camp of that piece (1 or 2).
	* @param posCamp the camp of the position to move to.
	* @return the validity and kind of this move. (0=invalid, 1=move, 2=capture).
	 */
	public byte checkMove(byte pieceCamp, byte posCamp)
	{
		// Get what kind of case the piece goes to.
		int endMove = (pieceCamp + posCamp) % 3;

		if (endMove == pieceCamp) // Piece moves to empty space.
			return 1;
		else if (endMove == 0) // Piece captures enemy piece.
			return 2;
		else return 0; // Piece moves to allied piece - prohibited so returns 0.

	}

	/**
	* Checks if a position is within bounds.
	* @param pos the position to check bounds for.
	* @param boardDims the dimensions of the board.
	* @return whether this position is in or out of bounds.
	 */
	public boolean checkBounds (int[] pos, int[] boardDims)
	{
		// Loop check if position is out of bounds.
		for (int i = 0; i < 2; i++)
			if (pos[i] >= boardDims[i] || pos[i] < 0)
				return false;
		return true;
	}

	/**
	* Method that converts an integer to byte - or more precisely returns the first byte.
	* @param smallInt the integer to convert
	* @return the first byte of the integer - if it was small, returns it as a byte.
	 */
	public static byte intToByte(int smallInt)
	{
		ByteBuffer smallIntToByte = ByteBuffer.allocate(4);
		smallIntToByte.putInt(smallInt);
		return smallIntToByte.array()[3];
	}

	/**
	* Getter for the sum of all elements in an array of bytes or ints.
	* @param sumArray the array of all elements of E to make the sum of.
	* @return the sum of all those elements.
	 */
	public int getSumOf (byte[][] sumArray)
	{
		int sum = 0;

		// Add all elements of sum array to the sum.
		for (int i = 0; i < sumArray.length; i++)
			for (int j = 0; j < sumArray[i].length; j++)
				sum +=  sumArray[i][j];

		return sum;
	}

	public LogicHandler()
	{
		resetVars();
		shogiRules = new GameRules();
	}
}
