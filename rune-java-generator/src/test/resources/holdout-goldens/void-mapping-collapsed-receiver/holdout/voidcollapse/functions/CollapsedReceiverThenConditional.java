package holdout.voidcollapse.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import holdout.voidcollapse.Carrier;
import holdout.voidcollapse.Pair;
import java.util.Optional;
import javax.inject.Inject;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(CollapsedReceiverThenConditional.CollapsedReceiverThenConditionalDefault.class)
public abstract class CollapsedReceiverThenConditional implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param flag 
	* @param c1 
	* @param c2 
	* @param a 
	* @param b 
	* @return p 
	*/
	public Pair evaluate(Boolean flag, Carrier c1, Carrier c2, String a, String b) {
		Pair.PairBuilder pBuilder = doEvaluate(flag, c1, c2, a, b);
		
		final Pair p;
		if (pBuilder == null) {
			p = null;
		} else {
			p = pBuilder.build();
			objectValidator.validate(Pair.class, p);
		}
		
		return p;
	}

	protected abstract Pair.PairBuilder doEvaluate(Boolean flag, Carrier c1, Carrier c2, String a, String b);

	public static class CollapsedReceiverThenConditionalDefault extends CollapsedReceiverThenConditional {
		@Override
		protected Pair.PairBuilder doEvaluate(Boolean flag, Carrier c1, Carrier c2, String a, String b) {
			Pair.PairBuilder p = Pair.builder();
			return assignOutput(p, flag, c1, c2, a, b);
		}
		
		protected Pair.PairBuilder assignOutput(Pair.PairBuilder p, Boolean flag, Carrier c1, Carrier c2, String a, String b) {
			p
				.setV(null);
			
			String ifThenElseResult1 = "y";
			if (areEqual(MapperS.of(a), MapperS.of(b), CardinalityOperator.All).getOrDefault(false)) {
				ifThenElseResult1 = "x";
			}
			p
				.setS(ifThenElseResult1);
			
			return Optional.ofNullable(p)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
