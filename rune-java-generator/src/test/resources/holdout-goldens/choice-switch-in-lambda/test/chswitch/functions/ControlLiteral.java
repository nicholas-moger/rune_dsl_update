package test.chswitch.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.util.ArrayList;
import java.util.List;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(ControlLiteral.ControlLiteralDefault.class)
public abstract class ControlLiteral implements RosettaFunction {

	/**
	* @param raw 
	* @return texts 
	*/
	public List<String> evaluate(String raw) {
		List<String> texts = doEvaluate(raw);
		
		return texts;
	}

	protected abstract List<String> doEvaluate(String raw);

	protected abstract MapperC<String> switched(String raw);

	public static class ControlLiteralDefault extends ControlLiteral {
		@Override
		protected List<String> doEvaluate(String raw) {
			List<String> texts = new ArrayList<>();
			return assignOutput(texts, raw);
		}
		
		protected List<String> assignOutput(List<String> texts, String raw) {
			texts.addAll(switched(raw).getMulti());
			
			return texts;
		}
		
		@Override
		protected MapperC<String> switched(String raw) {
			return MapperC.<String>of(MapperS.of(raw))
				.mapItem(item -> {
					if (item.get() == null) {
						return MapperS.<String>ofNull();
					}
					if (areEqual(item, MapperS.of("r"), CardinalityOperator.All).get()) {
						return MapperS.of("Red");
					}
					if (areEqual(item, MapperS.of("g"), CardinalityOperator.All).get()) {
						return MapperS.of("Green");
					}
					return MapperS.of("Blue");
				});
		}
	}
}
