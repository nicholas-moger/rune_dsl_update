package test.chswitch.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import test.chswitch.Either;
import test.chswitch.OptA;
import test.chswitch.OptB;


@ImplementedBy(SingleReceiver.SingleReceiverDefault.class)
public abstract class SingleReceiver implements RosettaFunction {

	/**
	* @param eth 
	* @return text 
	*/
	public String evaluate(Either eth) {
		String text = doEvaluate(eth);
		
		return text;
	}

	protected abstract String doEvaluate(Either eth);

	public static class SingleReceiverDefault extends SingleReceiver {
		@Override
		protected String doEvaluate(Either eth) {
			String text = null;
			return assignOutput(text, eth);
		}
		
		protected String assignOutput(String text, Either eth) {
			text = MapperS.of(eth)
				.mapSingleToItem(item -> {
					if (item.get() == null) {
						return MapperS.<String>ofNull();
					}
					if (item.<OptA>map("getOptA", either -> either.getOptA()).get() != null) {
						final MapperS<OptA> optA = item.<OptA>map("getOptA", either -> either.getOptA());
						return optA.<String>map("getAv", _optA -> _optA.getAv());
					}
					if (item.<OptB>map("getOptB", either -> either.getOptB()).get() != null) {
						final MapperS<OptB> optB = item.<OptB>map("getOptB", either -> either.getOptB());
						return optB.<BigDecimal>map("getBv", _optB -> _optB.getBv()).map("to-string", Object::toString);
					}
					return MapperS.of("none");
				}).get();
			
			return text;
		}
	}
}
