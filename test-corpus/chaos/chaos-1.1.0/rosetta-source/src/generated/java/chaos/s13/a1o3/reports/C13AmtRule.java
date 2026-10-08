package chaos.s13.a1o3.reports;

import chaos.s13.a1o3.C13Fact;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import java.math.BigDecimal;


@ImplementedBy(C13AmtRule.C13AmtRuleDefault.class)
public abstract class C13AmtRule implements ReportFunction<C13Fact, BigDecimal> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public BigDecimal evaluate(C13Fact input) {
		BigDecimal output = doEvaluate(input);
		
		return output;
	}

	protected abstract BigDecimal doEvaluate(C13Fact input);

	public static class C13AmtRuleDefault extends C13AmtRule {
		@Override
		protected BigDecimal doEvaluate(C13Fact input) {
			BigDecimal output = null;
			return assignOutput(output, input);
		}
		
		protected BigDecimal assignOutput(BigDecimal output, C13Fact input) {
			output = MapperS.of(input)
				.mapSingleToItem(item -> MapperS.of(item.<BigDecimal>map("getAmt", c13Fact -> c13Fact.getAmt()).getOrDefault(BigDecimal.valueOf(0)))).get();
			
			return output;
		}
	}
}
