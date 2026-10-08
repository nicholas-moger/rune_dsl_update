package chaos.s29.a2dangle.functions;

import chaos.s29.a2dangle.C29Inner;
import chaos.s29.a2dangle.C29Outer;
import chaos.s29.a2dangle.util.C29InnerDeepPathUtil;
import chaos.s29.a2dangle.util.C29OuterDeepPathUtil;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperListOfLists;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;


@ImplementedBy(C29LoL.C29LoLDefault.class)
public abstract class C29LoL implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected C29InnerDeepPathUtil c29InnerDeepPathUtil;
	@Inject protected C29OuterDeepPathUtil c29OuterDeepPathUtil;

	/**
	* @param outers 
	* @return vs 
	*/
	public List<BigDecimal> evaluate(List<? extends C29Outer> outers) {
		List<BigDecimal> vs = doEvaluate(outers);
		
		return vs;
	}

	protected abstract List<BigDecimal> doEvaluate(List<? extends C29Outer> outers);

	public static class C29LoLDefault extends C29LoL {
		@Override
		protected List<BigDecimal> doEvaluate(List<? extends C29Outer> outers) {
			if (outers == null) {
				outers = Collections.emptyList();
			}
			List<BigDecimal> vs = new ArrayList<>();
			return assignOutput(vs, outers);
		}
		
		protected List<BigDecimal> assignOutput(List<BigDecimal> vs, List<? extends C29Outer> outers) {
			final MapperListOfLists<C29Inner> thenArg = MapperC.<C29Outer>of(outers)
				.mapItemToList(o -> o.<C29Inner>mapC("chooseInners", c29Outer -> c29OuterDeepPathUtil.chooseInners(c29Outer)));
			vs.addAll(thenArg
				.mapListToItem(ins -> {
					final MapperC<String> _thenArg = ins.<String>map("chooseDeep", c29Inner -> c29InnerDeepPathUtil.chooseDeep(c29Inner));
					return MapperS.of(_thenArg.resultCount());
				}).<BigDecimal>map("Type coercion", integer -> BigDecimal.valueOf(integer)).getMulti());
			
			vs.addAll(MapperC.<C29Outer>of(outers)
				.mapItem(o -> {
					final MapperC<String> _thenArg = o.<C29Inner>mapC("chooseInners", c29Outer -> c29OuterDeepPathUtil.chooseInners(c29Outer)).<String>map("chooseDeep", c29Inner -> c29InnerDeepPathUtil.chooseDeep(c29Inner));
					return MapperS.of(_thenArg.resultCount());
				}).<BigDecimal>map("Type coercion", integer -> BigDecimal.valueOf(integer)).getMulti());
			
			return vs;
		}
	}
}
