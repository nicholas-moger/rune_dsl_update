package test.fctorref035;

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
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.model.metafields.MetaFields;
import java.util.Objects;
import test.fctorref035.meta.TypeWithKeyMeta;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="TypeWithKey", builder=TypeWithKey.TypeWithKeyBuilderImpl.class, version="0.0.0")
@RuneDataType(value="TypeWithKey", model="test", builder=TypeWithKey.TypeWithKeyBuilderImpl.class, version="0.0.0")
public interface TypeWithKey extends RosettaModelObject, GlobalKey {

	TypeWithKeyMeta metaData = new TypeWithKeyMeta();

	/*********************** Getter Methods  ***********************/
	MetaFields getMeta();

	/*********************** Build Methods  ***********************/
	TypeWithKey build();
	
	TypeWithKey.TypeWithKeyBuilder toBuilder();
	
	static TypeWithKey.TypeWithKeyBuilder builder() {
		return new TypeWithKey.TypeWithKeyBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends TypeWithKey> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends TypeWithKey> getType() {
		return TypeWithKey.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("meta"), processor, MetaFields.class, getMeta());
	}
	

	/*********************** Builder Interface  ***********************/
	interface TypeWithKeyBuilder extends TypeWithKey, RosettaModelObjectBuilder, GlobalKey.GlobalKeyBuilder {
		MetaFields.MetaFieldsBuilder getOrCreateMeta();
		@Override
		MetaFields.MetaFieldsBuilder getMeta();
		TypeWithKey.TypeWithKeyBuilder setMeta(MetaFields meta);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("meta"), processor, MetaFields.MetaFieldsBuilder.class, getMeta());
		}
		

		TypeWithKey.TypeWithKeyBuilder prune();
	}

	/*********************** Immutable Implementation of TypeWithKey  ***********************/
	class TypeWithKeyImpl implements TypeWithKey {
		private final MetaFields meta;
		
		protected TypeWithKeyImpl(TypeWithKey.TypeWithKeyBuilder builder) {
			this.meta = ofNullable(builder.getMeta()).map(f->f.build()).orElse(null);
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
		public TypeWithKey build() {
			return this;
		}
		
		@Override
		public TypeWithKey.TypeWithKeyBuilder toBuilder() {
			TypeWithKey.TypeWithKeyBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(TypeWithKey.TypeWithKeyBuilder builder) {
			ofNullable(getMeta()).ifPresent(builder::setMeta);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			TypeWithKey _that = getType().cast(o);
		
			if (!Objects.equals(meta, _that.getMeta())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (meta != null ? meta.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "TypeWithKey {" +
				"meta=" + this.meta +
			'}';
		}
	}

	/*********************** Builder Implementation of TypeWithKey  ***********************/
	class TypeWithKeyBuilderImpl implements TypeWithKey.TypeWithKeyBuilder {
	
		protected MetaFields.MetaFieldsBuilder meta;
		
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
		
		@RosettaAttribute("meta")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("meta")
		@RuneMetaType
		@Override
		public TypeWithKey.TypeWithKeyBuilder setMeta(MetaFields _meta) {
			this.meta = _meta == null ? null : _meta.toBuilder();
			return this;
		}
		
		@Override
		public TypeWithKey build() {
			return new TypeWithKey.TypeWithKeyImpl(this);
		}
		
		@Override
		public TypeWithKey.TypeWithKeyBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public TypeWithKey.TypeWithKeyBuilder prune() {
			if (meta!=null && !meta.prune().hasData()) meta = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public TypeWithKey.TypeWithKeyBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			TypeWithKey.TypeWithKeyBuilder o = (TypeWithKey.TypeWithKeyBuilder) other;
			
			merger.mergeRosetta(getMeta(), o.getMeta(), this::setMeta);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			TypeWithKey _that = getType().cast(o);
		
			if (!Objects.equals(meta, _that.getMeta())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (meta != null ? meta.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "TypeWithKeyBuilder {" +
				"meta=" + this.meta +
			'}';
		}
	}
}
