package test.mladderedge.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.metafields.FieldWithMetaString;
import java.util.Optional;
import javax.inject.Inject;
import test.mladderedge.Holder;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(WrappedOutput.WrappedOutputDefault.class)
public abstract class WrappedOutput implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param h 
	* @return pick 
	*/
	public FieldWithMetaString evaluate(Holder h) {
		FieldWithMetaString.FieldWithMetaStringBuilder pickBuilder = doEvaluate(h);
		
		final FieldWithMetaString pick;
		if (pickBuilder == null) {
			pick = null;
		} else {
			pick = pickBuilder.build();
			objectValidator.validate(FieldWithMetaString.class, pick);
		}
		
		return pick;
	}

	protected abstract FieldWithMetaString.FieldWithMetaStringBuilder doEvaluate(Holder h);

	protected abstract MapperC<? extends FieldWithMetaString> goodCodes(Holder h);

	public static class WrappedOutputDefault extends WrappedOutput {
		@Override
		protected FieldWithMetaString.FieldWithMetaStringBuilder doEvaluate(Holder h) {
			FieldWithMetaString.FieldWithMetaStringBuilder pick = FieldWithMetaString.builder();
			return assignOutput(pick, h);
		}
		
		protected FieldWithMetaString.FieldWithMetaStringBuilder assignOutput(FieldWithMetaString.FieldWithMetaStringBuilder pick, Holder h) {
			if (exists(MapperS.of(h).<FieldWithMetaString>map("getCoded", holder -> holder.getCoded())).getOrDefault(false)) {
				pick = toBuilder(MapperS.of(h).<FieldWithMetaString>map("getCoded", holder -> holder.getCoded()).get());
			} else {
				pick = toBuilder(goodCodes(h)
					.first().get());
			}
			
			return Optional.ofNullable(pick)
				.map(o -> o.prune())
				.orElse(null);
		}
		
		@Override
		protected MapperC<? extends FieldWithMetaString> goodCodes(Holder h) {
			final MapperC<FieldWithMetaString> thenArg = MapperS.of(h).<FieldWithMetaString>mapC("getCodes", holder -> holder.getCodes());
			return thenArg
				.filterItemNullSafe(item -> notEqual(item.<String>map("Type coercion", fieldWithMetaString -> fieldWithMetaString == null ? null : fieldWithMetaString.getValue()), MapperS.of("void"), CardinalityOperator.Any).get());
		}
	}
}
