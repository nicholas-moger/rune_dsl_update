package test.mladder.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.metafields.FieldWithMetaString;
import test.mladder.Holder;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(TopLevel.TopLevelDefault.class)
public abstract class TopLevel implements RosettaFunction {

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

	public static class TopLevelDefault extends TopLevel {
		@Override
		protected String doEvaluate(Holder h) {
			String pick = null;
			return assignOutput(pick, h);
		}
		
		protected String assignOutput(String pick, Holder h) {
			final FieldWithMetaString fieldWithMetaString = goodCodes(h)
				.first().get();
			if (fieldWithMetaString == null) {
				pick = null;
			} else {
				pick = fieldWithMetaString.getValue();
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
