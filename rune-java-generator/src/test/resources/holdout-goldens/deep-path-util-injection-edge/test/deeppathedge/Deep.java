package test.deeppathedge;

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
import test.deeppathedge.meta.DeepMeta;

import static java.util.Optional.ofNullable;

/**
 * The chaos C3Deep shape with a LAMBDA deep call in a type condition (the datarule path).
 * @version 0.0.0
 */
@RosettaDataType(value="Deep", builder=Deep.DeepBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Deep", model="test", builder=Deep.DeepBuilderImpl.class, version="0.0.0")
public interface Deep extends RosettaModelObject {

	DeepMeta metaData = new DeepMeta();

	/*********************** Getter Methods  ***********************/
	Outer getPick();
	List<? extends Inner> getPicks();

	/*********************** Build Methods  ***********************/
	Deep build();
	
	Deep.DeepBuilder toBuilder();
	
	static Deep.DeepBuilder builder() {
		return new Deep.DeepBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Deep> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Deep> getType() {
		return Deep.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("pick"), processor, Outer.class, getPick());
		processRosetta(path.newSubPath("picks"), processor, Inner.class, getPicks());
	}
	

	/*********************** Builder Interface  ***********************/
	interface DeepBuilder extends Deep, RosettaModelObjectBuilder {
		Outer.OuterBuilder getOrCreatePick();
		@Override
		Outer.OuterBuilder getPick();
		Inner.InnerBuilder getOrCreatePicks(int index);
		@Override
		List<? extends Inner.InnerBuilder> getPicks();
		Deep.DeepBuilder setPick(Outer pick);
		Deep.DeepBuilder addPicks(Inner picks);
		Deep.DeepBuilder addPicks(Inner picks, int idx);
		Deep.DeepBuilder addPicks(List<? extends Inner> picks);
		Deep.DeepBuilder setPicks(List<? extends Inner> picks);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("pick"), processor, Outer.OuterBuilder.class, getPick());
			processRosetta(path.newSubPath("picks"), processor, Inner.InnerBuilder.class, getPicks());
		}
		

		Deep.DeepBuilder prune();
	}

	/*********************** Immutable Implementation of Deep  ***********************/
	class DeepImpl implements Deep {
		private final Outer pick;
		private final List<? extends Inner> picks;
		
		protected DeepImpl(Deep.DeepBuilder builder) {
			this.pick = ofNullable(builder.getPick()).map(f->f.build()).orElse(null);
			this.picks = ofNullable(builder.getPicks()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
		}
		
		@Override
		@RosettaAttribute("pick")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("pick")
		public Outer getPick() {
			return pick;
		}
		
		@Override
		@RosettaAttribute("picks")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("picks")
		public List<? extends Inner> getPicks() {
			return picks;
		}
		
		@Override
		public Deep build() {
			return this;
		}
		
		@Override
		public Deep.DeepBuilder toBuilder() {
			Deep.DeepBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Deep.DeepBuilder builder) {
			ofNullable(getPick()).ifPresent(builder::setPick);
			ofNullable(getPicks()).ifPresent(builder::setPicks);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Deep _that = getType().cast(o);
		
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
			return "Deep {" +
				"pick=" + this.pick + ", " +
				"picks=" + this.picks +
			'}';
		}
	}

	/*********************** Builder Implementation of Deep  ***********************/
	class DeepBuilderImpl implements Deep.DeepBuilder {
	
		protected Outer.OuterBuilder pick;
		protected List<Inner.InnerBuilder> picks = new ArrayList<>();
		
		@Override
		@RosettaAttribute("pick")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("pick")
		public Outer.OuterBuilder getPick() {
			return pick;
		}
		
		@Override
		public Outer.OuterBuilder getOrCreatePick() {
			Outer.OuterBuilder result;
			if (pick!=null) {
				result = pick;
			}
			else {
				result = pick = Outer.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("picks")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("picks")
		public List<? extends Inner.InnerBuilder> getPicks() {
			return picks;
		}
		
		@Override
		public Inner.InnerBuilder getOrCreatePicks(int index) {
			if (picks==null) {
				this.picks = new ArrayList<>();
			}
			return getIndex(picks, index, () -> {
						Inner.InnerBuilder newPicks = Inner.builder();
						return newPicks;
					});
		}
		
		@RosettaAttribute("pick")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("pick")
		@Override
		public Deep.DeepBuilder setPick(Outer _pick) {
			this.pick = _pick == null ? null : _pick.toBuilder();
			return this;
		}
		
		@RosettaAttribute("picks")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("picks")
		@Override
		public Deep.DeepBuilder addPicks(Inner _picks) {
			if (_picks != null) {
				this.picks.add(_picks.toBuilder());
			}
			return this;
		}
		
		@Override
		public Deep.DeepBuilder addPicks(Inner _picks, int idx) {
			getIndex(this.picks, idx, () -> _picks.toBuilder());
			return this;
		}
		
		@Override
		public Deep.DeepBuilder addPicks(List<? extends Inner> pickss) {
			if (pickss != null) {
				for (final Inner toAdd : pickss) {
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
		public Deep.DeepBuilder setPicks(List<? extends Inner> pickss) {
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
		public Deep build() {
			return new Deep.DeepImpl(this);
		}
		
		@Override
		public Deep.DeepBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Deep.DeepBuilder prune() {
			if (pick!=null && !pick.prune().hasData()) pick = null;
			picks = picks.stream().filter(b->b!=null).<Inner.InnerBuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
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
		public Deep.DeepBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Deep.DeepBuilder o = (Deep.DeepBuilder) other;
			
			merger.mergeRosetta(getPick(), o.getPick(), this::setPick);
			merger.mergeRosetta(getPicks(), o.getPicks(), this::getOrCreatePicks);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Deep _that = getType().cast(o);
		
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
			return "DeepBuilder {" +
				"pick=" + this.pick + ", " +
				"picks=" + this.picks +
			'}';
		}
	}
}
