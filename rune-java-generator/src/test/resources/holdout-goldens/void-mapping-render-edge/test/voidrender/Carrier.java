package test.voidrender;

import com.google.common.collect.ImmutableList;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Multi;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.util.ListEquals;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import test.voidrender.meta.CarrierMeta;

import static java.util.Optional.ofNullable;

/**
 * Void-typed attributes beside a flag.
 * @version 1.0.0
 */
@RosettaDataType(value="Carrier", builder=Carrier.CarrierBuilderImpl.class, version="1.0.0")
@RuneDataType(value="Carrier", model="test", builder=Carrier.CarrierBuilderImpl.class, version="1.0.0")
public interface Carrier extends RosettaModelObject {

	CarrierMeta metaData = new CarrierMeta();

	/*********************** Getter Methods  ***********************/
	Void getTok();
	List<Void> getToks();
	Boolean getFlag();

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
		processor.processBasic(path.newSubPath("toks"), Void.class, getToks(), this);
		processor.processBasic(path.newSubPath("flag"), Boolean.class, getFlag(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface CarrierBuilder extends Carrier, RosettaModelObjectBuilder {
		Carrier.CarrierBuilder setTok(Void tok);
		Carrier.CarrierBuilder addToks(Void toks);
		Carrier.CarrierBuilder addToks(Void toks, int idx);
		Carrier.CarrierBuilder addToks(List<Void> toks);
		Carrier.CarrierBuilder setToks(List<Void> toks);
		Carrier.CarrierBuilder setFlag(Boolean flag);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("tok"), Void.class, getTok(), this);
			processor.processBasic(path.newSubPath("toks"), Void.class, getToks(), this);
			processor.processBasic(path.newSubPath("flag"), Boolean.class, getFlag(), this);
		}
		

		Carrier.CarrierBuilder prune();
	}

	/*********************** Immutable Implementation of Carrier  ***********************/
	class CarrierImpl implements Carrier {
		private final Void tok;
		private final List<Void> toks;
		private final Boolean flag;
		
		protected CarrierImpl(Carrier.CarrierBuilder builder) {
			this.tok = builder.getTok();
			this.toks = ofNullable(builder.getToks()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.flag = builder.getFlag();
		}
		
		@Override
		@RosettaAttribute("tok")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("tok")
		public Void getTok() {
			return tok;
		}
		
		@Override
		@RosettaAttribute("toks")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("toks")
		public List<Void> getToks() {
			return toks;
		}
		
		@Override
		@RosettaAttribute("flag")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("flag")
		public Boolean getFlag() {
			return flag;
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
			ofNullable(getToks()).ifPresent(builder::setToks);
			ofNullable(getFlag()).ifPresent(builder::setFlag);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Carrier _that = getType().cast(o);
		
			if (!Objects.equals(tok, _that.getTok())) return false;
			if (!ListEquals.listEquals(toks, _that.getToks())) return false;
			if (!Objects.equals(flag, _that.getFlag())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (tok != null ? tok.hashCode() : 0);
			_result = 31 * _result + (toks != null ? toks.hashCode() : 0);
			_result = 31 * _result + (flag != null ? flag.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Carrier {" +
				"tok=" + this.tok + ", " +
				"toks=" + this.toks + ", " +
				"flag=" + this.flag +
			'}';
		}
	}

	/*********************** Builder Implementation of Carrier  ***********************/
	class CarrierBuilderImpl implements Carrier.CarrierBuilder {
	
		protected Void tok;
		protected List<Void> toks = new ArrayList<>();
		protected Boolean flag;
		
		@Override
		@RosettaAttribute("tok")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("tok")
		public Void getTok() {
			return tok;
		}
		
		@Override
		@RosettaAttribute("toks")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("toks")
		public List<Void> getToks() {
			return toks;
		}
		
		@Override
		@RosettaAttribute("flag")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("flag")
		public Boolean getFlag() {
			return flag;
		}
		
		@RosettaAttribute("tok")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("tok")
		@Override
		public Carrier.CarrierBuilder setTok(Void _tok) {
			this.tok = _tok == null ? null : _tok;
			return this;
		}
		
		@RosettaAttribute("toks")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("toks")
		@Override
		public Carrier.CarrierBuilder addToks(Void _toks) {
			if (_toks != null) {
				this.toks.add(_toks);
			}
			return this;
		}
		
		@Override
		public Carrier.CarrierBuilder addToks(Void _toks, int idx) {
			getIndex(this.toks, idx, () -> _toks);
			return this;
		}
		
		@Override
		public Carrier.CarrierBuilder addToks(List<Void> tokss) {
			if (tokss != null) {
				for (final Void toAdd : tokss) {
					this.toks.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("toks")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("toks")
		@Override
		public Carrier.CarrierBuilder setToks(List<Void> tokss) {
			if (tokss == null) {
				this.toks = new ArrayList<>();
			} else {
				this.toks = tokss.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("flag")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("flag")
		@Override
		public Carrier.CarrierBuilder setFlag(Boolean _flag) {
			this.flag = _flag == null ? null : _flag;
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
			if (getToks()!=null && !getToks().isEmpty()) return true;
			if (getFlag()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Carrier.CarrierBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Carrier.CarrierBuilder o = (Carrier.CarrierBuilder) other;
			
			
			merger.mergeBasic(getTok(), o.getTok(), this::setTok);
			merger.mergeBasic(getToks(), o.getToks(), (Consumer<Void>) this::addToks);
			merger.mergeBasic(getFlag(), o.getFlag(), this::setFlag);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Carrier _that = getType().cast(o);
		
			if (!Objects.equals(tok, _that.getTok())) return false;
			if (!ListEquals.listEquals(toks, _that.getToks())) return false;
			if (!Objects.equals(flag, _that.getFlag())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (tok != null ? tok.hashCode() : 0);
			_result = 31 * _result + (toks != null ? toks.hashCode() : 0);
			_result = 31 * _result + (flag != null ? flag.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "CarrierBuilder {" +
				"tok=" + this.tok + ", " +
				"toks=" + this.toks + ", " +
				"flag=" + this.flag +
			'}';
		}
	}
}
