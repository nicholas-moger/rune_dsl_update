package test.rsr.b.labels;

import com.regnosys.rosetta.lib.labelprovider.GraphBasedLabelProvider;
import com.regnosys.rosetta.lib.labelprovider.LabelNode;
import java.util.Arrays;


public class RsrAUTHRsrRegLabelProvider extends GraphBasedLabelProvider {
	public RsrAUTHRsrRegLabelProvider() {
		super(new LabelNode());
		
		startNode.addLabel(Arrays.asList("utiField"), "UTI");
		startNode.addLabel(Arrays.asList("notionalField"), "Notional");
	}
}
