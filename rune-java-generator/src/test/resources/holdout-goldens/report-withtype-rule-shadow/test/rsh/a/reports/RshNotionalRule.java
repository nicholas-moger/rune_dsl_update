package test.rsh.a.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import java.math.BigDecimal;
import test.rsh.a.RshTrade;


@ImplementedBy(RshNotionalRule.RshNotionalRuleDefault.class)
public abstract class RshNotionalRule implements ReportFunction<RshTrade, BigDecimal> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public BigDecimal evaluate(RshTrade input) {
		BigDecimal output = doEvaluate(input);
		
		return output;
	}

	protected abstract BigDecimal doEvaluate(RshTrade input);

	public static class RshNotionalRuleDefault extends RshNotionalRule {
		@Override
		protected BigDecimal doEvaluate(RshTrade input) {
			BigDecimal output = null;
			return assignOutput(output, input);
		}
		
		protected BigDecimal assignOutput(BigDecimal output, RshTrade input) {
			output = MapperS.of(input)
				.mapSingleToItem(item -> MapperS.of(item.<BigDecimal>map("getNotional", rshTrade -> rshTrade.getNotional()).getOrDefault(BigDecimal.valueOf(0)))).get();
			
			return output;
		}
	}
}
