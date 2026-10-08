package holdout.listliteraladditemcoerce.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.util.ArrayList;
import java.util.List;


@ImplementedBy(AddIntListToInt.AddIntListToIntDefault.class)
public abstract class AddIntListToInt implements RosettaFunction {

	/**
	* @return rs 
	*/
	public List<Integer> evaluate() {
		List<Integer> rs = doEvaluate();
		
		return rs;
	}

	protected abstract List<Integer> doEvaluate();

	public static class AddIntListToIntDefault extends AddIntListToInt {
		@Override
		protected List<Integer> doEvaluate() {
			List<Integer> rs = new ArrayList<>();
			return assignOutput(rs);
		}
		
		protected List<Integer> assignOutput(List<Integer> rs) {
			rs.addAll(MapperC.<Integer>of(MapperS.of(1), MapperS.of(2), MapperS.of(3)).getMulti());
			
			return rs;
		}
	}
}
