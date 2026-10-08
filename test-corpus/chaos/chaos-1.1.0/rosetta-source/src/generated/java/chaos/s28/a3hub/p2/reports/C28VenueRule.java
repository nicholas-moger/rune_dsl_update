package chaos.s28.a3hub.p2.reports;

import chaos.s28.a3hub.p2.C28Trade;
import chaos.s28.a3hub.p2.functions.C28VenueOf;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import com.rosetta.model.metafields.FieldWithMetaString;
import javax.inject.Inject;


@ImplementedBy(C28VenueRule.C28VenueRuleDefault.class)
public abstract class C28VenueRule implements ReportFunction<C28Trade, String> {
	
	// RosettaFunction dependencies
	//
	@Inject protected C28VenueOf c28VenueOf;

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

	public static class C28VenueRuleDefault extends C28VenueRule {
		@Override
		protected String doEvaluate(C28Trade input) {
			String output = null;
			return assignOutput(output, input);
		}
		
		protected String assignOutput(String output, C28Trade input) {
			final FieldWithMetaString fieldWithMetaString = MapperS.of(input)
				.mapSingleToItem(item -> MapperS.of(c28VenueOf.evaluate(item.get()))).get();
			if (fieldWithMetaString == null) {
				output = null;
			} else {
				output = fieldWithMetaString.getValue();
			}
			
			return output;
		}
	}
}
