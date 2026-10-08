package test.mladderedge.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.metafields.FieldWithMetaString;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import test.mladderedge.Holder;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(InLambda.InLambdaDefault.class)
public abstract class InLambda implements RosettaFunction {

	/**
	* @param hs 
	* @return picks 
	*/
	public List<String> evaluate(List<? extends Holder> hs) {
		List<String> picks = doEvaluate(hs);
		
		return picks;
	}

	protected abstract List<String> doEvaluate(List<? extends Holder> hs);

	public static class InLambdaDefault extends InLambda {
		@Override
		protected List<String> doEvaluate(List<? extends Holder> hs) {
			if (hs == null) {
				hs = Collections.emptyList();
			}
			List<String> picks = new ArrayList<>();
			return assignOutput(picks, hs);
		}
		
		protected List<String> assignOutput(List<String> picks, List<? extends Holder> hs) {
			picks = MapperC.<Holder>of(hs)
				.mapItem(h -> {
					final MapperC<FieldWithMetaString> thenArg0;
					if (exists(h.<FieldWithMetaString>map("getCoded", holder -> holder.getCoded())).getOrDefault(false)) {
						thenArg0 = MapperC.of(h.<FieldWithMetaString>map("getCoded", holder -> holder.getCoded()));
					} else {
						thenArg0 = h.<FieldWithMetaString>mapC("getCodes", holder -> holder.getCodes());
					}
					final MapperC<FieldWithMetaString> thenArg1 = thenArg0
						.filterItemNullSafe(c -> notEqual(c.<String>map("Type coercion", fieldWithMetaString -> fieldWithMetaString == null ? null : fieldWithMetaString.getValue()), MapperS.of("void"), CardinalityOperator.Any).get());
					return thenArg1
						.first();
				}).<String>map("Type coercion", fieldWithMetaString -> fieldWithMetaString.getValue()).getMulti();
			
			return picks;
		}
	}
}
