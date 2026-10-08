package holdout.tostringoverdefaultenum.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import holdout.tostringoverdefaultenum.Books;
import holdout.tostringoverdefaultenum.SideEnum;


@ImplementedBy(DefaultEnumToStringSet.DefaultEnumToStringSetDefault.class)
public abstract class DefaultEnumToStringSet implements RosettaFunction {

	/**
	* @param b 
	* @return s 
	*/
	public String evaluate(Books b) {
		String s = doEvaluate(b);
		
		return s;
	}

	protected abstract String doEvaluate(Books b);

	public static class DefaultEnumToStringSetDefault extends DefaultEnumToStringSet {
		@Override
		protected String doEvaluate(Books b) {
			String s = null;
			return assignOutput(s, b);
		}
		
		protected String assignOutput(String s, Books b) {
			s = MapperS.of(MapperS.of(b).<SideEnum>map("getSide", books -> books.getSide()).getOrDefault(SideEnum.LONG)).map("to-string", SideEnum::toDisplayString).get();
			
			return s;
		}
	}
}
