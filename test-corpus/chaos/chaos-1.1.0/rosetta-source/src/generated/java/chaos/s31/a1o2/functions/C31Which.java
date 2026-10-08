package chaos.s31.a1o2.functions;

import chaos.s31.a1o2.C31EscEnum;
import chaos.s31.a1o2.C31SideEnum;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;


@ImplementedBy(C31Which.C31WhichDefault.class)
public abstract class C31Which implements RosettaFunction {

	/**
	* @param s 
	* @param esc 
	* @return r 
	*/
	public String evaluate(C31SideEnum s, C31EscEnum esc) {
		String r = doEvaluate(s, esc);
		
		return r;
	}

	protected abstract String doEvaluate(C31SideEnum s, C31EscEnum esc);

	public static class C31WhichDefault extends C31Which {
		@Override
		protected String doEvaluate(C31SideEnum s, C31EscEnum esc) {
			String r = null;
			return assignOutput(r, s, esc);
		}
		
		protected String assignOutput(String r, C31SideEnum s, C31EscEnum esc) {
			final MapperS<String> ifThenElseResult0;
			if (s == null) {
				ifThenElseResult0 = MapperS.<String>ofNull();
			} else if (s == C31SideEnum.LONG) {
				ifThenElseResult0 = MapperS.of("side-long");
			} else if (s == C31SideEnum.SHORT) {
				ifThenElseResult0 = MapperS.of("side-short");
			} else {
				ifThenElseResult0 = MapperS.<String>ofNull();
			}
			final MapperS<String> ifThenElseResult1;
			if (esc == null) {
				ifThenElseResult1 = MapperS.<String>ofNull();
			} else if (esc == C31EscEnum.QUOTE) {
				ifThenElseResult1 = MapperS.of("q");
			} else if (esc == C31EscEnum.SLASH) {
				ifThenElseResult1 = MapperS.of("s");
			} else {
				ifThenElseResult1 = MapperS.of(esc).map("to-string", C31EscEnum::toDisplayString);
			}
			r = MapperMaths.<String, String, String>add(ifThenElseResult0, ifThenElseResult1).get();
			
			return r;
		}
	}
}
