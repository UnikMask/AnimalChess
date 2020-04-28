package LionShogi;

import java.util.ArrayList;

/** Class representing a game state */
public class GameState
{
	byte[][][] boardArray;
	ArrayList<Byte> capturesP0 = new ArrayList<>();
	ArrayList<Byte> capturesP1 = new ArrayList<>();
	byte[] selectPiece;
	boolean P0Turn;
	byte victor;
	StateMode stateMode;

	/** Mode of the game state */
	enum StateMode
	{
		PAWN_SELECT ("select"),
		MOVE_SELECT ("endTurn");

		private String modeStr;
		public String get() 
		{
			return modeStr;
		}
		private StateMode (String modeStr)
		{
			this.modeStr = modeStr;
		}
	}

	/**
	* Getter for this game state in selection mode.
	* @param possibleMoves the array of possible moves.
	* @param selectPiece the piece selected.
	* @return the new game state.
	 */
	public GameState setSelectMode(byte[][] possibleMoves, byte[] selectPiece)
	{
		if (possibleMoves.length == boardArray.length
				&& possibleMoves[0].length == boardArray[0].length)
		{
			// Initialize new board.
			byte[][][] selectBoard = 
				new byte[boardArray.length][boardArray[0].length][3];

			// Copy content into the selection board.
			for (int y = 0; y < boardArray.length; y++)
				for (int x = 0; x < boardArray[y].length; x++)
				{
					selectBoard[y][x][0] = boardArray[y][x][0];
					selectBoard[y][x][1] = boardArray[y][x][1];
					selectBoard[y][x][2] = possibleMoves[y][x];
				}

			// Return the new game state.
			return new GameState (selectBoard,
					this.capturesP0, this.capturesP1, this.P0Turn, selectPiece);
		}
		else 
		{
			StartShogi.println("ERROR in select mode!");
			return null;
		}
	}

	/**
	* Getter for a game state with that move played
	* @param pos_0 the position of base piece.
	* @param pos_1 the new position of that piece.
	* @return the new game state.
	 */
	public GameState playMove(byte[] pos_0, byte[] pos_1)
	{
		// Initialize new board
		byte[][][] newBoard = new byte[boardArray.length][boardArray[0].length][2];
		
		// Copy old board to new board
		for (int y = 0; y < newBoard.length; y++)
			for (int x = 0; x < newBoard[0].length; x++)
				for (int i = 0; i < newBoard[0][0].length; i++)
					newBoard[y][x][i] = boardArray[y][x][i]; 

		// Initialize moved position.
		byte[] movePos = newBoard[pos_1[0]][pos_1[1]];
		byte[] movePiece = newBoard[pos_0[0]][pos_0[1]];

		// Check if move piece is of wrong group
		if ((P0Turn && movePiece[1] == 2) || (!P0Turn && movePiece[1] == 1))
		{
			StartShogi.println("You cannot move another user's piece!");
			return this;
		}

		// Copy capture lists
		ArrayList<Byte> nCapturesP0 = new ArrayList<>(capturesP0.size());
		for (Byte currCapture : capturesP0) nCapturesP0.add(currCapture);
		ArrayList<Byte> nCapturesP1 = new ArrayList<>(capturesP1.size());
		for (Byte currCapture : capturesP1) nCapturesP1.add(currCapture);

		// Move pieces
		if (movePos[1] == 1) // Piece from black camp has been captured. 
		{
			// Demote the piece that has been takem.
			movePos[0] = LogicHandler.shogiRules.getDemote(movePos[0]);
			nCapturesP1.add(movePos[0]);
		}
		if (movePos[1] == 2) // Piece from white camp has been captured.
		{
			movePos[0] = LogicHandler.shogiRules.getDemote(movePos[0]);
			nCapturesP0.add(movePos[0]);
		}
		newBoard[pos_1[0]][pos_1[1]] = movePiece;
		newBoard[pos_0[0]][pos_0[1]] = new byte[] {0, 0}; // Make move piece empty.

		// Check if piece is within promotion range
		byte[] promoProps = LogicHandler.shogiRules.getPromotionProperties(movePiece[0]);
		boolean isInBounds = false;
		if (movePiece[1] == 1) 
			isInBounds = pos_1[0] < promoProps[1];
		else isInBounds = pos_1[0] >= newBoard.length - promoProps[1];
		if (isInBounds)
			newBoard[pos_1[0]][pos_1[1]][0] = promoProps[0];

		// Set new game state
		return new GameState (newBoard, nCapturesP0, nCapturesP1, !P0Turn, null);

	}

	/**
	* Plays a move on a new game state from the selected piece.
	* @param pos_1 the position to move the selected piece.
	* @return the new game state.
	 */
	public GameState playMove(byte[] pos_1)
	{
		if (selectPiece != null && stateMode == StateMode.MOVE_SELECT)
		{
			if (boardArray[pos_1[0]][pos_1[1]][2] != 0)
			{
				return playMove(selectPiece, pos_1);
			}
			else 
			{
				StartShogi.println("ERROR : Move illegal! " 
					+ pos_1[0] + '-' + pos_1[1]);
				return this;
			}
		}
		else
		{
			StartShogi.println("ERROR : Can't play a move without a piece first!");
			return this;
		}
	}

	/**
	* Getter for a game state with a captured piece moved to the board.
	* @param captPosition the index of the captured piece.
	* @param movePos the position where the piece is moved to.
	* @return the new game state.
	 */
	public GameState placeCapture(byte captPosition, byte[] movePos)
	{
		// Initialize new board
		byte[][][] newBoard = new byte[boardArray.length][boardArray[0].length][2];
		
		// Copy old board to new board
		for (int y = 0; y < newBoard.length; y++)
			for (int x = 0; x < newBoard[0].length; x++)
				for (int i = 0; i < newBoard[0][0].length; i++)
					newBoard[y][x][i] = boardArray[y][x][i]; 
		
		// Copy capture lists
		ArrayList<Byte> nCapturesP0 = new ArrayList<>(capturesP0.size());
		for (Byte currCapture : capturesP0) nCapturesP0.add(currCapture);
		ArrayList<Byte> nCapturesP1 = new ArrayList<>(capturesP1.size());
		for (Byte currCapture : capturesP1) nCapturesP1.add(currCapture);

		// Remove capture from right capture.
		ArrayList<Byte> capturePtr;
		if (P0Turn) capturePtr = nCapturesP0;
		else capturePtr = nCapturesP1;
		if (captPosition < capturePtr.size())
		{
			// Add captured piece to board at position
			newBoard[movePos[0]][movePos[1]] = new byte[] {
				capturePtr.get(captPosition), 
				getTurnByte()
			};
			capturePtr.remove(captPosition);

			// Return final game state.
			return new GameState (newBoard, nCapturesP0, nCapturesP1, !P0Turn, null);
		}
		else return this;
	}

	/**
	* Cancels the move selection mode.
	* @return the new game state.
	 */
	public GameState cancelSelect()
	{
		if (stateMode == StateMode.MOVE_SELECT)
		{
			// Remove 3rd layer from the board array.
			byte[][][] deboardArray = new byte[boardArray.length][boardArray[0].length][2];
			for (int y = 0; y < boardArray.length; y++)
				for (int x = 0; x < boardArray[0].length; x++)
					for (int i = 0; i < deboardArray[0][0].length; i++)
						deboardArray[y][x][i] = boardArray[y][x][i];

			// Return the new game state.
			return new GameState (deboardArray, capturesP0, capturesP1, P0Turn, null);
		}
		else
		{
			StartShogi.println("Nothing to cancel!");
			return this;
		}
	}

	/**
	* Indicates that this state is a forfeit.
	* @return this object, but with victory condition.
	 */
	public GameState forfeit ()
	{
		if (P0Turn) victor = 2;
		else victor = 1;
		return this;
	}

	/**
	* Constructor for a full game state
	* @param boardArray the array representing types and factions of pieces on the board.
	* @param capturesP0 the pieces captured by P0.
	* @param capturesP1 the pieces captured by P1.
	* @param P0Turn whether it's P0's turn or not.
	* @param selectPiece the piece selected.
	 */
	public GameState(byte[][][] boardArray,
			ArrayList<Byte> capturesP0, 
			ArrayList<Byte> capturesP1,
			boolean P0Turn, byte[] selectPiece)
	{
		// Define variables based on parameters.
		this.boardArray = boardArray;
		this.capturesP0 = capturesP0;
		this.capturesP1 = capturesP1;
		this.P0Turn = P0Turn;
		this.selectPiece = selectPiece;

		// Define the state mode on whether the array of possible moves is on the board array.
		if(boardArray[0][0].length == 3)
			stateMode = StateMode.MOVE_SELECT;
		else stateMode = StateMode.PAWN_SELECT;

		// Check board array for kings
		boolean p0king = false, p1king = false;
		for (int y = 0; y < boardArray.length; y++)
			for (int x = 0; x < boardArray[0].length; x++)
				if (boardArray[y][x][0] == 1)
				{
					if (boardArray[y][x][1] == 1)
						p0king = true;
					else p1king = true;
				}
		if (!p0king) victor = 2;
		else if (!p1king) victor = 1;
		else victor = 0;

	}

	/**
	* Getter for a piece by it's position.
	* @param piecePosition the position of the piece.
	* @return the representation of the piece.
	 */
	public byte[] selectPiece(byte[] piecePosition)
	{
		try
		{
			// Return the wanted piece.
			return boardArray[piecePosition[0]][piecePosition[1]];
		}
		catch (IndexOutOfBoundsException e) // In case input is out of bounds.
		{
			StartShogi.println("[ERROR]: position not within board bounds: " 
					+ piecePosition[0] + '-' + piecePosition[1]);
			return null;
		}
	}

	/**
	* Getter for the turn of the game as a byte.
	* @return the byte representing which turn is it.
	 */
	public byte getTurnByte ()
	{
		if (P0Turn) return 1;
		else return 2;
	}
}
