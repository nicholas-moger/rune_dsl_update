package test.aliascond;

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
import com.rosetta.util.ListEquals;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import test.aliascond.meta.HolderMeta;

import static java.util.Optional.ofNullable;

/**
 * Alias-typed attributes at every cardinality, plus the type&#39;s OWN condition beside them.
 * @version 0.0.0
 */
@RosettaDataType(value="Holder", builder=Holder.HolderBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Holder", model="test", builder=Holder.HolderBuilderImpl.class, version="0.0.0")
public interface Holder extends RosettaModelObject {

	HolderMeta metaData = new HolderMeta();

	/*********************** Getter Methods  ***********************/
	Integer getOne();
	Integer getOpt();
	List<Integer> getMany();
	List<Integer> getSome();
	BigDecimal getPct();
	List<BigDecimal> getPcts();
	String getCode();
	Boolean getFlag();
	Integer getNested();
	List<Integer> getNesteds();
	Integer getUnnamed();
	BigDecimal getChecked();
	BigDecimal getPlain();

	/*********************** Build Methods  ***********************/
	Holder build();
	
	Holder.HolderBuilder toBuilder();
	
	static Holder.HolderBuilder builder() {
		return new Holder.HolderBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Holder> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Holder> getType() {
		return Holder.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("one"), Integer.class, getOne(), this);
		processor.processBasic(path.newSubPath("opt"), Integer.class, getOpt(), this);
		processor.processBasic(path.newSubPath("many"), Integer.class, getMany(), this);
		processor.processBasic(path.newSubPath("some"), Integer.class, getSome(), this);
		processor.processBasic(path.newSubPath("pct"), BigDecimal.class, getPct(), this);
		processor.processBasic(path.newSubPath("pcts"), BigDecimal.class, getPcts(), this);
		processor.processBasic(path.newSubPath("code"), String.class, getCode(), this);
		processor.processBasic(path.newSubPath("flag"), Boolean.class, getFlag(), this);
		processor.processBasic(path.newSubPath("nested"), Integer.class, getNested(), this);
		processor.processBasic(path.newSubPath("nesteds"), Integer.class, getNesteds(), this);
		processor.processBasic(path.newSubPath("unnamed"), Integer.class, getUnnamed(), this);
		processor.processBasic(path.newSubPath("checked"), BigDecimal.class, getChecked(), this);
		processor.processBasic(path.newSubPath("plain"), BigDecimal.class, getPlain(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface HolderBuilder extends Holder, RosettaModelObjectBuilder {
		Holder.HolderBuilder setOne(Integer one);
		Holder.HolderBuilder setOpt(Integer opt);
		Holder.HolderBuilder addMany(Integer many);
		Holder.HolderBuilder addMany(Integer many, int idx);
		Holder.HolderBuilder addMany(List<Integer> many);
		Holder.HolderBuilder setMany(List<Integer> many);
		Holder.HolderBuilder addSome(Integer some);
		Holder.HolderBuilder addSome(Integer some, int idx);
		Holder.HolderBuilder addSome(List<Integer> some);
		Holder.HolderBuilder setSome(List<Integer> some);
		Holder.HolderBuilder setPct(BigDecimal pct);
		Holder.HolderBuilder addPcts(BigDecimal pcts);
		Holder.HolderBuilder addPcts(BigDecimal pcts, int idx);
		Holder.HolderBuilder addPcts(List<BigDecimal> pcts);
		Holder.HolderBuilder setPcts(List<BigDecimal> pcts);
		Holder.HolderBuilder setCode(String code);
		Holder.HolderBuilder setFlag(Boolean flag);
		Holder.HolderBuilder setNested(Integer nested);
		Holder.HolderBuilder addNesteds(Integer nesteds);
		Holder.HolderBuilder addNesteds(Integer nesteds, int idx);
		Holder.HolderBuilder addNesteds(List<Integer> nesteds);
		Holder.HolderBuilder setNesteds(List<Integer> nesteds);
		Holder.HolderBuilder setUnnamed(Integer unnamed);
		Holder.HolderBuilder setChecked(BigDecimal checked);
		Holder.HolderBuilder setPlain(BigDecimal plain);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("one"), Integer.class, getOne(), this);
			processor.processBasic(path.newSubPath("opt"), Integer.class, getOpt(), this);
			processor.processBasic(path.newSubPath("many"), Integer.class, getMany(), this);
			processor.processBasic(path.newSubPath("some"), Integer.class, getSome(), this);
			processor.processBasic(path.newSubPath("pct"), BigDecimal.class, getPct(), this);
			processor.processBasic(path.newSubPath("pcts"), BigDecimal.class, getPcts(), this);
			processor.processBasic(path.newSubPath("code"), String.class, getCode(), this);
			processor.processBasic(path.newSubPath("flag"), Boolean.class, getFlag(), this);
			processor.processBasic(path.newSubPath("nested"), Integer.class, getNested(), this);
			processor.processBasic(path.newSubPath("nesteds"), Integer.class, getNesteds(), this);
			processor.processBasic(path.newSubPath("unnamed"), Integer.class, getUnnamed(), this);
			processor.processBasic(path.newSubPath("checked"), BigDecimal.class, getChecked(), this);
			processor.processBasic(path.newSubPath("plain"), BigDecimal.class, getPlain(), this);
		}
		

		Holder.HolderBuilder prune();
	}

	/*********************** Immutable Implementation of Holder  ***********************/
	class HolderImpl implements Holder {
		private final Integer one;
		private final Integer opt;
		private final List<Integer> many;
		private final List<Integer> some;
		private final BigDecimal pct;
		private final List<BigDecimal> pcts;
		private final String code;
		private final Boolean flag;
		private final Integer nested;
		private final List<Integer> nesteds;
		private final Integer unnamed;
		private final BigDecimal checked;
		private final BigDecimal plain;
		
		protected HolderImpl(Holder.HolderBuilder builder) {
			this.one = builder.getOne();
			this.opt = builder.getOpt();
			this.many = ofNullable(builder.getMany()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.some = ofNullable(builder.getSome()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.pct = builder.getPct();
			this.pcts = ofNullable(builder.getPcts()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.code = builder.getCode();
			this.flag = builder.getFlag();
			this.nested = builder.getNested();
			this.nesteds = ofNullable(builder.getNesteds()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.unnamed = builder.getUnnamed();
			this.checked = builder.getChecked();
			this.plain = builder.getPlain();
		}
		
		@Override
		@RosettaAttribute("one")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("one")
		public Integer getOne() {
			return one;
		}
		
		@Override
		@RosettaAttribute("opt")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("opt")
		public Integer getOpt() {
			return opt;
		}
		
		@Override
		@RosettaAttribute("many")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("many")
		public List<Integer> getMany() {
			return many;
		}
		
		@Override
		@RosettaAttribute("some")
		@Accessor(AccessorType.GETTER)
		@Required
		@Multi
		@RuneAttribute("some")
		public List<Integer> getSome() {
			return some;
		}
		
		@Override
		@RosettaAttribute("pct")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("pct")
		public BigDecimal getPct() {
			return pct;
		}
		
		@Override
		@RosettaAttribute("pcts")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("pcts")
		public List<BigDecimal> getPcts() {
			return pcts;
		}
		
		@Override
		@RosettaAttribute("code")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("code")
		public String getCode() {
			return code;
		}
		
		@Override
		@RosettaAttribute("flag")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("flag")
		public Boolean getFlag() {
			return flag;
		}
		
		@Override
		@RosettaAttribute("nested")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("nested")
		public Integer getNested() {
			return nested;
		}
		
		@Override
		@RosettaAttribute("nesteds")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("nesteds")
		public List<Integer> getNesteds() {
			return nesteds;
		}
		
		@Override
		@RosettaAttribute("unnamed")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("unnamed")
		public Integer getUnnamed() {
			return unnamed;
		}
		
		@Override
		@RosettaAttribute("checked")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("checked")
		public BigDecimal getChecked() {
			return checked;
		}
		
		@Override
		@RosettaAttribute("plain")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("plain")
		public BigDecimal getPlain() {
			return plain;
		}
		
		@Override
		public Holder build() {
			return this;
		}
		
		@Override
		public Holder.HolderBuilder toBuilder() {
			Holder.HolderBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Holder.HolderBuilder builder) {
			ofNullable(getOne()).ifPresent(builder::setOne);
			ofNullable(getOpt()).ifPresent(builder::setOpt);
			ofNullable(getMany()).ifPresent(builder::setMany);
			ofNullable(getSome()).ifPresent(builder::setSome);
			ofNullable(getPct()).ifPresent(builder::setPct);
			ofNullable(getPcts()).ifPresent(builder::setPcts);
			ofNullable(getCode()).ifPresent(builder::setCode);
			ofNullable(getFlag()).ifPresent(builder::setFlag);
			ofNullable(getNested()).ifPresent(builder::setNested);
			ofNullable(getNesteds()).ifPresent(builder::setNesteds);
			ofNullable(getUnnamed()).ifPresent(builder::setUnnamed);
			ofNullable(getChecked()).ifPresent(builder::setChecked);
			ofNullable(getPlain()).ifPresent(builder::setPlain);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Holder _that = getType().cast(o);
		
			if (!Objects.equals(one, _that.getOne())) return false;
			if (!Objects.equals(opt, _that.getOpt())) return false;
			if (!ListEquals.listEquals(many, _that.getMany())) return false;
			if (!ListEquals.listEquals(some, _that.getSome())) return false;
			if (!Objects.equals(pct, _that.getPct())) return false;
			if (!ListEquals.listEquals(pcts, _that.getPcts())) return false;
			if (!Objects.equals(code, _that.getCode())) return false;
			if (!Objects.equals(flag, _that.getFlag())) return false;
			if (!Objects.equals(nested, _that.getNested())) return false;
			if (!ListEquals.listEquals(nesteds, _that.getNesteds())) return false;
			if (!Objects.equals(unnamed, _that.getUnnamed())) return false;
			if (!Objects.equals(checked, _that.getChecked())) return false;
			if (!Objects.equals(plain, _that.getPlain())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (one != null ? one.hashCode() : 0);
			_result = 31 * _result + (opt != null ? opt.hashCode() : 0);
			_result = 31 * _result + (many != null ? many.hashCode() : 0);
			_result = 31 * _result + (some != null ? some.hashCode() : 0);
			_result = 31 * _result + (pct != null ? pct.hashCode() : 0);
			_result = 31 * _result + (pcts != null ? pcts.hashCode() : 0);
			_result = 31 * _result + (code != null ? code.hashCode() : 0);
			_result = 31 * _result + (flag != null ? flag.hashCode() : 0);
			_result = 31 * _result + (nested != null ? nested.hashCode() : 0);
			_result = 31 * _result + (nesteds != null ? nesteds.hashCode() : 0);
			_result = 31 * _result + (unnamed != null ? unnamed.hashCode() : 0);
			_result = 31 * _result + (checked != null ? checked.hashCode() : 0);
			_result = 31 * _result + (plain != null ? plain.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Holder {" +
				"one=" + this.one + ", " +
				"opt=" + this.opt + ", " +
				"many=" + this.many + ", " +
				"some=" + this.some + ", " +
				"pct=" + this.pct + ", " +
				"pcts=" + this.pcts + ", " +
				"code=" + this.code + ", " +
				"flag=" + this.flag + ", " +
				"nested=" + this.nested + ", " +
				"nesteds=" + this.nesteds + ", " +
				"unnamed=" + this.unnamed + ", " +
				"checked=" + this.checked + ", " +
				"plain=" + this.plain +
			'}';
		}
	}

	/*********************** Builder Implementation of Holder  ***********************/
	class HolderBuilderImpl implements Holder.HolderBuilder {
	
		protected Integer one;
		protected Integer opt;
		protected List<Integer> many = new ArrayList<>();
		protected List<Integer> some = new ArrayList<>();
		protected BigDecimal pct;
		protected List<BigDecimal> pcts = new ArrayList<>();
		protected String code;
		protected Boolean flag;
		protected Integer nested;
		protected List<Integer> nesteds = new ArrayList<>();
		protected Integer unnamed;
		protected BigDecimal checked;
		protected BigDecimal plain;
		
		@Override
		@RosettaAttribute("one")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("one")
		public Integer getOne() {
			return one;
		}
		
		@Override
		@RosettaAttribute("opt")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("opt")
		public Integer getOpt() {
			return opt;
		}
		
		@Override
		@RosettaAttribute("many")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("many")
		public List<Integer> getMany() {
			return many;
		}
		
		@Override
		@RosettaAttribute("some")
		@Accessor(AccessorType.GETTER)
		@Required
		@Multi
		@RuneAttribute("some")
		public List<Integer> getSome() {
			return some;
		}
		
		@Override
		@RosettaAttribute("pct")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("pct")
		public BigDecimal getPct() {
			return pct;
		}
		
		@Override
		@RosettaAttribute("pcts")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("pcts")
		public List<BigDecimal> getPcts() {
			return pcts;
		}
		
		@Override
		@RosettaAttribute("code")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("code")
		public String getCode() {
			return code;
		}
		
		@Override
		@RosettaAttribute("flag")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("flag")
		public Boolean getFlag() {
			return flag;
		}
		
		@Override
		@RosettaAttribute("nested")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("nested")
		public Integer getNested() {
			return nested;
		}
		
		@Override
		@RosettaAttribute("nesteds")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("nesteds")
		public List<Integer> getNesteds() {
			return nesteds;
		}
		
		@Override
		@RosettaAttribute("unnamed")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("unnamed")
		public Integer getUnnamed() {
			return unnamed;
		}
		
		@Override
		@RosettaAttribute("checked")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("checked")
		public BigDecimal getChecked() {
			return checked;
		}
		
		@Override
		@RosettaAttribute("plain")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("plain")
		public BigDecimal getPlain() {
			return plain;
		}
		
		@RosettaAttribute("one")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("one")
		@Override
		public Holder.HolderBuilder setOne(Integer _one) {
			this.one = _one == null ? null : _one;
			return this;
		}
		
		@RosettaAttribute("opt")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("opt")
		@Override
		public Holder.HolderBuilder setOpt(Integer _opt) {
			this.opt = _opt == null ? null : _opt;
			return this;
		}
		
		@RosettaAttribute("many")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("many")
		@Override
		public Holder.HolderBuilder addMany(Integer _many) {
			if (_many != null) {
				this.many.add(_many);
			}
			return this;
		}
		
		@Override
		public Holder.HolderBuilder addMany(Integer _many, int idx) {
			getIndex(this.many, idx, () -> _many);
			return this;
		}
		
		@Override
		public Holder.HolderBuilder addMany(List<Integer> manys) {
			if (manys != null) {
				for (final Integer toAdd : manys) {
					this.many.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("many")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("many")
		@Override
		public Holder.HolderBuilder setMany(List<Integer> manys) {
			if (manys == null) {
				this.many = new ArrayList<>();
			} else {
				this.many = manys.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("some")
		@Accessor(AccessorType.ADDER)
		@Required
		@Multi
		@RuneAttribute("some")
		@Override
		public Holder.HolderBuilder addSome(Integer _some) {
			if (_some != null) {
				this.some.add(_some);
			}
			return this;
		}
		
		@Override
		public Holder.HolderBuilder addSome(Integer _some, int idx) {
			getIndex(this.some, idx, () -> _some);
			return this;
		}
		
		@Override
		public Holder.HolderBuilder addSome(List<Integer> somes) {
			if (somes != null) {
				for (final Integer toAdd : somes) {
					this.some.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("some")
		@Accessor(AccessorType.SETTER)
		@Required
		@Multi
		@RuneAttribute("some")
		@Override
		public Holder.HolderBuilder setSome(List<Integer> somes) {
			if (somes == null) {
				this.some = new ArrayList<>();
			} else {
				this.some = somes.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("pct")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("pct")
		@Override
		public Holder.HolderBuilder setPct(BigDecimal _pct) {
			this.pct = _pct == null ? null : _pct;
			return this;
		}
		
		@RosettaAttribute("pcts")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("pcts")
		@Override
		public Holder.HolderBuilder addPcts(BigDecimal _pcts) {
			if (_pcts != null) {
				this.pcts.add(_pcts);
			}
			return this;
		}
		
		@Override
		public Holder.HolderBuilder addPcts(BigDecimal _pcts, int idx) {
			getIndex(this.pcts, idx, () -> _pcts);
			return this;
		}
		
		@Override
		public Holder.HolderBuilder addPcts(List<BigDecimal> pctss) {
			if (pctss != null) {
				for (final BigDecimal toAdd : pctss) {
					this.pcts.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("pcts")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("pcts")
		@Override
		public Holder.HolderBuilder setPcts(List<BigDecimal> pctss) {
			if (pctss == null) {
				this.pcts = new ArrayList<>();
			} else {
				this.pcts = pctss.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("code")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("code")
		@Override
		public Holder.HolderBuilder setCode(String _code) {
			this.code = _code == null ? null : _code;
			return this;
		}
		
		@RosettaAttribute("flag")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("flag")
		@Override
		public Holder.HolderBuilder setFlag(Boolean _flag) {
			this.flag = _flag == null ? null : _flag;
			return this;
		}
		
		@RosettaAttribute("nested")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("nested")
		@Override
		public Holder.HolderBuilder setNested(Integer _nested) {
			this.nested = _nested == null ? null : _nested;
			return this;
		}
		
		@RosettaAttribute("nesteds")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("nesteds")
		@Override
		public Holder.HolderBuilder addNesteds(Integer _nesteds) {
			if (_nesteds != null) {
				this.nesteds.add(_nesteds);
			}
			return this;
		}
		
		@Override
		public Holder.HolderBuilder addNesteds(Integer _nesteds, int idx) {
			getIndex(this.nesteds, idx, () -> _nesteds);
			return this;
		}
		
		@Override
		public Holder.HolderBuilder addNesteds(List<Integer> nestedss) {
			if (nestedss != null) {
				for (final Integer toAdd : nestedss) {
					this.nesteds.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("nesteds")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("nesteds")
		@Override
		public Holder.HolderBuilder setNesteds(List<Integer> nestedss) {
			if (nestedss == null) {
				this.nesteds = new ArrayList<>();
			} else {
				this.nesteds = nestedss.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("unnamed")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("unnamed")
		@Override
		public Holder.HolderBuilder setUnnamed(Integer _unnamed) {
			this.unnamed = _unnamed == null ? null : _unnamed;
			return this;
		}
		
		@RosettaAttribute("checked")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("checked")
		@Override
		public Holder.HolderBuilder setChecked(BigDecimal _checked) {
			this.checked = _checked == null ? null : _checked;
			return this;
		}
		
		@RosettaAttribute("plain")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("plain")
		@Override
		public Holder.HolderBuilder setPlain(BigDecimal _plain) {
			this.plain = _plain == null ? null : _plain;
			return this;
		}
		
		@Override
		public Holder build() {
			return new Holder.HolderImpl(this);
		}
		
		@Override
		public Holder.HolderBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Holder.HolderBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getOne()!=null) return true;
			if (getOpt()!=null) return true;
			if (getMany()!=null && !getMany().isEmpty()) return true;
			if (getSome()!=null && !getSome().isEmpty()) return true;
			if (getPct()!=null) return true;
			if (getPcts()!=null && !getPcts().isEmpty()) return true;
			if (getCode()!=null) return true;
			if (getFlag()!=null) return true;
			if (getNested()!=null) return true;
			if (getNesteds()!=null && !getNesteds().isEmpty()) return true;
			if (getUnnamed()!=null) return true;
			if (getChecked()!=null) return true;
			if (getPlain()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Holder.HolderBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Holder.HolderBuilder o = (Holder.HolderBuilder) other;
			
			
			merger.mergeBasic(getOne(), o.getOne(), this::setOne);
			merger.mergeBasic(getOpt(), o.getOpt(), this::setOpt);
			merger.mergeBasic(getMany(), o.getMany(), (Consumer<Integer>) this::addMany);
			merger.mergeBasic(getSome(), o.getSome(), (Consumer<Integer>) this::addSome);
			merger.mergeBasic(getPct(), o.getPct(), this::setPct);
			merger.mergeBasic(getPcts(), o.getPcts(), (Consumer<BigDecimal>) this::addPcts);
			merger.mergeBasic(getCode(), o.getCode(), this::setCode);
			merger.mergeBasic(getFlag(), o.getFlag(), this::setFlag);
			merger.mergeBasic(getNested(), o.getNested(), this::setNested);
			merger.mergeBasic(getNesteds(), o.getNesteds(), (Consumer<Integer>) this::addNesteds);
			merger.mergeBasic(getUnnamed(), o.getUnnamed(), this::setUnnamed);
			merger.mergeBasic(getChecked(), o.getChecked(), this::setChecked);
			merger.mergeBasic(getPlain(), o.getPlain(), this::setPlain);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Holder _that = getType().cast(o);
		
			if (!Objects.equals(one, _that.getOne())) return false;
			if (!Objects.equals(opt, _that.getOpt())) return false;
			if (!ListEquals.listEquals(many, _that.getMany())) return false;
			if (!ListEquals.listEquals(some, _that.getSome())) return false;
			if (!Objects.equals(pct, _that.getPct())) return false;
			if (!ListEquals.listEquals(pcts, _that.getPcts())) return false;
			if (!Objects.equals(code, _that.getCode())) return false;
			if (!Objects.equals(flag, _that.getFlag())) return false;
			if (!Objects.equals(nested, _that.getNested())) return false;
			if (!ListEquals.listEquals(nesteds, _that.getNesteds())) return false;
			if (!Objects.equals(unnamed, _that.getUnnamed())) return false;
			if (!Objects.equals(checked, _that.getChecked())) return false;
			if (!Objects.equals(plain, _that.getPlain())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (one != null ? one.hashCode() : 0);
			_result = 31 * _result + (opt != null ? opt.hashCode() : 0);
			_result = 31 * _result + (many != null ? many.hashCode() : 0);
			_result = 31 * _result + (some != null ? some.hashCode() : 0);
			_result = 31 * _result + (pct != null ? pct.hashCode() : 0);
			_result = 31 * _result + (pcts != null ? pcts.hashCode() : 0);
			_result = 31 * _result + (code != null ? code.hashCode() : 0);
			_result = 31 * _result + (flag != null ? flag.hashCode() : 0);
			_result = 31 * _result + (nested != null ? nested.hashCode() : 0);
			_result = 31 * _result + (nesteds != null ? nesteds.hashCode() : 0);
			_result = 31 * _result + (unnamed != null ? unnamed.hashCode() : 0);
			_result = 31 * _result + (checked != null ? checked.hashCode() : 0);
			_result = 31 * _result + (plain != null ? plain.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "HolderBuilder {" +
				"one=" + this.one + ", " +
				"opt=" + this.opt + ", " +
				"many=" + this.many + ", " +
				"some=" + this.some + ", " +
				"pct=" + this.pct + ", " +
				"pcts=" + this.pcts + ", " +
				"code=" + this.code + ", " +
				"flag=" + this.flag + ", " +
				"nested=" + this.nested + ", " +
				"nesteds=" + this.nesteds + ", " +
				"unnamed=" + this.unnamed + ", " +
				"checked=" + this.checked + ", " +
				"plain=" + this.plain +
			'}';
		}
	}
}
