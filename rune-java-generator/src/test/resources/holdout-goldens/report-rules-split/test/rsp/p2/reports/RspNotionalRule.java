package test.rsp.p2.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import java.math.BigDecimal;
import test.rsp.p2.RspTrade;


@ImplementedBy(RspNotionalRule.RspNotionalRuleDefault.class)
public abstract class RspNotionalRule implements ReportFunction<RspTrade, BigDecimal> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public BigDecimal evaluate(RspTrade input) {
		BigDecimal output = doEvaluate(input);
		
		return output;
	}

	protected abstract BigDecimal doEvaluate(RspTrade input);

	public static class RspNotionalRuleDefault extends RspNotionalRule {
		@Override
		protected BigDecimal doEvaluate(RspTrade input) {
			BigDecimal output = null;
			return assignOutput(output, input);
		}
		
		protected BigDecimal assignOutput(BigDecimal output, RspTrade input) {
			output = MapperS.of(input)
				.mapSingleToItem(item -> MapperS.of(item.<BigDecimal>map("getNotional", rspTrade -> rspTrade.getNotional()).getOrDefault(BigDecimal.valueOf(0)))).get();
			
			return output;
		}
	}
}
