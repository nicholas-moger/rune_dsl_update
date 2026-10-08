package chaos.s04.a8pkg.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;


@ImplementedBy(C4Grow.C4GrowDefault.class)
public abstract class C4Grow implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected C4Add c4Add;

	/**
	* @param seedv 
	* @param n 
	* @return outs 
	*/
	public List<BigDecimal> evaluate(BigDecimal seedv, BigDecimal n) {
		List<BigDecimal> outs = doEvaluate(seedv, n);
		
		return outs;
	}

	protected abstract List<BigDecimal> doEvaluate(BigDecimal seedv, BigDecimal n);

	public static class C4GrowDefault extends C4Grow {
		@Override
		protected List<BigDecimal> doEvaluate(BigDecimal seedv, BigDecimal n) {
			List<BigDecimal> outs = new ArrayList<>();
			return assignOutput(outs, seedv, n);
		}
		
		protected List<BigDecimal> assignOutput(List<BigDecimal> outs, BigDecimal seedv, BigDecimal n) {
			if (seedv == null) {
				outs.addAll(Collections.<BigDecimal>emptyList());
			} else {
				outs.addAll(Collections.singletonList(seedv));
			}
			
			outs.addAll(MapperMaths.<BigDecimal, BigDecimal, BigDecimal>multiply(MapperS.of(seedv), MapperS.of(n)).getMulti());
			
			outs.addAll(MapperC.<BigDecimal>of(MapperS.of(seedv), MapperS.of(n))
				.mapItem(item -> MapperMaths.<BigDecimal, BigDecimal, BigDecimal>add(item, MapperS.of(BigDecimal.valueOf(1)))).getMulti());
			
			final BigDecimal bigDecimal = c4Add.evaluate(seedv, n);
			if (bigDecimal == null) {
				outs.addAll(Collections.<BigDecimal>emptyList());
			} else {
				outs.addAll(Collections.singletonList(bigDecimal));
			}
			
			return outs;
		}
	}
}
