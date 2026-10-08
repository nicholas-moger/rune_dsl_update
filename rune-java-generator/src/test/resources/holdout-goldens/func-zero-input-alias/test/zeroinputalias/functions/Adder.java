package test.zeroinputalias.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import javax.inject.Inject;


@ImplementedBy(Adder.AdderDefault.class)
public abstract class Adder implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected AddOne addOne;

	/**
	* @return res 
	*/
	public Integer evaluate() {
		Integer res = doEvaluate();
		
		return res;
	}

	protected abstract Integer doEvaluate();

	protected abstract MapperS<Integer> arg1();

	public static class AdderDefault extends Adder {
		@Override
		protected Integer doEvaluate() {
			Integer res = null;
			return assignOutput(res);
		}
		
		protected Integer assignOutput(Integer res) {
			res = arg1().get();
			
			return res;
		}
		
		@Override
		protected MapperS<Integer> arg1() {
			return MapperS.of(addOne.evaluate(1));
		}
	}
}
