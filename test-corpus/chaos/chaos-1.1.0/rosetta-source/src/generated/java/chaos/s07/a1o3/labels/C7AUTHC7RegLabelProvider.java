package chaos.s07.a1o3.labels;

import com.regnosys.rosetta.lib.labelprovider.GraphBasedLabelProvider;
import com.regnosys.rosetta.lib.labelprovider.LabelNode;
import java.util.Arrays;


public class C7AUTHC7RegLabelProvider extends GraphBasedLabelProvider {
	public C7AUTHC7RegLabelProvider() {
		super(new LabelNode());
		
		startNode.addLabel(Arrays.asList("utiField"), "UTI");
		startNode.addLabel(Arrays.asList("notionalField"), "Notional");
	}
}
