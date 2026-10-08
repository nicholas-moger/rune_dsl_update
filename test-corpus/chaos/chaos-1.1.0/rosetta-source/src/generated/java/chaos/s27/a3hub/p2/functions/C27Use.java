package chaos.s27.a3hub.p2.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;


@ImplementedBy(C27Use.C27UseDefault.class)
public abstract class C27Use implements RosettaFunction {

	/**
	* @param n 
	* @param l 
	* @return r 
	*/
	public Integer evaluate(Integer n, Integer l) {
		Integer r = doEvaluate(n, l);
		
		return r;
	}

	protected abstract Integer doEvaluate(Integer n, Integer l);

	protected abstract MapperS<Integer> doubled(Integer n, Integer l);

	public static class C27UseDefault extends C27Use {
		@Override
		protected Integer doEvaluate(Integer n, Integer l) {
			Integer r = null;
			return assignOutput(r, n, l);
		}
		
		protected Integer assignOutput(Integer r, Integer n, Integer l) {
			r = MapperMaths.<Integer, Integer, Integer>add(doubled(n, l), MapperS.of(MapperS.of(l).getOrDefault(0))).get();
			
			return r;
		}
		
		@Override
		protected MapperS<Integer> doubled(Integer n, Integer l) {
			return MapperMaths.<Integer, Integer, Integer>multiply(MapperS.of(n), MapperS.of(2));
		}
	}
}
