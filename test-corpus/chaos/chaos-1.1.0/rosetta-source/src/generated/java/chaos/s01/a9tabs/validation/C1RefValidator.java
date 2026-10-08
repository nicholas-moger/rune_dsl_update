package chaos.s01.a9tabs.validation;

import chaos.s01.a9tabs.C1Ref;
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

public class C1RefValidator implements Validator<C1Ref> {

	private List<ComparisonResult> getComparisonResults(C1Ref o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("refCode", (String) o.getRefCode() != null ? 1 : 0, 1, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C1Ref o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C1Ref", ValidationResult.ValidationType.CARDINALITY, "C1Ref", path, "", res.getError());
				}
				return success("C1Ref", ValidationResult.ValidationType.CARDINALITY, "C1Ref", path, "");
			})
			.collect(toList());
	}

}
