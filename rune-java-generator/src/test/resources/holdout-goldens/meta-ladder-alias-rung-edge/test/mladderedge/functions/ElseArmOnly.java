package test.mladderedge.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.metafields.FieldWithMetaString;
import test.mladderedge.Holder;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(ElseArmOnly.ElseArmOnlyDefault.class)
public abstract class ElseArmOnly implements RosettaFunction {

	/**
	* @param h 
	* @param p 
	* @return pick 
	*/
	public String evaluate(Holder h, String p) {
		String pick = doEvaluate(h, p);
		
		return pick;
	}

	protected abstract String doEvaluate(Holder h, String p);

	protected abstract MapperC<? extends FieldWithMetaString> goodCodes(Holder h, String p);

	public static class ElseArmOnlyDefault extends ElseArmOnly {
		@Override
		protected String doEvaluate(Holder h, String p) {
			String pick = null;
			return assignOutput(pick, h, p);
		}
		
		protected String assignOutput(String pick, Holder h, String p) {
			if (exists(MapperS.of(p)).getOrDefault(false)) {
				pick = p;
			} else {
				final FieldWithMetaString fieldWithMetaString = goodCodes(h, p)
					.first().get();
				if (fieldWithMetaString == null) {
					pick = null;
				} else {
					pick = fieldWithMetaString.getValue();
				}
			}
			
			return pick;
		}
		
		@Override
		protected MapperC<? extends FieldWithMetaString> goodCodes(Holder h, String p) {
			final MapperC<FieldWithMetaString> thenArg = MapperS.of(h).<FieldWithMetaString>mapC("getCodes", holder -> holder.getCodes());
			return thenArg
				.filterItemNullSafe(item -> notEqual(item.<String>map("Type coercion", fieldWithMetaString -> fieldWithMetaString == null ? null : fieldWithMetaString.getValue()), MapperS.of("void"), CardinalityOperator.Any).get());
		}
	}
}
