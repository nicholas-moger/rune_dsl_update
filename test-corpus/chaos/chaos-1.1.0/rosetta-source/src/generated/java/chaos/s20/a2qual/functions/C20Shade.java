package chaos.s20.a2qual.functions;

import chaos.s20.a2qual.C20Branch;
import chaos.s20.a2qual.C20Trunk;
import chaos.s20.a2qual.C20Twig;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperListOfLists;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;


@ImplementedBy(C20Shade.C20ShadeDefault.class)
public abstract class C20Shade implements RosettaFunction {

	/**
	* @param trunks 
	* @return n 
	*/
	public BigDecimal evaluate(List<? extends C20Trunk> trunks) {
		BigDecimal n = doEvaluate(trunks);
		
		return n;
	}

	protected abstract BigDecimal doEvaluate(List<? extends C20Trunk> trunks);

	public static class C20ShadeDefault extends C20Shade {
		@Override
		protected BigDecimal doEvaluate(List<? extends C20Trunk> trunks) {
			if (trunks == null) {
				trunks = Collections.emptyList();
			}
			BigDecimal n = null;
			return assignOutput(n, trunks);
		}
		
		protected BigDecimal assignOutput(BigDecimal n, List<? extends C20Trunk> trunks) {
			final MapperListOfLists<Integer> thenArg0 = MapperC.<C20Trunk>of(trunks)
				.mapItemToList(t -> t.<C20Branch>mapC("getBranches", c20Trunk -> c20Trunk.getBranches())
					.mapItem(tb -> MapperS.of(tb.<C20Twig>mapC("getTwigs", c20Branch -> c20Branch.getTwigs()).resultCount())));
			final MapperC<Integer> thenArg1 = thenArg0
				.flattenList();
			final MapperC<String> thenArg2 = MapperC.<C20Trunk>of(trunks)
				.mapItem(t -> t.<String>map("getTitle", c20Trunk -> c20Trunk.getTitle()));
			final Integer integer = MapperMaths.<Integer, Integer, Integer>add(thenArg1
				.sumInteger(), MapperS.of(thenArg2.resultCount())).get();
			if (integer == null) {
				n = null;
			} else {
				n = BigDecimal.valueOf(integer);
			}
			
			return n;
		}
	}
}
