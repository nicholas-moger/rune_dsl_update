package chaos.s20.a2qual.functions;

import chaos.s20.a2qual.C20Branch;
import chaos.s20.a2qual.C20Trunk;
import chaos.s20.a2qual.C20Twig;
import chaos.s20.a2qual.h.C20Leaf;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperListOfLists;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@ImplementedBy(C20Harvest.C20HarvestDefault.class)
public abstract class C20Harvest implements RosettaFunction {

	/**
	* @param trunks 
	* @return vs 
	*/
	public List<BigDecimal> evaluate(List<? extends C20Trunk> trunks) {
		List<BigDecimal> vs = doEvaluate(trunks);
		
		return vs;
	}

	protected abstract List<BigDecimal> doEvaluate(List<? extends C20Trunk> trunks);

	public static class C20HarvestDefault extends C20Harvest {
		@Override
		protected List<BigDecimal> doEvaluate(List<? extends C20Trunk> trunks) {
			if (trunks == null) {
				trunks = Collections.emptyList();
			}
			List<BigDecimal> vs = new ArrayList<>();
			return assignOutput(vs, trunks);
		}
		
		protected List<BigDecimal> assignOutput(List<BigDecimal> vs, List<? extends C20Trunk> trunks) {
			final MapperC<C20Trunk> thenArg0 = MapperC.<C20Trunk>of(trunks);
			final MapperListOfLists<BigDecimal> thenArg1 = thenArg0
				.mapItemToList(t -> t.<C20Branch>mapC("getBranches", c20Trunk -> c20Trunk.getBranches())
					.mapItemToList(b -> b.<C20Twig>mapC("getTwigs", c20Branch -> c20Branch.getTwigs())
						.mapItemToList(w -> w.<C20Leaf>mapC("getLeaves", c20Twig -> c20Twig.getLeaves())
							.mapItem(l -> l.<BigDecimal>map("getV", c20Leaf -> c20Leaf.getV())))));
			vs.addAll(thenArg1
				.flattenList().getMulti());
			
			return vs;
		}
	}
}
