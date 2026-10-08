package holdout.voidexistsclean.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import holdout.voidexistsclean.Pair;
import java.util.Optional;
import javax.inject.Inject;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(ExistsThenCleanConditional.ExistsThenCleanConditionalDefault.class)
public abstract class ExistsThenCleanConditional implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param flag 
	* @param t 
	* @param u 
	* @param a 
	* @param b 
	* @return p 
	*/
	public Pair evaluate(Boolean flag, Void t, Void u, String a, String b) {
		Pair.PairBuilder pBuilder = doEvaluate(flag, t, u, a, b);
		
		final Pair p;
		if (pBuilder == null) {
			p = null;
		} else {
			p = pBuilder.build();
			objectValidator.validate(Pair.class, p);
		}
		
		return p;
	}

	protected abstract Pair.PairBuilder doEvaluate(Boolean flag, Void t, Void u, String a, String b);

	public static class ExistsThenCleanConditionalDefault extends ExistsThenCleanConditional {
		@Override
		protected Pair.PairBuilder doEvaluate(Boolean flag, Void t, Void u, String a, String b) {
			Pair.PairBuilder p = Pair.builder();
			return assignOutput(p, flag, t, u, a, b);
		}
		
		protected Pair.PairBuilder assignOutput(Pair.PairBuilder p, Boolean flag, Void t, Void u, String a, String b) {
			p
				.setPresent(exists(MapperS.<Void>ofNull()).get());
			
			String ifThenElseResult = "z";
			if (areEqual(MapperS.of(a), MapperS.of(b), CardinalityOperator.All).getOrDefault(false)) {
				ifThenElseResult = "y";
			}
			p
				.setS(ifThenElseResult);
			
			return Optional.ofNullable(p)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
