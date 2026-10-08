package chaos.s28.a3hub.p2.reports;

import chaos.s28.a3hub.p2.C28Trade;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;


@ImplementedBy(C28UtidRule.C28UtidRuleDefault.class)
public abstract class C28UtidRule implements ReportFunction<C28Trade, String> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public String evaluate(C28Trade input) {
		String output = doEvaluate(input);
		
		return output;
	}

	protected abstract String doEvaluate(C28Trade input);

	public static class C28UtidRuleDefault extends C28UtidRule {
		@Override
		protected String doEvaluate(C28Trade input) {
			String output = null;
			return assignOutput(output, input);
		}
		
		protected String assignOutput(String output, C28Trade input) {
			output = MapperS.of(input)
				.mapSingleToItem(item -> item.<String>map("getUtid", c28Trade -> c28Trade.getUtid())).get();
			
			return output;
		}
	}
}
