package test.singletolistset;

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
import test.singletolistset.meta.FooMeta;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="Foo", builder=Foo.FooBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Foo", model="test", builder=Foo.FooBuilderImpl.class, version="0.0.0")
public interface Foo extends RosettaModelObject {

	FooMeta metaData = new FooMeta();

	/*********************** Getter Methods  ***********************/
	Baz getBaz();

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
		processRosetta(path.newSubPath("baz"), processor, Baz.class, getBaz());
	}
	

	/*********************** Builder Interface  ***********************/
	interface FooBuilder extends Foo, RosettaModelObjectBuilder {
		Baz.BazBuilder getOrCreateBaz();
		@Override
		Baz.BazBuilder getBaz();
		Foo.FooBuilder setBaz(Baz baz);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("baz"), processor, Baz.BazBuilder.class, getBaz());
		}
		

		Foo.FooBuilder prune();
	}

	/*********************** Immutable Implementation of Foo  ***********************/
	class FooImpl implements Foo {
		private final Baz baz;
		
		protected FooImpl(Foo.FooBuilder builder) {
			this.baz = ofNullable(builder.getBaz()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("baz")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("baz")
		public Baz getBaz() {
			return baz;
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
			ofNullable(getBaz()).ifPresent(builder::setBaz);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Foo _that = getType().cast(o);
		
			if (!Objects.equals(baz, _that.getBaz())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (baz != null ? baz.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Foo {" +
				"baz=" + this.baz +
			'}';
		}
	}

	/*********************** Builder Implementation of Foo  ***********************/
	class FooBuilderImpl implements Foo.FooBuilder {
	
		protected Baz.BazBuilder baz;
		
		@Override
		@RosettaAttribute("baz")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("baz")
		public Baz.BazBuilder getBaz() {
			return baz;
		}
		
		@Override
		public Baz.BazBuilder getOrCreateBaz() {
			Baz.BazBuilder result;
			if (baz!=null) {
				result = baz;
			}
			else {
				result = baz = Baz.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("baz")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("baz")
		@Override
		public Foo.FooBuilder setBaz(Baz _baz) {
			this.baz = _baz == null ? null : _baz.toBuilder();
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
			if (baz!=null && !baz.prune().hasData()) baz = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getBaz()!=null && getBaz().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Foo.FooBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Foo.FooBuilder o = (Foo.FooBuilder) other;
			
			merger.mergeRosetta(getBaz(), o.getBaz(), this::setBaz);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Foo _that = getType().cast(o);
		
			if (!Objects.equals(baz, _that.getBaz())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (baz != null ? baz.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "FooBuilder {" +
				"baz=" + this.baz +
			'}';
		}
	}
}
