package chaos.s26.a3half.p2.validation;

import chaos.s26.a3half.p2.C26OptB;
import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.math.BigDecimal;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class C26OptBValidator implements Validator<C26OptB> {

	private List<ComparisonResult> getComparisonResults(C26OptB o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("bv", (BigDecimal) o.getBv() != null ? 1 : 0, 1, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C26OptB o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C26OptB", ValidationResult.ValidationType.CARDINALITY, "C26OptB", path, "", res.getError());
				}
				return success("C26OptB", ValidationResult.ValidationType.CARDINALITY, "C26OptB", path, "");
			})
			.collect(toList());
	}

}
