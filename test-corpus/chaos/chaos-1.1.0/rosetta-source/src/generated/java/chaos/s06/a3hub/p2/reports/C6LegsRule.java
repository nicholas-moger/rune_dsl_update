package chaos.s06.a3hub.p2.reports;

import chaos.s06.a3hub.p2.C6Event;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C6LegsRule.C6LegsRuleDefault.class)
public abstract class C6LegsRule implements ReportFunction<C6Event, String> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public String evaluate(C6Event input) {
		String output = doEvaluate(input);
		
		return output;
	}

	protected abstract String doEvaluate(C6Event input);

	public static class C6LegsRuleDefault extends C6LegsRule {
		@Override
		protected String doEvaluate(C6Event input) {
			String output = null;
			return assignOutput(output, input);
		}
		
		protected String assignOutput(String output, C6Event input) {
			final MapperC<String> thenArg0 = MapperS.of(input)
				.mapSingleToList(item -> item.<String>mapC("getLegs", c6Event -> c6Event.getLegs()));
			final MapperC<String> thenArg1 = thenArg0
				.filterItemNullSafe(item -> notEqual(item, MapperS.of(""), CardinalityOperator.Any).get());
			output = thenArg1.join(MapperS.of(";")).get();
			
			return output;
		}
	}
}
