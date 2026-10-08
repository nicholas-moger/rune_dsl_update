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
import test.expressions.Account;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(ComplexExpression.ComplexExpressionDefault.class)
public abstract class ComplexExpression implements RosettaFunction {

	/**
	* @param accounts 
	* @param threshold 
	* @return result 
	*/
	public BigDecimal evaluate(List<? extends Account> accounts, BigDecimal threshold) {
		BigDecimal result = doEvaluate(accounts, threshold);
		
		return result;
	}

	protected abstract BigDecimal doEvaluate(List<? extends Account> accounts, BigDecimal threshold);

	protected abstract MapperC<? extends Account> activeAccounts(List<? extends Account> accounts, BigDecimal threshold);

	protected abstract MapperC<BigDecimal> balances(List<? extends Account> accounts, BigDecimal threshold);

	protected abstract MapperS<BigDecimal> total(List<? extends Account> accounts, BigDecimal threshold);

	public static class ComplexExpressionDefault extends ComplexExpression {
		@Override
		protected BigDecimal doEvaluate(List<? extends Account> accounts, BigDecimal threshold) {
			if (accounts == null) {
				accounts = Collections.emptyList();
			}
			BigDecimal result = null;
			return assignOutput(result, accounts, threshold);
		}
		
		protected BigDecimal assignOutput(BigDecimal result, List<? extends Account> accounts, BigDecimal threshold) {
			if (greaterThan(total(accounts, threshold), MapperS.of(threshold), CardinalityOperator.All).getOrDefault(false)) {
				result = MapperMaths.<BigDecimal, BigDecimal, BigDecimal>subtract(total(accounts, threshold), MapperS.of(threshold)).get();
			} else {
				result = BigDecimal.valueOf(0);
			}
			
			return result;
		}
		
		@Override
		protected MapperC<? extends Account> activeAccounts(List<? extends Account> accounts, BigDecimal threshold) {
			return MapperC.<Account>of(accounts)
				.filterItemNullSafe(item -> areEqual(item.<Boolean>map("getActive", account -> account.getActive()), MapperS.of(true), CardinalityOperator.All).get());
		}
		
		@Override
		protected MapperC<BigDecimal> balances(List<? extends Account> accounts, BigDecimal threshold) {
			return activeAccounts(accounts, threshold)
				.mapItem(item -> item.<BigDecimal>map("getBalance", account -> account.getBalance()));
		}
		
		@Override
		protected MapperS<BigDecimal> total(List<? extends Account> accounts, BigDecimal threshold) {
			return balances(accounts, threshold)
				.sumBigDecimal();
		}
	}
}
