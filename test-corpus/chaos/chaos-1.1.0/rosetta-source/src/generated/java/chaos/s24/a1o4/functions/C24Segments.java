package chaos.s24.a1o4.functions;

import chaos.s24.a1o4.C24Carrier;
import chaos.s24.a1o4.C24Keyed;
import chaos.s24.a1o4.C24Out;
import chaos.s24.a1o4.metafields.ReferenceWithMetaC24Keyed;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.metafields.FieldWithMetaVoid;
import java.util.Collections;
import java.util.Optional;
import javax.inject.Inject;


@ImplementedBy(C24Segments.C24SegmentsDefault.class)
public abstract class C24Segments implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param flag 
	* @param c 
	* @param d 
	* @param k 
	* @return out 
	*/
	public C24Out evaluate(Boolean flag, C24Carrier c, C24Carrier d, C24Keyed k) {
		C24Out.C24OutBuilder outBuilder = doEvaluate(flag, c, d, k);
		
		final C24Out out;
		if (outBuilder == null) {
			out = null;
		} else {
			out = outBuilder.build();
			objectValidator.validate(C24Out.class, out);
		}
		
		return out;
	}

	protected abstract C24Out.C24OutBuilder doEvaluate(Boolean flag, C24Carrier c, C24Carrier d, C24Keyed k);

	public static class C24SegmentsDefault extends C24Segments {
		@Override
		protected C24Out.C24OutBuilder doEvaluate(Boolean flag, C24Carrier c, C24Carrier d, C24Keyed k) {
			C24Out.C24OutBuilder out = C24Out.builder();
			return assignOutput(out, flag, c, d, k);
		}
		
		protected C24Out.C24OutBuilder assignOutput(C24Out.C24OutBuilder out, Boolean flag, C24Carrier c, C24Carrier d, C24Keyed k) {
			final FieldWithMetaVoid ifThenElseResult0;
			if ((flag == null ? false : flag)) {
				ifThenElseResult0 = FieldWithMetaVoid.builder().build();
			} else {
				ifThenElseResult0 = FieldWithMetaVoid.builder().build();
			}
			out
				.setVm(ifThenElseResult0);
			
			out
				.setVs(Collections.<Void>emptyList());
			
			out
				.setV(null);
			
			out
				.getOrCreateCar()
				.setTok(null);
			
			C24Keyed outKref = null;
			if ((flag == null ? false : flag)) {
				outKref = k;
			}
			out
				.setKref(ReferenceWithMetaC24Keyed.builder()
					.setGlobalReference(Optional.ofNullable(outKref)
						.map(r -> r.getMeta())
						.map(m -> m.getGlobalKey())
						.orElse(null))
					.setExternalReference(Optional.ofNullable(outKref)
						.map(r -> r.getMeta())
						.map(m -> m.getExternalKey())
						.orElse(null))
					.build()
				);
			
			String ifThenElseResult1 = "n";
			if ((flag == null ? false : flag)) {
				ifThenElseResult1 = MapperS.of(c).<String>map("getName", c24Carrier -> c24Carrier.getName()).get();
			}
			out
				.setS(ifThenElseResult1);
			
			return Optional.ofNullable(out)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
