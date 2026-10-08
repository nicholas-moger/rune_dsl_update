package test.rrec001.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import javax.inject.Inject;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(FacRule.FacRuleDefault.class)
public abstract class FacRule implements ReportFunction<Integer, Integer> {
	
	// RosettaFunction dependencies
	//
	@Inject protected FacRule facRule;

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public Integer evaluate(Integer input) {
		Integer output = doEvaluate(input);
		
		return output;
	}

	protected abstract Integer doEvaluate(Integer input);

	public static class FacRuleDefault extends FacRule {
		@Override
		protected Integer doEvaluate(Integer input) {
			Integer output = null;
			return assignOutput(output, input);
		}
		
		protected Integer assignOutput(Integer output, Integer input) {
			if (areEqual(MapperS.of(input), MapperS.of(1), CardinalityOperator.All).getOrDefault(false)) {
				output = 1;
			} else {
				output = MapperMaths.<Integer, Integer, Integer>multiply(MapperS.of(input), MapperS.of(facRule.evaluate(MapperMaths.<Integer, Integer, Integer>subtract(MapperS.of(input), MapperS.of(1)).get()))).get();
			}
			
			return output;
		}
	}
}
