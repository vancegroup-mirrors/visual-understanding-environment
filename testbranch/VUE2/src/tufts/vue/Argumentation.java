package tufts.vue;

import java.util.*;

public class Argumentation {

	private HashMap<String,String> closedList = new HashMap<String,String>();
	private LWComponent node;
	
	public Argumentation(LWComponent node) {
		// ActiveInstance.addAllActiveListener(this);
		this.node = node;
	}

	// TODO This function should be triggered by an event which occurs each time
	// an answer node is created/modified or any of its descendants are
	// modified, the event should pass the reference to the modified answer node
	// as well

	// Stage1: Check that the LWComponent is a LWIBISNode
	public void computeScores() {
		if (node.getClass().equals(tufts.vue.LWIBISNode.class)) {
			computeScores1((LWIBISNode) node);
		} else
			System.out.println("IBISARG ERROR: NOT A IBIS NODE!");
	}

	// Stage2: Check that the LWIBISNode is of type 'Answer'
	private void computeScores1(LWIBISNode node) {
		node.determineNodeImageAndType();
		if (node.getIBISType().startsWith("answer")) {
			computeScores2(node);
		} else
			System.out.println("IBISARG ERROR: NOT AN IBIS ANSWER NODE!");
	}

	// Stage3: Do postorder DFS from node.
	// WORKAROUND: It seems getHead returns the tail and vice versa
	private void computeScores2(LWIBISNode node) {
		System.out.println("IBISARG: Current node is:" + node.getLabel());
		// Recursive step: for all directly connected nodes
		// descend (recalling yourself) skipping non ibis nodes
		for (LWLink l : node.getIncomingLinks()) {
			// System.out.println("IBISARG: Iterating on incoming link " + l.getLabel());
			if (l.getHead().getClass().equals(tufts.vue.LWIBISNode.class)) {
				((LWIBISNode) l.getHead()).determineNodeImageAndType();
				// Checking if visiting the same node twice
				if(closedList.containsKey(l.getHead().getID())) {
					System.out.println("IBISARG: Skipping EXPANDED node:" + l.getHead().getLabel());
				} else {
					// System.out.println("IBISARG: Recursively entering node:" + l.getHead().getLabel());
					computeScores2((LWIBISNode) l.getHead());
				}
			} else
				System.out.println("IBISARG: Skipping non IBIS node:" + l.getHead().getLabel());
		}

		// Node has been expanded, let's put it into the closed list
		closedList.put(node.getID(),null);
		
		// If we're in a leaf we set score to baseScore, else we expand the node and compute direct attacks & supports
		if (node.getIncomingLinks().isEmpty()) {
			node.setScore(node.getBaseScore());
			System.out.println("IBISARG: Leaf node " + node.getLabel() + " score = " + node.getBaseScore());
		} else {
			System.out.print("IBISARG: Expanding node " + node.getLabel() + ":");
			double att = node.getBaseScore();
			double supp = node.getBaseScore();
			for (LWLink l : node.getIncomingLinks()) {
				// Attacks
				if (l.getHead().getClass().equals(tufts.vue.LWIBISNode.class) && ((LWIBISNode) l.getHead()).getIBISType().startsWith("con_argument")) {
					att = att - (att * ((LWIBISNode) l.getHead()).getScore());
					System.out.print(" att=" + att);
				}
				// Supports
				else if (l.getHead().getClass().equals(tufts.vue.LWIBISNode.class) && ((LWIBISNode) l.getHead()).getIBISType().startsWith("pro_argument")) {
					supp = supp + ((1 - supp) * ((LWIBISNode) l.getHead()).getScore());
					System.out.print(" supp=" + supp);
				}
			}
			// Average & set score
			if (att == node.getBaseScore()) node.setScore(supp);
			else if (supp == node.getBaseScore()) node.setScore(att);
			else node.setScore((att + supp) / 2);
			System.out.print(" Score=" + node.getScore());
			System.out.println("");
		}
	}

}
