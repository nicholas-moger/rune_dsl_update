package chaos.s06.a5uni.functions;

import chaos.s06.a5uni.C6Event;
import chaos.s06.a5uni.C6Tag;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.util.Optional;
import javax.inject.Inject;


@ImplementedBy(C6MarkOf.C6MarkOfDefault.class)
public abstract class C6MarkOf implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param ev 
	* @return t 
	*/
	public C6Tag evaluate(C6Event ev) {
		C6Tag.C6TagBuilder tBuilder = doEvaluate(ev);
		
		final C6Tag t;
		if (tBuilder == null) {
			t = null;
		} else {
			t = tBuilder.build();
			objectValidator.validate(C6Tag.class, t);
		}
		
		return t;
	}

	protected abstract C6Tag.C6TagBuilder doEvaluate(C6Event ev);

	public static class C6MarkOfDefault extends C6MarkOf {
		@Override
		protected C6Tag.C6TagBuilder doEvaluate(C6Event ev) {
			C6Tag.C6TagBuilder t = C6Tag.builder();
			return assignOutput(t, ev);
		}
		
		protected C6Tag.C6TagBuilder assignOutput(C6Tag.C6TagBuilder t, C6Event ev) {
			t = toBuilder(MapperS.of(ev).<C6Tag>map("getMark", c6Event -> c6Event.getMark()).get());
			
			return Optional.ofNullable(t)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
