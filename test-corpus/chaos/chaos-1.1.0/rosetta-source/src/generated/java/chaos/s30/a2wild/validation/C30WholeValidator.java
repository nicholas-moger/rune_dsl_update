package chaos.s30.a2wild.validation;

import chaos.s30.a2wild.C30Whole;
import chaos.s30.a2wild.h.C30Part;
import chaos.s30.a2wild.h.metafields.ReferenceWithMetaC30Part;
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

public class C30WholeValidator implements Validator<C30Whole> {

	private List<ComparisonResult> getComparisonResults(C30Whole o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("name", (String) o.getName() != null ? 1 : 0, 1, 1), 
				checkCardinality("code", (FieldWithMetaString) o.getCode() != null ? 1 : 0, 0, 1), 
				checkCardinality("part", (C30Part) o.getPart() != null ? 1 : 0, 0, 1), 
				checkCardinality("partRef", (ReferenceWithMetaC30Part) o.getPartRef() != null ? 1 : 0, 0, 1), 
				checkCardinality("nested", (C30Whole) o.getNested() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C30Whole o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C30Whole", ValidationResult.ValidationType.CARDINALITY, "C30Whole", path, "", res.getError());
				}
				return success("C30Whole", ValidationResult.ValidationType.CARDINALITY, "C30Whole", path, "");
			})
			.collect(toList());
	}

}
