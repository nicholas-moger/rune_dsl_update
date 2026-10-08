package chaos.s05.a5uni.validation;

import chaos.s05.a5uni.C5Item;
import chaos.s05.a5uni.metafields.ReferenceWithMetaC5Sub;
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

public class C5ItemValidator implements Validator<C5Item> {

	private List<ComparisonResult> getComparisonResults(C5Item o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("one", (String) o.getOne() != null ? 1 : 0, 1, 1), 
				checkCardinality("opt", (BigDecimal) o.getOpt() != null ? 1 : 0, 0, 1), 
				checkCardinality("subRef", (ReferenceWithMetaC5Sub) o.getSubRef() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C5Item o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C5Item", ValidationResult.ValidationType.CARDINALITY, "C5Item", path, "", res.getError());
				}
				return success("C5Item", ValidationResult.ValidationType.CARDINALITY, "C5Item", path, "");
			})
			.collect(toList());
	}

}
