package holdout.rulemetaoutputunwrap.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import com.rosetta.model.metafields.FieldWithMetaString;
import holdout.rulemetaoutputunwrap.Trade;


@ImplementedBy(VenueDirectRule.VenueDirectRuleDefault.class)
public abstract class VenueDirectRule implements ReportFunction<Trade, String> {

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

	public static class VenueDirectRuleDefault extends VenueDirectRule {
		@Override
		protected String doEvaluate(Trade input) {
			String output = null;
			return assignOutput(output, input);
		}
		
		protected String assignOutput(String output, Trade input) {
			final FieldWithMetaString fieldWithMetaString = MapperS.of(input)
				.mapSingleToItem(item -> item.<FieldWithMetaString>map("getVenue", trade -> trade.getVenue())).get();
			if (fieldWithMetaString == null) {
				output = null;
			} else {
				output = fieldWithMetaString.getValue();
			}
			
			return output;
		}
	}
}
