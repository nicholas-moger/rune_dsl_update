package holdout.extractmetaelemdereffunction.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.metafields.FieldWithMetaString;
import holdout.extractmetaelemdereffunction.Outer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@ImplementedBy(ExtractMetaNavBody.ExtractMetaNavBodyDefault.class)
public abstract class ExtractMetaNavBody implements RosettaFunction {

	/**
	* @param outers 
	* @return picks 
	*/
	public List<String> evaluate(List<? extends Outer> outers) {
		List<String> picks = doEvaluate(outers);
		
		return picks;
	}

	protected abstract List<String> doEvaluate(List<? extends Outer> outers);

	public static class ExtractMetaNavBodyDefault extends ExtractMetaNavBody {
		@Override
		protected List<String> doEvaluate(List<? extends Outer> outers) {
			if (outers == null) {
				outers = Collections.emptyList();
			}
			List<String> picks = new ArrayList<>();
			return assignOutput(picks, outers);
		}
		
		protected List<String> assignOutput(List<String> picks, List<? extends Outer> outers) {
			picks = MapperC.<Outer>of(outers)
				.mapItem(o -> o.<FieldWithMetaString>map("getCode", outer -> outer.getCode())).<String>map("Type coercion", fieldWithMetaString -> fieldWithMetaString.getValue()).getMulti();
			
			return picks;
		}
	}
}
