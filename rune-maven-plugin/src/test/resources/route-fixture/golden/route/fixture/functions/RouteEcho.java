package route.fixture.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;


@ImplementedBy(RouteEcho.RouteEchoDefault.class)
public abstract class RouteEcho implements RosettaFunction {

	/**
	* @param in1 
	* @return result 
	*/
	public String evaluate(String in1) {
		String result = doEvaluate(in1);
		
		return result;
	}

	protected abstract String doEvaluate(String in1);

	public static class RouteEchoDefault extends RouteEcho {
		@Override
		protected String doEvaluate(String in1) {
			String result = null;
			return assignOutput(result, in1);
		}
		
		protected String assignOutput(String result, String in1) {
			result = in1;
			
			return result;
		}
	}
}
