package test.deeppath.functions;

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
import test.deeppath.Outer;
import test.deeppath.util.OuterDeepPathUtil;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(Filtered.FilteredDefault.class)
public abstract class Filtered implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;
	
	// RosettaFunction dependencies
	//
	@Inject protected OuterDeepPathUtil outerDeepPathUtil;

	/**
	* @param outers 
	* @return kept 
	*/
	public List<? extends Outer> evaluate(List<? extends Outer> outers) {
		List<Outer.OuterBuilder> keptBuilder = doEvaluate(outers);
		
		final List<? extends Outer> kept;
		if (keptBuilder == null) {
			kept = null;
		} else {
			kept = keptBuilder.stream().map(Outer::build).collect(Collectors.toList());
			objectValidator.validate(Outer.class, kept);
		}
		
		return kept;
	}

	protected abstract List<Outer.OuterBuilder> doEvaluate(List<? extends Outer> outers);

	public static class FilteredDefault extends Filtered {
		@Override
		protected List<Outer.OuterBuilder> doEvaluate(List<? extends Outer> outers) {
			if (outers == null) {
				outers = Collections.emptyList();
			}
			List<Outer.OuterBuilder> kept = new ArrayList<>();
			return assignOutput(kept, outers);
		}
		
		protected List<Outer.OuterBuilder> assignOutput(List<Outer.OuterBuilder> kept, List<? extends Outer> outers) {
			kept.addAll(toBuilder(MapperC.<Outer>of(outers)
				.filterItemNullSafe(item -> exists(item.<String>map("chooseText", outer -> outerDeepPathUtil.chooseText(outer))).get()).getMulti()));
			
			return Optional.ofNullable(kept)
				.map(o -> o.stream().map(i -> i.prune()).collect(Collectors.toList()))
				.orElse(null);
		}
	}
}
