package test.wmwrap.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.metafields.FieldWithMetaString;
import test.wmwrap.Holder;


@ImplementedBy(WrappedToPlainOutput.WrappedToPlainOutputDefault.class)
public abstract class WrappedToPlainOutput implements RosettaFunction {

	/**
	* @param h 
	* @return c 
	*/
	public String evaluate(Holder h) {
		String c = doEvaluate(h);
		
		return c;
	}

	protected abstract String doEvaluate(Holder h);

	public static class WrappedToPlainOutputDefault extends WrappedToPlainOutput {
		@Override
		protected String doEvaluate(Holder h) {
			String c = null;
			return assignOutput(c, h);
		}
		
		protected String assignOutput(String c, Holder h) {
			final FieldWithMetaString.FieldWithMetaStringBuilder withMetaArgument = MapperS.of(h).<FieldWithMetaString>map("getCoded", holder -> holder.getCoded()).get() == null ? null : MapperS.of(h).<FieldWithMetaString>map("getCoded", holder -> holder.getCoded()).get().toBuilder();
			withMetaArgument.getOrCreateMeta().setScheme("oracle-scheme");
			if (withMetaArgument == null) {
				c = null;
			} else {
				c = withMetaArgument.getValue();
			}
			
			return c;
		}
	}
}
