package test.voidmap;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Required;
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
import test.voidmap.meta.RequiredCarrierMeta;

import static java.util.Optional.ofNullable;

/**
 * Required cardinality over the model-declared types.
 * @version 1.0.0
 */
@RosettaDataType(value="RequiredCarrier", builder=RequiredCarrier.RequiredCarrierBuilderImpl.class, version="1.0.0")
@RuneDataType(value="RequiredCarrier", model="test", builder=RequiredCarrier.RequiredCarrierBuilderImpl.class, version="1.0.0")
public interface RequiredCarrier extends RosettaModelObject {

	RequiredCarrierMeta metaData = new RequiredCarrierMeta();

	/*********************** Getter Methods  ***********************/
	Void getTok();
	Void getSpan();

	/*********************** Build Methods  ***********************/
	RequiredCarrier build();
	
	RequiredCarrier.RequiredCarrierBuilder toBuilder();
	
	static RequiredCarrier.RequiredCarrierBuilder builder() {
		return new RequiredCarrier.RequiredCarrierBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends RequiredCarrier> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends RequiredCarrier> getType() {
		return RequiredCarrier.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("tok"), Void.class, getTok(), this);
		processor.processBasic(path.newSubPath("span"), Void.class, getSpan(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface RequiredCarrierBuilder extends RequiredCarrier, RosettaModelObjectBuilder {
		RequiredCarrier.RequiredCarrierBuilder setTok(Void tok);
		RequiredCarrier.RequiredCarrierBuilder setSpan(Void span);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("tok"), Void.class, getTok(), this);
			processor.processBasic(path.newSubPath("span"), Void.class, getSpan(), this);
		}
		

		RequiredCarrier.RequiredCarrierBuilder prune();
	}

	/*********************** Immutable Implementation of RequiredCarrier  ***********************/
	class RequiredCarrierImpl implements RequiredCarrier {
		private final Void tok;
		private final Void span;
		
		protected RequiredCarrierImpl(RequiredCarrier.RequiredCarrierBuilder builder) {
			this.tok = builder.getTok();
			this.span = builder.getSpan();
		}
		
		@Override
		@RosettaAttribute("tok")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("tok")
		public Void getTok() {
			return tok;
		}
		
		@Override
		@RosettaAttribute("span")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("span")
		public Void getSpan() {
			return span;
		}
		
		@Override
		public RequiredCarrier build() {
			return this;
		}
		
		@Override
		public RequiredCarrier.RequiredCarrierBuilder toBuilder() {
			RequiredCarrier.RequiredCarrierBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(RequiredCarrier.RequiredCarrierBuilder builder) {
			ofNullable(getTok()).ifPresent(builder::setTok);
			ofNullable(getSpan()).ifPresent(builder::setSpan);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RequiredCarrier _that = getType().cast(o);
		
			if (!Objects.equals(tok, _that.getTok())) return false;
			if (!Objects.equals(span, _that.getSpan())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (tok != null ? tok.hashCode() : 0);
			_result = 31 * _result + (span != null ? span.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "RequiredCarrier {" +
				"tok=" + this.tok + ", " +
				"span=" + this.span +
			'}';
		}
	}

	/*********************** Builder Implementation of RequiredCarrier  ***********************/
	class RequiredCarrierBuilderImpl implements RequiredCarrier.RequiredCarrierBuilder {
	
		protected Void tok;
		protected Void span;
		
		@Override
		@RosettaAttribute("tok")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("tok")
		public Void getTok() {
			return tok;
		}
		
		@Override
		@RosettaAttribute("span")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("span")
		public Void getSpan() {
			return span;
		}
		
		@RosettaAttribute("tok")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("tok")
		@Override
		public RequiredCarrier.RequiredCarrierBuilder setTok(Void _tok) {
			this.tok = _tok == null ? null : _tok;
			return this;
		}
		
		@RosettaAttribute("span")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("span")
		@Override
		public RequiredCarrier.RequiredCarrierBuilder setSpan(Void _span) {
			this.span = _span == null ? null : _span;
			return this;
		}
		
		@Override
		public RequiredCarrier build() {
			return new RequiredCarrier.RequiredCarrierImpl(this);
		}
		
		@Override
		public RequiredCarrier.RequiredCarrierBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RequiredCarrier.RequiredCarrierBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getTok()!=null) return true;
			if (getSpan()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RequiredCarrier.RequiredCarrierBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			RequiredCarrier.RequiredCarrierBuilder o = (RequiredCarrier.RequiredCarrierBuilder) other;
			
			
			merger.mergeBasic(getTok(), o.getTok(), this::setTok);
			merger.mergeBasic(getSpan(), o.getSpan(), this::setSpan);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RequiredCarrier _that = getType().cast(o);
		
			if (!Objects.equals(tok, _that.getTok())) return false;
			if (!Objects.equals(span, _that.getSpan())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (tok != null ? tok.hashCode() : 0);
			_result = 31 * _result + (span != null ? span.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "RequiredCarrierBuilder {" +
				"tok=" + this.tok + ", " +
				"span=" + this.span +
			'}';
		}
	}
}
