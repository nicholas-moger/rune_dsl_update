package test.deeppath.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.deeppath.Note;
import test.deeppath.Outer;
import test.deeppath.Wrap;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class OuterValidator implements Validator<Outer> {

	private List<ComparisonResult> getComparisonResults(Outer o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("Wrap", (Wrap) o.getWrap() != null ? 1 : 0, 0, 1), 
				checkCardinality("Note", (Note) o.getNote() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Outer o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Outer", ValidationResult.ValidationType.CARDINALITY, "Outer", path, "", res.getError());
				}
				return success("Outer", ValidationResult.ValidationType.CARDINALITY, "Outer", path, "");
			})
			.collect(toList());
	}

}
