package chaos.s33.a1o2.validation;

import chaos.s33.a1o2.C33Extra;
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

public class C33ExtraValidator implements Validator<C33Extra> {

	private List<ComparisonResult> getComparisonResults(C33Extra o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("memo", (String) o.getMemo() != null ? 1 : 0, 1, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C33Extra o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C33Extra", ValidationResult.ValidationType.CARDINALITY, "C33Extra", path, "", res.getError());
				}
				return success("C33Extra", ValidationResult.ValidationType.CARDINALITY, "C33Extra", path, "");
			})
			.collect(toList());
	}

}
