package chaos.s17.a2alias.functions;

import chaos.s17.a2alias.C17SideEnum;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C17Sift.C17SiftDefault.class)
public abstract class C17Sift implements RosettaFunction {

	/**
	* @param sides 
	* @return buys 
	*/
	public List<C17SideEnum> evaluate(List<C17SideEnum> sides) {
		List<C17SideEnum> buys = doEvaluate(sides);
		
		return buys;
	}

	protected abstract List<C17SideEnum> doEvaluate(List<C17SideEnum> sides);

	public static class C17SiftDefault extends C17Sift {
		@Override
		protected List<C17SideEnum> doEvaluate(List<C17SideEnum> sides) {
			if (sides == null) {
				sides = Collections.emptyList();
			}
			List<C17SideEnum> buys = new ArrayList<>();
			return assignOutput(buys, sides);
		}
		
		protected List<C17SideEnum> assignOutput(List<C17SideEnum> buys, List<C17SideEnum> sides) {
			buys.addAll(MapperC.<C17SideEnum>of(sides)
				.filterItemNullSafe(item -> areEqual(item, MapperS.of(C17SideEnum.BUY), CardinalityOperator.All).get()).getMulti());
			
			buys.addAll(MapperC.<C17SideEnum>of(sides)
				.filterItemNullSafe(item -> areEqual(item, MapperS.of(C17SideEnum.SELL), CardinalityOperator.All).get()).getMulti());
			
			return buys;
		}
	}
}
