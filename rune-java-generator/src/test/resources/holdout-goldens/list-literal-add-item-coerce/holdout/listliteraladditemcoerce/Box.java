package holdout.listliteraladditemcoerce;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import holdout.listliteraladditemcoerce.meta.BoxMeta;
import java.math.BigDecimal;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * The holder.
 * @version 0.0.0
 */
@RosettaDataType(value="Box", builder=Box.BoxBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Box", model="holdout", builder=Box.BoxBuilderImpl.class, version="0.0.0")
public interface Box extends RosettaModelObject {

	BoxMeta metaData = new BoxMeta();

	/*********************** Getter Methods  ***********************/
	BigDecimal getWeight();

	/*********************** Build Methods  ***********************/
	Box build();
	
	Box.BoxBuilder toBuilder();
	
	static Box.BoxBuilder builder() {
		return new Box.BoxBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Box> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Box> getType() {
		return Box.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("weight"), BigDecimal.class, getWeight(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface BoxBuilder extends Box, RosettaModelObjectBuilder {
		Box.BoxBuilder setWeight(BigDecimal weight);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("weight"), BigDecimal.class, getWeight(), this);
		}
		

		Box.BoxBuilder prune();
	}

	/*********************** Immutable Implementation of Box  ***********************/
	class BoxImpl implements Box {
		private final BigDecimal weight;
		
		protected BoxImpl(Box.BoxBuilder builder) {
			this.weight = builder.getWeight();
		}
		
		@Override
		@RosettaAttribute("weight")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("weight")
		public BigDecimal getWeight() {
			return weight;
		}
		
		@Override
		public Box build() {
			return this;
		}
		
		@Override
		public Box.BoxBuilder toBuilder() {
			Box.BoxBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Box.BoxBuilder builder) {
			ofNullable(getWeight()).ifPresent(builder::setWeight);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Box _that = getType().cast(o);
		
			if (!Objects.equals(weight, _that.getWeight())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (weight != null ? weight.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Box {" +
				"weight=" + this.weight +
			'}';
		}
	}

	/*********************** Builder Implementation of Box  ***********************/
	class BoxBuilderImpl implements Box.BoxBuilder {
	
		protected BigDecimal weight;
		
		@Override
		@RosettaAttribute("weight")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("weight")
		public BigDecimal getWeight() {
			return weight;
		}
		
		@RosettaAttribute("weight")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("weight")
		@Override
		public Box.BoxBuilder setWeight(BigDecimal _weight) {
			this.weight = _weight == null ? null : _weight;
			return this;
		}
		
		@Override
		public Box build() {
			return new Box.BoxImpl(this);
		}
		
		@Override
		public Box.BoxBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Box.BoxBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getWeight()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Box.BoxBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Box.BoxBuilder o = (Box.BoxBuilder) other;
			
			
			merger.mergeBasic(getWeight(), o.getWeight(), this::setWeight);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Box _that = getType().cast(o);
		
			if (!Objects.equals(weight, _that.getWeight())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (weight != null ? weight.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "BoxBuilder {" +
				"weight=" + this.weight +
			'}';
		}
	}
}
