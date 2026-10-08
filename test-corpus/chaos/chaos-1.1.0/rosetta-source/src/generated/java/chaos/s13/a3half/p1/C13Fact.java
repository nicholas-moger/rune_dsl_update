package chaos.s13.a3half.p1;

import chaos.s13.a3half.p1.meta.C13FactMeta;
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
import java.math.BigDecimal;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * Rule-source subject type.
 * @version 1.0.0
 */
@RosettaDataType(value="C13Fact", builder=C13Fact.C13FactBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C13Fact", model="chaos", builder=C13Fact.C13FactBuilderImpl.class, version="1.0.0")
public interface C13Fact extends RosettaModelObject {

	C13FactMeta metaData = new C13FactMeta();

	/*********************** Getter Methods  ***********************/
	String getFid();
	BigDecimal getAmt();
	C13Aux getAux();

	/*********************** Build Methods  ***********************/
	C13Fact build();
	
	C13Fact.C13FactBuilder toBuilder();
	
	static C13Fact.C13FactBuilder builder() {
		return new C13Fact.C13FactBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C13Fact> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C13Fact> getType() {
		return C13Fact.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("fid"), String.class, getFid(), this);
		processor.processBasic(path.newSubPath("amt"), BigDecimal.class, getAmt(), this);
		processRosetta(path.newSubPath("aux"), processor, C13Aux.class, getAux());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C13FactBuilder extends C13Fact, RosettaModelObjectBuilder {
		C13Aux.C13AuxBuilder getOrCreateAux();
		@Override
		C13Aux.C13AuxBuilder getAux();
		C13Fact.C13FactBuilder setFid(String fid);
		C13Fact.C13FactBuilder setAmt(BigDecimal amt);
		C13Fact.C13FactBuilder setAux(C13Aux aux);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("fid"), String.class, getFid(), this);
			processor.processBasic(path.newSubPath("amt"), BigDecimal.class, getAmt(), this);
			processRosetta(path.newSubPath("aux"), processor, C13Aux.C13AuxBuilder.class, getAux());
		}
		

		C13Fact.C13FactBuilder prune();
	}

	/*********************** Immutable Implementation of C13Fact  ***********************/
	class C13FactImpl implements C13Fact {
		private final String fid;
		private final BigDecimal amt;
		private final C13Aux aux;
		
		protected C13FactImpl(C13Fact.C13FactBuilder builder) {
			this.fid = builder.getFid();
			this.amt = builder.getAmt();
			this.aux = ofNullable(builder.getAux()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("fid")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("fid")
		public String getFid() {
			return fid;
		}
		
		@Override
		@RosettaAttribute("amt")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("amt")
		public BigDecimal getAmt() {
			return amt;
		}
		
		@Override
		@RosettaAttribute("aux")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("aux")
		public C13Aux getAux() {
			return aux;
		}
		
		@Override
		public C13Fact build() {
			return this;
		}
		
		@Override
		public C13Fact.C13FactBuilder toBuilder() {
			C13Fact.C13FactBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C13Fact.C13FactBuilder builder) {
			ofNullable(getFid()).ifPresent(builder::setFid);
			ofNullable(getAmt()).ifPresent(builder::setAmt);
			ofNullable(getAux()).ifPresent(builder::setAux);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C13Fact _that = getType().cast(o);
		
			if (!Objects.equals(fid, _that.getFid())) return false;
			if (!Objects.equals(amt, _that.getAmt())) return false;
			if (!Objects.equals(aux, _that.getAux())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (fid != null ? fid.hashCode() : 0);
			_result = 31 * _result + (amt != null ? amt.hashCode() : 0);
			_result = 31 * _result + (aux != null ? aux.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C13Fact {" +
				"fid=" + this.fid + ", " +
				"amt=" + this.amt + ", " +
				"aux=" + this.aux +
			'}';
		}
	}

	/*********************** Builder Implementation of C13Fact  ***********************/
	class C13FactBuilderImpl implements C13Fact.C13FactBuilder {
	
		protected String fid;
		protected BigDecimal amt;
		protected C13Aux.C13AuxBuilder aux;
		
		@Override
		@RosettaAttribute("fid")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("fid")
		public String getFid() {
			return fid;
		}
		
		@Override
		@RosettaAttribute("amt")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("amt")
		public BigDecimal getAmt() {
			return amt;
		}
		
		@Override
		@RosettaAttribute("aux")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("aux")
		public C13Aux.C13AuxBuilder getAux() {
			return aux;
		}
		
		@Override
		public C13Aux.C13AuxBuilder getOrCreateAux() {
			C13Aux.C13AuxBuilder result;
			if (aux!=null) {
				result = aux;
			}
			else {
				result = aux = C13Aux.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("fid")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("fid")
		@Override
		public C13Fact.C13FactBuilder setFid(String _fid) {
			this.fid = _fid == null ? null : _fid;
			return this;
		}
		
		@RosettaAttribute("amt")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("amt")
		@Override
		public C13Fact.C13FactBuilder setAmt(BigDecimal _amt) {
			this.amt = _amt == null ? null : _amt;
			return this;
		}
		
		@RosettaAttribute("aux")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("aux")
		@Override
		public C13Fact.C13FactBuilder setAux(C13Aux _aux) {
			this.aux = _aux == null ? null : _aux.toBuilder();
			return this;
		}
		
		@Override
		public C13Fact build() {
			return new C13Fact.C13FactImpl(this);
		}
		
		@Override
		public C13Fact.C13FactBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C13Fact.C13FactBuilder prune() {
			if (aux!=null && !aux.prune().hasData()) aux = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getFid()!=null) return true;
			if (getAmt()!=null) return true;
			if (getAux()!=null && getAux().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C13Fact.C13FactBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C13Fact.C13FactBuilder o = (C13Fact.C13FactBuilder) other;
			
			merger.mergeRosetta(getAux(), o.getAux(), this::setAux);
			
			merger.mergeBasic(getFid(), o.getFid(), this::setFid);
			merger.mergeBasic(getAmt(), o.getAmt(), this::setAmt);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C13Fact _that = getType().cast(o);
		
			if (!Objects.equals(fid, _that.getFid())) return false;
			if (!Objects.equals(amt, _that.getAmt())) return false;
			if (!Objects.equals(aux, _that.getAux())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (fid != null ? fid.hashCode() : 0);
			_result = 31 * _result + (amt != null ? amt.hashCode() : 0);
			_result = 31 * _result + (aux != null ? aux.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C13FactBuilder {" +
				"fid=" + this.fid + ", " +
				"amt=" + this.amt + ", " +
				"aux=" + this.aux +
			'}';
		}
	}
}
