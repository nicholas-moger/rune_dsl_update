package test.chswitchbare.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import test.chswitchbare.OptA;
import test.chswitchbare.OptB;
import test.chswitchbare.Outer2;


@ImplementedBy(PickAll.PickAllDefault.class)
public abstract class PickAll implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected FnA fnA;
	@Inject protected FnB fnB;

	/**
	* @param outers 
	* @return outs 
	*/
	public List<String> evaluate(List<? extends Outer2> outers) {
		List<String> outs = doEvaluate(outers);
		
		return outs;
	}

	protected abstract List<String> doEvaluate(List<? extends Outer2> outers);

	public static class PickAllDefault extends PickAll {
		@Override
		protected List<String> doEvaluate(List<? extends Outer2> outers) {
			if (outers == null) {
				outers = Collections.emptyList();
			}
			List<String> outs = new ArrayList<>();
			return assignOutput(outs, outers);
		}
		
		protected List<String> assignOutput(List<String> outs, List<? extends Outer2> outers) {
			outs = MapperC.<Outer2>of(outers)
				.mapItem(item -> {
					if (item.get() == null) {
						return MapperS.<String>ofNull();
					}
					if (item.<OptA>map("getOptA", outer2 -> outer2.getOptA()).get() != null) {
						final MapperS<OptA> optA = item.<OptA>map("getOptA", outer2 -> outer2.getOptA());
						return MapperS.of(fnA.evaluate(optA.get()));
					}
					if (item.<OptB>map("getOptB", outer2 -> outer2.getOptB()).get() != null) {
						final MapperS<OptB> optB = item.<OptB>map("getOptB", outer2 -> outer2.getOptB());
						return MapperS.of(fnB.evaluate(optB.get()));
					}
					return MapperS.<String>ofNull();
				}).getMulti();
			
			return outs;
		}
	}
}
