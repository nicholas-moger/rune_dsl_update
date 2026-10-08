package chaos.s27.a2dangle.validation.datarule;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.annotations.RosettaDataRule;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

/**
 * @version 1.0.0
 */
@RosettaDataRule("C27NatOps")
@ImplementedBy(C27NatOps.Default.class)
public interface C27NatOps extends Validator<Integer> {
	
	String NAME = "C27NatOps";
	String DEFINITION = "([item, 0] then max) >= 0 and (if item > 100 then item < 1000 else item >= 0) and (item switch 0 then True, default (item > 0)) and item exists and [item] contains item";
	
	class Default implements C27NatOps {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Integer c27Nat) {
			ComparisonResult result = executeDataRule(c27Nat);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "C27Nat", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "C27Nat", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(Integer c27Nat) {
			try {
				final MapperC<Integer> thenArg = MapperC.<Integer>of(MapperS.of(c27Nat), MapperS.of(0));
				final ComparisonResult ifThenElseResult0;
				if (greaterThan(MapperS.of(c27Nat), MapperS.of(100), CardinalityOperator.All).getOrDefault(false)) {
					ifThenElseResult0 = lessThan(MapperS.of(c27Nat), MapperS.of(1000), CardinalityOperator.All);
				} else {
					ifThenElseResult0 = greaterThanEquals(MapperS.of(c27Nat), MapperS.of(0), CardinalityOperator.All);
				}
				final MapperS<Integer> switchArgument = MapperS.of(c27Nat);
				final ComparisonResult ifThenElseResult1;
				if (switchArgument.get() == null) {
					ifThenElseResult1 = ComparisonResult.ofEmpty();
				} else if (areEqual(switchArgument, MapperS.of(0), CardinalityOperator.All).get()) {
					ifThenElseResult1 = ComparisonResult.ofNullSafe(MapperS.of(true));
				} else {
					ifThenElseResult1 = greaterThan(MapperS.of(c27Nat), MapperS.of(0), CardinalityOperator.All);
				}
				return greaterThanEquals(thenArg
					.max(), MapperS.of(0), CardinalityOperator.All).andNullSafe(ifThenElseResult0).andNullSafe(ifThenElseResult1).andNullSafe(exists(MapperS.of(c27Nat))).andNullSafe(contains(MapperC.<Integer>of(MapperS.of(c27Nat)), MapperS.of(c27Nat)));
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements C27NatOps {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Integer c27Nat) {
			return Collections.emptyList();
		}
	}
}
