package chaos.s31.a6enum.functions;

import chaos.s31.a6enum.C31Books;
import chaos.s31.a6enum.C31MoveEnum;
import chaos.s31.a6enum.C31SideEnum;
import chaos.s31.a6enum.C31TopEnum;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.metafields.FieldWithMetaString;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@ImplementedBy(C31Defaults.C31DefaultsDefault.class)
public abstract class C31Defaults implements RosettaFunction {

	/**
	* @param b 
	* @return outs 
	*/
	public List<String> evaluate(C31Books b) {
		List<String> outs = doEvaluate(b);
		
		return outs;
	}

	protected abstract List<String> doEvaluate(C31Books b);

	public static class C31DefaultsDefault extends C31Defaults {
		@Override
		protected List<String> doEvaluate(C31Books b) {
			List<String> outs = new ArrayList<>();
			return assignOutput(outs, b);
		}
		
		protected List<String> assignOutput(List<String> outs, C31Books b) {
			final String string = MapperS.of(b).<FieldWithMetaString>map("getCoded", c31Books -> c31Books.getCoded()).<String>map("Type coercion", fieldWithMetaString -> fieldWithMetaString == null ? null : fieldWithMetaString.getValue()).getOrDefault("none");
			if (string == null) {
				outs.addAll(Collections.<String>emptyList());
			} else {
				outs.addAll(Collections.singletonList(string));
			}
			
			outs.addAll(MapperS.of(MapperS.of(b).<C31SideEnum>map("getSide", c31Books -> c31Books.getSide()).getOrDefault(C31SideEnum.LONG)).map("to-string", C31SideEnum::toDisplayString).getMulti());
			
			outs.addAll(MapperS.of(MapperS.of(b).<C31SideEnum>map("getSide", c31Books -> c31Books.getSide()).getOrDefault(C31SideEnum.SHORT)).map("to-string", C31SideEnum::toDisplayString).getMulti());
			
			outs.addAll(MapperS.of(MapperS.of(b).<C31MoveEnum>map("getMove", c31Books -> c31Books.getMove()).getOrDefault(C31MoveEnum.FLAT)).map("to-string", C31MoveEnum::toDisplayString).getMulti());
			
			outs.addAll(MapperS.of(MapperS.of(b).<C31TopEnum>map("getTop", c31Books -> c31Books.getTop()).getOrDefault(C31TopEnum.A)).map("to-string", C31TopEnum::toDisplayString).getMulti());
			
			return outs;
		}
	}
}
