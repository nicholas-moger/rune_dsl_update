package test.reports.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import test.reports.Trade;


@ImplementedBy(TradeIdRuleRule.TradeIdRuleRuleDefault.class)
public abstract class TradeIdRuleRule implements ReportFunction<Trade, String> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public String evaluate(Trade input) {
		String output = doEvaluate(input);
		
		return output;
	}

	protected abstract String doEvaluate(Trade input);

	public static class TradeIdRuleRuleDefault extends TradeIdRuleRule {
		@Override
		protected String doEvaluate(Trade input) {
			String output = null;
			return assignOutput(output, input);
		}
		
		protected String assignOutput(String output, Trade input) {
			output = MapperS.of(input)
				.mapSingleToItem(item -> item.<String>map("getTradeId", trade -> trade.getTradeId())).get();
			
			return output;
		}
	}
}
