package chaos.s07.a4none.validation;

import chaos.s07.a4none.C7Extra;
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

public class C7ExtraValidator implements Validator<C7Extra> {

	private List<ComparisonResult> getComparisonResults(C7Extra o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("memo", (String) o.getMemo() != null ? 1 : 0, 1, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C7Extra o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C7Extra", ValidationResult.ValidationType.CARDINALITY, "C7Extra", path, "", res.getError());
				}
				return success("C7Extra", ValidationResult.ValidationType.CARDINALITY, "C7Extra", path, "");
			})
			.collect(toList());
	}

}
