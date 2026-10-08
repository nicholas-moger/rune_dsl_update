package test.mladder.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.metafields.FieldWithMetaString;
import test.mladder.Holder;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(ThreeRungs.ThreeRungsDefault.class)
public abstract class ThreeRungs implements RosettaFunction {

	/**
	* @param h 
	* @param g 
	* @return pick 
	*/
	public String evaluate(Holder h, Holder g) {
		String pick = doEvaluate(h, g);
		
		return pick;
	}

	protected abstract String doEvaluate(Holder h, Holder g);

	protected abstract MapperC<? extends FieldWithMetaString> goodCodes(Holder h, Holder g);

	public static class ThreeRungsDefault extends ThreeRungs {
		@Override
		protected String doEvaluate(Holder h, Holder g) {
			String pick = null;
			return assignOutput(pick, h, g);
		}
		
		protected String assignOutput(String pick, Holder h, Holder g) {
			if (exists(MapperS.of(h).<FieldWithMetaString>map("getCoded", holder -> holder.getCoded())).getOrDefault(false)) {
				final FieldWithMetaString fieldWithMetaString0 = MapperS.of(h).<FieldWithMetaString>map("getCoded", holder -> holder.getCoded()).get();
				if (fieldWithMetaString0 == null) {
					pick = null;
				} else {
					pick = fieldWithMetaString0.getValue();
				}
			} else if (exists(goodCodes(h, g)).getOrDefault(false)) {
				final FieldWithMetaString fieldWithMetaString1 = goodCodes(h, g)
					.first().get();
				if (fieldWithMetaString1 == null) {
					pick = null;
				} else {
					pick = fieldWithMetaString1.getValue();
				}
			} else {
				final FieldWithMetaString fieldWithMetaString2 = MapperS.of(g).<FieldWithMetaString>map("getCoded", holder -> holder.getCoded()).get();
				if (fieldWithMetaString2 == null) {
					pick = null;
				} else {
					pick = fieldWithMetaString2.getValue();
				}
			}
			
			return pick;
		}
		
		@Override
		protected MapperC<? extends FieldWithMetaString> goodCodes(Holder h, Holder g) {
			final MapperC<FieldWithMetaString> thenArg = MapperS.of(h).<FieldWithMetaString>mapC("getCodes", holder -> holder.getCodes());
			return thenArg
				.filterItemNullSafe(item -> notEqual(item.<String>map("Type coercion", fieldWithMetaString -> fieldWithMetaString == null ? null : fieldWithMetaString.getValue()), MapperS.of("void"), CardinalityOperator.Any).get());
		}
	}
}
