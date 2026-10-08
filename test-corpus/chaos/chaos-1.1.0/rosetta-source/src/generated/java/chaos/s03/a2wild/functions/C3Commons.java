package chaos.s03.a2wild.functions;

import chaos.s03.a2wild.C3Outer;
import chaos.s03.a2wild.util.C3OuterDeepPathUtil;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;


@ImplementedBy(C3Commons.C3CommonsDefault.class)
public abstract class C3Commons implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected C3OuterDeepPathUtil c3OuterDeepPathUtil;

	/**
	* @param outers 
	* @return texts 
	*/
	public List<String> evaluate(List<? extends C3Outer> outers) {
		List<String> texts = doEvaluate(outers);
		
		return texts;
	}

	protected abstract List<String> doEvaluate(List<? extends C3Outer> outers);

	public static class C3CommonsDefault extends C3Commons {
		@Override
		protected List<String> doEvaluate(List<? extends C3Outer> outers) {
			if (outers == null) {
				outers = Collections.emptyList();
			}
			List<String> texts = new ArrayList<>();
			return assignOutput(texts, outers);
		}
		
		protected List<String> assignOutput(List<String> texts, List<? extends C3Outer> outers) {
			texts.addAll(MapperC.<C3Outer>of(outers)
				.mapItem(item -> item.<String>map("chooseText", c3Outer -> c3OuterDeepPathUtil.chooseText(c3Outer))).getMulti());
			
			return texts;
		}
	}
}
