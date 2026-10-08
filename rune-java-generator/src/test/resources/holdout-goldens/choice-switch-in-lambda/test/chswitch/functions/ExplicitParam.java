package test.chswitch.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import test.chswitch.Either;
import test.chswitch.OptA;
import test.chswitch.OptB;


@ImplementedBy(ExplicitParam.ExplicitParamDefault.class)
public abstract class ExplicitParam implements RosettaFunction {

	/**
	* @param eths 
	* @return texts 
	*/
	public List<String> evaluate(List<? extends Either> eths) {
		List<String> texts = doEvaluate(eths);
		
		return texts;
	}

	protected abstract List<String> doEvaluate(List<? extends Either> eths);

	public static class ExplicitParamDefault extends ExplicitParam {
		@Override
		protected List<String> doEvaluate(List<? extends Either> eths) {
			if (eths == null) {
				eths = Collections.emptyList();
			}
			List<String> texts = new ArrayList<>();
			return assignOutput(texts, eths);
		}
		
		protected List<String> assignOutput(List<String> texts, List<? extends Either> eths) {
			texts.addAll(MapperC.<Either>of(eths)
				.mapItem(eth -> {
					if (eth.get() == null) {
						return MapperS.<String>ofNull();
					}
					if (eth.<OptA>map("getOptA", either -> either.getOptA()).get() != null) {
						final MapperS<OptA> optA = eth.<OptA>map("getOptA", either -> either.getOptA());
						return optA.<String>map("getAv", _optA -> _optA.getAv());
					}
					if (eth.<OptB>map("getOptB", either -> either.getOptB()).get() != null) {
						final MapperS<OptB> optB = eth.<OptB>map("getOptB", either -> either.getOptB());
						return optB.<BigDecimal>map("getBv", _optB -> _optB.getBv()).map("to-string", Object::toString);
					}
					return MapperS.of("none");
				}).getMulti());
			
			return texts;
		}
	}
}
