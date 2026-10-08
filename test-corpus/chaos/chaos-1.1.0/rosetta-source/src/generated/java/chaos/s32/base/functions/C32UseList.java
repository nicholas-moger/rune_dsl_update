package chaos.s32.base.functions;

import chaos.s32.base.List;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;


@ImplementedBy(C32UseList.C32UseListDefault.class)
public abstract class C32UseList implements RosettaFunction {

	/**
	* @param l 
	* @return n 
	*/
	public Integer evaluate(List l) {
		Integer n = doEvaluate(l);
		
		return n;
	}

	protected abstract Integer doEvaluate(List l);

	public static class C32UseListDefault extends C32UseList {
		@Override
		protected Integer doEvaluate(List l) {
			Integer n = null;
			return assignOutput(n, l);
		}
		
		protected Integer assignOutput(Integer n, List l) {
			n = MapperS.of(l).<String>mapC("getItems", list -> list.getItems()).resultCount();
			
			return n;
		}
	}
}
