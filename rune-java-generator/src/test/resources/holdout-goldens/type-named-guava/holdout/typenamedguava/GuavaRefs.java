package holdout.typenamedguava;

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
import holdout.typenamedguava.meta.GuavaRefsMeta;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * A sibling referencing every guava-named type singly, its own multi string last.
 * @version 0.0.0
 */
@RosettaDataType(value="GuavaRefs", builder=GuavaRefs.GuavaRefsBuilderImpl.class, version="0.0.0")
@RuneDataType(value="GuavaRefs", model="holdout", builder=GuavaRefs.GuavaRefsBuilderImpl.class, version="0.0.0")
public interface GuavaRefs extends RosettaModelObject {

	GuavaRefsMeta metaData = new GuavaRefsMeta();

	/*********************** Getter Methods  ***********************/
	ImmutableList getImmutableList();
	ImmutableMap getImmutableMap();
	Lists getLists();
	List<String> getNames();

	/*********************** Build Methods  ***********************/
	GuavaRefs build();
	
	GuavaRefs.GuavaRefsBuilder toBuilder();
	
	static GuavaRefs.GuavaRefsBuilder builder() {
		return new GuavaRefs.GuavaRefsBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends GuavaRefs> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends GuavaRefs> getType() {
		return GuavaRefs.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("immutableList"), processor, ImmutableList.class, getImmutableList());
		processRosetta(path.newSubPath("immutableMap"), processor, ImmutableMap.class, getImmutableMap());
		processRosetta(path.newSubPath("lists"), processor, Lists.class, getLists());
		processor.processBasic(path.newSubPath("names"), String.class, getNames(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface GuavaRefsBuilder extends GuavaRefs, RosettaModelObjectBuilder {
		ImmutableList.ImmutableListBuilder getOrCreateImmutableList();
		@Override
		ImmutableList.ImmutableListBuilder getImmutableList();
		ImmutableMap.ImmutableMapBuilder getOrCreateImmutableMap();
		@Override
		ImmutableMap.ImmutableMapBuilder getImmutableMap();
		Lists.ListsBuilder getOrCreateLists();
		@Override
		Lists.ListsBuilder getLists();
		GuavaRefs.GuavaRefsBuilder setImmutableList(ImmutableList immutableList);
		GuavaRefs.GuavaRefsBuilder setImmutableMap(ImmutableMap immutableMap);
		GuavaRefs.GuavaRefsBuilder setLists(Lists lists);
		GuavaRefs.GuavaRefsBuilder addNames(String names);
		GuavaRefs.GuavaRefsBuilder addNames(String names, int idx);
		GuavaRefs.GuavaRefsBuilder addNames(List<String> names);
		GuavaRefs.GuavaRefsBuilder setNames(List<String> names);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("immutableList"), processor, ImmutableList.ImmutableListBuilder.class, getImmutableList());
			processRosetta(path.newSubPath("immutableMap"), processor, ImmutableMap.ImmutableMapBuilder.class, getImmutableMap());
			processRosetta(path.newSubPath("lists"), processor, Lists.ListsBuilder.class, getLists());
			processor.processBasic(path.newSubPath("names"), String.class, getNames(), this);
		}
		

		GuavaRefs.GuavaRefsBuilder prune();
	}

	/*********************** Immutable Implementation of GuavaRefs  ***********************/
	class GuavaRefsImpl implements GuavaRefs {
		private final ImmutableList immutableList;
		private final ImmutableMap immutableMap;
		private final Lists lists;
		private final List<String> names;
		
		protected GuavaRefsImpl(GuavaRefs.GuavaRefsBuilder builder) {
			this.immutableList = ofNullable(builder.getImmutableList()).map(f->f.build()).orElse(null);
			this.immutableMap = ofNullable(builder.getImmutableMap()).map(f->f.build()).orElse(null);
			this.lists = ofNullable(builder.getLists()).map(f->f.build()).orElse(null);
			this.names = ofNullable(builder.getNames()).filter(_l->!_l.isEmpty()).map(com.google.common.collect.ImmutableList::copyOf).orElse(null);
		}
		
		@Override
		@RosettaAttribute("immutableList")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("immutableList")
		public ImmutableList getImmutableList() {
			return immutableList;
		}
		
		@Override
		@RosettaAttribute("immutableMap")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("immutableMap")
		public ImmutableMap getImmutableMap() {
			return immutableMap;
		}
		
		@Override
		@RosettaAttribute("lists")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("lists")
		public Lists getLists() {
			return lists;
		}
		
		@Override
		@RosettaAttribute("names")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("names")
		public List<String> getNames() {
			return names;
		}
		
		@Override
		public GuavaRefs build() {
			return this;
		}
		
		@Override
		public GuavaRefs.GuavaRefsBuilder toBuilder() {
			GuavaRefs.GuavaRefsBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(GuavaRefs.GuavaRefsBuilder builder) {
			ofNullable(getImmutableList()).ifPresent(builder::setImmutableList);
			ofNullable(getImmutableMap()).ifPresent(builder::setImmutableMap);
			ofNullable(getLists()).ifPresent(builder::setLists);
			ofNullable(getNames()).ifPresent(builder::setNames);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			GuavaRefs _that = getType().cast(o);
		
			if (!Objects.equals(immutableList, _that.getImmutableList())) return false;
			if (!Objects.equals(immutableMap, _that.getImmutableMap())) return false;
			if (!Objects.equals(lists, _that.getLists())) return false;
			if (!ListEquals.listEquals(names, _that.getNames())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (immutableList != null ? immutableList.hashCode() : 0);
			_result = 31 * _result + (immutableMap != null ? immutableMap.hashCode() : 0);
			_result = 31 * _result + (lists != null ? lists.hashCode() : 0);
			_result = 31 * _result + (names != null ? names.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "GuavaRefs {" +
				"immutableList=" + this.immutableList + ", " +
				"immutableMap=" + this.immutableMap + ", " +
				"lists=" + this.lists + ", " +
				"names=" + this.names +
			'}';
		}
	}

	/*********************** Builder Implementation of GuavaRefs  ***********************/
	class GuavaRefsBuilderImpl implements GuavaRefs.GuavaRefsBuilder {
	
		protected ImmutableList.ImmutableListBuilder immutableList;
		protected ImmutableMap.ImmutableMapBuilder immutableMap;
		protected Lists.ListsBuilder lists;
		protected List<String> names = new ArrayList<>();
		
		@Override
		@RosettaAttribute("immutableList")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("immutableList")
		public ImmutableList.ImmutableListBuilder getImmutableList() {
			return immutableList;
		}
		
		@Override
		public ImmutableList.ImmutableListBuilder getOrCreateImmutableList() {
			ImmutableList.ImmutableListBuilder result;
			if (immutableList!=null) {
				result = immutableList;
			}
			else {
				result = immutableList = ImmutableList.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("immutableMap")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("immutableMap")
		public ImmutableMap.ImmutableMapBuilder getImmutableMap() {
			return immutableMap;
		}
		
		@Override
		public ImmutableMap.ImmutableMapBuilder getOrCreateImmutableMap() {
			ImmutableMap.ImmutableMapBuilder result;
			if (immutableMap!=null) {
				result = immutableMap;
			}
			else {
				result = immutableMap = ImmutableMap.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("lists")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("lists")
		public Lists.ListsBuilder getLists() {
			return lists;
		}
		
		@Override
		public Lists.ListsBuilder getOrCreateLists() {
			Lists.ListsBuilder result;
			if (lists!=null) {
				result = lists;
			}
			else {
				result = lists = Lists.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("names")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("names")
		public List<String> getNames() {
			return names;
		}
		
		@RosettaAttribute("immutableList")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("immutableList")
		@Override
		public GuavaRefs.GuavaRefsBuilder setImmutableList(ImmutableList _immutableList) {
			this.immutableList = _immutableList == null ? null : _immutableList.toBuilder();
			return this;
		}
		
		@RosettaAttribute("immutableMap")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("immutableMap")
		@Override
		public GuavaRefs.GuavaRefsBuilder setImmutableMap(ImmutableMap _immutableMap) {
			this.immutableMap = _immutableMap == null ? null : _immutableMap.toBuilder();
			return this;
		}
		
		@RosettaAttribute("lists")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("lists")
		@Override
		public GuavaRefs.GuavaRefsBuilder setLists(Lists _lists) {
			this.lists = _lists == null ? null : _lists.toBuilder();
			return this;
		}
		
		@RosettaAttribute("names")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("names")
		@Override
		public GuavaRefs.GuavaRefsBuilder addNames(String _names) {
			if (_names != null) {
				this.names.add(_names);
			}
			return this;
		}
		
		@Override
		public GuavaRefs.GuavaRefsBuilder addNames(String _names, int idx) {
			getIndex(this.names, idx, () -> _names);
			return this;
		}
		
		@Override
		public GuavaRefs.GuavaRefsBuilder addNames(List<String> namess) {
			if (namess != null) {
				for (final String toAdd : namess) {
					this.names.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("names")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("names")
		@Override
		public GuavaRefs.GuavaRefsBuilder setNames(List<String> namess) {
			if (namess == null) {
				this.names = new ArrayList<>();
			} else {
				this.names = namess.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public GuavaRefs build() {
			return new GuavaRefs.GuavaRefsImpl(this);
		}
		
		@Override
		public GuavaRefs.GuavaRefsBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public GuavaRefs.GuavaRefsBuilder prune() {
			if (immutableList!=null && !immutableList.prune().hasData()) immutableList = null;
			if (immutableMap!=null && !immutableMap.prune().hasData()) immutableMap = null;
			if (lists!=null && !lists.prune().hasData()) lists = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getImmutableList()!=null && getImmutableList().hasData()) return true;
			if (getImmutableMap()!=null && getImmutableMap().hasData()) return true;
			if (getLists()!=null && getLists().hasData()) return true;
			if (getNames()!=null && !getNames().isEmpty()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public GuavaRefs.GuavaRefsBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			GuavaRefs.GuavaRefsBuilder o = (GuavaRefs.GuavaRefsBuilder) other;
			
			merger.mergeRosetta(getImmutableList(), o.getImmutableList(), this::setImmutableList);
			merger.mergeRosetta(getImmutableMap(), o.getImmutableMap(), this::setImmutableMap);
			merger.mergeRosetta(getLists(), o.getLists(), this::setLists);
			
			merger.mergeBasic(getNames(), o.getNames(), (Consumer<String>) this::addNames);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			GuavaRefs _that = getType().cast(o);
		
			if (!Objects.equals(immutableList, _that.getImmutableList())) return false;
			if (!Objects.equals(immutableMap, _that.getImmutableMap())) return false;
			if (!Objects.equals(lists, _that.getLists())) return false;
			if (!ListEquals.listEquals(names, _that.getNames())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (immutableList != null ? immutableList.hashCode() : 0);
			_result = 31 * _result + (immutableMap != null ? immutableMap.hashCode() : 0);
			_result = 31 * _result + (lists != null ? lists.hashCode() : 0);
			_result = 31 * _result + (names != null ? names.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "GuavaRefsBuilder {" +
				"immutableList=" + this.immutableList + ", " +
				"immutableMap=" + this.immutableMap + ", " +
				"lists=" + this.lists + ", " +
				"names=" + this.names +
			'}';
		}
	}
}
