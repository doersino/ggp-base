package seggp.uni.tueb.player.minimax;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

/**
 * Gamer using an implementation of the minimax algorithm to determine which
 * move is likely to result in a positive result.
 *
 * First builds the tree as deep as possible without timing out, then assigns
 * a minimax score to each node, and finally returns the move with the highest
 * score.
 *
 * TODO reuse tree for following move
 * TODO optimize, e.g. by using different tree/queue implementations
 *
 * @author noah
 */
public final class MinimaxGamer extends SampleGamer
{
	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		long start = System.currentTimeMillis();

		Node tree = buildTreeBreadthFirst(getCurrentState(), timeout - 1000); // after building the tree, some time is needed for the minimax algorithm
		Move move = getBestMove(tree);

		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		long stop = System.currentTimeMillis();
		notifyObservers(new GamerSelectedMoveEvent(moves, move, stop - start));

		return move;
	}

	/**
	 * Breadth-first tree generation, using a queue to store nodes that need to
	 * be visited later on.
	 *
	 * Unlike a simpler depth-first implementation, this
	 * allows us to stop building the tree at any time while still getting
	 * useful results. Note that some games, e.g. Tic-Tac-Toe, don't contain
	 * any loops, rendering complete tree generation possible, however aborting
	 * the tree generation before the timeout _will_ deliver better results than
	 * you can expect the Player to randomly choose when a *Gamer times out.
	 *
	 * If timeout is -1, the tree generation will be running indefinitely.
	 *
	 * @param initialState
	 * @param timeout
	 * @return root node
	 * @throws TransitionDefinitionException
	 * @throws MoveDefinitionException
	 * @throws GoalDefinitionException
	 */
	private Node buildTreeBreadthFirst(MachineState initialState, long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		Node root = new Node(initialState);
		Node node;
		Node child;
		LinkedList<Node> queue = new LinkedList<Node>(); // can be any other queue implementation that provides add() and remove()
		Map<Move, List<MachineState>> nextStates;

		queue.add(root);
		while (queue.size() > 0 && (System.currentTimeMillis() < timeout || timeout == -1)) {
			node = queue.remove();
			nextStates = getStateMachine().getNextStates(node.getState(), getRole());

			for (Move move : nextStates.keySet()) {
				for (MachineState state : nextStates.get(move)) {
					// don't create subtree for terminal states, i.e. don't add to the queue
					if (getStateMachine().isTerminal(state)) {
						child = new Node(move, state, getStateMachine().getGoal(state, getRole()));
					} else {
						child = new Node(move, state);
						queue.add(child);
					}
					node.addChild(child);
				}
			}
		}

		return root;
	}

	/**
	 * Depth-first tree generation, limited by a maximum tree depth.
	 *
	 * If depth is negative, the tree generation will be running indefinitely.
	 *
	 * @param state
	 * @param depth
	 * @return root node
	 * @throws TransitionDefinitionException
	 * @throws MoveDefinitionException
	 * @throws GoalDefinitionException
	 */
	private Node buildTree(MachineState state, int depth) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		Node root = new Node(state);
		buildSubtree(root, depth);

		return root;
	}

	/**
	 * Recursive function for building subtrees depth-first, limited by a
	 * maximum tree depth.
	 *
	 * If depth is negative, the tree generation will be running indefinitely.
	 *
	 * @param parent
	 * @param depth
	 * @throws TransitionDefinitionException
	 * @throws MoveDefinitionException
	 * @throws GoalDefinitionException
	 */
	private void buildSubtree(Node parent, int depth) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		if (depth == 0) return; // TODO while we're here, we could assign a score based on very basic heuristics that work for all games

		Map<Move, List<MachineState>> nextStates = getStateMachine().getNextStates(parent.getState(), getRole());

		Node node;
		for (Move move : nextStates.keySet()) {
			for (MachineState state : nextStates.get(move)) {
				// don't create subtree for terminal states
				if (getStateMachine().isTerminal(state)) {
					node = new Node(move, state, getStateMachine().getGoal(state, getRole())); // TODO opportunity to save opponent's score as well
				} else {
					node = new Node(move, state);
					buildSubtree(node, depth-1);
				}
				parent.addChild(node);
			}
		}
	}

	/**
	 * Implementation of the minimax algorithm. Differs from maxScore in that it
	 * returns the Move with the highest score as opposed to the highest score.
	 *
	 * @param tree
	 * @return best move as determined by the minimax algorithm operating on
	 *         the supplied tree
	 */
	private Move getBestMove(Node tree) {
		// calculate minimax score for each child of the root node
		// this can't just be a call of getMaxScore because we need the best child's index as well
		Node[] children = new Node[tree.getChildCount()];
		int score = 0;
		int index = 0;
		for (int i = 0; i < children.length; ++i) {
			children[i] = tree.getChild(i);
			children[i].setScore(minScore(children[i]));

			if (children[i].getScore() > score) {
				score = children[i].getScore();
				index = i;
			}
		}
		return children[index].getMove();
	}

	/**
	 * Returns the score of the child with the lowest score, recursively calls
	 * maxScore()
	 *
	 * @param node
	 * @return minimum child score
	 */
	private int minScore(Node node) {
		if (node.getChildCount() == 0)
			// returns -1 for nonterminal leaves (standard score in StateObject)
			return node.getScore();
		else {
			Node child;
			int score = 100; // might lead to problems if all children are nonterminal leaves and we ignore their scores
			for (int i = 0; i < node.getChildCount(); ++i) {
				child = node.getChild(i);
				child.setScore(maxScore(node.getChild(i)));
				if (child.getScore() < score)
					score = child.getScore();
			}

			return score;
		}
	}

	/**
	 * Returns the score of the child with the highest score, recursively calls
	 * minScore().
	 *
	 * @param node
	 * @return maximum child score
	 */
	private int maxScore(Node node) {
		if (node.getChildCount() == 0)
			// returns -1 for nonterminal leaves (standard score in StateObject)
			return node.getScore();
		else {
			Node child;
			int score = 0; // might lead to problems if all children are nonterminal leaves and we ignore their scores
			for (int i = 0; i < node.getChildCount(); ++i) {
				child = node.getChild(i);
				child.setScore(minScore(node.getChild(i)));
				if (child.getScore() > score)
					score = child.getScore();
			}

			return score;
		}
	}
}
