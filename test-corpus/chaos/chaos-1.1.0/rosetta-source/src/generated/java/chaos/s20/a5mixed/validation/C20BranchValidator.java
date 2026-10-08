package chaos.s20.a5mixed.validation;

import chaos.s20.a5mixed.C20Branch;
import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class C20BranchValidator implements Validator<C20Branch> {

	private List<ComparisonResult> getComparisonResults(C20Branch o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C20Branch o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C20Branch", ValidationResult.ValidationType.CARDINALITY, "C20Branch", path, "", res.getError());
				}
				return success("C20Branch", ValidationResult.ValidationType.CARDINALITY, "C20Branch", path, "");
			})
			.collect(toList());
	}

}
