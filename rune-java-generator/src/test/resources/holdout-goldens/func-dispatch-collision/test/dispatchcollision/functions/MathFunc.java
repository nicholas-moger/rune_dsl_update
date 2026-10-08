package test.dispatchcollision.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import javax.inject.Inject;
import test.dispatchcollision.MathInput;


/**
 * @version 0.0.0
 */
public class MathFunc implements RosettaFunction {
	
	@Inject protected MathFunc.MathFuncINCR mathFuncINCR;
	@Inject protected MathFunc.MathFuncDECR mathFuncDECR;
	
	public String evaluate(test.dispatchcollision.Math in1, MathInput in2) {
		switch (in1) {
			case INCR:
				return mathFuncINCR.evaluate(in1, in2);
			case DECR:
				return mathFuncDECR.evaluate(in1, in2);
			default:
				throw new IllegalArgumentException("Enum value not implemented: " + in1);
		}
	}
	
	@ImplementedBy(MathFunc.MathFuncINCR.MathFuncINCRDefault.class)
	public static abstract class MathFuncINCR implements RosettaFunction {
		
		// RosettaFunction dependencies
		//
		@Inject protected AddOne addOne;
	
		/**
		* @param in1 
		* @param in2 
		* @return arg1 
		*/
		public String evaluate(test.dispatchcollision.Math in1, MathInput in2) {
			String arg1 = doEvaluate(in1, in2);
			
			return arg1;
		}
	
		protected abstract String doEvaluate(test.dispatchcollision.Math in1, MathInput in2);
	
		public static class MathFuncINCRDefault extends MathFunc.MathFuncINCR {
			@Override
			protected String doEvaluate(test.dispatchcollision.Math in1, MathInput in2) {
				String arg1 = null;
				return assignOutput(arg1, in1, in2);
			}
			
			protected String assignOutput(String arg1, test.dispatchcollision.Math in1, MathInput in2) {
				arg1 = addOne.evaluate(MapperS.of(in2).<String>map("getMathInput", mathInput -> mathInput.getMathInput()).get());
				
				return arg1;
			}
		}
	}
	@ImplementedBy(MathFunc.MathFuncDECR.MathFuncDECRDefault.class)
	public static abstract class MathFuncDECR implements RosettaFunction {
		
		// RosettaFunction dependencies
		//
		@Inject protected SubOne subOne;
	
		/**
		* @param in1 
		* @param in2 
		* @return arg1 
		*/
		public String evaluate(test.dispatchcollision.Math in1, MathInput in2) {
			String arg1 = doEvaluate(in1, in2);
			
			return arg1;
		}
	
		protected abstract String doEvaluate(test.dispatchcollision.Math in1, MathInput in2);
	
		public static class MathFuncDECRDefault extends MathFunc.MathFuncDECR {
			@Override
			protected String doEvaluate(test.dispatchcollision.Math in1, MathInput in2) {
				String arg1 = null;
				return assignOutput(arg1, in1, in2);
			}
			
			protected String assignOutput(String arg1, test.dispatchcollision.Math in1, MathInput in2) {
				arg1 = subOne.evaluate(MapperS.of(in2).<String>map("getMathInput", mathInput -> mathInput.getMathInput()).get());
				
				return arg1;
			}
		}
	}
}
