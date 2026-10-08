package test.bulkaskey.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import test.bulkaskey.OtherType;
import test.bulkaskey.WithMeta;
import test.bulkaskey.metafields.ReferenceWithMetaWithMeta;


@ImplementedBy(asKeyUsage.asKeyUsageDefault.class)
public abstract class asKeyUsage implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param withMeta 
	* @return out 
	*/
	public OtherType evaluate(List<? extends WithMeta> withMeta) {
		OtherType.OtherTypeBuilder outBuilder = doEvaluate(withMeta);
		
		final OtherType out;
		if (outBuilder == null) {
			out = null;
		} else {
			out = outBuilder.build();
			objectValidator.validate(OtherType.class, out);
		}
		
		return out;
	}

	protected abstract OtherType.OtherTypeBuilder doEvaluate(List<? extends WithMeta> withMeta);

	public static class asKeyUsageDefault extends asKeyUsage {
		@Override
		protected OtherType.OtherTypeBuilder doEvaluate(List<? extends WithMeta> withMeta) {
			if (withMeta == null) {
				withMeta = Collections.emptyList();
			}
			OtherType.OtherTypeBuilder out = OtherType.builder();
			return assignOutput(out, withMeta);
		}
		
		protected OtherType.OtherTypeBuilder assignOutput(OtherType.OtherTypeBuilder out, List<? extends WithMeta> withMeta) {
			out
				.addAttrMulti(MapperC.<WithMeta>of(withMeta)
					.getItems()
					.map(item -> ReferenceWithMetaWithMeta.builder()
						.setExternalReference(item.getMappedObject().getMeta().getExternalKey())
						.setGlobalReference(item.getMappedObject().getMeta().getGlobalKey())
						.build())
					.collect(Collectors.toList())
				);
			
			final WithMeta outAttrSingle = MapperC.of(withMeta).get();
			out
				.setAttrSingle(ReferenceWithMetaWithMeta.builder()
					.setGlobalReference(Optional.ofNullable(outAttrSingle)
						.map(r -> r.getMeta())
						.map(m -> m.getGlobalKey())
						.orElse(null))
					.setExternalReference(Optional.ofNullable(outAttrSingle)
						.map(r -> r.getMeta())
						.map(m -> m.getExternalKey())
						.orElse(null))
					.build()
				);
			
			return Optional.ofNullable(out)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
