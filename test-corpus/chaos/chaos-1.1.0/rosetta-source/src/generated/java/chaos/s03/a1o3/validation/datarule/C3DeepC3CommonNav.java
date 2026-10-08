package chaos.s03.a1o3.validation.datarule;

import chaos.s03.a1o3.C3Deep;
import chaos.s03.a1o3.C3Inner;
import chaos.s03.a1o3.util.C3InnerDeepPathUtil;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.annotations.RosettaDataRule;
import com.rosetta.model.lib.expression.CardinalityOperator;
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
@RosettaDataRule("C3DeepC3CommonNav")
@ImplementedBy(C3DeepC3CommonNav.Default.class)
public interface C3DeepC3CommonNav extends Validator<C3Deep> {
	
	String NAME = "C3DeepC3CommonNav";
	String DEFINITION = "if picks exists then (picks ->> common) count <= picks count";
	
	class Default implements C3DeepC3CommonNav {
	
		@Inject protected C3InnerDeepPathUtil c3InnerDeepPathUtil;
		
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
				if (exists(MapperS.of(c3Deep).<C3Inner>mapC("getPicks", _c3Deep -> _c3Deep.getPicks())).getOrDefault(false)) {
					return lessThanEquals(MapperS.of(MapperS.of(c3Deep).<C3Inner>mapC("getPicks", _c3Deep -> _c3Deep.getPicks()).<String>map("chooseCommon", c3Inner -> c3InnerDeepPathUtil.chooseCommon(c3Inner)).resultCount()), MapperS.of(MapperS.of(c3Deep).<C3Inner>mapC("getPicks", _c3Deep -> _c3Deep.getPicks()).resultCount()), CardinalityOperator.All);
				}
				return ComparisonResult.ofEmpty();
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements C3DeepC3CommonNav {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C3Deep c3Deep) {
			return Collections.emptyList();
		}
	}
}
