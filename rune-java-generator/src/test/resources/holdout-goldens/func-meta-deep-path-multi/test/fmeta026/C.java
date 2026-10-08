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
import test.fmeta026.meta.CMeta;


/**
 * @version 0.0.0
 */
@RosettaDataType(value="C", builder=C.CBuilderImpl.class, version="0.0.0")
@RuneDataType(value="C", model="test", builder=C.CBuilderImpl.class, version="0.0.0")
public interface C extends ABase {

	CMeta metaData = new CMeta();

	/*********************** Getter Methods  ***********************/

	/*********************** Build Methods  ***********************/
	C build();
	
	C.CBuilder toBuilder();
	
	static C.CBuilder builder() {
		return new C.CBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C> getType() {
		return C.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("prop"), processor, FieldWithMetaInteger.class, getProp());
	}
	

	/*********************** Builder Interface  ***********************/
	interface CBuilder extends C, ABase.ABaseBuilder {
		@Override
		C.CBuilder addProp(FieldWithMetaInteger prop);
		@Override
		C.CBuilder addProp(FieldWithMetaInteger prop, int idx);
		@Override
		C.CBuilder addPropValue(Integer prop);
		@Override
		C.CBuilder addPropValue(Integer prop, int idx);
		@Override
		C.CBuilder addProp(List<? extends FieldWithMetaInteger> prop);
		@Override
		C.CBuilder setProp(List<? extends FieldWithMetaInteger> prop);
		@Override
		C.CBuilder addPropValue(List<? extends Integer> prop);
		@Override
		C.CBuilder setPropValue(List<? extends Integer> prop);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("prop"), processor, FieldWithMetaInteger.FieldWithMetaIntegerBuilder.class, getProp());
		}
		

		C.CBuilder prune();
	}

	/*********************** Immutable Implementation of C  ***********************/
	class CImpl extends ABase.ABaseImpl implements C {
		
		protected CImpl(C.CBuilder builder) {
			super(builder);
		}
		
		@Override
		public C build() {
			return this;
		}
		
		@Override
		public C.CBuilder toBuilder() {
			C.CBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C.CBuilder builder) {
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
			return "C {" +
			'}' + " " + super.toString();
		}
	}

	/*********************** Builder Implementation of C  ***********************/
	class CBuilderImpl extends ABase.ABaseBuilderImpl implements C.CBuilder {
	
		
		@RosettaAttribute("prop")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("prop")
		@Override
		public C.CBuilder addProp(FieldWithMetaInteger _prop) {
			if (_prop != null) {
				this.prop.add(_prop.toBuilder());
			}
			return this;
		}
		
		@Override
		public C.CBuilder addProp(FieldWithMetaInteger _prop, int idx) {
			getIndex(this.prop, idx, () -> _prop.toBuilder());
			return this;
		}
		
		@Override
		public C.CBuilder addPropValue(Integer _prop) {
			this.getOrCreateProp(-1).setValue(_prop);
			return this;
		}
		
		@Override
		public C.CBuilder addPropValue(Integer _prop, int idx) {
			this.getOrCreateProp(idx).setValue(_prop);
			return this;
		}
		
		@Override
		public C.CBuilder addProp(List<? extends FieldWithMetaInteger> props) {
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
		public C.CBuilder setProp(List<? extends FieldWithMetaInteger> props) {
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
		public C.CBuilder addPropValue(List<? extends Integer> props) {
			if (props != null) {
				for (final Integer toAdd : props) {
					this.addPropValue(toAdd);
				}
			}
			return this;
		}
		
		@Override
		public C.CBuilder setPropValue(List<? extends Integer> props) {
			this.prop.clear();
			if (props != null) {
				props.forEach(this::addPropValue);
			}
			return this;
		}
		
		@Override
		public C build() {
			return new C.CImpl(this);
		}
		
		@Override
		public C.CBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C.CBuilder prune() {
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
		public C.CBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			super.merge(other, merger);
			C.CBuilder o = (C.CBuilder) other;
			
			
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
			return "CBuilder {" +
			'}' + " " + super.toString();
		}
	}
}
