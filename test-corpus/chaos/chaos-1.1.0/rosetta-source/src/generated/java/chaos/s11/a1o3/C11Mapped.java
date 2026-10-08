package chaos.s11.a1o3;

import chaos.s11.a1o3.meta.C11MappedMeta;
import com.google.common.collect.ImmutableList;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Multi;
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
import com.rosetta.model.lib.records.Date;
import com.rosetta.util.ListEquals;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * Inline synonym bodies: every alternative the grammar&#39;s rosettaSynonymBody admits.
 * @version 1.0.0
 */
@RosettaDataType(value="C11Mapped", builder=C11Mapped.C11MappedBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C11Mapped", model="chaos", builder=C11Mapped.C11MappedBuilderImpl.class, version="1.0.0")
public interface C11Mapped extends RosettaModelObject {

	C11MappedMeta metaData = new C11MappedMeta();

	/*********************** Getter Methods  ***********************/
	String getIdent();
	BigDecimal getTotal();
	String getKindCode();
	String getFlagged();
	List<String> getMerged();
	String getTagged();
	String getMetaCarrier();
	String getDefaulted();
	String getTested();
	String getPathed();
	Date getDated();
	String getPatterned();
	String getMetaOnly();
	C11Aux getAux();

	/*********************** Build Methods  ***********************/
	C11Mapped build();
	
	C11Mapped.C11MappedBuilder toBuilder();
	
	static C11Mapped.C11MappedBuilder builder() {
		return new C11Mapped.C11MappedBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C11Mapped> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C11Mapped> getType() {
		return C11Mapped.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("ident"), String.class, getIdent(), this);
		processor.processBasic(path.newSubPath("total"), BigDecimal.class, getTotal(), this);
		processor.processBasic(path.newSubPath("kindCode"), String.class, getKindCode(), this);
		processor.processBasic(path.newSubPath("flagged"), String.class, getFlagged(), this);
		processor.processBasic(path.newSubPath("merged"), String.class, getMerged(), this);
		processor.processBasic(path.newSubPath("tagged"), String.class, getTagged(), this);
		processor.processBasic(path.newSubPath("metaCarrier"), String.class, getMetaCarrier(), this);
		processor.processBasic(path.newSubPath("defaulted"), String.class, getDefaulted(), this);
		processor.processBasic(path.newSubPath("tested"), String.class, getTested(), this);
		processor.processBasic(path.newSubPath("pathed"), String.class, getPathed(), this);
		processor.processBasic(path.newSubPath("dated"), Date.class, getDated(), this);
		processor.processBasic(path.newSubPath("patterned"), String.class, getPatterned(), this);
		processor.processBasic(path.newSubPath("metaOnly"), String.class, getMetaOnly(), this);
		processRosetta(path.newSubPath("aux"), processor, C11Aux.class, getAux());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C11MappedBuilder extends C11Mapped, RosettaModelObjectBuilder {
		C11Aux.C11AuxBuilder getOrCreateAux();
		@Override
		C11Aux.C11AuxBuilder getAux();
		C11Mapped.C11MappedBuilder setIdent(String ident);
		C11Mapped.C11MappedBuilder setTotal(BigDecimal total);
		C11Mapped.C11MappedBuilder setKindCode(String kindCode);
		C11Mapped.C11MappedBuilder setFlagged(String flagged);
		C11Mapped.C11MappedBuilder addMerged(String merged);
		C11Mapped.C11MappedBuilder addMerged(String merged, int idx);
		C11Mapped.C11MappedBuilder addMerged(List<String> merged);
		C11Mapped.C11MappedBuilder setMerged(List<String> merged);
		C11Mapped.C11MappedBuilder setTagged(String tagged);
		C11Mapped.C11MappedBuilder setMetaCarrier(String metaCarrier);
		C11Mapped.C11MappedBuilder setDefaulted(String defaulted);
		C11Mapped.C11MappedBuilder setTested(String tested);
		C11Mapped.C11MappedBuilder setPathed(String pathed);
		C11Mapped.C11MappedBuilder setDated(Date dated);
		C11Mapped.C11MappedBuilder setPatterned(String patterned);
		C11Mapped.C11MappedBuilder setMetaOnly(String metaOnly);
		C11Mapped.C11MappedBuilder setAux(C11Aux aux);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("ident"), String.class, getIdent(), this);
			processor.processBasic(path.newSubPath("total"), BigDecimal.class, getTotal(), this);
			processor.processBasic(path.newSubPath("kindCode"), String.class, getKindCode(), this);
			processor.processBasic(path.newSubPath("flagged"), String.class, getFlagged(), this);
			processor.processBasic(path.newSubPath("merged"), String.class, getMerged(), this);
			processor.processBasic(path.newSubPath("tagged"), String.class, getTagged(), this);
			processor.processBasic(path.newSubPath("metaCarrier"), String.class, getMetaCarrier(), this);
			processor.processBasic(path.newSubPath("defaulted"), String.class, getDefaulted(), this);
			processor.processBasic(path.newSubPath("tested"), String.class, getTested(), this);
			processor.processBasic(path.newSubPath("pathed"), String.class, getPathed(), this);
			processor.processBasic(path.newSubPath("dated"), Date.class, getDated(), this);
			processor.processBasic(path.newSubPath("patterned"), String.class, getPatterned(), this);
			processor.processBasic(path.newSubPath("metaOnly"), String.class, getMetaOnly(), this);
			processRosetta(path.newSubPath("aux"), processor, C11Aux.C11AuxBuilder.class, getAux());
		}
		

		C11Mapped.C11MappedBuilder prune();
	}

	/*********************** Immutable Implementation of C11Mapped  ***********************/
	class C11MappedImpl implements C11Mapped {
		private final String ident;
		private final BigDecimal total;
		private final String kindCode;
		private final String flagged;
		private final List<String> merged;
		private final String tagged;
		private final String metaCarrier;
		private final String defaulted;
		private final String tested;
		private final String pathed;
		private final Date dated;
		private final String patterned;
		private final String metaOnly;
		private final C11Aux aux;
		
		protected C11MappedImpl(C11Mapped.C11MappedBuilder builder) {
			this.ident = builder.getIdent();
			this.total = builder.getTotal();
			this.kindCode = builder.getKindCode();
			this.flagged = builder.getFlagged();
			this.merged = ofNullable(builder.getMerged()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.tagged = builder.getTagged();
			this.metaCarrier = builder.getMetaCarrier();
			this.defaulted = builder.getDefaulted();
			this.tested = builder.getTested();
			this.pathed = builder.getPathed();
			this.dated = builder.getDated();
			this.patterned = builder.getPatterned();
			this.metaOnly = builder.getMetaOnly();
			this.aux = ofNullable(builder.getAux()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("ident")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("ident")
		public String getIdent() {
			return ident;
		}
		
		@Override
		@RosettaAttribute("total")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("total")
		public BigDecimal getTotal() {
			return total;
		}
		
		@Override
		@RosettaAttribute("kindCode")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("kindCode")
		public String getKindCode() {
			return kindCode;
		}
		
		@Override
		@RosettaAttribute("flagged")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("flagged")
		public String getFlagged() {
			return flagged;
		}
		
		@Override
		@RosettaAttribute("merged")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("merged")
		public List<String> getMerged() {
			return merged;
		}
		
		@Override
		@RosettaAttribute("tagged")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("tagged")
		public String getTagged() {
			return tagged;
		}
		
		@Override
		@RosettaAttribute("metaCarrier")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("metaCarrier")
		public String getMetaCarrier() {
			return metaCarrier;
		}
		
		@Override
		@RosettaAttribute("defaulted")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("defaulted")
		public String getDefaulted() {
			return defaulted;
		}
		
		@Override
		@RosettaAttribute("tested")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("tested")
		public String getTested() {
			return tested;
		}
		
		@Override
		@RosettaAttribute("pathed")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("pathed")
		public String getPathed() {
			return pathed;
		}
		
		@Override
		@RosettaAttribute("dated")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("dated")
		public Date getDated() {
			return dated;
		}
		
		@Override
		@RosettaAttribute("patterned")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("patterned")
		public String getPatterned() {
			return patterned;
		}
		
		@Override
		@RosettaAttribute("metaOnly")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("metaOnly")
		public String getMetaOnly() {
			return metaOnly;
		}
		
		@Override
		@RosettaAttribute("aux")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("aux")
		public C11Aux getAux() {
			return aux;
		}
		
		@Override
		public C11Mapped build() {
			return this;
		}
		
		@Override
		public C11Mapped.C11MappedBuilder toBuilder() {
			C11Mapped.C11MappedBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C11Mapped.C11MappedBuilder builder) {
			ofNullable(getIdent()).ifPresent(builder::setIdent);
			ofNullable(getTotal()).ifPresent(builder::setTotal);
			ofNullable(getKindCode()).ifPresent(builder::setKindCode);
			ofNullable(getFlagged()).ifPresent(builder::setFlagged);
			ofNullable(getMerged()).ifPresent(builder::setMerged);
			ofNullable(getTagged()).ifPresent(builder::setTagged);
			ofNullable(getMetaCarrier()).ifPresent(builder::setMetaCarrier);
			ofNullable(getDefaulted()).ifPresent(builder::setDefaulted);
			ofNullable(getTested()).ifPresent(builder::setTested);
			ofNullable(getPathed()).ifPresent(builder::setPathed);
			ofNullable(getDated()).ifPresent(builder::setDated);
			ofNullable(getPatterned()).ifPresent(builder::setPatterned);
			ofNullable(getMetaOnly()).ifPresent(builder::setMetaOnly);
			ofNullable(getAux()).ifPresent(builder::setAux);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C11Mapped _that = getType().cast(o);
		
			if (!Objects.equals(ident, _that.getIdent())) return false;
			if (!Objects.equals(total, _that.getTotal())) return false;
			if (!Objects.equals(kindCode, _that.getKindCode())) return false;
			if (!Objects.equals(flagged, _that.getFlagged())) return false;
			if (!ListEquals.listEquals(merged, _that.getMerged())) return false;
			if (!Objects.equals(tagged, _that.getTagged())) return false;
			if (!Objects.equals(metaCarrier, _that.getMetaCarrier())) return false;
			if (!Objects.equals(defaulted, _that.getDefaulted())) return false;
			if (!Objects.equals(tested, _that.getTested())) return false;
			if (!Objects.equals(pathed, _that.getPathed())) return false;
			if (!Objects.equals(dated, _that.getDated())) return false;
			if (!Objects.equals(patterned, _that.getPatterned())) return false;
			if (!Objects.equals(metaOnly, _that.getMetaOnly())) return false;
			if (!Objects.equals(aux, _that.getAux())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (ident != null ? ident.hashCode() : 0);
			_result = 31 * _result + (total != null ? total.hashCode() : 0);
			_result = 31 * _result + (kindCode != null ? kindCode.hashCode() : 0);
			_result = 31 * _result + (flagged != null ? flagged.hashCode() : 0);
			_result = 31 * _result + (merged != null ? merged.hashCode() : 0);
			_result = 31 * _result + (tagged != null ? tagged.hashCode() : 0);
			_result = 31 * _result + (metaCarrier != null ? metaCarrier.hashCode() : 0);
			_result = 31 * _result + (defaulted != null ? defaulted.hashCode() : 0);
			_result = 31 * _result + (tested != null ? tested.hashCode() : 0);
			_result = 31 * _result + (pathed != null ? pathed.hashCode() : 0);
			_result = 31 * _result + (dated != null ? dated.hashCode() : 0);
			_result = 31 * _result + (patterned != null ? patterned.hashCode() : 0);
			_result = 31 * _result + (metaOnly != null ? metaOnly.hashCode() : 0);
			_result = 31 * _result + (aux != null ? aux.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C11Mapped {" +
				"ident=" + this.ident + ", " +
				"total=" + this.total + ", " +
				"kindCode=" + this.kindCode + ", " +
				"flagged=" + this.flagged + ", " +
				"merged=" + this.merged + ", " +
				"tagged=" + this.tagged + ", " +
				"metaCarrier=" + this.metaCarrier + ", " +
				"defaulted=" + this.defaulted + ", " +
				"tested=" + this.tested + ", " +
				"pathed=" + this.pathed + ", " +
				"dated=" + this.dated + ", " +
				"patterned=" + this.patterned + ", " +
				"metaOnly=" + this.metaOnly + ", " +
				"aux=" + this.aux +
			'}';
		}
	}

	/*********************** Builder Implementation of C11Mapped  ***********************/
	class C11MappedBuilderImpl implements C11Mapped.C11MappedBuilder {
	
		protected String ident;
		protected BigDecimal total;
		protected String kindCode;
		protected String flagged;
		protected List<String> merged = new ArrayList<>();
		protected String tagged;
		protected String metaCarrier;
		protected String defaulted;
		protected String tested;
		protected String pathed;
		protected Date dated;
		protected String patterned;
		protected String metaOnly;
		protected C11Aux.C11AuxBuilder aux;
		
		@Override
		@RosettaAttribute("ident")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("ident")
		public String getIdent() {
			return ident;
		}
		
		@Override
		@RosettaAttribute("total")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("total")
		public BigDecimal getTotal() {
			return total;
		}
		
		@Override
		@RosettaAttribute("kindCode")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("kindCode")
		public String getKindCode() {
			return kindCode;
		}
		
		@Override
		@RosettaAttribute("flagged")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("flagged")
		public String getFlagged() {
			return flagged;
		}
		
		@Override
		@RosettaAttribute("merged")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("merged")
		public List<String> getMerged() {
			return merged;
		}
		
		@Override
		@RosettaAttribute("tagged")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("tagged")
		public String getTagged() {
			return tagged;
		}
		
		@Override
		@RosettaAttribute("metaCarrier")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("metaCarrier")
		public String getMetaCarrier() {
			return metaCarrier;
		}
		
		@Override
		@RosettaAttribute("defaulted")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("defaulted")
		public String getDefaulted() {
			return defaulted;
		}
		
		@Override
		@RosettaAttribute("tested")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("tested")
		public String getTested() {
			return tested;
		}
		
		@Override
		@RosettaAttribute("pathed")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("pathed")
		public String getPathed() {
			return pathed;
		}
		
		@Override
		@RosettaAttribute("dated")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("dated")
		public Date getDated() {
			return dated;
		}
		
		@Override
		@RosettaAttribute("patterned")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("patterned")
		public String getPatterned() {
			return patterned;
		}
		
		@Override
		@RosettaAttribute("metaOnly")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("metaOnly")
		public String getMetaOnly() {
			return metaOnly;
		}
		
		@Override
		@RosettaAttribute("aux")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("aux")
		public C11Aux.C11AuxBuilder getAux() {
			return aux;
		}
		
		@Override
		public C11Aux.C11AuxBuilder getOrCreateAux() {
			C11Aux.C11AuxBuilder result;
			if (aux!=null) {
				result = aux;
			}
			else {
				result = aux = C11Aux.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("ident")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("ident")
		@Override
		public C11Mapped.C11MappedBuilder setIdent(String _ident) {
			this.ident = _ident == null ? null : _ident;
			return this;
		}
		
		@RosettaAttribute("total")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("total")
		@Override
		public C11Mapped.C11MappedBuilder setTotal(BigDecimal _total) {
			this.total = _total == null ? null : _total;
			return this;
		}
		
		@RosettaAttribute("kindCode")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("kindCode")
		@Override
		public C11Mapped.C11MappedBuilder setKindCode(String _kindCode) {
			this.kindCode = _kindCode == null ? null : _kindCode;
			return this;
		}
		
		@RosettaAttribute("flagged")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("flagged")
		@Override
		public C11Mapped.C11MappedBuilder setFlagged(String _flagged) {
			this.flagged = _flagged == null ? null : _flagged;
			return this;
		}
		
		@RosettaAttribute("merged")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("merged")
		@Override
		public C11Mapped.C11MappedBuilder addMerged(String _merged) {
			if (_merged != null) {
				this.merged.add(_merged);
			}
			return this;
		}
		
		@Override
		public C11Mapped.C11MappedBuilder addMerged(String _merged, int idx) {
			getIndex(this.merged, idx, () -> _merged);
			return this;
		}
		
		@Override
		public C11Mapped.C11MappedBuilder addMerged(List<String> mergeds) {
			if (mergeds != null) {
				for (final String toAdd : mergeds) {
					this.merged.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("merged")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("merged")
		@Override
		public C11Mapped.C11MappedBuilder setMerged(List<String> mergeds) {
			if (mergeds == null) {
				this.merged = new ArrayList<>();
			} else {
				this.merged = mergeds.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("tagged")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("tagged")
		@Override
		public C11Mapped.C11MappedBuilder setTagged(String _tagged) {
			this.tagged = _tagged == null ? null : _tagged;
			return this;
		}
		
		@RosettaAttribute("metaCarrier")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("metaCarrier")
		@Override
		public C11Mapped.C11MappedBuilder setMetaCarrier(String _metaCarrier) {
			this.metaCarrier = _metaCarrier == null ? null : _metaCarrier;
			return this;
		}
		
		@RosettaAttribute("defaulted")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("defaulted")
		@Override
		public C11Mapped.C11MappedBuilder setDefaulted(String _defaulted) {
			this.defaulted = _defaulted == null ? null : _defaulted;
			return this;
		}
		
		@RosettaAttribute("tested")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("tested")
		@Override
		public C11Mapped.C11MappedBuilder setTested(String _tested) {
			this.tested = _tested == null ? null : _tested;
			return this;
		}
		
		@RosettaAttribute("pathed")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("pathed")
		@Override
		public C11Mapped.C11MappedBuilder setPathed(String _pathed) {
			this.pathed = _pathed == null ? null : _pathed;
			return this;
		}
		
		@RosettaAttribute("dated")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("dated")
		@Override
		public C11Mapped.C11MappedBuilder setDated(Date _dated) {
			this.dated = _dated == null ? null : _dated;
			return this;
		}
		
		@RosettaAttribute("patterned")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("patterned")
		@Override
		public C11Mapped.C11MappedBuilder setPatterned(String _patterned) {
			this.patterned = _patterned == null ? null : _patterned;
			return this;
		}
		
		@RosettaAttribute("metaOnly")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("metaOnly")
		@Override
		public C11Mapped.C11MappedBuilder setMetaOnly(String _metaOnly) {
			this.metaOnly = _metaOnly == null ? null : _metaOnly;
			return this;
		}
		
		@RosettaAttribute("aux")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("aux")
		@Override
		public C11Mapped.C11MappedBuilder setAux(C11Aux _aux) {
			this.aux = _aux == null ? null : _aux.toBuilder();
			return this;
		}
		
		@Override
		public C11Mapped build() {
			return new C11Mapped.C11MappedImpl(this);
		}
		
		@Override
		public C11Mapped.C11MappedBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C11Mapped.C11MappedBuilder prune() {
			if (aux!=null && !aux.prune().hasData()) aux = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getIdent()!=null) return true;
			if (getTotal()!=null) return true;
			if (getKindCode()!=null) return true;
			if (getFlagged()!=null) return true;
			if (getMerged()!=null && !getMerged().isEmpty()) return true;
			if (getTagged()!=null) return true;
			if (getMetaCarrier()!=null) return true;
			if (getDefaulted()!=null) return true;
			if (getTested()!=null) return true;
			if (getPathed()!=null) return true;
			if (getDated()!=null) return true;
			if (getPatterned()!=null) return true;
			if (getMetaOnly()!=null) return true;
			if (getAux()!=null && getAux().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C11Mapped.C11MappedBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C11Mapped.C11MappedBuilder o = (C11Mapped.C11MappedBuilder) other;
			
			merger.mergeRosetta(getAux(), o.getAux(), this::setAux);
			
			merger.mergeBasic(getIdent(), o.getIdent(), this::setIdent);
			merger.mergeBasic(getTotal(), o.getTotal(), this::setTotal);
			merger.mergeBasic(getKindCode(), o.getKindCode(), this::setKindCode);
			merger.mergeBasic(getFlagged(), o.getFlagged(), this::setFlagged);
			merger.mergeBasic(getMerged(), o.getMerged(), (Consumer<String>) this::addMerged);
			merger.mergeBasic(getTagged(), o.getTagged(), this::setTagged);
			merger.mergeBasic(getMetaCarrier(), o.getMetaCarrier(), this::setMetaCarrier);
			merger.mergeBasic(getDefaulted(), o.getDefaulted(), this::setDefaulted);
			merger.mergeBasic(getTested(), o.getTested(), this::setTested);
			merger.mergeBasic(getPathed(), o.getPathed(), this::setPathed);
			merger.mergeBasic(getDated(), o.getDated(), this::setDated);
			merger.mergeBasic(getPatterned(), o.getPatterned(), this::setPatterned);
			merger.mergeBasic(getMetaOnly(), o.getMetaOnly(), this::setMetaOnly);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C11Mapped _that = getType().cast(o);
		
			if (!Objects.equals(ident, _that.getIdent())) return false;
			if (!Objects.equals(total, _that.getTotal())) return false;
			if (!Objects.equals(kindCode, _that.getKindCode())) return false;
			if (!Objects.equals(flagged, _that.getFlagged())) return false;
			if (!ListEquals.listEquals(merged, _that.getMerged())) return false;
			if (!Objects.equals(tagged, _that.getTagged())) return false;
			if (!Objects.equals(metaCarrier, _that.getMetaCarrier())) return false;
			if (!Objects.equals(defaulted, _that.getDefaulted())) return false;
			if (!Objects.equals(tested, _that.getTested())) return false;
			if (!Objects.equals(pathed, _that.getPathed())) return false;
			if (!Objects.equals(dated, _that.getDated())) return false;
			if (!Objects.equals(patterned, _that.getPatterned())) return false;
			if (!Objects.equals(metaOnly, _that.getMetaOnly())) return false;
			if (!Objects.equals(aux, _that.getAux())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (ident != null ? ident.hashCode() : 0);
			_result = 31 * _result + (total != null ? total.hashCode() : 0);
			_result = 31 * _result + (kindCode != null ? kindCode.hashCode() : 0);
			_result = 31 * _result + (flagged != null ? flagged.hashCode() : 0);
			_result = 31 * _result + (merged != null ? merged.hashCode() : 0);
			_result = 31 * _result + (tagged != null ? tagged.hashCode() : 0);
			_result = 31 * _result + (metaCarrier != null ? metaCarrier.hashCode() : 0);
			_result = 31 * _result + (defaulted != null ? defaulted.hashCode() : 0);
			_result = 31 * _result + (tested != null ? tested.hashCode() : 0);
			_result = 31 * _result + (pathed != null ? pathed.hashCode() : 0);
			_result = 31 * _result + (dated != null ? dated.hashCode() : 0);
			_result = 31 * _result + (patterned != null ? patterned.hashCode() : 0);
			_result = 31 * _result + (metaOnly != null ? metaOnly.hashCode() : 0);
			_result = 31 * _result + (aux != null ? aux.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C11MappedBuilder {" +
				"ident=" + this.ident + ", " +
				"total=" + this.total + ", " +
				"kindCode=" + this.kindCode + ", " +
				"flagged=" + this.flagged + ", " +
				"merged=" + this.merged + ", " +
				"tagged=" + this.tagged + ", " +
				"metaCarrier=" + this.metaCarrier + ", " +
				"defaulted=" + this.defaulted + ", " +
				"tested=" + this.tested + ", " +
				"pathed=" + this.pathed + ", " +
				"dated=" + this.dated + ", " +
				"patterned=" + this.patterned + ", " +
				"metaOnly=" + this.metaOnly + ", " +
				"aux=" + this.aux +
			'}';
		}
	}
}
