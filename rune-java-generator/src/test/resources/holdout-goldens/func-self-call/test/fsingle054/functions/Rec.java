package test.fsingle054.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import javax.inject.Inject;


@ImplementedBy(Rec.RecDefault.class)
public abstract class Rec implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected Rec rec;

	/**
	* @return result 
	*/
	public Integer evaluate() {
		Integer result = doEvaluate();
		
		return result;
	}

	protected abstract Integer doEvaluate();

	protected abstract MapperS<Integer> test();

	public static class RecDefault extends Rec {
		@Override
		protected Integer doEvaluate() {
			Integer result = null;
			return assignOutput(result);
		}
		
		protected Integer assignOutput(Integer result) {
			result = rec.evaluate();
			
			return result;
		}
		
		@Override
		protected MapperS<Integer> test() {
			return MapperS.of(rec.evaluate());
		}
	}
}
