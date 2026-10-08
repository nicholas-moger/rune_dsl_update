package chaos.s24.x13half.p2.functions;

import chaos.s24.x13half.p2.C24Carrier;
import chaos.s24.x13half.p2.C24Out;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ConditionValidator;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.util.Collections;
import java.util.Optional;
import javax.inject.Inject;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C24Multi.C24MultiDefault.class)
public abstract class C24Multi implements RosettaFunction {
	
	@Inject protected ConditionValidator conditionValidator;
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param c 
	* @return out 
	*/
	public C24Out evaluate(C24Carrier c) {
		// pre-conditions
		conditionValidator.validate(() -> multipleExists(MapperS.<Void>ofNull()).orNullSafe(singleExists(MapperS.<Void>ofNull())).orNullSafe(notExists(MapperS.<Void>ofNull())),
			"");
		
		C24Out.C24OutBuilder outBuilder = doEvaluate(c);
		
		final C24Out out;
		if (outBuilder == null) {
			out = null;
		} else {
			out = outBuilder.build();
			objectValidator.validate(C24Out.class, out);
		}
		
		return out;
	}

	protected abstract C24Out.C24OutBuilder doEvaluate(C24Carrier c);

	public static class C24MultiDefault extends C24Multi {
		@Override
		protected C24Out.C24OutBuilder doEvaluate(C24Carrier c) {
			C24Out.C24OutBuilder out = C24Out.builder();
			return assignOutput(out, c);
		}
		
		protected C24Out.C24OutBuilder assignOutput(C24Out.C24OutBuilder out, C24Carrier c) {
			out
				.setVs(Collections.<Void>emptyList());
			
			out
				.addVs(Collections.<Void>emptyList());
			
			return Optional.ofNullable(out)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
