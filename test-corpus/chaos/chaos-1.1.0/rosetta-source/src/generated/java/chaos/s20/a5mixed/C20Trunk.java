package chaos.s20.a5mixed;

import chaos.s20.a5mixed.meta.C20TrunkMeta;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * Depth 1.
 * @version 1.0.0
 */
@RosettaDataType(value="C20Trunk", builder=C20Trunk.C20TrunkBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C20Trunk", model="chaos", builder=C20Trunk.C20TrunkBuilderImpl.class, version="1.0.0")
public interface C20Trunk extends RosettaModelObject {

	C20TrunkMeta metaData = new C20TrunkMeta();

	/*********************** Getter Methods  ***********************/
	List<? extends C20Branch> getBranches();
	String getTitle();

	/*********************** Build Methods  ***********************/
	C20Trunk build();
	
	C20Trunk.C20TrunkBuilder toBuilder();
	
	static C20Trunk.C20TrunkBuilder builder() {
		return new C20Trunk.C20TrunkBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C20Trunk> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C20Trunk> getType() {
		return C20Trunk.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("branches"), processor, C20Branch.class, getBranches());
		processor.processBasic(path.newSubPath("title"), String.class, getTitle(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C20TrunkBuilder extends C20Trunk, RosettaModelObjectBuilder {
		C20Branch.C20BranchBuilder getOrCreateBranches(int index);
		@Override
		List<? extends C20Branch.C20BranchBuilder> getBranches();
		C20Trunk.C20TrunkBuilder addBranches(C20Branch branches);
		C20Trunk.C20TrunkBuilder addBranches(C20Branch branches, int idx);
		C20Trunk.C20TrunkBuilder addBranches(List<? extends C20Branch> branches);
		C20Trunk.C20TrunkBuilder setBranches(List<? extends C20Branch> branches);
		C20Trunk.C20TrunkBuilder setTitle(String title);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("branches"), processor, C20Branch.C20BranchBuilder.class, getBranches());
			processor.processBasic(path.newSubPath("title"), String.class, getTitle(), this);
		}
		

		C20Trunk.C20TrunkBuilder prune();
	}

	/*********************** Immutable Implementation of C20Trunk  ***********************/
	class C20TrunkImpl implements C20Trunk {
		private final List<? extends C20Branch> branches;
		private final String title;
		
		protected C20TrunkImpl(C20Trunk.C20TrunkBuilder builder) {
			this.branches = ofNullable(builder.getBranches()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
			this.title = builder.getTitle();
		}
		
		@Override
		@RosettaAttribute("branches")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("branches")
		public List<? extends C20Branch> getBranches() {
			return branches;
		}
		
		@Override
		@RosettaAttribute("title")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("title")
		public String getTitle() {
			return title;
		}
		
		@Override
		public C20Trunk build() {
			return this;
		}
		
		@Override
		public C20Trunk.C20TrunkBuilder toBuilder() {
			C20Trunk.C20TrunkBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C20Trunk.C20TrunkBuilder builder) {
			ofNullable(getBranches()).ifPresent(builder::setBranches);
			ofNullable(getTitle()).ifPresent(builder::setTitle);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C20Trunk _that = getType().cast(o);
		
			if (!ListEquals.listEquals(branches, _that.getBranches())) return false;
			if (!Objects.equals(title, _that.getTitle())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (branches != null ? branches.hashCode() : 0);
			_result = 31 * _result + (title != null ? title.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C20Trunk {" +
				"branches=" + this.branches + ", " +
				"title=" + this.title +
			'}';
		}
	}

	/*********************** Builder Implementation of C20Trunk  ***********************/
	class C20TrunkBuilderImpl implements C20Trunk.C20TrunkBuilder {
	
		protected List<C20Branch.C20BranchBuilder> branches = new ArrayList<>();
		protected String title;
		
		@Override
		@RosettaAttribute("branches")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("branches")
		public List<? extends C20Branch.C20BranchBuilder> getBranches() {
			return branches;
		}
		
		@Override
		public C20Branch.C20BranchBuilder getOrCreateBranches(int index) {
			if (branches==null) {
				this.branches = new ArrayList<>();
			}
			return getIndex(branches, index, () -> {
						C20Branch.C20BranchBuilder newBranches = C20Branch.builder();
						return newBranches;
					});
		}
		
		@Override
		@RosettaAttribute("title")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("title")
		public String getTitle() {
			return title;
		}
		
		@RosettaAttribute("branches")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("branches")
		@Override
		public C20Trunk.C20TrunkBuilder addBranches(C20Branch _branches) {
			if (_branches != null) {
				this.branches.add(_branches.toBuilder());
			}
			return this;
		}
		
		@Override
		public C20Trunk.C20TrunkBuilder addBranches(C20Branch _branches, int idx) {
			getIndex(this.branches, idx, () -> _branches.toBuilder());
			return this;
		}
		
		@Override
		public C20Trunk.C20TrunkBuilder addBranches(List<? extends C20Branch> branchess) {
			if (branchess != null) {
				for (final C20Branch toAdd : branchess) {
					this.branches.add(toAdd.toBuilder());
				}
			}
			return this;
		}
		
		@RosettaAttribute("branches")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("branches")
		@Override
		public C20Trunk.C20TrunkBuilder setBranches(List<? extends C20Branch> branchess) {
			if (branchess == null) {
				this.branches = new ArrayList<>();
			} else {
				this.branches = branchess.stream()
					.map(_a->_a.toBuilder())
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("title")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("title")
		@Override
		public C20Trunk.C20TrunkBuilder setTitle(String _title) {
			this.title = _title == null ? null : _title;
			return this;
		}
		
		@Override
		public C20Trunk build() {
			return new C20Trunk.C20TrunkImpl(this);
		}
		
		@Override
		public C20Trunk.C20TrunkBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C20Trunk.C20TrunkBuilder prune() {
			branches = branches.stream().filter(b->b!=null).<C20Branch.C20BranchBuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getBranches()!=null && getBranches().stream().filter(Objects::nonNull).anyMatch(a->a.hasData())) return true;
			if (getTitle()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C20Trunk.C20TrunkBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C20Trunk.C20TrunkBuilder o = (C20Trunk.C20TrunkBuilder) other;
			
			merger.mergeRosetta(getBranches(), o.getBranches(), this::getOrCreateBranches);
			
			merger.mergeBasic(getTitle(), o.getTitle(), this::setTitle);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C20Trunk _that = getType().cast(o);
		
			if (!ListEquals.listEquals(branches, _that.getBranches())) return false;
			if (!Objects.equals(title, _that.getTitle())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (branches != null ? branches.hashCode() : 0);
			_result = 31 * _result + (title != null ? title.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C20TrunkBuilder {" +
				"branches=" + this.branches + ", " +
				"title=" + this.title +
			'}';
		}
	}
}
