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
import test.aliasscope.meta.FieldClashMeta;

import static java.util.Optional.ofNullable;

/**
 * A multi attribute named after the condition&#39;s own @Inject field (natNonNeg) - a body local against a class-scope field.
 * @version 0.0.0
 */
@RosettaDataType(value="FieldClash", builder=FieldClash.FieldClashBuilderImpl.class, version="0.0.0")
@RuneDataType(value="FieldClash", model="test", builder=FieldClash.FieldClashBuilderImpl.class, version="0.0.0")
public interface FieldClash extends RosettaModelObject {

	FieldClashMeta metaData = new FieldClashMeta();

	/*********************** Getter Methods  ***********************/
	List<Integer> getNatNonNeg();

	/*********************** Build Methods  ***********************/
	FieldClash build();
	
	FieldClash.FieldClashBuilder toBuilder();
	
	static FieldClash.FieldClashBuilder builder() {
		return new FieldClash.FieldClashBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends FieldClash> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends FieldClash> getType() {
		return FieldClash.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("natNonNeg"), Integer.class, getNatNonNeg(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface FieldClashBuilder extends FieldClash, RosettaModelObjectBuilder {
		FieldClash.FieldClashBuilder addNatNonNeg(Integer natNonNeg);
		FieldClash.FieldClashBuilder addNatNonNeg(Integer natNonNeg, int idx);
		FieldClash.FieldClashBuilder addNatNonNeg(List<Integer> natNonNeg);
		FieldClash.FieldClashBuilder setNatNonNeg(List<Integer> natNonNeg);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("natNonNeg"), Integer.class, getNatNonNeg(), this);
		}
		

		FieldClash.FieldClashBuilder prune();
	}

	/*********************** Immutable Implementation of FieldClash  ***********************/
	class FieldClashImpl implements FieldClash {
		private final List<Integer> natNonNeg;
		
		protected FieldClashImpl(FieldClash.FieldClashBuilder builder) {
			this.natNonNeg = ofNullable(builder.getNatNonNeg()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
		}
		
		@Override
		@RosettaAttribute("natNonNeg")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("natNonNeg")
		public List<Integer> getNatNonNeg() {
			return natNonNeg;
		}
		
		@Override
		public FieldClash build() {
			return this;
		}
		
		@Override
		public FieldClash.FieldClashBuilder toBuilder() {
			FieldClash.FieldClashBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(FieldClash.FieldClashBuilder builder) {
			ofNullable(getNatNonNeg()).ifPresent(builder::setNatNonNeg);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			FieldClash _that = getType().cast(o);
		
			if (!ListEquals.listEquals(natNonNeg, _that.getNatNonNeg())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (natNonNeg != null ? natNonNeg.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "FieldClash {" +
				"natNonNeg=" + this.natNonNeg +
			'}';
		}
	}

	/*********************** Builder Implementation of FieldClash  ***********************/
	class FieldClashBuilderImpl implements FieldClash.FieldClashBuilder {
	
		protected List<Integer> natNonNeg = new ArrayList<>();
		
		@Override
		@RosettaAttribute("natNonNeg")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("natNonNeg")
		public List<Integer> getNatNonNeg() {
			return natNonNeg;
		}
		
		@RosettaAttribute("natNonNeg")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("natNonNeg")
		@Override
		public FieldClash.FieldClashBuilder addNatNonNeg(Integer _natNonNeg) {
			if (_natNonNeg != null) {
				this.natNonNeg.add(_natNonNeg);
			}
			return this;
		}
		
		@Override
		public FieldClash.FieldClashBuilder addNatNonNeg(Integer _natNonNeg, int idx) {
			getIndex(this.natNonNeg, idx, () -> _natNonNeg);
			return this;
		}
		
		@Override
		public FieldClash.FieldClashBuilder addNatNonNeg(List<Integer> natNonNegs) {
			if (natNonNegs != null) {
				for (final Integer toAdd : natNonNegs) {
					this.natNonNeg.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("natNonNeg")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("natNonNeg")
		@Override
		public FieldClash.FieldClashBuilder setNatNonNeg(List<Integer> natNonNegs) {
			if (natNonNegs == null) {
				this.natNonNeg = new ArrayList<>();
			} else {
				this.natNonNeg = natNonNegs.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public FieldClash build() {
			return new FieldClash.FieldClashImpl(this);
		}
		
		@Override
		public FieldClash.FieldClashBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public FieldClash.FieldClashBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getNatNonNeg()!=null && !getNatNonNeg().isEmpty()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public FieldClash.FieldClashBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			FieldClash.FieldClashBuilder o = (FieldClash.FieldClashBuilder) other;
			
			
			merger.mergeBasic(getNatNonNeg(), o.getNatNonNeg(), (Consumer<Integer>) this::addNatNonNeg);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			FieldClash _that = getType().cast(o);
		
			if (!ListEquals.listEquals(natNonNeg, _that.getNatNonNeg())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (natNonNeg != null ? natNonNeg.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "FieldClashBuilder {" +
				"natNonNeg=" + this.natNonNeg +
			'}';
		}
	}
}
