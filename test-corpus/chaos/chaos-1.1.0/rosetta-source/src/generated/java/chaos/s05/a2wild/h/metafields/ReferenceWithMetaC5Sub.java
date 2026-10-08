package chaos.s05.a2wild.h.metafields;

import chaos.s05.a2wild.h.C5Sub;
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

@RosettaDataType(value="ReferenceWithMetaC5Sub", builder=ReferenceWithMetaC5Sub.ReferenceWithMetaC5SubBuilderImpl.class, version="0.0.0")
@RuneDataType(value="ReferenceWithMetaC5Sub", model="chaos", builder=ReferenceWithMetaC5Sub.ReferenceWithMetaC5SubBuilderImpl.class, version="0.0.0")
public interface ReferenceWithMetaC5Sub extends RosettaModelObject, ReferenceWithMeta<C5Sub> {

	ReferenceWithMetaC5SubMeta metaData = new ReferenceWithMetaC5SubMeta();

	/*********************** Getter Methods  ***********************/
	C5Sub getValue();
	String getGlobalReference();
	String getExternalReference();
	Reference getReference();

	/*********************** Build Methods  ***********************/
	ReferenceWithMetaC5Sub build();
	
	ReferenceWithMetaC5Sub.ReferenceWithMetaC5SubBuilder toBuilder();
	
	static ReferenceWithMetaC5Sub.ReferenceWithMetaC5SubBuilder builder() {
		return new ReferenceWithMetaC5Sub.ReferenceWithMetaC5SubBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends ReferenceWithMetaC5Sub> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends ReferenceWithMetaC5Sub> getType() {
		return ReferenceWithMetaC5Sub.class;
	}
	
	@Override
	default Class<C5Sub> getValueType() {
		return C5Sub.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("value"), processor, C5Sub.class, getValue());
		processor.processBasic(path.newSubPath("globalReference"), String.class, getGlobalReference(), this, AttributeMeta.META);
		processor.processBasic(path.newSubPath("externalReference"), String.class, getExternalReference(), this, AttributeMeta.META);
		processRosetta(path.newSubPath("reference"), processor, Reference.class, getReference());
	}
	

	/*********************** Builder Interface  ***********************/
	interface ReferenceWithMetaC5SubBuilder extends ReferenceWithMetaC5Sub, RosettaModelObjectBuilder, ReferenceWithMeta.ReferenceWithMetaBuilder<C5Sub> {
		C5Sub.C5SubBuilder getOrCreateValue();
		@Override
		C5Sub.C5SubBuilder getValue();
		Reference.ReferenceBuilder getOrCreateReference();
		@Override
		Reference.ReferenceBuilder getReference();
		ReferenceWithMetaC5Sub.ReferenceWithMetaC5SubBuilder setValue(C5Sub value);
		ReferenceWithMetaC5Sub.ReferenceWithMetaC5SubBuilder setGlobalReference(String globalReference);
		ReferenceWithMetaC5Sub.ReferenceWithMetaC5SubBuilder setExternalReference(String externalReference);
		ReferenceWithMetaC5Sub.ReferenceWithMetaC5SubBuilder setReference(Reference reference);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("value"), processor, C5Sub.C5SubBuilder.class, getValue());
			processor.processBasic(path.newSubPath("globalReference"), String.class, getGlobalReference(), this, AttributeMeta.META);
			processor.processBasic(path.newSubPath("externalReference"), String.class, getExternalReference(), this, AttributeMeta.META);
			processRosetta(path.newSubPath("reference"), processor, Reference.ReferenceBuilder.class, getReference());
		}
		

		ReferenceWithMetaC5Sub.ReferenceWithMetaC5SubBuilder prune();
	}

	/*********************** Immutable Implementation of ReferenceWithMetaC5Sub  ***********************/
	class ReferenceWithMetaC5SubImpl implements ReferenceWithMetaC5Sub {
		private final C5Sub value;
		private final String globalReference;
		private final String externalReference;
		private final Reference reference;
		
		protected ReferenceWithMetaC5SubImpl(ReferenceWithMetaC5Sub.ReferenceWithMetaC5SubBuilder builder) {
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
		public C5Sub getValue() {
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
		public ReferenceWithMetaC5Sub build() {
			return this;
		}
		
		@Override
		public ReferenceWithMetaC5Sub.ReferenceWithMetaC5SubBuilder toBuilder() {
			ReferenceWithMetaC5Sub.ReferenceWithMetaC5SubBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(ReferenceWithMetaC5Sub.ReferenceWithMetaC5SubBuilder builder) {
			ofNullable(getValue()).ifPresent(builder::setValue);
			ofNullable(getGlobalReference()).ifPresent(builder::setGlobalReference);
			ofNullable(getExternalReference()).ifPresent(builder::setExternalReference);
			ofNullable(getReference()).ifPresent(builder::setReference);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			ReferenceWithMetaC5Sub _that = getType().cast(o);
		
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
			return "ReferenceWithMetaC5Sub {" +
				"value=" + this.value + ", " +
				"globalReference=" + this.globalReference + ", " +
				"externalReference=" + this.externalReference + ", " +
				"reference=" + this.reference +
			'}';
		}
	}

	/*********************** Builder Implementation of ReferenceWithMetaC5Sub  ***********************/
	class ReferenceWithMetaC5SubBuilderImpl implements ReferenceWithMetaC5Sub.ReferenceWithMetaC5SubBuilder {
	
		protected C5Sub.C5SubBuilder value;
		protected String globalReference;
		protected String externalReference;
		protected Reference.ReferenceBuilder reference;
		
		@Override
		@RosettaAttribute("value")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("@data")
		@RuneMetaType
		public C5Sub.C5SubBuilder getValue() {
			return value;
		}
		
		@Override
		public C5Sub.C5SubBuilder getOrCreateValue() {
			C5Sub.C5SubBuilder result;
			if (value!=null) {
				result = value;
			}
			else {
				result = value = C5Sub.builder();
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
		public ReferenceWithMetaC5Sub.ReferenceWithMetaC5SubBuilder setValue(C5Sub _value) {
			this.value = _value == null ? null : _value.toBuilder();
			return this;
		}
		
		@RosettaAttribute("globalReference")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("@ref")
		@Override
		public ReferenceWithMetaC5Sub.ReferenceWithMetaC5SubBuilder setGlobalReference(String _globalReference) {
			this.globalReference = _globalReference == null ? null : _globalReference;
			return this;
		}
		
		@RosettaAttribute("externalReference")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("@ref:external")
		@Override
		public ReferenceWithMetaC5Sub.ReferenceWithMetaC5SubBuilder setExternalReference(String _externalReference) {
			this.externalReference = _externalReference == null ? null : _externalReference;
			return this;
		}
		
		@RosettaAttribute("address")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("@ref:scoped")
		@RuneMetaType
		@Override
		public ReferenceWithMetaC5Sub.ReferenceWithMetaC5SubBuilder setReference(Reference _reference) {
			this.reference = _reference == null ? null : _reference.toBuilder();
			return this;
		}
		
		@Override
		public ReferenceWithMetaC5Sub build() {
			return new ReferenceWithMetaC5Sub.ReferenceWithMetaC5SubImpl(this);
		}
		
		@Override
		public ReferenceWithMetaC5Sub.ReferenceWithMetaC5SubBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public ReferenceWithMetaC5Sub.ReferenceWithMetaC5SubBuilder prune() {
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
		public ReferenceWithMetaC5Sub.ReferenceWithMetaC5SubBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			ReferenceWithMetaC5Sub.ReferenceWithMetaC5SubBuilder o = (ReferenceWithMetaC5Sub.ReferenceWithMetaC5SubBuilder) other;
			
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
		
			ReferenceWithMetaC5Sub _that = getType().cast(o);
		
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
			return "ReferenceWithMetaC5SubBuilder {" +
				"value=" + this.value + ", " +
				"globalReference=" + this.globalReference + ", " +
				"externalReference=" + this.externalReference + ", " +
				"reference=" + this.reference +
			'}';
		}
	}
}

class ReferenceWithMetaC5SubMeta extends BasicRosettaMetaData<ReferenceWithMetaC5Sub> {

}
