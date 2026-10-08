package test.rsr.b.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import java.math.BigDecimal;
import test.rsr.b.RsrTrade;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(RsrEligibleRule.RsrEligibleRuleDefault.class)
public abstract class RsrEligibleRule implements ReportFunction<RsrTrade, Boolean> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public Boolean evaluate(RsrTrade input) {
		Boolean output = doEvaluate(input);
		
		return output;
	}

	protected abstract Boolean doEvaluate(RsrTrade input);

	public static class RsrEligibleRuleDefault extends RsrEligibleRule {
		@Override
		protected Boolean doEvaluate(RsrTrade input) {
			Boolean output = null;
			return assignOutput(output, input);
		}
		
		protected Boolean assignOutput(Boolean output, RsrTrade input) {
			output = exists(MapperS.of(input).<BigDecimal>map("getNotional", rsrTrade -> rsrTrade.getNotional())).get();
			
			return output;
		}
	}
}
