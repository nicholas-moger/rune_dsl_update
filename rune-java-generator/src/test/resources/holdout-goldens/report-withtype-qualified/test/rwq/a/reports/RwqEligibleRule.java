package test.rwq.a.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import java.math.BigDecimal;
import test.rwq.a.RwqTrade;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(RwqEligibleRule.RwqEligibleRuleDefault.class)
public abstract class RwqEligibleRule implements ReportFunction<RwqTrade, Boolean> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public Boolean evaluate(RwqTrade input) {
		Boolean output = doEvaluate(input);
		
		return output;
	}

	protected abstract Boolean doEvaluate(RwqTrade input);

	public static class RwqEligibleRuleDefault extends RwqEligibleRule {
		@Override
		protected Boolean doEvaluate(RwqTrade input) {
			Boolean output = null;
			return assignOutput(output, input);
		}
		
		protected Boolean assignOutput(Boolean output, RwqTrade input) {
			output = exists(MapperS.of(input).<BigDecimal>map("getNotional", rwqTrade -> rwqTrade.getNotional())).get();
			
			return output;
		}
	}
}
