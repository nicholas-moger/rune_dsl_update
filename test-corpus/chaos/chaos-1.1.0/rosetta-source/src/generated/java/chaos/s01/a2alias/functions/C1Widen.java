package chaos.s01.a2alias.functions;

import chaos.s01.a2alias.C1Base;
import chaos.s01.a2alias.C1Leaf;
import chaos.s01.a2alias.C1Mid;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import java.util.Optional;
import javax.inject.Inject;


@ImplementedBy(C1Widen.C1WidenDefault.class)
public abstract class C1Widen implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param b 
	* @param m 
	* @param l 
	* @return out 
	*/
	public C1Base evaluate(Boolean b, C1Mid m, C1Leaf l) {
		C1Base.C1BaseBuilder outBuilder = doEvaluate(b, m, l);
		
		final C1Base out;
		if (outBuilder == null) {
			out = null;
		} else {
			out = outBuilder.build();
			objectValidator.validate(C1Base.class, out);
		}
		
		return out;
	}

	protected abstract C1Base.C1BaseBuilder doEvaluate(Boolean b, C1Mid m, C1Leaf l);

	public static class C1WidenDefault extends C1Widen {
		@Override
		protected C1Base.C1BaseBuilder doEvaluate(Boolean b, C1Mid m, C1Leaf l) {
			C1Base.C1BaseBuilder out = C1Base.builder();
			return assignOutput(out, b, m, l);
		}
		
		protected C1Base.C1BaseBuilder assignOutput(C1Base.C1BaseBuilder out, Boolean b, C1Mid m, C1Leaf l) {
			if ((b == null ? false : b)) {
				out = toBuilder(l);
			} else {
				out = toBuilder(m);
			}
			
			return Optional.ofNullable(out)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
