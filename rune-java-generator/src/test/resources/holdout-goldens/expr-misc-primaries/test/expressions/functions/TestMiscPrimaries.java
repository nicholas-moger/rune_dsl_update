package test.expressions.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(TestMiscPrimaries.TestMiscPrimariesDefault.class)
public abstract class TestMiscPrimaries implements RosettaFunction {

	/**
	* @param a 
	* @param b 
	* @param items 
	* @return result 
	*/
	public BigDecimal evaluate(BigDecimal a, BigDecimal b, List<BigDecimal> items) {
		BigDecimal result = doEvaluate(a, b, items);
		
		return result;
	}

	protected abstract BigDecimal doEvaluate(BigDecimal a, BigDecimal b, List<BigDecimal> items);

	protected abstract MapperS<BigDecimal> paren(BigDecimal a, BigDecimal b, List<BigDecimal> items);

	protected abstract MapperS<Boolean> _notEqual(BigDecimal a, BigDecimal b, List<BigDecimal> items);

	protected abstract MapperS<BigDecimal> onlyEl(BigDecimal a, BigDecimal b, List<BigDecimal> items);

	protected abstract MapperS<Boolean> multiExists(BigDecimal a, BigDecimal b, List<BigDecimal> items);

	public static class TestMiscPrimariesDefault extends TestMiscPrimaries {
		@Override
		protected BigDecimal doEvaluate(BigDecimal a, BigDecimal b, List<BigDecimal> items) {
			if (items == null) {
				items = Collections.emptyList();
			}
			BigDecimal result = null;
			return assignOutput(result, a, b, items);
		}
		
		protected BigDecimal assignOutput(BigDecimal result, BigDecimal a, BigDecimal b, List<BigDecimal> items) {
			if (notEqual(MapperS.of(a), MapperS.of(BigDecimal.valueOf(0)), CardinalityOperator.Any).getOrDefault(false)) {
				result = paren(a, b, items).get();
			} else {
				result = BigDecimal.valueOf(0);
			}
			
			return result;
		}
		
		@Override
		protected MapperS<BigDecimal> paren(BigDecimal a, BigDecimal b, List<BigDecimal> items) {
			return MapperMaths.<BigDecimal, BigDecimal, BigDecimal>multiply(MapperMaths.<BigDecimal, BigDecimal, BigDecimal>add(MapperS.of(a), MapperS.of(b)), MapperS.of(BigDecimal.valueOf(2)));
		}
		
		@Override
		protected MapperS<Boolean> _notEqual(BigDecimal a, BigDecimal b, List<BigDecimal> items) {
			return notEqual(MapperS.of(a), MapperS.of(b), CardinalityOperator.Any).asMapper();
		}
		
		@Override
		protected MapperS<BigDecimal> onlyEl(BigDecimal a, BigDecimal b, List<BigDecimal> items) {
			return MapperS.of(MapperC.of(items).get());
		}
		
		@Override
		protected MapperS<Boolean> multiExists(BigDecimal a, BigDecimal b, List<BigDecimal> items) {
			return multipleExists(MapperC.<BigDecimal>of(items)).asMapper();
		}
	}
}
