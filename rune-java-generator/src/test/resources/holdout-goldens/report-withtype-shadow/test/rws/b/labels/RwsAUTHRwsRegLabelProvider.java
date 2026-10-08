package test.rws.b.labels;

import com.regnosys.rosetta.lib.labelprovider.GraphBasedLabelProvider;
import com.regnosys.rosetta.lib.labelprovider.LabelNode;
import java.util.Arrays;


public class RwsAUTHRwsRegLabelProvider extends GraphBasedLabelProvider {
	public RwsAUTHRwsRegLabelProvider() {
		super(new LabelNode());
		
		startNode.addLabel(Arrays.asList("utiField"), "UTI");
		startNode.addLabel(Arrays.asList("notionalField"), "Notional");
	}
}
