package holdout.voidcollapse;

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
import holdout.voidcollapse.meta.CarrierMeta;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * A model type carrying a Void-typed attribute.
 * @version 0.0.0
 */
@RosettaDataType(value="Carrier", builder=Carrier.CarrierBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Carrier", model="holdout", builder=Carrier.CarrierBuilderImpl.class, version="0.0.0")
public interface Carrier extends RosettaModelObject {

	CarrierMeta metaData = new CarrierMeta();

	/*********************** Getter Methods  ***********************/
	Void getTok();

	/*********************** Build Methods  ***********************/
	Carrier build();
	
	Carrier.CarrierBuilder toBuilder();
	
	static Carrier.CarrierBuilder builder() {
		return new Carrier.CarrierBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Carrier> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Carrier> getType() {
		return Carrier.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("tok"), Void.class, getTok(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface CarrierBuilder extends Carrier, RosettaModelObjectBuilder {
		Carrier.CarrierBuilder setTok(Void tok);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("tok"), Void.class, getTok(), this);
		}
		

		Carrier.CarrierBuilder prune();
	}

	/*********************** Immutable Implementation of Carrier  ***********************/
	class CarrierImpl implements Carrier {
		private final Void tok;
		
		protected CarrierImpl(Carrier.CarrierBuilder builder) {
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
		public Carrier build() {
			return this;
		}
		
		@Override
		public Carrier.CarrierBuilder toBuilder() {
			Carrier.CarrierBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Carrier.CarrierBuilder builder) {
			ofNullable(getTok()).ifPresent(builder::setTok);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Carrier _that = getType().cast(o);
		
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
			return "Carrier {" +
				"tok=" + this.tok +
			'}';
		}
	}

	/*********************** Builder Implementation of Carrier  ***********************/
	class CarrierBuilderImpl implements Carrier.CarrierBuilder {
	
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
		public Carrier.CarrierBuilder setTok(Void _tok) {
			this.tok = _tok == null ? null : _tok;
			return this;
		}
		
		@Override
		public Carrier build() {
			return new Carrier.CarrierImpl(this);
		}
		
		@Override
		public Carrier.CarrierBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Carrier.CarrierBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getTok()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Carrier.CarrierBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Carrier.CarrierBuilder o = (Carrier.CarrierBuilder) other;
			
			
			merger.mergeBasic(getTok(), o.getTok(), this::setTok);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Carrier _that = getType().cast(o);
		
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
			return "CarrierBuilder {" +
				"tok=" + this.tok +
			'}';
		}
	}
}
