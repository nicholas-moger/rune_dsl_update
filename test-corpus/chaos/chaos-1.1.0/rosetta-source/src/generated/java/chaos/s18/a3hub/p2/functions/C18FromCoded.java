package chaos.s18.a3hub.p2.functions;

import chaos.s18.a3hub.p2.C18Coded;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.metafields.FieldWithMetaString;
import java.math.BigDecimal;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C18FromCoded.C18FromCodedDefault.class)
public abstract class C18FromCoded implements RosettaFunction {

	/**
	* @param c 
	* @return n 
	*/
	public BigDecimal evaluate(C18Coded c) {
		BigDecimal n = doEvaluate(c);
		
		return n;
	}

	protected abstract BigDecimal doEvaluate(C18Coded c);

	public static class C18FromCodedDefault extends C18FromCoded {
		@Override
		protected BigDecimal doEvaluate(C18Coded c) {
			BigDecimal n = null;
			return assignOutput(n, c);
		}
		
		protected BigDecimal assignOutput(BigDecimal n, C18Coded c) {
			final MapperS<String> switchArgument = MapperS.of(c).<FieldWithMetaString>map("getKind", c18Coded -> c18Coded.getKind()).<String>map("Type coercion", fieldWithMetaString -> fieldWithMetaString == null ? null : fieldWithMetaString.getValue());
			if (switchArgument.get() == null) {
				n = null;
			} else if (areEqual(switchArgument, MapperS.of("spot"), CardinalityOperator.All).get()) {
				n = BigDecimal.valueOf(1);
			} else if (areEqual(switchArgument, MapperS.of("fwd"), CardinalityOperator.All).get()) {
				n = BigDecimal.valueOf(2);
			} else {
				n = BigDecimal.valueOf(0);
			}
			
			return n;
		}
	}
}
