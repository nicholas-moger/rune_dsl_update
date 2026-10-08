package test.rsh.a.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import java.math.BigDecimal;
import test.rsh.a.RshTrade;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(RshEligibleRule.RshEligibleRuleDefault.class)
public abstract class RshEligibleRule implements ReportFunction<RshTrade, Boolean> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public Boolean evaluate(RshTrade input) {
		Boolean output = doEvaluate(input);
		
		return output;
	}

	protected abstract Boolean doEvaluate(RshTrade input);

	public static class RshEligibleRuleDefault extends RshEligibleRule {
		@Override
		protected Boolean doEvaluate(RshTrade input) {
			Boolean output = null;
			return assignOutput(output, input);
		}
		
		protected Boolean assignOutput(Boolean output, RshTrade input) {
			output = exists(MapperS.of(input).<BigDecimal>map("getNotional", rshTrade -> rshTrade.getNotional())).get();
			
			return output;
		}
	}
}
