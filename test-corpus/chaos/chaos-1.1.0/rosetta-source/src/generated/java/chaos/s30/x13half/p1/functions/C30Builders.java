package chaos.s30.x13half.p1.functions;

import chaos.s30.x13half.p1.C30Part;
import chaos.s30.x13half.p1.metafields.ReferenceWithMetaC30Part;
import chaos.s30.x13half.p2.C30Whole;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;


@ImplementedBy(C30Builders.C30BuildersDefault.class)
public abstract class C30Builders implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param ps 
	* @param p 
	* @return ws 
	*/
	public List<? extends C30Whole> evaluate(List<? extends C30Part> ps, C30Part p) {
		List<C30Whole.C30WholeBuilder> wsBuilder = doEvaluate(ps, p);
		
		final List<? extends C30Whole> ws;
		if (wsBuilder == null) {
			ws = null;
		} else {
			ws = wsBuilder.stream().map(C30Whole::build).collect(Collectors.toList());
			objectValidator.validate(C30Whole.class, ws);
		}
		
		return ws;
	}

	protected abstract List<C30Whole.C30WholeBuilder> doEvaluate(List<? extends C30Part> ps, C30Part p);

	public static class C30BuildersDefault extends C30Builders {
		@Override
		protected List<C30Whole.C30WholeBuilder> doEvaluate(List<? extends C30Part> ps, C30Part p) {
			if (ps == null) {
				ps = Collections.emptyList();
			}
			List<C30Whole.C30WholeBuilder> ws = new ArrayList<>();
			return assignOutput(ws, ps, p);
		}
		
		protected List<C30Whole.C30WholeBuilder> assignOutput(List<C30Whole.C30WholeBuilder> ws, List<? extends C30Part> ps, C30Part p) {
			ws = toBuilder(MapperC.<C30Part>of(ps)
				.mapItem(x -> MapperS.of(C30Whole.builder()
					.setName(x.<String>map("getPid", c30Part -> c30Part.getPid()).get())
					.build())).getMulti());
			
			final C30Whole c30Whole0 = C30Whole.builder()
				.setName("tail")
				.setPartRef(ReferenceWithMetaC30Part.builder()
					.setGlobalReference(Optional.ofNullable(p)
						.map(r -> r.getMeta())
						.map(m -> m.getGlobalKey())
						.orElse(null))
					.setExternalReference(Optional.ofNullable(p)
						.map(r -> r.getMeta())
						.map(m -> m.getExternalKey())
						.orElse(null))
					.build())
				.build();
			if (c30Whole0 == null) {
				ws.addAll(toBuilder(Collections.<C30Whole>emptyList()));
			} else {
				ws.addAll(toBuilder(Collections.singletonList(c30Whole0)));
			}
			
			final C30Whole c30Whole1 = C30Whole.builder()
				.setName("empty")
				.setPartRefValue(null)
				.build();
			if (c30Whole1 == null) {
				ws.addAll(toBuilder(Collections.<C30Whole>emptyList()));
			} else {
				ws.addAll(toBuilder(Collections.singletonList(c30Whole1)));
			}
			
			return Optional.ofNullable(ws)
				.map(o -> o.stream().map(i -> i.prune()).collect(Collectors.toList()))
				.orElse(null);
		}
	}
}
