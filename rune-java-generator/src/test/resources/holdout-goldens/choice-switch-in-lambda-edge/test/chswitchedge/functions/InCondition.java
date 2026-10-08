package test.chswitchedge.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.functions.ConditionValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import test.chswitchedge.Either;
import test.chswitchedge.OptA;
import test.chswitchedge.OptB;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(InCondition.InConditionDefault.class)
public abstract class InCondition implements RosettaFunction {
	
	@Inject protected ConditionValidator conditionValidator;

	/**
	* @param eths 
	* @return n 
	*/
	public Integer evaluate(List<? extends Either> eths) {
		// pre-conditions
		conditionValidator.validate(() -> {
			final MapperC<String> thenArg = MapperC.<Either>of(eths)
				.mapItem(item -> {
					if (item.get() == null) {
						return MapperS.<String>ofNull();
					}
					if (item.<OptA>map("getOptA", either -> either.getOptA()).get() != null) {
						final MapperS<OptA> optA = item.<OptA>map("getOptA", either -> either.getOptA());
						return optA.<String>map("getAv", _optA -> _optA.getAv());
					}
					if (item.<OptB>map("getOptB", either -> either.getOptB()).get() != null) {
						final MapperS<OptB> optB = item.<OptB>map("getOptB", either -> either.getOptB());
						return optB.<BigDecimal>map("getBv", _optB -> _optB.getBv()).map("to-string", Object::toString);
					}
					return MapperS.of("none");
				});
			return ComparisonResult.ofNullSafe(exists(thenArg).asMapper());
		},
			"");
		
		Integer n = doEvaluate(eths);
		
		return n;
	}

	protected abstract Integer doEvaluate(List<? extends Either> eths);

	public static class InConditionDefault extends InCondition {
		@Override
		protected Integer doEvaluate(List<? extends Either> eths) {
			if (eths == null) {
				eths = Collections.emptyList();
			}
			Integer n = null;
			return assignOutput(n, eths);
		}
		
		protected Integer assignOutput(Integer n, List<? extends Either> eths) {
			n = MapperC.<Either>of(eths).resultCount();
			
			return n;
		}
	}
}
