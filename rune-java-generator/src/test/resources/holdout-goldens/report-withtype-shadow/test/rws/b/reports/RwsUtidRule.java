package test.rws.b.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import test.rws.b.RwsTrade;


@ImplementedBy(RwsUtidRule.RwsUtidRuleDefault.class)
public abstract class RwsUtidRule implements ReportFunction<RwsTrade, String> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public String evaluate(RwsTrade input) {
		String output = doEvaluate(input);
		
		return output;
	}

	protected abstract String doEvaluate(RwsTrade input);

	public static class RwsUtidRuleDefault extends RwsUtidRule {
		@Override
		protected String doEvaluate(RwsTrade input) {
			String output = null;
			return assignOutput(output, input);
		}
		
		protected String assignOutput(String output, RwsTrade input) {
			output = MapperS.of(input)
				.mapSingleToItem(item -> item.<String>map("getUtid", rwsTrade -> rwsTrade.getUtid())).get();
			
			return output;
		}
	}
}
