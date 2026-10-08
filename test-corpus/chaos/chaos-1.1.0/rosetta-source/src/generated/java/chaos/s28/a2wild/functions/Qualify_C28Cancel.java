package chaos.s28.a2wild.functions;

import chaos.s28.a2wild.C28Event;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.IQualifyFunctionExtension;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(Qualify_C28Cancel.Qualify_C28CancelDefault.class)
public abstract class Qualify_C28Cancel implements RosettaFunction,IQualifyFunctionExtension<C28Event> {

	/**
	* @param event 
	* @return is_event 
	*/
	@Override
	public Boolean evaluate(C28Event event) {
		Boolean is_event = doEvaluate(event);
		
		return is_event;
	}

	protected abstract Boolean doEvaluate(C28Event event);

	public static class Qualify_C28CancelDefault extends Qualify_C28Cancel {
		@Override
		protected Boolean doEvaluate(C28Event event) {
			Boolean is_event = null;
			return assignOutput(is_event, event);
		}
		
		protected Boolean assignOutput(Boolean is_event, C28Event event) {
			is_event = areEqual(MapperS.of(event).<String>map("getKind", c28Event -> c28Event.getKind()), MapperS.of("cancel"), CardinalityOperator.All).andNullSafe(exists(MapperS.of(event).<BigDecimal>map("getSize", c28Event -> c28Event.getSize()))).get();
			
			return is_event;
		}
	}
		
		@Override
		public String getNamePrefix() {
			return "Qualify";
		}
}
