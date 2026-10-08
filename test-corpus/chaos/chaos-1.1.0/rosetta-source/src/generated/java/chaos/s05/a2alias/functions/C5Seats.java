package chaos.s05.a2alias.functions;

import chaos.s05.a2alias.C5Item;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.functions.ConditionValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import javax.inject.Inject;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C5Seats.C5SeatsDefault.class)
public abstract class C5Seats implements RosettaFunction {
	
	@Inject protected ConditionValidator conditionValidator;

	/**
	* @param it 
	* @return r 
	*/
	public BigDecimal evaluate(C5Item it) {
		// pre-conditions
		conditionValidator.validate(() -> {
			final MapperS<BigDecimal> ifThenElseResult;
			if (exists(MapperS.of(it).<BigDecimal>map("getOpt", c5Item -> c5Item.getOpt())).getOrDefault(false)) {
				ifThenElseResult = MapperS.of(it).<BigDecimal>map("getOpt", c5Item -> c5Item.getOpt());
			} else {
				ifThenElseResult = MapperS.of(BigDecimal.valueOf(0));
			}
			return greaterThanEquals(ifThenElseResult, MapperS.of(BigDecimal.valueOf(0)), CardinalityOperator.All);
		},
			"");
		
		BigDecimal r = doEvaluate(it);
		
		return r;
	}

	protected abstract BigDecimal doEvaluate(C5Item it);

	protected abstract MapperS<BigDecimal> seatAlias(C5Item it);

	public static class C5SeatsDefault extends C5Seats {
		@Override
		protected BigDecimal doEvaluate(C5Item it) {
			BigDecimal r = null;
			return assignOutput(r, it);
		}
		
		protected BigDecimal assignOutput(BigDecimal r, C5Item it) {
			final MapperS<BigDecimal> ifThenElseResult;
			if (exists(MapperS.of(it).<BigDecimal>map("getOpt", c5Item -> c5Item.getOpt())).getOrDefault(false)) {
				ifThenElseResult = MapperS.of(it).<BigDecimal>map("getOpt", c5Item -> c5Item.getOpt());
			} else {
				ifThenElseResult = MapperS.of(BigDecimal.valueOf(0));
			}
			r = MapperMaths.<BigDecimal, BigDecimal, BigDecimal>add(ifThenElseResult, seatAlias(it)).get();
			
			return r;
		}
		
		@Override
		protected MapperS<BigDecimal> seatAlias(C5Item it) {
			if (exists(MapperS.of(it).<BigDecimal>map("getOpt", c5Item -> c5Item.getOpt())).getOrDefault(false)) {
				return MapperS.of(it).<BigDecimal>map("getOpt", c5Item -> c5Item.getOpt());
			}
			return MapperS.of(BigDecimal.valueOf(0));
		}
	}
}
