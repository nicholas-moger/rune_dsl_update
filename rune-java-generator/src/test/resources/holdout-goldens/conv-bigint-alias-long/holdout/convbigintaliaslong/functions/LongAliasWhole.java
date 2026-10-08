package holdout.convbigintaliaslong.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;


@ImplementedBy(LongAliasWhole.LongAliasWholeDefault.class)
public abstract class LongAliasWhole implements RosettaFunction {

	/**
	* @param v 
	* @return r 
	*/
	public BigDecimal evaluate(BigDecimal v) {
		BigDecimal r = doEvaluate(v);
		
		return r;
	}

	protected abstract BigDecimal doEvaluate(BigDecimal v);

	protected abstract MapperS<Long> big12(BigDecimal v);

	public static class LongAliasWholeDefault extends LongAliasWhole {
		@Override
		protected BigDecimal doEvaluate(BigDecimal v) {
			BigDecimal r = null;
			return assignOutput(r, v);
		}
		
		protected BigDecimal assignOutput(BigDecimal r, BigDecimal v) {
			final Long _long = big12(v).get();
			if (_long == null) {
				r = null;
			} else {
				r = BigDecimal.valueOf(_long);
			}
			
			return r;
		}
		
		@Override
		protected MapperS<Long> big12(BigDecimal v) {
			return MapperS.of(123456789012l);
		}
	}
}
