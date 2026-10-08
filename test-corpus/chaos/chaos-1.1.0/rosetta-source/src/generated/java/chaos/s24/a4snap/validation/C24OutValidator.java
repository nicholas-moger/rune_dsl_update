package chaos.s24.a4snap.validation;

import chaos.s24.a4snap.C24Carrier;
import chaos.s24.a4snap.C24Out;
import chaos.s24.a4snap.metafields.ReferenceWithMetaC24Keyed;
import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.metafields.FieldWithMetaVoid;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class C24OutValidator implements Validator<C24Out> {

	private List<ComparisonResult> getComparisonResults(C24Out o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("v", (Void) o.getV() != null ? 1 : 0, 0, 1), 
				checkCardinality("vm", (FieldWithMetaVoid) o.getVm() != null ? 1 : 0, 0, 1), 
				checkCardinality("kref", (ReferenceWithMetaC24Keyed) o.getKref() != null ? 1 : 0, 0, 1), 
				checkCardinality("s", (String) o.getS() != null ? 1 : 0, 0, 1), 
				checkCardinality("car", (C24Carrier) o.getCar() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C24Out o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C24Out", ValidationResult.ValidationType.CARDINALITY, "C24Out", path, "", res.getError());
				}
				return success("C24Out", ValidationResult.ValidationType.CARDINALITY, "C24Out", path, "");
			})
			.collect(toList());
	}

}
