package chaos.s19.a2wild.h.validation;

import chaos.s19.a2wild.h.C19Part;
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

public class C19PartValidator implements Validator<C19Part> {

	private List<ComparisonResult> getComparisonResults(C19Part o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("pid", (String) o.getPid() != null ? 1 : 0, 1, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C19Part o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C19Part", ValidationResult.ValidationType.CARDINALITY, "C19Part", path, "", res.getError());
				}
				return success("C19Part", ValidationResult.ValidationType.CARDINALITY, "C19Part", path, "");
			})
			.collect(toList());
	}

}
