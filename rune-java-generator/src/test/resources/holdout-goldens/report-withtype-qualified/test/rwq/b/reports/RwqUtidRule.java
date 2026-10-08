package test.rwq.b.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import test.rwq.b.RwqTrade;


@ImplementedBy(RwqUtidRule.RwqUtidRuleDefault.class)
public abstract class RwqUtidRule implements ReportFunction<RwqTrade, String> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public String evaluate(RwqTrade input) {
		String output = doEvaluate(input);
		
		return output;
	}

	protected abstract String doEvaluate(RwqTrade input);

	public static class RwqUtidRuleDefault extends RwqUtidRule {
		@Override
		protected String doEvaluate(RwqTrade input) {
			String output = null;
			return assignOutput(output, input);
		}
		
		protected String assignOutput(String output, RwqTrade input) {
			output = MapperS.of(input)
				.mapSingleToItem(item -> item.<String>map("getUtid", rwqTrade -> rwqTrade.getUtid())).get();
			
			return output;
		}
	}
}
