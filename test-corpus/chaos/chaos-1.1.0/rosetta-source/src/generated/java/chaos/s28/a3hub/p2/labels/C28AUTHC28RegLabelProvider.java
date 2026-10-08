package chaos.s28.a3hub.p2.labels;

import com.regnosys.rosetta.lib.labelprovider.GraphBasedLabelProvider;
import com.regnosys.rosetta.lib.labelprovider.LabelNode;
import java.util.Arrays;


public class C28AUTHC28RegLabelProvider extends GraphBasedLabelProvider {
	public C28AUTHC28RegLabelProvider() {
		super(new LabelNode());
		
		startNode.addLabel(Arrays.asList("utiField"), "UTI");
		startNode.addLabel(Arrays.asList("avField"), "Option A Value");
		startNode.addLabel(Arrays.asList("venueField"), "Venue");
	}
}
