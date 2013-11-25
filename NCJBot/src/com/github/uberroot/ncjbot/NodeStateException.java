package com.github.uberroot.ncjbot;

/**
 * An exception that is thrown when a requested action on a node is unavailable due to its state.
 * 
 * @author Carter Waxman
 *
 */
public class NodeStateException extends NCJBotException {
	private NodeState state;
	
	public NodeStateException(NodeState state) {
		super("The node is in an invalid state for the operation: " + state);
		this.state = state;
	}
	
	public NodeState getState(){
		return state;
	};

}
