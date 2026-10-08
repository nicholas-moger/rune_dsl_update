package test.rsh.a.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import test.rsh.a.RshTrade;


@ImplementedBy(RshReportRule.RshReportRuleDefault.class)
public abstract class RshReportRule implements ReportFunction<RshTrade, String> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public String evaluate(RshTrade input) {
		String output = doEvaluate(input);
		
		return output;
	}

	protected abstract String doEvaluate(RshTrade input);

	public static class RshReportRuleDefault extends RshReportRule {
		@Override
		protected String doEvaluate(RshTrade input) {
			String output = null;
			return assignOutput(output, input);
		}
		
		protected String assignOutput(String output, RshTrade input) {
			output = MapperS.of(input)
				.mapSingleToItem(item -> item.<String>map("getUtid", rshTrade -> rshTrade.getUtid())).get();
			
			return output;
		}
	}
}
