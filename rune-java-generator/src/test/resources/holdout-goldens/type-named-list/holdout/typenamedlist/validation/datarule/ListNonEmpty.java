package holdout.typenamedlist.validation.datarule;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.annotations.RosettaDataRule;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.typenamedlist.List;
import java.util.Arrays;
import java.util.Collections;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

/**
 * @version 0.0.0
 */
@RosettaDataRule("ListNonEmpty")
@ImplementedBy(ListNonEmpty.Default.class)
public interface ListNonEmpty extends Validator<List> {
	
	String NAME = "ListNonEmpty";
	String DEFINITION = "items exists";
	
	class Default implements ListNonEmpty {
	
		@Override
		public java.util.List<ValidationResult<?>> getValidationResults(RosettaPath path, List list) {
			ComparisonResult result = executeDataRule(list);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "List", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "List", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(List list) {
			try {
				return exists(MapperS.of(list).<String>mapC("getItems", _list -> _list.getItems()));
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements ListNonEmpty {
	
		@Override
		public java.util.List<ValidationResult<?>> getValidationResults(RosettaPath path, List list) {
			return Collections.emptyList();
		}
	}
}
