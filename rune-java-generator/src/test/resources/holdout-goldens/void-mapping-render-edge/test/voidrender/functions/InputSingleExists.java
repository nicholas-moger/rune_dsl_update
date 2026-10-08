package test.voidrender.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.util.Collections;
import java.util.List;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(InputSingleExists.InputSingleExistsDefault.class)
public abstract class InputSingleExists implements RosettaFunction {

	/**
	* @param ts 
	* @return r 
	*/
	public Boolean evaluate(List<Void> ts) {
		Boolean r = doEvaluate(ts);
		
		return r;
	}

	protected abstract Boolean doEvaluate(List<Void> ts);

	public static class InputSingleExistsDefault extends InputSingleExists {
		@Override
		protected Boolean doEvaluate(List<Void> ts) {
			if (ts == null) {
				ts = Collections.emptyList();
			}
			Boolean r = null;
			return assignOutput(r, ts);
		}
		
		protected Boolean assignOutput(Boolean r, List<Void> ts) {
			r = singleExists(MapperS.<Void>ofNull()).get();
			
			return r;
		}
	}
}
