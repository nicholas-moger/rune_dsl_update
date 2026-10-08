package chaos.s08.a5uni.validation;

import chaos.s08.a5uni.C8Box;
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

public class C8BoxValidator implements Validator<C8Box> {

	private List<ComparisonResult> getComparisonResults(C8Box o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("lid", (String) o.getLid() != null ? 1 : 0, 1, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C8Box o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C8Box", ValidationResult.ValidationType.CARDINALITY, "C8Box", path, "", res.getError());
				}
				return success("C8Box", ValidationResult.ValidationType.CARDINALITY, "C8Box", path, "");
			})
			.collect(toList());
	}

}
