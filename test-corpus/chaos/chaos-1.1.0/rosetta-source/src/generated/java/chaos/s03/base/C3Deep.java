package chaos.s03.base;

import chaos.s03.base.meta.C3DeepMeta;
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
 * Navigates the common attribute across choice options.
 * @version 1.0.0
 */
@RosettaDataType(value="C3Deep", builder=C3Deep.C3DeepBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C3Deep", model="chaos", builder=C3Deep.C3DeepBuilderImpl.class, version="1.0.0")
public interface C3Deep extends RosettaModelObject {

	C3DeepMeta metaData = new C3DeepMeta();

	/*********************** Getter Methods  ***********************/
	C3Outer getPick();
	List<? extends C3Inner> getPicks();

	/*********************** Build Methods  ***********************/
	C3Deep build();
	
	C3Deep.C3DeepBuilder toBuilder();
	
	static C3Deep.C3DeepBuilder builder() {
		return new C3Deep.C3DeepBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C3Deep> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C3Deep> getType() {
		return C3Deep.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("pick"), processor, C3Outer.class, getPick());
		processRosetta(path.newSubPath("picks"), processor, C3Inner.class, getPicks());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C3DeepBuilder extends C3Deep, RosettaModelObjectBuilder {
		C3Outer.C3OuterBuilder getOrCreatePick();
		@Override
		C3Outer.C3OuterBuilder getPick();
		C3Inner.C3InnerBuilder getOrCreatePicks(int index);
		@Override
		List<? extends C3Inner.C3InnerBuilder> getPicks();
		C3Deep.C3DeepBuilder setPick(C3Outer pick);
		C3Deep.C3DeepBuilder addPicks(C3Inner picks);
		C3Deep.C3DeepBuilder addPicks(C3Inner picks, int idx);
		C3Deep.C3DeepBuilder addPicks(List<? extends C3Inner> picks);
		C3Deep.C3DeepBuilder setPicks(List<? extends C3Inner> picks);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("pick"), processor, C3Outer.C3OuterBuilder.class, getPick());
			processRosetta(path.newSubPath("picks"), processor, C3Inner.C3InnerBuilder.class, getPicks());
		}
		

		C3Deep.C3DeepBuilder prune();
	}

	/*********************** Immutable Implementation of C3Deep  ***********************/
	class C3DeepImpl implements C3Deep {
		private final C3Outer pick;
		private final List<? extends C3Inner> picks;
		
		protected C3DeepImpl(C3Deep.C3DeepBuilder builder) {
			this.pick = ofNullable(builder.getPick()).map(f->f.build()).orElse(null);
			this.picks = ofNullable(builder.getPicks()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
		}
		
		@Override
		@RosettaAttribute("pick")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("pick")
		public C3Outer getPick() {
			return pick;
		}
		
		@Override
		@RosettaAttribute("picks")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("picks")
		public List<? extends C3Inner> getPicks() {
			return picks;
		}
		
		@Override
		public C3Deep build() {
			return this;
		}
		
		@Override
		public C3Deep.C3DeepBuilder toBuilder() {
			C3Deep.C3DeepBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C3Deep.C3DeepBuilder builder) {
			ofNullable(getPick()).ifPresent(builder::setPick);
			ofNullable(getPicks()).ifPresent(builder::setPicks);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C3Deep _that = getType().cast(o);
		
			if (!Objects.equals(pick, _that.getPick())) return false;
			if (!ListEquals.listEquals(picks, _that.getPicks())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (pick != null ? pick.hashCode() : 0);
			_result = 31 * _result + (picks != null ? picks.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C3Deep {" +
				"pick=" + this.pick + ", " +
				"picks=" + this.picks +
			'}';
		}
	}

	/*********************** Builder Implementation of C3Deep  ***********************/
	class C3DeepBuilderImpl implements C3Deep.C3DeepBuilder {
	
		protected C3Outer.C3OuterBuilder pick;
		protected List<C3Inner.C3InnerBuilder> picks = new ArrayList<>();
		
		@Override
		@RosettaAttribute("pick")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("pick")
		public C3Outer.C3OuterBuilder getPick() {
			return pick;
		}
		
		@Override
		public C3Outer.C3OuterBuilder getOrCreatePick() {
			C3Outer.C3OuterBuilder result;
			if (pick!=null) {
				result = pick;
			}
			else {
				result = pick = C3Outer.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("picks")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("picks")
		public List<? extends C3Inner.C3InnerBuilder> getPicks() {
			return picks;
		}
		
		@Override
		public C3Inner.C3InnerBuilder getOrCreatePicks(int index) {
			if (picks==null) {
				this.picks = new ArrayList<>();
			}
			return getIndex(picks, index, () -> {
						C3Inner.C3InnerBuilder newPicks = C3Inner.builder();
						return newPicks;
					});
		}
		
		@RosettaAttribute("pick")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("pick")
		@Override
		public C3Deep.C3DeepBuilder setPick(C3Outer _pick) {
			this.pick = _pick == null ? null : _pick.toBuilder();
			return this;
		}
		
		@RosettaAttribute("picks")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("picks")
		@Override
		public C3Deep.C3DeepBuilder addPicks(C3Inner _picks) {
			if (_picks != null) {
				this.picks.add(_picks.toBuilder());
			}
			return this;
		}
		
		@Override
		public C3Deep.C3DeepBuilder addPicks(C3Inner _picks, int idx) {
			getIndex(this.picks, idx, () -> _picks.toBuilder());
			return this;
		}
		
		@Override
		public C3Deep.C3DeepBuilder addPicks(List<? extends C3Inner> pickss) {
			if (pickss != null) {
				for (final C3Inner toAdd : pickss) {
					this.picks.add(toAdd.toBuilder());
				}
			}
			return this;
		}
		
		@RosettaAttribute("picks")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("picks")
		@Override
		public C3Deep.C3DeepBuilder setPicks(List<? extends C3Inner> pickss) {
			if (pickss == null) {
				this.picks = new ArrayList<>();
			} else {
				this.picks = pickss.stream()
					.map(_a->_a.toBuilder())
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public C3Deep build() {
			return new C3Deep.C3DeepImpl(this);
		}
		
		@Override
		public C3Deep.C3DeepBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C3Deep.C3DeepBuilder prune() {
			if (pick!=null && !pick.prune().hasData()) pick = null;
			picks = picks.stream().filter(b->b!=null).<C3Inner.C3InnerBuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getPick()!=null && getPick().hasData()) return true;
			if (getPicks()!=null && getPicks().stream().filter(Objects::nonNull).anyMatch(a->a.hasData())) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C3Deep.C3DeepBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C3Deep.C3DeepBuilder o = (C3Deep.C3DeepBuilder) other;
			
			merger.mergeRosetta(getPick(), o.getPick(), this::setPick);
			merger.mergeRosetta(getPicks(), o.getPicks(), this::getOrCreatePicks);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C3Deep _that = getType().cast(o);
		
			if (!Objects.equals(pick, _that.getPick())) return false;
			if (!ListEquals.listEquals(picks, _that.getPicks())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (pick != null ? pick.hashCode() : 0);
			_result = 31 * _result + (picks != null ? picks.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C3DeepBuilder {" +
				"pick=" + this.pick + ", " +
				"picks=" + this.picks +
			'}';
		}
	}
}
