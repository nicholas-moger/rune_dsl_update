package chaos.s03.a3half.p2.validation.datarule;

import chaos.s03.a3half.p2.C3Deep;
import chaos.s03.a3half.p2.C3Outer;
import chaos.s03.a3half.p2.util.C3OuterDeepPathUtil;
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
@RosettaDataRule("C3DeepC3TextWhenPicked")
@ImplementedBy(C3DeepC3TextWhenPicked.Default.class)
public interface C3DeepC3TextWhenPicked extends Validator<C3Deep> {
	
	String NAME = "C3DeepC3TextWhenPicked";
	String DEFINITION = "if pick exists then pick ->> text exists";
	
	class Default implements C3DeepC3TextWhenPicked {
	
		@Inject protected C3OuterDeepPathUtil c3OuterDeepPathUtil;
		
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C3Deep c3Deep) {
			ComparisonResult result = executeDataRule(c3Deep);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "C3Deep", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "C3Deep", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(C3Deep c3Deep) {
			try {
				if (exists(MapperS.of(c3Deep).<C3Outer>map("getPick", _c3Deep -> _c3Deep.getPick())).getOrDefault(false)) {
					return exists(MapperS.of(c3Deep).<C3Outer>map("getPick", _c3Deep -> _c3Deep.getPick()).<String>map("chooseText", c3Outer -> c3OuterDeepPathUtil.chooseText(c3Outer)));
				}
				return ComparisonResult.ofEmpty();
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements C3DeepC3TextWhenPicked {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C3Deep c3Deep) {
			return Collections.emptyList();
		}
	}
}
