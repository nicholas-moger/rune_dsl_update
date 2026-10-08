package chaos.s24.a3hub.p2.functions;

import chaos.s24.a3hub.p2.C24Carrier;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C24Compare.C24CompareDefault.class)
public abstract class C24Compare implements RosettaFunction {

	/**
	* @param c 
	* @param d 
	* @return r 
	*/
	public Boolean evaluate(C24Carrier c, C24Carrier d) {
		Boolean r = doEvaluate(c, d);
		
		return r;
	}

	protected abstract Boolean doEvaluate(C24Carrier c, C24Carrier d);

	public static class C24CompareDefault extends C24Compare {
		@Override
		protected Boolean doEvaluate(C24Carrier c, C24Carrier d) {
			Boolean r = null;
			return assignOutput(r, c, d);
		}
		
		protected Boolean assignOutput(Boolean r, C24Carrier c, C24Carrier d) {
			r = areEqual(MapperS.<Void>ofNull(), MapperS.<Void>ofNull(), CardinalityOperator.All).orNullSafe(notEqual(MapperS.<Void>ofNull(), MapperS.<Void>ofNull(), CardinalityOperator.Any)).orNullSafe(exists(MapperS.<Void>ofNull()).andNullSafe(notExists(MapperS.<Void>ofNull()))).get();
			
			return r;
		}
	}
}
