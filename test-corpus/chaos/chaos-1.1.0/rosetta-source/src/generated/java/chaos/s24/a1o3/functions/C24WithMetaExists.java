package chaos.s24.a1o3.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.metafields.ReferenceWithMetaVoid;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C24WithMetaExists.C24WithMetaExistsDefault.class)
public abstract class C24WithMetaExists implements RosettaFunction {

	/**
	* @param href 
	* @return r 
	*/
	public Boolean evaluate(String href) {
		Boolean r = doEvaluate(href);
		
		return r;
	}

	protected abstract Boolean doEvaluate(String href);

	public static class C24WithMetaExistsDefault extends C24WithMetaExists {
		@Override
		protected Boolean doEvaluate(String href) {
			Boolean r = null;
			return assignOutput(r, href);
		}
		
		protected Boolean assignOutput(Boolean r, String href) {
			r = exists(MapperS.of(ReferenceWithMetaVoid.builder().setValue(null).setExternalReference(href).build())).get();
			
			return r;
		}
	}
}
