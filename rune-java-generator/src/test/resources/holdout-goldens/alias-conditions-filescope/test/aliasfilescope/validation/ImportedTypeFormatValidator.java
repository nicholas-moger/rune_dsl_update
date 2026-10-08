package test.aliasfilescope.validation;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import test.aliasfilescope.Imported;
import test.aliasfilescope.validation.datarule.NatNonNeg;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkNumber;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

public class ImportedTypeFormatValidator implements Validator<Imported> {
	@Inject
	protected NatNonNeg natNonNeg;

	private List<ComparisonResult> getComparisonResults(Imported o) {
		return Lists.<ComparisonResult>newArrayList(
				checkNumber("Streams", o.getStreams(), empty(), of(0), empty(), empty()), 
				checkNumber("ArrayList", o.getArrayList(), empty(), of(0), empty(), empty()), 
				checkNumber("Inject", o.getInject(), empty(), of(0), empty(), empty()), 
				checkNumber("Integer", o.getInteger(), empty(), of(0), empty(), empty())
			);
	}
	
	private List<ValidationResult<?>> runConditions(RosettaPath path, Imported o) {
		List<ValidationResult<?>> results = new ArrayList();
		final List<Integer> _Streams = o.getStreams();
		if (_Streams != null) {
			for (int i0 = 0; i0 < _Streams.size(); i0++) {
				results.addAll(natNonNeg.getValidationResults(path.newSubPath("Streams").withIndex(i0), _Streams.get(i0)));
			}
		}
		final List<Integer> _ArrayList = o.getArrayList();
		if (_ArrayList != null) {
			for (int i1 = 0; i1 < _ArrayList.size(); i1++) {
				results.addAll(natNonNeg.getValidationResults(path.newSubPath("ArrayList").withIndex(i1), _ArrayList.get(i1)));
			}
		}
		final List<Integer> _Inject = o.getInject();
		if (_Inject != null) {
			for (int i2 = 0; i2 < _Inject.size(); i2++) {
				results.addAll(natNonNeg.getValidationResults(path.newSubPath("Inject").withIndex(i2), _Inject.get(i2)));
			}
		}
		final List<Integer> _Integer = o.getInteger();
		if (_Integer != null) {
			for (int i3 = 0; i3 < _Integer.size(); i3++) {
				results.addAll(natNonNeg.getValidationResults(path.newSubPath("Integer").withIndex(i3), _Integer.get(i3)));
			}
		}
		return results;
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Imported o) {
		return Streams.concat(getComparisonResults(o)
				.stream()
				.map(res -> {
					if (!isNullOrEmpty(res.getError())) {
						return failure("Imported", ValidationResult.ValidationType.TYPE_FORMAT, "Imported", path, "", res.getError());
					}
					return success("Imported", ValidationResult.ValidationType.TYPE_FORMAT, "Imported", path, "");
				}),
				runConditions(path, o).stream()
			)
			.collect(toList());
	}

}
