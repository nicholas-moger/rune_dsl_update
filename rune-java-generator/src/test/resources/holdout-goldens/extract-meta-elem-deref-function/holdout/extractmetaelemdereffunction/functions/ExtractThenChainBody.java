package holdout.extractmetaelemdereffunction.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.metafields.FieldWithMetaString;
import holdout.extractmetaelemdereffunction.Outer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(ExtractThenChainBody.ExtractThenChainBodyDefault.class)
public abstract class ExtractThenChainBody implements RosettaFunction {

	/**
	* @param outers 
	* @return picks 
	*/
	public List<String> evaluate(List<? extends Outer> outers) {
		List<String> picks = doEvaluate(outers);
		
		return picks;
	}

	protected abstract List<String> doEvaluate(List<? extends Outer> outers);

	public static class ExtractThenChainBodyDefault extends ExtractThenChainBody {
		@Override
		protected List<String> doEvaluate(List<? extends Outer> outers) {
			if (outers == null) {
				outers = Collections.emptyList();
			}
			List<String> picks = new ArrayList<>();
			return assignOutput(picks, outers);
		}
		
		protected List<String> assignOutput(List<String> picks, List<? extends Outer> outers) {
			picks = MapperC.<Outer>of(outers)
				.mapItem(o -> {
					final MapperC<FieldWithMetaString> thenArg0 = o.<FieldWithMetaString>mapC("getCodes", outer -> outer.getCodes());
					final MapperC<FieldWithMetaString> thenArg1 = thenArg0
						.filterItemNullSafe(c -> notEqual(c.<String>map("Type coercion", fieldWithMetaString -> fieldWithMetaString == null ? null : fieldWithMetaString.getValue()), MapperS.of("void"), CardinalityOperator.Any).get());
					return thenArg1
						.first();
				}).<String>map("Type coercion", fieldWithMetaString -> fieldWithMetaString.getValue()).getMulti();
			
			return picks;
		}
	}
}
