package test.rwq.a.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import java.math.BigDecimal;
import test.rwq.a.RwqTrade;


@ImplementedBy(RwqNotionalRule.RwqNotionalRuleDefault.class)
public abstract class RwqNotionalRule implements ReportFunction<RwqTrade, BigDecimal> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public BigDecimal evaluate(RwqTrade input) {
		BigDecimal output = doEvaluate(input);
		
		return output;
	}

	protected abstract BigDecimal doEvaluate(RwqTrade input);

	public static class RwqNotionalRuleDefault extends RwqNotionalRule {
		@Override
		protected BigDecimal doEvaluate(RwqTrade input) {
			BigDecimal output = null;
			return assignOutput(output, input);
		}
		
		protected BigDecimal assignOutput(BigDecimal output, RwqTrade input) {
			output = MapperS.of(input)
				.mapSingleToItem(item -> MapperS.of(item.<BigDecimal>map("getNotional", rwqTrade -> rwqTrade.getNotional()).getOrDefault(BigDecimal.valueOf(0)))).get();
			
			return output;
		}
	}
}
