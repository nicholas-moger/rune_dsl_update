package test.deeppathedge.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import test.deeppathedge.Outer;
import test.deeppathedge.util.OuterDeepPathUtil;


@ImplementedBy(Sorted.SortedDefault.class)
public abstract class Sorted implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;
	
	// RosettaFunction dependencies
	//
	@Inject protected OuterDeepPathUtil outerDeepPathUtil;

	/**
	* @param outers 
	* @return sorted 
	*/
	public List<? extends Outer> evaluate(List<? extends Outer> outers) {
		List<Outer.OuterBuilder> sortedBuilder = doEvaluate(outers);
		
		final List<? extends Outer> sorted;
		if (sortedBuilder == null) {
			sorted = null;
		} else {
			sorted = sortedBuilder.stream().map(Outer::build).collect(Collectors.toList());
			objectValidator.validate(Outer.class, sorted);
		}
		
		return sorted;
	}

	protected abstract List<Outer.OuterBuilder> doEvaluate(List<? extends Outer> outers);

	public static class SortedDefault extends Sorted {
		@Override
		protected List<Outer.OuterBuilder> doEvaluate(List<? extends Outer> outers) {
			if (outers == null) {
				outers = Collections.emptyList();
			}
			List<Outer.OuterBuilder> sorted = new ArrayList<>();
			return assignOutput(sorted, outers);
		}
		
		protected List<Outer.OuterBuilder> assignOutput(List<Outer.OuterBuilder> sorted, List<? extends Outer> outers) {
			sorted.addAll(toBuilder(MapperC.<Outer>of(outers)
				.sort(item -> item.<String>map("chooseText", outer -> outerDeepPathUtil.chooseText(outer))).getMulti()));
			
			return Optional.ofNullable(sorted)
				.map(o -> o.stream().map(i -> i.prune()).collect(Collectors.toList()))
				.orElse(null);
		}
	}
}
