package test.voidcollide;

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
import test.voidcollide.meta.CarrierMeta;

import static java.util.Optional.ofNullable;

/**
 * An attribute typed by the colliding declaration.
 * @version 1.0.0
 */
@RosettaDataType(value="Carrier", builder=Carrier.CarrierBuilderImpl.class, version="1.0.0")
@RuneDataType(value="Carrier", model="test", builder=Carrier.CarrierBuilderImpl.class, version="1.0.0")
public interface Carrier extends RosettaModelObject {

	CarrierMeta metaData = new CarrierMeta();

	/*********************** Getter Methods  ***********************/
	String getS();

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
		processor.processBasic(path.newSubPath("s"), String.class, getS(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface CarrierBuilder extends Carrier, RosettaModelObjectBuilder {
		Carrier.CarrierBuilder setS(String s);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("s"), String.class, getS(), this);
		}
		

		Carrier.CarrierBuilder prune();
	}

	/*********************** Immutable Implementation of Carrier  ***********************/
	class CarrierImpl implements Carrier {
		private final String s;
		
		protected CarrierImpl(Carrier.CarrierBuilder builder) {
			this.s = builder.getS();
		}
		
		@Override
		@RosettaAttribute("s")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("s")
		public String getS() {
			return s;
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
			ofNullable(getS()).ifPresent(builder::setS);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Carrier _that = getType().cast(o);
		
			if (!Objects.equals(s, _that.getS())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (s != null ? s.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Carrier {" +
				"s=" + this.s +
			'}';
		}
	}

	/*********************** Builder Implementation of Carrier  ***********************/
	class CarrierBuilderImpl implements Carrier.CarrierBuilder {
	
		protected String s;
		
		@Override
		@RosettaAttribute("s")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("s")
		public String getS() {
			return s;
		}
		
		@RosettaAttribute("s")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("s")
		@Override
		public Carrier.CarrierBuilder setS(String _s) {
			this.s = _s == null ? null : _s;
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
			if (getS()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Carrier.CarrierBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Carrier.CarrierBuilder o = (Carrier.CarrierBuilder) other;
			
			
			merger.mergeBasic(getS(), o.getS(), this::setS);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Carrier _that = getType().cast(o);
		
			if (!Objects.equals(s, _that.getS())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (s != null ? s.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "CarrierBuilder {" +
				"s=" + this.s +
			'}';
		}
	}
}
