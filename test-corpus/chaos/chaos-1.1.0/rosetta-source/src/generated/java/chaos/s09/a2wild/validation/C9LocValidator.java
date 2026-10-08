package chaos.s09.a2wild.validation;

import chaos.s09.a2wild.C9Loc;
import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.metafields.FieldWithMetaString;
import com.rosetta.model.metafields.ReferenceWithMetaString;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class C9LocValidator implements Validator<C9Loc> {

	private List<ComparisonResult> getComparisonResults(C9Loc o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("spot", (FieldWithMetaString) o.getSpot() != null ? 1 : 0, 0, 1), 
				checkCardinality("ptr", (ReferenceWithMetaString) o.getPtr() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C9Loc o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C9Loc", ValidationResult.ValidationType.CARDINALITY, "C9Loc", path, "", res.getError());
				}
				return success("C9Loc", ValidationResult.ValidationType.CARDINALITY, "C9Loc", path, "");
			})
			.collect(toList());
	}

}
