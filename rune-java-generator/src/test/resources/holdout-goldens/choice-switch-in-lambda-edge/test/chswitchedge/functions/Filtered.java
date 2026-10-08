package test.chswitchedge.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import test.chswitchedge.Either;
import test.chswitchedge.OptA;
import test.chswitchedge.OptB;


@ImplementedBy(Filtered.FilteredDefault.class)
public abstract class Filtered implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param eths 
	* @return kept 
	*/
	public List<? extends Either> evaluate(List<? extends Either> eths) {
		List<Either.EitherBuilder> keptBuilder = doEvaluate(eths);
		
		final List<? extends Either> kept;
		if (keptBuilder == null) {
			kept = null;
		} else {
			kept = keptBuilder.stream().map(Either::build).collect(Collectors.toList());
			objectValidator.validate(Either.class, kept);
		}
		
		return kept;
	}

	protected abstract List<Either.EitherBuilder> doEvaluate(List<? extends Either> eths);

	public static class FilteredDefault extends Filtered {
		@Override
		protected List<Either.EitherBuilder> doEvaluate(List<? extends Either> eths) {
			if (eths == null) {
				eths = Collections.emptyList();
			}
			List<Either.EitherBuilder> kept = new ArrayList<>();
			return assignOutput(kept, eths);
		}
		
		protected List<Either.EitherBuilder> assignOutput(List<Either.EitherBuilder> kept, List<? extends Either> eths) {
			kept.addAll(toBuilder(MapperC.<Either>of(eths)
				.filterItemNullSafe(item -> {
					if (item.get() == null) {
						return null;
					}
					if (item.<OptA>map("getOptA", either -> either.getOptA()).get() != null) {
						final MapperS<OptA> optA = item.<OptA>map("getOptA", either -> either.getOptA());
						return true;
					}
					if (item.<OptB>map("getOptB", either -> either.getOptB()).get() != null) {
						final MapperS<OptB> optB = item.<OptB>map("getOptB", either -> either.getOptB());
						return false;
					}
					return false;
				}).getMulti()));
			
			return Optional.ofNullable(kept)
				.map(o -> o.stream().map(i -> i.prune()).collect(Collectors.toList()))
				.orElse(null);
		}
	}
}
