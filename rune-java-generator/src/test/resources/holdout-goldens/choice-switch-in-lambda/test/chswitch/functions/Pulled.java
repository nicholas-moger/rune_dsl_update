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


@ImplementedBy(Pulled.PulledDefault.class)
public abstract class Pulled implements RosettaFunction {

	/**
	* @param eths 
	* @return texts 
	*/
	public List<String> evaluate(List<? extends Either> eths) {
		List<String> texts = doEvaluate(eths);
		
		return texts;
	}

	protected abstract List<String> doEvaluate(List<? extends Either> eths);

	protected abstract MapperC<String> pulled(List<? extends Either> eths);

	public static class PulledDefault extends Pulled {
		@Override
		protected List<String> doEvaluate(List<? extends Either> eths) {
			if (eths == null) {
				eths = Collections.emptyList();
			}
			List<String> texts = new ArrayList<>();
			return assignOutput(texts, eths);
		}
		
		protected List<String> assignOutput(List<String> texts, List<? extends Either> eths) {
			texts.addAll(pulled(eths).getMulti());
			
			return texts;
		}
		
		@Override
		protected MapperC<String> pulled(List<? extends Either> eths) {
			return MapperC.<Either>of(eths)
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
		}
	}
}
