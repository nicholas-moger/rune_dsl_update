package chaos.s27.a2alias;

import chaos.s27.a2alias.h.C27Ref;
import chaos.s27.a2alias.meta.C27HolderMeta;
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

import static java.util.Optional.ofNullable;

/**
 * Multi and single attributes of the boilerplate-named-condition aliases, plus attribute locals named i / o / results beside a multi conditioned alias (the scope&#39;s numbering law).
 * @version 1.0.0
 */
@RosettaDataType(value="C27Holder", builder=C27Holder.C27HolderBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C27Holder", model="chaos", builder=C27Holder.C27HolderBuilderImpl.class, version="1.0.0")
public interface C27Holder extends RosettaModelObject {

	C27HolderMeta metaData = new C27HolderMeta();

	/*********************** Getter Methods  ***********************/
	List<Integer> getL();
	Integer getVr();
	List<Integer> getAr();
	Integer getH();
	List<Integer> getInj();
	List<Integer> getI();
	List<Integer> getO();
	List<Integer> getResults();
	C27Ref getRef();

	/*********************** Build Methods  ***********************/
	C27Holder build();
	
	C27Holder.C27HolderBuilder toBuilder();
	
	static C27Holder.C27HolderBuilder builder() {
		return new C27Holder.C27HolderBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C27Holder> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C27Holder> getType() {
		return C27Holder.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("l"), Integer.class, getL(), this);
		processor.processBasic(path.newSubPath("vr"), Integer.class, getVr(), this);
		processor.processBasic(path.newSubPath("ar"), Integer.class, getAr(), this);
		processor.processBasic(path.newSubPath("h"), Integer.class, getH(), this);
		processor.processBasic(path.newSubPath("inj"), Integer.class, getInj(), this);
		processor.processBasic(path.newSubPath("i"), Integer.class, getI(), this);
		processor.processBasic(path.newSubPath("o"), Integer.class, getO(), this);
		processor.processBasic(path.newSubPath("results"), Integer.class, getResults(), this);
		processRosetta(path.newSubPath("ref"), processor, C27Ref.class, getRef());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C27HolderBuilder extends C27Holder, RosettaModelObjectBuilder {
		C27Ref.C27RefBuilder getOrCreateRef();
		@Override
		C27Ref.C27RefBuilder getRef();
		C27Holder.C27HolderBuilder addL(Integer l);
		C27Holder.C27HolderBuilder addL(Integer l, int idx);
		C27Holder.C27HolderBuilder addL(List<Integer> l);
		C27Holder.C27HolderBuilder setL(List<Integer> l);
		C27Holder.C27HolderBuilder setVr(Integer vr);
		C27Holder.C27HolderBuilder addAr(Integer ar);
		C27Holder.C27HolderBuilder addAr(Integer ar, int idx);
		C27Holder.C27HolderBuilder addAr(List<Integer> ar);
		C27Holder.C27HolderBuilder setAr(List<Integer> ar);
		C27Holder.C27HolderBuilder setH(Integer h);
		C27Holder.C27HolderBuilder addInj(Integer inj);
		C27Holder.C27HolderBuilder addInj(Integer inj, int idx);
		C27Holder.C27HolderBuilder addInj(List<Integer> inj);
		C27Holder.C27HolderBuilder setInj(List<Integer> inj);
		C27Holder.C27HolderBuilder addI(Integer i);
		C27Holder.C27HolderBuilder addI(Integer i, int idx);
		C27Holder.C27HolderBuilder addI(List<Integer> i);
		C27Holder.C27HolderBuilder setI(List<Integer> i);
		C27Holder.C27HolderBuilder addO(Integer o);
		C27Holder.C27HolderBuilder addO(Integer o, int idx);
		C27Holder.C27HolderBuilder addO(List<Integer> o);
		C27Holder.C27HolderBuilder setO(List<Integer> o);
		C27Holder.C27HolderBuilder addResults(Integer results);
		C27Holder.C27HolderBuilder addResults(Integer results, int idx);
		C27Holder.C27HolderBuilder addResults(List<Integer> results);
		C27Holder.C27HolderBuilder setResults(List<Integer> results);
		C27Holder.C27HolderBuilder setRef(C27Ref ref);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("l"), Integer.class, getL(), this);
			processor.processBasic(path.newSubPath("vr"), Integer.class, getVr(), this);
			processor.processBasic(path.newSubPath("ar"), Integer.class, getAr(), this);
			processor.processBasic(path.newSubPath("h"), Integer.class, getH(), this);
			processor.processBasic(path.newSubPath("inj"), Integer.class, getInj(), this);
			processor.processBasic(path.newSubPath("i"), Integer.class, getI(), this);
			processor.processBasic(path.newSubPath("o"), Integer.class, getO(), this);
			processor.processBasic(path.newSubPath("results"), Integer.class, getResults(), this);
			processRosetta(path.newSubPath("ref"), processor, C27Ref.C27RefBuilder.class, getRef());
		}
		

		C27Holder.C27HolderBuilder prune();
	}

	/*********************** Immutable Implementation of C27Holder  ***********************/
	class C27HolderImpl implements C27Holder {
		private final List<Integer> l;
		private final Integer vr;
		private final List<Integer> ar;
		private final Integer h;
		private final List<Integer> inj;
		private final List<Integer> i;
		private final List<Integer> o;
		private final List<Integer> results;
		private final C27Ref ref;
		
		protected C27HolderImpl(C27Holder.C27HolderBuilder builder) {
			this.l = ofNullable(builder.getL()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.vr = builder.getVr();
			this.ar = ofNullable(builder.getAr()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.h = builder.getH();
			this.inj = ofNullable(builder.getInj()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.i = ofNullable(builder.getI()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.o = ofNullable(builder.getO()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.results = ofNullable(builder.getResults()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.ref = ofNullable(builder.getRef()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("l")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("l")
		public List<Integer> getL() {
			return l;
		}
		
		@Override
		@RosettaAttribute("vr")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("vr")
		public Integer getVr() {
			return vr;
		}
		
		@Override
		@RosettaAttribute("ar")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("ar")
		public List<Integer> getAr() {
			return ar;
		}
		
		@Override
		@RosettaAttribute("h")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("h")
		public Integer getH() {
			return h;
		}
		
		@Override
		@RosettaAttribute("inj")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("inj")
		public List<Integer> getInj() {
			return inj;
		}
		
		@Override
		@RosettaAttribute("i")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("i")
		public List<Integer> getI() {
			return i;
		}
		
		@Override
		@RosettaAttribute("o")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("o")
		public List<Integer> getO() {
			return o;
		}
		
		@Override
		@RosettaAttribute("results")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("results")
		public List<Integer> getResults() {
			return results;
		}
		
		@Override
		@RosettaAttribute("ref")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("ref")
		public C27Ref getRef() {
			return ref;
		}
		
		@Override
		public C27Holder build() {
			return this;
		}
		
		@Override
		public C27Holder.C27HolderBuilder toBuilder() {
			C27Holder.C27HolderBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C27Holder.C27HolderBuilder builder) {
			ofNullable(getL()).ifPresent(builder::setL);
			ofNullable(getVr()).ifPresent(builder::setVr);
			ofNullable(getAr()).ifPresent(builder::setAr);
			ofNullable(getH()).ifPresent(builder::setH);
			ofNullable(getInj()).ifPresent(builder::setInj);
			ofNullable(getI()).ifPresent(builder::setI);
			ofNullable(getO()).ifPresent(builder::setO);
			ofNullable(getResults()).ifPresent(builder::setResults);
			ofNullable(getRef()).ifPresent(builder::setRef);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C27Holder _that = getType().cast(o);
		
			if (!ListEquals.listEquals(l, _that.getL())) return false;
			if (!Objects.equals(vr, _that.getVr())) return false;
			if (!ListEquals.listEquals(ar, _that.getAr())) return false;
			if (!Objects.equals(h, _that.getH())) return false;
			if (!ListEquals.listEquals(inj, _that.getInj())) return false;
			if (!ListEquals.listEquals(i, _that.getI())) return false;
			if (!ListEquals.listEquals(o, _that.getO())) return false;
			if (!ListEquals.listEquals(results, _that.getResults())) return false;
			if (!Objects.equals(ref, _that.getRef())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (l != null ? l.hashCode() : 0);
			_result = 31 * _result + (vr != null ? vr.hashCode() : 0);
			_result = 31 * _result + (ar != null ? ar.hashCode() : 0);
			_result = 31 * _result + (h != null ? h.hashCode() : 0);
			_result = 31 * _result + (inj != null ? inj.hashCode() : 0);
			_result = 31 * _result + (i != null ? i.hashCode() : 0);
			_result = 31 * _result + (o != null ? o.hashCode() : 0);
			_result = 31 * _result + (results != null ? results.hashCode() : 0);
			_result = 31 * _result + (ref != null ? ref.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C27Holder {" +
				"l=" + this.l + ", " +
				"vr=" + this.vr + ", " +
				"ar=" + this.ar + ", " +
				"h=" + this.h + ", " +
				"inj=" + this.inj + ", " +
				"i=" + this.i + ", " +
				"o=" + this.o + ", " +
				"results=" + this.results + ", " +
				"ref=" + this.ref +
			'}';
		}
	}

	/*********************** Builder Implementation of C27Holder  ***********************/
	class C27HolderBuilderImpl implements C27Holder.C27HolderBuilder {
	
		protected List<Integer> l = new ArrayList<>();
		protected Integer vr;
		protected List<Integer> ar = new ArrayList<>();
		protected Integer h;
		protected List<Integer> inj = new ArrayList<>();
		protected List<Integer> i = new ArrayList<>();
		protected List<Integer> o = new ArrayList<>();
		protected List<Integer> results = new ArrayList<>();
		protected C27Ref.C27RefBuilder ref;
		
		@Override
		@RosettaAttribute("l")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("l")
		public List<Integer> getL() {
			return l;
		}
		
		@Override
		@RosettaAttribute("vr")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("vr")
		public Integer getVr() {
			return vr;
		}
		
		@Override
		@RosettaAttribute("ar")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("ar")
		public List<Integer> getAr() {
			return ar;
		}
		
		@Override
		@RosettaAttribute("h")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("h")
		public Integer getH() {
			return h;
		}
		
		@Override
		@RosettaAttribute("inj")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("inj")
		public List<Integer> getInj() {
			return inj;
		}
		
		@Override
		@RosettaAttribute("i")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("i")
		public List<Integer> getI() {
			return i;
		}
		
		@Override
		@RosettaAttribute("o")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("o")
		public List<Integer> getO() {
			return o;
		}
		
		@Override
		@RosettaAttribute("results")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("results")
		public List<Integer> getResults() {
			return results;
		}
		
		@Override
		@RosettaAttribute("ref")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("ref")
		public C27Ref.C27RefBuilder getRef() {
			return ref;
		}
		
		@Override
		public C27Ref.C27RefBuilder getOrCreateRef() {
			C27Ref.C27RefBuilder result;
			if (ref!=null) {
				result = ref;
			}
			else {
				result = ref = C27Ref.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("l")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("l")
		@Override
		public C27Holder.C27HolderBuilder addL(Integer _l) {
			if (_l != null) {
				this.l.add(_l);
			}
			return this;
		}
		
		@Override
		public C27Holder.C27HolderBuilder addL(Integer _l, int idx) {
			getIndex(this.l, idx, () -> _l);
			return this;
		}
		
		@Override
		public C27Holder.C27HolderBuilder addL(List<Integer> ls) {
			if (ls != null) {
				for (final Integer toAdd : ls) {
					this.l.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("l")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("l")
		@Override
		public C27Holder.C27HolderBuilder setL(List<Integer> ls) {
			if (ls == null) {
				this.l = new ArrayList<>();
			} else {
				this.l = ls.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("vr")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("vr")
		@Override
		public C27Holder.C27HolderBuilder setVr(Integer _vr) {
			this.vr = _vr == null ? null : _vr;
			return this;
		}
		
		@RosettaAttribute("ar")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("ar")
		@Override
		public C27Holder.C27HolderBuilder addAr(Integer _ar) {
			if (_ar != null) {
				this.ar.add(_ar);
			}
			return this;
		}
		
		@Override
		public C27Holder.C27HolderBuilder addAr(Integer _ar, int idx) {
			getIndex(this.ar, idx, () -> _ar);
			return this;
		}
		
		@Override
		public C27Holder.C27HolderBuilder addAr(List<Integer> ars) {
			if (ars != null) {
				for (final Integer toAdd : ars) {
					this.ar.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("ar")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("ar")
		@Override
		public C27Holder.C27HolderBuilder setAr(List<Integer> ars) {
			if (ars == null) {
				this.ar = new ArrayList<>();
			} else {
				this.ar = ars.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("h")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("h")
		@Override
		public C27Holder.C27HolderBuilder setH(Integer _h) {
			this.h = _h == null ? null : _h;
			return this;
		}
		
		@RosettaAttribute("inj")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("inj")
		@Override
		public C27Holder.C27HolderBuilder addInj(Integer _inj) {
			if (_inj != null) {
				this.inj.add(_inj);
			}
			return this;
		}
		
		@Override
		public C27Holder.C27HolderBuilder addInj(Integer _inj, int idx) {
			getIndex(this.inj, idx, () -> _inj);
			return this;
		}
		
		@Override
		public C27Holder.C27HolderBuilder addInj(List<Integer> injs) {
			if (injs != null) {
				for (final Integer toAdd : injs) {
					this.inj.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("inj")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("inj")
		@Override
		public C27Holder.C27HolderBuilder setInj(List<Integer> injs) {
			if (injs == null) {
				this.inj = new ArrayList<>();
			} else {
				this.inj = injs.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("i")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("i")
		@Override
		public C27Holder.C27HolderBuilder addI(Integer _i) {
			if (_i != null) {
				this.i.add(_i);
			}
			return this;
		}
		
		@Override
		public C27Holder.C27HolderBuilder addI(Integer _i, int idx) {
			getIndex(this.i, idx, () -> _i);
			return this;
		}
		
		@Override
		public C27Holder.C27HolderBuilder addI(List<Integer> is) {
			if (is != null) {
				for (final Integer toAdd : is) {
					this.i.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("i")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("i")
		@Override
		public C27Holder.C27HolderBuilder setI(List<Integer> is) {
			if (is == null) {
				this.i = new ArrayList<>();
			} else {
				this.i = is.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("o")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("o")
		@Override
		public C27Holder.C27HolderBuilder addO(Integer _o) {
			if (_o != null) {
				this.o.add(_o);
			}
			return this;
		}
		
		@Override
		public C27Holder.C27HolderBuilder addO(Integer _o, int idx) {
			getIndex(this.o, idx, () -> _o);
			return this;
		}
		
		@Override
		public C27Holder.C27HolderBuilder addO(List<Integer> os) {
			if (os != null) {
				for (final Integer toAdd : os) {
					this.o.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("o")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("o")
		@Override
		public C27Holder.C27HolderBuilder setO(List<Integer> os) {
			if (os == null) {
				this.o = new ArrayList<>();
			} else {
				this.o = os.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("results")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("results")
		@Override
		public C27Holder.C27HolderBuilder addResults(Integer _results) {
			if (_results != null) {
				this.results.add(_results);
			}
			return this;
		}
		
		@Override
		public C27Holder.C27HolderBuilder addResults(Integer _results, int idx) {
			getIndex(this.results, idx, () -> _results);
			return this;
		}
		
		@Override
		public C27Holder.C27HolderBuilder addResults(List<Integer> resultss) {
			if (resultss != null) {
				for (final Integer toAdd : resultss) {
					this.results.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("results")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("results")
		@Override
		public C27Holder.C27HolderBuilder setResults(List<Integer> resultss) {
			if (resultss == null) {
				this.results = new ArrayList<>();
			} else {
				this.results = resultss.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("ref")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("ref")
		@Override
		public C27Holder.C27HolderBuilder setRef(C27Ref _ref) {
			this.ref = _ref == null ? null : _ref.toBuilder();
			return this;
		}
		
		@Override
		public C27Holder build() {
			return new C27Holder.C27HolderImpl(this);
		}
		
		@Override
		public C27Holder.C27HolderBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C27Holder.C27HolderBuilder prune() {
			if (ref!=null && !ref.prune().hasData()) ref = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getL()!=null && !getL().isEmpty()) return true;
			if (getVr()!=null) return true;
			if (getAr()!=null && !getAr().isEmpty()) return true;
			if (getH()!=null) return true;
			if (getInj()!=null && !getInj().isEmpty()) return true;
			if (getI()!=null && !getI().isEmpty()) return true;
			if (getO()!=null && !getO().isEmpty()) return true;
			if (getResults()!=null && !getResults().isEmpty()) return true;
			if (getRef()!=null && getRef().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C27Holder.C27HolderBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C27Holder.C27HolderBuilder o = (C27Holder.C27HolderBuilder) other;
			
			merger.mergeRosetta(getRef(), o.getRef(), this::setRef);
			
			merger.mergeBasic(getL(), o.getL(), (Consumer<Integer>) this::addL);
			merger.mergeBasic(getVr(), o.getVr(), this::setVr);
			merger.mergeBasic(getAr(), o.getAr(), (Consumer<Integer>) this::addAr);
			merger.mergeBasic(getH(), o.getH(), this::setH);
			merger.mergeBasic(getInj(), o.getInj(), (Consumer<Integer>) this::addInj);
			merger.mergeBasic(getI(), o.getI(), (Consumer<Integer>) this::addI);
			merger.mergeBasic(getO(), o.getO(), (Consumer<Integer>) this::addO);
			merger.mergeBasic(getResults(), o.getResults(), (Consumer<Integer>) this::addResults);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C27Holder _that = getType().cast(o);
		
			if (!ListEquals.listEquals(l, _that.getL())) return false;
			if (!Objects.equals(vr, _that.getVr())) return false;
			if (!ListEquals.listEquals(ar, _that.getAr())) return false;
			if (!Objects.equals(h, _that.getH())) return false;
			if (!ListEquals.listEquals(inj, _that.getInj())) return false;
			if (!ListEquals.listEquals(i, _that.getI())) return false;
			if (!ListEquals.listEquals(o, _that.getO())) return false;
			if (!ListEquals.listEquals(results, _that.getResults())) return false;
			if (!Objects.equals(ref, _that.getRef())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (l != null ? l.hashCode() : 0);
			_result = 31 * _result + (vr != null ? vr.hashCode() : 0);
			_result = 31 * _result + (ar != null ? ar.hashCode() : 0);
			_result = 31 * _result + (h != null ? h.hashCode() : 0);
			_result = 31 * _result + (inj != null ? inj.hashCode() : 0);
			_result = 31 * _result + (i != null ? i.hashCode() : 0);
			_result = 31 * _result + (o != null ? o.hashCode() : 0);
			_result = 31 * _result + (results != null ? results.hashCode() : 0);
			_result = 31 * _result + (ref != null ? ref.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C27HolderBuilder {" +
				"l=" + this.l + ", " +
				"vr=" + this.vr + ", " +
				"ar=" + this.ar + ", " +
				"h=" + this.h + ", " +
				"inj=" + this.inj + ", " +
				"i=" + this.i + ", " +
				"o=" + this.o + ", " +
				"results=" + this.results + ", " +
				"ref=" + this.ref +
			'}';
		}
	}
}
