package cde.layer.validation;

import cde.layer.CriticalDataElement;
import cde.layer.price.NotationEnum;
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

public class CriticalDataElementValidator implements Validator<CriticalDataElement> {

	private List<ComparisonResult> getComparisonResults(CriticalDataElement o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("notation", (NotationEnum) o.getNotation() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, CriticalDataElement o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("CriticalDataElement", ValidationResult.ValidationType.CARDINALITY, "CriticalDataElement", path, "", res.getError());
				}
				return success("CriticalDataElement", ValidationResult.ValidationType.CARDINALITY, "CriticalDataElement", path, "");
			})
			.collect(toList());
	}

}
