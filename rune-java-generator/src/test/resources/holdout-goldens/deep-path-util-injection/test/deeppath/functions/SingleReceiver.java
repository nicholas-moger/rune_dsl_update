package test.deeppath.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import javax.inject.Inject;
import test.deeppath.Outer;
import test.deeppath.util.OuterDeepPathUtil;


@ImplementedBy(SingleReceiver.SingleReceiverDefault.class)
public abstract class SingleReceiver implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected OuterDeepPathUtil outerDeepPathUtil;

	/**
	* @param outer 
	* @return text 
	*/
	public String evaluate(Outer outer) {
		String text = doEvaluate(outer);
		
		return text;
	}

	protected abstract String doEvaluate(Outer outer);

	public static class SingleReceiverDefault extends SingleReceiver {
		@Override
		protected String doEvaluate(Outer outer) {
			String text = null;
			return assignOutput(text, outer);
		}
		
		protected String assignOutput(String text, Outer outer) {
			text = MapperS.of(outer)
				.mapSingleToItem(item -> item.<String>map("chooseText", _outer -> outerDeepPathUtil.chooseText(_outer))).get();
			
			return text;
		}
	}
}
