package com.rosetta.test.model.agreement;

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
import com.rosetta.test.model.agreement.meta.FooMeta;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * @version test
 */
@RosettaDataType(value="Foo", builder=Foo.FooBuilderImpl.class, version="test")
@RuneDataType(value="Foo", model="com", builder=Foo.FooBuilderImpl.class, version="test")
public interface Foo extends RosettaModelObject {

	FooMeta metaData = new FooMeta();

	/*********************** Getter Methods  ***********************/
	Bar getBar1();
	Bar getBar2();

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
		processRosetta(path.newSubPath("bar1"), processor, Bar.class, getBar1());
		processRosetta(path.newSubPath("bar2"), processor, Bar.class, getBar2());
	}
	

	/*********************** Builder Interface  ***********************/
	interface FooBuilder extends Foo, RosettaModelObjectBuilder {
		Bar.BarBuilder getOrCreateBar1();
		@Override
		Bar.BarBuilder getBar1();
		Bar.BarBuilder getOrCreateBar2();
		@Override
		Bar.BarBuilder getBar2();
		Foo.FooBuilder setBar1(Bar bar1);
		Foo.FooBuilder setBar2(Bar bar2);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("bar1"), processor, Bar.BarBuilder.class, getBar1());
			processRosetta(path.newSubPath("bar2"), processor, Bar.BarBuilder.class, getBar2());
		}
		

		Foo.FooBuilder prune();
	}

	/*********************** Immutable Implementation of Foo  ***********************/
	class FooImpl implements Foo {
		private final Bar bar1;
		private final Bar bar2;
		
		protected FooImpl(Foo.FooBuilder builder) {
			this.bar1 = ofNullable(builder.getBar1()).map(f->f.build()).orElse(null);
			this.bar2 = ofNullable(builder.getBar2()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("bar1")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("bar1")
		public Bar getBar1() {
			return bar1;
		}
		
		@Override
		@RosettaAttribute("bar2")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("bar2")
		public Bar getBar2() {
			return bar2;
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
			ofNullable(getBar1()).ifPresent(builder::setBar1);
			ofNullable(getBar2()).ifPresent(builder::setBar2);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Foo _that = getType().cast(o);
		
			if (!Objects.equals(bar1, _that.getBar1())) return false;
			if (!Objects.equals(bar2, _that.getBar2())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (bar1 != null ? bar1.hashCode() : 0);
			_result = 31 * _result + (bar2 != null ? bar2.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Foo {" +
				"bar1=" + this.bar1 + ", " +
				"bar2=" + this.bar2 +
			'}';
		}
	}

	/*********************** Builder Implementation of Foo  ***********************/
	class FooBuilderImpl implements Foo.FooBuilder {
	
		protected Bar.BarBuilder bar1;
		protected Bar.BarBuilder bar2;
		
		@Override
		@RosettaAttribute("bar1")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("bar1")
		public Bar.BarBuilder getBar1() {
			return bar1;
		}
		
		@Override
		public Bar.BarBuilder getOrCreateBar1() {
			Bar.BarBuilder result;
			if (bar1!=null) {
				result = bar1;
			}
			else {
				result = bar1 = Bar.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("bar2")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("bar2")
		public Bar.BarBuilder getBar2() {
			return bar2;
		}
		
		@Override
		public Bar.BarBuilder getOrCreateBar2() {
			Bar.BarBuilder result;
			if (bar2!=null) {
				result = bar2;
			}
			else {
				result = bar2 = Bar.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("bar1")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("bar1")
		@Override
		public Foo.FooBuilder setBar1(Bar _bar1) {
			this.bar1 = _bar1 == null ? null : _bar1.toBuilder();
			return this;
		}
		
		@RosettaAttribute("bar2")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("bar2")
		@Override
		public Foo.FooBuilder setBar2(Bar _bar2) {
			this.bar2 = _bar2 == null ? null : _bar2.toBuilder();
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
			if (bar1!=null && !bar1.prune().hasData()) bar1 = null;
			if (bar2!=null && !bar2.prune().hasData()) bar2 = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getBar1()!=null && getBar1().hasData()) return true;
			if (getBar2()!=null && getBar2().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Foo.FooBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Foo.FooBuilder o = (Foo.FooBuilder) other;
			
			merger.mergeRosetta(getBar1(), o.getBar1(), this::setBar1);
			merger.mergeRosetta(getBar2(), o.getBar2(), this::setBar2);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Foo _that = getType().cast(o);
		
			if (!Objects.equals(bar1, _that.getBar1())) return false;
			if (!Objects.equals(bar2, _that.getBar2())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (bar1 != null ? bar1.hashCode() : 0);
			_result = 31 * _result + (bar2 != null ? bar2.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "FooBuilder {" +
				"bar1=" + this.bar1 + ", " +
				"bar2=" + this.bar2 +
			'}';
		}
	}
}
