package chaos.s05.a2qual.h.validation;

import chaos.s05.a2qual.h.C5Sub;
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

public class C5SubValidator implements Validator<C5Sub> {

	private List<ComparisonResult> getComparisonResults(C5Sub o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("name", (String) o.getName() != null ? 1 : 0, 1, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C5Sub o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C5Sub", ValidationResult.ValidationType.CARDINALITY, "C5Sub", path, "", res.getError());
				}
				return success("C5Sub", ValidationResult.ValidationType.CARDINALITY, "C5Sub", path, "");
			})
			.collect(toList());
	}

}
