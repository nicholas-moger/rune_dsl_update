package test.fmeta018.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.metafields.FieldWithMetaBigDecimal;
import com.rosetta.model.metafields.ReferenceWithMetaInteger;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;


@ImplementedBy(SomeFunc.SomeFuncDefault.class)
public abstract class SomeFunc implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param myInput 
	* @return myResult 
	*/
	public List<? extends FieldWithMetaBigDecimal> evaluate(List<? extends ReferenceWithMetaInteger> myInput) {
		List<FieldWithMetaBigDecimal.FieldWithMetaBigDecimalBuilder> myResultBuilder = doEvaluate(myInput);
		
		final List<? extends FieldWithMetaBigDecimal> myResult;
		if (myResultBuilder == null) {
			myResult = null;
		} else {
			myResult = myResultBuilder.stream().map(FieldWithMetaBigDecimal::build).collect(Collectors.toList());
			objectValidator.validate(FieldWithMetaBigDecimal.class, myResult);
		}
		
		return myResult;
	}

	protected abstract List<FieldWithMetaBigDecimal.FieldWithMetaBigDecimalBuilder> doEvaluate(List<? extends ReferenceWithMetaInteger> myInput);

	public static class SomeFuncDefault extends SomeFunc {
		@Override
		protected List<FieldWithMetaBigDecimal.FieldWithMetaBigDecimalBuilder> doEvaluate(List<? extends ReferenceWithMetaInteger> myInput) {
			if (myInput == null) {
				myInput = Collections.emptyList();
			}
			List<FieldWithMetaBigDecimal.FieldWithMetaBigDecimalBuilder> myResult = new ArrayList<>();
			return assignOutput(myResult, myInput);
		}
		
		protected List<FieldWithMetaBigDecimal.FieldWithMetaBigDecimalBuilder> assignOutput(List<FieldWithMetaBigDecimal.FieldWithMetaBigDecimalBuilder> myResult, List<? extends ReferenceWithMetaInteger> myInput) {
			myResult = toBuilder(myInput.stream()
				.<FieldWithMetaBigDecimal>map(referenceWithMetaInteger -> {
					final Integer integer = referenceWithMetaInteger.getValue();
					return integer == null ? FieldWithMetaBigDecimal.builder().build() : FieldWithMetaBigDecimal.builder().setValue(BigDecimal.valueOf(integer)).build();
				})
				.collect(Collectors.toList())
			);
			
			return Optional.ofNullable(myResult)
				.map(o -> o.stream().map(i -> i.prune()).collect(Collectors.toList()))
				.orElse(null);
		}
	}
}
