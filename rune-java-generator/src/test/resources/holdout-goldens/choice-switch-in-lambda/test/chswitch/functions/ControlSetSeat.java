package test.chswitch.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import test.chswitch.Either;
import test.chswitch.OptA;
import test.chswitch.OptB;


@ImplementedBy(ControlSetSeat.ControlSetSeatDefault.class)
public abstract class ControlSetSeat implements RosettaFunction {

	/**
	* @param eth 
	* @return out 
	*/
	public String evaluate(Either eth) {
		String out = doEvaluate(eth);
		
		return out;
	}

	protected abstract String doEvaluate(Either eth);

	public static class ControlSetSeatDefault extends ControlSetSeat {
		@Override
		protected String doEvaluate(Either eth) {
			String out = null;
			return assignOutput(out, eth);
		}
		
		protected String assignOutput(String out, Either eth) {
			final MapperS<Either> switchArgument = MapperS.of(eth);
			if (switchArgument.get() == null) {
				out = null;
			} else if (switchArgument.<OptA>map("getOptA", either -> either.getOptA()).get() != null) {
				final MapperS<OptA> optA = switchArgument.<OptA>map("getOptA", either -> either.getOptA());
				out = optA.<String>map("getAv", _optA -> _optA.getAv()).get();
			} else if (switchArgument.<OptB>map("getOptB", either -> either.getOptB()).get() != null) {
				final MapperS<OptB> optB = switchArgument.<OptB>map("getOptB", either -> either.getOptB());
				out = optB.<BigDecimal>map("getBv", _optB -> _optB.getBv()).map("to-string", Object::toString).get();
			} else {
				out = "none";
			}
			
			return out;
		}
	}
}
