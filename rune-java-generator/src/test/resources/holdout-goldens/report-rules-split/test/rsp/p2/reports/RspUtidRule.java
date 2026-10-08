package test.rsp.p2.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import test.rsp.p2.RspTrade;


@ImplementedBy(RspUtidRule.RspUtidRuleDefault.class)
public abstract class RspUtidRule implements ReportFunction<RspTrade, String> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public String evaluate(RspTrade input) {
		String output = doEvaluate(input);
		
		return output;
	}

	protected abstract String doEvaluate(RspTrade input);

	public static class RspUtidRuleDefault extends RspUtidRule {
		@Override
		protected String doEvaluate(RspTrade input) {
			String output = null;
			return assignOutput(output, input);
		}
		
		protected String assignOutput(String output, RspTrade input) {
			output = MapperS.of(input)
				.mapSingleToItem(item -> item.<String>map("getUtid", rspTrade -> rspTrade.getUtid())).get();
			
			return output;
		}
	}
}
