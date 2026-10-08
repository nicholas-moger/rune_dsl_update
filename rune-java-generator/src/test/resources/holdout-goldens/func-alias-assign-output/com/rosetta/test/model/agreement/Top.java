package com.rosetta.test.model.agreement;

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
import com.rosetta.test.model.agreement.meta.TopMeta;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * @version test
 */
@RosettaDataType(value="Top", builder=Top.TopBuilderImpl.class, version="test")
@RuneDataType(value="Top", model="com", builder=Top.TopBuilderImpl.class, version="test")
public interface Top extends RosettaModelObject {

	TopMeta metaData = new TopMeta();

	/*********************** Getter Methods  ***********************/
	Foo getFoo();

	/*********************** Build Methods  ***********************/
	Top build();
	
	Top.TopBuilder toBuilder();
	
	static Top.TopBuilder builder() {
		return new Top.TopBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Top> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Top> getType() {
		return Top.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("foo"), processor, Foo.class, getFoo());
	}
	

	/*********************** Builder Interface  ***********************/
	interface TopBuilder extends Top, RosettaModelObjectBuilder {
		Foo.FooBuilder getOrCreateFoo();
		@Override
		Foo.FooBuilder getFoo();
		Top.TopBuilder setFoo(Foo foo);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("foo"), processor, Foo.FooBuilder.class, getFoo());
		}
		

		Top.TopBuilder prune();
	}

	/*********************** Immutable Implementation of Top  ***********************/
	class TopImpl implements Top {
		private final Foo foo;
		
		protected TopImpl(Top.TopBuilder builder) {
			this.foo = ofNullable(builder.getFoo()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("foo")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("foo")
		public Foo getFoo() {
			return foo;
		}
		
		@Override
		public Top build() {
			return this;
		}
		
		@Override
		public Top.TopBuilder toBuilder() {
			Top.TopBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Top.TopBuilder builder) {
			ofNullable(getFoo()).ifPresent(builder::setFoo);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Top _that = getType().cast(o);
		
			if (!Objects.equals(foo, _that.getFoo())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (foo != null ? foo.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Top {" +
				"foo=" + this.foo +
			'}';
		}
	}

	/*********************** Builder Implementation of Top  ***********************/
	class TopBuilderImpl implements Top.TopBuilder {
	
		protected Foo.FooBuilder foo;
		
		@Override
		@RosettaAttribute("foo")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("foo")
		public Foo.FooBuilder getFoo() {
			return foo;
		}
		
		@Override
		public Foo.FooBuilder getOrCreateFoo() {
			Foo.FooBuilder result;
			if (foo!=null) {
				result = foo;
			}
			else {
				result = foo = Foo.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("foo")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("foo")
		@Override
		public Top.TopBuilder setFoo(Foo _foo) {
			this.foo = _foo == null ? null : _foo.toBuilder();
			return this;
		}
		
		@Override
		public Top build() {
			return new Top.TopImpl(this);
		}
		
		@Override
		public Top.TopBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Top.TopBuilder prune() {
			if (foo!=null && !foo.prune().hasData()) foo = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getFoo()!=null && getFoo().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Top.TopBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Top.TopBuilder o = (Top.TopBuilder) other;
			
			merger.mergeRosetta(getFoo(), o.getFoo(), this::setFoo);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Top _that = getType().cast(o);
		
			if (!Objects.equals(foo, _that.getFoo())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (foo != null ? foo.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "TopBuilder {" +
				"foo=" + this.foo +
			'}';
		}
	}
}
