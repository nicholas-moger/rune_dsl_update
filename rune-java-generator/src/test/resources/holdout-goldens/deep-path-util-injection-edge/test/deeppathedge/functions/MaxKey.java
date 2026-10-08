package test.deeppathedge.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import test.deeppathedge.Outer;
import test.deeppathedge.util.OuterDeepPathUtil;


@ImplementedBy(MaxKey.MaxKeyDefault.class)
public abstract class MaxKey implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;
	
	// RosettaFunction dependencies
	//
	@Inject protected OuterDeepPathUtil outerDeepPathUtil;

	/**
	* @param outers 
	* @return top 
	*/
	public Outer evaluate(List<? extends Outer> outers) {
		Outer.OuterBuilder topBuilder = doEvaluate(outers);
		
		final Outer top;
		if (topBuilder == null) {
			top = null;
		} else {
			top = topBuilder.build();
			objectValidator.validate(Outer.class, top);
		}
		
		return top;
	}

	protected abstract Outer.OuterBuilder doEvaluate(List<? extends Outer> outers);

	public static class MaxKeyDefault extends MaxKey {
		@Override
		protected Outer.OuterBuilder doEvaluate(List<? extends Outer> outers) {
			if (outers == null) {
				outers = Collections.emptyList();
			}
			Outer.OuterBuilder top = Outer.builder();
			return assignOutput(top, outers);
		}
		
		protected Outer.OuterBuilder assignOutput(Outer.OuterBuilder top, List<? extends Outer> outers) {
			top = toBuilder(MapperC.<Outer>of(outers)
				.max(item -> item.<String>map("chooseText", outer -> outerDeepPathUtil.chooseText(outer))).get());
			
			return Optional.ofNullable(top)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
