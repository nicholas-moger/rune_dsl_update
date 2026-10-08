package test.chswitchedge.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import test.chswitchedge.Both;
import test.chswitchedge.Either;
import test.chswitchedge.OptA;
import test.chswitchedge.OptC;


@ImplementedBy(NestedChoice.NestedChoiceDefault.class)
public abstract class NestedChoice implements RosettaFunction {

	/**
	* @param boths 
	* @return texts 
	*/
	public List<String> evaluate(List<? extends Both> boths) {
		List<String> texts = doEvaluate(boths);
		
		return texts;
	}

	protected abstract List<String> doEvaluate(List<? extends Both> boths);

	public static class NestedChoiceDefault extends NestedChoice {
		@Override
		protected List<String> doEvaluate(List<? extends Both> boths) {
			if (boths == null) {
				boths = Collections.emptyList();
			}
			List<String> texts = new ArrayList<>();
			return assignOutput(texts, boths);
		}
		
		protected List<String> assignOutput(List<String> texts, List<? extends Both> boths) {
			texts.addAll(MapperC.<Both>of(boths)
				.mapItem(item -> {
					if (item.get() == null) {
						return MapperS.<String>ofNull();
					}
					if (item.<Either>map("getEither", both -> both.getEither()).<OptA>map("getOptA", either -> either.getOptA()).get() != null) {
						final MapperS<OptA> optA = item.<Either>map("getEither", both -> both.getEither()).<OptA>map("getOptA", either -> either.getOptA());
						return optA.<String>map("getAv", _optA -> _optA.getAv());
					}
					if (item.<OptC>map("getOptC", both -> both.getOptC()).get() != null) {
						final MapperS<OptC> optC = item.<OptC>map("getOptC", both -> both.getOptC());
						return optC.<String>map("getCv", _optC -> _optC.getCv());
					}
					return MapperS.of("none");
				}).getMulti());
			
			return texts;
		}
	}
}
