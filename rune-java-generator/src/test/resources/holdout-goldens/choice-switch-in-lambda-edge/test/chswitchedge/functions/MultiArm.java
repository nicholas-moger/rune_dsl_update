package test.chswitchedge.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperListOfLists;
import com.rosetta.model.lib.mapper.MapperS;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import test.chswitchedge.Either;
import test.chswitchedge.OptA;


@ImplementedBy(MultiArm.MultiArmDefault.class)
public abstract class MultiArm implements RosettaFunction {

	/**
	* @param eths 
	* @return tags 
	*/
	public List<String> evaluate(List<? extends Either> eths) {
		List<String> tags = doEvaluate(eths);
		
		return tags;
	}

	protected abstract List<String> doEvaluate(List<? extends Either> eths);

	public static class MultiArmDefault extends MultiArm {
		@Override
		protected List<String> doEvaluate(List<? extends Either> eths) {
			if (eths == null) {
				eths = Collections.emptyList();
			}
			List<String> tags = new ArrayList<>();
			return assignOutput(tags, eths);
		}
		
		protected List<String> assignOutput(List<String> tags, List<? extends Either> eths) {
			final MapperListOfLists<String> thenArg = MapperC.<Either>of(eths)
				.mapItemToList(item -> {
					if (item.get() == null) {
						return MapperC.<String>ofNull();
					}
					if (item.<OptA>map("getOptA", either -> either.getOptA()).get() != null) {
						final MapperS<OptA> optA = item.<OptA>map("getOptA", either -> either.getOptA());
						return optA.<String>mapC("getTags", _optA -> _optA.getTags());
					}
					return MapperC.<String>ofNull();
				});
			tags.addAll(thenArg
				.flattenList().getMulti());
			
			return tags;
		}
	}
}
