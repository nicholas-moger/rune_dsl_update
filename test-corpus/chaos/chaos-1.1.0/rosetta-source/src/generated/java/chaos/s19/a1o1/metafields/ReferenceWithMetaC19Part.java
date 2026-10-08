package chaos.s19.a1o1.metafields;

import chaos.s19.a1o1.C19Part;
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

@RosettaDataType(value="ReferenceWithMetaC19Part", builder=ReferenceWithMetaC19Part.ReferenceWithMetaC19PartBuilderImpl.class, version="0.0.0")
@RuneDataType(value="ReferenceWithMetaC19Part", model="chaos", builder=ReferenceWithMetaC19Part.ReferenceWithMetaC19PartBuilderImpl.class, version="0.0.0")
public interface ReferenceWithMetaC19Part extends RosettaModelObject, ReferenceWithMeta<C19Part> {

	ReferenceWithMetaC19PartMeta metaData = new ReferenceWithMetaC19PartMeta();

	/*********************** Getter Methods  ***********************/
	C19Part getValue();
	String getGlobalReference();
	String getExternalReference();
	Reference getReference();

	/*********************** Build Methods  ***********************/
	ReferenceWithMetaC19Part build();
	
	ReferenceWithMetaC19Part.ReferenceWithMetaC19PartBuilder toBuilder();
	
	static ReferenceWithMetaC19Part.ReferenceWithMetaC19PartBuilder builder() {
		return new ReferenceWithMetaC19Part.ReferenceWithMetaC19PartBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends ReferenceWithMetaC19Part> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends ReferenceWithMetaC19Part> getType() {
		return ReferenceWithMetaC19Part.class;
	}
	
	@Override
	default Class<C19Part> getValueType() {
		return C19Part.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("value"), processor, C19Part.class, getValue());
		processor.processBasic(path.newSubPath("globalReference"), String.class, getGlobalReference(), this, AttributeMeta.META);
		processor.processBasic(path.newSubPath("externalReference"), String.class, getExternalReference(), this, AttributeMeta.META);
		processRosetta(path.newSubPath("reference"), processor, Reference.class, getReference());
	}
	

	/*********************** Builder Interface  ***********************/
	interface ReferenceWithMetaC19PartBuilder extends ReferenceWithMetaC19Part, RosettaModelObjectBuilder, ReferenceWithMeta.ReferenceWithMetaBuilder<C19Part> {
		C19Part.C19PartBuilder getOrCreateValue();
		@Override
		C19Part.C19PartBuilder getValue();
		Reference.ReferenceBuilder getOrCreateReference();
		@Override
		Reference.ReferenceBuilder getReference();
		ReferenceWithMetaC19Part.ReferenceWithMetaC19PartBuilder setValue(C19Part value);
		ReferenceWithMetaC19Part.ReferenceWithMetaC19PartBuilder setGlobalReference(String globalReference);
		ReferenceWithMetaC19Part.ReferenceWithMetaC19PartBuilder setExternalReference(String externalReference);
		ReferenceWithMetaC19Part.ReferenceWithMetaC19PartBuilder setReference(Reference reference);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("value"), processor, C19Part.C19PartBuilder.class, getValue());
			processor.processBasic(path.newSubPath("globalReference"), String.class, getGlobalReference(), this, AttributeMeta.META);
			processor.processBasic(path.newSubPath("externalReference"), String.class, getExternalReference(), this, AttributeMeta.META);
			processRosetta(path.newSubPath("reference"), processor, Reference.ReferenceBuilder.class, getReference());
		}
		

		ReferenceWithMetaC19Part.ReferenceWithMetaC19PartBuilder prune();
	}

	/*********************** Immutable Implementation of ReferenceWithMetaC19Part  ***********************/
	class ReferenceWithMetaC19PartImpl implements ReferenceWithMetaC19Part {
		private final C19Part value;
		private final String globalReference;
		private final String externalReference;
		private final Reference reference;
		
		protected ReferenceWithMetaC19PartImpl(ReferenceWithMetaC19Part.ReferenceWithMetaC19PartBuilder builder) {
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
		public C19Part getValue() {
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
		public ReferenceWithMetaC19Part build() {
			return this;
		}
		
		@Override
		public ReferenceWithMetaC19Part.ReferenceWithMetaC19PartBuilder toBuilder() {
			ReferenceWithMetaC19Part.ReferenceWithMetaC19PartBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(ReferenceWithMetaC19Part.ReferenceWithMetaC19PartBuilder builder) {
			ofNullable(getValue()).ifPresent(builder::setValue);
			ofNullable(getGlobalReference()).ifPresent(builder::setGlobalReference);
			ofNullable(getExternalReference()).ifPresent(builder::setExternalReference);
			ofNullable(getReference()).ifPresent(builder::setReference);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			ReferenceWithMetaC19Part _that = getType().cast(o);
		
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
			return "ReferenceWithMetaC19Part {" +
				"value=" + this.value + ", " +
				"globalReference=" + this.globalReference + ", " +
				"externalReference=" + this.externalReference + ", " +
				"reference=" + this.reference +
			'}';
		}
	}

	/*********************** Builder Implementation of ReferenceWithMetaC19Part  ***********************/
	class ReferenceWithMetaC19PartBuilderImpl implements ReferenceWithMetaC19Part.ReferenceWithMetaC19PartBuilder {
	
		protected C19Part.C19PartBuilder value;
		protected String globalReference;
		protected String externalReference;
		protected Reference.ReferenceBuilder reference;
		
		@Override
		@RosettaAttribute("value")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("@data")
		@RuneMetaType
		public C19Part.C19PartBuilder getValue() {
			return value;
		}
		
		@Override
		public C19Part.C19PartBuilder getOrCreateValue() {
			C19Part.C19PartBuilder result;
			if (value!=null) {
				result = value;
			}
			else {
				result = value = C19Part.builder();
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
		public ReferenceWithMetaC19Part.ReferenceWithMetaC19PartBuilder setValue(C19Part _value) {
			this.value = _value == null ? null : _value.toBuilder();
			return this;
		}
		
		@RosettaAttribute("globalReference")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("@ref")
		@Override
		public ReferenceWithMetaC19Part.ReferenceWithMetaC19PartBuilder setGlobalReference(String _globalReference) {
			this.globalReference = _globalReference == null ? null : _globalReference;
			return this;
		}
		
		@RosettaAttribute("externalReference")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("@ref:external")
		@Override
		public ReferenceWithMetaC19Part.ReferenceWithMetaC19PartBuilder setExternalReference(String _externalReference) {
			this.externalReference = _externalReference == null ? null : _externalReference;
			return this;
		}
		
		@RosettaAttribute("address")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("@ref:scoped")
		@RuneMetaType
		@Override
		public ReferenceWithMetaC19Part.ReferenceWithMetaC19PartBuilder setReference(Reference _reference) {
			this.reference = _reference == null ? null : _reference.toBuilder();
			return this;
		}
		
		@Override
		public ReferenceWithMetaC19Part build() {
			return new ReferenceWithMetaC19Part.ReferenceWithMetaC19PartImpl(this);
		}
		
		@Override
		public ReferenceWithMetaC19Part.ReferenceWithMetaC19PartBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public ReferenceWithMetaC19Part.ReferenceWithMetaC19PartBuilder prune() {
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
		public ReferenceWithMetaC19Part.ReferenceWithMetaC19PartBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			ReferenceWithMetaC19Part.ReferenceWithMetaC19PartBuilder o = (ReferenceWithMetaC19Part.ReferenceWithMetaC19PartBuilder) other;
			
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
		
			ReferenceWithMetaC19Part _that = getType().cast(o);
		
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
			return "ReferenceWithMetaC19PartBuilder {" +
				"value=" + this.value + ", " +
				"globalReference=" + this.globalReference + ", " +
				"externalReference=" + this.externalReference + ", " +
				"reference=" + this.reference +
			'}';
		}
	}
}

class ReferenceWithMetaC19PartMeta extends BasicRosettaMetaData<ReferenceWithMetaC19Part> {

}
