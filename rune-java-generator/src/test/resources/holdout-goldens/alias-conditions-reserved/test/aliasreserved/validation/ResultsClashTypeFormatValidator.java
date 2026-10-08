package test.aliasreserved.validation;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import test.aliasreserved.ResultsClash;
import test.aliasreserved.validation.datarule.Results;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkNumber;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

public class ResultsClashTypeFormatValidator implements Validator<ResultsClash> {
	@Inject
	protected Results results;

	private List<ComparisonResult> getComparisonResults(ResultsClash o) {
		return Lists.<ComparisonResult>newArrayList(
				checkNumber("re", o.getRe(), empty(), of(0), empty(), empty())
			);
	}
	
	private List<ValidationResult<?>> runConditions(RosettaPath path, ResultsClash o) {
		List<ValidationResult<?>> _results = new ArrayList();
		final List<Integer> re = o.getRe();
		if (re != null) {
			for (int i = 0; i < re.size(); i++) {
				_results.addAll(results.getValidationResults(path.newSubPath("re").withIndex(i), re.get(i)));
			}
		}
		return _results;
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, ResultsClash o) {
		return Streams.concat(getComparisonResults(o)
				.stream()
				.map(res -> {
					if (!isNullOrEmpty(res.getError())) {
						return failure("ResultsClash", ValidationResult.ValidationType.TYPE_FORMAT, "ResultsClash", path, "", res.getError());
					}
					return success("ResultsClash", ValidationResult.ValidationType.TYPE_FORMAT, "ResultsClash", path, "");
				}),
				runConditions(path, o).stream()
			)
			.collect(toList());
	}

}
