package test.mladder.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.metafields.FieldWithMetaString;
import test.mladder.Holder;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(Direct.DirectDefault.class)
public abstract class Direct implements RosettaFunction {

	/**
	* @param h 
	* @return pick 
	*/
	public String evaluate(Holder h) {
		String pick = doEvaluate(h);
		
		return pick;
	}

	protected abstract String doEvaluate(Holder h);

	public static class DirectDefault extends Direct {
		@Override
		protected String doEvaluate(Holder h) {
			String pick = null;
			return assignOutput(pick, h);
		}
		
		protected String assignOutput(String pick, Holder h) {
			final MapperC<FieldWithMetaString> thenArg0;
			if (exists(MapperS.of(h).<FieldWithMetaString>map("getCoded", holder -> holder.getCoded())).getOrDefault(false)) {
				thenArg0 = MapperC.of(MapperS.of(h).<FieldWithMetaString>map("getCoded", holder -> holder.getCoded()));
			} else {
				thenArg0 = MapperS.of(h).<FieldWithMetaString>mapC("getCodes", holder -> holder.getCodes());
			}
			final MapperC<FieldWithMetaString> thenArg1 = thenArg0
				.filterItemNullSafe(item -> notEqual(item.<String>map("Type coercion", _fieldWithMetaString -> _fieldWithMetaString == null ? null : _fieldWithMetaString.getValue()), MapperS.of("void"), CardinalityOperator.Any).get());
			final FieldWithMetaString fieldWithMetaString = thenArg1
				.first().get();
			if (fieldWithMetaString == null) {
				pick = null;
			} else {
				pick = fieldWithMetaString.getValue();
			}
			
			return pick;
		}
	}
}
