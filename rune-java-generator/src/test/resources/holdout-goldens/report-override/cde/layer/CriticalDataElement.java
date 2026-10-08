package cde.layer;

import cde.layer.meta.CriticalDataElementMeta;
import cde.layer.price.NotationEnum;
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

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="CriticalDataElement", builder=CriticalDataElement.CriticalDataElementBuilderImpl.class, version="0.0.0")
@RuneDataType(value="CriticalDataElement", model="cde", builder=CriticalDataElement.CriticalDataElementBuilderImpl.class, version="0.0.0")
public interface CriticalDataElement extends RosettaModelObject {

	CriticalDataElementMeta metaData = new CriticalDataElementMeta();

	/*********************** Getter Methods  ***********************/
	NotationEnum getNotation();

	/*********************** Build Methods  ***********************/
	CriticalDataElement build();
	
	CriticalDataElement.CriticalDataElementBuilder toBuilder();
	
	static CriticalDataElement.CriticalDataElementBuilder builder() {
		return new CriticalDataElement.CriticalDataElementBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends CriticalDataElement> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends CriticalDataElement> getType() {
		return CriticalDataElement.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("notation"), NotationEnum.class, getNotation(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface CriticalDataElementBuilder extends CriticalDataElement, RosettaModelObjectBuilder {
		CriticalDataElement.CriticalDataElementBuilder setNotation(NotationEnum notation);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("notation"), NotationEnum.class, getNotation(), this);
		}
		

		CriticalDataElement.CriticalDataElementBuilder prune();
	}

	/*********************** Immutable Implementation of CriticalDataElement  ***********************/
	class CriticalDataElementImpl implements CriticalDataElement {
		private final NotationEnum notation;
		
		protected CriticalDataElementImpl(CriticalDataElement.CriticalDataElementBuilder builder) {
			this.notation = builder.getNotation();
		}
		
		@Override
		@RosettaAttribute("notation")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("notation")
		public NotationEnum getNotation() {
			return notation;
		}
		
		@Override
		public CriticalDataElement build() {
			return this;
		}
		
		@Override
		public CriticalDataElement.CriticalDataElementBuilder toBuilder() {
			CriticalDataElement.CriticalDataElementBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(CriticalDataElement.CriticalDataElementBuilder builder) {
			ofNullable(getNotation()).ifPresent(builder::setNotation);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			CriticalDataElement _that = getType().cast(o);
		
			if (!Objects.equals(notation, _that.getNotation())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (notation != null ? notation.getClass().getName().hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "CriticalDataElement {" +
				"notation=" + this.notation +
			'}';
		}
	}

	/*********************** Builder Implementation of CriticalDataElement  ***********************/
	class CriticalDataElementBuilderImpl implements CriticalDataElement.CriticalDataElementBuilder {
	
		protected NotationEnum notation;
		
		@Override
		@RosettaAttribute("notation")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("notation")
		public NotationEnum getNotation() {
			return notation;
		}
		
		@RosettaAttribute("notation")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("notation")
		@Override
		public CriticalDataElement.CriticalDataElementBuilder setNotation(NotationEnum _notation) {
			this.notation = _notation == null ? null : _notation;
			return this;
		}
		
		@Override
		public CriticalDataElement build() {
			return new CriticalDataElement.CriticalDataElementImpl(this);
		}
		
		@Override
		public CriticalDataElement.CriticalDataElementBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public CriticalDataElement.CriticalDataElementBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getNotation()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public CriticalDataElement.CriticalDataElementBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			CriticalDataElement.CriticalDataElementBuilder o = (CriticalDataElement.CriticalDataElementBuilder) other;
			
			
			merger.mergeBasic(getNotation(), o.getNotation(), this::setNotation);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			CriticalDataElement _that = getType().cast(o);
		
			if (!Objects.equals(notation, _that.getNotation())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (notation != null ? notation.getClass().getName().hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "CriticalDataElementBuilder {" +
				"notation=" + this.notation +
			'}';
		}
	}
}
