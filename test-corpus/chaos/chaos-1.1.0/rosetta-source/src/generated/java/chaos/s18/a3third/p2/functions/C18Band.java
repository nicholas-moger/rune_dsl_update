package chaos.s18.a3third.p2.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C18Band.C18BandDefault.class)
public abstract class C18Band implements RosettaFunction {

	/**
	* @param i 
	* @return b 
	*/
	public String evaluate(BigDecimal i) {
		String b = doEvaluate(i);
		
		return b;
	}

	protected abstract String doEvaluate(BigDecimal i);

	public static class C18BandDefault extends C18Band {
		@Override
		protected String doEvaluate(BigDecimal i) {
			String b = null;
			return assignOutput(b, i);
		}
		
		protected String assignOutput(String b, BigDecimal i) {
			final MapperS<BigDecimal> switchArgument = MapperS.of(i);
			if (switchArgument.get() == null) {
				b = null;
			} else if (areEqual(switchArgument, MapperS.of(BigDecimal.valueOf(1)), CardinalityOperator.All).get()) {
				b = "one";
			} else if (areEqual(switchArgument, MapperS.of(BigDecimal.valueOf(2)), CardinalityOperator.All).get()) {
				b = "two";
			} else {
				b = "many";
			}
			
			return b;
		}
	}
}
