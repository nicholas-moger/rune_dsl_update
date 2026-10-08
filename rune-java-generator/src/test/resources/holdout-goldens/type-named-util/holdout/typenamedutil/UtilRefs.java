package holdout.typenamedutil;

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
import holdout.typenamedutil.meta.UtilRefsMeta;
import java.util.List;

import static java.util.Optional.ofNullable;

/**
 * A sibling referencing every util-named type singly, its own multi string last.
 * @version 0.0.0
 */
@RosettaDataType(value="UtilRefs", builder=UtilRefs.UtilRefsBuilderImpl.class, version="0.0.0")
@RuneDataType(value="UtilRefs", model="holdout", builder=UtilRefs.UtilRefsBuilderImpl.class, version="0.0.0")
public interface UtilRefs extends RosettaModelObject {

	UtilRefsMeta metaData = new UtilRefsMeta();

	/*********************** Getter Methods  ***********************/
	Map getMap();
	Set getTheSet();
	Objects getObjects();
	Collectors getCollectors();
	Arrays getArrays();
	Collections getCollections();
	Function getFn();
	Consumer getConsumer();
	ArrayList getArrayList();
	Pattern getPat();
	BigDecimal getBigDecimal();
	List<String> getNames();

	/*********************** Build Methods  ***********************/
	UtilRefs build();
	
	UtilRefs.UtilRefsBuilder toBuilder();
	
	static UtilRefs.UtilRefsBuilder builder() {
		return new UtilRefs.UtilRefsBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends UtilRefs> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends UtilRefs> getType() {
		return UtilRefs.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("map"), processor, Map.class, getMap());
		processRosetta(path.newSubPath("theSet"), processor, Set.class, getTheSet());
		processRosetta(path.newSubPath("objects"), processor, Objects.class, getObjects());
		processRosetta(path.newSubPath("collectors"), processor, Collectors.class, getCollectors());
		processRosetta(path.newSubPath("arrays"), processor, Arrays.class, getArrays());
		processRosetta(path.newSubPath("collections"), processor, Collections.class, getCollections());
		processRosetta(path.newSubPath("fn"), processor, Function.class, getFn());
		processRosetta(path.newSubPath("consumer"), processor, Consumer.class, getConsumer());
		processRosetta(path.newSubPath("arrayList"), processor, ArrayList.class, getArrayList());
		processRosetta(path.newSubPath("pat"), processor, Pattern.class, getPat());
		processRosetta(path.newSubPath("bigDecimal"), processor, BigDecimal.class, getBigDecimal());
		processor.processBasic(path.newSubPath("names"), String.class, getNames(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface UtilRefsBuilder extends UtilRefs, RosettaModelObjectBuilder {
		Map.MapBuilder getOrCreateMap();
		@Override
		Map.MapBuilder getMap();
		Set.SetBuilder getOrCreateTheSet();
		@Override
		Set.SetBuilder getTheSet();
		Objects.ObjectsBuilder getOrCreateObjects();
		@Override
		Objects.ObjectsBuilder getObjects();
		Collectors.CollectorsBuilder getOrCreateCollectors();
		@Override
		Collectors.CollectorsBuilder getCollectors();
		Arrays.ArraysBuilder getOrCreateArrays();
		@Override
		Arrays.ArraysBuilder getArrays();
		Collections.CollectionsBuilder getOrCreateCollections();
		@Override
		Collections.CollectionsBuilder getCollections();
		Function.FunctionBuilder getOrCreateFn();
		@Override
		Function.FunctionBuilder getFn();
		Consumer.ConsumerBuilder getOrCreateConsumer();
		@Override
		Consumer.ConsumerBuilder getConsumer();
		ArrayList.ArrayListBuilder getOrCreateArrayList();
		@Override
		ArrayList.ArrayListBuilder getArrayList();
		Pattern.PatternBuilder getOrCreatePat();
		@Override
		Pattern.PatternBuilder getPat();
		BigDecimal.BigDecimalBuilder getOrCreateBigDecimal();
		@Override
		BigDecimal.BigDecimalBuilder getBigDecimal();
		UtilRefs.UtilRefsBuilder setMap(Map map);
		UtilRefs.UtilRefsBuilder setTheSet(Set theSet);
		UtilRefs.UtilRefsBuilder setObjects(Objects objects);
		UtilRefs.UtilRefsBuilder setCollectors(Collectors collectors);
		UtilRefs.UtilRefsBuilder setArrays(Arrays arrays);
		UtilRefs.UtilRefsBuilder setCollections(Collections collections);
		UtilRefs.UtilRefsBuilder setFn(Function fn);
		UtilRefs.UtilRefsBuilder setConsumer(Consumer consumer);
		UtilRefs.UtilRefsBuilder setArrayList(ArrayList arrayList);
		UtilRefs.UtilRefsBuilder setPat(Pattern pat);
		UtilRefs.UtilRefsBuilder setBigDecimal(BigDecimal bigDecimal);
		UtilRefs.UtilRefsBuilder addNames(String names);
		UtilRefs.UtilRefsBuilder addNames(String names, int idx);
		UtilRefs.UtilRefsBuilder addNames(List<String> names);
		UtilRefs.UtilRefsBuilder setNames(List<String> names);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("map"), processor, Map.MapBuilder.class, getMap());
			processRosetta(path.newSubPath("theSet"), processor, Set.SetBuilder.class, getTheSet());
			processRosetta(path.newSubPath("objects"), processor, Objects.ObjectsBuilder.class, getObjects());
			processRosetta(path.newSubPath("collectors"), processor, Collectors.CollectorsBuilder.class, getCollectors());
			processRosetta(path.newSubPath("arrays"), processor, Arrays.ArraysBuilder.class, getArrays());
			processRosetta(path.newSubPath("collections"), processor, Collections.CollectionsBuilder.class, getCollections());
			processRosetta(path.newSubPath("fn"), processor, Function.FunctionBuilder.class, getFn());
			processRosetta(path.newSubPath("consumer"), processor, Consumer.ConsumerBuilder.class, getConsumer());
			processRosetta(path.newSubPath("arrayList"), processor, ArrayList.ArrayListBuilder.class, getArrayList());
			processRosetta(path.newSubPath("pat"), processor, Pattern.PatternBuilder.class, getPat());
			processRosetta(path.newSubPath("bigDecimal"), processor, BigDecimal.BigDecimalBuilder.class, getBigDecimal());
			processor.processBasic(path.newSubPath("names"), String.class, getNames(), this);
		}
		

		UtilRefs.UtilRefsBuilder prune();
	}

	/*********************** Immutable Implementation of UtilRefs  ***********************/
	class UtilRefsImpl implements UtilRefs {
		private final Map map;
		private final Set theSet;
		private final Objects objects;
		private final Collectors collectors;
		private final Arrays arrays;
		private final Collections collections;
		private final Function fn;
		private final Consumer consumer;
		private final ArrayList arrayList;
		private final Pattern pat;
		private final BigDecimal bigDecimal;
		private final List<String> names;
		
		protected UtilRefsImpl(UtilRefs.UtilRefsBuilder builder) {
			this.map = ofNullable(builder.getMap()).map(f->f.build()).orElse(null);
			this.theSet = ofNullable(builder.getTheSet()).map(f->f.build()).orElse(null);
			this.objects = ofNullable(builder.getObjects()).map(f->f.build()).orElse(null);
			this.collectors = ofNullable(builder.getCollectors()).map(f->f.build()).orElse(null);
			this.arrays = ofNullable(builder.getArrays()).map(f->f.build()).orElse(null);
			this.collections = ofNullable(builder.getCollections()).map(f->f.build()).orElse(null);
			this.fn = ofNullable(builder.getFn()).map(f->f.build()).orElse(null);
			this.consumer = ofNullable(builder.getConsumer()).map(f->f.build()).orElse(null);
			this.arrayList = ofNullable(builder.getArrayList()).map(f->f.build()).orElse(null);
			this.pat = ofNullable(builder.getPat()).map(f->f.build()).orElse(null);
			this.bigDecimal = ofNullable(builder.getBigDecimal()).map(f->f.build()).orElse(null);
			this.names = ofNullable(builder.getNames()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
		}
		
		@Override
		@RosettaAttribute("map")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("map")
		public Map getMap() {
			return map;
		}
		
		@Override
		@RosettaAttribute("theSet")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("theSet")
		public Set getTheSet() {
			return theSet;
		}
		
		@Override
		@RosettaAttribute("objects")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("objects")
		public Objects getObjects() {
			return objects;
		}
		
		@Override
		@RosettaAttribute("collectors")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("collectors")
		public Collectors getCollectors() {
			return collectors;
		}
		
		@Override
		@RosettaAttribute("arrays")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("arrays")
		public Arrays getArrays() {
			return arrays;
		}
		
		@Override
		@RosettaAttribute("collections")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("collections")
		public Collections getCollections() {
			return collections;
		}
		
		@Override
		@RosettaAttribute("fn")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("fn")
		public Function getFn() {
			return fn;
		}
		
		@Override
		@RosettaAttribute("consumer")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("consumer")
		public Consumer getConsumer() {
			return consumer;
		}
		
		@Override
		@RosettaAttribute("arrayList")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("arrayList")
		public ArrayList getArrayList() {
			return arrayList;
		}
		
		@Override
		@RosettaAttribute("pat")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("pat")
		public Pattern getPat() {
			return pat;
		}
		
		@Override
		@RosettaAttribute("bigDecimal")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("bigDecimal")
		public BigDecimal getBigDecimal() {
			return bigDecimal;
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
		public UtilRefs build() {
			return this;
		}
		
		@Override
		public UtilRefs.UtilRefsBuilder toBuilder() {
			UtilRefs.UtilRefsBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(UtilRefs.UtilRefsBuilder builder) {
			ofNullable(getMap()).ifPresent(builder::setMap);
			ofNullable(getTheSet()).ifPresent(builder::setTheSet);
			ofNullable(getObjects()).ifPresent(builder::setObjects);
			ofNullable(getCollectors()).ifPresent(builder::setCollectors);
			ofNullable(getArrays()).ifPresent(builder::setArrays);
			ofNullable(getCollections()).ifPresent(builder::setCollections);
			ofNullable(getFn()).ifPresent(builder::setFn);
			ofNullable(getConsumer()).ifPresent(builder::setConsumer);
			ofNullable(getArrayList()).ifPresent(builder::setArrayList);
			ofNullable(getPat()).ifPresent(builder::setPat);
			ofNullable(getBigDecimal()).ifPresent(builder::setBigDecimal);
			ofNullable(getNames()).ifPresent(builder::setNames);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			UtilRefs _that = getType().cast(o);
		
			if (!java.util.Objects.equals(map, _that.getMap())) return false;
			if (!java.util.Objects.equals(theSet, _that.getTheSet())) return false;
			if (!java.util.Objects.equals(objects, _that.getObjects())) return false;
			if (!java.util.Objects.equals(collectors, _that.getCollectors())) return false;
			if (!java.util.Objects.equals(arrays, _that.getArrays())) return false;
			if (!java.util.Objects.equals(collections, _that.getCollections())) return false;
			if (!java.util.Objects.equals(fn, _that.getFn())) return false;
			if (!java.util.Objects.equals(consumer, _that.getConsumer())) return false;
			if (!java.util.Objects.equals(arrayList, _that.getArrayList())) return false;
			if (!java.util.Objects.equals(pat, _that.getPat())) return false;
			if (!java.util.Objects.equals(bigDecimal, _that.getBigDecimal())) return false;
			if (!ListEquals.listEquals(names, _that.getNames())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (map != null ? map.hashCode() : 0);
			_result = 31 * _result + (theSet != null ? theSet.hashCode() : 0);
			_result = 31 * _result + (objects != null ? objects.hashCode() : 0);
			_result = 31 * _result + (collectors != null ? collectors.hashCode() : 0);
			_result = 31 * _result + (arrays != null ? arrays.hashCode() : 0);
			_result = 31 * _result + (collections != null ? collections.hashCode() : 0);
			_result = 31 * _result + (fn != null ? fn.hashCode() : 0);
			_result = 31 * _result + (consumer != null ? consumer.hashCode() : 0);
			_result = 31 * _result + (arrayList != null ? arrayList.hashCode() : 0);
			_result = 31 * _result + (pat != null ? pat.hashCode() : 0);
			_result = 31 * _result + (bigDecimal != null ? bigDecimal.hashCode() : 0);
			_result = 31 * _result + (names != null ? names.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "UtilRefs {" +
				"map=" + this.map + ", " +
				"theSet=" + this.theSet + ", " +
				"objects=" + this.objects + ", " +
				"collectors=" + this.collectors + ", " +
				"arrays=" + this.arrays + ", " +
				"collections=" + this.collections + ", " +
				"fn=" + this.fn + ", " +
				"consumer=" + this.consumer + ", " +
				"arrayList=" + this.arrayList + ", " +
				"pat=" + this.pat + ", " +
				"bigDecimal=" + this.bigDecimal + ", " +
				"names=" + this.names +
			'}';
		}
	}

	/*********************** Builder Implementation of UtilRefs  ***********************/
	class UtilRefsBuilderImpl implements UtilRefs.UtilRefsBuilder {
	
		protected Map.MapBuilder map;
		protected Set.SetBuilder theSet;
		protected Objects.ObjectsBuilder objects;
		protected Collectors.CollectorsBuilder collectors;
		protected Arrays.ArraysBuilder arrays;
		protected Collections.CollectionsBuilder collections;
		protected Function.FunctionBuilder fn;
		protected Consumer.ConsumerBuilder consumer;
		protected ArrayList.ArrayListBuilder arrayList;
		protected Pattern.PatternBuilder pat;
		protected BigDecimal.BigDecimalBuilder bigDecimal;
		protected List<String> names = new java.util.ArrayList<>();
		
		@Override
		@RosettaAttribute("map")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("map")
		public Map.MapBuilder getMap() {
			return map;
		}
		
		@Override
		public Map.MapBuilder getOrCreateMap() {
			Map.MapBuilder result;
			if (map!=null) {
				result = map;
			}
			else {
				result = map = Map.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("theSet")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("theSet")
		public Set.SetBuilder getTheSet() {
			return theSet;
		}
		
		@Override
		public Set.SetBuilder getOrCreateTheSet() {
			Set.SetBuilder result;
			if (theSet!=null) {
				result = theSet;
			}
			else {
				result = theSet = Set.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("objects")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("objects")
		public Objects.ObjectsBuilder getObjects() {
			return objects;
		}
		
		@Override
		public Objects.ObjectsBuilder getOrCreateObjects() {
			Objects.ObjectsBuilder result;
			if (objects!=null) {
				result = objects;
			}
			else {
				result = objects = Objects.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("collectors")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("collectors")
		public Collectors.CollectorsBuilder getCollectors() {
			return collectors;
		}
		
		@Override
		public Collectors.CollectorsBuilder getOrCreateCollectors() {
			Collectors.CollectorsBuilder result;
			if (collectors!=null) {
				result = collectors;
			}
			else {
				result = collectors = Collectors.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("arrays")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("arrays")
		public Arrays.ArraysBuilder getArrays() {
			return arrays;
		}
		
		@Override
		public Arrays.ArraysBuilder getOrCreateArrays() {
			Arrays.ArraysBuilder result;
			if (arrays!=null) {
				result = arrays;
			}
			else {
				result = arrays = Arrays.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("collections")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("collections")
		public Collections.CollectionsBuilder getCollections() {
			return collections;
		}
		
		@Override
		public Collections.CollectionsBuilder getOrCreateCollections() {
			Collections.CollectionsBuilder result;
			if (collections!=null) {
				result = collections;
			}
			else {
				result = collections = Collections.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("fn")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("fn")
		public Function.FunctionBuilder getFn() {
			return fn;
		}
		
		@Override
		public Function.FunctionBuilder getOrCreateFn() {
			Function.FunctionBuilder result;
			if (fn!=null) {
				result = fn;
			}
			else {
				result = fn = Function.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("consumer")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("consumer")
		public Consumer.ConsumerBuilder getConsumer() {
			return consumer;
		}
		
		@Override
		public Consumer.ConsumerBuilder getOrCreateConsumer() {
			Consumer.ConsumerBuilder result;
			if (consumer!=null) {
				result = consumer;
			}
			else {
				result = consumer = Consumer.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("arrayList")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("arrayList")
		public ArrayList.ArrayListBuilder getArrayList() {
			return arrayList;
		}
		
		@Override
		public ArrayList.ArrayListBuilder getOrCreateArrayList() {
			ArrayList.ArrayListBuilder result;
			if (arrayList!=null) {
				result = arrayList;
			}
			else {
				result = arrayList = ArrayList.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("pat")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("pat")
		public Pattern.PatternBuilder getPat() {
			return pat;
		}
		
		@Override
		public Pattern.PatternBuilder getOrCreatePat() {
			Pattern.PatternBuilder result;
			if (pat!=null) {
				result = pat;
			}
			else {
				result = pat = Pattern.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("bigDecimal")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("bigDecimal")
		public BigDecimal.BigDecimalBuilder getBigDecimal() {
			return bigDecimal;
		}
		
		@Override
		public BigDecimal.BigDecimalBuilder getOrCreateBigDecimal() {
			BigDecimal.BigDecimalBuilder result;
			if (bigDecimal!=null) {
				result = bigDecimal;
			}
			else {
				result = bigDecimal = BigDecimal.builder();
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
		
		@RosettaAttribute("map")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("map")
		@Override
		public UtilRefs.UtilRefsBuilder setMap(Map _map) {
			this.map = _map == null ? null : _map.toBuilder();
			return this;
		}
		
		@RosettaAttribute("theSet")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("theSet")
		@Override
		public UtilRefs.UtilRefsBuilder setTheSet(Set _theSet) {
			this.theSet = _theSet == null ? null : _theSet.toBuilder();
			return this;
		}
		
		@RosettaAttribute("objects")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("objects")
		@Override
		public UtilRefs.UtilRefsBuilder setObjects(Objects _objects) {
			this.objects = _objects == null ? null : _objects.toBuilder();
			return this;
		}
		
		@RosettaAttribute("collectors")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("collectors")
		@Override
		public UtilRefs.UtilRefsBuilder setCollectors(Collectors _collectors) {
			this.collectors = _collectors == null ? null : _collectors.toBuilder();
			return this;
		}
		
		@RosettaAttribute("arrays")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("arrays")
		@Override
		public UtilRefs.UtilRefsBuilder setArrays(Arrays _arrays) {
			this.arrays = _arrays == null ? null : _arrays.toBuilder();
			return this;
		}
		
		@RosettaAttribute("collections")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("collections")
		@Override
		public UtilRefs.UtilRefsBuilder setCollections(Collections _collections) {
			this.collections = _collections == null ? null : _collections.toBuilder();
			return this;
		}
		
		@RosettaAttribute("fn")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("fn")
		@Override
		public UtilRefs.UtilRefsBuilder setFn(Function _fn) {
			this.fn = _fn == null ? null : _fn.toBuilder();
			return this;
		}
		
		@RosettaAttribute("consumer")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("consumer")
		@Override
		public UtilRefs.UtilRefsBuilder setConsumer(Consumer _consumer) {
			this.consumer = _consumer == null ? null : _consumer.toBuilder();
			return this;
		}
		
		@RosettaAttribute("arrayList")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("arrayList")
		@Override
		public UtilRefs.UtilRefsBuilder setArrayList(ArrayList _arrayList) {
			this.arrayList = _arrayList == null ? null : _arrayList.toBuilder();
			return this;
		}
		
		@RosettaAttribute("pat")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("pat")
		@Override
		public UtilRefs.UtilRefsBuilder setPat(Pattern _pat) {
			this.pat = _pat == null ? null : _pat.toBuilder();
			return this;
		}
		
		@RosettaAttribute("bigDecimal")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("bigDecimal")
		@Override
		public UtilRefs.UtilRefsBuilder setBigDecimal(BigDecimal _bigDecimal) {
			this.bigDecimal = _bigDecimal == null ? null : _bigDecimal.toBuilder();
			return this;
		}
		
		@RosettaAttribute("names")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("names")
		@Override
		public UtilRefs.UtilRefsBuilder addNames(String _names) {
			if (_names != null) {
				this.names.add(_names);
			}
			return this;
		}
		
		@Override
		public UtilRefs.UtilRefsBuilder addNames(String _names, int idx) {
			getIndex(this.names, idx, () -> _names);
			return this;
		}
		
		@Override
		public UtilRefs.UtilRefsBuilder addNames(List<String> namess) {
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
		public UtilRefs.UtilRefsBuilder setNames(List<String> namess) {
			if (namess == null) {
				this.names = new java.util.ArrayList<>();
			} else {
				this.names = namess.stream()
					.collect(java.util.stream.Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public UtilRefs build() {
			return new UtilRefs.UtilRefsImpl(this);
		}
		
		@Override
		public UtilRefs.UtilRefsBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public UtilRefs.UtilRefsBuilder prune() {
			if (map!=null && !map.prune().hasData()) map = null;
			if (theSet!=null && !theSet.prune().hasData()) theSet = null;
			if (objects!=null && !objects.prune().hasData()) objects = null;
			if (collectors!=null && !collectors.prune().hasData()) collectors = null;
			if (arrays!=null && !arrays.prune().hasData()) arrays = null;
			if (collections!=null && !collections.prune().hasData()) collections = null;
			if (fn!=null && !fn.prune().hasData()) fn = null;
			if (consumer!=null && !consumer.prune().hasData()) consumer = null;
			if (arrayList!=null && !arrayList.prune().hasData()) arrayList = null;
			if (pat!=null && !pat.prune().hasData()) pat = null;
			if (bigDecimal!=null && !bigDecimal.prune().hasData()) bigDecimal = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getMap()!=null && getMap().hasData()) return true;
			if (getTheSet()!=null && getTheSet().hasData()) return true;
			if (getObjects()!=null && getObjects().hasData()) return true;
			if (getCollectors()!=null && getCollectors().hasData()) return true;
			if (getArrays()!=null && getArrays().hasData()) return true;
			if (getCollections()!=null && getCollections().hasData()) return true;
			if (getFn()!=null && getFn().hasData()) return true;
			if (getConsumer()!=null && getConsumer().hasData()) return true;
			if (getArrayList()!=null && getArrayList().hasData()) return true;
			if (getPat()!=null && getPat().hasData()) return true;
			if (getBigDecimal()!=null && getBigDecimal().hasData()) return true;
			if (getNames()!=null && !getNames().isEmpty()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public UtilRefs.UtilRefsBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			UtilRefs.UtilRefsBuilder o = (UtilRefs.UtilRefsBuilder) other;
			
			merger.mergeRosetta(getMap(), o.getMap(), this::setMap);
			merger.mergeRosetta(getTheSet(), o.getTheSet(), this::setTheSet);
			merger.mergeRosetta(getObjects(), o.getObjects(), this::setObjects);
			merger.mergeRosetta(getCollectors(), o.getCollectors(), this::setCollectors);
			merger.mergeRosetta(getArrays(), o.getArrays(), this::setArrays);
			merger.mergeRosetta(getCollections(), o.getCollections(), this::setCollections);
			merger.mergeRosetta(getFn(), o.getFn(), this::setFn);
			merger.mergeRosetta(getConsumer(), o.getConsumer(), this::setConsumer);
			merger.mergeRosetta(getArrayList(), o.getArrayList(), this::setArrayList);
			merger.mergeRosetta(getPat(), o.getPat(), this::setPat);
			merger.mergeRosetta(getBigDecimal(), o.getBigDecimal(), this::setBigDecimal);
			
			merger.mergeBasic(getNames(), o.getNames(), (java.util.function.Consumer<String>) this::addNames);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			UtilRefs _that = getType().cast(o);
		
			if (!java.util.Objects.equals(map, _that.getMap())) return false;
			if (!java.util.Objects.equals(theSet, _that.getTheSet())) return false;
			if (!java.util.Objects.equals(objects, _that.getObjects())) return false;
			if (!java.util.Objects.equals(collectors, _that.getCollectors())) return false;
			if (!java.util.Objects.equals(arrays, _that.getArrays())) return false;
			if (!java.util.Objects.equals(collections, _that.getCollections())) return false;
			if (!java.util.Objects.equals(fn, _that.getFn())) return false;
			if (!java.util.Objects.equals(consumer, _that.getConsumer())) return false;
			if (!java.util.Objects.equals(arrayList, _that.getArrayList())) return false;
			if (!java.util.Objects.equals(pat, _that.getPat())) return false;
			if (!java.util.Objects.equals(bigDecimal, _that.getBigDecimal())) return false;
			if (!ListEquals.listEquals(names, _that.getNames())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (map != null ? map.hashCode() : 0);
			_result = 31 * _result + (theSet != null ? theSet.hashCode() : 0);
			_result = 31 * _result + (objects != null ? objects.hashCode() : 0);
			_result = 31 * _result + (collectors != null ? collectors.hashCode() : 0);
			_result = 31 * _result + (arrays != null ? arrays.hashCode() : 0);
			_result = 31 * _result + (collections != null ? collections.hashCode() : 0);
			_result = 31 * _result + (fn != null ? fn.hashCode() : 0);
			_result = 31 * _result + (consumer != null ? consumer.hashCode() : 0);
			_result = 31 * _result + (arrayList != null ? arrayList.hashCode() : 0);
			_result = 31 * _result + (pat != null ? pat.hashCode() : 0);
			_result = 31 * _result + (bigDecimal != null ? bigDecimal.hashCode() : 0);
			_result = 31 * _result + (names != null ? names.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "UtilRefsBuilder {" +
				"map=" + this.map + ", " +
				"theSet=" + this.theSet + ", " +
				"objects=" + this.objects + ", " +
				"collectors=" + this.collectors + ", " +
				"arrays=" + this.arrays + ", " +
				"collections=" + this.collections + ", " +
				"fn=" + this.fn + ", " +
				"consumer=" + this.consumer + ", " +
				"arrayList=" + this.arrayList + ", " +
				"pat=" + this.pat + ", " +
				"bigDecimal=" + this.bigDecimal + ", " +
				"names=" + this.names +
			'}';
		}
	}
}
