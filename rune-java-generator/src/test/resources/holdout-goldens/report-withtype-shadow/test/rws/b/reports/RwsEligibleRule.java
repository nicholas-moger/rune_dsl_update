package test.rws.b.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import java.math.BigDecimal;
import test.rws.b.RwsTrade;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(RwsEligibleRule.RwsEligibleRuleDefault.class)
public abstract class RwsEligibleRule implements ReportFunction<RwsTrade, Boolean> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public Boolean evaluate(RwsTrade input) {
		Boolean output = doEvaluate(input);
		
		return output;
	}

	protected abstract Boolean doEvaluate(RwsTrade input);

	public static class RwsEligibleRuleDefault extends RwsEligibleRule {
		@Override
		protected Boolean doEvaluate(RwsTrade input) {
			Boolean output = null;
			return assignOutput(output, input);
		}
		
		protected Boolean assignOutput(Boolean output, RwsTrade input) {
			output = exists(MapperS.of(input).<BigDecimal>map("getNotional", rwsTrade -> rwsTrade.getNotional())).get();
			
			return output;
		}
	}
}
