package test.fctorref035.metafields;

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
import com.rosetta.model.lib.meta.Reference;
import com.rosetta.model.lib.meta.ReferenceWithMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.util.Objects;
import test.fctorref035.TypeWithKey;

import static java.util.Optional.ofNullable;

@RosettaDataType(value="ReferenceWithMetaTypeWithKey", builder=ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyBuilderImpl.class, version="0.0.0")
@RuneDataType(value="ReferenceWithMetaTypeWithKey", model="test", builder=ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyBuilderImpl.class, version="0.0.0")
public interface ReferenceWithMetaTypeWithKey extends RosettaModelObject, ReferenceWithMeta<TypeWithKey> {

	ReferenceWithMetaTypeWithKeyMeta metaData = new ReferenceWithMetaTypeWithKeyMeta();

	/*********************** Getter Methods  ***********************/
	TypeWithKey getValue();
	String getGlobalReference();
	String getExternalReference();
	Reference getReference();

	/*********************** Build Methods  ***********************/
	ReferenceWithMetaTypeWithKey build();
	
	ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyBuilder toBuilder();
	
	static ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyBuilder builder() {
		return new ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends ReferenceWithMetaTypeWithKey> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends ReferenceWithMetaTypeWithKey> getType() {
		return ReferenceWithMetaTypeWithKey.class;
	}
	
	@Override
	default Class<TypeWithKey> getValueType() {
		return TypeWithKey.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("value"), processor, TypeWithKey.class, getValue());
		processor.processBasic(path.newSubPath("globalReference"), String.class, getGlobalReference(), this, AttributeMeta.META);
		processor.processBasic(path.newSubPath("externalReference"), String.class, getExternalReference(), this, AttributeMeta.META);
		processRosetta(path.newSubPath("reference"), processor, Reference.class, getReference());
	}
	

	/*********************** Builder Interface  ***********************/
	interface ReferenceWithMetaTypeWithKeyBuilder extends ReferenceWithMetaTypeWithKey, RosettaModelObjectBuilder, ReferenceWithMeta.ReferenceWithMetaBuilder<TypeWithKey> {
		TypeWithKey.TypeWithKeyBuilder getOrCreateValue();
		@Override
		TypeWithKey.TypeWithKeyBuilder getValue();
		Reference.ReferenceBuilder getOrCreateReference();
		@Override
		Reference.ReferenceBuilder getReference();
		ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyBuilder setValue(TypeWithKey value);
		ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyBuilder setGlobalReference(String globalReference);
		ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyBuilder setExternalReference(String externalReference);
		ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyBuilder setReference(Reference reference);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("value"), processor, TypeWithKey.TypeWithKeyBuilder.class, getValue());
			processor.processBasic(path.newSubPath("globalReference"), String.class, getGlobalReference(), this, AttributeMeta.META);
			processor.processBasic(path.newSubPath("externalReference"), String.class, getExternalReference(), this, AttributeMeta.META);
			processRosetta(path.newSubPath("reference"), processor, Reference.ReferenceBuilder.class, getReference());
		}
		

		ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyBuilder prune();
	}

	/*********************** Immutable Implementation of ReferenceWithMetaTypeWithKey  ***********************/
	class ReferenceWithMetaTypeWithKeyImpl implements ReferenceWithMetaTypeWithKey {
		private final TypeWithKey value;
		private final String globalReference;
		private final String externalReference;
		private final Reference reference;
		
		protected ReferenceWithMetaTypeWithKeyImpl(ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyBuilder builder) {
			this.value = ofNullable(builder.getValue()).map(f->f.build()).orElse(null);
			this.globalReference = builder.getGlobalReference();
			this.externalReference = builder.getExternalReference();
			this.reference = ofNullable(builder.getReference()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("value")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("@data")
		@RuneMetaType
		public TypeWithKey getValue() {
			return value;
		}
		
		@Override
		@RosettaAttribute("globalReference")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("@ref")
		public String getGlobalReference() {
			return globalReference;
		}
		
		@Override
		@RosettaAttribute("externalReference")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("@ref:external")
		public String getExternalReference() {
			return externalReference;
		}
		
		@Override
		@RosettaAttribute("address")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("@ref:scoped")
		@RuneMetaType
		public Reference getReference() {
			return reference;
		}
		
		@Override
		public ReferenceWithMetaTypeWithKey build() {
			return this;
		}
		
		@Override
		public ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyBuilder toBuilder() {
			ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyBuilder builder) {
			ofNullable(getValue()).ifPresent(builder::setValue);
			ofNullable(getGlobalReference()).ifPresent(builder::setGlobalReference);
			ofNullable(getExternalReference()).ifPresent(builder::setExternalReference);
			ofNullable(getReference()).ifPresent(builder::setReference);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			ReferenceWithMetaTypeWithKey _that = getType().cast(o);
		
			if (!Objects.equals(value, _that.getValue())) return false;
			if (!Objects.equals(globalReference, _that.getGlobalReference())) return false;
			if (!Objects.equals(externalReference, _that.getExternalReference())) return false;
			if (!Objects.equals(reference, _that.getReference())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (value != null ? value.hashCode() : 0);
			_result = 31 * _result + (globalReference != null ? globalReference.hashCode() : 0);
			_result = 31 * _result + (externalReference != null ? externalReference.hashCode() : 0);
			_result = 31 * _result + (reference != null ? reference.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "ReferenceWithMetaTypeWithKey {" +
				"value=" + this.value + ", " +
				"globalReference=" + this.globalReference + ", " +
				"externalReference=" + this.externalReference + ", " +
				"reference=" + this.reference +
			'}';
		}
	}

	/*********************** Builder Implementation of ReferenceWithMetaTypeWithKey  ***********************/
	class ReferenceWithMetaTypeWithKeyBuilderImpl implements ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyBuilder {
	
		protected TypeWithKey.TypeWithKeyBuilder value;
		protected String globalReference;
		protected String externalReference;
		protected Reference.ReferenceBuilder reference;
		
		@Override
		@RosettaAttribute("value")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("@data")
		@RuneMetaType
		public TypeWithKey.TypeWithKeyBuilder getValue() {
			return value;
		}
		
		@Override
		public TypeWithKey.TypeWithKeyBuilder getOrCreateValue() {
			TypeWithKey.TypeWithKeyBuilder result;
			if (value!=null) {
				result = value;
			}
			else {
				result = value = TypeWithKey.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("globalReference")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("@ref")
		public String getGlobalReference() {
			return globalReference;
		}
		
		@Override
		@RosettaAttribute("externalReference")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("@ref:external")
		public String getExternalReference() {
			return externalReference;
		}
		
		@Override
		@RosettaAttribute("address")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("@ref:scoped")
		@RuneMetaType
		public Reference.ReferenceBuilder getReference() {
			return reference;
		}
		
		@Override
		public Reference.ReferenceBuilder getOrCreateReference() {
			Reference.ReferenceBuilder result;
			if (reference!=null) {
				result = reference;
			}
			else {
				result = reference = Reference.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("value")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("@data")
		@RuneMetaType
		@Override
		public ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyBuilder setValue(TypeWithKey _value) {
			this.value = _value == null ? null : _value.toBuilder();
			return this;
		}
		
		@RosettaAttribute("globalReference")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("@ref")
		@Override
		public ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyBuilder setGlobalReference(String _globalReference) {
			this.globalReference = _globalReference == null ? null : _globalReference;
			return this;
		}
		
		@RosettaAttribute("externalReference")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("@ref:external")
		@Override
		public ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyBuilder setExternalReference(String _externalReference) {
			this.externalReference = _externalReference == null ? null : _externalReference;
			return this;
		}
		
		@RosettaAttribute("address")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("@ref:scoped")
		@RuneMetaType
		@Override
		public ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyBuilder setReference(Reference _reference) {
			this.reference = _reference == null ? null : _reference.toBuilder();
			return this;
		}
		
		@Override
		public ReferenceWithMetaTypeWithKey build() {
			return new ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyImpl(this);
		}
		
		@Override
		public ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyBuilder prune() {
			if (value!=null && !value.prune().hasData()) value = null;
			if (reference!=null && !reference.prune().hasData()) reference = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getValue()!=null && getValue().hasData()) return true;
			if (getGlobalReference()!=null) return true;
			if (getExternalReference()!=null) return true;
			if (getReference()!=null && getReference().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyBuilder o = (ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyBuilder) other;
			
			merger.mergeRosetta(getValue(), o.getValue(), this::setValue);
			merger.mergeRosetta(getReference(), o.getReference(), this::setReference);
			
			merger.mergeBasic(getGlobalReference(), o.getGlobalReference(), this::setGlobalReference);
			merger.mergeBasic(getExternalReference(), o.getExternalReference(), this::setExternalReference);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			ReferenceWithMetaTypeWithKey _that = getType().cast(o);
		
			if (!Objects.equals(value, _that.getValue())) return false;
			if (!Objects.equals(globalReference, _that.getGlobalReference())) return false;
			if (!Objects.equals(externalReference, _that.getExternalReference())) return false;
			if (!Objects.equals(reference, _that.getReference())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (value != null ? value.hashCode() : 0);
			_result = 31 * _result + (globalReference != null ? globalReference.hashCode() : 0);
			_result = 31 * _result + (externalReference != null ? externalReference.hashCode() : 0);
			_result = 31 * _result + (reference != null ? reference.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "ReferenceWithMetaTypeWithKeyBuilder {" +
				"value=" + this.value + ", " +
				"globalReference=" + this.globalReference + ", " +
				"externalReference=" + this.externalReference + ", " +
				"reference=" + this.reference +
			'}';
		}
	}
}

class ReferenceWithMetaTypeWithKeyMeta extends BasicRosettaMetaData<ReferenceWithMetaTypeWithKey> {

}
