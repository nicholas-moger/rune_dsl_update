package chaos.s24.a3half.p2.reports;

import chaos.s24.a3half.p1.C24Carrier;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C24TokPresenceRule.C24TokPresenceRuleDefault.class)
public abstract class C24TokPresenceRule implements ReportFunction<C24Carrier, String> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public String evaluate(C24Carrier input) {
		String output = doEvaluate(input);
		
		return output;
	}

	protected abstract String doEvaluate(C24Carrier input);

	public static class C24TokPresenceRuleDefault extends C24TokPresenceRule {
		@Override
		protected String doEvaluate(C24Carrier input) {
			String output = null;
			return assignOutput(output, input);
		}
		
		protected String assignOutput(String output, C24Carrier input) {
			output = MapperS.of(input)
				.mapSingleToItem(item -> {
					if (exists(MapperS.<Void>ofNull()).getOrDefault(false)) {
						return item.<String>map("getName", c24Carrier -> c24Carrier.getName());
					}
					return MapperS.of("absent");
				}).get();
			
			return output;
		}
	}
}
