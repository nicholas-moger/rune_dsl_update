package test.aliasheader.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.aliasheader.Injected;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class InjectedValidator implements Validator<Injected> {

	private List<ComparisonResult> getComparisonResults(Injected o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("inj", (Integer) o.getInj() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Injected o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Injected", ValidationResult.ValidationType.CARDINALITY, "Injected", path, "", res.getError());
				}
				return success("Injected", ValidationResult.ValidationType.CARDINALITY, "Injected", path, "");
			})
			.collect(toList());
	}

}
