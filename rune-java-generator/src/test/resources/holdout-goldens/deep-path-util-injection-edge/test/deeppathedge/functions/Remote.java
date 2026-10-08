package test.deeppathedge.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import test.deeppathedgetypes.ROuter;
import test.deeppathedgetypes.util.ROuterDeepPathUtil;


@ImplementedBy(Remote.RemoteDefault.class)
public abstract class Remote implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected ROuterDeepPathUtil rOuterDeepPathUtil;

	/**
	* @param rs 
	* @return texts 
	*/
	public List<String> evaluate(List<? extends ROuter> rs) {
		List<String> texts = doEvaluate(rs);
		
		return texts;
	}

	protected abstract List<String> doEvaluate(List<? extends ROuter> rs);

	public static class RemoteDefault extends Remote {
		@Override
		protected List<String> doEvaluate(List<? extends ROuter> rs) {
			if (rs == null) {
				rs = Collections.emptyList();
			}
			List<String> texts = new ArrayList<>();
			return assignOutput(texts, rs);
		}
		
		protected List<String> assignOutput(List<String> texts, List<? extends ROuter> rs) {
			texts.addAll(MapperC.<ROuter>of(rs)
				.mapItem(item -> item.<String>map("chooseText", rOuter -> rOuterDeepPathUtil.chooseText(rOuter))).getMulti());
			
			return texts;
		}
	}
}
