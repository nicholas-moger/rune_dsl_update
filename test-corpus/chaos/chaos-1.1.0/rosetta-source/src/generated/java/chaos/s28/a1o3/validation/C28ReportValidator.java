package chaos.s28.a1o3.validation;

import chaos.s28.a1o3.C28Report;
import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class C28ReportValidator implements Validator<C28Report> {

	private List<ComparisonResult> getComparisonResults(C28Report o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("utiField", (String) o.getUtiField() != null ? 1 : 0, 1, 1), 
				checkCardinality("avField", (String) o.getAvField() != null ? 1 : 0, 0, 1), 
				checkCardinality("venueField", (String) o.getVenueField() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C28Report o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C28Report", ValidationResult.ValidationType.CARDINALITY, "C28Report", path, "", res.getError());
				}
				return success("C28Report", ValidationResult.ValidationType.CARDINALITY, "C28Report", path, "");
			})
			.collect(toList());
	}

}
