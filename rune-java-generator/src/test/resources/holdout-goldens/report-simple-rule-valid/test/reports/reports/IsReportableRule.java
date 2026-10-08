package test.reports.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import test.reports.Trade;


@ImplementedBy(IsReportableRule.IsReportableRuleDefault.class)
public abstract class IsReportableRule implements ReportFunction<Trade, Boolean> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public Boolean evaluate(Trade input) {
		Boolean output = doEvaluate(input);
		
		return output;
	}

	protected abstract Boolean doEvaluate(Trade input);

	public static class IsReportableRuleDefault extends IsReportableRule {
		@Override
		protected Boolean doEvaluate(Trade input) {
			Boolean output = null;
			return assignOutput(output, input);
		}
		
		protected Boolean assignOutput(Boolean output, Trade input) {
			output = MapperS.of(input)
				.mapSingleToItem(item -> item.<Boolean>map("getIsActive", trade -> trade.getIsActive())).get();
			
			return output;
		}
	}
}
