package chaos.s09.base.metafields;

import chaos.s09.base.C9Keyed;
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

@RosettaDataType(value="ReferenceWithMetaC9Keyed", builder=ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedBuilderImpl.class, version="0.0.0")
@RuneDataType(value="ReferenceWithMetaC9Keyed", model="chaos", builder=ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedBuilderImpl.class, version="0.0.0")
public interface ReferenceWithMetaC9Keyed extends RosettaModelObject, ReferenceWithMeta<C9Keyed> {

	ReferenceWithMetaC9KeyedMeta metaData = new ReferenceWithMetaC9KeyedMeta();

	/*********************** Getter Methods  ***********************/
	C9Keyed getValue();
	String getGlobalReference();
	String getExternalReference();
	Reference getReference();

	/*********************** Build Methods  ***********************/
	ReferenceWithMetaC9Keyed build();
	
	ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedBuilder toBuilder();
	
	static ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedBuilder builder() {
		return new ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends ReferenceWithMetaC9Keyed> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends ReferenceWithMetaC9Keyed> getType() {
		return ReferenceWithMetaC9Keyed.class;
	}
	
	@Override
	default Class<C9Keyed> getValueType() {
		return C9Keyed.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("value"), processor, C9Keyed.class, getValue());
		processor.processBasic(path.newSubPath("globalReference"), String.class, getGlobalReference(), this, AttributeMeta.META);
		processor.processBasic(path.newSubPath("externalReference"), String.class, getExternalReference(), this, AttributeMeta.META);
		processRosetta(path.newSubPath("reference"), processor, Reference.class, getReference());
	}
	

	/*********************** Builder Interface  ***********************/
	interface ReferenceWithMetaC9KeyedBuilder extends ReferenceWithMetaC9Keyed, RosettaModelObjectBuilder, ReferenceWithMeta.ReferenceWithMetaBuilder<C9Keyed> {
		C9Keyed.C9KeyedBuilder getOrCreateValue();
		@Override
		C9Keyed.C9KeyedBuilder getValue();
		Reference.ReferenceBuilder getOrCreateReference();
		@Override
		Reference.ReferenceBuilder getReference();
		ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedBuilder setValue(C9Keyed value);
		ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedBuilder setGlobalReference(String globalReference);
		ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedBuilder setExternalReference(String externalReference);
		ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedBuilder setReference(Reference reference);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("value"), processor, C9Keyed.C9KeyedBuilder.class, getValue());
			processor.processBasic(path.newSubPath("globalReference"), String.class, getGlobalReference(), this, AttributeMeta.META);
			processor.processBasic(path.newSubPath("externalReference"), String.class, getExternalReference(), this, AttributeMeta.META);
			processRosetta(path.newSubPath("reference"), processor, Reference.ReferenceBuilder.class, getReference());
		}
		

		ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedBuilder prune();
	}

	/*********************** Immutable Implementation of ReferenceWithMetaC9Keyed  ***********************/
	class ReferenceWithMetaC9KeyedImpl implements ReferenceWithMetaC9Keyed {
		private final C9Keyed value;
		private final String globalReference;
		private final String externalReference;
		private final Reference reference;
		
		protected ReferenceWithMetaC9KeyedImpl(ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedBuilder builder) {
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
		public C9Keyed getValue() {
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
		public ReferenceWithMetaC9Keyed build() {
			return this;
		}
		
		@Override
		public ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedBuilder toBuilder() {
			ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedBuilder builder) {
			ofNullable(getValue()).ifPresent(builder::setValue);
			ofNullable(getGlobalReference()).ifPresent(builder::setGlobalReference);
			ofNullable(getExternalReference()).ifPresent(builder::setExternalReference);
			ofNullable(getReference()).ifPresent(builder::setReference);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			ReferenceWithMetaC9Keyed _that = getType().cast(o);
		
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
			return "ReferenceWithMetaC9Keyed {" +
				"value=" + this.value + ", " +
				"globalReference=" + this.globalReference + ", " +
				"externalReference=" + this.externalReference + ", " +
				"reference=" + this.reference +
			'}';
		}
	}

	/*********************** Builder Implementation of ReferenceWithMetaC9Keyed  ***********************/
	class ReferenceWithMetaC9KeyedBuilderImpl implements ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedBuilder {
	
		protected C9Keyed.C9KeyedBuilder value;
		protected String globalReference;
		protected String externalReference;
		protected Reference.ReferenceBuilder reference;
		
		@Override
		@RosettaAttribute("value")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("@data")
		@RuneMetaType
		public C9Keyed.C9KeyedBuilder getValue() {
			return value;
		}
		
		@Override
		public C9Keyed.C9KeyedBuilder getOrCreateValue() {
			C9Keyed.C9KeyedBuilder result;
			if (value!=null) {
				result = value;
			}
			else {
				result = value = C9Keyed.builder();
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
		public ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedBuilder setValue(C9Keyed _value) {
			this.value = _value == null ? null : _value.toBuilder();
			return this;
		}
		
		@RosettaAttribute("globalReference")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("@ref")
		@Override
		public ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedBuilder setGlobalReference(String _globalReference) {
			this.globalReference = _globalReference == null ? null : _globalReference;
			return this;
		}
		
		@RosettaAttribute("externalReference")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("@ref:external")
		@Override
		public ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedBuilder setExternalReference(String _externalReference) {
			this.externalReference = _externalReference == null ? null : _externalReference;
			return this;
		}
		
		@RosettaAttribute("address")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("@ref:scoped")
		@RuneMetaType
		@Override
		public ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedBuilder setReference(Reference _reference) {
			this.reference = _reference == null ? null : _reference.toBuilder();
			return this;
		}
		
		@Override
		public ReferenceWithMetaC9Keyed build() {
			return new ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedImpl(this);
		}
		
		@Override
		public ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedBuilder prune() {
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
		public ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedBuilder o = (ReferenceWithMetaC9Keyed.ReferenceWithMetaC9KeyedBuilder) other;
			
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
		
			ReferenceWithMetaC9Keyed _that = getType().cast(o);
		
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
			return "ReferenceWithMetaC9KeyedBuilder {" +
				"value=" + this.value + ", " +
				"globalReference=" + this.globalReference + ", " +
				"externalReference=" + this.externalReference + ", " +
				"reference=" + this.reference +
			'}';
		}
	}
}

class ReferenceWithMetaC9KeyedMeta extends BasicRosettaMetaData<ReferenceWithMetaC9Keyed> {

}
