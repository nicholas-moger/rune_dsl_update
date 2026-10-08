package holdout.rulemetaoutputunwrap.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.metafields.FieldWithMetaString;
import holdout.rulemetaoutputunwrap.Trade;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;


@ImplementedBy(VenuesOf.VenuesOfDefault.class)
public abstract class VenuesOf implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param t 
	* @return venues 
	*/
	public List<? extends FieldWithMetaString> evaluate(Trade t) {
		List<FieldWithMetaString.FieldWithMetaStringBuilder> venuesBuilder = doEvaluate(t);
		
		final List<? extends FieldWithMetaString> venues;
		if (venuesBuilder == null) {
			venues = null;
		} else {
			venues = venuesBuilder.stream().map(FieldWithMetaString::build).collect(Collectors.toList());
			objectValidator.validate(FieldWithMetaString.class, venues);
		}
		
		return venues;
	}

	protected abstract List<FieldWithMetaString.FieldWithMetaStringBuilder> doEvaluate(Trade t);

	public static class VenuesOfDefault extends VenuesOf {
		@Override
		protected List<FieldWithMetaString.FieldWithMetaStringBuilder> doEvaluate(Trade t) {
			List<FieldWithMetaString.FieldWithMetaStringBuilder> venues = new ArrayList<>();
			return assignOutput(venues, t);
		}
		
		protected List<FieldWithMetaString.FieldWithMetaStringBuilder> assignOutput(List<FieldWithMetaString.FieldWithMetaStringBuilder> venues, Trade t) {
			venues.addAll(toBuilder(MapperS.of(t).<FieldWithMetaString>mapC("getVenues", trade -> trade.getVenues()).getMulti()));
			
			return Optional.ofNullable(venues)
				.map(o -> o.stream().map(i -> i.prune()).collect(Collectors.toList()))
				.orElse(null);
		}
	}
}
