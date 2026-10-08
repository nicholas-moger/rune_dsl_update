package chaos.s27.base.validation;

import chaos.s27.base.C27Ref;
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

public class C27RefValidator implements Validator<C27Ref> {

	private List<ComparisonResult> getComparisonResults(C27Ref o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("caption", (String) o.getCaption() != null ? 1 : 0, 1, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C27Ref o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C27Ref", ValidationResult.ValidationType.CARDINALITY, "C27Ref", path, "", res.getError());
				}
				return success("C27Ref", ValidationResult.ValidationType.CARDINALITY, "C27Ref", path, "");
			})
			.collect(toList());
	}

}
