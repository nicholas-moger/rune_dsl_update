package chaos.s06.a1o2.reports;

import chaos.s06.a1o2.C6Event;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import java.math.BigDecimal;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C6QtyRule.C6QtyRuleDefault.class)
public abstract class C6QtyRule implements ReportFunction<C6Event, BigDecimal> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public BigDecimal evaluate(C6Event input) {
		BigDecimal output = doEvaluate(input);
		
		return output;
	}

	protected abstract BigDecimal doEvaluate(C6Event input);

	public static class C6QtyRuleDefault extends C6QtyRule {
		@Override
		protected BigDecimal doEvaluate(C6Event input) {
			BigDecimal output = null;
			return assignOutput(output, input);
		}
		
		protected BigDecimal assignOutput(BigDecimal output, C6Event input) {
			output = MapperS.of(input)
				.mapSingleToItem(item -> {
					if (exists(item.<BigDecimal>map("getQty", c6Event -> c6Event.getQty())).getOrDefault(false)) {
						return MapperMaths.<BigDecimal, BigDecimal, BigDecimal>multiply(item.<BigDecimal>map("getQty", c6Event -> c6Event.getQty()), MapperS.of(BigDecimal.valueOf(1)));
					}
					return MapperS.of(BigDecimal.valueOf(0));
				}).get();
			
			return output;
		}
	}
}
