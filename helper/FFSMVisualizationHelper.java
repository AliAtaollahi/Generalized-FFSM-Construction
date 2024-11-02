package uk.le.ac.helper;

import java.io.IOException;
import java.util.Map;

import org.prop4j.Node;

import net.automatalib.automata.MutableAutomaton;
import net.automatalib.serialization.dot.DOTVisualizationHelper;
import uk.le.ac.ffsm.ConditionalState;
import uk.le.ac.ffsm.ConditionalTransition;
import uk.le.ac.ffsm.FeaturedMealyUtils;

public class FFSMVisualizationHelper<I,O> implements DOTVisualizationHelper<ConditionalState<ConditionalTransition<I, O>>, ConditionalTransition<I, O>>{

	private boolean plotSelfloops;

	public FFSMVisualizationHelper(
			MutableAutomaton<ConditionalState<ConditionalTransition<I, O>>, I, ConditionalTransition<I, O>, Node, O> automaton) {
		 this.plotSelfloops = true;
	}
	
	
	public void setPlotSelfloops(boolean plotSelfloops) {
		this.plotSelfloops = plotSelfloops;
	}
	
	
	@Override
	public boolean getNodeProperties(ConditionalState<ConditionalTransition<I, O>> node,
			Map<String, String> properties) {
		final StringBuilder labelBuilder = new StringBuilder();
		labelBuilder.append(String.valueOf(node.getId()));
		labelBuilder.append("@");
		labelBuilder.append("[");
		labelBuilder.append(((node.getCondition()==null)?"null":FeaturedMealyUtils.getInstance().nodeWriter(node.getCondition())));
		labelBuilder.append("]");
		properties.put(NodeAttrs.LABEL, labelBuilder.toString());
		return true;
	}


	@Override
	public boolean getEdgeProperties(ConditionalState<ConditionalTransition<I, O>> src,
			ConditionalTransition<I, O> edge, ConditionalState<ConditionalTransition<I, O>> tgt,
			Map<String, String> properties) {
		if(!this.plotSelfloops && src.equals(tgt)) {
			return false;
		}

		final StringBuilder labelBuilder = new StringBuilder();
		labelBuilder.append(String.valueOf(edge.getInput()));
		labelBuilder.append("@");
		labelBuilder.append("[");
		labelBuilder.append(((edge.getCondition()==null)?"TRUE":FeaturedMealyUtils.getInstance().nodeWriter(edge.getCondition())));
		labelBuilder.append("]");
		labelBuilder.append("/");
		O output = edge.getOutput();
		if (output != null) {
			labelBuilder.append(String.valueOf(output));
		}
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
