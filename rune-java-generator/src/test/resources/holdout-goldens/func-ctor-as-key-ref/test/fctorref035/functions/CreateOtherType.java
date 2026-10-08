package test.fctorref035.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import test.fctorref035.OtherType;
import test.fctorref035.TypeWithKey;
import test.fctorref035.metafields.ReferenceWithMetaTypeWithKey;


@ImplementedBy(CreateOtherType.CreateOtherTypeDefault.class)
public abstract class CreateOtherType implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param key 
	* @return result 
	*/
	public OtherType evaluate(TypeWithKey key) {
		OtherType.OtherTypeBuilder resultBuilder = doEvaluate(key);
		
		final OtherType result;
		if (resultBuilder == null) {
			result = null;
		} else {
			result = resultBuilder.build();
			objectValidator.validate(OtherType.class, result);
		}
		
		return result;
	}

	protected abstract OtherType.OtherTypeBuilder doEvaluate(TypeWithKey key);

	public static class CreateOtherTypeDefault extends CreateOtherType {
		@Override
		protected OtherType.OtherTypeBuilder doEvaluate(TypeWithKey key) {
			OtherType.OtherTypeBuilder result = OtherType.builder();
			return assignOutput(result, key);
		}
		
		protected OtherType.OtherTypeBuilder assignOutput(OtherType.OtherTypeBuilder result, TypeWithKey key) {
			result = toBuilder(OtherType.builder()
				.setAttrSingle(ReferenceWithMetaTypeWithKey.builder()
					.setGlobalReference(Optional.ofNullable(key)
						.map(r -> r.getMeta())
						.map(m -> m.getGlobalKey())
						.orElse(null))
					.setExternalReference(Optional.ofNullable(key)
						.map(r -> r.getMeta())
						.map(m -> m.getExternalKey())
						.orElse(null))
					.build())
				.setAttrMulti(MapperC.<TypeWithKey>of(MapperS.of(key), MapperS.of(key))
					.getItems()
					.map(item -> ReferenceWithMetaTypeWithKey.builder()
						.setExternalReference(item.getMappedObject().getMeta().getExternalKey())
						.setGlobalReference(item.getMappedObject().getMeta().getGlobalKey())
						.build())
					.collect(Collectors.toList()))
				.build());
			
			return Optional.ofNullable(result)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
