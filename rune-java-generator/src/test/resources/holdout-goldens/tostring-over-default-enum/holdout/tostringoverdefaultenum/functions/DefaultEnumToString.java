package holdout.tostringoverdefaultenum.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import holdout.tostringoverdefaultenum.Books;
import holdout.tostringoverdefaultenum.SideEnum;
import java.util.ArrayList;
import java.util.List;


@ImplementedBy(DefaultEnumToString.DefaultEnumToStringDefault.class)
public abstract class DefaultEnumToString implements RosettaFunction {

	/**
	* @param b 
	* @return outs 
	*/
	public List<String> evaluate(Books b) {
		List<String> outs = doEvaluate(b);
		
		return outs;
	}

	protected abstract List<String> doEvaluate(Books b);

	public static class DefaultEnumToStringDefault extends DefaultEnumToString {
		@Override
		protected List<String> doEvaluate(Books b) {
			List<String> outs = new ArrayList<>();
			return assignOutput(outs, b);
		}
		
		protected List<String> assignOutput(List<String> outs, Books b) {
			outs.addAll(MapperS.of(MapperS.of(b).<SideEnum>map("getSide", books -> books.getSide()).getOrDefault(SideEnum.LONG)).map("to-string", SideEnum::toDisplayString).getMulti());
			
			return outs;
		}
	}
}
