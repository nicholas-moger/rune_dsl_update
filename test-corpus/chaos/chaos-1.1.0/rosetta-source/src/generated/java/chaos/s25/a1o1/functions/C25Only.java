package chaos.s25.a1o1.functions;

import chaos.s25.a1o1.C25Paths;
import chaos.s25.a1o1.C25Pick;
import chaos.s25.a1o1.C25Sub;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C25Only.C25OnlyDefault.class)
public abstract class C25Only implements RosettaFunction {

	/**
	* @param ps 
	* @param t 
	* @return oks 
	*/
	public List<Boolean> evaluate(List<? extends C25Paths> ps, C25Paths t) {
		List<Boolean> oks = doEvaluate(ps, t);
		
		return oks;
	}

	protected abstract List<Boolean> doEvaluate(List<? extends C25Paths> ps, C25Paths t);

	public static class C25OnlyDefault extends C25Only {
		@Override
		protected List<Boolean> doEvaluate(List<? extends C25Paths> ps, C25Paths t) {
			if (ps == null) {
				ps = Collections.emptyList();
			}
			List<Boolean> oks = new ArrayList<>();
			return assignOutput(oks, ps, t);
		}
		
		protected List<Boolean> assignOutput(List<Boolean> oks, List<? extends C25Paths> ps, C25Paths t) {
			oks.addAll(onlyExists(MapperS.of(t), Arrays.asList("p", "q", "sub", "pick", "subs"), Arrays.asList("p")).getMulti());
			
			oks.addAll(onlyExists(MapperS.of(t).<C25Sub>map("getSub", c25Paths -> c25Paths.getSub()), Arrays.asList("sname", "subs"), Arrays.asList("sname", "subs")).getMulti());
			
			oks.addAll(MapperC.<C25Paths>of(ps)
				.mapItem(x -> onlyExists(x.<C25Sub>map("getSub", c25Paths -> c25Paths.getSub()), Arrays.asList("sname", "subs"), Arrays.asList("sname")).asMapper()).getMulti());
			
			final MapperC<C25Paths> thenArg = MapperC.<C25Paths>of(ps);
			oks.addAll(thenArg
				.mapItem(item -> onlyExists(item.<C25Pick>map("getPick", c25Paths -> c25Paths.getPick()), Arrays.asList("C25OptA", "C25OptB"), Arrays.asList("C25OptA")).asMapper()).getMulti());
			
			return oks;
		}
	}
}
