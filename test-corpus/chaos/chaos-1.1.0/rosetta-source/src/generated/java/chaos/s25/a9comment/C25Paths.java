package chaos.s25.a9comment;

import chaos.s25.a9comment.meta.C25PathsMeta;
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
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * only-exists targets: a bare, a nested, a choice-typed and a multi attribute.
 * @version 1.0.0
 */
@RosettaDataType(value="C25Paths", builder=C25Paths.C25PathsBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C25Paths", model="chaos", builder=C25Paths.C25PathsBuilderImpl.class, version="1.0.0")
public interface C25Paths extends RosettaModelObject {

	C25PathsMeta metaData = new C25PathsMeta();

	/*********************** Getter Methods  ***********************/
	String getP();
	String getQ();
	C25Sub getSub();
	C25Pick getPick();
	List<? extends C25Sub> getSubs();

	/*********************** Build Methods  ***********************/
	C25Paths build();
	
	C25Paths.C25PathsBuilder toBuilder();
	
	static C25Paths.C25PathsBuilder builder() {
		return new C25Paths.C25PathsBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C25Paths> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C25Paths> getType() {
		return C25Paths.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("p"), String.class, getP(), this);
		processor.processBasic(path.newSubPath("q"), String.class, getQ(), this);
		processRosetta(path.newSubPath("sub"), processor, C25Sub.class, getSub());
		processRosetta(path.newSubPath("pick"), processor, C25Pick.class, getPick());
		processRosetta(path.newSubPath("subs"), processor, C25Sub.class, getSubs());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C25PathsBuilder extends C25Paths, RosettaModelObjectBuilder {
		C25Sub.C25SubBuilder getOrCreateSub();
		@Override
		C25Sub.C25SubBuilder getSub();
		C25Pick.C25PickBuilder getOrCreatePick();
		@Override
		C25Pick.C25PickBuilder getPick();
		C25Sub.C25SubBuilder getOrCreateSubs(int index);
		@Override
		List<? extends C25Sub.C25SubBuilder> getSubs();
		C25Paths.C25PathsBuilder setP(String p);
		C25Paths.C25PathsBuilder setQ(String q);
		C25Paths.C25PathsBuilder setSub(C25Sub sub);
		C25Paths.C25PathsBuilder setPick(C25Pick pick);
		C25Paths.C25PathsBuilder addSubs(C25Sub subs);
		C25Paths.C25PathsBuilder addSubs(C25Sub subs, int idx);
		C25Paths.C25PathsBuilder addSubs(List<? extends C25Sub> subs);
		C25Paths.C25PathsBuilder setSubs(List<? extends C25Sub> subs);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("p"), String.class, getP(), this);
			processor.processBasic(path.newSubPath("q"), String.class, getQ(), this);
			processRosetta(path.newSubPath("sub"), processor, C25Sub.C25SubBuilder.class, getSub());
			processRosetta(path.newSubPath("pick"), processor, C25Pick.C25PickBuilder.class, getPick());
			processRosetta(path.newSubPath("subs"), processor, C25Sub.C25SubBuilder.class, getSubs());
		}
		

		C25Paths.C25PathsBuilder prune();
	}

	/*********************** Immutable Implementation of C25Paths  ***********************/
	class C25PathsImpl implements C25Paths {
		private final String p;
		private final String q;
		private final C25Sub sub;
		private final C25Pick pick;
		private final List<? extends C25Sub> subs;
		
		protected C25PathsImpl(C25Paths.C25PathsBuilder builder) {
			this.p = builder.getP();
			this.q = builder.getQ();
			this.sub = ofNullable(builder.getSub()).map(f->f.build()).orElse(null);
			this.pick = ofNullable(builder.getPick()).map(f->f.build()).orElse(null);
			this.subs = ofNullable(builder.getSubs()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
		}
		
		@Override
		@RosettaAttribute("p")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("p")
		public String getP() {
			return p;
		}
		
		@Override
		@RosettaAttribute("q")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("q")
		public String getQ() {
			return q;
		}
		
		@Override
		@RosettaAttribute("sub")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("sub")
		public C25Sub getSub() {
			return sub;
		}
		
		@Override
		@RosettaAttribute("pick")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("pick")
		public C25Pick getPick() {
			return pick;
		}
		
		@Override
		@RosettaAttribute("subs")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("subs")
		public List<? extends C25Sub> getSubs() {
			return subs;
		}
		
		@Override
		public C25Paths build() {
			return this;
		}
		
		@Override
		public C25Paths.C25PathsBuilder toBuilder() {
			C25Paths.C25PathsBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C25Paths.C25PathsBuilder builder) {
			ofNullable(getP()).ifPresent(builder::setP);
			ofNullable(getQ()).ifPresent(builder::setQ);
			ofNullable(getSub()).ifPresent(builder::setSub);
			ofNullable(getPick()).ifPresent(builder::setPick);
			ofNullable(getSubs()).ifPresent(builder::setSubs);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C25Paths _that = getType().cast(o);
		
			if (!Objects.equals(p, _that.getP())) return false;
			if (!Objects.equals(q, _that.getQ())) return false;
			if (!Objects.equals(sub, _that.getSub())) return false;
			if (!Objects.equals(pick, _that.getPick())) return false;
			if (!ListEquals.listEquals(subs, _that.getSubs())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (p != null ? p.hashCode() : 0);
			_result = 31 * _result + (q != null ? q.hashCode() : 0);
			_result = 31 * _result + (sub != null ? sub.hashCode() : 0);
			_result = 31 * _result + (pick != null ? pick.hashCode() : 0);
			_result = 31 * _result + (subs != null ? subs.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C25Paths {" +
				"p=" + this.p + ", " +
				"q=" + this.q + ", " +
				"sub=" + this.sub + ", " +
				"pick=" + this.pick + ", " +
				"subs=" + this.subs +
			'}';
		}
	}

	/*********************** Builder Implementation of C25Paths  ***********************/
	class C25PathsBuilderImpl implements C25Paths.C25PathsBuilder {
	
		protected String p;
		protected String q;
		protected C25Sub.C25SubBuilder sub;
		protected C25Pick.C25PickBuilder pick;
		protected List<C25Sub.C25SubBuilder> subs = new ArrayList<>();
		
		@Override
		@RosettaAttribute("p")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("p")
		public String getP() {
			return p;
		}
		
		@Override
		@RosettaAttribute("q")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("q")
		public String getQ() {
			return q;
		}
		
		@Override
		@RosettaAttribute("sub")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("sub")
		public C25Sub.C25SubBuilder getSub() {
			return sub;
		}
		
		@Override
		public C25Sub.C25SubBuilder getOrCreateSub() {
			C25Sub.C25SubBuilder result;
			if (sub!=null) {
				result = sub;
			}
			else {
				result = sub = C25Sub.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("pick")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("pick")
		public C25Pick.C25PickBuilder getPick() {
			return pick;
		}
		
		@Override
		public C25Pick.C25PickBuilder getOrCreatePick() {
			C25Pick.C25PickBuilder result;
			if (pick!=null) {
				result = pick;
			}
			else {
				result = pick = C25Pick.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("subs")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("subs")
		public List<? extends C25Sub.C25SubBuilder> getSubs() {
			return subs;
		}
		
		@Override
		public C25Sub.C25SubBuilder getOrCreateSubs(int index) {
			if (subs==null) {
				this.subs = new ArrayList<>();
			}
			return getIndex(subs, index, () -> {
						C25Sub.C25SubBuilder newSubs = C25Sub.builder();
						return newSubs;
					});
		}
		
		@RosettaAttribute("p")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("p")
		@Override
		public C25Paths.C25PathsBuilder setP(String _p) {
			this.p = _p == null ? null : _p;
			return this;
		}
		
		@RosettaAttribute("q")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("q")
		@Override
		public C25Paths.C25PathsBuilder setQ(String _q) {
			this.q = _q == null ? null : _q;
			return this;
		}
		
		@RosettaAttribute("sub")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("sub")
		@Override
		public C25Paths.C25PathsBuilder setSub(C25Sub _sub) {
			this.sub = _sub == null ? null : _sub.toBuilder();
			return this;
		}
		
		@RosettaAttribute("pick")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("pick")
		@Override
		public C25Paths.C25PathsBuilder setPick(C25Pick _pick) {
			this.pick = _pick == null ? null : _pick.toBuilder();
			return this;
		}
		
		@RosettaAttribute("subs")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("subs")
		@Override
		public C25Paths.C25PathsBuilder addSubs(C25Sub _subs) {
			if (_subs != null) {
				this.subs.add(_subs.toBuilder());
			}
			return this;
		}
		
		@Override
		public C25Paths.C25PathsBuilder addSubs(C25Sub _subs, int idx) {
			getIndex(this.subs, idx, () -> _subs.toBuilder());
			return this;
		}
		
		@Override
		public C25Paths.C25PathsBuilder addSubs(List<? extends C25Sub> subss) {
			if (subss != null) {
				for (final C25Sub toAdd : subss) {
					this.subs.add(toAdd.toBuilder());
				}
			}
			return this;
		}
		
		@RosettaAttribute("subs")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("subs")
		@Override
		public C25Paths.C25PathsBuilder setSubs(List<? extends C25Sub> subss) {
			if (subss == null) {
				this.subs = new ArrayList<>();
			} else {
				this.subs = subss.stream()
					.map(_a->_a.toBuilder())
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public C25Paths build() {
			return new C25Paths.C25PathsImpl(this);
		}
		
		@Override
		public C25Paths.C25PathsBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C25Paths.C25PathsBuilder prune() {
			if (sub!=null && !sub.prune().hasData()) sub = null;
			if (pick!=null && !pick.prune().hasData()) pick = null;
			subs = subs.stream().filter(b->b!=null).<C25Sub.C25SubBuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getP()!=null) return true;
			if (getQ()!=null) return true;
			if (getSub()!=null && getSub().hasData()) return true;
			if (getPick()!=null && getPick().hasData()) return true;
			if (getSubs()!=null && getSubs().stream().filter(Objects::nonNull).anyMatch(a->a.hasData())) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C25Paths.C25PathsBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C25Paths.C25PathsBuilder o = (C25Paths.C25PathsBuilder) other;
			
			merger.mergeRosetta(getSub(), o.getSub(), this::setSub);
			merger.mergeRosetta(getPick(), o.getPick(), this::setPick);
			merger.mergeRosetta(getSubs(), o.getSubs(), this::getOrCreateSubs);
			
			merger.mergeBasic(getP(), o.getP(), this::setP);
			merger.mergeBasic(getQ(), o.getQ(), this::setQ);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C25Paths _that = getType().cast(o);
		
			if (!Objects.equals(p, _that.getP())) return false;
			if (!Objects.equals(q, _that.getQ())) return false;
			if (!Objects.equals(sub, _that.getSub())) return false;
			if (!Objects.equals(pick, _that.getPick())) return false;
			if (!ListEquals.listEquals(subs, _that.getSubs())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (p != null ? p.hashCode() : 0);
			_result = 31 * _result + (q != null ? q.hashCode() : 0);
			_result = 31 * _result + (sub != null ? sub.hashCode() : 0);
			_result = 31 * _result + (pick != null ? pick.hashCode() : 0);
			_result = 31 * _result + (subs != null ? subs.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C25PathsBuilder {" +
				"p=" + this.p + ", " +
				"q=" + this.q + ", " +
				"sub=" + this.sub + ", " +
				"pick=" + this.pick + ", " +
				"subs=" + this.subs +
			'}';
		}
	}
}
