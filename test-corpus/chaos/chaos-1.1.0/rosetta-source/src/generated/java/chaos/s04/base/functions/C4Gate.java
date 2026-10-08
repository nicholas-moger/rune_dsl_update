package chaos.s04.base.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.math.BigInteger;
import javax.inject.Inject;


@ImplementedBy(C4Gate.C4GateDefault.class)
public abstract class C4Gate implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected C4IsBig c4IsBig;

	/**
	* @param _final 
	* @param _static 
	* @param _interface 
	* @return verdict 
	*/
	public BigDecimal evaluate(BigDecimal _final, BigDecimal _static, String _interface) {
		BigDecimal verdict = doEvaluate(_final, _static, _interface);
		
		return verdict;
	}

	protected abstract BigDecimal doEvaluate(BigDecimal _final, BigDecimal _static, String _interface);

	public static class C4GateDefault extends C4Gate {
		@Override
		protected BigDecimal doEvaluate(BigDecimal _final, BigDecimal _static, String _interface) {
			BigDecimal verdict = null;
			return assignOutput(verdict, _final, _static, _interface);
		}
		
		protected BigDecimal assignOutput(BigDecimal verdict, BigDecimal _final, BigDecimal _static, String _interface) {
			final Boolean _boolean = c4IsBig.evaluate(_final);
			if ((_boolean == null ? false : _boolean)) {
				final BigInteger bigInteger = new BigInteger("9999999999999999999999999");
				if (bigInteger == null) {
					verdict = null;
				} else {
					verdict = new BigDecimal(bigInteger);
				}
			} else {
				verdict = MapperMaths.<BigDecimal, BigDecimal, BigDecimal>add(MapperS.of(_final), MapperS.of(MapperS.of(_static).getOrDefault(BigDecimal.valueOf(0)))).get();
			}
			
			return verdict;
		}
	}
}
