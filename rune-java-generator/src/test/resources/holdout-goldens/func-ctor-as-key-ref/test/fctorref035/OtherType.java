package test.fctorref035;

import com.google.common.collect.ImmutableList;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Multi;
import com.rosetta.model.lib.annotations.Required;
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
import java.util.Objects;
import java.util.stream.Collectors;
import test.fctorref035.meta.OtherTypeMeta;
import test.fctorref035.metafields.ReferenceWithMetaTypeWithKey;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="OtherType", builder=OtherType.OtherTypeBuilderImpl.class, version="0.0.0")
@RuneDataType(value="OtherType", model="test", builder=OtherType.OtherTypeBuilderImpl.class, version="0.0.0")
public interface OtherType extends RosettaModelObject {

	OtherTypeMeta metaData = new OtherTypeMeta();

	/*********************** Getter Methods  ***********************/
	ReferenceWithMetaTypeWithKey getAttrSingle();
	List<? extends ReferenceWithMetaTypeWithKey> getAttrMulti();

	/*********************** Build Methods  ***********************/
	OtherType build();
	
	OtherType.OtherTypeBuilder toBuilder();
	
	static OtherType.OtherTypeBuilder builder() {
		return new OtherType.OtherTypeBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends OtherType> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends OtherType> getType() {
		return OtherType.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("attrSingle"), processor, ReferenceWithMetaTypeWithKey.class, getAttrSingle());
		processRosetta(path.newSubPath("attrMulti"), processor, ReferenceWithMetaTypeWithKey.class, getAttrMulti());
	}
	

	/*********************** Builder Interface  ***********************/
	interface OtherTypeBuilder extends OtherType, RosettaModelObjectBuilder {
		ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyBuilder getOrCreateAttrSingle();
		@Override
		ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyBuilder getAttrSingle();
		ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyBuilder getOrCreateAttrMulti(int index);
		@Override
		List<? extends ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyBuilder> getAttrMulti();
		OtherType.OtherTypeBuilder setAttrSingle(ReferenceWithMetaTypeWithKey attrSingle);
		OtherType.OtherTypeBuilder setAttrSingleValue(TypeWithKey attrSingle);
		OtherType.OtherTypeBuilder addAttrMulti(ReferenceWithMetaTypeWithKey attrMulti);
		OtherType.OtherTypeBuilder addAttrMulti(ReferenceWithMetaTypeWithKey attrMulti, int idx);
		OtherType.OtherTypeBuilder addAttrMultiValue(TypeWithKey attrMulti);
		OtherType.OtherTypeBuilder addAttrMultiValue(TypeWithKey attrMulti, int idx);
		OtherType.OtherTypeBuilder addAttrMulti(List<? extends ReferenceWithMetaTypeWithKey> attrMulti);
		OtherType.OtherTypeBuilder setAttrMulti(List<? extends ReferenceWithMetaTypeWithKey> attrMulti);
		OtherType.OtherTypeBuilder addAttrMultiValue(List<? extends TypeWithKey> attrMulti);
		OtherType.OtherTypeBuilder setAttrMultiValue(List<? extends TypeWithKey> attrMulti);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("attrSingle"), processor, ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyBuilder.class, getAttrSingle());
			processRosetta(path.newSubPath("attrMulti"), processor, ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyBuilder.class, getAttrMulti());
		}
		

		OtherType.OtherTypeBuilder prune();
	}

	/*********************** Immutable Implementation of OtherType  ***********************/
	class OtherTypeImpl implements OtherType {
		private final ReferenceWithMetaTypeWithKey attrSingle;
		private final List<? extends ReferenceWithMetaTypeWithKey> attrMulti;
		
		protected OtherTypeImpl(OtherType.OtherTypeBuilder builder) {
			this.attrSingle = ofNullable(builder.getAttrSingle()).map(f->f.build()).orElse(null);
			this.attrMulti = ofNullable(builder.getAttrMulti()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
		}
		
		@Override
		@RosettaAttribute("attrSingle")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("attrSingle")
		public ReferenceWithMetaTypeWithKey getAttrSingle() {
			return attrSingle;
		}
		
		@Override
		@RosettaAttribute("attrMulti")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("attrMulti")
		public List<? extends ReferenceWithMetaTypeWithKey> getAttrMulti() {
			return attrMulti;
		}
		
		@Override
		public OtherType build() {
			return this;
		}
		
		@Override
		public OtherType.OtherTypeBuilder toBuilder() {
			OtherType.OtherTypeBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(OtherType.OtherTypeBuilder builder) {
			ofNullable(getAttrSingle()).ifPresent(builder::setAttrSingle);
			ofNullable(getAttrMulti()).ifPresent(builder::setAttrMulti);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			OtherType _that = getType().cast(o);
		
			if (!Objects.equals(attrSingle, _that.getAttrSingle())) return false;
			if (!ListEquals.listEquals(attrMulti, _that.getAttrMulti())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (attrSingle != null ? attrSingle.hashCode() : 0);
			_result = 31 * _result + (attrMulti != null ? attrMulti.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "OtherType {" +
				"attrSingle=" + this.attrSingle + ", " +
				"attrMulti=" + this.attrMulti +
			'}';
		}
	}

	/*********************** Builder Implementation of OtherType  ***********************/
	class OtherTypeBuilderImpl implements OtherType.OtherTypeBuilder {
	
		protected ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyBuilder attrSingle;
		protected List<ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyBuilder> attrMulti = new ArrayList<>();
		
		@Override
		@RosettaAttribute("attrSingle")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("attrSingle")
		public ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyBuilder getAttrSingle() {
			return attrSingle;
		}
		
		@Override
		public ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyBuilder getOrCreateAttrSingle() {
			ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyBuilder result;
			if (attrSingle!=null) {
				result = attrSingle;
			}
			else {
				result = attrSingle = ReferenceWithMetaTypeWithKey.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("attrMulti")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("attrMulti")
		public List<? extends ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyBuilder> getAttrMulti() {
			return attrMulti;
		}
		
		@Override
		public ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyBuilder getOrCreateAttrMulti(int index) {
			if (attrMulti==null) {
				this.attrMulti = new ArrayList<>();
			}
			return getIndex(attrMulti, index, () -> {
						ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyBuilder newAttrMulti = ReferenceWithMetaTypeWithKey.builder();
						return newAttrMulti;
					});
		}
		
		@RosettaAttribute("attrSingle")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("attrSingle")
		@Override
		public OtherType.OtherTypeBuilder setAttrSingle(ReferenceWithMetaTypeWithKey _attrSingle) {
			this.attrSingle = _attrSingle == null ? null : _attrSingle.toBuilder();
			return this;
		}
		
		@Override
		public OtherType.OtherTypeBuilder setAttrSingleValue(TypeWithKey _attrSingle) {
			this.getOrCreateAttrSingle().setValue(_attrSingle);
			return this;
		}
		
		@RosettaAttribute("attrMulti")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("attrMulti")
		@Override
		public OtherType.OtherTypeBuilder addAttrMulti(ReferenceWithMetaTypeWithKey _attrMulti) {
			if (_attrMulti != null) {
				this.attrMulti.add(_attrMulti.toBuilder());
			}
			return this;
		}
		
		@Override
		public OtherType.OtherTypeBuilder addAttrMulti(ReferenceWithMetaTypeWithKey _attrMulti, int idx) {
			getIndex(this.attrMulti, idx, () -> _attrMulti.toBuilder());
			return this;
		}
		
		@Override
		public OtherType.OtherTypeBuilder addAttrMultiValue(TypeWithKey _attrMulti) {
			this.getOrCreateAttrMulti(-1).setValue(_attrMulti.toBuilder());
			return this;
		}
		
		@Override
		public OtherType.OtherTypeBuilder addAttrMultiValue(TypeWithKey _attrMulti, int idx) {
			this.getOrCreateAttrMulti(idx).setValue(_attrMulti.toBuilder());
			return this;
		}
		
		@Override
		public OtherType.OtherTypeBuilder addAttrMulti(List<? extends ReferenceWithMetaTypeWithKey> attrMultis) {
			if (attrMultis != null) {
				for (final ReferenceWithMetaTypeWithKey toAdd : attrMultis) {
					this.attrMulti.add(toAdd.toBuilder());
				}
			}
			return this;
		}
		
		@RosettaAttribute("attrMulti")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("attrMulti")
		@Override
		public OtherType.OtherTypeBuilder setAttrMulti(List<? extends ReferenceWithMetaTypeWithKey> attrMultis) {
			if (attrMultis == null) {
				this.attrMulti = new ArrayList<>();
			} else {
				this.attrMulti = attrMultis.stream()
					.map(_a->_a.toBuilder())
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public OtherType.OtherTypeBuilder addAttrMultiValue(List<? extends TypeWithKey> attrMultis) {
			if (attrMultis != null) {
				for (final TypeWithKey toAdd : attrMultis) {
					this.addAttrMultiValue(toAdd);
				}
			}
			return this;
		}
		
		@Override
		public OtherType.OtherTypeBuilder setAttrMultiValue(List<? extends TypeWithKey> attrMultis) {
			this.attrMulti.clear();
			if (attrMultis != null) {
				attrMultis.forEach(this::addAttrMultiValue);
			}
			return this;
		}
		
		@Override
		public OtherType build() {
			return new OtherType.OtherTypeImpl(this);
		}
		
		@Override
		public OtherType.OtherTypeBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public OtherType.OtherTypeBuilder prune() {
			if (attrSingle!=null && !attrSingle.prune().hasData()) attrSingle = null;
			attrMulti = attrMulti.stream().filter(b->b!=null).<ReferenceWithMetaTypeWithKey.ReferenceWithMetaTypeWithKeyBuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getAttrSingle()!=null && getAttrSingle().hasData()) return true;
			if (getAttrMulti()!=null && getAttrMulti().stream().filter(Objects::nonNull).anyMatch(a->a.hasData())) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public OtherType.OtherTypeBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			OtherType.OtherTypeBuilder o = (OtherType.OtherTypeBuilder) other;
			
			merger.mergeRosetta(getAttrSingle(), o.getAttrSingle(), this::setAttrSingle);
			merger.mergeRosetta(getAttrMulti(), o.getAttrMulti(), this::getOrCreateAttrMulti);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			OtherType _that = getType().cast(o);
		
			if (!Objects.equals(attrSingle, _that.getAttrSingle())) return false;
			if (!ListEquals.listEquals(attrMulti, _that.getAttrMulti())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (attrSingle != null ? attrSingle.hashCode() : 0);
			_result = 31 * _result + (attrMulti != null ? attrMulti.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "OtherTypeBuilder {" +
				"attrSingle=" + this.attrSingle + ", " +
				"attrMulti=" + this.attrMulti +
			'}';
		}
	}
}
