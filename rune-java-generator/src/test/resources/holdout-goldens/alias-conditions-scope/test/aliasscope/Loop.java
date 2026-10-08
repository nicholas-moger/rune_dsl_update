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
import test.aliasscope.meta.LoopMeta;

import static java.util.Optional.ofNullable;

/**
 * ONE multi attribute named i - the attribute local and the loop index want the same name in one body scope.
 * @version 0.0.0
 */
@RosettaDataType(value="Loop", builder=Loop.LoopBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Loop", model="test", builder=Loop.LoopBuilderImpl.class, version="0.0.0")
public interface Loop extends RosettaModelObject {

	LoopMeta metaData = new LoopMeta();

	/*********************** Getter Methods  ***********************/
	List<Integer> getI();

	/*********************** Build Methods  ***********************/
	Loop build();
	
	Loop.LoopBuilder toBuilder();
	
	static Loop.LoopBuilder builder() {
		return new Loop.LoopBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Loop> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Loop> getType() {
		return Loop.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("i"), Integer.class, getI(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface LoopBuilder extends Loop, RosettaModelObjectBuilder {
		Loop.LoopBuilder addI(Integer i);
		Loop.LoopBuilder addI(Integer i, int idx);
		Loop.LoopBuilder addI(List<Integer> i);
		Loop.LoopBuilder setI(List<Integer> i);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("i"), Integer.class, getI(), this);
		}
		

		Loop.LoopBuilder prune();
	}

	/*********************** Immutable Implementation of Loop  ***********************/
	class LoopImpl implements Loop {
		private final List<Integer> i;
		
		protected LoopImpl(Loop.LoopBuilder builder) {
			this.i = ofNullable(builder.getI()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
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
		public Loop build() {
			return this;
		}
		
		@Override
		public Loop.LoopBuilder toBuilder() {
			Loop.LoopBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Loop.LoopBuilder builder) {
			ofNullable(getI()).ifPresent(builder::setI);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Loop _that = getType().cast(o);
		
			if (!ListEquals.listEquals(i, _that.getI())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (i != null ? i.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Loop {" +
				"i=" + this.i +
			'}';
		}
	}

	/*********************** Builder Implementation of Loop  ***********************/
	class LoopBuilderImpl implements Loop.LoopBuilder {
	
		protected List<Integer> i = new ArrayList<>();
		
		@Override
		@RosettaAttribute("i")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("i")
		public List<Integer> getI() {
			return i;
		}
		
		@RosettaAttribute("i")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("i")
		@Override
		public Loop.LoopBuilder addI(Integer _i) {
			if (_i != null) {
				this.i.add(_i);
			}
			return this;
		}
		
		@Override
		public Loop.LoopBuilder addI(Integer _i, int idx) {
			getIndex(this.i, idx, () -> _i);
			return this;
		}
		
		@Override
		public Loop.LoopBuilder addI(List<Integer> is) {
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
		public Loop.LoopBuilder setI(List<Integer> is) {
			if (is == null) {
				this.i = new ArrayList<>();
			} else {
				this.i = is.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public Loop build() {
			return new Loop.LoopImpl(this);
		}
		
		@Override
		public Loop.LoopBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Loop.LoopBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getI()!=null && !getI().isEmpty()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Loop.LoopBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Loop.LoopBuilder o = (Loop.LoopBuilder) other;
			
			
			merger.mergeBasic(getI(), o.getI(), (Consumer<Integer>) this::addI);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Loop _that = getType().cast(o);
		
			if (!ListEquals.listEquals(i, _that.getI())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (i != null ? i.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "LoopBuilder {" +
				"i=" + this.i +
			'}';
		}
	}
}
