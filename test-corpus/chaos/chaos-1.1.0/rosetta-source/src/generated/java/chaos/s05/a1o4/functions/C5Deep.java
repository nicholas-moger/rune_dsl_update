package chaos.s05.a1o4.functions;

import chaos.s05.a1o4.C5Item;
import chaos.s05.a1o4.C5Sub;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperListOfLists;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@ImplementedBy(C5Deep.C5DeepDefault.class)
public abstract class C5Deep implements RosettaFunction {

	/**
	* @param items 
	* @return names 
	*/
	public List<String> evaluate(List<? extends C5Item> items) {
		List<String> names = doEvaluate(items);
		
		return names;
	}

	protected abstract List<String> doEvaluate(List<? extends C5Item> items);

	public static class C5DeepDefault extends C5Deep {
		@Override
		protected List<String> doEvaluate(List<? extends C5Item> items) {
			if (items == null) {
				items = Collections.emptyList();
			}
			List<String> names = new ArrayList<>();
			return assignOutput(names, items);
		}
		
		protected List<String> assignOutput(List<String> names, List<? extends C5Item> items) {
			final MapperC<C5Item> thenArg0 = MapperC.<C5Item>of(items);
			final MapperListOfLists<String> thenArg1 = thenArg0
				.mapItemToList(x -> x.<C5Sub>mapC("getSub", c5Item -> c5Item.getSub())
					.mapItem(y -> MapperMaths.<String, String, String>add(MapperMaths.<String, String, String>add(y.<String>map("getName", c5Sub -> c5Sub.getName()), MapperS.of("-")), MapperS.of(y.<BigDecimal>mapC("getVals", c5Sub -> c5Sub.getVals()).resultCount()).map("to-string", Object::toString))));
			names.addAll(thenArg1
				.flattenList().getMulti());
			
			return names;
		}
	}
}
