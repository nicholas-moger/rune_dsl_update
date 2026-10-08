package chaos.s05.a2wild;

import chaos.s05.a2wild.h.C5Sub;
import chaos.s05.a2wild.h.metafields.ReferenceWithMetaC5Sub;
import chaos.s05.a2wild.meta.C5ItemMeta;
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

import static java.util.Optional.ofNullable;

/**
 * The shape battery: single, optional, multi, list-of-list rung, and a meta-wrapped seat (charter 2.2b).
 * @version 1.0.0
 */
@RosettaDataType(value="C5Item", builder=C5Item.C5ItemBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C5Item", model="chaos", builder=C5Item.C5ItemBuilderImpl.class, version="1.0.0")
public interface C5Item extends RosettaModelObject {

	C5ItemMeta metaData = new C5ItemMeta();

	/*********************** Getter Methods  ***********************/
	String getOne();
	BigDecimal getOpt();
	List<BigDecimal> getMany();
	List<? extends C5Sub> getSub();
	ReferenceWithMetaC5Sub getSubRef();

	/*********************** Build Methods  ***********************/
	C5Item build();
	
	C5Item.C5ItemBuilder toBuilder();
	
	static C5Item.C5ItemBuilder builder() {
		return new C5Item.C5ItemBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C5Item> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C5Item> getType() {
		return C5Item.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("one"), String.class, getOne(), this);
		processor.processBasic(path.newSubPath("opt"), BigDecimal.class, getOpt(), this);
		processor.processBasic(path.newSubPath("many"), BigDecimal.class, getMany(), this);
		processRosetta(path.newSubPath("sub"), processor, C5Sub.class, getSub());
		processRosetta(path.newSubPath("subRef"), processor, ReferenceWithMetaC5Sub.class, getSubRef());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C5ItemBuilder extends C5Item, RosettaModelObjectBuilder {
		C5Sub.C5SubBuilder getOrCreateSub(int index);
		@Override
		List<? extends C5Sub.C5SubBuilder> getSub();
		ReferenceWithMetaC5Sub.ReferenceWithMetaC5SubBuilder getOrCreateSubRef();
		@Override
		ReferenceWithMetaC5Sub.ReferenceWithMetaC5SubBuilder getSubRef();
		C5Item.C5ItemBuilder setOne(String one);
		C5Item.C5ItemBuilder setOpt(BigDecimal opt);
		C5Item.C5ItemBuilder addMany(BigDecimal many);
		C5Item.C5ItemBuilder addMany(BigDecimal many, int idx);
		C5Item.C5ItemBuilder addMany(List<BigDecimal> many);
		C5Item.C5ItemBuilder setMany(List<BigDecimal> many);
		C5Item.C5ItemBuilder addSub(C5Sub sub);
		C5Item.C5ItemBuilder addSub(C5Sub sub, int idx);
		C5Item.C5ItemBuilder addSub(List<? extends C5Sub> sub);
		C5Item.C5ItemBuilder setSub(List<? extends C5Sub> sub);
		C5Item.C5ItemBuilder setSubRef(ReferenceWithMetaC5Sub subRef);
		C5Item.C5ItemBuilder setSubRefValue(C5Sub subRef);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("one"), String.class, getOne(), this);
			processor.processBasic(path.newSubPath("opt"), BigDecimal.class, getOpt(), this);
			processor.processBasic(path.newSubPath("many"), BigDecimal.class, getMany(), this);
			processRosetta(path.newSubPath("sub"), processor, C5Sub.C5SubBuilder.class, getSub());
			processRosetta(path.newSubPath("subRef"), processor, ReferenceWithMetaC5Sub.ReferenceWithMetaC5SubBuilder.class, getSubRef());
		}
		

		C5Item.C5ItemBuilder prune();
	}

	/*********************** Immutable Implementation of C5Item  ***********************/
	class C5ItemImpl implements C5Item {
		private final String one;
		private final BigDecimal opt;
		private final List<BigDecimal> many;
		private final List<? extends C5Sub> sub;
		private final ReferenceWithMetaC5Sub subRef;
		
		protected C5ItemImpl(C5Item.C5ItemBuilder builder) {
			this.one = builder.getOne();
			this.opt = builder.getOpt();
			this.many = ofNullable(builder.getMany()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.sub = ofNullable(builder.getSub()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
			this.subRef = ofNullable(builder.getSubRef()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("one")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("one")
		public String getOne() {
			return one;
		}
		
		@Override
		@RosettaAttribute("opt")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("opt")
		public BigDecimal getOpt() {
			return opt;
		}
		
		@Override
		@RosettaAttribute("many")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("many")
		public List<BigDecimal> getMany() {
			return many;
		}
		
		@Override
		@RosettaAttribute("sub")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("sub")
		public List<? extends C5Sub> getSub() {
			return sub;
		}
		
		@Override
		@RosettaAttribute("subRef")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("subRef")
		public ReferenceWithMetaC5Sub getSubRef() {
			return subRef;
		}
		
		@Override
		public C5Item build() {
			return this;
		}
		
		@Override
		public C5Item.C5ItemBuilder toBuilder() {
			C5Item.C5ItemBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C5Item.C5ItemBuilder builder) {
			ofNullable(getOne()).ifPresent(builder::setOne);
			ofNullable(getOpt()).ifPresent(builder::setOpt);
			ofNullable(getMany()).ifPresent(builder::setMany);
			ofNullable(getSub()).ifPresent(builder::setSub);
			ofNullable(getSubRef()).ifPresent(builder::setSubRef);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C5Item _that = getType().cast(o);
		
			if (!Objects.equals(one, _that.getOne())) return false;
			if (!Objects.equals(opt, _that.getOpt())) return false;
			if (!ListEquals.listEquals(many, _that.getMany())) return false;
			if (!ListEquals.listEquals(sub, _that.getSub())) return false;
			if (!Objects.equals(subRef, _that.getSubRef())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (one != null ? one.hashCode() : 0);
			_result = 31 * _result + (opt != null ? opt.hashCode() : 0);
			_result = 31 * _result + (many != null ? many.hashCode() : 0);
			_result = 31 * _result + (sub != null ? sub.hashCode() : 0);
			_result = 31 * _result + (subRef != null ? subRef.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C5Item {" +
				"one=" + this.one + ", " +
				"opt=" + this.opt + ", " +
				"many=" + this.many + ", " +
				"sub=" + this.sub + ", " +
				"subRef=" + this.subRef +
			'}';
		}
	}

	/*********************** Builder Implementation of C5Item  ***********************/
	class C5ItemBuilderImpl implements C5Item.C5ItemBuilder {
	
		protected String one;
		protected BigDecimal opt;
		protected List<BigDecimal> many = new ArrayList<>();
		protected List<C5Sub.C5SubBuilder> sub = new ArrayList<>();
		protected ReferenceWithMetaC5Sub.ReferenceWithMetaC5SubBuilder subRef;
		
		@Override
		@RosettaAttribute("one")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("one")
		public String getOne() {
			return one;
		}
		
		@Override
		@RosettaAttribute("opt")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("opt")
		public BigDecimal getOpt() {
			return opt;
		}
		
		@Override
		@RosettaAttribute("many")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("many")
		public List<BigDecimal> getMany() {
			return many;
		}
		
		@Override
		@RosettaAttribute("sub")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("sub")
		public List<? extends C5Sub.C5SubBuilder> getSub() {
			return sub;
		}
		
		@Override
		public C5Sub.C5SubBuilder getOrCreateSub(int index) {
			if (sub==null) {
				this.sub = new ArrayList<>();
			}
			return getIndex(sub, index, () -> {
						C5Sub.C5SubBuilder newSub = C5Sub.builder();
						return newSub;
					});
		}
		
		@Override
		@RosettaAttribute("subRef")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("subRef")
		public ReferenceWithMetaC5Sub.ReferenceWithMetaC5SubBuilder getSubRef() {
			return subRef;
		}
		
		@Override
		public ReferenceWithMetaC5Sub.ReferenceWithMetaC5SubBuilder getOrCreateSubRef() {
			ReferenceWithMetaC5Sub.ReferenceWithMetaC5SubBuilder result;
			if (subRef!=null) {
				result = subRef;
			}
			else {
				result = subRef = ReferenceWithMetaC5Sub.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("one")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("one")
		@Override
		public C5Item.C5ItemBuilder setOne(String _one) {
			this.one = _one == null ? null : _one;
			return this;
		}
		
		@RosettaAttribute("opt")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("opt")
		@Override
		public C5Item.C5ItemBuilder setOpt(BigDecimal _opt) {
			this.opt = _opt == null ? null : _opt;
			return this;
		}
		
		@RosettaAttribute("many")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("many")
		@Override
		public C5Item.C5ItemBuilder addMany(BigDecimal _many) {
			if (_many != null) {
				this.many.add(_many);
			}
			return this;
		}
		
		@Override
		public C5Item.C5ItemBuilder addMany(BigDecimal _many, int idx) {
			getIndex(this.many, idx, () -> _many);
			return this;
		}
		
		@Override
		public C5Item.C5ItemBuilder addMany(List<BigDecimal> manys) {
			if (manys != null) {
				for (final BigDecimal toAdd : manys) {
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
		public C5Item.C5ItemBuilder setMany(List<BigDecimal> manys) {
			if (manys == null) {
				this.many = new ArrayList<>();
			} else {
				this.many = manys.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("sub")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("sub")
		@Override
		public C5Item.C5ItemBuilder addSub(C5Sub _sub) {
			if (_sub != null) {
				this.sub.add(_sub.toBuilder());
			}
			return this;
		}
		
		@Override
		public C5Item.C5ItemBuilder addSub(C5Sub _sub, int idx) {
			getIndex(this.sub, idx, () -> _sub.toBuilder());
			return this;
		}
		
		@Override
		public C5Item.C5ItemBuilder addSub(List<? extends C5Sub> subs) {
			if (subs != null) {
				for (final C5Sub toAdd : subs) {
					this.sub.add(toAdd.toBuilder());
				}
			}
			return this;
		}
		
		@RosettaAttribute("sub")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("sub")
		@Override
		public C5Item.C5ItemBuilder setSub(List<? extends C5Sub> subs) {
			if (subs == null) {
				this.sub = new ArrayList<>();
			} else {
				this.sub = subs.stream()
					.map(_a->_a.toBuilder())
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("subRef")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("subRef")
		@Override
		public C5Item.C5ItemBuilder setSubRef(ReferenceWithMetaC5Sub _subRef) {
			this.subRef = _subRef == null ? null : _subRef.toBuilder();
			return this;
		}
		
		@Override
		public C5Item.C5ItemBuilder setSubRefValue(C5Sub _subRef) {
			this.getOrCreateSubRef().setValue(_subRef);
			return this;
		}
		
		@Override
		public C5Item build() {
			return new C5Item.C5ItemImpl(this);
		}
		
		@Override
		public C5Item.C5ItemBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C5Item.C5ItemBuilder prune() {
			sub = sub.stream().filter(b->b!=null).<C5Sub.C5SubBuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
			if (subRef!=null && !subRef.prune().hasData()) subRef = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getOne()!=null) return true;
			if (getOpt()!=null) return true;
			if (getMany()!=null && !getMany().isEmpty()) return true;
			if (getSub()!=null && getSub().stream().filter(Objects::nonNull).anyMatch(a->a.hasData())) return true;
			if (getSubRef()!=null && getSubRef().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C5Item.C5ItemBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C5Item.C5ItemBuilder o = (C5Item.C5ItemBuilder) other;
			
			merger.mergeRosetta(getSub(), o.getSub(), this::getOrCreateSub);
			merger.mergeRosetta(getSubRef(), o.getSubRef(), this::setSubRef);
			
			merger.mergeBasic(getOne(), o.getOne(), this::setOne);
			merger.mergeBasic(getOpt(), o.getOpt(), this::setOpt);
			merger.mergeBasic(getMany(), o.getMany(), (Consumer<BigDecimal>) this::addMany);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C5Item _that = getType().cast(o);
		
			if (!Objects.equals(one, _that.getOne())) return false;
			if (!Objects.equals(opt, _that.getOpt())) return false;
			if (!ListEquals.listEquals(many, _that.getMany())) return false;
			if (!ListEquals.listEquals(sub, _that.getSub())) return false;
			if (!Objects.equals(subRef, _that.getSubRef())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (one != null ? one.hashCode() : 0);
			_result = 31 * _result + (opt != null ? opt.hashCode() : 0);
			_result = 31 * _result + (many != null ? many.hashCode() : 0);
			_result = 31 * _result + (sub != null ? sub.hashCode() : 0);
			_result = 31 * _result + (subRef != null ? subRef.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C5ItemBuilder {" +
				"one=" + this.one + ", " +
				"opt=" + this.opt + ", " +
				"many=" + this.many + ", " +
				"sub=" + this.sub + ", " +
				"subRef=" + this.subRef +
			'}';
		}
	}
}
