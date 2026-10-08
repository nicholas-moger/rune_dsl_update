package chaos.s29.a1o1.util;

import chaos.s29.a1o1.C29Inner;
import chaos.s29.a1o1.C29Leaf;
import chaos.s29.a1o1.C29Note;
import chaos.s29.a1o1.C29Outer;
import chaos.s29.a1o1.C29Wrap;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.metafields.FieldWithMetaString;
import java.util.Collections;
import java.util.List;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

public class C29OuterDeepPathUtil {
	public List<FieldWithMetaString> chooseCodes(C29Outer c29Outer) {
		final MapperS<C29Note> c29Note = MapperS.of(c29Outer).<C29Note>map("getC29Note", _c29Outer -> _c29Outer.getC29Note());
		if (exists(c29Note).getOrDefault(false)) {
			return c29Note.<FieldWithMetaString>mapC("getCodes", _c29Note -> _c29Note.getCodes()).getMulti();
		}
		final MapperS<C29Wrap> c29Wrap = MapperS.of(c29Outer).<C29Wrap>map("getC29Wrap", _c29Outer -> _c29Outer.getC29Wrap());
		if (exists(c29Wrap).getOrDefault(false)) {
			return c29Wrap.<FieldWithMetaString>mapC("getCodes", _c29Wrap -> _c29Wrap.getCodes()).getMulti();
		}
		return Collections.<FieldWithMetaString>emptyList();
	}
	
	public FieldWithMetaString chooseCode(C29Outer c29Outer) {
		final MapperS<C29Note> c29Note = MapperS.of(c29Outer).<C29Note>map("getC29Note", _c29Outer -> _c29Outer.getC29Note());
		if (exists(c29Note).getOrDefault(false)) {
			return c29Note.<FieldWithMetaString>map("getCode", _c29Note -> _c29Note.getCode()).get();
		}
		final MapperS<C29Wrap> c29Wrap = MapperS.of(c29Outer).<C29Wrap>map("getC29Wrap", _c29Outer -> _c29Outer.getC29Wrap());
		if (exists(c29Wrap).getOrDefault(false)) {
			return c29Wrap.<FieldWithMetaString>map("getCode", _c29Wrap -> _c29Wrap.getCode()).get();
		}
		return FieldWithMetaString.builder().build();
	}
	
	public List<C29Inner> chooseInners(C29Outer c29Outer) {
		final MapperS<C29Note> c29Note = MapperS.of(c29Outer).<C29Note>map("getC29Note", _c29Outer -> _c29Outer.getC29Note());
		if (exists(c29Note).getOrDefault(false)) {
			return c29Note.<C29Inner>mapC("getInners", _c29Note -> _c29Note.getInners()).getMulti();
		}
		final MapperS<C29Wrap> c29Wrap = MapperS.of(c29Outer).<C29Wrap>map("getC29Wrap", _c29Outer -> _c29Outer.getC29Wrap());
		if (exists(c29Wrap).getOrDefault(false)) {
			return c29Wrap.<C29Inner>mapC("getInners", _c29Wrap -> _c29Wrap.getInners()).getMulti();
		}
		return Collections.<C29Inner>emptyList();
	}
	
	public List<C29Leaf> chooseLeaves(C29Outer c29Outer) {
		final MapperS<C29Note> c29Note = MapperS.of(c29Outer).<C29Note>map("getC29Note", _c29Outer -> _c29Outer.getC29Note());
		if (exists(c29Note).getOrDefault(false)) {
			return c29Note.<C29Leaf>mapC("getLeaves", _c29Note -> _c29Note.getLeaves()).getMulti();
		}
		final MapperS<C29Wrap> c29Wrap = MapperS.of(c29Outer).<C29Wrap>map("getC29Wrap", _c29Outer -> _c29Outer.getC29Wrap());
		if (exists(c29Wrap).getOrDefault(false)) {
			return c29Wrap.<C29Leaf>mapC("getLeaves", _c29Wrap -> _c29Wrap.getLeaves()).getMulti();
		}
		return Collections.<C29Leaf>emptyList();
	}
	
	public String chooseText(C29Outer c29Outer) {
		final MapperS<C29Note> c29Note = MapperS.of(c29Outer).<C29Note>map("getC29Note", _c29Outer -> _c29Outer.getC29Note());
		if (exists(c29Note).getOrDefault(false)) {
			return c29Note.<String>map("getText", _c29Note -> _c29Note.getText()).get();
		}
		final MapperS<C29Wrap> c29Wrap = MapperS.of(c29Outer).<C29Wrap>map("getC29Wrap", _c29Outer -> _c29Outer.getC29Wrap());
		if (exists(c29Wrap).getOrDefault(false)) {
			return c29Wrap.<String>map("getText", _c29Wrap -> _c29Wrap.getText()).get();
		}
		return null;
	}
	
}
