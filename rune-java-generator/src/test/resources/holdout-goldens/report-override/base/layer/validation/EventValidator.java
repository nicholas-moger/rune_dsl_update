package base.layer.validation;

import base.layer.Event;
import base.layer.TradeEnum;
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

public class EventValidator implements Validator<Event> {

	private List<ComparisonResult> getComparisonResults(Event o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("trade", (TradeEnum) o.getTrade() != null ? 1 : 0, 1, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Event o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Event", ValidationResult.ValidationType.CARDINALITY, "Event", path, "", res.getError());
				}
				return success("Event", ValidationResult.ValidationType.CARDINALITY, "Event", path, "");
			})
			.collect(toList());
	}

}
