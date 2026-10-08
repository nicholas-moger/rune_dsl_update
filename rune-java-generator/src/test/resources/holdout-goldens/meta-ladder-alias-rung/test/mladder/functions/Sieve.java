package test.mladder.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.metafields.FieldWithMetaString;
import test.mladder.Holder;
import test.mladder.Keyed;
import test.mladder.metafields.ReferenceWithMetaKeyed;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(Sieve.SieveDefault.class)
public abstract class Sieve implements RosettaFunction {

	/**
	* @param h 
	* @return pick 
	*/
	public String evaluate(Holder h) {
		String pick = doEvaluate(h);
		
		return pick;
	}

	protected abstract String doEvaluate(Holder h);

	protected abstract MapperS<? extends ReferenceWithMetaKeyed> firstRef(Holder h);

	protected abstract MapperC<? extends FieldWithMetaString> goodCodes(Holder h);

	public static class SieveDefault extends Sieve {
		@Override
		protected String doEvaluate(Holder h) {
			String pick = null;
			return assignOutput(pick, h);
		}
		
		protected String assignOutput(String pick, Holder h) {
			if (exists(MapperS.of(h).<FieldWithMetaString>map("getCoded", holder -> holder.getCoded())).getOrDefault(false)) {
				final FieldWithMetaString fieldWithMetaString0 = MapperS.of(h).<FieldWithMetaString>map("getCoded", holder -> holder.getCoded()).get();
				if (fieldWithMetaString0 == null) {
					pick = null;
				} else {
					pick = fieldWithMetaString0.getValue();
				}
			} else if (exists(goodCodes(h)).getOrDefault(false)) {
				final FieldWithMetaString fieldWithMetaString1 = goodCodes(h)
					.first().get();
				if (fieldWithMetaString1 == null) {
					pick = null;
				} else {
					pick = fieldWithMetaString1.getValue();
				}
			} else {
				pick = firstRef(h).<Keyed>map("Type coercion", referenceWithMetaKeyed -> referenceWithMetaKeyed == null ? null : referenceWithMetaKeyed.getValue()).<String>map("getKid", keyed -> keyed.getKid()).get();
			}
			
			return pick;
		}
		
		@Override
		protected MapperS<? extends ReferenceWithMetaKeyed> firstRef(Holder h) {
			final MapperC<ReferenceWithMetaKeyed> thenArg0 = MapperS.of(h).<ReferenceWithMetaKeyed>mapC("getByRefs", holder -> holder.getByRefs());
			final MapperC<ReferenceWithMetaKeyed> thenArg1 = thenArg0
				.filterItemNullSafe(item -> exists(item.<Keyed>map("Type coercion", referenceWithMetaKeyed -> referenceWithMetaKeyed == null ? null : referenceWithMetaKeyed.getValue()).<String>map("getKid", keyed -> keyed.getKid())).get());
			return thenArg1
				.first();
		}
		
		@Override
		protected MapperC<? extends FieldWithMetaString> goodCodes(Holder h) {
			final MapperC<FieldWithMetaString> thenArg = MapperS.of(h).<FieldWithMetaString>mapC("getCodes", holder -> holder.getCodes());
			return thenArg
				.filterItemNullSafe(item -> notEqual(item.<String>map("Type coercion", fieldWithMetaString -> fieldWithMetaString == null ? null : fieldWithMetaString.getValue()), MapperS.of("void"), CardinalityOperator.Any).get());
		}
	}
}
