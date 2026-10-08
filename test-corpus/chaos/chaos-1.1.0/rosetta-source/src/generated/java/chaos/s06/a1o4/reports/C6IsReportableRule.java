package chaos.s06.a1o4.reports;

import chaos.s06.a1o4.C6Event;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import java.math.BigDecimal;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C6IsReportableRule.C6IsReportableRuleDefault.class)
public abstract class C6IsReportableRule implements ReportFunction<C6Event, Boolean> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public Boolean evaluate(C6Event input) {
		Boolean output = doEvaluate(input);
		
		return output;
	}

	protected abstract Boolean doEvaluate(C6Event input);

	public static class C6IsReportableRuleDefault extends C6IsReportableRule {
		@Override
		protected Boolean doEvaluate(C6Event input) {
			Boolean output = null;
			return assignOutput(output, input);
		}
		
		protected Boolean assignOutput(Boolean output, C6Event input) {
			output = exists(MapperS.of(input).<BigDecimal>map("getQty", c6Event -> c6Event.getQty())).andNullSafe(greaterThan(MapperS.of(input).<BigDecimal>map("getQty", c6Event -> c6Event.getQty()), MapperS.of(BigDecimal.valueOf(0)), CardinalityOperator.All)).get();
			
			return output;
		}
	}
}
