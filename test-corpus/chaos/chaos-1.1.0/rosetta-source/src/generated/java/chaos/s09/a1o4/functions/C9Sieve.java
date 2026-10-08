package chaos.s09.a1o4.functions;

import chaos.s09.a1o4.C9Holder;
import chaos.s09.a1o4.C9Keyed;
import chaos.s09.a1o4.metafields.ReferenceWithMetaC9Keyed;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.metafields.FieldWithMetaString;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C9Sieve.C9SieveDefault.class)
public abstract class C9Sieve implements RosettaFunction {

	/**
	* @param h 
	* @return pick 
	*/
	public String evaluate(C9Holder h) {
		String pick = doEvaluate(h);
		
		return pick;
	}

	protected abstract String doEvaluate(C9Holder h);

	protected abstract MapperS<? extends ReferenceWithMetaC9Keyed> firstRef(C9Holder h);

	protected abstract MapperC<? extends FieldWithMetaString> goodCodes(C9Holder h);

	public static class C9SieveDefault extends C9Sieve {
		@Override
		protected String doEvaluate(C9Holder h) {
			String pick = null;
			return assignOutput(pick, h);
		}
		
		protected String assignOutput(String pick, C9Holder h) {
			if (exists(MapperS.of(h).<FieldWithMetaString>map("getCoded", c9Holder -> c9Holder.getCoded())).getOrDefault(false)) {
				final FieldWithMetaString fieldWithMetaString0 = MapperS.of(h).<FieldWithMetaString>map("getCoded", c9Holder -> c9Holder.getCoded()).get();
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
				pick = firstRef(h).<C9Keyed>map("Type coercion", referenceWithMetaC9Keyed -> referenceWithMetaC9Keyed == null ? null : referenceWithMetaC9Keyed.getValue()).<String>map("getKid", c9Keyed -> c9Keyed.getKid()).get();
			}
			
			return pick;
		}
		
		@Override
		protected MapperS<? extends ReferenceWithMetaC9Keyed> firstRef(C9Holder h) {
			final MapperC<ReferenceWithMetaC9Keyed> thenArg0 = MapperS.of(h).<ReferenceWithMetaC9Keyed>mapC("getByRefs", c9Holder -> c9Holder.getByRefs());
			final MapperC<ReferenceWithMetaC9Keyed> thenArg1 = thenArg0
				.filterItemNullSafe(item -> exists(item.<C9Keyed>map("Type coercion", referenceWithMetaC9Keyed -> referenceWithMetaC9Keyed == null ? null : referenceWithMetaC9Keyed.getValue()).<String>map("getKid", c9Keyed -> c9Keyed.getKid())).get());
			return thenArg1
				.first();
		}
		
		@Override
		protected MapperC<? extends FieldWithMetaString> goodCodes(C9Holder h) {
			final MapperC<FieldWithMetaString> thenArg = MapperS.of(h).<FieldWithMetaString>mapC("getCodes", c9Holder -> c9Holder.getCodes());
			return thenArg
				.filterItemNullSafe(item -> notEqual(item.<String>map("Type coercion", fieldWithMetaString -> fieldWithMetaString == null ? null : fieldWithMetaString.getValue()), MapperS.of("void"), CardinalityOperator.Any).get());
		}
	}
}
