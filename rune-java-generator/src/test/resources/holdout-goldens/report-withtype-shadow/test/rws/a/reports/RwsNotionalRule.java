package test.rws.a.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import java.math.BigDecimal;
import test.rws.a.RwsTrade;


@ImplementedBy(RwsNotionalRule.RwsNotionalRuleDefault.class)
public abstract class RwsNotionalRule implements ReportFunction<RwsTrade, BigDecimal> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public BigDecimal evaluate(RwsTrade input) {
		BigDecimal output = doEvaluate(input);
		
		return output;
	}

	protected abstract BigDecimal doEvaluate(RwsTrade input);

	public static class RwsNotionalRuleDefault extends RwsNotionalRule {
		@Override
		protected BigDecimal doEvaluate(RwsTrade input) {
			BigDecimal output = null;
			return assignOutput(output, input);
		}
		
		protected BigDecimal assignOutput(BigDecimal output, RwsTrade input) {
			output = MapperS.of(input)
				.mapSingleToItem(item -> MapperS.of(item.<BigDecimal>map("getNotional", rwsTrade -> rwsTrade.getNotional()).getOrDefault(BigDecimal.valueOf(0)))).get();
			
			return output;
		}
	}
}
