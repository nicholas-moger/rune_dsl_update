package holdout.convbigintaliaslong.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;


@ImplementedBy(LongAliasTwice.LongAliasTwiceDefault.class)
public abstract class LongAliasTwice implements RosettaFunction {

	/**
	* @param v 
	* @param f 
	* @return r 
	*/
	public BigDecimal evaluate(BigDecimal v, Boolean f) {
		BigDecimal r = doEvaluate(v, f);
		
		return r;
	}

	protected abstract BigDecimal doEvaluate(BigDecimal v, Boolean f);

	protected abstract MapperS<Long> big12(BigDecimal v, Boolean f);

	protected abstract MapperS<Long> big13(BigDecimal v, Boolean f);

	public static class LongAliasTwiceDefault extends LongAliasTwice {
		@Override
		protected BigDecimal doEvaluate(BigDecimal v, Boolean f) {
			BigDecimal r = null;
			return assignOutput(r, v, f);
		}
		
		protected BigDecimal assignOutput(BigDecimal r, BigDecimal v, Boolean f) {
			if ((f == null ? false : f)) {
				final Long long0 = big12(v, f).get();
				if (long0 == null) {
					r = null;
				} else {
					r = BigDecimal.valueOf(long0);
				}
			} else {
				final Long long1 = big13(v, f).get();
				if (long1 == null) {
					r = null;
				} else {
					r = BigDecimal.valueOf(long1);
				}
			}
			
			return r;
		}
		
		@Override
		protected MapperS<Long> big12(BigDecimal v, Boolean f) {
			return MapperS.of(123456789012l);
		}
		
		@Override
		protected MapperS<Long> big13(BigDecimal v, Boolean f) {
			return MapperS.of(1234567890123l);
		}
	}
}
