package chaos.s06.a5crlf.validation;

import chaos.s06.a5crlf.C6Tag;
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

public class C6TagValidator implements Validator<C6Tag> {

	private List<ComparisonResult> getComparisonResults(C6Tag o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("caption", (String) o.getCaption() != null ? 1 : 0, 1, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C6Tag o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C6Tag", ValidationResult.ValidationType.CARDINALITY, "C6Tag", path, "", res.getError());
				}
				return success("C6Tag", ValidationResult.ValidationType.CARDINALITY, "C6Tag", path, "");
			})
			.collect(toList());
	}

}
