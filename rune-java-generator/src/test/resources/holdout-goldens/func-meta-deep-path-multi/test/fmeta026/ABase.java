package test.fmeta026;

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
import com.rosetta.model.metafields.FieldWithMetaInteger;
import com.rosetta.util.ListEquals;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import test.fmeta026.meta.ABaseMeta;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="ABase", builder=ABase.ABaseBuilderImpl.class, version="0.0.0")
@RuneDataType(value="ABase", model="test", builder=ABase.ABaseBuilderImpl.class, version="0.0.0")
public interface ABase extends RosettaModelObject {

	ABaseMeta metaData = new ABaseMeta();

	/*********************** Getter Methods  ***********************/
	List<? extends FieldWithMetaInteger> getProp();

	/*********************** Build Methods  ***********************/
	ABase build();
	
	ABase.ABaseBuilder toBuilder();
	
	static ABase.ABaseBuilder builder() {
		return new ABase.ABaseBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends ABase> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends ABase> getType() {
		return ABase.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("prop"), processor, FieldWithMetaInteger.class, getProp());
	}
	

	/*********************** Builder Interface  ***********************/
	interface ABaseBuilder extends ABase, RosettaModelObjectBuilder {
		FieldWithMetaInteger.FieldWithMetaIntegerBuilder getOrCreateProp(int index);
		@Override
		List<? extends FieldWithMetaInteger.FieldWithMetaIntegerBuilder> getProp();
		ABase.ABaseBuilder addProp(FieldWithMetaInteger prop);
		ABase.ABaseBuilder addProp(FieldWithMetaInteger prop, int idx);
		ABase.ABaseBuilder addPropValue(Integer prop);
		ABase.ABaseBuilder addPropValue(Integer prop, int idx);
		ABase.ABaseBuilder addProp(List<? extends FieldWithMetaInteger> prop);
		ABase.ABaseBuilder setProp(List<? extends FieldWithMetaInteger> prop);
		ABase.ABaseBuilder addPropValue(List<? extends Integer> prop);
		ABase.ABaseBuilder setPropValue(List<? extends Integer> prop);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("prop"), processor, FieldWithMetaInteger.FieldWithMetaIntegerBuilder.class, getProp());
		}
		

		ABase.ABaseBuilder prune();
	}

	/*********************** Immutable Implementation of ABase  ***********************/
	class ABaseImpl implements ABase {
		private final List<? extends FieldWithMetaInteger> prop;
		
		protected ABaseImpl(ABase.ABaseBuilder builder) {
			this.prop = ofNullable(builder.getProp()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
		}
		
		@Override
		@RosettaAttribute("prop")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("prop")
		public List<? extends FieldWithMetaInteger> getProp() {
			return prop;
		}
		
		@Override
		public ABase build() {
			return this;
		}
		
		@Override
		public ABase.ABaseBuilder toBuilder() {
			ABase.ABaseBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(ABase.ABaseBuilder builder) {
			ofNullable(getProp()).ifPresent(builder::setProp);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			ABase _that = getType().cast(o);
		
			if (!ListEquals.listEquals(prop, _that.getProp())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (prop != null ? prop.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "ABase {" +
				"prop=" + this.prop +
			'}';
		}
	}

	/*********************** Builder Implementation of ABase  ***********************/
	class ABaseBuilderImpl implements ABase.ABaseBuilder {
	
		protected List<FieldWithMetaInteger.FieldWithMetaIntegerBuilder> prop = new ArrayList<>();
		
		@Override
		@RosettaAttribute("prop")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("prop")
		public List<? extends FieldWithMetaInteger.FieldWithMetaIntegerBuilder> getProp() {
			return prop;
		}
		
		@Override
		public FieldWithMetaInteger.FieldWithMetaIntegerBuilder getOrCreateProp(int index) {
			if (prop==null) {
				this.prop = new ArrayList<>();
			}
			return getIndex(prop, index, () -> {
						FieldWithMetaInteger.FieldWithMetaIntegerBuilder newProp = FieldWithMetaInteger.builder();
						return newProp;
					});
		}
		
		@RosettaAttribute("prop")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("prop")
		@Override
		public ABase.ABaseBuilder addProp(FieldWithMetaInteger _prop) {
			if (_prop != null) {
				this.prop.add(_prop.toBuilder());
			}
			return this;
		}
		
		@Override
		public ABase.ABaseBuilder addProp(FieldWithMetaInteger _prop, int idx) {
			getIndex(this.prop, idx, () -> _prop.toBuilder());
			return this;
		}
		
		@Override
		public ABase.ABaseBuilder addPropValue(Integer _prop) {
			this.getOrCreateProp(-1).setValue(_prop);
			return this;
		}
		
		@Override
		public ABase.ABaseBuilder addPropValue(Integer _prop, int idx) {
			this.getOrCreateProp(idx).setValue(_prop);
			return this;
		}
		
		@Override
		public ABase.ABaseBuilder addProp(List<? extends FieldWithMetaInteger> props) {
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
		public ABase.ABaseBuilder setProp(List<? extends FieldWithMetaInteger> props) {
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
		public ABase.ABaseBuilder addPropValue(List<? extends Integer> props) {
			if (props != null) {
				for (final Integer toAdd : props) {
					this.addPropValue(toAdd);
				}
			}
			return this;
		}
		
		@Override
		public ABase.ABaseBuilder setPropValue(List<? extends Integer> props) {
			this.prop.clear();
			if (props != null) {
				props.forEach(this::addPropValue);
			}
			return this;
		}
		
		@Override
		public ABase build() {
			return new ABase.ABaseImpl(this);
		}
		
		@Override
		public ABase.ABaseBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public ABase.ABaseBuilder prune() {
			prop = prop.stream().filter(b->b!=null).<FieldWithMetaInteger.FieldWithMetaIntegerBuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getProp()!=null && !getProp().isEmpty()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public ABase.ABaseBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			ABase.ABaseBuilder o = (ABase.ABaseBuilder) other;
			
			merger.mergeRosetta(getProp(), o.getProp(), this::getOrCreateProp);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			ABase _that = getType().cast(o);
		
			if (!ListEquals.listEquals(prop, _that.getProp())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (prop != null ? prop.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "ABaseBuilder {" +
				"prop=" + this.prop +
			'}';
		}
	}
}
