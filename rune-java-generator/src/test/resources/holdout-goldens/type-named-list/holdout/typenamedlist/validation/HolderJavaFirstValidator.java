package holdout.typenamedlist.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.typenamedlist.HolderJavaFirst;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class HolderJavaFirstValidator implements Validator<HolderJavaFirst> {

	private List<ComparisonResult> getComparisonResults(HolderJavaFirst o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("subject", (holdout.typenamedlist.List) o.getSubject() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, HolderJavaFirst o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("HolderJavaFirst", ValidationResult.ValidationType.CARDINALITY, "HolderJavaFirst", path, "", res.getError());
				}
				return success("HolderJavaFirst", ValidationResult.ValidationType.CARDINALITY, "HolderJavaFirst", path, "");
			})
			.collect(toList());
	}

}
