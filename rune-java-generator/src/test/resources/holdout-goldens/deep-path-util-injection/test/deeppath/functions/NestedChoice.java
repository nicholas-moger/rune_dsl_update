package test.deeppath.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import test.deeppath.Inner;
import test.deeppath.util.InnerDeepPathUtil;


@ImplementedBy(NestedChoice.NestedChoiceDefault.class)
public abstract class NestedChoice implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected InnerDeepPathUtil innerDeepPathUtil;

	/**
	* @param inners 
	* @return commons 
	*/
	public List<String> evaluate(List<? extends Inner> inners) {
		List<String> commons = doEvaluate(inners);
		
		return commons;
	}

	protected abstract List<String> doEvaluate(List<? extends Inner> inners);

	public static class NestedChoiceDefault extends NestedChoice {
		@Override
		protected List<String> doEvaluate(List<? extends Inner> inners) {
			if (inners == null) {
				inners = Collections.emptyList();
			}
			List<String> commons = new ArrayList<>();
			return assignOutput(commons, inners);
		}
		
		protected List<String> assignOutput(List<String> commons, List<? extends Inner> inners) {
			commons.addAll(MapperC.<Inner>of(inners)
				.mapItem(item -> item.<String>map("chooseCommon", inner -> innerDeepPathUtil.chooseCommon(inner))).getMulti());
			
			return commons;
		}
	}
}
