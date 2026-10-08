package chaos.s21.a1o3.functions;

import chaos.s21.a1o3.C21Paths;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ConditionValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C21Void.C21VoidDefault.class)
public abstract class C21Void implements RosettaFunction {
	
	@Inject protected ConditionValidator conditionValidator;

	/**
	* @param t 
	* @param many 
	* @return n 
	*/
	public BigDecimal evaluate(C21Paths t, List<String> many) {
		// pre-conditions
		conditionValidator.validate(() -> notExists(MapperS.of(t).<String>map("getP", c21Paths -> c21Paths.getP())).orNullSafe(exists(MapperS.of(t).<String>map("getP", c21Paths -> c21Paths.getP()))),
			"");
		
		conditionValidator.validate(() -> multipleExists(MapperC.<String>of(many)).orNullSafe(singleExists(MapperC.<String>of(many))).orNullSafe(notExists(MapperC.<String>of(many))),
			"");
		
		BigDecimal n = doEvaluate(t, many);
		
		return n;
	}

	protected abstract BigDecimal doEvaluate(C21Paths t, List<String> many);

	public static class C21VoidDefault extends C21Void {
		@Override
		protected BigDecimal doEvaluate(C21Paths t, List<String> many) {
			if (many == null) {
				many = Collections.emptyList();
			}
			BigDecimal n = null;
			return assignOutput(n, t, many);
		}
		
		protected BigDecimal assignOutput(BigDecimal n, C21Paths t, List<String> many) {
			n = BigDecimal.valueOf(MapperC.<String>of(many).resultCount());
			
			return n;
		}
	}
}
