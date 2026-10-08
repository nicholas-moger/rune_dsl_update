package chaos.s15.base;

import chaos.s15.base.meta.C15CarrierMeta;
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
 * Uses the model-declared builtins beside real ones.
 * @version 1.0.0
 */
@RosettaDataType(value="C15Carrier", builder=C15Carrier.C15CarrierBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C15Carrier", model="chaos", builder=C15Carrier.C15CarrierBuilderImpl.class, version="1.0.0")
public interface C15Carrier extends RosettaModelObject {

	C15CarrierMeta metaData = new C15CarrierMeta();

	/*********************** Getter Methods  ***********************/
	Void getTok();
	Void getSpan();
	C15Aux getAux();

	/*********************** Build Methods  ***********************/
	C15Carrier build();
	
	C15Carrier.C15CarrierBuilder toBuilder();
	
	static C15Carrier.C15CarrierBuilder builder() {
		return new C15Carrier.C15CarrierBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C15Carrier> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C15Carrier> getType() {
		return C15Carrier.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("tok"), Void.class, getTok(), this);
		processor.processBasic(path.newSubPath("span"), Void.class, getSpan(), this);
		processRosetta(path.newSubPath("aux"), processor, C15Aux.class, getAux());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C15CarrierBuilder extends C15Carrier, RosettaModelObjectBuilder {
		C15Aux.C15AuxBuilder getOrCreateAux();
		@Override
		C15Aux.C15AuxBuilder getAux();
		C15Carrier.C15CarrierBuilder setTok(Void tok);
		C15Carrier.C15CarrierBuilder setSpan(Void span);
		C15Carrier.C15CarrierBuilder setAux(C15Aux aux);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("tok"), Void.class, getTok(), this);
			processor.processBasic(path.newSubPath("span"), Void.class, getSpan(), this);
			processRosetta(path.newSubPath("aux"), processor, C15Aux.C15AuxBuilder.class, getAux());
		}
		

		C15Carrier.C15CarrierBuilder prune();
	}

	/*********************** Immutable Implementation of C15Carrier  ***********************/
	class C15CarrierImpl implements C15Carrier {
		private final Void tok;
		private final Void span;
		private final C15Aux aux;
		
		protected C15CarrierImpl(C15Carrier.C15CarrierBuilder builder) {
			this.tok = builder.getTok();
			this.span = builder.getSpan();
			this.aux = ofNullable(builder.getAux()).map(f->f.build()).orElse(null);
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
		@RosettaAttribute("aux")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("aux")
		public C15Aux getAux() {
			return aux;
		}
		
		@Override
		public C15Carrier build() {
			return this;
		}
		
		@Override
		public C15Carrier.C15CarrierBuilder toBuilder() {
			C15Carrier.C15CarrierBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C15Carrier.C15CarrierBuilder builder) {
			ofNullable(getTok()).ifPresent(builder::setTok);
			ofNullable(getSpan()).ifPresent(builder::setSpan);
			ofNullable(getAux()).ifPresent(builder::setAux);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C15Carrier _that = getType().cast(o);
		
			if (!Objects.equals(tok, _that.getTok())) return false;
			if (!Objects.equals(span, _that.getSpan())) return false;
			if (!Objects.equals(aux, _that.getAux())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (tok != null ? tok.hashCode() : 0);
			_result = 31 * _result + (span != null ? span.hashCode() : 0);
			_result = 31 * _result + (aux != null ? aux.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C15Carrier {" +
				"tok=" + this.tok + ", " +
				"span=" + this.span + ", " +
				"aux=" + this.aux +
			'}';
		}
	}

	/*********************** Builder Implementation of C15Carrier  ***********************/
	class C15CarrierBuilderImpl implements C15Carrier.C15CarrierBuilder {
	
		protected Void tok;
		protected Void span;
		protected C15Aux.C15AuxBuilder aux;
		
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
		@RosettaAttribute("aux")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("aux")
		public C15Aux.C15AuxBuilder getAux() {
			return aux;
		}
		
		@Override
		public C15Aux.C15AuxBuilder getOrCreateAux() {
			C15Aux.C15AuxBuilder result;
			if (aux!=null) {
				result = aux;
			}
			else {
				result = aux = C15Aux.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("tok")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("tok")
		@Override
		public C15Carrier.C15CarrierBuilder setTok(Void _tok) {
			this.tok = _tok == null ? null : _tok;
			return this;
		}
		
		@RosettaAttribute("span")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("span")
		@Override
		public C15Carrier.C15CarrierBuilder setSpan(Void _span) {
			this.span = _span == null ? null : _span;
			return this;
		}
		
		@RosettaAttribute("aux")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("aux")
		@Override
		public C15Carrier.C15CarrierBuilder setAux(C15Aux _aux) {
			this.aux = _aux == null ? null : _aux.toBuilder();
			return this;
		}
		
		@Override
		public C15Carrier build() {
			return new C15Carrier.C15CarrierImpl(this);
		}
		
		@Override
		public C15Carrier.C15CarrierBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C15Carrier.C15CarrierBuilder prune() {
			if (aux!=null && !aux.prune().hasData()) aux = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getTok()!=null) return true;
			if (getSpan()!=null) return true;
			if (getAux()!=null && getAux().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C15Carrier.C15CarrierBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C15Carrier.C15CarrierBuilder o = (C15Carrier.C15CarrierBuilder) other;
			
			merger.mergeRosetta(getAux(), o.getAux(), this::setAux);
			
			merger.mergeBasic(getTok(), o.getTok(), this::setTok);
			merger.mergeBasic(getSpan(), o.getSpan(), this::setSpan);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C15Carrier _that = getType().cast(o);
		
			if (!Objects.equals(tok, _that.getTok())) return false;
			if (!Objects.equals(span, _that.getSpan())) return false;
			if (!Objects.equals(aux, _that.getAux())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (tok != null ? tok.hashCode() : 0);
			_result = 31 * _result + (span != null ? span.hashCode() : 0);
			_result = 31 * _result + (aux != null ? aux.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C15CarrierBuilder {" +
				"tok=" + this.tok + ", " +
				"span=" + this.span + ", " +
				"aux=" + this.aux +
			'}';
		}
	}
}
