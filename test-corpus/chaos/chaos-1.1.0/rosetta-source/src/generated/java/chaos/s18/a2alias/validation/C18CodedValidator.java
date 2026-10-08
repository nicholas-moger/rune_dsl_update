package chaos.s18.a2alias.validation;

import chaos.s18.a2alias.C18Coded;
import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.metafields.FieldWithMetaString;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class C18CodedValidator implements Validator<C18Coded> {

	private List<ComparisonResult> getComparisonResults(C18Coded o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("kind", (FieldWithMetaString) o.getKind() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C18Coded o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C18Coded", ValidationResult.ValidationType.CARDINALITY, "C18Coded", path, "", res.getError());
				}
				return success("C18Coded", ValidationResult.ValidationType.CARDINALITY, "C18Coded", path, "");
			})
			.collect(toList());
	}

}
