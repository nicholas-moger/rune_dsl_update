package chaos.s28.a3hub.p2.functions;

import chaos.s28.a3hub.p2.C28Trade;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.metafields.FieldWithMetaString;
import java.util.Optional;
import javax.inject.Inject;


@ImplementedBy(C28VenueOf.C28VenueOfDefault.class)
public abstract class C28VenueOf implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param t 
	* @return v 
	*/
	public FieldWithMetaString evaluate(C28Trade t) {
		FieldWithMetaString.FieldWithMetaStringBuilder vBuilder = doEvaluate(t);
		
		final FieldWithMetaString v;
		if (vBuilder == null) {
			v = null;
		} else {
			v = vBuilder.build();
			objectValidator.validate(FieldWithMetaString.class, v);
		}
		
		return v;
	}

	protected abstract FieldWithMetaString.FieldWithMetaStringBuilder doEvaluate(C28Trade t);

	public static class C28VenueOfDefault extends C28VenueOf {
		@Override
		protected FieldWithMetaString.FieldWithMetaStringBuilder doEvaluate(C28Trade t) {
			FieldWithMetaString.FieldWithMetaStringBuilder v = FieldWithMetaString.builder();
			return assignOutput(v, t);
		}
		
		protected FieldWithMetaString.FieldWithMetaStringBuilder assignOutput(FieldWithMetaString.FieldWithMetaStringBuilder v, C28Trade t) {
			v = toBuilder(MapperS.of(t).<FieldWithMetaString>map("getVenue", c28Trade -> c28Trade.getVenue()).get());
			
			return Optional.ofNullable(v)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
