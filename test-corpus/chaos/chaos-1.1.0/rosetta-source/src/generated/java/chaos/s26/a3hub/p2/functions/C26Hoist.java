package chaos.s26.a3hub.p2.functions;

import chaos.s26.a3hub.p2.C26KindEnum;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C26Hoist.C26HoistDefault.class)
public abstract class C26Hoist implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected C26Name c26Name;

	/**
	* @param raws 
	* @param k 
	* @return outs 
	*/
	public List<String> evaluate(List<String> raws, C26KindEnum k) {
		List<String> outs = doEvaluate(raws, k);
		
		return outs;
	}

	protected abstract List<String> doEvaluate(List<String> raws, C26KindEnum k);

	public static class C26HoistDefault extends C26Hoist {
		@Override
		protected List<String> doEvaluate(List<String> raws, C26KindEnum k) {
			if (raws == null) {
				raws = Collections.emptyList();
			}
			List<String> outs = new ArrayList<>();
			return assignOutput(outs, raws, k);
		}
		
		protected List<String> assignOutput(List<String> outs, List<String> raws, C26KindEnum k) {
			final MapperC<String> thenArg0 = MapperC.<String>of(raws);
			final MapperC<String> thenArg1 = thenArg0
				.mapItem(item -> {
					if (item.get() == null) {
						return MapperS.<String>ofNull();
					}
					if (areEqual(item, MapperS.of("a"), CardinalityOperator.All).get()) {
						return MapperS.of("A");
					}
					return item;
				});
			outs.addAll(distinct(thenArg1).getMulti());
			
			outs.addAll(MapperC.<C26KindEnum>of(MapperS.of(k))
				.mapItem(item -> {
					final C26KindEnum switchArgument = item.get();
					if (switchArgument == null) {
						return MapperS.<String>ofNull();
					}
					if (switchArgument == C26KindEnum.RED) {
						return MapperS.of(c26Name.evaluate(item.get()));
					}
					if (switchArgument == C26KindEnum.GREEN) {
						return item.map("to-string", C26KindEnum::toDisplayString);
					}
					return MapperS.of("d");
				}).getMulti());
			
			outs.addAll(MapperC.<C26KindEnum>of(MapperS.of(k))
				.mapItem(item -> {
					final C26KindEnum switchArgument = item.get();
					if (switchArgument == null) {
						return MapperS.<String>ofNull();
					}
					if (switchArgument == C26KindEnum.RED) {
						return item.map("to-string", C26KindEnum::toDisplayString);
					}
					return MapperS.of(MapperS.of(c26Name.evaluate(item.get())).getOrDefault("q"));
				}).getMulti());
			
			outs.addAll(MapperC.<C26KindEnum>of(MapperS.of(k))
				.mapItem(item -> {
					final C26KindEnum switchArgument = item.get();
					if (switchArgument == null) {
						return MapperS.<String>ofNull();
					}
					if (switchArgument == C26KindEnum.RED) {
						return MapperS.of("r");
					}
					if (switchArgument == C26KindEnum.GREEN) {
						return MapperS.of("g");
					}
					if (switchArgument == C26KindEnum.BLUE) {
						return MapperS.of("b");
					}
					return MapperS.<String>ofNull();
				}).getMulti());
			
			return outs;
		}
	}
}
