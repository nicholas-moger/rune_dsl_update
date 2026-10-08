package chaos.s28.a1o3.labels;

import com.regnosys.rosetta.lib.labelprovider.GraphBasedLabelProvider;
import com.regnosys.rosetta.lib.labelprovider.LabelNode;
import java.util.Arrays;


public class C28ALTC28RegLabelProvider extends GraphBasedLabelProvider {
	public C28ALTC28RegLabelProvider() {
		super(new LabelNode());
		
		
		LabelNode c28ChoiceReportNode = new LabelNode();
		c28ChoiceReportNode.addLabel(Arrays.asList("utiField"), "UTI");
		
		LabelNode c28ReportNode = new LabelNode();
		c28ReportNode.addLabel(Arrays.asList("utiField"), "UTI");
		c28ReportNode.addLabel(Arrays.asList("avField"), "Option A Value");
		c28ReportNode.addLabel(Arrays.asList("venueField"), "Venue");
		
		startNode.addOutgoingEdge("C28ChoiceReport", c28ChoiceReportNode);
		startNode.addOutgoingEdge("C28Report", c28ReportNode);
	}
}
