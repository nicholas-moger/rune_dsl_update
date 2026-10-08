package chaos.s27.base.validation;

import chaos.s27.base.C27Holder;
import chaos.s27.base.validation.datarule.ArrayList;
import chaos.s27.base.validation.datarule.C27NatCalled;
import chaos.s27.base.validation.datarule.C27NatNonNeg;
import chaos.s27.base.validation.datarule.C27NatOps;
import chaos.s27.base.validation.datarule.Lists;
import chaos.s27.base.validation.datarule.ValidationResult;
import com.google.common.collect.Streams;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import javax.inject.Inject;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkNumber;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

public class C27HolderTypeFormatValidator implements Validator<C27Holder> {
	@Inject
	protected Lists lists;
	@Inject
	protected ValidationResult validationResult;
	@Inject
	protected ArrayList arrayList;
	@Inject
	protected chaos.s27.base.validation.datarule.C27Holder c27Holder;
	@Inject
	protected chaos.s27.base.validation.datarule.Inject inject;
	@Inject
	protected C27NatNonNeg c27NatNonNeg;
	@Inject
	protected C27NatOps c27NatOps;
	@Inject
	protected C27NatCalled c27NatCalled;

	private List<ComparisonResult> getComparisonResults(C27Holder o) {
		return com.google.common.collect.Lists.<ComparisonResult>newArrayList(
				checkNumber("l", o.getL(), empty(), of(0), empty(), empty()), 
				checkNumber("vr", o.getVr(), empty(), of(0), empty(), empty()), 
				checkNumber("ar", o.getAr(), empty(), of(0), empty(), empty()), 
				checkNumber("h", o.getH(), empty(), of(0), empty(), empty()), 
				checkNumber("inj", o.getInj(), empty(), of(0), empty(), empty()), 
				checkNumber("i", o.getI(), empty(), of(0), empty(), empty()), 
				checkNumber("o", o.getO(), empty(), of(0), empty(), empty()), 
				checkNumber("results", o.getResults(), empty(), of(0), empty(), empty())
			);
	}
	
	private List<com.rosetta.model.lib.validation.ValidationResult<?>> runConditions(RosettaPath path, C27Holder o) {
		List<com.rosetta.model.lib.validation.ValidationResult<?>> results = new java.util.ArrayList();
		final List<Integer> l = o.getL();
		if (l != null) {
			for (int i0 = 0; i0 < l.size(); i0++) {
				results.addAll(lists.getValidationResults(path.newSubPath("l").withIndex(i0), l.get(i0)));
			}
		}
		results.addAll(validationResult.getValidationResults(path.newSubPath("vr"), o.getVr()));
		final List<Integer> ar = o.getAr();
		if (ar != null) {
			for (int i1 = 0; i1 < ar.size(); i1++) {
				results.addAll(arrayList.getValidationResults(path.newSubPath("ar").withIndex(i1), ar.get(i1)));
			}
		}
		results.addAll(c27Holder.getValidationResults(path.newSubPath("h"), o.getH()));
		final List<Integer> inj = o.getInj();
		if (inj != null) {
			for (int i2 = 0; i2 < inj.size(); i2++) {
				results.addAll(inject.getValidationResults(path.newSubPath("inj").withIndex(i2), inj.get(i2)));
			}
		}
		final List<Integer> i4 = o.getI();
		if (i4 != null) {
			for (int i3 = 0; i3 < i4.size(); i3++) {
				results.addAll(c27NatNonNeg.getValidationResults(path.newSubPath("i").withIndex(i3), i4.get(i3)));
				results.addAll(c27NatOps.getValidationResults(path.newSubPath("i").withIndex(i3), i4.get(i3)));
				results.addAll(c27NatCalled.getValidationResults(path.newSubPath("i").withIndex(i3), i4.get(i3)));
			}
		}
		final List<Integer> _o = o.getO();
		if (_o != null) {
			for (int i5 = 0; i5 < _o.size(); i5++) {
				results.addAll(c27NatNonNeg.getValidationResults(path.newSubPath("o").withIndex(i5), _o.get(i5)));
				results.addAll(c27NatOps.getValidationResults(path.newSubPath("o").withIndex(i5), _o.get(i5)));
				results.addAll(c27NatCalled.getValidationResults(path.newSubPath("o").withIndex(i5), _o.get(i5)));
			}
		}
		final List<Integer> _results = o.getResults();
		if (_results != null) {
			for (int i6 = 0; i6 < _results.size(); i6++) {
				results.addAll(c27NatNonNeg.getValidationResults(path.newSubPath("results").withIndex(i6), _results.get(i6)));
				results.addAll(c27NatOps.getValidationResults(path.newSubPath("results").withIndex(i6), _results.get(i6)));
				results.addAll(c27NatCalled.getValidationResults(path.newSubPath("results").withIndex(i6), _results.get(i6)));
			}
		}
		return results;
	}

	@Override
	public List<com.rosetta.model.lib.validation.ValidationResult<?>> getValidationResults(RosettaPath path, C27Holder o) {
		return Streams.concat(getComparisonResults(o)
				.stream()
				.map(res -> {
					if (!isNullOrEmpty(res.getError())) {
						return failure("C27Holder", com.rosetta.model.lib.validation.ValidationResult.ValidationType.TYPE_FORMAT, "C27Holder", path, "", res.getError());
					}
					return success("C27Holder", com.rosetta.model.lib.validation.ValidationResult.ValidationType.TYPE_FORMAT, "C27Holder", path, "");
				}),
				runConditions(path, o).stream()
			)
			.collect(toList());
	}

}
