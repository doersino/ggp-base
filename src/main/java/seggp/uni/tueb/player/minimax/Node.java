package seggp.uni.tueb.player.minimax;

import java.util.ArrayList;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;

/**
 * Simple node structure for a minimax tree. Note that there's no need for a
 * Tree class (so far) - just use your root node.
 *
 * @author noah
 */
public class Node {
	private Move move;
	private MachineState state;
	private int score;
	//private Node parent;
	private ArrayList<Node> children = new ArrayList<Node>();

	public Node(MachineState state) {
		this.state = state;
	}

	public Node(Move move, MachineState state) {
		this.move = move;
		this.state = state;
	}

	public Node(Move move, MachineState state, int score) {
		this.move = move;
		this.state = state;
		this.score = score;
	}

	public Move getMove() {
		return move;
	}

	public MachineState getState() {
		return state;
	}

	public int getScore() {
		return score;
	}

	public int getChildCount() {
		return children.size();
	}

	public Node getChild(int index) {
		return children.get(index);
	}

	public void setScore(int score) {
		this.score = score;
	}

	public void addChild(Node node) {
		//node.removeParent();
		children.add(node);
	}

	/**
	 * Recursively prints the (sub-)tree this node is the root of.
	 *
	 * @param indent
	 */
	public void print(int indent) {
		System.out.println("Node [move=" + move + ", state=" + state + ", score=" + score + "]");
		for (Node child : children) {
			for (int i = 0; i < indent; ++i) {
				System.out.print("\t");
			}
			child.print(indent + 1);
		}
	}

	@Override
	public String toString() {
		return "Node [move=" + move + ", state=" + state + ", score=" + score + ", children=" + children + "]";
	}
}
