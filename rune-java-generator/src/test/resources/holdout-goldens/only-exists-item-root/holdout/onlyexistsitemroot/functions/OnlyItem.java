package holdout.onlyexistsitemroot.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import holdout.onlyexistsitemroot.Paths;
import holdout.onlyexistsitemroot.Pick;
import holdout.onlyexistsitemroot.Sub;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(OnlyItem.OnlyItemDefault.class)
public abstract class OnlyItem implements RosettaFunction {

	/**
	* @param ps 
	* @param t 
	* @return oks 
	*/
	public List<Boolean> evaluate(List<? extends Paths> ps, Paths t) {
		List<Boolean> oks = doEvaluate(ps, t);
		
		return oks;
	}

	protected abstract List<Boolean> doEvaluate(List<? extends Paths> ps, Paths t);

	public static class OnlyItemDefault extends OnlyItem {
		@Override
		protected List<Boolean> doEvaluate(List<? extends Paths> ps, Paths t) {
			if (ps == null) {
				ps = Collections.emptyList();
			}
			List<Boolean> oks = new ArrayList<>();
			return assignOutput(oks, ps, t);
		}
		
		protected List<Boolean> assignOutput(List<Boolean> oks, List<? extends Paths> ps, Paths t) {
			final MapperC<Paths> thenArg0 = MapperC.<Paths>of(ps);
			oks.addAll(thenArg0
				.mapItem(item -> onlyExists(item, Arrays.asList("p", "q", "sub", "pick"), Arrays.asList("p")).asMapper()).getMulti());
			
			final MapperC<Paths> thenArg1 = MapperC.<Paths>of(ps);
			oks.addAll(thenArg1
				.mapItem(item -> onlyExists(item.<Pick>map("getPick", paths -> paths.getPick()), Arrays.asList("OptA", "OptB"), Arrays.asList("OptA")).asMapper()).getMulti());
			
			final MapperC<Paths> thenArg2 = MapperC.<Paths>of(ps);
			oks.addAll(thenArg2
				.mapItem(item -> onlyExists(item.<Sub>map("getSub", paths -> paths.getSub()), Arrays.asList("sname", "subs"), Arrays.asList("sname", "subs")).asMapper()).getMulti());
			
			return oks;
		}
	}
}
