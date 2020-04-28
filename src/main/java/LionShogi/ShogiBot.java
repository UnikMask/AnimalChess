package LionShogi;
import java.util.ArrayList;
import java.util.HashMap;

import LionShogi.json.*;

/** Class representing a bot for shogi. */
public class ShogiBot implements ShogiSelector
{
	// Variables for keeping with state of the game.
	GameRules shogiRules;
	LogicHandler mainHandler;
	byte side = 0;
	int baseLayer = 3;
	int MIN_ALPHA = 0;
	int MAX_BETA = 10000;

	@Override
	public void onObject(HashMap<String, String> currentObject)
	{
		// Find properties of bot for a shogi variant.
		if (currentObject.get("name").equals("bot"))
		{
			if (currentObject.containsKey("layers"))
				if (currentObject.get("layers").matches("[0-9]*?"))
					baseLayer = Integer.valueOf(currentObject.get("layers"));
		}
	}

	/**
	* Getter for an array list of all possible game states from a base game state.
	* @param baseState the game state the other game states are based off
	* @return the array list of game states.
	 */
	public ArrayList<GameState> getAllGameStates(GameState baseState)
	{
		// Initialize array list.
		ArrayList<GameState> gameStateArray = new ArrayList<>();

		// Initialize board dimensions and initialize pointer to board.
		byte[][][] board = baseState.boardArray;
		int[] currDims = new int[] {board.length, board[0].length};

		// Get side of the bot.
		byte botSide = 2;
		if (baseState.P0Turn) botSide = 1;

		// Get pointer for capture array using side.
		ArrayList<Byte> captures = baseState.capturesP1;
		if (botSide == 1) captures = baseState.capturesP0;

		// Loop get a game state for all pieces of same side as in game state's round.
		for (byte y = 0; y < currDims[0]; y++)
		{
			for (byte x = 0; x < currDims[1]; x++)
			{
				// Get list of found piece's all possible moves.
				if (board[y][x][1] == botSide)
				{
					// Get array of possible moves
					byte[][] possMvs = 
						mainHandler.getPossibleMoves(new byte[] {y,x}, baseState);

					// Loop through all possible moves
					for (byte y_1 = 0; y_1 < currDims[0]; y_1++)
						for (byte x_1 = 0; x_1 < currDims[1]; x_1++)
							// Add move to array list when found.
							if (possMvs[y_1][x_1] != 0)
								gameStateArray.add(baseState.playMove(
									new byte[] {y, x},
									new byte[] {y_1, x_1}));
				}

				// Get a move for all captures in that empty space.
				else if (board[y][x][1] == 0)
				{
					for (byte i = 0; i < captures.size(); i++)
						gameStateArray.add(baseState.placeCapture(
									i, new byte[] {y, x}));
				}
			}
		}

		// Return array list of all moves
		return gameStateArray;
	}

	/**
	* Getter for best state to choose from according to minmax algorithm.
	* @param baseState the base game state.
	* @return the chosen game state.
	 */
	public GameState chooseBestState (GameState baseState)
	{
		// Initialize children game states
		side = baseState.getTurnByte();
		ArrayList<GameState> childrenStates = getAllGameStates(baseState);
		if (childrenStates == null)
			StartShogi.println("Uh Oh");
		GameState retState = null;
		int maxVal = MIN_ALPHA;

		// Loop through all child states to get highest value.
		for (GameState childState : childrenStates)
		{
			int newVal = alphabeta(childState, MIN_ALPHA, MAX_BETA, baseLayer, true);
			// If current value higher than first value, switch to maximal game state.
			if (newVal > maxVal)
			{
				retState = childState;
				maxVal = newVal;
			}
		}

		// Return final game state.
		return retState;
	}

	/**
	* Get a quantification of how good this game state for the current player
	* @param rewardState the game state to link to.
	* @return the quantification as an integer.
	 */
	public int getReward(GameState rewardState)
	{
		int reward = 0;

		// Loop add to reward each piece's value
		byte[][][] board = rewardState.boardArray;
		for (int y = 0; y < board.length; y++)
			for (int x = 0; x < board[0].length; x++)
				// Current case is a piece bot owns
				if (board[y][x][1] == side) 
					reward += shogiRules.getValue(board[y][x][0]);

		// Loop reward for each capture
		ArrayList<Byte> captures = rewardState.capturesP1;
		if (rewardState.P0Turn) captures = rewardState.capturesP1;
		for (Byte piece: captures)
			reward += shogiRules.getValue(piece);

		// Return the final reward.
		return reward;
	}

	/**
	* Getter for minmax value of a state.
	* @param nodeState the game state to develop and give a value to.
	* @param alpha the lowest score the maximizing player
	* @param beta the highest score of the minimizing player
	* @param layer the depth layer of the algorithm.
	* @param maxPlayer whether current player is maximizing player or not.
	* @return the value of the algorithm.
	 */
	public int alphabeta(GameState nodeState, int alpha, int beta, int layer, boolean maxPlayer)
	{
		// If layer is 0, return the reward.
		if (layer == 0)
			return getReward(nodeState);
		// Perform the pruning.
		else
		{
			// Initialize return variable.
			int value = 0;

			// Get all child game states.
			ArrayList<GameState> childrenStates = getAllGameStates(nodeState);

			// Player wants to maximise value - cut off at alpha >= beta.
			if (maxPlayer)
			{
				value = MIN_ALPHA; // set value at lowest possible reward
				for (GameState childState : childrenStates)
				{
					value = Math.max(value, alphabeta(childState, alpha, beta,
								layer - 1, !maxPlayer));
					alpha = Math.max(alpha, value);
					if (alpha >= beta) break;
				}
			}
			// Enemy wants to minimize bot's value - cut off at alpha >= beta
			else
			{
				value = MAX_BETA; // Set value at unreachably high reward.
				for (GameState childState : childrenStates)
				{
					value = Math.min(value, alphabeta(childState, alpha, beta,
								layer - 1, !maxPlayer));
					beta = Math.min(beta, value);

					if (alpha >= beta) break;
				}
			}

			// Return value
			return value;
		}
	}

	/**
	* Constructor for shogi boat.
	* @param mainHandler the main logic handler.
	 */
	public ShogiBot (LogicHandler mainHandler)
	{
		this.mainHandler = mainHandler;
		this.shogiRules = LogicHandler.shogiRules;
	}


}
