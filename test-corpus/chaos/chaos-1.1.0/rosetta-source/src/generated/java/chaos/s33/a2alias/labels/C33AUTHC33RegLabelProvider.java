package chaos.s33.a2alias.labels;

import com.regnosys.rosetta.lib.labelprovider.GraphBasedLabelProvider;
import com.regnosys.rosetta.lib.labelprovider.LabelNode;
import java.util.Arrays;


public class C33AUTHC33RegLabelProvider extends GraphBasedLabelProvider {
	public C33AUTHC33RegLabelProvider() {
		super(new LabelNode());
		
		startNode.addLabel(Arrays.asList("utiField"), "UTI \u00AB\u00FC\u00BB \"q\"");
		startNode.addLabel(Arrays.asList("litField"), "Literal");
	}
}
