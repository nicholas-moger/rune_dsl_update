package chaos.s20.a2dangle;

import chaos.s20.a2dangle.h.C20Leaf;
import chaos.s20.a2dangle.meta.C20TwigMeta;
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
 * Depth 3.
 * @version 1.0.0
 */
@RosettaDataType(value="C20Twig", builder=C20Twig.C20TwigBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C20Twig", model="chaos", builder=C20Twig.C20TwigBuilderImpl.class, version="1.0.0")
public interface C20Twig extends RosettaModelObject {

	C20TwigMeta metaData = new C20TwigMeta();

	/*********************** Getter Methods  ***********************/
	List<? extends C20Leaf> getLeaves();

	/*********************** Build Methods  ***********************/
	C20Twig build();
	
	C20Twig.C20TwigBuilder toBuilder();
	
	static C20Twig.C20TwigBuilder builder() {
		return new C20Twig.C20TwigBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C20Twig> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C20Twig> getType() {
		return C20Twig.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("leaves"), processor, C20Leaf.class, getLeaves());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C20TwigBuilder extends C20Twig, RosettaModelObjectBuilder {
		C20Leaf.C20LeafBuilder getOrCreateLeaves(int index);
		@Override
		List<? extends C20Leaf.C20LeafBuilder> getLeaves();
		C20Twig.C20TwigBuilder addLeaves(C20Leaf leaves);
		C20Twig.C20TwigBuilder addLeaves(C20Leaf leaves, int idx);
		C20Twig.C20TwigBuilder addLeaves(List<? extends C20Leaf> leaves);
		C20Twig.C20TwigBuilder setLeaves(List<? extends C20Leaf> leaves);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("leaves"), processor, C20Leaf.C20LeafBuilder.class, getLeaves());
		}
		

		C20Twig.C20TwigBuilder prune();
	}

	/*********************** Immutable Implementation of C20Twig  ***********************/
	class C20TwigImpl implements C20Twig {
		private final List<? extends C20Leaf> leaves;
		
		protected C20TwigImpl(C20Twig.C20TwigBuilder builder) {
			this.leaves = ofNullable(builder.getLeaves()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
		}
		
		@Override
		@RosettaAttribute("leaves")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("leaves")
		public List<? extends C20Leaf> getLeaves() {
			return leaves;
		}
		
		@Override
		public C20Twig build() {
			return this;
		}
		
		@Override
		public C20Twig.C20TwigBuilder toBuilder() {
			C20Twig.C20TwigBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C20Twig.C20TwigBuilder builder) {
			ofNullable(getLeaves()).ifPresent(builder::setLeaves);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C20Twig _that = getType().cast(o);
		
			if (!ListEquals.listEquals(leaves, _that.getLeaves())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (leaves != null ? leaves.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C20Twig {" +
				"leaves=" + this.leaves +
			'}';
		}
	}

	/*********************** Builder Implementation of C20Twig  ***********************/
	class C20TwigBuilderImpl implements C20Twig.C20TwigBuilder {
	
		protected List<C20Leaf.C20LeafBuilder> leaves = new ArrayList<>();
		
		@Override
		@RosettaAttribute("leaves")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("leaves")
		public List<? extends C20Leaf.C20LeafBuilder> getLeaves() {
			return leaves;
		}
		
		@Override
		public C20Leaf.C20LeafBuilder getOrCreateLeaves(int index) {
			if (leaves==null) {
				this.leaves = new ArrayList<>();
			}
			return getIndex(leaves, index, () -> {
						C20Leaf.C20LeafBuilder newLeaves = C20Leaf.builder();
						return newLeaves;
					});
		}
		
		@RosettaAttribute("leaves")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("leaves")
		@Override
		public C20Twig.C20TwigBuilder addLeaves(C20Leaf _leaves) {
			if (_leaves != null) {
				this.leaves.add(_leaves.toBuilder());
			}
			return this;
		}
		
		@Override
		public C20Twig.C20TwigBuilder addLeaves(C20Leaf _leaves, int idx) {
			getIndex(this.leaves, idx, () -> _leaves.toBuilder());
			return this;
		}
		
		@Override
		public C20Twig.C20TwigBuilder addLeaves(List<? extends C20Leaf> leavess) {
			if (leavess != null) {
				for (final C20Leaf toAdd : leavess) {
					this.leaves.add(toAdd.toBuilder());
				}
			}
			return this;
		}
		
		@RosettaAttribute("leaves")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("leaves")
		@Override
		public C20Twig.C20TwigBuilder setLeaves(List<? extends C20Leaf> leavess) {
			if (leavess == null) {
				this.leaves = new ArrayList<>();
			} else {
				this.leaves = leavess.stream()
					.map(_a->_a.toBuilder())
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public C20Twig build() {
			return new C20Twig.C20TwigImpl(this);
		}
		
		@Override
		public C20Twig.C20TwigBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C20Twig.C20TwigBuilder prune() {
			leaves = leaves.stream().filter(b->b!=null).<C20Leaf.C20LeafBuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getLeaves()!=null && getLeaves().stream().filter(Objects::nonNull).anyMatch(a->a.hasData())) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C20Twig.C20TwigBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C20Twig.C20TwigBuilder o = (C20Twig.C20TwigBuilder) other;
			
			merger.mergeRosetta(getLeaves(), o.getLeaves(), this::getOrCreateLeaves);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C20Twig _that = getType().cast(o);
		
			if (!ListEquals.listEquals(leaves, _that.getLeaves())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (leaves != null ? leaves.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C20TwigBuilder {" +
				"leaves=" + this.leaves +
			'}';
		}
	}
}
