package test.deeppath.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperListOfLists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import test.deeppath.Outer;
import test.deeppath.util.OuterDeepPathUtil;


@ImplementedBy(MultiFeature.MultiFeatureDefault.class)
public abstract class MultiFeature implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected OuterDeepPathUtil outerDeepPathUtil;

	/**
	* @param outers 
	* @return tags 
	*/
	public List<String> evaluate(List<? extends Outer> outers) {
		List<String> tags = doEvaluate(outers);
		
		return tags;
	}

	protected abstract List<String> doEvaluate(List<? extends Outer> outers);

	public static class MultiFeatureDefault extends MultiFeature {
		@Override
		protected List<String> doEvaluate(List<? extends Outer> outers) {
			if (outers == null) {
				outers = Collections.emptyList();
			}
			List<String> tags = new ArrayList<>();
			return assignOutput(tags, outers);
		}
		
		protected List<String> assignOutput(List<String> tags, List<? extends Outer> outers) {
			final MapperListOfLists<String> thenArg = MapperC.<Outer>of(outers)
				.mapItemToList(item -> item.<String>mapC("chooseTags", outer -> outerDeepPathUtil.chooseTags(outer)));
			tags.addAll(thenArg
				.flattenList().getMulti());
			
			return tags;
		}
	}
}
