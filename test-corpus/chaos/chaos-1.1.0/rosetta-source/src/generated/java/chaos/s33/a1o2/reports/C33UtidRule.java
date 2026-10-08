package chaos.s33.a1o2.reports;

import chaos.s33.a1o2.C33Trade;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;


@ImplementedBy(C33UtidRule.C33UtidRuleDefault.class)
public abstract class C33UtidRule implements ReportFunction<C33Trade, String> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public String evaluate(C33Trade input) {
		String output = doEvaluate(input);
		
		return output;
	}

	protected abstract String doEvaluate(C33Trade input);

	public static class C33UtidRuleDefault extends C33UtidRule {
		@Override
		protected String doEvaluate(C33Trade input) {
			String output = null;
			return assignOutput(output, input);
		}
		
		protected String assignOutput(String output, C33Trade input) {
			output = MapperS.of(input)
				.mapSingleToItem(item -> item.<String>map("getUtid", c33Trade -> c33Trade.getUtid())).get();
			
			return output;
		}
	}
}
