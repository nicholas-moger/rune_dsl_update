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
import test.aliasheader.Injected;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkNumber;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

public class InjectedTypeFormatValidator implements Validator<Injected> {
	@Inject
	protected test.aliasheader.validation.datarule.Inject inject;

	private List<ComparisonResult> getComparisonResults(Injected o) {
		return Lists.<ComparisonResult>newArrayList(
				checkNumber("inj", o.getInj(), empty(), of(0), empty(), empty()), 
				checkNumber("injs", o.getInjs(), empty(), of(0), empty(), empty())
			);
	}
	
	private List<ValidationResult<?>> runConditions(RosettaPath path, Injected o) {
		List<ValidationResult<?>> results = new ArrayList();
		results.addAll(inject.getValidationResults(path.newSubPath("inj"), o.getInj()));
		final List<Integer> injs = o.getInjs();
		if (injs != null) {
			for (int i = 0; i < injs.size(); i++) {
				results.addAll(inject.getValidationResults(path.newSubPath("injs").withIndex(i), injs.get(i)));
			}
		}
		return results;
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Injected o) {
		return Streams.concat(getComparisonResults(o)
				.stream()
				.map(res -> {
					if (!isNullOrEmpty(res.getError())) {
						return failure("Injected", ValidationResult.ValidationType.TYPE_FORMAT, "Injected", path, "", res.getError());
					}
					return success("Injected", ValidationResult.ValidationType.TYPE_FORMAT, "Injected", path, "");
				}),
				runConditions(path, o).stream()
			)
			.collect(toList());
	}

}
