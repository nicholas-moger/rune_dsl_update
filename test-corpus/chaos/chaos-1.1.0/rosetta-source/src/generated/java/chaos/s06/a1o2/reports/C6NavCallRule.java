package chaos.s06.a1o2.reports;

import chaos.s06.a1o2.C6Event;
import chaos.s06.a1o2.functions.C6MarkOf;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import java.math.BigDecimal;
import javax.inject.Inject;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C6NavCallRule.C6NavCallRuleDefault.class)
public abstract class C6NavCallRule implements ReportFunction<C6Event, String> {
	
	// RosettaFunction dependencies
	//
	@Inject protected C6MarkOf c6MarkOf;

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

	public static class C6NavCallRuleDefault extends C6NavCallRule {
		@Override
		protected String doEvaluate(C6Event input) {
			String output = null;
			return assignOutput(output, input);
		}
		
		protected String assignOutput(String output, C6Event input) {
			output = MapperS.of(input)
				.mapSingleToItem(item -> {
					if (exists(item.<BigDecimal>map("getQty", c6Event -> c6Event.getQty())).getOrDefault(false)) {
						return MapperS.of(MapperS.of(c6MarkOf.evaluate(item.get())).<String>map("getCaption", c6Tag -> c6Tag.getCaption()).getOrDefault("untagged"));
					}
					return MapperS.of("no-qty");
				}).get();
			
			return output;
		}
	}
}
