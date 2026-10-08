package chaos.s14.a1o3;

import chaos.s14.a1o3.meta.C14ClauseMeta;
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
 * docReference AND regulatoryReference at type and attribute level, incl. for-path and rationale_author.
 * @version 1.0.0
 *
 * Body C14REG
 * Corpus Regulation C14Act C14 Chaos Act "Primary corpus." 
 * c14article "3"
 *
 * Provision Type-level provision text.
 *
 *
 * cid
 * Body C14REG
 * Corpus Regulation C14Act C14 Chaos Act "Primary corpus." 
 * c14article "9"
 *
 * Provision For-path provision.
 *
 *
 * Body C14REG
 * Corpus Regulation C14Act C14 Chaos Act "Primary corpus." 
 * c14article "12"
 *
 * Provision Regulatory-reference provision.
 *
 */
@RosettaDataType(value="C14Clause", builder=C14Clause.C14ClauseBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C14Clause", model="chaos", builder=C14Clause.C14ClauseBuilderImpl.class, version="1.0.0")
public interface C14Clause extends RosettaModelObject {

	C14ClauseMeta metaData = new C14ClauseMeta();

	/*********************** Getter Methods  ***********************/
	/**
	 *
	 * Body C14REG
	 * Corpus Regulation C14Act C14 Chaos Act "Primary corpus." 
	 * c14article "3" * c14paragraph "1a"
	 *
	 * Provision Attribute-level provision.
	 *
	 */
	String getCid();
	/**
	 *
	 * Body C14ORG
	 * Corpus Guidance C14Guide  "Secondary corpus without a display string." 
	 * c14annex "B"
	 *
	 * Provision Weighted per annex B.
	 *
	 */
	BigDecimal getWeight();
	C14Aux getAux();

	/*********************** Build Methods  ***********************/
	C14Clause build();
	
	C14Clause.C14ClauseBuilder toBuilder();
	
	static C14Clause.C14ClauseBuilder builder() {
		return new C14Clause.C14ClauseBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C14Clause> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C14Clause> getType() {
		return C14Clause.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("cid"), String.class, getCid(), this);
		processor.processBasic(path.newSubPath("weight"), BigDecimal.class, getWeight(), this);
		processRosetta(path.newSubPath("aux"), processor, C14Aux.class, getAux());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C14ClauseBuilder extends C14Clause, RosettaModelObjectBuilder {
		C14Aux.C14AuxBuilder getOrCreateAux();
		@Override
		C14Aux.C14AuxBuilder getAux();
		C14Clause.C14ClauseBuilder setCid(String cid);
		C14Clause.C14ClauseBuilder setWeight(BigDecimal weight);
		C14Clause.C14ClauseBuilder setAux(C14Aux aux);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("cid"), String.class, getCid(), this);
			processor.processBasic(path.newSubPath("weight"), BigDecimal.class, getWeight(), this);
			processRosetta(path.newSubPath("aux"), processor, C14Aux.C14AuxBuilder.class, getAux());
		}
		

		C14Clause.C14ClauseBuilder prune();
	}

	/*********************** Immutable Implementation of C14Clause  ***********************/
	class C14ClauseImpl implements C14Clause {
		private final String cid;
		private final BigDecimal weight;
		private final C14Aux aux;
		
		protected C14ClauseImpl(C14Clause.C14ClauseBuilder builder) {
			this.cid = builder.getCid();
			this.weight = builder.getWeight();
			this.aux = ofNullable(builder.getAux()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("cid")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("cid")
		public String getCid() {
			return cid;
		}
		
		@Override
		@RosettaAttribute("weight")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("weight")
		public BigDecimal getWeight() {
			return weight;
		}
		
		@Override
		@RosettaAttribute("aux")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("aux")
		public C14Aux getAux() {
			return aux;
		}
		
		@Override
		public C14Clause build() {
			return this;
		}
		
		@Override
		public C14Clause.C14ClauseBuilder toBuilder() {
			C14Clause.C14ClauseBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C14Clause.C14ClauseBuilder builder) {
			ofNullable(getCid()).ifPresent(builder::setCid);
			ofNullable(getWeight()).ifPresent(builder::setWeight);
			ofNullable(getAux()).ifPresent(builder::setAux);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C14Clause _that = getType().cast(o);
		
			if (!Objects.equals(cid, _that.getCid())) return false;
			if (!Objects.equals(weight, _that.getWeight())) return false;
			if (!Objects.equals(aux, _that.getAux())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (cid != null ? cid.hashCode() : 0);
			_result = 31 * _result + (weight != null ? weight.hashCode() : 0);
			_result = 31 * _result + (aux != null ? aux.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C14Clause {" +
				"cid=" + this.cid + ", " +
				"weight=" + this.weight + ", " +
				"aux=" + this.aux +
			'}';
		}
	}

	/*********************** Builder Implementation of C14Clause  ***********************/
	class C14ClauseBuilderImpl implements C14Clause.C14ClauseBuilder {
	
		protected String cid;
		protected BigDecimal weight;
		protected C14Aux.C14AuxBuilder aux;
		
		@Override
		@RosettaAttribute("cid")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("cid")
		public String getCid() {
			return cid;
		}
		
		@Override
		@RosettaAttribute("weight")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("weight")
		public BigDecimal getWeight() {
			return weight;
		}
		
		@Override
		@RosettaAttribute("aux")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("aux")
		public C14Aux.C14AuxBuilder getAux() {
			return aux;
		}
		
		@Override
		public C14Aux.C14AuxBuilder getOrCreateAux() {
			C14Aux.C14AuxBuilder result;
			if (aux!=null) {
				result = aux;
			}
			else {
				result = aux = C14Aux.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("cid")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("cid")
		@Override
		public C14Clause.C14ClauseBuilder setCid(String _cid) {
			this.cid = _cid == null ? null : _cid;
			return this;
		}
		
		@RosettaAttribute("weight")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("weight")
		@Override
		public C14Clause.C14ClauseBuilder setWeight(BigDecimal _weight) {
			this.weight = _weight == null ? null : _weight;
			return this;
		}
		
		@RosettaAttribute("aux")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("aux")
		@Override
		public C14Clause.C14ClauseBuilder setAux(C14Aux _aux) {
			this.aux = _aux == null ? null : _aux.toBuilder();
			return this;
		}
		
		@Override
		public C14Clause build() {
			return new C14Clause.C14ClauseImpl(this);
		}
		
		@Override
		public C14Clause.C14ClauseBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C14Clause.C14ClauseBuilder prune() {
			if (aux!=null && !aux.prune().hasData()) aux = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getCid()!=null) return true;
			if (getWeight()!=null) return true;
			if (getAux()!=null && getAux().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C14Clause.C14ClauseBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C14Clause.C14ClauseBuilder o = (C14Clause.C14ClauseBuilder) other;
			
			merger.mergeRosetta(getAux(), o.getAux(), this::setAux);
			
			merger.mergeBasic(getCid(), o.getCid(), this::setCid);
			merger.mergeBasic(getWeight(), o.getWeight(), this::setWeight);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C14Clause _that = getType().cast(o);
		
			if (!Objects.equals(cid, _that.getCid())) return false;
			if (!Objects.equals(weight, _that.getWeight())) return false;
			if (!Objects.equals(aux, _that.getAux())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (cid != null ? cid.hashCode() : 0);
			_result = 31 * _result + (weight != null ? weight.hashCode() : 0);
			_result = 31 * _result + (aux != null ? aux.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C14ClauseBuilder {" +
				"cid=" + this.cid + ", " +
				"weight=" + this.weight + ", " +
				"aux=" + this.aux +
			'}';
		}
	}
}
