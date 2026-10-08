package test.deeppathedge.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import test.deeppathedge.Outer;
import test.deeppathedge.util.OuterDeepPathUtil;


@ImplementedBy(AliasItem.AliasItemDefault.class)
public abstract class AliasItem implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected OuterDeepPathUtil outerDeepPathUtil;

	/**
	* @param outers 
	* @return texts 
	*/
	public List<String> evaluate(List<? extends Outer> outers) {
		List<String> texts = doEvaluate(outers);
		
		return texts;
	}

	protected abstract List<String> doEvaluate(List<? extends Outer> outers);

	protected abstract MapperC<String> picked(List<? extends Outer> outers);

	public static class AliasItemDefault extends AliasItem {
		@Override
		protected List<String> doEvaluate(List<? extends Outer> outers) {
			if (outers == null) {
				outers = Collections.emptyList();
			}
			List<String> texts = new ArrayList<>();
			return assignOutput(texts, outers);
		}
		
		protected List<String> assignOutput(List<String> texts, List<? extends Outer> outers) {
			texts.addAll(picked(outers).getMulti());
			
			return texts;
		}
		
		@Override
		protected MapperC<String> picked(List<? extends Outer> outers) {
			return MapperC.<Outer>of(outers)
				.mapItem(item -> item.<String>map("chooseText", outer -> outerDeepPathUtil.chooseText(outer)));
		}
	}
}
