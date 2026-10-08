package test.mladder.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.metafields.FieldWithMetaString;
import test.mladder.Holder;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(NoFilter.NoFilterDefault.class)
public abstract class NoFilter implements RosettaFunction {

	/**
	* @param h 
	* @return pick 
	*/
	public String evaluate(Holder h) {
		String pick = doEvaluate(h);
		
		return pick;
	}

	protected abstract String doEvaluate(Holder h);

	protected abstract MapperC<? extends FieldWithMetaString> allCodes(Holder h);

	public static class NoFilterDefault extends NoFilter {
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
			} else {
				final FieldWithMetaString fieldWithMetaString1 = allCodes(h)
					.first().get();
				if (fieldWithMetaString1 == null) {
					pick = null;
				} else {
					pick = fieldWithMetaString1.getValue();
				}
			}
			
			return pick;
		}
		
		@Override
		protected MapperC<? extends FieldWithMetaString> allCodes(Holder h) {
			return MapperS.of(h).<FieldWithMetaString>mapC("getCodes", holder -> holder.getCodes());
		}
	}
}
