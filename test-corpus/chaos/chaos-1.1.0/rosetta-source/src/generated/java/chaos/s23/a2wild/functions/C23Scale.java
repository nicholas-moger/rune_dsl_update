package chaos.s23.a2wild.functions;

import chaos.s23.a2wild.h.C23Box;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;


@ImplementedBy(C23Scale.C23ScaleDefault.class)
public abstract class C23Scale implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected C23Mul c23Mul;

	/**
	* @param q 
	* @param w 
	* @param bs 
	* @return r 
	*/
	public BigDecimal evaluate(Integer q, BigDecimal w, List<? extends C23Box> bs) {
		BigDecimal r = doEvaluate(q, w, bs);
		
		return r;
	}

	protected abstract BigDecimal doEvaluate(Integer q, BigDecimal w, List<? extends C23Box> bs);

	protected abstract MapperS<BigDecimal> scaled(Integer q, BigDecimal w, List<? extends C23Box> bs);

	protected abstract MapperS<Integer> counted(Integer q, BigDecimal w, List<? extends C23Box> bs);

	protected abstract MapperS<Integer> summed(Integer q, BigDecimal w, List<? extends C23Box> bs);

	public static class C23ScaleDefault extends C23Scale {
		@Override
		protected BigDecimal doEvaluate(Integer q, BigDecimal w, List<? extends C23Box> bs) {
			if (bs == null) {
				bs = Collections.emptyList();
			}
			BigDecimal r = null;
			return assignOutput(r, q, w, bs);
		}
		
		protected BigDecimal assignOutput(BigDecimal r, Integer q, BigDecimal w, List<? extends C23Box> bs) {
			r = MapperMaths.<BigDecimal, BigDecimal, BigDecimal>add(MapperMaths.<BigDecimal, BigDecimal, BigDecimal>add(MapperMaths.<BigDecimal, BigDecimal, BigDecimal>add(scaled(q, w, bs), counted(q, w, bs).<BigDecimal>map("Type coercion", integer0 -> integer0 == null ? null : BigDecimal.valueOf(integer0))), summed(q, w, bs).<BigDecimal>map("Type coercion", integer1 -> integer1 == null ? null : BigDecimal.valueOf(integer1))), (q == null ? MapperS.<BigDecimal>ofNull() : MapperS.of(BigDecimal.valueOf(q)))).get();
			
			return r;
		}
		
		@Override
		protected MapperS<BigDecimal> scaled(Integer q, BigDecimal w, List<? extends C23Box> bs) {
			return MapperS.of(c23Mul.evaluate((q == null ? null : BigDecimal.valueOf(q)), w));
		}
		
		@Override
		protected MapperS<Integer> counted(Integer q, BigDecimal w, List<? extends C23Box> bs) {
			final MapperC<String> thenArg = MapperC.<C23Box>of(bs)
				.mapItem(b -> b.<String>map("getLid", c23Box -> c23Box.getLid()));
			return MapperS.of(thenArg.resultCount());
		}
		
		@Override
		protected MapperS<Integer> summed(Integer q, BigDecimal w, List<? extends C23Box> bs) {
			final MapperC<Integer> thenArg = MapperC.<C23Box>of(bs)
				.mapItem(b -> MapperS.of(b.<String>map("getLid", c23Box -> c23Box.getLid()).resultCount()));
			return thenArg
				.sumInteger();
		}
	}
}
