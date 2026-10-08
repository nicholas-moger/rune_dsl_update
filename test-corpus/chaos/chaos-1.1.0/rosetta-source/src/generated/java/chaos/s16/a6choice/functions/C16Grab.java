package chaos.s16.a6choice.functions;

import chaos.s16.a6choice.C16Alpha;
import chaos.s16.a6choice.C16Beta;
import chaos.s16.a6choice.C16Pick;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;


@ImplementedBy(C16Grab.C16GrabDefault.class)
public abstract class C16Grab implements RosettaFunction {

	/**
	* @param p 
	* @return s 
	*/
	public String evaluate(C16Pick p) {
		String s = doEvaluate(p);
		
		return s;
	}

	protected abstract String doEvaluate(C16Pick p);

	public static class C16GrabDefault extends C16Grab {
		@Override
		protected String doEvaluate(C16Pick p) {
			String s = null;
			return assignOutput(s, p);
		}
		
		protected String assignOutput(String s, C16Pick p) {
			final MapperS<C16Pick> switchArgument = MapperS.of(p);
			if (switchArgument.get() == null) {
				s = null;
			} else if (switchArgument.<C16Alpha>map("getC16Alpha", c16Pick -> c16Pick.getC16Alpha()).get() != null) {
				final MapperS<C16Alpha> c16Alpha = switchArgument.<C16Alpha>map("getC16Alpha", c16Pick -> c16Pick.getC16Alpha());
				s = "was-alpha";
			} else if (switchArgument.<C16Beta>map("getC16Beta", c16Pick -> c16Pick.getC16Beta()).get() != null) {
				final MapperS<C16Beta> c16Beta = switchArgument.<C16Beta>map("getC16Beta", c16Pick -> c16Pick.getC16Beta());
				s = "was-beta";
			} else {
				s = "neither";
			}
			
			return s;
		}
	}
}
