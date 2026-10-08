package chaos.s07.a3half.p2.reports;

import chaos.s07.a3half.p1.C7Trade;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import java.math.BigDecimal;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C7EligibleRule.C7EligibleRuleDefault.class)
public abstract class C7EligibleRule implements ReportFunction<C7Trade, Boolean> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public Boolean evaluate(C7Trade input) {
		Boolean output = doEvaluate(input);
		
		return output;
	}

	protected abstract Boolean doEvaluate(C7Trade input);

	public static class C7EligibleRuleDefault extends C7EligibleRule {
		@Override
		protected Boolean doEvaluate(C7Trade input) {
			Boolean output = null;
			return assignOutput(output, input);
		}
		
		protected Boolean assignOutput(Boolean output, C7Trade input) {
			output = exists(MapperS.of(input).<BigDecimal>map("getNotional", c7Trade -> c7Trade.getNotional())).get();
			
			return output;
		}
	}
}
