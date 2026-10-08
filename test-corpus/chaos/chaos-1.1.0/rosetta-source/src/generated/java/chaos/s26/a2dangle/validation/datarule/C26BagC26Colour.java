package chaos.s26.a2dangle.validation.datarule;

import chaos.s26.a2dangle.C26Bag;
import chaos.s26.a2dangle.C26KindEnum;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.annotations.RosettaDataRule;
import com.rosetta.model.lib.expression.ComparisonResult;
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
@RosettaDataRule("C26BagC26Colour")
@ImplementedBy(C26BagC26Colour.Default.class)
public interface C26BagC26Colour extends Validator<C26Bag> {
	
	String NAME = "C26BagC26Colour";
	String DEFINITION = "if kind exists then (kind switch Red then True, Green then True, default False)";
	
	class Default implements C26BagC26Colour {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C26Bag c26Bag) {
			ComparisonResult result = executeDataRule(c26Bag);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "C26Bag", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "C26Bag", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(C26Bag c26Bag) {
			try {
				if (exists(MapperS.of(c26Bag).<C26KindEnum>map("getKind", _c26Bag -> _c26Bag.getKind())).getOrDefault(false)) {
					final C26KindEnum switchArgument = MapperS.of(c26Bag).<C26KindEnum>map("getKind", _c26Bag -> _c26Bag.getKind()).get();
					if (switchArgument == null) {
						return ComparisonResult.ofEmpty();
					}
					if (switchArgument == C26KindEnum.RED) {
						return ComparisonResult.ofNullSafe(MapperS.of(true));
					}
					if (switchArgument == C26KindEnum.GREEN) {
						return ComparisonResult.ofNullSafe(MapperS.of(true));
					}
					return ComparisonResult.ofNullSafe(MapperS.of(false));
				}
				return ComparisonResult.ofEmpty();
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements C26BagC26Colour {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C26Bag c26Bag) {
			return Collections.emptyList();
		}
	}
}
