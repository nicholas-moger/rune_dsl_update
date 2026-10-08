package test.fmeta026.util;

import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.metafields.FieldWithMetaInteger;
import java.util.Collections;
import java.util.List;
import test.fmeta026.A;
import test.fmeta026.B;
import test.fmeta026.C;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

public class ADeepPathUtil {
	public List<FieldWithMetaInteger> chooseProp(A a) {
		final MapperS<B> b = MapperS.of(a).<B>map("getB", _a -> _a.getB());
		if (exists(b).getOrDefault(false)) {
			return b.<FieldWithMetaInteger>mapC("getProp", _b -> _b.getProp()).getMulti();
		}
		final MapperS<C> c = MapperS.of(a).<C>map("getC", _a -> _a.getC());
		if (exists(c).getOrDefault(false)) {
			return c.<FieldWithMetaInteger>mapC("getProp", _c -> _c.getProp()).getMulti();
		}
		return Collections.<FieldWithMetaInteger>emptyList();
	}
	
}
