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
import test.aliasreserved.Keywords;
import test.aliasreserved.validation.datarule.NatNonNeg;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkNumber;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

public class KeywordsTypeFormatValidator implements Validator<Keywords> {
	@Inject
	protected NatNonNeg natNonNeg;

	private List<ComparisonResult> getComparisonResults(Keywords o) {
		return Lists.<ComparisonResult>newArrayList(
				checkNumber("new", o.getNew(), empty(), of(0), empty(), empty()), 
				checkNumber("final", o.getFinal(), empty(), of(0), empty(), empty())
			);
	}
	
	private List<ValidationResult<?>> runConditions(RosettaPath path, Keywords o) {
		List<ValidationResult<?>> results = new ArrayList();
		final List<Integer> _new = o.getNew();
		if (_new != null) {
			for (int i0 = 0; i0 < _new.size(); i0++) {
				results.addAll(natNonNeg.getValidationResults(path.newSubPath("new").withIndex(i0), _new.get(i0)));
			}
		}
		final List<Integer> _final = o.getFinal();
		if (_final != null) {
			for (int i1 = 0; i1 < _final.size(); i1++) {
				results.addAll(natNonNeg.getValidationResults(path.newSubPath("final").withIndex(i1), _final.get(i1)));
			}
		}
		return results;
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Keywords o) {
		return Streams.concat(getComparisonResults(o)
				.stream()
				.map(res -> {
					if (!isNullOrEmpty(res.getError())) {
						return failure("Keywords", ValidationResult.ValidationType.TYPE_FORMAT, "Keywords", path, "", res.getError());
					}
					return success("Keywords", ValidationResult.ValidationType.TYPE_FORMAT, "Keywords", path, "");
				}),
				runConditions(path, o).stream()
			)
			.collect(toList());
	}

}
