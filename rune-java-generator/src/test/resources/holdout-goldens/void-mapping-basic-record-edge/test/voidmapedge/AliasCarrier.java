package test.voidmapedge;

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
import test.voidmapedge.meta.AliasCarrierMeta;

import static java.util.Optional.ofNullable;

/**
 * The alias at an attribute.
 * @version 1.0.0
 */
@RosettaDataType(value="AliasCarrier", builder=AliasCarrier.AliasCarrierBuilderImpl.class, version="1.0.0")
@RuneDataType(value="AliasCarrier", model="test", builder=AliasCarrier.AliasCarrierBuilderImpl.class, version="1.0.0")
public interface AliasCarrier extends RosettaModelObject {

	AliasCarrierMeta metaData = new AliasCarrierMeta();

	/*********************** Getter Methods  ***********************/
	Void getTok();

	/*********************** Build Methods  ***********************/
	AliasCarrier build();
	
	AliasCarrier.AliasCarrierBuilder toBuilder();
	
	static AliasCarrier.AliasCarrierBuilder builder() {
		return new AliasCarrier.AliasCarrierBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends AliasCarrier> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends AliasCarrier> getType() {
		return AliasCarrier.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("tok"), Void.class, getTok(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface AliasCarrierBuilder extends AliasCarrier, RosettaModelObjectBuilder {
		AliasCarrier.AliasCarrierBuilder setTok(Void tok);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("tok"), Void.class, getTok(), this);
		}
		

		AliasCarrier.AliasCarrierBuilder prune();
	}

	/*********************** Immutable Implementation of AliasCarrier  ***********************/
	class AliasCarrierImpl implements AliasCarrier {
		private final Void tok;
		
		protected AliasCarrierImpl(AliasCarrier.AliasCarrierBuilder builder) {
			this.tok = builder.getTok();
		}
		
		@Override
		@RosettaAttribute("tok")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("tok")
		public Void getTok() {
			return tok;
		}
		
		@Override
		public AliasCarrier build() {
			return this;
		}
		
		@Override
		public AliasCarrier.AliasCarrierBuilder toBuilder() {
			AliasCarrier.AliasCarrierBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(AliasCarrier.AliasCarrierBuilder builder) {
			ofNullable(getTok()).ifPresent(builder::setTok);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			AliasCarrier _that = getType().cast(o);
		
			if (!Objects.equals(tok, _that.getTok())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (tok != null ? tok.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "AliasCarrier {" +
				"tok=" + this.tok +
			'}';
		}
	}

	/*********************** Builder Implementation of AliasCarrier  ***********************/
	class AliasCarrierBuilderImpl implements AliasCarrier.AliasCarrierBuilder {
	
		protected Void tok;
		
		@Override
		@RosettaAttribute("tok")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("tok")
		public Void getTok() {
			return tok;
		}
		
		@RosettaAttribute("tok")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("tok")
		@Override
		public AliasCarrier.AliasCarrierBuilder setTok(Void _tok) {
			this.tok = _tok == null ? null : _tok;
			return this;
		}
		
		@Override
		public AliasCarrier build() {
			return new AliasCarrier.AliasCarrierImpl(this);
		}
		
		@Override
		public AliasCarrier.AliasCarrierBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public AliasCarrier.AliasCarrierBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getTok()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public AliasCarrier.AliasCarrierBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			AliasCarrier.AliasCarrierBuilder o = (AliasCarrier.AliasCarrierBuilder) other;
			
			
			merger.mergeBasic(getTok(), o.getTok(), this::setTok);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			AliasCarrier _that = getType().cast(o);
		
			if (!Objects.equals(tok, _that.getTok())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (tok != null ? tok.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "AliasCarrierBuilder {" +
				"tok=" + this.tok +
			'}';
		}
	}
}
