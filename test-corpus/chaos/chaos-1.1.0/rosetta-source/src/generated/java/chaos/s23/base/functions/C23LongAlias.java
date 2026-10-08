package chaos.s23.base.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;


@ImplementedBy(C23LongAlias.C23LongAliasDefault.class)
public abstract class C23LongAlias implements RosettaFunction {

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

	public static class C23LongAliasDefault extends C23LongAlias {
		@Override
		protected BigDecimal doEvaluate(BigDecimal v, Boolean f) {
			BigDecimal r = null;
			return assignOutput(r, v, f);
		}
		
		protected BigDecimal assignOutput(BigDecimal r, BigDecimal v, Boolean f) {
			if ((f == null ? false : f)) {
				final Long _long = big12(v, f).get();
				if (_long == null) {
					r = null;
				} else {
					r = BigDecimal.valueOf(_long);
				}
			} else {
				r = v;
			}
			
			return r;
		}
		
		@Override
		protected MapperS<Long> big12(BigDecimal v, Boolean f) {
			return MapperS.of(123456789012l);
		}
	}
}
