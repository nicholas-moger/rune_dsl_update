package chaos.s02.a3hub.p2.functions;

import chaos.s02.a3hub.p2.C2DirEnum;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C2Flip.C2FlipDefault.class)
public abstract class C2Flip implements RosettaFunction {

	/**
	* @param d 
	* @return r 
	*/
	public C2DirEnum evaluate(C2DirEnum d) {
		C2DirEnum r = doEvaluate(d);
		
		return r;
	}

	protected abstract C2DirEnum doEvaluate(C2DirEnum d);

	public static class C2FlipDefault extends C2Flip {
		@Override
		protected C2DirEnum doEvaluate(C2DirEnum d) {
			C2DirEnum r = null;
			return assignOutput(r, d);
		}
		
		protected C2DirEnum assignOutput(C2DirEnum r, C2DirEnum d) {
			if (areEqual(MapperS.of(d), MapperS.of(C2DirEnum.BUY), CardinalityOperator.All).getOrDefault(false)) {
				r = C2DirEnum.SELL;
			} else if (areEqual(MapperS.of(d), MapperS.of(C2DirEnum.SELL), CardinalityOperator.All).getOrDefault(false)) {
				r = C2DirEnum.BUY;
			} else {
				r = C2DirEnum.HOLD;
			}
			
			return r;
		}
	}
}
