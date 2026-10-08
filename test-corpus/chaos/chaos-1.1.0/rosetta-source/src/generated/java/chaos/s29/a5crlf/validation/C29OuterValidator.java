package chaos.s29.a5crlf.validation;

import chaos.s29.a5crlf.C29Note;
import chaos.s29.a5crlf.C29Outer;
import chaos.s29.a5crlf.C29Wrap;
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

public class C29OuterValidator implements Validator<C29Outer> {

	private List<ComparisonResult> getComparisonResults(C29Outer o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("C29Note", (C29Note) o.getC29Note() != null ? 1 : 0, 0, 1), 
				checkCardinality("C29Wrap", (C29Wrap) o.getC29Wrap() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C29Outer o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C29Outer", ValidationResult.ValidationType.CARDINALITY, "C29Outer", path, "", res.getError());
				}
				return success("C29Outer", ValidationResult.ValidationType.CARDINALITY, "C29Outer", path, "");
			})
			.collect(toList());
	}

}
