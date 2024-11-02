package uk.le.ac.helper;

import java.io.IOException;
import java.util.Map;

import net.automatalib.automata.fsa.impl.FastNFAState;
import net.automatalib.automata.graphs.TransitionEdge;
import net.automatalib.serialization.dot.DOTVisualizationHelper;

public class NFAVisualizationHelper implements DOTVisualizationHelper<FastNFAState, TransitionEdge> {

	@Override
	public boolean getNodeProperties(FastNFAState node, Map<String, String> properties) {
		final StringBuilder labelBuilder = new StringBuilder();
		labelBuilder.append(node.getId());
		properties.put(NodeAttrs.LABEL, labelBuilder.toString());
		return true;
	}

	@Override
	public boolean getEdgeProperties(FastNFAState src, TransitionEdge edge, FastNFAState tgt,
			Map<String, String> properties) {

		final StringBuilder labelBuilder = new StringBuilder();
		labelBuilder.append(edge.getInput());
		properties.put(EdgeAttrs.LABEL, labelBuilder.toString());
		return true;
	}

	@Override
	public void writePreamble(Appendable a) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writePostamble(Appendable a) throws IOException {
		// TODO Auto-generated method stub
		
	}
}