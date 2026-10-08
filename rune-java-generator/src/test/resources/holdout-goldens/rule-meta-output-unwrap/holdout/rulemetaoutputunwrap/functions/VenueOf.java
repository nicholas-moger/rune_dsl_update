package holdout.rulemetaoutputunwrap.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.metafields.FieldWithMetaString;
import holdout.rulemetaoutputunwrap.Trade;
import java.util.Optional;
import javax.inject.Inject;


@ImplementedBy(VenueOf.VenueOfDefault.class)
public abstract class VenueOf implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param t 
	* @return venue 
	*/
	public FieldWithMetaString evaluate(Trade t) {
		FieldWithMetaString.FieldWithMetaStringBuilder venueBuilder = doEvaluate(t);
		
		final FieldWithMetaString venue;
		if (venueBuilder == null) {
			venue = null;
		} else {
			venue = venueBuilder.build();
			objectValidator.validate(FieldWithMetaString.class, venue);
		}
		
		return venue;
	}

	protected abstract FieldWithMetaString.FieldWithMetaStringBuilder doEvaluate(Trade t);

	public static class VenueOfDefault extends VenueOf {
		@Override
		protected FieldWithMetaString.FieldWithMetaStringBuilder doEvaluate(Trade t) {
			FieldWithMetaString.FieldWithMetaStringBuilder venue = FieldWithMetaString.builder();
			return assignOutput(venue, t);
		}
		
		protected FieldWithMetaString.FieldWithMetaStringBuilder assignOutput(FieldWithMetaString.FieldWithMetaStringBuilder venue, Trade t) {
			venue = toBuilder(MapperS.of(t).<FieldWithMetaString>map("getVenue", trade -> trade.getVenue()).get());
			
			return Optional.ofNullable(venue)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
