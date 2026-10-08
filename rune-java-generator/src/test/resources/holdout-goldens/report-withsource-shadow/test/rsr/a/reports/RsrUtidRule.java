package test.rsr.a.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import test.rsr.a.RsrTrade;


@ImplementedBy(RsrUtidRule.RsrUtidRuleDefault.class)
public abstract class RsrUtidRule implements ReportFunction<RsrTrade, String> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public String evaluate(RsrTrade input) {
		String output = doEvaluate(input);
		
		return output;
	}

	protected abstract String doEvaluate(RsrTrade input);

	public static class RsrUtidRuleDefault extends RsrUtidRule {
		@Override
		protected String doEvaluate(RsrTrade input) {
			String output = null;
			return assignOutput(output, input);
		}
		
		protected String assignOutput(String output, RsrTrade input) {
			output = MapperS.of(input)
				.mapSingleToItem(item -> item.<String>map("getUtid", rsrTrade -> rsrTrade.getUtid())).get();
			
			return output;
		}
	}
}
