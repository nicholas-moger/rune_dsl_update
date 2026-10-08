package chaos.s34.a2qual.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;


@ImplementedBy(C34Stage_Tag.C34Stage_TagDefault.class)
public abstract class C34Stage_Tag implements RosettaFunction {

	/**
	* @param s 
	* @return t 
	*/
	public String evaluate(String s) {
		String t = doEvaluate(s);
		
		return t;
	}

	protected abstract String doEvaluate(String s);

	public static class C34Stage_TagDefault extends C34Stage_Tag {
		@Override
		protected String doEvaluate(String s) {
			String t = null;
			return assignOutput(t, s);
		}
		
		protected String assignOutput(String t, String s) {
			t = MapperMaths.<String, String, String>add(MapperS.of(s), MapperS.of("!")).get();
			
			return t;
		}
	}
}
