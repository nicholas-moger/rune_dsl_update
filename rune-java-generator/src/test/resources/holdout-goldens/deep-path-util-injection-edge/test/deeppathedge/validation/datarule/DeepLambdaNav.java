package test.deeppathedge.validation.datarule;

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
import javax.inject.Inject;
import test.deeppathedge.Deep;
import test.deeppathedge.Inner;
import test.deeppathedge.util.InnerDeepPathUtil;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

/**
 * @version 0.0.0
 */
@RosettaDataRule("DeepLambdaNav")
@ImplementedBy(DeepLambdaNav.Default.class)
public interface DeepLambdaNav extends Validator<Deep> {
	
	String NAME = "DeepLambdaNav";
	String DEFINITION = "if picks exists then (picks extract [ item ->> common ] then count) <= picks count";
	
	class Default implements DeepLambdaNav {
	
		@Inject protected InnerDeepPathUtil innerDeepPathUtil;
		
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Deep deep) {
			ComparisonResult result = executeDataRule(deep);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "Deep", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "Deep", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(Deep deep) {
			try {
				if (exists(MapperS.of(deep).<Inner>mapC("getPicks", _deep -> _deep.getPicks())).getOrDefault(false)) {
					final MapperC<String> thenArg = MapperS.of(deep).<Inner>mapC("getPicks", _deep -> _deep.getPicks())
						.mapItem(item -> item.<String>map("chooseCommon", inner -> innerDeepPathUtil.chooseCommon(inner)));
					return lessThanEquals(MapperS.of(thenArg.resultCount()), MapperS.of(MapperS.of(deep).<Inner>mapC("getPicks", _deep -> _deep.getPicks()).resultCount()), CardinalityOperator.All);
				}
				return ComparisonResult.ofEmpty();
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements DeepLambdaNav {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Deep deep) {
			return Collections.emptyList();
		}
	}
}
