package holdout.rulemetaoutputunwrap.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import com.rosetta.model.metafields.FieldWithMetaString;
import holdout.rulemetaoutputunwrap.Trade;
import holdout.rulemetaoutputunwrap.functions.VenueOf;
import javax.inject.Inject;


@ImplementedBy(VenueRule.VenueRuleDefault.class)
public abstract class VenueRule implements ReportFunction<Trade, String> {
	
	// RosettaFunction dependencies
	//
	@Inject protected VenueOf venueOf;

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public String evaluate(Trade input) {
		String output = doEvaluate(input);
		
		return output;
	}

	protected abstract String doEvaluate(Trade input);

	public static class VenueRuleDefault extends VenueRule {
		@Override
		protected String doEvaluate(Trade input) {
			String output = null;
			return assignOutput(output, input);
		}
		
		protected String assignOutput(String output, Trade input) {
			final FieldWithMetaString fieldWithMetaString = MapperS.of(input)
				.mapSingleToItem(item -> MapperS.of(venueOf.evaluate(item.get()))).get();
			if (fieldWithMetaString == null) {
				output = null;
			} else {
				output = fieldWithMetaString.getValue();
			}
			
			return output;
		}
	}
}
