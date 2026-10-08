package chaos.s30.a1o3.functions;

import chaos.s30.a1o3.C30Part;
import chaos.s30.a1o3.C30Whole;
import chaos.s30.a1o3.metafields.ReferenceWithMetaC30Part;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.metafields.FieldWithMetaString;
import com.rosetta.model.metafields.MetaFields;
import java.util.Optional;
import javax.inject.Inject;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C30TwoOp.C30TwoOpDefault.class)
public abstract class C30TwoOp implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param p 
	* @param flag 
	* @param a 
	* @return w 
	*/
	public C30Whole evaluate(C30Part p, Boolean flag, String a) {
		C30Whole.C30WholeBuilder wBuilder = doEvaluate(p, flag, a);
		
		final C30Whole w;
		if (wBuilder == null) {
			w = null;
		} else {
			w = wBuilder.build();
			objectValidator.validate(C30Whole.class, w);
		}
		
		return w;
	}

	protected abstract C30Whole.C30WholeBuilder doEvaluate(C30Part p, Boolean flag, String a);

	public static class C30TwoOpDefault extends C30TwoOp {
		@Override
		protected C30Whole.C30WholeBuilder doEvaluate(C30Part p, Boolean flag, String a) {
			C30Whole.C30WholeBuilder w = C30Whole.builder();
			return assignOutput(w, p, flag, a);
		}
		
		protected C30Whole.C30WholeBuilder assignOutput(C30Whole.C30WholeBuilder w, C30Part p, Boolean flag, String a) {
			final String withMetaArgument = "c";
			w = toBuilder(C30Whole.builder()
				.setName("root")
				.setCode(FieldWithMetaString.builder().setValue(withMetaArgument).setMeta(MetaFields.builder().setScheme("chaos")))
				.build(), () -> C30Whole.builder());
			
			w
				.getOrCreateNested()
				.setPartRef(ReferenceWithMetaC30Part.builder()
					.setGlobalReference(Optional.ofNullable(p)
						.map(r -> r.getMeta())
						.map(m -> m.getGlobalKey())
						.orElse(null))
					.setExternalReference(Optional.ofNullable(p)
						.map(r -> r.getMeta())
						.map(m -> m.getExternalKey())
						.orElse(null))
					.build()
				);
			
			String ifThenElseResult0 = "m";
			if ((flag == null ? false : flag)) {
				ifThenElseResult0 = "n";
			}
			w
				.getOrCreateNested()
				.setName(ifThenElseResult0);
			
			final FieldWithMetaString ifThenElseResult1;
			if (exists(MapperS.of(a)).getOrDefault(false)) {
				ifThenElseResult1 = a == null ? FieldWithMetaString.builder().build() : FieldWithMetaString.builder().setValue(a).build();
			} else {
				final String string = "z";
				ifThenElseResult1 = string == null ? FieldWithMetaString.builder().build() : FieldWithMetaString.builder().setValue(string).build();
			}
			w
				.setCode(ifThenElseResult1);
			
			return Optional.ofNullable(w)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
