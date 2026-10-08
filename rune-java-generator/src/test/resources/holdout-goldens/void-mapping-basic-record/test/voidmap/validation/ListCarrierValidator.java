package test.voidmap.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.voidmap.ListCarrier;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class ListCarrierValidator implements Validator<ListCarrier> {

	private List<ComparisonResult> getComparisonResults(ListCarrier o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, ListCarrier o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("ListCarrier", ValidationResult.ValidationType.CARDINALITY, "ListCarrier", path, "", res.getError());
				}
				return success("ListCarrier", ValidationResult.ValidationType.CARDINALITY, "ListCarrier", path, "");
			})
			.collect(toList());
	}

}
