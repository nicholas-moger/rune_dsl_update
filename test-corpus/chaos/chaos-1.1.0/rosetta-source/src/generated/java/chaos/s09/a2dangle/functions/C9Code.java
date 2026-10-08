package chaos.s09.a2dangle.functions;

import chaos.s09.a2dangle.C9Holder;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.metafields.FieldWithMetaString;


@ImplementedBy(C9Code.C9CodeDefault.class)
public abstract class C9Code implements RosettaFunction {

	/**
	* @param h 
	* @return c 
	*/
	public String evaluate(C9Holder h) {
		String c = doEvaluate(h);
		
		return c;
	}

	protected abstract String doEvaluate(C9Holder h);

	public static class C9CodeDefault extends C9Code {
		@Override
		protected String doEvaluate(C9Holder h) {
			String c = null;
			return assignOutput(c, h);
		}
		
		protected String assignOutput(String c, C9Holder h) {
			final FieldWithMetaString fieldWithMetaString = MapperS.of(h).<FieldWithMetaString>map("getCoded", c9Holder -> c9Holder.getCoded()).get();
			if (fieldWithMetaString == null) {
				c = null;
			} else {
				c = fieldWithMetaString.getValue();
			}
			
			return c;
		}
	}
}
