package test.chswitch.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import test.chswitch.KindEnum;


@ImplementedBy(ControlEnum.ControlEnumDefault.class)
public abstract class ControlEnum implements RosettaFunction {

	/**
	* @param kinds 
	* @return hexes 
	*/
	public List<String> evaluate(List<KindEnum> kinds) {
		List<String> hexes = doEvaluate(kinds);
		
		return hexes;
	}

	protected abstract List<String> doEvaluate(List<KindEnum> kinds);

	public static class ControlEnumDefault extends ControlEnum {
		@Override
		protected List<String> doEvaluate(List<KindEnum> kinds) {
			if (kinds == null) {
				kinds = Collections.emptyList();
			}
			List<String> hexes = new ArrayList<>();
			return assignOutput(hexes, kinds);
		}
		
		protected List<String> assignOutput(List<String> hexes, List<KindEnum> kinds) {
			hexes.addAll(MapperC.<KindEnum>of(kinds)
				.mapItem(item -> {
					final KindEnum switchArgument = item.get();
					if (switchArgument == null) {
						return MapperS.<String>ofNull();
					}
					if (switchArgument == KindEnum.RED) {
						return MapperS.of("ff0000");
					}
					if (switchArgument == KindEnum.GREEN) {
						return MapperS.of("00ff00");
					}
					return MapperS.of("0000ff");
				}).getMulti());
			
			return hexes;
		}
	}
}
