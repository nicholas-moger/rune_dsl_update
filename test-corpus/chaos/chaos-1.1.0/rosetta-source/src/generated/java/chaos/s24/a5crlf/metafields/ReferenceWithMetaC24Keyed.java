package chaos.s24.a5crlf.metafields;

import chaos.s24.a5crlf.C24Keyed;
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

import static java.util.Optional.ofNullable;

@RosettaDataType(value="ReferenceWithMetaC24Keyed", builder=ReferenceWithMetaC24Keyed.ReferenceWithMetaC24KeyedBuilderImpl.class, version="0.0.0")
@RuneDataType(value="ReferenceWithMetaC24Keyed", model="chaos", builder=ReferenceWithMetaC24Keyed.ReferenceWithMetaC24KeyedBuilderImpl.class, version="0.0.0")
public interface ReferenceWithMetaC24Keyed extends RosettaModelObject, ReferenceWithMeta<C24Keyed> {

	ReferenceWithMetaC24KeyedMeta metaData = new ReferenceWithMetaC24KeyedMeta();

	/*********************** Getter Methods  ***********************/
	C24Keyed getValue();
	String getGlobalReference();
	String getExternalReference();
	Reference getReference();

	/*********************** Build Methods  ***********************/
	ReferenceWithMetaC24Keyed build();
	
	ReferenceWithMetaC24Keyed.ReferenceWithMetaC24KeyedBuilder toBuilder();
	
	static ReferenceWithMetaC24Keyed.ReferenceWithMetaC24KeyedBuilder builder() {
		return new ReferenceWithMetaC24Keyed.ReferenceWithMetaC24KeyedBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends ReferenceWithMetaC24Keyed> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends ReferenceWithMetaC24Keyed> getType() {
		return ReferenceWithMetaC24Keyed.class;
	}
	
	@Override
	default Class<C24Keyed> getValueType() {
		return C24Keyed.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("value"), processor, C24Keyed.class, getValue());
		processor.processBasic(path.newSubPath("globalReference"), String.class, getGlobalReference(), this, AttributeMeta.META);
		processor.processBasic(path.newSubPath("externalReference"), String.class, getExternalReference(), this, AttributeMeta.META);
		processRosetta(path.newSubPath("reference"), processor, Reference.class, getReference());
	}
	

	/*********************** Builder Interface  ***********************/
	interface ReferenceWithMetaC24KeyedBuilder extends ReferenceWithMetaC24Keyed, RosettaModelObjectBuilder, ReferenceWithMeta.ReferenceWithMetaBuilder<C24Keyed> {
		C24Keyed.C24KeyedBuilder getOrCreateValue();
		@Override
		C24Keyed.C24KeyedBuilder getValue();
		Reference.ReferenceBuilder getOrCreateReference();
		@Override
		Reference.ReferenceBuilder getReference();
		ReferenceWithMetaC24Keyed.ReferenceWithMetaC24KeyedBuilder setValue(C24Keyed value);
		ReferenceWithMetaC24Keyed.ReferenceWithMetaC24KeyedBuilder setGlobalReference(String globalReference);
		ReferenceWithMetaC24Keyed.ReferenceWithMetaC24KeyedBuilder setExternalReference(String externalReference);
		ReferenceWithMetaC24Keyed.ReferenceWithMetaC24KeyedBuilder setReference(Reference reference);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("value"), processor, C24Keyed.C24KeyedBuilder.class, getValue());
			processor.processBasic(path.newSubPath("globalReference"), String.class, getGlobalReference(), this, AttributeMeta.META);
			processor.processBasic(path.newSubPath("externalReference"), String.class, getExternalReference(), this, AttributeMeta.META);
			processRosetta(path.newSubPath("reference"), processor, Reference.ReferenceBuilder.class, getReference());
		}
		

		ReferenceWithMetaC24Keyed.ReferenceWithMetaC24KeyedBuilder prune();
	}

	/*********************** Immutable Implementation of ReferenceWithMetaC24Keyed  ***********************/
	class ReferenceWithMetaC24KeyedImpl implements ReferenceWithMetaC24Keyed {
		private final C24Keyed value;
		private final String globalReference;
		private final String externalReference;
		private final Reference reference;
		
		protected ReferenceWithMetaC24KeyedImpl(ReferenceWithMetaC24Keyed.ReferenceWithMetaC24KeyedBuilder builder) {
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
		public C24Keyed getValue() {
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
		public ReferenceWithMetaC24Keyed build() {
			return this;
		}
		
		@Override
		public ReferenceWithMetaC24Keyed.ReferenceWithMetaC24KeyedBuilder toBuilder() {
			ReferenceWithMetaC24Keyed.ReferenceWithMetaC24KeyedBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(ReferenceWithMetaC24Keyed.ReferenceWithMetaC24KeyedBuilder builder) {
			ofNullable(getValue()).ifPresent(builder::setValue);
			ofNullable(getGlobalReference()).ifPresent(builder::setGlobalReference);
			ofNullable(getExternalReference()).ifPresent(builder::setExternalReference);
			ofNullable(getReference()).ifPresent(builder::setReference);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			ReferenceWithMetaC24Keyed _that = getType().cast(o);
		
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
			return "ReferenceWithMetaC24Keyed {" +
				"value=" + this.value + ", " +
				"globalReference=" + this.globalReference + ", " +
				"externalReference=" + this.externalReference + ", " +
				"reference=" + this.reference +
			'}';
		}
	}

	/*********************** Builder Implementation of ReferenceWithMetaC24Keyed  ***********************/
	class ReferenceWithMetaC24KeyedBuilderImpl implements ReferenceWithMetaC24Keyed.ReferenceWithMetaC24KeyedBuilder {
	
		protected C24Keyed.C24KeyedBuilder value;
		protected String globalReference;
		protected String externalReference;
		protected Reference.ReferenceBuilder reference;
		
		@Override
		@RosettaAttribute("value")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("@data")
		@RuneMetaType
		public C24Keyed.C24KeyedBuilder getValue() {
			return value;
		}
		
		@Override
		public C24Keyed.C24KeyedBuilder getOrCreateValue() {
			C24Keyed.C24KeyedBuilder result;
			if (value!=null) {
				result = value;
			}
			else {
				result = value = C24Keyed.builder();
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
		public ReferenceWithMetaC24Keyed.ReferenceWithMetaC24KeyedBuilder setValue(C24Keyed _value) {
			this.value = _value == null ? null : _value.toBuilder();
			return this;
		}
		
		@RosettaAttribute("globalReference")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("@ref")
		@Override
		public ReferenceWithMetaC24Keyed.ReferenceWithMetaC24KeyedBuilder setGlobalReference(String _globalReference) {
			this.globalReference = _globalReference == null ? null : _globalReference;
			return this;
		}
		
		@RosettaAttribute("externalReference")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("@ref:external")
		@Override
		public ReferenceWithMetaC24Keyed.ReferenceWithMetaC24KeyedBuilder setExternalReference(String _externalReference) {
			this.externalReference = _externalReference == null ? null : _externalReference;
			return this;
		}
		
		@RosettaAttribute("address")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("@ref:scoped")
		@RuneMetaType
		@Override
		public ReferenceWithMetaC24Keyed.ReferenceWithMetaC24KeyedBuilder setReference(Reference _reference) {
			this.reference = _reference == null ? null : _reference.toBuilder();
			return this;
		}
		
		@Override
		public ReferenceWithMetaC24Keyed build() {
			return new ReferenceWithMetaC24Keyed.ReferenceWithMetaC24KeyedImpl(this);
		}
		
		@Override
		public ReferenceWithMetaC24Keyed.ReferenceWithMetaC24KeyedBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public ReferenceWithMetaC24Keyed.ReferenceWithMetaC24KeyedBuilder prune() {
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
		public ReferenceWithMetaC24Keyed.ReferenceWithMetaC24KeyedBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			ReferenceWithMetaC24Keyed.ReferenceWithMetaC24KeyedBuilder o = (ReferenceWithMetaC24Keyed.ReferenceWithMetaC24KeyedBuilder) other;
			
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
		
			ReferenceWithMetaC24Keyed _that = getType().cast(o);
		
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
			return "ReferenceWithMetaC24KeyedBuilder {" +
				"value=" + this.value + ", " +
				"globalReference=" + this.globalReference + ", " +
				"externalReference=" + this.externalReference + ", " +
				"reference=" + this.reference +
			'}';
		}
	}
}

class ReferenceWithMetaC24KeyedMeta extends BasicRosettaMetaData<ReferenceWithMetaC24Keyed> {

}
