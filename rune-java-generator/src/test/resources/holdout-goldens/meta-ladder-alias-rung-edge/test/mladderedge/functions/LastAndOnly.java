package test.mladderedge.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.metafields.FieldWithMetaString;
import test.mladderedge.Holder;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(LastAndOnly.LastAndOnlyDefault.class)
public abstract class LastAndOnly implements RosettaFunction {

	/**
	* @param h 
	* @return pick 
	*/
	public String evaluate(Holder h) {
		String pick = doEvaluate(h);
		
		return pick;
	}

	protected abstract String doEvaluate(Holder h);

	protected abstract MapperC<? extends FieldWithMetaString> goodCodes(Holder h);

	public static class LastAndOnlyDefault extends LastAndOnly {
		@Override
		protected String doEvaluate(Holder h) {
			String pick = null;
			return assignOutput(pick, h);
		}
		
		protected String assignOutput(String pick, Holder h) {
			if (exists(MapperS.of(h).<FieldWithMetaString>map("getCoded", holder -> holder.getCoded())).getOrDefault(false)) {
				final FieldWithMetaString fieldWithMetaString0 = goodCodes(h)
					.last().get();
				if (fieldWithMetaString0 == null) {
					pick = null;
				} else {
					pick = fieldWithMetaString0.getValue();
				}
			} else {
				final FieldWithMetaString fieldWithMetaString1 = goodCodes(h).get();
				if (fieldWithMetaString1 == null) {
					pick = null;
				} else {
					pick = fieldWithMetaString1.getValue();
				}
			}
			
			return pick;
		}
		
		@Override
		protected MapperC<? extends FieldWithMetaString> goodCodes(Holder h) {
			final MapperC<FieldWithMetaString> thenArg = MapperS.of(h).<FieldWithMetaString>mapC("getCodes", holder -> holder.getCodes());
			return thenArg
				.filterItemNullSafe(item -> notEqual(item.<String>map("Type coercion", fieldWithMetaString -> fieldWithMetaString == null ? null : fieldWithMetaString.getValue()), MapperS.of("void"), CardinalityOperator.Any).get());
		}
	}
}
