package chaos.s29.a2alias.functions;

import chaos.s29.a2alias.C29Outer;
import chaos.s29.a2alias.util.C29OuterDeepPathUtil;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.metafields.FieldWithMetaString;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C29InLambda.C29InLambdaDefault.class)
public abstract class C29InLambda implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected C29OuterDeepPathUtil c29OuterDeepPathUtil;

	/**
	* @param outers 
	* @return picks 
	*/
	public List<String> evaluate(List<? extends C29Outer> outers) {
		List<String> picks = doEvaluate(outers);
		
		return picks;
	}

	protected abstract List<String> doEvaluate(List<? extends C29Outer> outers);

	public static class C29InLambdaDefault extends C29InLambda {
		@Override
		protected List<String> doEvaluate(List<? extends C29Outer> outers) {
			if (outers == null) {
				outers = Collections.emptyList();
			}
			List<String> picks = new ArrayList<>();
			return assignOutput(picks, outers);
		}
		
		protected List<String> assignOutput(List<String> picks, List<? extends C29Outer> outers) {
			picks = MapperC.<C29Outer>of(outers)
				.mapItem(o -> {
					final MapperC<FieldWithMetaString> thenArg0;
					if (exists(o.<FieldWithMetaString>map("chooseCode", c29Outer -> c29OuterDeepPathUtil.chooseCode(c29Outer))).getOrDefault(false)) {
						thenArg0 = MapperC.of(o.<FieldWithMetaString>map("chooseCode", c29Outer -> c29OuterDeepPathUtil.chooseCode(c29Outer)));
					} else {
						thenArg0 = o.<FieldWithMetaString>mapC("chooseCodes", c29Outer -> c29OuterDeepPathUtil.chooseCodes(c29Outer));
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
