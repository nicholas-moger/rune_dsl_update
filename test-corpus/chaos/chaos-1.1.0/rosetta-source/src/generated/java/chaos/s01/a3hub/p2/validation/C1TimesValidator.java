package chaos.s01.a3hub.p2.validation;

import chaos.s01.a3hub.p2.C1Times;
import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.records.Date;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class C1TimesValidator implements Validator<C1Times> {

	private List<ComparisonResult> getComparisonResults(C1Times o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("d", (Date) o.getD() != null ? 1 : 0, 0, 1), 
				checkCardinality("t", (LocalTime) o.getT() != null ? 1 : 0, 0, 1), 
				checkCardinality("dt", (LocalDateTime) o.getDt() != null ? 1 : 0, 0, 1), 
				checkCardinality("z", (ZonedDateTime) o.getZ() != null ? 1 : 0, 0, 1), 
				checkCardinality("stamp", (LocalDateTime) o.getStamp() != null ? 1 : 0, 1, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C1Times o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C1Times", ValidationResult.ValidationType.CARDINALITY, "C1Times", path, "", res.getError());
				}
				return success("C1Times", ValidationResult.ValidationType.CARDINALITY, "C1Times", path, "");
			})
			.collect(toList());
	}

}
