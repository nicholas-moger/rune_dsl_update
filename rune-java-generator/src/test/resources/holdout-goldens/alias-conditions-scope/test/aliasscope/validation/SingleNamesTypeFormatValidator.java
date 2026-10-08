package test.aliasscope.validation;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import test.aliasscope.SingleNames;
import test.aliasscope.validation.datarule.NatNonNeg;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkNumber;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

public class SingleNamesTypeFormatValidator implements Validator<SingleNames> {
	@Inject
	protected NatNonNeg natNonNeg;

	private List<ComparisonResult> getComparisonResults(SingleNames o) {
		return Lists.<ComparisonResult>newArrayList(
				checkNumber("results", o.getResults(), empty(), of(0), empty(), empty()), 
				checkNumber("o", o.getO(), empty(), of(0), empty(), empty())
			);
	}
	
	private List<ValidationResult<?>> runConditions(RosettaPath path, SingleNames o) {
		List<ValidationResult<?>> results = new ArrayList();
		results.addAll(natNonNeg.getValidationResults(path.newSubPath("results"), o.getResults()));
		results.addAll(natNonNeg.getValidationResults(path.newSubPath("o"), o.getO()));
		return results;
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, SingleNames o) {
		return Streams.concat(getComparisonResults(o)
				.stream()
				.map(res -> {
					if (!isNullOrEmpty(res.getError())) {
						return failure("SingleNames", ValidationResult.ValidationType.TYPE_FORMAT, "SingleNames", path, "", res.getError());
					}
					return success("SingleNames", ValidationResult.ValidationType.TYPE_FORMAT, "SingleNames", path, "");
				}),
				runConditions(path, o).stream()
			)
			.collect(toList());
	}

}
