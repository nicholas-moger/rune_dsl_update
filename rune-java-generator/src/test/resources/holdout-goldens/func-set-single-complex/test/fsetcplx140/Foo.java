package test.fsetcplx140;

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
import java.util.Objects;
import test.fsetcplx140.meta.FooMeta;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="Foo", builder=Foo.FooBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Foo", model="test", builder=Foo.FooBuilderImpl.class, version="0.0.0")
public interface Foo extends RosettaModelObject {

	FooMeta metaData = new FooMeta();

	/*********************** Getter Methods  ***********************/
	String getAttr();

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
		processor.processBasic(path.newSubPath("attr"), String.class, getAttr(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface FooBuilder extends Foo, RosettaModelObjectBuilder {
		Foo.FooBuilder setAttr(String attr);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("attr"), String.class, getAttr(), this);
		}
		

		Foo.FooBuilder prune();
	}

	/*********************** Immutable Implementation of Foo  ***********************/
	class FooImpl implements Foo {
		private final String attr;
		
		protected FooImpl(Foo.FooBuilder builder) {
			this.attr = builder.getAttr();
		}
		
		@Override
		@RosettaAttribute("attr")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("attr")
		public String getAttr() {
			return attr;
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
			ofNullable(getAttr()).ifPresent(builder::setAttr);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Foo _that = getType().cast(o);
		
			if (!Objects.equals(attr, _that.getAttr())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (attr != null ? attr.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Foo {" +
				"attr=" + this.attr +
			'}';
		}
	}

	/*********************** Builder Implementation of Foo  ***********************/
	class FooBuilderImpl implements Foo.FooBuilder {
	
		protected String attr;
		
		@Override
		@RosettaAttribute("attr")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("attr")
		public String getAttr() {
			return attr;
		}
		
		@RosettaAttribute("attr")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("attr")
		@Override
		public Foo.FooBuilder setAttr(String _attr) {
			this.attr = _attr == null ? null : _attr;
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
			if (getAttr()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Foo.FooBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Foo.FooBuilder o = (Foo.FooBuilder) other;
			
			
			merger.mergeBasic(getAttr(), o.getAttr(), this::setAttr);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Foo _that = getType().cast(o);
		
			if (!Objects.equals(attr, _that.getAttr())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (attr != null ? attr.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "FooBuilder {" +
				"attr=" + this.attr +
			'}';
		}
	}
}
