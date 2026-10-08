package test.chswitch.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import test.chswitch.Base;
import test.chswitch.Sub1;
import test.chswitch.Sub2;


@ImplementedBy(ExtendsSubject.ExtendsSubjectDefault.class)
public abstract class ExtendsSubject implements RosettaFunction {

	/**
	* @param bases 
	* @return texts 
	*/
	public List<String> evaluate(List<? extends Base> bases) {
		List<String> texts = doEvaluate(bases);
		
		return texts;
	}

	protected abstract List<String> doEvaluate(List<? extends Base> bases);

	public static class ExtendsSubjectDefault extends ExtendsSubject {
		@Override
		protected List<String> doEvaluate(List<? extends Base> bases) {
			if (bases == null) {
				bases = Collections.emptyList();
			}
			List<String> texts = new ArrayList<>();
			return assignOutput(texts, bases);
		}
		
		protected List<String> assignOutput(List<String> texts, List<? extends Base> bases) {
			texts.addAll(MapperC.<Base>of(bases)
				.mapItem(item -> {
					final Base switchArgument = item.get();
					if (switchArgument == null) {
						return MapperS.<String>ofNull();
					}
					if (switchArgument instanceof Sub1) {
						final Sub1 sub1 = (Sub1) switchArgument;
						return MapperS.of(sub1).<String>map("getA", _sub1 -> _sub1.getA());
					}
					if (switchArgument instanceof Sub2) {
						final Sub2 sub2 = (Sub2) switchArgument;
						return MapperS.of(sub2).<BigDecimal>map("getB", _sub2 -> _sub2.getB()).map("to-string", Object::toString);
					}
					return MapperS.of("none");
				}).getMulti());
			
			return texts;
		}
	}
}
