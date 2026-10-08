package chaos.s29.a1o3.validation.datarule;

import chaos.s29.a1o3.C29Bag;
import chaos.s29.a1o3.C29Outer;
import chaos.s29.a1o3.util.C29OuterDeepPathUtil;
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
import javax.inject.Inject;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

/**
 * @version 1.0.0
 */
@RosettaDataRule("C29BagC29DeepCond")
@ImplementedBy(C29BagC29DeepCond.Default.class)
public interface C29BagC29DeepCond extends Validator<C29Bag> {
	
	String NAME = "C29BagC29DeepCond";
	String DEFINITION = "if outer exists then outer ->> text exists";
	
	class Default implements C29BagC29DeepCond {
	
		@Inject protected C29OuterDeepPathUtil c29OuterDeepPathUtil;
		
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C29Bag c29Bag) {
			ComparisonResult result = executeDataRule(c29Bag);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "C29Bag", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "C29Bag", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(C29Bag c29Bag) {
			try {
				if (exists(MapperS.of(c29Bag).<C29Outer>map("getOuter", _c29Bag -> _c29Bag.getOuter())).getOrDefault(false)) {
					return exists(MapperS.of(c29Bag).<C29Outer>map("getOuter", _c29Bag -> _c29Bag.getOuter()).<String>map("chooseText", c29Outer -> c29OuterDeepPathUtil.chooseText(c29Outer)));
				}
				return ComparisonResult.ofEmpty();
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements C29BagC29DeepCond {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C29Bag c29Bag) {
			return Collections.emptyList();
		}
	}
}
