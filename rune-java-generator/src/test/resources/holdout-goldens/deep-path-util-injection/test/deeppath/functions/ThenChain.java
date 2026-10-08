package test.deeppath.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import test.deeppath.Outer;
import test.deeppath.util.OuterDeepPathUtil;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(ThenChain.ThenChainDefault.class)
public abstract class ThenChain implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected OuterDeepPathUtil outerDeepPathUtil;

	/**
	* @param outers 
	* @return texts 
	*/
	public List<String> evaluate(List<? extends Outer> outers) {
		List<String> texts = doEvaluate(outers);
		
		return texts;
	}

	protected abstract List<String> doEvaluate(List<? extends Outer> outers);

	public static class ThenChainDefault extends ThenChain {
		@Override
		protected List<String> doEvaluate(List<? extends Outer> outers) {
			if (outers == null) {
				outers = Collections.emptyList();
			}
			List<String> texts = new ArrayList<>();
			return assignOutput(texts, outers);
		}
		
		protected List<String> assignOutput(List<String> texts, List<? extends Outer> outers) {
			final MapperC<Outer> thenArg = MapperC.<Outer>of(outers)
				.filterItemNullSafe(item -> exists(item.<String>map("chooseText", outer -> outerDeepPathUtil.chooseText(outer))).get());
			texts.addAll(thenArg
				.mapItem(item -> item.<String>map("chooseText", outer -> outerDeepPathUtil.chooseText(outer))).getMulti());
			
			return texts;
		}
	}
}
