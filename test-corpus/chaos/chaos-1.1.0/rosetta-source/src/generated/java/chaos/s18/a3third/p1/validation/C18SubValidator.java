package chaos.s18.a3third.p1.validation;

import chaos.s18.a3third.p1.C18Sub;
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

public class C18SubValidator implements Validator<C18Sub> {

	private List<ComparisonResult> getComparisonResults(C18Sub o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("s", (String) o.getS() != null ? 1 : 0, 1, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C18Sub o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C18Sub", ValidationResult.ValidationType.CARDINALITY, "C18Sub", path, "", res.getError());
				}
				return success("C18Sub", ValidationResult.ValidationType.CARDINALITY, "C18Sub", path, "");
			})
			.collect(toList());
	}

}
