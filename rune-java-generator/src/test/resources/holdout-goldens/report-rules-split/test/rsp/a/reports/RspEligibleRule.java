package test.rsp.a.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import java.math.BigDecimal;
import test.rsp.a.RspTrade;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(RspEligibleRule.RspEligibleRuleDefault.class)
public abstract class RspEligibleRule implements ReportFunction<RspTrade, Boolean> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public Boolean evaluate(RspTrade input) {
		Boolean output = doEvaluate(input);
		
		return output;
	}

	protected abstract Boolean doEvaluate(RspTrade input);

	public static class RspEligibleRuleDefault extends RspEligibleRule {
		@Override
		protected Boolean doEvaluate(RspTrade input) {
			Boolean output = null;
			return assignOutput(output, input);
		}
		
		protected Boolean assignOutput(Boolean output, RspTrade input) {
			output = exists(MapperS.of(input).<BigDecimal>map("getNotional", rspTrade -> rspTrade.getNotional())).get();
			
			return output;
		}
	}
}
