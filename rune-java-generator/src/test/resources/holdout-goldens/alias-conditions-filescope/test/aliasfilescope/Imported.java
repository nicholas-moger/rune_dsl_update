package test.aliasfilescope;

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
import test.aliasfilescope.meta.ImportedMeta;

import static java.util.Optional.ofNullable;

/**
 * Multi attributes named after the wing&#39;s imported simple names (Streams, ArrayList, Inject) and a java.lang one (Integer) - the file scope&#39;s imports against the attribute-named local.
 * @version 0.0.0
 */
@RosettaDataType(value="Imported", builder=Imported.ImportedBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Imported", model="test", builder=Imported.ImportedBuilderImpl.class, version="0.0.0")
public interface Imported extends RosettaModelObject {

	ImportedMeta metaData = new ImportedMeta();

	/*********************** Getter Methods  ***********************/
	List<Integer> getStreams();
	List<Integer> getArrayList();
	List<Integer> getInject();
	List<Integer> getInteger();

	/*********************** Build Methods  ***********************/
	Imported build();
	
	Imported.ImportedBuilder toBuilder();
	
	static Imported.ImportedBuilder builder() {
		return new Imported.ImportedBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Imported> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Imported> getType() {
		return Imported.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("Streams"), Integer.class, getStreams(), this);
		processor.processBasic(path.newSubPath("ArrayList"), Integer.class, getArrayList(), this);
		processor.processBasic(path.newSubPath("Inject"), Integer.class, getInject(), this);
		processor.processBasic(path.newSubPath("Integer"), Integer.class, getInteger(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface ImportedBuilder extends Imported, RosettaModelObjectBuilder {
		Imported.ImportedBuilder addStreams(Integer Streams);
		Imported.ImportedBuilder addStreams(Integer Streams, int idx);
		Imported.ImportedBuilder addStreams(List<Integer> Streams);
		Imported.ImportedBuilder setStreams(List<Integer> Streams);
		Imported.ImportedBuilder addArrayList(Integer _ArrayList);
		Imported.ImportedBuilder addArrayList(Integer _ArrayList, int idx);
		Imported.ImportedBuilder addArrayList(List<Integer> _ArrayList);
		Imported.ImportedBuilder setArrayList(List<Integer> _ArrayList);
		Imported.ImportedBuilder addInject(Integer Inject);
		Imported.ImportedBuilder addInject(Integer Inject, int idx);
		Imported.ImportedBuilder addInject(List<Integer> Inject);
		Imported.ImportedBuilder setInject(List<Integer> Inject);
		Imported.ImportedBuilder addInteger(Integer _Integer);
		Imported.ImportedBuilder addInteger(Integer _Integer, int idx);
		Imported.ImportedBuilder addInteger(List<Integer> _Integer);
		Imported.ImportedBuilder setInteger(List<Integer> _Integer);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("Streams"), Integer.class, getStreams(), this);
			processor.processBasic(path.newSubPath("ArrayList"), Integer.class, getArrayList(), this);
			processor.processBasic(path.newSubPath("Inject"), Integer.class, getInject(), this);
			processor.processBasic(path.newSubPath("Integer"), Integer.class, getInteger(), this);
		}
		

		Imported.ImportedBuilder prune();
	}

	/*********************** Immutable Implementation of Imported  ***********************/
	class ImportedImpl implements Imported {
		private final List<Integer> streams;
		private final List<Integer> arrayList;
		private final List<Integer> inject;
		private final List<Integer> integer;
		
		protected ImportedImpl(Imported.ImportedBuilder builder) {
			this.streams = ofNullable(builder.getStreams()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.arrayList = ofNullable(builder.getArrayList()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.inject = ofNullable(builder.getInject()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.integer = ofNullable(builder.getInteger()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
		}
		
		@Override
		@RosettaAttribute("Streams")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("Streams")
		public List<Integer> getStreams() {
			return streams;
		}
		
		@Override
		@RosettaAttribute("ArrayList")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("ArrayList")
		public List<Integer> getArrayList() {
			return arrayList;
		}
		
		@Override
		@RosettaAttribute("Inject")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("Inject")
		public List<Integer> getInject() {
			return inject;
		}
		
		@Override
		@RosettaAttribute("Integer")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("Integer")
		public List<Integer> getInteger() {
			return integer;
		}
		
		@Override
		public Imported build() {
			return this;
		}
		
		@Override
		public Imported.ImportedBuilder toBuilder() {
			Imported.ImportedBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Imported.ImportedBuilder builder) {
			ofNullable(getStreams()).ifPresent(builder::setStreams);
			ofNullable(getArrayList()).ifPresent(builder::setArrayList);
			ofNullable(getInject()).ifPresent(builder::setInject);
			ofNullable(getInteger()).ifPresent(builder::setInteger);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Imported _that = getType().cast(o);
		
			if (!ListEquals.listEquals(streams, _that.getStreams())) return false;
			if (!ListEquals.listEquals(arrayList, _that.getArrayList())) return false;
			if (!ListEquals.listEquals(inject, _that.getInject())) return false;
			if (!ListEquals.listEquals(integer, _that.getInteger())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (streams != null ? streams.hashCode() : 0);
			_result = 31 * _result + (arrayList != null ? arrayList.hashCode() : 0);
			_result = 31 * _result + (inject != null ? inject.hashCode() : 0);
			_result = 31 * _result + (integer != null ? integer.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Imported {" +
				"Streams=" + this.streams + ", " +
				"ArrayList=" + this.arrayList + ", " +
				"Inject=" + this.inject + ", " +
				"Integer=" + this.integer +
			'}';
		}
	}

	/*********************** Builder Implementation of Imported  ***********************/
	class ImportedBuilderImpl implements Imported.ImportedBuilder {
	
		protected List<Integer> streams = new ArrayList<>();
		protected List<Integer> arrayList = new ArrayList<>();
		protected List<Integer> inject = new ArrayList<>();
		protected List<Integer> integer = new ArrayList<>();
		
		@Override
		@RosettaAttribute("Streams")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("Streams")
		public List<Integer> getStreams() {
			return streams;
		}
		
		@Override
		@RosettaAttribute("ArrayList")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("ArrayList")
		public List<Integer> getArrayList() {
			return arrayList;
		}
		
		@Override
		@RosettaAttribute("Inject")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("Inject")
		public List<Integer> getInject() {
			return inject;
		}
		
		@Override
		@RosettaAttribute("Integer")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("Integer")
		public List<Integer> getInteger() {
			return integer;
		}
		
		@RosettaAttribute("Streams")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("Streams")
		@Override
		public Imported.ImportedBuilder addStreams(Integer _streams) {
			if (_streams != null) {
				this.streams.add(_streams);
			}
			return this;
		}
		
		@Override
		public Imported.ImportedBuilder addStreams(Integer _streams, int idx) {
			getIndex(this.streams, idx, () -> _streams);
			return this;
		}
		
		@Override
		public Imported.ImportedBuilder addStreams(List<Integer> streamss) {
			if (streamss != null) {
				for (final Integer toAdd : streamss) {
					this.streams.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("Streams")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("Streams")
		@Override
		public Imported.ImportedBuilder setStreams(List<Integer> streamss) {
			if (streamss == null) {
				this.streams = new ArrayList<>();
			} else {
				this.streams = streamss.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("ArrayList")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("ArrayList")
		@Override
		public Imported.ImportedBuilder addArrayList(Integer _arrayList) {
			if (_arrayList != null) {
				this.arrayList.add(_arrayList);
			}
			return this;
		}
		
		@Override
		public Imported.ImportedBuilder addArrayList(Integer _arrayList, int idx) {
			getIndex(this.arrayList, idx, () -> _arrayList);
			return this;
		}
		
		@Override
		public Imported.ImportedBuilder addArrayList(List<Integer> arrayLists) {
			if (arrayLists != null) {
				for (final Integer toAdd : arrayLists) {
					this.arrayList.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("ArrayList")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("ArrayList")
		@Override
		public Imported.ImportedBuilder setArrayList(List<Integer> arrayLists) {
			if (arrayLists == null) {
				this.arrayList = new ArrayList<>();
			} else {
				this.arrayList = arrayLists.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("Inject")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("Inject")
		@Override
		public Imported.ImportedBuilder addInject(Integer _inject) {
			if (_inject != null) {
				this.inject.add(_inject);
			}
			return this;
		}
		
		@Override
		public Imported.ImportedBuilder addInject(Integer _inject, int idx) {
			getIndex(this.inject, idx, () -> _inject);
			return this;
		}
		
		@Override
		public Imported.ImportedBuilder addInject(List<Integer> injects) {
			if (injects != null) {
				for (final Integer toAdd : injects) {
					this.inject.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("Inject")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("Inject")
		@Override
		public Imported.ImportedBuilder setInject(List<Integer> injects) {
			if (injects == null) {
				this.inject = new ArrayList<>();
			} else {
				this.inject = injects.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("Integer")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("Integer")
		@Override
		public Imported.ImportedBuilder addInteger(Integer _integer) {
			if (_integer != null) {
				this.integer.add(_integer);
			}
			return this;
		}
		
		@Override
		public Imported.ImportedBuilder addInteger(Integer _integer, int idx) {
			getIndex(this.integer, idx, () -> _integer);
			return this;
		}
		
		@Override
		public Imported.ImportedBuilder addInteger(List<Integer> integers) {
			if (integers != null) {
				for (final Integer toAdd : integers) {
					this.integer.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("Integer")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("Integer")
		@Override
		public Imported.ImportedBuilder setInteger(List<Integer> integers) {
			if (integers == null) {
				this.integer = new ArrayList<>();
			} else {
				this.integer = integers.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public Imported build() {
			return new Imported.ImportedImpl(this);
		}
		
		@Override
		public Imported.ImportedBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Imported.ImportedBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getStreams()!=null && !getStreams().isEmpty()) return true;
			if (getArrayList()!=null && !getArrayList().isEmpty()) return true;
			if (getInject()!=null && !getInject().isEmpty()) return true;
			if (getInteger()!=null && !getInteger().isEmpty()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Imported.ImportedBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Imported.ImportedBuilder o = (Imported.ImportedBuilder) other;
			
			
			merger.mergeBasic(getStreams(), o.getStreams(), (Consumer<Integer>) this::addStreams);
			merger.mergeBasic(getArrayList(), o.getArrayList(), (Consumer<Integer>) this::addArrayList);
			merger.mergeBasic(getInject(), o.getInject(), (Consumer<Integer>) this::addInject);
			merger.mergeBasic(getInteger(), o.getInteger(), (Consumer<Integer>) this::addInteger);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Imported _that = getType().cast(o);
		
			if (!ListEquals.listEquals(streams, _that.getStreams())) return false;
			if (!ListEquals.listEquals(arrayList, _that.getArrayList())) return false;
			if (!ListEquals.listEquals(inject, _that.getInject())) return false;
			if (!ListEquals.listEquals(integer, _that.getInteger())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (streams != null ? streams.hashCode() : 0);
			_result = 31 * _result + (arrayList != null ? arrayList.hashCode() : 0);
			_result = 31 * _result + (inject != null ? inject.hashCode() : 0);
			_result = 31 * _result + (integer != null ? integer.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "ImportedBuilder {" +
				"Streams=" + this.streams + ", " +
				"ArrayList=" + this.arrayList + ", " +
				"Inject=" + this.inject + ", " +
				"Integer=" + this.integer +
			'}';
		}
	}
}
