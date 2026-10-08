package chaos.s19.a1o2.validation;

import chaos.s19.a1o2.C19Part;
import chaos.s19.a1o2.C19Whole;
import chaos.s19.a1o2.metafields.ReferenceWithMetaC19Part;
import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.metafields.FieldWithMetaString;
import com.rosetta.model.metafields.ReferenceWithMetaString;
import java.math.BigDecimal;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class C19WholeValidator implements Validator<C19Whole> {

	private List<ComparisonResult> getComparisonResults(C19Whole o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("name", (String) o.getName() != null ? 1 : 0, 1, 1), 
				checkCardinality("opt", (BigDecimal) o.getOpt() != null ? 1 : 0, 0, 1), 
				checkCardinality("part", (C19Part) o.getPart() != null ? 1 : 0, 0, 1), 
				checkCardinality("partRef", (ReferenceWithMetaC19Part) o.getPartRef() != null ? 1 : 0, 0, 1), 
				checkCardinality("spot", (FieldWithMetaString) o.getSpot() != null ? 1 : 0, 0, 1), 
				checkCardinality("ptr", (ReferenceWithMetaString) o.getPtr() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C19Whole o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C19Whole", ValidationResult.ValidationType.CARDINALITY, "C19Whole", path, "", res.getError());
				}
				return success("C19Whole", ValidationResult.ValidationType.CARDINALITY, "C19Whole", path, "");
			})
			.collect(toList());
	}

}
