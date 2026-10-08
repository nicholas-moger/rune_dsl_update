package chaos.s29.a3half.p1.validation;

import chaos.s29.a3half.p1.C29Note;
import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.metafields.FieldWithMetaString;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class C29NoteValidator implements Validator<C29Note> {

	private List<ComparisonResult> getComparisonResults(C29Note o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("text", (String) o.getText() != null ? 1 : 0, 0, 1), 
				checkCardinality("code", (FieldWithMetaString) o.getCode() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C29Note o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C29Note", ValidationResult.ValidationType.CARDINALITY, "C29Note", path, "", res.getError());
				}
				return success("C29Note", ValidationResult.ValidationType.CARDINALITY, "C29Note", path, "");
			})
			.collect(toList());
	}

}
