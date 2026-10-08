package chaos.s03.a1o3.util;

import chaos.s03.a1o3.C3Note;
import chaos.s03.a1o3.C3Outer;
import chaos.s03.a1o3.C3Wrap;
import com.rosetta.model.lib.mapper.MapperS;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

public class C3OuterDeepPathUtil {
	public String chooseText(C3Outer c3Outer) {
		final MapperS<C3Wrap> c3Wrap = MapperS.of(c3Outer).<C3Wrap>map("getC3Wrap", _c3Outer -> _c3Outer.getC3Wrap());
		if (exists(c3Wrap).getOrDefault(false)) {
			return c3Wrap.<String>map("getText", _c3Wrap -> _c3Wrap.getText()).get();
		}
		final MapperS<C3Note> c3Note = MapperS.of(c3Outer).<C3Note>map("getC3Note", _c3Outer -> _c3Outer.getC3Note());
		if (exists(c3Note).getOrDefault(false)) {
			return c3Note.<String>map("getText", _c3Note -> _c3Note.getText()).get();
		}
		return null;
	}
	
}
