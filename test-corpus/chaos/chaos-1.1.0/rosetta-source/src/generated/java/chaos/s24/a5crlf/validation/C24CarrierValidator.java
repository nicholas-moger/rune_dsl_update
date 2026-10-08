package chaos.s24.a5crlf.validation;

import chaos.s24.a5crlf.C24Carrier;
import chaos.s24.a5crlf.C24Ref;
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

public class C24CarrierValidator implements Validator<C24Carrier> {

	private List<ComparisonResult> getComparisonResults(C24Carrier o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("tok", (Void) o.getTok() != null ? 1 : 0, 0, 1), 
				checkCardinality("coded", (FieldWithMetaVoid) o.getCoded() != null ? 1 : 0, 0, 1), 
				checkCardinality("name", (String) o.getName() != null ? 1 : 0, 0, 1), 
				checkCardinality("flag", (Boolean) o.getFlag() != null ? 1 : 0, 0, 1), 
				checkCardinality("ref", (C24Ref) o.getRef() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C24Carrier o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C24Carrier", ValidationResult.ValidationType.CARDINALITY, "C24Carrier", path, "", res.getError());
				}
				return success("C24Carrier", ValidationResult.ValidationType.CARDINALITY, "C24Carrier", path, "");
			})
			.collect(toList());
	}

}
