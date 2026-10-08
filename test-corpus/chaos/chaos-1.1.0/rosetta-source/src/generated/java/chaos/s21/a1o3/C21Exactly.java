package chaos.s21.a1o3;

import chaos.s21.a1o3.meta.C21ExactlyMeta;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * one-of over three optionals.
 * @version 1.0.0
 */
@RosettaDataType(value="C21Exactly", builder=C21Exactly.C21ExactlyBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C21Exactly", model="chaos", builder=C21Exactly.C21ExactlyBuilderImpl.class, version="1.0.0")
public interface C21Exactly extends RosettaModelObject {

	C21ExactlyMeta metaData = new C21ExactlyMeta();

	/*********************** Getter Methods  ***********************/
	String getA();
	String getB();
	String getC();

	/*********************** Build Methods  ***********************/
	C21Exactly build();
	
	C21Exactly.C21ExactlyBuilder toBuilder();
	
	static C21Exactly.C21ExactlyBuilder builder() {
		return new C21Exactly.C21ExactlyBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C21Exactly> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C21Exactly> getType() {
		return C21Exactly.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("a"), String.class, getA(), this);
		processor.processBasic(path.newSubPath("b"), String.class, getB(), this);
		processor.processBasic(path.newSubPath("c"), String.class, getC(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C21ExactlyBuilder extends C21Exactly, RosettaModelObjectBuilder {
		C21Exactly.C21ExactlyBuilder setA(String a);
		C21Exactly.C21ExactlyBuilder setB(String b);
		C21Exactly.C21ExactlyBuilder setC(String c);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("a"), String.class, getA(), this);
			processor.processBasic(path.newSubPath("b"), String.class, getB(), this);
			processor.processBasic(path.newSubPath("c"), String.class, getC(), this);
		}
		

		C21Exactly.C21ExactlyBuilder prune();
	}

	/*********************** Immutable Implementation of C21Exactly  ***********************/
	class C21ExactlyImpl implements C21Exactly {
		private final String a;
		private final String b;
		private final String c;
		
		protected C21ExactlyImpl(C21Exactly.C21ExactlyBuilder builder) {
			this.a = builder.getA();
			this.b = builder.getB();
			this.c = builder.getC();
		}
		
		@Override
		@RosettaAttribute("a")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("a")
		public String getA() {
			return a;
		}
		
		@Override
		@RosettaAttribute("b")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("b")
		public String getB() {
			return b;
		}
		
		@Override
		@RosettaAttribute("c")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("c")
		public String getC() {
			return c;
		}
		
		@Override
		public C21Exactly build() {
			return this;
		}
		
		@Override
		public C21Exactly.C21ExactlyBuilder toBuilder() {
			C21Exactly.C21ExactlyBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C21Exactly.C21ExactlyBuilder builder) {
			ofNullable(getA()).ifPresent(builder::setA);
			ofNullable(getB()).ifPresent(builder::setB);
			ofNullable(getC()).ifPresent(builder::setC);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C21Exactly _that = getType().cast(o);
		
			if (!Objects.equals(a, _that.getA())) return false;
			if (!Objects.equals(b, _that.getB())) return false;
			if (!Objects.equals(c, _that.getC())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (a != null ? a.hashCode() : 0);
			_result = 31 * _result + (b != null ? b.hashCode() : 0);
			_result = 31 * _result + (c != null ? c.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C21Exactly {" +
				"a=" + this.a + ", " +
				"b=" + this.b + ", " +
				"c=" + this.c +
			'}';
		}
	}

	/*********************** Builder Implementation of C21Exactly  ***********************/
	class C21ExactlyBuilderImpl implements C21Exactly.C21ExactlyBuilder {
	
		protected String a;
		protected String b;
		protected String c;
		
		@Override
		@RosettaAttribute("a")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("a")
		public String getA() {
			return a;
		}
		
		@Override
		@RosettaAttribute("b")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("b")
		public String getB() {
			return b;
		}
		
		@Override
		@RosettaAttribute("c")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("c")
		public String getC() {
			return c;
		}
		
		@RosettaAttribute("a")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("a")
		@Override
		public C21Exactly.C21ExactlyBuilder setA(String _a) {
			this.a = _a == null ? null : _a;
			return this;
		}
		
		@RosettaAttribute("b")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("b")
		@Override
		public C21Exactly.C21ExactlyBuilder setB(String _b) {
			this.b = _b == null ? null : _b;
			return this;
		}
		
		@RosettaAttribute("c")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("c")
		@Override
		public C21Exactly.C21ExactlyBuilder setC(String _c) {
			this.c = _c == null ? null : _c;
			return this;
		}
		
		@Override
		public C21Exactly build() {
			return new C21Exactly.C21ExactlyImpl(this);
		}
		
		@Override
		public C21Exactly.C21ExactlyBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C21Exactly.C21ExactlyBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getA()!=null) return true;
			if (getB()!=null) return true;
			if (getC()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C21Exactly.C21ExactlyBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C21Exactly.C21ExactlyBuilder o = (C21Exactly.C21ExactlyBuilder) other;
			
			
			merger.mergeBasic(getA(), o.getA(), this::setA);
			merger.mergeBasic(getB(), o.getB(), this::setB);
			merger.mergeBasic(getC(), o.getC(), this::setC);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C21Exactly _that = getType().cast(o);
		
			if (!Objects.equals(a, _that.getA())) return false;
			if (!Objects.equals(b, _that.getB())) return false;
			if (!Objects.equals(c, _that.getC())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (a != null ? a.hashCode() : 0);
			_result = 31 * _result + (b != null ? b.hashCode() : 0);
			_result = 31 * _result + (c != null ? c.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C21ExactlyBuilder {" +
				"a=" + this.a + ", " +
				"b=" + this.b + ", " +
				"c=" + this.c +
			'}';
		}
	}
}
