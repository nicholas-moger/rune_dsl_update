package test.aliasscope;

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
import java.util.function.Consumer;
import java.util.stream.Collectors;
import test.aliasscope.meta.ClashMeta;

import static java.util.Optional.ofNullable;

/**
 * Multi attributes named after the wing&#39;s own method-scope identifiers (results, o) and its loop index (i) - the scope&#39;s numbering law under collision.
 * @version 0.0.0
 */
@RosettaDataType(value="Clash", builder=Clash.ClashBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Clash", model="test", builder=Clash.ClashBuilderImpl.class, version="0.0.0")
public interface Clash extends RosettaModelObject {

	ClashMeta metaData = new ClashMeta();

	/*********************** Getter Methods  ***********************/
	List<Integer> getResults();
	List<Integer> getO();
	List<Integer> getI();

	/*********************** Build Methods  ***********************/
	Clash build();
	
	Clash.ClashBuilder toBuilder();
	
	static Clash.ClashBuilder builder() {
		return new Clash.ClashBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Clash> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Clash> getType() {
		return Clash.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("results"), Integer.class, getResults(), this);
		processor.processBasic(path.newSubPath("o"), Integer.class, getO(), this);
		processor.processBasic(path.newSubPath("i"), Integer.class, getI(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface ClashBuilder extends Clash, RosettaModelObjectBuilder {
		Clash.ClashBuilder addResults(Integer results);
		Clash.ClashBuilder addResults(Integer results, int idx);
		Clash.ClashBuilder addResults(List<Integer> results);
		Clash.ClashBuilder setResults(List<Integer> results);
		Clash.ClashBuilder addO(Integer o);
		Clash.ClashBuilder addO(Integer o, int idx);
		Clash.ClashBuilder addO(List<Integer> o);
		Clash.ClashBuilder setO(List<Integer> o);
		Clash.ClashBuilder addI(Integer i);
		Clash.ClashBuilder addI(Integer i, int idx);
		Clash.ClashBuilder addI(List<Integer> i);
		Clash.ClashBuilder setI(List<Integer> i);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("results"), Integer.class, getResults(), this);
			processor.processBasic(path.newSubPath("o"), Integer.class, getO(), this);
			processor.processBasic(path.newSubPath("i"), Integer.class, getI(), this);
		}
		

		Clash.ClashBuilder prune();
	}

	/*********************** Immutable Implementation of Clash  ***********************/
	class ClashImpl implements Clash {
		private final List<Integer> results;
		private final List<Integer> o;
		private final List<Integer> i;
		
		protected ClashImpl(Clash.ClashBuilder builder) {
			this.results = ofNullable(builder.getResults()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.o = ofNullable(builder.getO()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.i = ofNullable(builder.getI()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
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
		@RosettaAttribute("o")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("o")
		public List<Integer> getO() {
			return o;
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
		public Clash build() {
			return this;
		}
		
		@Override
		public Clash.ClashBuilder toBuilder() {
			Clash.ClashBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Clash.ClashBuilder builder) {
			ofNullable(getResults()).ifPresent(builder::setResults);
			ofNullable(getO()).ifPresent(builder::setO);
			ofNullable(getI()).ifPresent(builder::setI);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Clash _that = getType().cast(o);
		
			if (!ListEquals.listEquals(results, _that.getResults())) return false;
			if (!ListEquals.listEquals(o, _that.getO())) return false;
			if (!ListEquals.listEquals(i, _that.getI())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (results != null ? results.hashCode() : 0);
			_result = 31 * _result + (o != null ? o.hashCode() : 0);
			_result = 31 * _result + (i != null ? i.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Clash {" +
				"results=" + this.results + ", " +
				"o=" + this.o + ", " +
				"i=" + this.i +
			'}';
		}
	}

	/*********************** Builder Implementation of Clash  ***********************/
	class ClashBuilderImpl implements Clash.ClashBuilder {
	
		protected List<Integer> results = new ArrayList<>();
		protected List<Integer> o = new ArrayList<>();
		protected List<Integer> i = new ArrayList<>();
		
		@Override
		@RosettaAttribute("results")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("results")
		public List<Integer> getResults() {
			return results;
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
		@RosettaAttribute("i")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("i")
		public List<Integer> getI() {
			return i;
		}
		
		@RosettaAttribute("results")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("results")
		@Override
		public Clash.ClashBuilder addResults(Integer _results) {
			if (_results != null) {
				this.results.add(_results);
			}
			return this;
		}
		
		@Override
		public Clash.ClashBuilder addResults(Integer _results, int idx) {
			getIndex(this.results, idx, () -> _results);
			return this;
		}
		
		@Override
		public Clash.ClashBuilder addResults(List<Integer> resultss) {
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
		public Clash.ClashBuilder setResults(List<Integer> resultss) {
			if (resultss == null) {
				this.results = new ArrayList<>();
			} else {
				this.results = resultss.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("o")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("o")
		@Override
		public Clash.ClashBuilder addO(Integer _o) {
			if (_o != null) {
				this.o.add(_o);
			}
			return this;
		}
		
		@Override
		public Clash.ClashBuilder addO(Integer _o, int idx) {
			getIndex(this.o, idx, () -> _o);
			return this;
		}
		
		@Override
		public Clash.ClashBuilder addO(List<Integer> os) {
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
		public Clash.ClashBuilder setO(List<Integer> os) {
			if (os == null) {
				this.o = new ArrayList<>();
			} else {
				this.o = os.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("i")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("i")
		@Override
		public Clash.ClashBuilder addI(Integer _i) {
			if (_i != null) {
				this.i.add(_i);
			}
			return this;
		}
		
		@Override
		public Clash.ClashBuilder addI(Integer _i, int idx) {
			getIndex(this.i, idx, () -> _i);
			return this;
		}
		
		@Override
		public Clash.ClashBuilder addI(List<Integer> is) {
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
		public Clash.ClashBuilder setI(List<Integer> is) {
			if (is == null) {
				this.i = new ArrayList<>();
			} else {
				this.i = is.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public Clash build() {
			return new Clash.ClashImpl(this);
		}
		
		@Override
		public Clash.ClashBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Clash.ClashBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getResults()!=null && !getResults().isEmpty()) return true;
			if (getO()!=null && !getO().isEmpty()) return true;
			if (getI()!=null && !getI().isEmpty()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Clash.ClashBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Clash.ClashBuilder o = (Clash.ClashBuilder) other;
			
			
			merger.mergeBasic(getResults(), o.getResults(), (Consumer<Integer>) this::addResults);
			merger.mergeBasic(getO(), o.getO(), (Consumer<Integer>) this::addO);
			merger.mergeBasic(getI(), o.getI(), (Consumer<Integer>) this::addI);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Clash _that = getType().cast(o);
		
			if (!ListEquals.listEquals(results, _that.getResults())) return false;
			if (!ListEquals.listEquals(o, _that.getO())) return false;
			if (!ListEquals.listEquals(i, _that.getI())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (results != null ? results.hashCode() : 0);
			_result = 31 * _result + (o != null ? o.hashCode() : 0);
			_result = 31 * _result + (i != null ? i.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "ClashBuilder {" +
				"results=" + this.results + ", " +
				"o=" + this.o + ", " +
				"i=" + this.i +
			'}';
		}
	}
}
