package test.rsh.a.labels;

import com.regnosys.rosetta.lib.labelprovider.GraphBasedLabelProvider;
import com.regnosys.rosetta.lib.labelprovider.LabelNode;
import java.util.Arrays;


public class RshAUTHRshRegLabelProvider extends GraphBasedLabelProvider {
	public RshAUTHRshRegLabelProvider() {
		super(new LabelNode());
		
		startNode.addLabel(Arrays.asList("utiField"), "UTI");
		startNode.addLabel(Arrays.asList("notionalField"), "Notional");
	}
}
