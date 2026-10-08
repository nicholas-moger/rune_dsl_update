package holdout.voiddeeptok.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.metafields.FieldWithMetaVoid;
import holdout.voiddeeptok.HolderA;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class HolderAValidator implements Validator<HolderA> {

	private List<ComparisonResult> getComparisonResults(HolderA o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("tok", (FieldWithMetaVoid) o.getTok() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, HolderA o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("HolderA", ValidationResult.ValidationType.CARDINALITY, "HolderA", path, "", res.getError());
				}
				return success("HolderA", ValidationResult.ValidationType.CARDINALITY, "HolderA", path, "");
			})
			.collect(toList());
	}

}
