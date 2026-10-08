package test.chswitchbare.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import test.chswitchbare.OptA;
import test.chswitchbare.Outer2;


@ImplementedBy(PickItems.PickItemsDefault.class)
public abstract class PickItems implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param outers 
	* @return outs 
	*/
	public List<? extends OptA> evaluate(List<? extends Outer2> outers) {
		List<OptA.OptABuilder> outsBuilder = doEvaluate(outers);
		
		final List<? extends OptA> outs;
		if (outsBuilder == null) {
			outs = null;
		} else {
			outs = outsBuilder.stream().map(OptA::build).collect(Collectors.toList());
			objectValidator.validate(OptA.class, outs);
		}
		
		return outs;
	}

	protected abstract List<OptA.OptABuilder> doEvaluate(List<? extends Outer2> outers);

	public static class PickItemsDefault extends PickItems {
		@Override
		protected List<OptA.OptABuilder> doEvaluate(List<? extends Outer2> outers) {
			if (outers == null) {
				outers = Collections.emptyList();
			}
			List<OptA.OptABuilder> outs = new ArrayList<>();
			return assignOutput(outs, outers);
		}
		
		protected List<OptA.OptABuilder> assignOutput(List<OptA.OptABuilder> outs, List<? extends Outer2> outers) {
			outs = toBuilder(MapperC.<Outer2>of(outers)
				.mapItem(item -> {
					if (item.get() == null) {
						return MapperS.<OptA>ofNull();
					}
					if (item.<OptA>map("getOptA", outer2 -> outer2.getOptA()).get() != null) {
						final MapperS<OptA> optA = item.<OptA>map("getOptA", outer2 -> outer2.getOptA());
						return optA;
					}
					return MapperS.<OptA>ofNull();
				}).getMulti());
			
			return Optional.ofNullable(outs)
				.map(o -> o.stream().map(i -> i.prune()).collect(Collectors.toList()))
				.orElse(null);
		}
	}
}
