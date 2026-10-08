package com.rosetta.model.metafields;

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
import java.math.BigDecimal;
import java.util.Objects;

import static java.util.Optional.ofNullable;

@RosettaDataType(value="FieldWithMetaBigDecimal", builder=FieldWithMetaBigDecimal.FieldWithMetaBigDecimalBuilderImpl.class, version="0.0.0")
@RuneDataType(value="FieldWithMetaBigDecimal", model="com", builder=FieldWithMetaBigDecimal.FieldWithMetaBigDecimalBuilderImpl.class, version="0.0.0")
public interface FieldWithMetaBigDecimal extends RosettaModelObject, FieldWithMeta<BigDecimal>, GlobalKey {

	FieldWithMetaBigDecimalMeta metaData = new FieldWithMetaBigDecimalMeta();

	/*********************** Getter Methods  ***********************/
	BigDecimal getValue();
	MetaFields getMeta();

	/*********************** Build Methods  ***********************/
	FieldWithMetaBigDecimal build();
	
	FieldWithMetaBigDecimal.FieldWithMetaBigDecimalBuilder toBuilder();
	
	static FieldWithMetaBigDecimal.FieldWithMetaBigDecimalBuilder builder() {
		return new FieldWithMetaBigDecimal.FieldWithMetaBigDecimalBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends FieldWithMetaBigDecimal> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends FieldWithMetaBigDecimal> getType() {
		return FieldWithMetaBigDecimal.class;
	}
	
	@Override
	default Class<BigDecimal> getValueType() {
		return BigDecimal.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("value"), BigDecimal.class, getValue(), this);
		processRosetta(path.newSubPath("meta"), processor, MetaFields.class, getMeta());
	}
	

	/*********************** Builder Interface  ***********************/
	interface FieldWithMetaBigDecimalBuilder extends FieldWithMetaBigDecimal, RosettaModelObjectBuilder, FieldWithMeta.FieldWithMetaBuilder<BigDecimal>, GlobalKey.GlobalKeyBuilder {
		MetaFields.MetaFieldsBuilder getOrCreateMeta();
		@Override
		MetaFields.MetaFieldsBuilder getMeta();
		FieldWithMetaBigDecimal.FieldWithMetaBigDecimalBuilder setValue(BigDecimal value);
		FieldWithMetaBigDecimal.FieldWithMetaBigDecimalBuilder setMeta(MetaFields meta);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("value"), BigDecimal.class, getValue(), this);
			processRosetta(path.newSubPath("meta"), processor, MetaFields.MetaFieldsBuilder.class, getMeta());
		}
		

		FieldWithMetaBigDecimal.FieldWithMetaBigDecimalBuilder prune();
	}

	/*********************** Immutable Implementation of FieldWithMetaBigDecimal  ***********************/
	class FieldWithMetaBigDecimalImpl implements FieldWithMetaBigDecimal {
		private final BigDecimal value;
		private final MetaFields meta;
		
		protected FieldWithMetaBigDecimalImpl(FieldWithMetaBigDecimal.FieldWithMetaBigDecimalBuilder builder) {
			this.value = builder.getValue();
			this.meta = ofNullable(builder.getMeta()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("value")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("@data")
		public BigDecimal getValue() {
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
		public FieldWithMetaBigDecimal build() {
			return this;
		}
		
		@Override
		public FieldWithMetaBigDecimal.FieldWithMetaBigDecimalBuilder toBuilder() {
			FieldWithMetaBigDecimal.FieldWithMetaBigDecimalBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(FieldWithMetaBigDecimal.FieldWithMetaBigDecimalBuilder builder) {
			ofNullable(getValue()).ifPresent(builder::setValue);
			ofNullable(getMeta()).ifPresent(builder::setMeta);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			FieldWithMetaBigDecimal _that = getType().cast(o);
		
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
			return "FieldWithMetaBigDecimal {" +
				"value=" + this.value + ", " +
				"meta=" + this.meta +
			'}';
		}
	}

	/*********************** Builder Implementation of FieldWithMetaBigDecimal  ***********************/
	class FieldWithMetaBigDecimalBuilderImpl implements FieldWithMetaBigDecimal.FieldWithMetaBigDecimalBuilder {
	
		protected BigDecimal value;
		protected MetaFields.MetaFieldsBuilder meta;
		
		@Override
		@RosettaAttribute("value")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("@data")
		public BigDecimal getValue() {
			return value;
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
		@Override
		public FieldWithMetaBigDecimal.FieldWithMetaBigDecimalBuilder setValue(BigDecimal _value) {
			this.value = _value == null ? null : _value;
			return this;
		}
		
		@RosettaAttribute("meta")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("meta")
		@RuneMetaType
		@Override
		public FieldWithMetaBigDecimal.FieldWithMetaBigDecimalBuilder setMeta(MetaFields _meta) {
			this.meta = _meta == null ? null : _meta.toBuilder();
			return this;
		}
		
		@Override
		public FieldWithMetaBigDecimal build() {
			return new FieldWithMetaBigDecimal.FieldWithMetaBigDecimalImpl(this);
		}
		
		@Override
		public FieldWithMetaBigDecimal.FieldWithMetaBigDecimalBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public FieldWithMetaBigDecimal.FieldWithMetaBigDecimalBuilder prune() {
			if (meta!=null && !meta.prune().hasData()) meta = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getValue()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public FieldWithMetaBigDecimal.FieldWithMetaBigDecimalBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			FieldWithMetaBigDecimal.FieldWithMetaBigDecimalBuilder o = (FieldWithMetaBigDecimal.FieldWithMetaBigDecimalBuilder) other;
			
			merger.mergeRosetta(getMeta(), o.getMeta(), this::setMeta);
			
			merger.mergeBasic(getValue(), o.getValue(), this::setValue);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			FieldWithMetaBigDecimal _that = getType().cast(o);
		
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
			return "FieldWithMetaBigDecimalBuilder {" +
				"value=" + this.value + ", " +
				"meta=" + this.meta +
			'}';
		}
	}
}

class FieldWithMetaBigDecimalMeta extends BasicRosettaMetaData<FieldWithMetaBigDecimal> {

}
