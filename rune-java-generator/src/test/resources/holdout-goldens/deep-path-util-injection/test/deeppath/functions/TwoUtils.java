package test.deeppath.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import test.deeppath.Inner;
import test.deeppath.Outer;
import test.deeppath.util.InnerDeepPathUtil;
import test.deeppath.util.OuterDeepPathUtil;


@ImplementedBy(TwoUtils.TwoUtilsDefault.class)
public abstract class TwoUtils implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected InnerDeepPathUtil innerDeepPathUtil;
	@Inject protected OuterDeepPathUtil outerDeepPathUtil;

	/**
	* @param outers 
	* @param inners 
	* @return texts 
	*/
	public List<String> evaluate(List<? extends Outer> outers, List<? extends Inner> inners) {
		List<String> texts = doEvaluate(outers, inners);
		
		return texts;
	}

	protected abstract List<String> doEvaluate(List<? extends Outer> outers, List<? extends Inner> inners);

	public static class TwoUtilsDefault extends TwoUtils {
		@Override
		protected List<String> doEvaluate(List<? extends Outer> outers, List<? extends Inner> inners) {
			if (outers == null) {
				outers = Collections.emptyList();
			}
			if (inners == null) {
				inners = Collections.emptyList();
			}
			List<String> texts = new ArrayList<>();
			return assignOutput(texts, outers, inners);
		}
		
		protected List<String> assignOutput(List<String> texts, List<? extends Outer> outers, List<? extends Inner> inners) {
			texts.addAll(MapperC.<Outer>of(outers)
				.mapItem(item -> item.<String>map("chooseText", outer -> outerDeepPathUtil.chooseText(outer))).getMulti());
			
			texts.addAll(MapperC.<Inner>of(inners)
				.mapItem(item -> item.<String>map("chooseCommon", inner -> innerDeepPathUtil.chooseCommon(inner))).getMulti());
			
			return texts;
		}
	}
}
