package chaos.s26.a3third.p3.validation;

import chaos.s26.a3third.p1.C26KindEnum;
import chaos.s26.a3third.p3.C26Bag;
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

public class C26BagValidator implements Validator<C26Bag> {

	private List<ComparisonResult> getComparisonResults(C26Bag o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("kind", (C26KindEnum) o.getKind() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C26Bag o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C26Bag", ValidationResult.ValidationType.CARDINALITY, "C26Bag", path, "", res.getError());
				}
				return success("C26Bag", ValidationResult.ValidationType.CARDINALITY, "C26Bag", path, "");
			})
			.collect(toList());
	}

}
