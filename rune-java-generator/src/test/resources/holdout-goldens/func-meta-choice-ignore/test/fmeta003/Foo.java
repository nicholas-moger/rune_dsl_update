package test.fmeta003;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Required;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.math.BigDecimal;
import java.util.Objects;
import test.fmeta003.meta.FooMeta;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="Foo", builder=Foo.FooBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Foo", model="test", builder=Foo.FooBuilderImpl.class, version="0.0.0")
public interface Foo extends RosettaModelObject {

	FooMeta metaData = new FooMeta();

	/*********************** Getter Methods  ***********************/
	String getA();
	Integer getB();
	BigDecimal getC();

	/*********************** Build Methods  ***********************/
	Foo build();
	
	Foo.FooBuilder toBuilder();
	
	static Foo.FooBuilder builder() {
		return new Foo.FooBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Foo> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Foo> getType() {
		return Foo.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("a"), String.class, getA(), this);
		processor.processBasic(path.newSubPath("b"), Integer.class, getB(), this);
		processor.processBasic(path.newSubPath("c"), BigDecimal.class, getC(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface FooBuilder extends Foo, RosettaModelObjectBuilder {
		Foo.FooBuilder setA(String a);
		Foo.FooBuilder setB(Integer b);
		Foo.FooBuilder setC(BigDecimal c);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("a"), String.class, getA(), this);
			processor.processBasic(path.newSubPath("b"), Integer.class, getB(), this);
			processor.processBasic(path.newSubPath("c"), BigDecimal.class, getC(), this);
		}
		

		Foo.FooBuilder prune();
	}

	/*********************** Immutable Implementation of Foo  ***********************/
	class FooImpl implements Foo {
		private final String a;
		private final Integer b;
		private final BigDecimal c;
		
		protected FooImpl(Foo.FooBuilder builder) {
			this.a = builder.getA();
			this.b = builder.getB();
			this.c = builder.getC();
		}
		
		@Override
		@RosettaAttribute("a")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("a")
		public String getA() {
			return a;
		}
		
		@Override
		@RosettaAttribute("b")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("b")
		public Integer getB() {
			return b;
		}
		
		@Override
		@RosettaAttribute("c")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("c")
		public BigDecimal getC() {
			return c;
		}
		
		@Override
		public Foo build() {
			return this;
		}
		
		@Override
		public Foo.FooBuilder toBuilder() {
			Foo.FooBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Foo.FooBuilder builder) {
			ofNullable(getA()).ifPresent(builder::setA);
			ofNullable(getB()).ifPresent(builder::setB);
			ofNullable(getC()).ifPresent(builder::setC);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Foo _that = getType().cast(o);
		
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
			return "Foo {" +
				"a=" + this.a + ", " +
				"b=" + this.b + ", " +
				"c=" + this.c +
			'}';
		}
	}

	/*********************** Builder Implementation of Foo  ***********************/
	class FooBuilderImpl implements Foo.FooBuilder {
	
		protected String a;
		protected Integer b;
		protected BigDecimal c;
		
		@Override
		@RosettaAttribute("a")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("a")
		public String getA() {
			return a;
		}
		
		@Override
		@RosettaAttribute("b")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("b")
		public Integer getB() {
			return b;
		}
		
		@Override
		@RosettaAttribute("c")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("c")
		public BigDecimal getC() {
			return c;
		}
		
		@RosettaAttribute("a")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("a")
		@Override
		public Foo.FooBuilder setA(String _a) {
			this.a = _a == null ? null : _a;
			return this;
		}
		
		@RosettaAttribute("b")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("b")
		@Override
		public Foo.FooBuilder setB(Integer _b) {
			this.b = _b == null ? null : _b;
			return this;
		}
		
		@RosettaAttribute("c")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("c")
		@Override
		public Foo.FooBuilder setC(BigDecimal _c) {
			this.c = _c == null ? null : _c;
			return this;
		}
		
		@Override
		public Foo build() {
			return new Foo.FooImpl(this);
		}
		
		@Override
		public Foo.FooBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Foo.FooBuilder prune() {
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
		public Foo.FooBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Foo.FooBuilder o = (Foo.FooBuilder) other;
			
			
			merger.mergeBasic(getA(), o.getA(), this::setA);
			merger.mergeBasic(getB(), o.getB(), this::setB);
			merger.mergeBasic(getC(), o.getC(), this::setC);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Foo _that = getType().cast(o);
		
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
			return "FooBuilder {" +
				"a=" + this.a + ", " +
				"b=" + this.b + ", " +
				"c=" + this.c +
			'}';
		}
	}
}
