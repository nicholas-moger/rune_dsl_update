package test.deeppathedge.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.functions.ConditionValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import test.deeppathedge.Outer;
import test.deeppathedge.util.OuterDeepPathUtil;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(InCondition.InConditionDefault.class)
public abstract class InCondition implements RosettaFunction {
	
	@Inject protected ConditionValidator conditionValidator;
	
	// RosettaFunction dependencies
	//
	@Inject protected OuterDeepPathUtil outerDeepPathUtil;

	/**
	* @param outers 
	* @return n 
	*/
	public Integer evaluate(List<? extends Outer> outers) {
		// pre-conditions
		conditionValidator.validate(() -> {
			final MapperC<String> thenArg = MapperC.<Outer>of(outers)
				.mapItem(item -> item.<String>map("chooseText", outer -> outerDeepPathUtil.chooseText(outer)));
			return ComparisonResult.ofNullSafe(exists(thenArg).asMapper());
		},
			"");
		
		Integer n = doEvaluate(outers);
		
		return n;
	}

	protected abstract Integer doEvaluate(List<? extends Outer> outers);

	public static class InConditionDefault extends InCondition {
		@Override
		protected Integer doEvaluate(List<? extends Outer> outers) {
			if (outers == null) {
				outers = Collections.emptyList();
			}
			Integer n = null;
			return assignOutput(n, outers);
		}
		
		protected Integer assignOutput(Integer n, List<? extends Outer> outers) {
			n = MapperC.<Outer>of(outers).resultCount();
			
			return n;
		}
	}
}
