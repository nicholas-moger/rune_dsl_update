package test.aliasheader.validation;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import test.aliasheader.PayLoad;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkNumber;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

public class PayLoadTypeFormatValidator implements Validator<PayLoad> {
	@Inject
	protected test.aliasheader.validation.datarule.PayLoad payLoad;

	private List<ComparisonResult> getComparisonResults(PayLoad o) {
		return Lists.<ComparisonResult>newArrayList(
				checkNumber("pay", o.getPay(), empty(), of(0), empty(), empty()), 
				checkNumber("pays", o.getPays(), empty(), of(0), empty(), empty())
			);
	}
	
	private List<ValidationResult<?>> runConditions(RosettaPath path, PayLoad o) {
		List<ValidationResult<?>> results = new ArrayList();
		results.addAll(payLoad.getValidationResults(path.newSubPath("pay"), o.getPay()));
		final List<Integer> pays = o.getPays();
		if (pays != null) {
			for (int i = 0; i < pays.size(); i++) {
				results.addAll(payLoad.getValidationResults(path.newSubPath("pays").withIndex(i), pays.get(i)));
			}
		}
		return results;
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, PayLoad o) {
		return Streams.concat(getComparisonResults(o)
				.stream()
				.map(res -> {
					if (!isNullOrEmpty(res.getError())) {
						return failure("PayLoad", ValidationResult.ValidationType.TYPE_FORMAT, "PayLoad", path, "", res.getError());
					}
					return success("PayLoad", ValidationResult.ValidationType.TYPE_FORMAT, "PayLoad", path, "");
				}),
				runConditions(path, o).stream()
			)
			.collect(toList());
	}

}
