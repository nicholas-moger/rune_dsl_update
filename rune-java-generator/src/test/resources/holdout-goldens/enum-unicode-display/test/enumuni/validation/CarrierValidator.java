package test.enumuni.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.enumuni.Carrier;
import test.enumuni.DefinedEnum;
import test.enumuni.MixedEnum;
import test.enumuni.ProbeEnum;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class CarrierValidator implements Validator<Carrier> {

	private List<ComparisonResult> getComparisonResults(Carrier o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("p", (ProbeEnum) o.getP() != null ? 1 : 0, 0, 1), 
				checkCardinality("m", (MixedEnum) o.getM() != null ? 1 : 0, 0, 1), 
				checkCardinality("d", (DefinedEnum) o.getD() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Carrier o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Carrier", ValidationResult.ValidationType.CARDINALITY, "Carrier", path, "", res.getError());
				}
				return success("Carrier", ValidationResult.ValidationType.CARDINALITY, "Carrier", path, "");
			})
			.collect(toList());
	}

}
