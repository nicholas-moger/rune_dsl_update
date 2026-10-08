package holdout.voiddeeptok.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.metafields.FieldWithMetaVoid;
import holdout.voiddeeptok.HolderB;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class HolderBValidator implements Validator<HolderB> {

	private List<ComparisonResult> getComparisonResults(HolderB o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("tok", (FieldWithMetaVoid) o.getTok() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, HolderB o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("HolderB", ValidationResult.ValidationType.CARDINALITY, "HolderB", path, "", res.getError());
				}
				return success("HolderB", ValidationResult.ValidationType.CARDINALITY, "HolderB", path, "");
			})
			.collect(toList());
	}

}
