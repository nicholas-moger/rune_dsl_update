package test.fmeta003.metafields;

import com.rosetta.model.lib.GlobalKey;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.annotations.RuneMetaType;
import com.rosetta.model.lib.meta.BasicRosettaMetaData;
import com.rosetta.model.lib.meta.FieldWithMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.model.metafields.MetaFields;
import java.util.Objects;
import test.fmeta003.Foo;

import static java.util.Optional.ofNullable;

@RosettaDataType(value="FieldWithMetaFoo", builder=FieldWithMetaFoo.FieldWithMetaFooBuilderImpl.class, version="0.0.0")
@RuneDataType(value="FieldWithMetaFoo", model="test", builder=FieldWithMetaFoo.FieldWithMetaFooBuilderImpl.class, version="0.0.0")
public interface FieldWithMetaFoo extends RosettaModelObject, FieldWithMeta<Foo>, GlobalKey {

	FieldWithMetaFooMeta metaData = new FieldWithMetaFooMeta();

	/*********************** Getter Methods  ***********************/
	Foo getValue();
	MetaFields getMeta();

	/*********************** Build Methods  ***********************/
	FieldWithMetaFoo build();
	
	FieldWithMetaFoo.FieldWithMetaFooBuilder toBuilder();
	
	static FieldWithMetaFoo.FieldWithMetaFooBuilder builder() {
		return new FieldWithMetaFoo.FieldWithMetaFooBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends FieldWithMetaFoo> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends FieldWithMetaFoo> getType() {
		return FieldWithMetaFoo.class;
	}
	
	@Override
	default Class<Foo> getValueType() {
		return Foo.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("value"), processor, Foo.class, getValue());
		processRosetta(path.newSubPath("meta"), processor, MetaFields.class, getMeta());
	}
	

	/*********************** Builder Interface  ***********************/
	interface FieldWithMetaFooBuilder extends FieldWithMetaFoo, RosettaModelObjectBuilder, FieldWithMeta.FieldWithMetaBuilder<Foo>, GlobalKey.GlobalKeyBuilder {
		Foo.FooBuilder getOrCreateValue();
		@Override
		Foo.FooBuilder getValue();
		MetaFields.MetaFieldsBuilder getOrCreateMeta();
		@Override
		MetaFields.MetaFieldsBuilder getMeta();
		FieldWithMetaFoo.FieldWithMetaFooBuilder setValue(Foo value);
		FieldWithMetaFoo.FieldWithMetaFooBuilder setMeta(MetaFields meta);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("value"), processor, Foo.FooBuilder.class, getValue());
			processRosetta(path.newSubPath("meta"), processor, MetaFields.MetaFieldsBuilder.class, getMeta());
		}
		

		FieldWithMetaFoo.FieldWithMetaFooBuilder prune();
	}

	/*********************** Immutable Implementation of FieldWithMetaFoo  ***********************/
	class FieldWithMetaFooImpl implements FieldWithMetaFoo {
		private final Foo value;
		private final MetaFields meta;
		
		protected FieldWithMetaFooImpl(FieldWithMetaFoo.FieldWithMetaFooBuilder builder) {
			this.value = ofNullable(builder.getValue()).map(f->f.build()).orElse(null);
			this.meta = ofNullable(builder.getMeta()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("value")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("@data")
		@RuneMetaType
		public Foo getValue() {
			return value;
		}
		
		@Override
		@RosettaAttribute("meta")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("meta")
		@RuneMetaType
		public MetaFields getMeta() {
			return meta;
		}
		
		@Override
		public FieldWithMetaFoo build() {
			return this;
		}
		
		@Override
		public FieldWithMetaFoo.FieldWithMetaFooBuilder toBuilder() {
			FieldWithMetaFoo.FieldWithMetaFooBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(FieldWithMetaFoo.FieldWithMetaFooBuilder builder) {
			ofNullable(getValue()).ifPresent(builder::setValue);
			ofNullable(getMeta()).ifPresent(builder::setMeta);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			FieldWithMetaFoo _that = getType().cast(o);
		
			if (!Objects.equals(value, _that.getValue())) return false;
			if (!Objects.equals(meta, _that.getMeta())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (value != null ? value.hashCode() : 0);
			_result = 31 * _result + (meta != null ? meta.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "FieldWithMetaFoo {" +
				"value=" + this.value + ", " +
				"meta=" + this.meta +
			'}';
		}
	}

	/*********************** Builder Implementation of FieldWithMetaFoo  ***********************/
	class FieldWithMetaFooBuilderImpl implements FieldWithMetaFoo.FieldWithMetaFooBuilder {
	
		protected Foo.FooBuilder value;
		protected MetaFields.MetaFieldsBuilder meta;
		
		@Override
		@RosettaAttribute("value")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("@data")
		@RuneMetaType
		public Foo.FooBuilder getValue() {
			return value;
		}
		
		@Override
		public Foo.FooBuilder getOrCreateValue() {
			Foo.FooBuilder result;
			if (value!=null) {
				result = value;
			}
			else {
				result = value = Foo.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("meta")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("meta")
		@RuneMetaType
		public MetaFields.MetaFieldsBuilder getMeta() {
			return meta;
		}
		
		@Override
		public MetaFields.MetaFieldsBuilder getOrCreateMeta() {
			MetaFields.MetaFieldsBuilder result;
			if (meta!=null) {
				result = meta;
			}
			else {
				result = meta = MetaFields.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("value")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("@data")
		@RuneMetaType
		@Override
		public FieldWithMetaFoo.FieldWithMetaFooBuilder setValue(Foo _value) {
			this.value = _value == null ? null : _value.toBuilder();
			return this;
		}
		
		@RosettaAttribute("meta")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("meta")
		@RuneMetaType
		@Override
		public FieldWithMetaFoo.FieldWithMetaFooBuilder setMeta(MetaFields _meta) {
			this.meta = _meta == null ? null : _meta.toBuilder();
			return this;
		}
		
		@Override
		public FieldWithMetaFoo build() {
			return new FieldWithMetaFoo.FieldWithMetaFooImpl(this);
		}
		
		@Override
		public FieldWithMetaFoo.FieldWithMetaFooBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public FieldWithMetaFoo.FieldWithMetaFooBuilder prune() {
			if (value!=null && !value.prune().hasData()) value = null;
			if (meta!=null && !meta.prune().hasData()) meta = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getValue()!=null && getValue().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public FieldWithMetaFoo.FieldWithMetaFooBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			FieldWithMetaFoo.FieldWithMetaFooBuilder o = (FieldWithMetaFoo.FieldWithMetaFooBuilder) other;
			
			merger.mergeRosetta(getValue(), o.getValue(), this::setValue);
			merger.mergeRosetta(getMeta(), o.getMeta(), this::setMeta);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			FieldWithMetaFoo _that = getType().cast(o);
		
			if (!Objects.equals(value, _that.getValue())) return false;
			if (!Objects.equals(meta, _that.getMeta())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (value != null ? value.hashCode() : 0);
			_result = 31 * _result + (meta != null ? meta.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "FieldWithMetaFooBuilder {" +
				"value=" + this.value + ", " +
				"meta=" + this.meta +
			'}';
		}
	}
}

class FieldWithMetaFooMeta extends BasicRosettaMetaData<FieldWithMetaFoo> {

}
