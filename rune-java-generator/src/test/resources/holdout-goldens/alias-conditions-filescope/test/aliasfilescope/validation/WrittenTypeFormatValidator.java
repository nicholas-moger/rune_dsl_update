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
import test.aliasfilescope.Written;
import test.aliasfilescope.validation.datarule.NatNonNeg;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkNumber;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

public class WrittenTypeFormatValidator implements Validator<Written> {
	@Inject
	protected NatNonNeg natNonNeg;

	private List<ComparisonResult> getComparisonResults(Written o) {
		return Lists.<ComparisonResult>newArrayList(
				checkNumber("Object", o.getObject(), empty(), of(0), empty(), empty()), 
				checkNumber("String", o.getString(), empty(), of(0), empty(), empty()), 
				checkNumber("List", o.getList(), empty(), of(0), empty(), empty()), 
				checkNumber("Objects", o.getObjects(), empty(), of(0), empty(), empty()), 
				checkNumber("Consumer", o.getConsumer(), empty(), of(0), empty(), empty()), 
				checkNumber("Collectors", o.getCollectors(), empty(), of(0), empty(), empty()), 
				checkNumber("ImmutableList", o.getImmutableList(), empty(), of(0), empty(), empty()), 
				checkNumber("Processor", o.getProcessor(), empty(), of(0), empty(), empty()), 
				checkNumber("Multi", o.getMulti(), empty(), of(0), empty(), empty()), 
				checkNumber("Override", o.getOverride(), empty(), of(0), empty(), empty()), 
				checkNumber("Lists", o.getLists(), empty(), of(0), empty(), empty()), 
				checkNumber("ValidationResult", o.getValidationResult(), empty(), of(0), empty(), empty()), 
				checkNumber("RosettaPath", o.getRosettaPath(), empty(), of(0), empty(), empty()), 
				checkNumber("Validator", o.getValidator(), empty(), of(0), empty(), empty()), 
				checkNumber("ComparisonResult", o.getComparisonResult(), empty(), of(0), empty(), empty())
			);
	}
	
	private List<ValidationResult<?>> runConditions(RosettaPath path, Written o) {
		List<ValidationResult<?>> results = new ArrayList();
		final List<Integer> Object = o.getObject();
		if (Object != null) {
			for (int i0 = 0; i0 < Object.size(); i0++) {
				results.addAll(natNonNeg.getValidationResults(path.newSubPath("Object").withIndex(i0), Object.get(i0)));
			}
		}
		final List<Integer> String = o.getString();
		if (String != null) {
			for (int i1 = 0; i1 < String.size(); i1++) {
				results.addAll(natNonNeg.getValidationResults(path.newSubPath("String").withIndex(i1), String.get(i1)));
			}
		}
		final List<Integer> _List = o.getList();
		if (_List != null) {
			for (int i2 = 0; i2 < _List.size(); i2++) {
				results.addAll(natNonNeg.getValidationResults(path.newSubPath("List").withIndex(i2), _List.get(i2)));
			}
		}
		final List<Integer> Objects = o.getObjects();
		if (Objects != null) {
			for (int i3 = 0; i3 < Objects.size(); i3++) {
				results.addAll(natNonNeg.getValidationResults(path.newSubPath("Objects").withIndex(i3), Objects.get(i3)));
			}
		}
		final List<Integer> Consumer = o.getConsumer();
		if (Consumer != null) {
			for (int i4 = 0; i4 < Consumer.size(); i4++) {
				results.addAll(natNonNeg.getValidationResults(path.newSubPath("Consumer").withIndex(i4), Consumer.get(i4)));
			}
		}
		final List<Integer> Collectors = o.getCollectors();
		if (Collectors != null) {
			for (int i5 = 0; i5 < Collectors.size(); i5++) {
				results.addAll(natNonNeg.getValidationResults(path.newSubPath("Collectors").withIndex(i5), Collectors.get(i5)));
			}
		}
		final List<Integer> ImmutableList = o.getImmutableList();
		if (ImmutableList != null) {
			for (int i6 = 0; i6 < ImmutableList.size(); i6++) {
				results.addAll(natNonNeg.getValidationResults(path.newSubPath("ImmutableList").withIndex(i6), ImmutableList.get(i6)));
			}
		}
		final List<Integer> Processor = o.getProcessor();
		if (Processor != null) {
			for (int i7 = 0; i7 < Processor.size(); i7++) {
				results.addAll(natNonNeg.getValidationResults(path.newSubPath("Processor").withIndex(i7), Processor.get(i7)));
			}
		}
		final List<Integer> Multi = o.getMulti();
		if (Multi != null) {
			for (int i8 = 0; i8 < Multi.size(); i8++) {
				results.addAll(natNonNeg.getValidationResults(path.newSubPath("Multi").withIndex(i8), Multi.get(i8)));
			}
		}
		final List<Integer> Override = o.getOverride();
		if (Override != null) {
			for (int i9 = 0; i9 < Override.size(); i9++) {
				results.addAll(natNonNeg.getValidationResults(path.newSubPath("Override").withIndex(i9), Override.get(i9)));
			}
		}
		final List<Integer> _Lists = o.getLists();
		if (_Lists != null) {
			for (int i10 = 0; i10 < _Lists.size(); i10++) {
				results.addAll(natNonNeg.getValidationResults(path.newSubPath("Lists").withIndex(i10), _Lists.get(i10)));
			}
		}
		final List<Integer> _ValidationResult = o.getValidationResult();
		if (_ValidationResult != null) {
			for (int i11 = 0; i11 < _ValidationResult.size(); i11++) {
				results.addAll(natNonNeg.getValidationResults(path.newSubPath("ValidationResult").withIndex(i11), _ValidationResult.get(i11)));
			}
		}
		final List<Integer> _RosettaPath = o.getRosettaPath();
		if (_RosettaPath != null) {
			for (int i12 = 0; i12 < _RosettaPath.size(); i12++) {
				results.addAll(natNonNeg.getValidationResults(path.newSubPath("RosettaPath").withIndex(i12), _RosettaPath.get(i12)));
			}
		}
		final List<Integer> _Validator = o.getValidator();
		if (_Validator != null) {
			for (int i13 = 0; i13 < _Validator.size(); i13++) {
				results.addAll(natNonNeg.getValidationResults(path.newSubPath("Validator").withIndex(i13), _Validator.get(i13)));
			}
		}
		final List<Integer> _ComparisonResult = o.getComparisonResult();
		if (_ComparisonResult != null) {
			for (int i14 = 0; i14 < _ComparisonResult.size(); i14++) {
				results.addAll(natNonNeg.getValidationResults(path.newSubPath("ComparisonResult").withIndex(i14), _ComparisonResult.get(i14)));
			}
		}
		return results;
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Written o) {
		return Streams.concat(getComparisonResults(o)
				.stream()
				.map(res -> {
					if (!isNullOrEmpty(res.getError())) {
						return failure("Written", ValidationResult.ValidationType.TYPE_FORMAT, "Written", path, "", res.getError());
					}
					return success("Written", ValidationResult.ValidationType.TYPE_FORMAT, "Written", path, "");
				}),
				runConditions(path, o).stream()
			)
			.collect(toList());
	}

}
