package test.voidmap;

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
import test.voidmap.meta.CarrierMeta;

import static java.util.Optional.ofNullable;

/**
 * The seed&#39;s shape: the model-declared builtins beside a real one, optional.
 * @version 1.0.0
 */
@RosettaDataType(value="Carrier", builder=Carrier.CarrierBuilderImpl.class, version="1.0.0")
@RuneDataType(value="Carrier", model="test", builder=Carrier.CarrierBuilderImpl.class, version="1.0.0")
public interface Carrier extends RosettaModelObject {

	CarrierMeta metaData = new CarrierMeta();

	/*********************** Getter Methods  ***********************/
	Void getTok();
	Void getSpan();
	String getName();

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
		processor.processBasic(path.newSubPath("span"), Void.class, getSpan(), this);
		processor.processBasic(path.newSubPath("name"), String.class, getName(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface CarrierBuilder extends Carrier, RosettaModelObjectBuilder {
		Carrier.CarrierBuilder setTok(Void tok);
		Carrier.CarrierBuilder setSpan(Void span);
		Carrier.CarrierBuilder setName(String name);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("tok"), Void.class, getTok(), this);
			processor.processBasic(path.newSubPath("span"), Void.class, getSpan(), this);
			processor.processBasic(path.newSubPath("name"), String.class, getName(), this);
		}
		

		Carrier.CarrierBuilder prune();
	}

	/*********************** Immutable Implementation of Carrier  ***********************/
	class CarrierImpl implements Carrier {
		private final Void tok;
		private final Void span;
		private final String name;
		
		protected CarrierImpl(Carrier.CarrierBuilder builder) {
			this.tok = builder.getTok();
			this.span = builder.getSpan();
			this.name = builder.getName();
		}
		
		@Override
		@RosettaAttribute("tok")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("tok")
		public Void getTok() {
			return tok;
		}
		
		@Override
		@RosettaAttribute("span")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("span")
		public Void getSpan() {
			return span;
		}
		
		@Override
		@RosettaAttribute("name")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("name")
		public String getName() {
			return name;
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
			ofNullable(getSpan()).ifPresent(builder::setSpan);
			ofNullable(getName()).ifPresent(builder::setName);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Carrier _that = getType().cast(o);
		
			if (!Objects.equals(tok, _that.getTok())) return false;
			if (!Objects.equals(span, _that.getSpan())) return false;
			if (!Objects.equals(name, _that.getName())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (tok != null ? tok.hashCode() : 0);
			_result = 31 * _result + (span != null ? span.hashCode() : 0);
			_result = 31 * _result + (name != null ? name.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Carrier {" +
				"tok=" + this.tok + ", " +
				"span=" + this.span + ", " +
				"name=" + this.name +
			'}';
		}
	}

	/*********************** Builder Implementation of Carrier  ***********************/
	class CarrierBuilderImpl implements Carrier.CarrierBuilder {
	
		protected Void tok;
		protected Void span;
		protected String name;
		
		@Override
		@RosettaAttribute("tok")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("tok")
		public Void getTok() {
			return tok;
		}
		
		@Override
		@RosettaAttribute("span")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("span")
		public Void getSpan() {
			return span;
		}
		
		@Override
		@RosettaAttribute("name")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("name")
		public String getName() {
			return name;
		}
		
		@RosettaAttribute("tok")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("tok")
		@Override
		public Carrier.CarrierBuilder setTok(Void _tok) {
			this.tok = _tok == null ? null : _tok;
			return this;
		}
		
		@RosettaAttribute("span")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("span")
		@Override
		public Carrier.CarrierBuilder setSpan(Void _span) {
			this.span = _span == null ? null : _span;
			return this;
		}
		
		@RosettaAttribute("name")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("name")
		@Override
		public Carrier.CarrierBuilder setName(String _name) {
			this.name = _name == null ? null : _name;
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
			if (getSpan()!=null) return true;
			if (getName()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Carrier.CarrierBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Carrier.CarrierBuilder o = (Carrier.CarrierBuilder) other;
			
			
			merger.mergeBasic(getTok(), o.getTok(), this::setTok);
			merger.mergeBasic(getSpan(), o.getSpan(), this::setSpan);
			merger.mergeBasic(getName(), o.getName(), this::setName);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Carrier _that = getType().cast(o);
		
			if (!Objects.equals(tok, _that.getTok())) return false;
			if (!Objects.equals(span, _that.getSpan())) return false;
			if (!Objects.equals(name, _that.getName())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (tok != null ? tok.hashCode() : 0);
			_result = 31 * _result + (span != null ? span.hashCode() : 0);
			_result = 31 * _result + (name != null ? name.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "CarrierBuilder {" +
				"tok=" + this.tok + ", " +
				"span=" + this.span + ", " +
				"name=" + this.name +
			'}';
		}
	}
}
