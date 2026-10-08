package holdout.rulemetaoutputunwrap.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import com.rosetta.model.metafields.FieldWithMetaString;
import holdout.rulemetaoutputunwrap.Trade;
import holdout.rulemetaoutputunwrap.functions.VenueOf;
import javax.inject.Inject;


@ImplementedBy(VenueThenRule.VenueThenRuleDefault.class)
public abstract class VenueThenRule implements ReportFunction<Trade, String> {
	
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

	public static class VenueThenRuleDefault extends VenueThenRule {
		@Override
		protected String doEvaluate(Trade input) {
			String output = null;
			return assignOutput(output, input);
		}
		
		protected String assignOutput(String output, Trade input) {
			final MapperS<FieldWithMetaString> thenArg = MapperS.of(input)
				.mapSingleToItem(item -> MapperS.of(venueOf.evaluate(item.get())));
			output = thenArg
				.mapSingleToItem(item -> MapperMaths.<String, String, String>add(item.<String>map("Type coercion", fieldWithMetaString -> fieldWithMetaString == null ? null : fieldWithMetaString.getValue()), MapperS.of("-x"))).get();
			
			return output;
		}
	}
}
