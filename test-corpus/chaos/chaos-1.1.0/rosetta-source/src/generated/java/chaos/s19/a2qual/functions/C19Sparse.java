package chaos.s19.a2qual.functions;

import chaos.s19.a2qual.C19Whole;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import java.util.Optional;
import javax.inject.Inject;


@ImplementedBy(C19Sparse.C19SparseDefault.class)
public abstract class C19Sparse implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param nm 
	* @return w 
	*/
	public C19Whole evaluate(String nm) {
		C19Whole.C19WholeBuilder wBuilder = doEvaluate(nm);
		
		final C19Whole w;
		if (wBuilder == null) {
			w = null;
		} else {
			w = wBuilder.build();
			objectValidator.validate(C19Whole.class, w);
		}
		
		return w;
	}

	protected abstract C19Whole.C19WholeBuilder doEvaluate(String nm);

	public static class C19SparseDefault extends C19Sparse {
		@Override
		protected C19Whole.C19WholeBuilder doEvaluate(String nm) {
			C19Whole.C19WholeBuilder w = C19Whole.builder();
			return assignOutput(w, nm);
		}
		
		protected C19Whole.C19WholeBuilder assignOutput(C19Whole.C19WholeBuilder w, String nm) {
			w = toBuilder(C19Whole.builder()
				.setName(nm)
				.build());
			
			return Optional.ofNullable(w)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
