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
	Bar getBar();

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
		processRosetta(path.newSubPath("bar"), processor, Bar.class, getBar());
	}
	

	/*********************** Builder Interface  ***********************/
	interface FooBuilder extends Foo, RosettaModelObjectBuilder {
		Bar.BarBuilder getOrCreateBar();
		@Override
		Bar.BarBuilder getBar();
		Foo.FooBuilder setBar(Bar bar);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("bar"), processor, Bar.BarBuilder.class, getBar());
		}
		

		Foo.FooBuilder prune();
	}

	/*********************** Immutable Implementation of Foo  ***********************/
	class FooImpl implements Foo {
		private final Bar bar;
		
		protected FooImpl(Foo.FooBuilder builder) {
			this.bar = ofNullable(builder.getBar()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("bar")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("bar")
		public Bar getBar() {
			return bar;
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
			ofNullable(getBar()).ifPresent(builder::setBar);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Foo _that = getType().cast(o);
		
			if (!Objects.equals(bar, _that.getBar())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (bar != null ? bar.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Foo {" +
				"bar=" + this.bar +
			'}';
		}
	}

	/*********************** Builder Implementation of Foo  ***********************/
	class FooBuilderImpl implements Foo.FooBuilder {
	
		protected Bar.BarBuilder bar;
		
		@Override
		@RosettaAttribute("bar")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("bar")
		public Bar.BarBuilder getBar() {
			return bar;
		}
		
		@Override
		public Bar.BarBuilder getOrCreateBar() {
			Bar.BarBuilder result;
			if (bar!=null) {
				result = bar;
			}
			else {
				result = bar = Bar.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("bar")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("bar")
		@Override
		public Foo.FooBuilder setBar(Bar _bar) {
			this.bar = _bar == null ? null : _bar.toBuilder();
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
			if (bar!=null && !bar.prune().hasData()) bar = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getBar()!=null && getBar().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Foo.FooBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Foo.FooBuilder o = (Foo.FooBuilder) other;
			
			merger.mergeRosetta(getBar(), o.getBar(), this::setBar);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Foo _that = getType().cast(o);
		
			if (!Objects.equals(bar, _that.getBar())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (bar != null ? bar.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "FooBuilder {" +
				"bar=" + this.bar +
			'}';
		}
	}
}
