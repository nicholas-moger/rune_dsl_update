package test.chswitch.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import test.chswitch.Either;
import test.chswitch.KindEnum;
import test.chswitch.OptA;
import test.chswitch.OptB;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(Chaos.ChaosDefault.class)
public abstract class Chaos implements RosettaFunction {

	/**
	* @param raw 
	* @param eths 
	* @return k 
	*/
	public KindEnum evaluate(String raw, List<? extends Either> eths) {
		KindEnum k = doEvaluate(raw, eths);
		
		return k;
	}

	protected abstract KindEnum doEvaluate(String raw, List<? extends Either> eths);

	protected abstract MapperC<String> switched(String raw, List<? extends Either> eths);

	protected abstract MapperC<String> pulled(String raw, List<? extends Either> eths);

	public static class ChaosDefault extends Chaos {
		@Override
		protected KindEnum doEvaluate(String raw, List<? extends Either> eths) {
			if (eths == null) {
				eths = Collections.emptyList();
			}
			KindEnum k = null;
			return assignOutput(k, raw, eths);
		}
		
		protected KindEnum assignOutput(KindEnum k, String raw, List<? extends Either> eths) {
			if (areEqual(MapperS.of(pulled(raw, eths).resultCount()), MapperS.of(switched(raw, eths).resultCount()), CardinalityOperator.All).orNullSafe(notExists(pulled(raw, eths))).getOrDefault(false)) {
				k = MapperS.of(raw).checkedMap("to-enum", KindEnum::fromDisplayName, IllegalArgumentException.class).get();
			} else {
				k = null;
			}
			
			return k;
		}
		
		@Override
		protected MapperC<String> switched(String raw, List<? extends Either> eths) {
			return MapperC.<String>of(MapperS.of(raw))
				.mapItem(item -> {
					if (item.get() == null) {
						return MapperS.<String>ofNull();
					}
					if (areEqual(item, MapperS.of("r"), CardinalityOperator.All).get()) {
						return MapperS.of("Red");
					}
					if (areEqual(item, MapperS.of("g"), CardinalityOperator.All).get()) {
						return MapperS.of("Green");
					}
					return MapperS.of("Blue");
				});
		}
		
		@Override
		protected MapperC<String> pulled(String raw, List<? extends Either> eths) {
			return MapperC.<Either>of(eths)
				.mapItem(item -> {
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
				});
		}
	}
}
