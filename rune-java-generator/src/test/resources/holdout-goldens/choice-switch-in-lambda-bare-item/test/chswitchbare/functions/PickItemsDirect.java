package test.chswitchbare.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.util.Optional;
import javax.inject.Inject;
import test.chswitchbare.OptA;
import test.chswitchbare.Outer2;


@ImplementedBy(PickItemsDirect.PickItemsDirectDefault.class)
public abstract class PickItemsDirect implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param outer 
	* @return out 
	*/
	public OptA evaluate(Outer2 outer) {
		OptA.OptABuilder outBuilder = doEvaluate(outer);
		
		final OptA out;
		if (outBuilder == null) {
			out = null;
		} else {
			out = outBuilder.build();
			objectValidator.validate(OptA.class, out);
		}
		
		return out;
	}

	protected abstract OptA.OptABuilder doEvaluate(Outer2 outer);

	public static class PickItemsDirectDefault extends PickItemsDirect {
		@Override
		protected OptA.OptABuilder doEvaluate(Outer2 outer) {
			OptA.OptABuilder out = OptA.builder();
			return assignOutput(out, outer);
		}
		
		protected OptA.OptABuilder assignOutput(OptA.OptABuilder out, Outer2 outer) {
			final MapperS<Outer2> switchArgument = MapperS.of(outer);
			if (switchArgument.get() == null) {
				out = null;
			} else if (switchArgument.<OptA>map("getOptA", outer2 -> outer2.getOptA()).get() != null) {
				final MapperS<OptA> optA = switchArgument.<OptA>map("getOptA", outer2 -> outer2.getOptA());
				out = toBuilder(optA.get());
			} else {
				out = null;
			}
			
			return Optional.ofNullable(out)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
