package chaos.s09.a3half.p2.validation;

import chaos.s09.a3half.p1.C9Keyed;
import chaos.s09.a3half.p1.C9Plain;
import chaos.s09.a3half.p1.metafields.ReferenceWithMetaC9Keyed;
import chaos.s09.a3half.p2.C9Holder;
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

public class C9HolderValidator implements Validator<C9Holder> {

	private List<ComparisonResult> getComparisonResults(C9Holder o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("direct", (C9Keyed) o.getDirect() != null ? 1 : 0, 0, 1), 
				checkCardinality("byRef", (ReferenceWithMetaC9Keyed) o.getByRef() != null ? 1 : 0, 0, 1), 
				checkCardinality("coded", (FieldWithMetaString) o.getCoded() != null ? 1 : 0, 0, 1), 
				checkCardinality("marked", (FieldWithMetaString) o.getMarked() != null ? 1 : 0, 0, 1), 
				checkCardinality("plain", (C9Plain) o.getPlain() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C9Holder o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C9Holder", ValidationResult.ValidationType.CARDINALITY, "C9Holder", path, "", res.getError());
				}
				return success("C9Holder", ValidationResult.ValidationType.CARDINALITY, "C9Holder", path, "");
			})
			.collect(toList());
	}

}
