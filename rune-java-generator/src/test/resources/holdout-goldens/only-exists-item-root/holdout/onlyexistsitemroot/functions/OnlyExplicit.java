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

@ImplementedBy(OnlyExplicit.OnlyExplicitDefault.class)
public abstract class OnlyExplicit implements RosettaFunction {

	/**
	* @param ps 
	* @return oks 
	*/
	public List<Boolean> evaluate(List<? extends Paths> ps) {
		List<Boolean> oks = doEvaluate(ps);
		
		return oks;
	}

	protected abstract List<Boolean> doEvaluate(List<? extends Paths> ps);

	public static class OnlyExplicitDefault extends OnlyExplicit {
		@Override
		protected List<Boolean> doEvaluate(List<? extends Paths> ps) {
			if (ps == null) {
				ps = Collections.emptyList();
			}
			List<Boolean> oks = new ArrayList<>();
			return assignOutput(oks, ps);
		}
		
		protected List<Boolean> assignOutput(List<Boolean> oks, List<? extends Paths> ps) {
			oks.addAll(MapperC.<Paths>of(ps)
				.mapItem(x -> onlyExists(x, Arrays.asList("p", "q", "sub", "pick"), Arrays.asList("p")).asMapper()).getMulti());
			
			oks.addAll(MapperC.<Paths>of(ps)
				.mapItem(x -> onlyExists(x.<Pick>map("getPick", paths -> paths.getPick()), Arrays.asList("OptA", "OptB"), Arrays.asList("OptA")).asMapper()).getMulti());
			
			oks.addAll(MapperC.<Paths>of(ps)
				.mapItem(x -> onlyExists(x.<Sub>map("getSub", paths -> paths.getSub()), Arrays.asList("sname", "subs"), Arrays.asList("sname", "subs")).asMapper()).getMulti());
			
			return oks;
		}
	}
}
