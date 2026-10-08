package test.qep.a.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.IQualifyFunctionExtension;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import test.qep.a.QepEvent;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(Qualify_QepE.Qualify_QepEDefault.class)
public abstract class Qualify_QepE implements RosettaFunction,IQualifyFunctionExtension<QepEvent> {

	/**
	* @param event 
	* @return is_event 
	*/
	@Override
	public Boolean evaluate(QepEvent event) {
		Boolean is_event = doEvaluate(event);
		
		return is_event;
	}

	protected abstract Boolean doEvaluate(QepEvent event);

	public static class Qualify_QepEDefault extends Qualify_QepE {
		@Override
		protected Boolean doEvaluate(QepEvent event) {
			Boolean is_event = null;
			return assignOutput(is_event, event);
		}
		
		protected Boolean assignOutput(Boolean is_event, QepEvent event) {
			is_event = areEqual(MapperS.of(event).<String>map("getKind", qepEvent -> qepEvent.getKind()), MapperS.of("trade"), CardinalityOperator.All).get();
			
			return is_event;
		}
	}
		
		@Override
		public String getNamePrefix() {
			return "Qualify";
		}
}
