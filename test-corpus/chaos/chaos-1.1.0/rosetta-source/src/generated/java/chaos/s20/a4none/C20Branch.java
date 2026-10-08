package chaos.s20.a4none;

import chaos.s20.a4none.meta.C20BranchMeta;
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
 * Depth 2.
 * @version 0.0.0
 */
@RosettaDataType(value="C20Branch", builder=C20Branch.C20BranchBuilderImpl.class, version="0.0.0")
@RuneDataType(value="C20Branch", model="chaos", builder=C20Branch.C20BranchBuilderImpl.class, version="0.0.0")
public interface C20Branch extends RosettaModelObject {

	C20BranchMeta metaData = new C20BranchMeta();

	/*********************** Getter Methods  ***********************/
	List<? extends C20Twig> getTwigs();

	/*********************** Build Methods  ***********************/
	C20Branch build();
	
	C20Branch.C20BranchBuilder toBuilder();
	
	static C20Branch.C20BranchBuilder builder() {
		return new C20Branch.C20BranchBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C20Branch> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C20Branch> getType() {
		return C20Branch.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("twigs"), processor, C20Twig.class, getTwigs());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C20BranchBuilder extends C20Branch, RosettaModelObjectBuilder {
		C20Twig.C20TwigBuilder getOrCreateTwigs(int index);
		@Override
		List<? extends C20Twig.C20TwigBuilder> getTwigs();
		C20Branch.C20BranchBuilder addTwigs(C20Twig twigs);
		C20Branch.C20BranchBuilder addTwigs(C20Twig twigs, int idx);
		C20Branch.C20BranchBuilder addTwigs(List<? extends C20Twig> twigs);
		C20Branch.C20BranchBuilder setTwigs(List<? extends C20Twig> twigs);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("twigs"), processor, C20Twig.C20TwigBuilder.class, getTwigs());
		}
		

		C20Branch.C20BranchBuilder prune();
	}

	/*********************** Immutable Implementation of C20Branch  ***********************/
	class C20BranchImpl implements C20Branch {
		private final List<? extends C20Twig> twigs;
		
		protected C20BranchImpl(C20Branch.C20BranchBuilder builder) {
			this.twigs = ofNullable(builder.getTwigs()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
		}
		
		@Override
		@RosettaAttribute("twigs")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("twigs")
		public List<? extends C20Twig> getTwigs() {
			return twigs;
		}
		
		@Override
		public C20Branch build() {
			return this;
		}
		
		@Override
		public C20Branch.C20BranchBuilder toBuilder() {
			C20Branch.C20BranchBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C20Branch.C20BranchBuilder builder) {
			ofNullable(getTwigs()).ifPresent(builder::setTwigs);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C20Branch _that = getType().cast(o);
		
			if (!ListEquals.listEquals(twigs, _that.getTwigs())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (twigs != null ? twigs.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C20Branch {" +
				"twigs=" + this.twigs +
			'}';
		}
	}

	/*********************** Builder Implementation of C20Branch  ***********************/
	class C20BranchBuilderImpl implements C20Branch.C20BranchBuilder {
	
		protected List<C20Twig.C20TwigBuilder> twigs = new ArrayList<>();
		
		@Override
		@RosettaAttribute("twigs")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("twigs")
		public List<? extends C20Twig.C20TwigBuilder> getTwigs() {
			return twigs;
		}
		
		@Override
		public C20Twig.C20TwigBuilder getOrCreateTwigs(int index) {
			if (twigs==null) {
				this.twigs = new ArrayList<>();
			}
			return getIndex(twigs, index, () -> {
						C20Twig.C20TwigBuilder newTwigs = C20Twig.builder();
						return newTwigs;
					});
		}
		
		@RosettaAttribute("twigs")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("twigs")
		@Override
		public C20Branch.C20BranchBuilder addTwigs(C20Twig _twigs) {
			if (_twigs != null) {
				this.twigs.add(_twigs.toBuilder());
			}
			return this;
		}
		
		@Override
		public C20Branch.C20BranchBuilder addTwigs(C20Twig _twigs, int idx) {
			getIndex(this.twigs, idx, () -> _twigs.toBuilder());
			return this;
		}
		
		@Override
		public C20Branch.C20BranchBuilder addTwigs(List<? extends C20Twig> twigss) {
			if (twigss != null) {
				for (final C20Twig toAdd : twigss) {
					this.twigs.add(toAdd.toBuilder());
				}
			}
			return this;
		}
		
		@RosettaAttribute("twigs")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("twigs")
		@Override
		public C20Branch.C20BranchBuilder setTwigs(List<? extends C20Twig> twigss) {
			if (twigss == null) {
				this.twigs = new ArrayList<>();
			} else {
				this.twigs = twigss.stream()
					.map(_a->_a.toBuilder())
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public C20Branch build() {
			return new C20Branch.C20BranchImpl(this);
		}
		
		@Override
		public C20Branch.C20BranchBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C20Branch.C20BranchBuilder prune() {
			twigs = twigs.stream().filter(b->b!=null).<C20Twig.C20TwigBuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getTwigs()!=null && getTwigs().stream().filter(Objects::nonNull).anyMatch(a->a.hasData())) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C20Branch.C20BranchBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C20Branch.C20BranchBuilder o = (C20Branch.C20BranchBuilder) other;
			
			merger.mergeRosetta(getTwigs(), o.getTwigs(), this::getOrCreateTwigs);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C20Branch _that = getType().cast(o);
		
			if (!ListEquals.listEquals(twigs, _that.getTwigs())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (twigs != null ? twigs.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C20BranchBuilder {" +
				"twigs=" + this.twigs +
			'}';
		}
	}
}
