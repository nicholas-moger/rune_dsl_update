package holdout.tostringoverdefaultenum.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.metafields.FieldWithMetaString;
import holdout.tostringoverdefaultenum.Books;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@ImplementedBy(DefaultMetaString.DefaultMetaStringDefault.class)
public abstract class DefaultMetaString implements RosettaFunction {

	/**
	* @param b 
	* @return outs 
	*/
	public List<String> evaluate(Books b) {
		List<String> outs = doEvaluate(b);
		
		return outs;
	}

	protected abstract List<String> doEvaluate(Books b);

	public static class DefaultMetaStringDefault extends DefaultMetaString {
		@Override
		protected List<String> doEvaluate(Books b) {
			List<String> outs = new ArrayList<>();
			return assignOutput(outs, b);
		}
		
		protected List<String> assignOutput(List<String> outs, Books b) {
			final String string = MapperS.of(b).<FieldWithMetaString>map("getCoded", books -> books.getCoded()).<String>map("Type coercion", fieldWithMetaString -> fieldWithMetaString == null ? null : fieldWithMetaString.getValue()).getOrDefault("none");
			if (string == null) {
				outs.addAll(Collections.<String>emptyList());
			} else {
				outs.addAll(Collections.singletonList(string));
			}
			
			return outs;
		}
	}
}
