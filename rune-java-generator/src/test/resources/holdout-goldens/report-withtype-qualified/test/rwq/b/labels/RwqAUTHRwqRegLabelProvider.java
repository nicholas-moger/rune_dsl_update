package test.rwq.b.labels;

import com.regnosys.rosetta.lib.labelprovider.GraphBasedLabelProvider;
import com.regnosys.rosetta.lib.labelprovider.LabelNode;
import java.util.Arrays;


public class RwqAUTHRwqRegLabelProvider extends GraphBasedLabelProvider {
	public RwqAUTHRwqRegLabelProvider() {
		super(new LabelNode());
		
		startNode.addLabel(Arrays.asList("utiField"), "UTI");
		startNode.addLabel(Arrays.asList("notionalField"), "Notional");
	}
}
