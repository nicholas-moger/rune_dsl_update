package chaos.s03.a1o2.validation;

import chaos.s03.a1o2.C3Note;
import chaos.s03.a1o2.C3Outer;
import chaos.s03.a1o2.C3Wrap;
import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class C3OuterValidator implements Validator<C3Outer> {

	private List<ComparisonResult> getComparisonResults(C3Outer o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("C3Wrap", (C3Wrap) o.getC3Wrap() != null ? 1 : 0, 0, 1), 
				checkCardinality("C3Note", (C3Note) o.getC3Note() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C3Outer o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C3Outer", ValidationResult.ValidationType.CARDINALITY, "C3Outer", path, "", res.getError());
				}
				return success("C3Outer", ValidationResult.ValidationType.CARDINALITY, "C3Outer", path, "");
			})
			.collect(toList());
	}

}
