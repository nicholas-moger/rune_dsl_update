package chaos.s28.base.reports;

import chaos.s28.base.C28Trade;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C28EligibleRule.C28EligibleRuleDefault.class)
public abstract class C28EligibleRule implements ReportFunction<C28Trade, Boolean> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public Boolean evaluate(C28Trade input) {
		Boolean output = doEvaluate(input);
		
		return output;
	}

	protected abstract Boolean doEvaluate(C28Trade input);

	public static class C28EligibleRuleDefault extends C28EligibleRule {
		@Override
		protected Boolean doEvaluate(C28Trade input) {
			Boolean output = null;
			return assignOutput(output, input);
		}
		
		protected Boolean assignOutput(Boolean output, C28Trade input) {
			output = exists(MapperS.of(input).<String>map("getUtid", c28Trade -> c28Trade.getUtid())).get();
			
			return output;
		}
	}
}
