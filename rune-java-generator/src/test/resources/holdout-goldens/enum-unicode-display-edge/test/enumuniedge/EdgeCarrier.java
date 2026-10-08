package test.enumuniedge;

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
import test.enumuniedge.meta.EdgeCarrierMeta;

import static java.util.Optional.ofNullable;

/**
 * An attribute of each enum so every enum is reachable from a type. (The first cut named the first attribute `e` - the released grammar REFUSED it at the lexer, &#39;no viable alternative at input e&#39;, the seat-8 banked refusal met again: target/v32-seat9-instruments/scratch/oracle-s9a-enum-unicode-display-edge-run1.log.)
 * @version 1.0.0
 */
@RosettaDataType(value="EdgeCarrier", builder=EdgeCarrier.EdgeCarrierBuilderImpl.class, version="1.0.0")
@RuneDataType(value="EdgeCarrier", model="test", builder=EdgeCarrier.EdgeCarrierBuilderImpl.class, version="1.0.0")
public interface EdgeCarrier extends RosettaModelObject {

	EdgeCarrierMeta metaData = new EdgeCarrierMeta();

	/*********************** Getter Methods  ***********************/
	EscapeEnum getEsc();
	SynonymEnum getSyn();

	/*********************** Build Methods  ***********************/
	EdgeCarrier build();
	
	EdgeCarrier.EdgeCarrierBuilder toBuilder();
	
	static EdgeCarrier.EdgeCarrierBuilder builder() {
		return new EdgeCarrier.EdgeCarrierBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends EdgeCarrier> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends EdgeCarrier> getType() {
		return EdgeCarrier.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("esc"), EscapeEnum.class, getEsc(), this);
		processor.processBasic(path.newSubPath("syn"), SynonymEnum.class, getSyn(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface EdgeCarrierBuilder extends EdgeCarrier, RosettaModelObjectBuilder {
		EdgeCarrier.EdgeCarrierBuilder setEsc(EscapeEnum esc);
		EdgeCarrier.EdgeCarrierBuilder setSyn(SynonymEnum syn);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("esc"), EscapeEnum.class, getEsc(), this);
			processor.processBasic(path.newSubPath("syn"), SynonymEnum.class, getSyn(), this);
		}
		

		EdgeCarrier.EdgeCarrierBuilder prune();
	}

	/*********************** Immutable Implementation of EdgeCarrier  ***********************/
	class EdgeCarrierImpl implements EdgeCarrier {
		private final EscapeEnum esc;
		private final SynonymEnum syn;
		
		protected EdgeCarrierImpl(EdgeCarrier.EdgeCarrierBuilder builder) {
			this.esc = builder.getEsc();
			this.syn = builder.getSyn();
		}
		
		@Override
		@RosettaAttribute("esc")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("esc")
		public EscapeEnum getEsc() {
			return esc;
		}
		
		@Override
		@RosettaAttribute("syn")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("syn")
		public SynonymEnum getSyn() {
			return syn;
		}
		
		@Override
		public EdgeCarrier build() {
			return this;
		}
		
		@Override
		public EdgeCarrier.EdgeCarrierBuilder toBuilder() {
			EdgeCarrier.EdgeCarrierBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(EdgeCarrier.EdgeCarrierBuilder builder) {
			ofNullable(getEsc()).ifPresent(builder::setEsc);
			ofNullable(getSyn()).ifPresent(builder::setSyn);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			EdgeCarrier _that = getType().cast(o);
		
			if (!Objects.equals(esc, _that.getEsc())) return false;
			if (!Objects.equals(syn, _that.getSyn())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (esc != null ? esc.getClass().getName().hashCode() : 0);
			_result = 31 * _result + (syn != null ? syn.getClass().getName().hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "EdgeCarrier {" +
				"esc=" + this.esc + ", " +
				"syn=" + this.syn +
			'}';
		}
	}

	/*********************** Builder Implementation of EdgeCarrier  ***********************/
	class EdgeCarrierBuilderImpl implements EdgeCarrier.EdgeCarrierBuilder {
	
		protected EscapeEnum esc;
		protected SynonymEnum syn;
		
		@Override
		@RosettaAttribute("esc")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("esc")
		public EscapeEnum getEsc() {
			return esc;
		}
		
		@Override
		@RosettaAttribute("syn")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("syn")
		public SynonymEnum getSyn() {
			return syn;
		}
		
		@RosettaAttribute("esc")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("esc")
		@Override
		public EdgeCarrier.EdgeCarrierBuilder setEsc(EscapeEnum _esc) {
			this.esc = _esc == null ? null : _esc;
			return this;
		}
		
		@RosettaAttribute("syn")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("syn")
		@Override
		public EdgeCarrier.EdgeCarrierBuilder setSyn(SynonymEnum _syn) {
			this.syn = _syn == null ? null : _syn;
			return this;
		}
		
		@Override
		public EdgeCarrier build() {
			return new EdgeCarrier.EdgeCarrierImpl(this);
		}
		
		@Override
		public EdgeCarrier.EdgeCarrierBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public EdgeCarrier.EdgeCarrierBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getEsc()!=null) return true;
			if (getSyn()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public EdgeCarrier.EdgeCarrierBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			EdgeCarrier.EdgeCarrierBuilder o = (EdgeCarrier.EdgeCarrierBuilder) other;
			
			
			merger.mergeBasic(getEsc(), o.getEsc(), this::setEsc);
			merger.mergeBasic(getSyn(), o.getSyn(), this::setSyn);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			EdgeCarrier _that = getType().cast(o);
		
			if (!Objects.equals(esc, _that.getEsc())) return false;
			if (!Objects.equals(syn, _that.getSyn())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (esc != null ? esc.getClass().getName().hashCode() : 0);
			_result = 31 * _result + (syn != null ? syn.getClass().getName().hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "EdgeCarrierBuilder {" +
				"esc=" + this.esc + ", " +
				"syn=" + this.syn +
			'}';
		}
	}
}
