package test.chswitchedge.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import test.chswitchedge.Either;
import test.chswitchedge.OptA;
import test.chswitchedge.OptB;


@ImplementedBy(NumberArms.NumberArmsDefault.class)
public abstract class NumberArms implements RosettaFunction {

	/**
	* @param eths 
	* @return nums 
	*/
	public List<BigDecimal> evaluate(List<? extends Either> eths) {
		List<BigDecimal> nums = doEvaluate(eths);
		
		return nums;
	}

	protected abstract List<BigDecimal> doEvaluate(List<? extends Either> eths);

	public static class NumberArmsDefault extends NumberArms {
		@Override
		protected List<BigDecimal> doEvaluate(List<? extends Either> eths) {
			if (eths == null) {
				eths = Collections.emptyList();
			}
			List<BigDecimal> nums = new ArrayList<>();
			return assignOutput(nums, eths);
		}
		
		protected List<BigDecimal> assignOutput(List<BigDecimal> nums, List<? extends Either> eths) {
			nums.addAll(MapperC.<Either>of(eths)
				.mapItem(item -> {
					if (item.get() == null) {
						return MapperS.<BigDecimal>ofNull();
					}
					if (item.<OptA>map("getOptA", either -> either.getOptA()).get() != null) {
						final MapperS<OptA> optA = item.<OptA>map("getOptA", either -> either.getOptA());
						return MapperS.of(BigDecimal.valueOf(1));
					}
					if (item.<OptB>map("getOptB", either -> either.getOptB()).get() != null) {
						final MapperS<OptB> optB = item.<OptB>map("getOptB", either -> either.getOptB());
						return optB.<BigDecimal>map("getBv", _optB -> _optB.getBv());
					}
					return MapperS.of(BigDecimal.valueOf(0));
				}).getMulti());
			
			return nums;
		}
	}
}
