package test.fmeta026;

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
import com.rosetta.model.metafields.FieldWithMetaInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import test.fmeta026.meta.BMeta;


/**
 * @version 0.0.0
 */
@RosettaDataType(value="B", builder=B.BBuilderImpl.class, version="0.0.0")
@RuneDataType(value="B", model="test", builder=B.BBuilderImpl.class, version="0.0.0")
public interface B extends ABase {

	BMeta metaData = new BMeta();

	/*********************** Getter Methods  ***********************/

	/*********************** Build Methods  ***********************/
	B build();
	
	B.BBuilder toBuilder();
	
	static B.BBuilder builder() {
		return new B.BBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends B> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends B> getType() {
		return B.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("prop"), processor, FieldWithMetaInteger.class, getProp());
	}
	

	/*********************** Builder Interface  ***********************/
	interface BBuilder extends B, ABase.ABaseBuilder {
		@Override
		B.BBuilder addProp(FieldWithMetaInteger prop);
		@Override
		B.BBuilder addProp(FieldWithMetaInteger prop, int idx);
		@Override
		B.BBuilder addPropValue(Integer prop);
		@Override
		B.BBuilder addPropValue(Integer prop, int idx);
		@Override
		B.BBuilder addProp(List<? extends FieldWithMetaInteger> prop);
		@Override
		B.BBuilder setProp(List<? extends FieldWithMetaInteger> prop);
		@Override
		B.BBuilder addPropValue(List<? extends Integer> prop);
		@Override
		B.BBuilder setPropValue(List<? extends Integer> prop);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("prop"), processor, FieldWithMetaInteger.FieldWithMetaIntegerBuilder.class, getProp());
		}
		

		B.BBuilder prune();
	}

	/*********************** Immutable Implementation of B  ***********************/
	class BImpl extends ABase.ABaseImpl implements B {
		
		protected BImpl(B.BBuilder builder) {
			super(builder);
		}
		
		@Override
		public B build() {
			return this;
		}
		
		@Override
		public B.BBuilder toBuilder() {
			B.BBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(B.BBuilder builder) {
			super.setBuilderFields(builder);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
			if (!super.equals(o)) return false;
		
		
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = super.hashCode();
			return _result;
		}
		
		@Override
		public String toString() {
			return "B {" +
			'}' + " " + super.toString();
		}
	}

	/*********************** Builder Implementation of B  ***********************/
	class BBuilderImpl extends ABase.ABaseBuilderImpl implements B.BBuilder {
	
		
		@RosettaAttribute("prop")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("prop")
		@Override
		public B.BBuilder addProp(FieldWithMetaInteger _prop) {
			if (_prop != null) {
				this.prop.add(_prop.toBuilder());
			}
			return this;
		}
		
		@Override
		public B.BBuilder addProp(FieldWithMetaInteger _prop, int idx) {
			getIndex(this.prop, idx, () -> _prop.toBuilder());
			return this;
		}
		
		@Override
		public B.BBuilder addPropValue(Integer _prop) {
			this.getOrCreateProp(-1).setValue(_prop);
			return this;
		}
		
		@Override
		public B.BBuilder addPropValue(Integer _prop, int idx) {
			this.getOrCreateProp(idx).setValue(_prop);
			return this;
		}
		
		@Override
		public B.BBuilder addProp(List<? extends FieldWithMetaInteger> props) {
			if (props != null) {
				for (final FieldWithMetaInteger toAdd : props) {
					this.prop.add(toAdd.toBuilder());
				}
			}
			return this;
		}
		
		@RosettaAttribute("prop")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("prop")
		@Override
		public B.BBuilder setProp(List<? extends FieldWithMetaInteger> props) {
			if (props == null) {
				this.prop = new ArrayList<>();
			} else {
				this.prop = props.stream()
					.map(_a->_a.toBuilder())
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public B.BBuilder addPropValue(List<? extends Integer> props) {
			if (props != null) {
				for (final Integer toAdd : props) {
					this.addPropValue(toAdd);
				}
			}
			return this;
		}
		
		@Override
		public B.BBuilder setPropValue(List<? extends Integer> props) {
			this.prop.clear();
			if (props != null) {
				props.forEach(this::addPropValue);
			}
			return this;
		}
		
		@Override
		public B build() {
			return new B.BImpl(this);
		}
		
		@Override
		public B.BBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public B.BBuilder prune() {
			super.prune();
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (super.hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public B.BBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			super.merge(other, merger);
			B.BBuilder o = (B.BBuilder) other;
			
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
			if (!super.equals(o)) return false;
		
		
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = super.hashCode();
			return _result;
		}
		
		@Override
		public String toString() {
			return "BBuilder {" +
			'}' + " " + super.toString();
		}
	}
}
