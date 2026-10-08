package test.rsr.a.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import java.math.BigDecimal;
import test.rsr.a.RsrTrade;


@ImplementedBy(RsrNotionalRule.RsrNotionalRuleDefault.class)
public abstract class RsrNotionalRule implements ReportFunction<RsrTrade, BigDecimal> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public BigDecimal evaluate(RsrTrade input) {
		BigDecimal output = doEvaluate(input);
		
		return output;
	}

	protected abstract BigDecimal doEvaluate(RsrTrade input);

	public static class RsrNotionalRuleDefault extends RsrNotionalRule {
		@Override
		protected BigDecimal doEvaluate(RsrTrade input) {
			BigDecimal output = null;
			return assignOutput(output, input);
		}
		
		protected BigDecimal assignOutput(BigDecimal output, RsrTrade input) {
			output = MapperS.of(input)
				.mapSingleToItem(item -> MapperS.of(item.<BigDecimal>map("getNotional", rsrTrade -> rsrTrade.getNotional()).getOrDefault(BigDecimal.valueOf(0)))).get();
			
			return output;
		}
	}
}
