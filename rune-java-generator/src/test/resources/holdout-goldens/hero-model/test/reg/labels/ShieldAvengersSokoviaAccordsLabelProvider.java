package test.reg.labels;

import com.regnosys.rosetta.lib.labelprovider.GraphBasedLabelProvider;
import com.regnosys.rosetta.lib.labelprovider.LabelNode;
import java.util.Arrays;


public class ShieldAvengersSokoviaAccordsLabelProvider extends GraphBasedLabelProvider {
	public ShieldAvengersSokoviaAccordsLabelProvider() {
		super(new LabelNode());
		
		startNode.addLabel(Arrays.asList("heroName"), "Hero Name");
		startNode.addLabel(Arrays.asList("dateOfBirth"), "Date of Birth");
		startNode.addLabel(Arrays.asList("nationality"), "Nationality");
		startNode.addLabel(Arrays.asList("hasSpecialAbilities"), "Has Special Abilities");
		startNode.addLabel(Arrays.asList("powers"), "Powers");
		startNode.addLabel(Arrays.asList("organisations"), "Hero Organisations");
		startNode.addLabel(Arrays.asList("notModelled"), "Not Modelled");
		
		LabelNode attributeReportNode = new LabelNode();
		attributeReportNode.addLabel(Arrays.asList("heroInt"), "Attribute - Int");
		attributeReportNode.addLabel(Arrays.asList("heroNumber"), "Attribute - Number");
		attributeReportNode.addLabel(Arrays.asList("heroTime"), "Attribute - Time");
		attributeReportNode.addLabel(Arrays.asList("heroZonedDateTime"), "Attribute - ZonedDateTime");
		
		startNode.addOutgoingEdge("attribute", attributeReportNode);
	}
}
