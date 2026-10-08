package chaos.s33.a3half.p2.reports;

import chaos.s33.a3half.p1.C33Trade;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import java.math.BigDecimal;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C33EligRule.C33EligRuleDefault.class)
public abstract class C33EligRule implements ReportFunction<C33Trade, Boolean> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public Boolean evaluate(C33Trade input) {
		Boolean output = doEvaluate(input);
		
		return output;
	}

	protected abstract Boolean doEvaluate(C33Trade input);

	public static class C33EligRuleDefault extends C33EligRule {
		@Override
		protected Boolean doEvaluate(C33Trade input) {
			Boolean output = null;
			return assignOutput(output, input);
		}
		
		protected Boolean assignOutput(Boolean output, C33Trade input) {
			output = exists(MapperS.of(input).<BigDecimal>map("getNotional", c33Trade -> c33Trade.getNotional())).get();
			
			return output;
		}
	}
}
