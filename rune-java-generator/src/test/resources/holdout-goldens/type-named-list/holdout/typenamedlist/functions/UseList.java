package holdout.typenamedlist.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@ImplementedBy(UseList.UseListDefault.class)
public abstract class UseList implements RosettaFunction {

	/**
	* @param l 
	* @param ss 
	* @return out 
	*/
	public List<String> evaluate(holdout.typenamedlist.List l, List<String> ss) {
		List<String> out = doEvaluate(l, ss);
		
		return out;
	}

	protected abstract List<String> doEvaluate(holdout.typenamedlist.List l, List<String> ss);

	public static class UseListDefault extends UseList {
		@Override
		protected List<String> doEvaluate(holdout.typenamedlist.List l, List<String> ss) {
			if (ss == null) {
				ss = Collections.emptyList();
			}
			List<String> out = new ArrayList<>();
			return assignOutput(out, l, ss);
		}
		
		protected List<String> assignOutput(List<String> out, holdout.typenamedlist.List l, List<String> ss) {
			out.addAll(MapperS.of(l).<String>mapC("getItems", list -> list.getItems()).getMulti());
			
			out.addAll(ss);
			
			return out;
		}
	}
}
