package holdout.closureparamduplicatereads.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.closureparamduplicatereads.Item;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class ItemTypeFormatValidator implements Validator<Item> {

	private List<ComparisonResult> getComparisonResults(Item o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Item o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Item", ValidationResult.ValidationType.TYPE_FORMAT, "Item", path, "", res.getError());
				}
				return success("Item", ValidationResult.ValidationType.TYPE_FORMAT, "Item", path, "");
			})
			.collect(toList());
	}

}
