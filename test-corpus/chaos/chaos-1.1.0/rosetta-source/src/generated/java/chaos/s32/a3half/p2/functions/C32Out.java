package chaos.s32.a3half.p2.functions;

import chaos.s32.a3half.p1.C32Keyworded;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import java.math.BigDecimal;
import java.util.Optional;
import javax.inject.Inject;


@ImplementedBy(C32Out.C32OutDefault.class)
public abstract class C32Out implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param _class 
	* @param _long 
	* @return o 
	*/
	public C32Keyworded evaluate(String _class, BigDecimal _long) {
		C32Keyworded.C32KeywordedBuilder oBuilder = doEvaluate(_class, _long);
		
		final C32Keyworded o;
		if (oBuilder == null) {
			o = null;
		} else {
			o = oBuilder.build();
			objectValidator.validate(C32Keyworded.class, o);
		}
		
		return o;
	}

	protected abstract C32Keyworded.C32KeywordedBuilder doEvaluate(String _class, BigDecimal _long);

	public static class C32OutDefault extends C32Out {
		@Override
		protected C32Keyworded.C32KeywordedBuilder doEvaluate(String _class, BigDecimal _long) {
			C32Keyworded.C32KeywordedBuilder o = C32Keyworded.builder();
			return assignOutput(o, _class, _long);
		}
		
		protected C32Keyworded.C32KeywordedBuilder assignOutput(C32Keyworded.C32KeywordedBuilder o, String _class, BigDecimal _long) {
			o
				.setClass(_class);
			
			o
				.setLong(_long);
			
			return Optional.ofNullable(o)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
