package holdout.rulemetaoutputunwrap.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import holdout.rulemetaoutputunwrap.Trade;


@ImplementedBy(UtidRule.UtidRuleDefault.class)
public abstract class UtidRule implements ReportFunction<Trade, String> {

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

	public static class UtidRuleDefault extends UtidRule {
		@Override
		protected String doEvaluate(Trade input) {
			String output = null;
			return assignOutput(output, input);
		}
		
		protected String assignOutput(String output, Trade input) {
			output = MapperS.of(input)
				.mapSingleToItem(item -> item.<String>map("getUtid", trade -> trade.getUtid())).get();
			
			return output;
		}
	}
}
