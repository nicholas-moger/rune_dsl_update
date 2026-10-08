package test.chswitchedge.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import test.chswitchedge.Either;
import test.chswitchedge.OptA;
import test.chswitchedge.OptB;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(ThenChain.ThenChainDefault.class)
public abstract class ThenChain implements RosettaFunction {

	/**
	* @param eths 
	* @return texts 
	*/
	public List<String> evaluate(List<? extends Either> eths) {
		List<String> texts = doEvaluate(eths);
		
		return texts;
	}

	protected abstract List<String> doEvaluate(List<? extends Either> eths);

	public static class ThenChainDefault extends ThenChain {
		@Override
		protected List<String> doEvaluate(List<? extends Either> eths) {
			if (eths == null) {
				eths = Collections.emptyList();
			}
			List<String> texts = new ArrayList<>();
			return assignOutput(texts, eths);
		}
		
		protected List<String> assignOutput(List<String> texts, List<? extends Either> eths) {
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
			texts.addAll(thenArg
				.filterItemNullSafe(item -> notEqual(item, MapperS.of("none"), CardinalityOperator.Any).get()).getMulti());
			
			return texts;
		}
	}
}
